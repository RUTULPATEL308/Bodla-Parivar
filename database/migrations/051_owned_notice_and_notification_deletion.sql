CREATE OR REPLACE FUNCTION public.delete_notice(p_notice_id UUID)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_profile_id UUID;
    v_published_by UUID;
BEGIN
    SELECT p.id
      INTO v_profile_id
      FROM public.profiles p
     WHERE p.auth_user_id = auth.uid()
       AND COALESCE(p.status, 'ACTIVE') = 'ACTIVE';

    SELECT n.published_by
      INTO v_published_by
      FROM public.notices n
     WHERE n.id = p_notice_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Notice not found';
    END IF;

    IF NOT public.is_staff()
       AND (v_profile_id IS NULL OR v_published_by IS DISTINCT FROM v_profile_id) THEN
        RAISE EXCEPTION 'Not authorized to delete this notice';
    END IF;

    DELETE FROM public.notifications
     WHERE reference_id = p_notice_id;

    DELETE FROM public.notices
     WHERE id = p_notice_id;
END;
$$;

REVOKE ALL ON FUNCTION public.delete_notice(UUID) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.delete_notice(UUID) TO authenticated;

CREATE OR REPLACE FUNCTION public.delete_notification(p_notification_id UUID)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_profile_id UUID;
    v_created_by UUID;
    v_reference_id UUID;
    v_type TEXT;
    v_notice_owner UUID;
BEGIN
    SELECT p.id
      INTO v_profile_id
      FROM public.profiles p
     WHERE p.auth_user_id = auth.uid()
       AND COALESCE(p.status, 'ACTIVE') = 'ACTIVE';

    SELECT n.created_by, n.reference_id, n.type
      INTO v_created_by, v_reference_id, v_type
      FROM public.notifications n
     WHERE n.id = p_notification_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Notification not found';
    END IF;

    IF NOT public.is_staff()
       AND (v_profile_id IS NULL OR v_created_by IS DISTINCT FROM v_profile_id) THEN
        RAISE EXCEPTION 'Not authorized to delete this notification';
    END IF;

    IF v_type = 'NOTICE' AND v_reference_id IS NOT NULL THEN
        SELECT n.published_by
          INTO v_notice_owner
          FROM public.notices n
         WHERE n.id = v_reference_id;

        IF FOUND AND (public.is_staff() OR v_notice_owner = v_profile_id) THEN
            DELETE FROM public.notifications
             WHERE reference_id = v_reference_id;
            DELETE FROM public.notices
             WHERE id = v_reference_id;
            RETURN;
        END IF;
    END IF;

    DELETE FROM public.notifications
     WHERE id = p_notification_id;
END;
$$;

REVOKE ALL ON FUNCTION public.delete_notification(UUID) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.delete_notification(UUID) TO authenticated;

NOTIFY pgrst, 'reload schema';
