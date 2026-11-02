-- Store generated Antim Yatra posters in a public bucket so Notices and
-- notification history can display the actual poster image.
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES ('antim-yatra-posters', 'antim-yatra-posters', TRUE, 10485760, ARRAY['image/png'])
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    public = TRUE,
    file_size_limit = EXCLUDED.file_size_limit,
    allowed_mime_types = EXCLUDED.allowed_mime_types;

-- Check membership through a definer function so the Storage policy does not
-- depend on profile-table SELECT policies or an optional current_profile_id().
CREATE OR REPLACE FUNCTION public.antim_yatra_active_member()
RETURNS BOOLEAN
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
    SELECT EXISTS (
        SELECT 1
        FROM public.profiles AS p
        WHERE p.auth_user_id = auth.uid()
          AND p.status = 'ACTIVE'
    );
$$;

REVOKE ALL ON FUNCTION public.antim_yatra_active_member() FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.antim_yatra_active_member() TO authenticated;

DROP POLICY IF EXISTS "Active members upload Antim Yatra posters" ON storage.objects;
CREATE POLICY "Active members upload Antim Yatra posters" ON storage.objects
    FOR INSERT TO authenticated
    WITH CHECK (
        bucket_id = 'antim-yatra-posters'
        AND (storage.foldername(name))[1] = auth.uid()::text
        AND public.antim_yatra_active_member()
    );

DROP POLICY IF EXISTS "Members remove own Antim Yatra posters" ON storage.objects;
CREATE POLICY "Members remove own Antim Yatra posters" ON storage.objects
    FOR DELETE TO authenticated
    USING (
        bucket_id = 'antim-yatra-posters'
        AND (storage.foldername(name))[1] = auth.uid()::text
    );

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
    IF length(image_url) > 2048 OR (image_url <> '' AND image_url NOT LIKE '%/object/public/antim-yatra-posters/%') THEN
        RAISE EXCEPTION 'The poster image URL is invalid.';
    END IF;

    SELECT count(*) INTO recent_count
    FROM public.notices AS n
    WHERE n.published_by = profile_id
      AND n.created_at >= now() - interval '24 hours';

    IF recent_count >= 3 THEN
        RAISE EXCEPTION 'You have reached the limit of 3 community notices in 24 hours.';
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
        title_gu, COALESCE(NULLIF(title_en, ''), title_gu),
        body_gu, COALESCE(NULLIF(body_en, ''), body_gu), 'NOTICE', notice_id, profile_id,
        CASE WHEN image_url = '' THEN '{}'::jsonb ELSE jsonb_build_object('image_url', image_url) END
    ) RETURNING id INTO notification_id;

    RETURN jsonb_build_object(
        'success', TRUE,
        'notice_created', TRUE,
        'notification_created', TRUE,
        'notice_id', notice_id,
        'notification_id', notification_id,
        'image_url', NULLIF(image_url, ''),
        'push_sent', FALSE
    );
END;
$$;

REVOKE ALL ON FUNCTION public.publish_antim_yatra_notice(JSONB) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.publish_antim_yatra_notice(JSONB) TO authenticated;
