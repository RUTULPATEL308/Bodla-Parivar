-- Increase community notice rate limit from 3 to 20 notices per 24 hours
CREATE OR REPLACE FUNCTION public.publish_antim_yatra_notice(p_payload jsonb)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth
AS $$
DECLARE
    profile_id uuid;
    recent_count integer;
    notice_id uuid;
    notification_id uuid;
    title_gu text;
    title_en text;
    notification_title_gu text;
    notification_title_en text;
    body_gu text;
    body_en text;
    description_gu text;
    description_en text;
    image_url text;
    user_is_staff boolean;
BEGIN
    SELECT p.id INTO profile_id
    FROM public.profiles AS p
    WHERE p.auth_user_id = auth.uid()
      AND p.status = 'ACTIVE';

    IF profile_id IS NULL THEN
        RAISE EXCEPTION 'An active member profile is required to publish this notice.';
    END IF;

    title_gu := trim(COALESCE(p_payload->>'title_gu', ''));
    title_en := trim(COALESCE(p_payload->>'title_en', ''));
    notification_title_gu := trim(COALESCE(p_payload->>'notification_title_gu', title_gu));
    notification_title_en := trim(COALESCE(p_payload->>'notification_title_en', title_en));
    body_gu := trim(COALESCE(p_payload->>'body_gu', ''));
    body_en := trim(COALESCE(p_payload->>'body_en', ''));
    description_gu := trim(COALESCE(p_payload->>'description_gu', body_gu));
    description_en := trim(COALESCE(p_payload->>'description_en', body_en));
    image_url := trim(COALESCE(p_payload->>'image_url', ''));

    IF title_gu = '' OR body_gu = '' OR length(title_gu) > 80 OR length(title_en) > 120
       OR length(notification_title_gu) > 80 OR length(notification_title_en) > 120
       OR length(body_gu) > 240 OR length(body_en) > 300
       OR length(description_gu) > 4000 OR length(description_en) > 4000 THEN
        RAISE EXCEPTION 'Notice title or message is missing or too long.';
    END IF;

    SELECT EXISTS (
        SELECT 1 FROM public.user_roles ur
        JOIN public.roles r ON r.id = ur.role_id
        WHERE ur.user_id = profile_id
          AND r.name IN ('ADMIN', 'SUPER_ADMIN', 'MODERATOR', 'CONTENT_MANAGER')
    ) INTO user_is_staff;

    IF NOT user_is_staff THEN
        SELECT count(*) INTO recent_count
        FROM public.notices AS n
        WHERE n.published_by = profile_id
          AND n.created_at >= now() - interval '24 hours';

        IF recent_count >= 20 THEN
            RAISE EXCEPTION 'You have reached the limit of 20 community notices in 24 hours.';
        END IF;
    END IF;

    INSERT INTO public.notices (
        title_gu, title_en, description_gu, description_en, image_url,
        status, is_pinned, publish_at, published_by
    ) VALUES (
        title_gu, COALESCE(NULLIF(title_en, ''), title_gu),
        COALESCE(NULLIF(description_gu, ''), body_gu),
        COALESCE(NULLIF(description_en, ''), body_en), NULLIF(image_url, ''),
        'PUBLISHED', FALSE, now(), profile_id
    ) RETURNING id INTO notice_id;

    INSERT INTO public.notifications (
        title_gu, title_en, body_gu, body_en, type, reference_id, created_by, payload
    ) VALUES (
        notification_title_gu, COALESCE(NULLIF(notification_title_en, ''), notification_title_gu),
        body_gu, COALESCE(NULLIF(body_en, ''), body_gu),
        'NOTICE', notice_id, profile_id,
        jsonb_strip_nulls(jsonb_build_object('image_url', NULLIF(image_url, '')))
    ) RETURNING id INTO notification_id;

    RETURN jsonb_build_object(
        'success', true,
        'notice_created', true,
        'notification_created', true,
        'notice_id', notice_id,
        'notification_id', notification_id
    );
END;
$$;

