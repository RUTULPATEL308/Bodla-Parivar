-- ==============================================================================
-- Migration: 053_publish_notice_rpc_media.sql
-- Description: Ensure publish_antim_yatra_notice handles image_url and attachment_url correctly
-- ==============================================================================

CREATE OR REPLACE FUNCTION public.publish_antim_yatra_notice(p_payload JSONB)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    profile_id UUID;
    notice_id UUID;
    notification_id UUID;
    title_gu TEXT := btrim(COALESCE(p_payload ->> 'title_gu', ''));
    title_en TEXT := btrim(COALESCE(p_payload ->> 'title_en', ''));
    body_gu TEXT := btrim(COALESCE(p_payload ->> 'body_gu', ''));
    body_en TEXT := btrim(COALESCE(p_payload ->> 'body_en', ''));
    description_gu TEXT := btrim(COALESCE(p_payload ->> 'description_gu', ''));
    description_en TEXT := btrim(COALESCE(p_payload ->> 'description_en', ''));
    image_url TEXT := btrim(COALESCE(p_payload ->> 'image_url', ''));
    attachment_url TEXT := btrim(COALESCE(p_payload ->> 'attachment_url', ''));
    recent_count BIGINT;
BEGIN
    IF auth.uid() IS NULL THEN
        RAISE EXCEPTION 'Sign in to publish this notice.';
    END IF;

    SELECT p.id INTO profile_id
    FROM public.profiles AS p
    WHERE p.auth_user_id = auth.uid() AND p.status = 'ACTIVE'
    LIMIT 1;

    IF profile_id IS NULL THEN
        RAISE EXCEPTION 'An active member profile is required to publish this notice.';
    END IF;

    IF title_gu = '' OR body_gu = '' OR length(title_gu) > 80 OR length(title_en) > 120
       OR length(body_gu) > 240 OR length(body_en) > 300
       OR length(description_gu) > 4000 OR length(description_en) > 4000 THEN
        RAISE EXCEPTION 'Notice title or message is missing or too long.';
    END IF;

    SELECT count(*) INTO recent_count
    FROM public.notices AS n
    WHERE n.published_by = profile_id
      AND n.created_at >= now() - interval '24 hours';

    IF recent_count >= 5 THEN
        RAISE EXCEPTION 'You have reached the limit of 5 community notices in 24 hours.';
    END IF;

    INSERT INTO public.notices (
        title_gu, title_en, description_gu, description_en,
        image_url, attachment_url,
        status, is_pinned, publish_at, published_by
    ) VALUES (
        title_gu, COALESCE(NULLIF(title_en, ''), title_gu),
        COALESCE(NULLIF(description_gu, ''), body_gu),
        COALESCE(NULLIF(description_en, ''), body_en),
        NULLIF(image_url, ''), NULLIF(attachment_url, ''),
        'PUBLISHED', FALSE, now(), profile_id
    ) RETURNING id INTO notice_id;

    INSERT INTO public.notifications (
        title_gu, title_en, body_gu, body_en, type, reference_id, created_by,
        image_url, attachment_url
    ) VALUES (
        title_gu, COALESCE(NULLIF(title_en, ''), title_gu),
        body_gu, COALESCE(NULLIF(body_en, ''), body_gu),
        'NOTICE', notice_id, profile_id,
        NULLIF(image_url, ''), NULLIF(attachment_url, '')
    ) RETURNING id INTO notification_id;

    RETURN jsonb_build_object(
        'success', TRUE,
        'notice_created', TRUE,
        'notification_created', TRUE,
        'notice_id', notice_id,
        'notification_id', notification_id,
        'push_sent', FALSE
    );
END;
$$;

REVOKE ALL ON FUNCTION public.publish_antim_yatra_notice(JSONB) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.publish_antim_yatra_notice(JSONB) TO authenticated;
