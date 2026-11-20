-- ==============================================================================
-- Migration: 058_approve_notice_rpc.sql
-- Description: RPC function to approve a pending notice securely (bypassing RLS)
-- ==============================================================================

CREATE OR REPLACE FUNCTION public.approve_notice(p_notice_id UUID)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    updated_notice public.notices%ROWTYPE;
    notif_id UUID;
BEGIN
    UPDATE public.notices
    SET status = 'PUBLISHED', publish_at = now()
    WHERE id = p_notice_id
    RETURNING * INTO updated_notice;

    IF updated_notice.id IS NULL THEN
        RAISE EXCEPTION 'Notice not found.';
    END IF;

    INSERT INTO public.notifications (
        title_gu, title_en, body_gu, body_en, type, reference_id, image_url, attachment_url
    ) VALUES (
        updated_notice.title_gu, updated_notice.title_en,
        updated_notice.description_gu, updated_notice.description_en,
        'NOTICE', updated_notice.id, updated_notice.image_url, updated_notice.attachment_url
    ) RETURNING id INTO notif_id;

    RETURN jsonb_build_object('success', TRUE, 'notice_id', updated_notice.id, 'notification_id', notif_id);
END;
$$;

REVOKE ALL ON FUNCTION public.approve_notice(UUID) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.approve_notice(UUID) TO authenticated;
