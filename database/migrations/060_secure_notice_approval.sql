-- Secure notice approval and keep the mobile app's approval path idempotent.
CREATE OR REPLACE FUNCTION public.approve_notice(p_notice_id UUID)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    caller_profile_id UUID;
    updated_notice public.notices%ROWTYPE;
    notif_id UUID;
BEGIN
    SELECT p.id
      INTO caller_profile_id
      FROM public.profiles AS p
      JOIN public.user_roles AS ur ON ur.user_id = p.id
      JOIN public.roles AS r ON r.id = ur.role_id
     WHERE p.auth_user_id = auth.uid()
       AND p.status = 'ACTIVE'
       AND r.name IN ('ADMIN', 'SUPER_ADMIN', 'MODERATOR', 'CONTENT_MANAGER')
     LIMIT 1;

    IF caller_profile_id IS NULL THEN
        RAISE EXCEPTION 'Only active staff can approve notices.' USING ERRCODE = '42501';
    END IF;

    UPDATE public.notices
       SET status = 'PUBLISHED', publish_at = now()
     WHERE id = p_notice_id
       AND status IN ('PENDING', 'PENDING_APPROVAL')
     RETURNING * INTO updated_notice;

    IF updated_notice.id IS NULL THEN
        IF EXISTS (SELECT 1 FROM public.notices WHERE id = p_notice_id AND status = 'PUBLISHED') THEN
            RETURN jsonb_build_object('success', TRUE, 'already_published', TRUE, 'notice_id', p_notice_id);
        END IF;
        RAISE EXCEPTION 'Pending notice not found.';
    END IF;

    INSERT INTO public.notifications (
        title_gu, title_en, body_gu, body_en, type, reference_id, image_url, attachment_url
    ) VALUES (
        updated_notice.title_gu, updated_notice.title_en,
        COALESCE(updated_notice.description_gu, ''), COALESCE(updated_notice.description_en, ''),
        'NOTICE', updated_notice.id, updated_notice.image_url, updated_notice.attachment_url
    ) RETURNING id INTO notif_id;

    RETURN jsonb_build_object('success', TRUE, 'notice_id', updated_notice.id, 'notification_id', notif_id);
END;
$$;

REVOKE ALL ON FUNCTION public.approve_notice(UUID) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.approve_notice(UUID) TO authenticated;
