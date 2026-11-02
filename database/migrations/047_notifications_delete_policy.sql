-- Allow staff to delete notifications (needed for notice deletion cleanup)
DROP POLICY IF EXISTS "Staff can delete notifications" ON public.notifications;
CREATE POLICY "Staff can delete notifications"
    ON public.notifications FOR DELETE
    TO authenticated
    USING (public.is_staff());

GRANT DELETE ON public.notifications TO authenticated;