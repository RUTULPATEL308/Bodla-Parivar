-- RPC function for staff to delete a notice (bypasses RLS via SECURITY DEFINER)
CREATE OR REPLACE FUNCTION public.delete_notice(p_notice_id UUID)
RETURNS VOID AS $$
BEGIN
    -- Check if caller is staff
    IF NOT public.is_staff() THEN
        RAISE EXCEPTION 'Not authorized';
    END IF;
    -- Delete related notifications
    DELETE FROM public.notifications WHERE reference_id = p_notice_id;
    -- Delete the notice
    DELETE FROM public.notices WHERE id = p_notice_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

GRANT EXECUTE ON FUNCTION public.delete_notice(UUID) TO authenticated;