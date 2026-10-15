-- ==============================================================================
-- Migration: 025_notifications_read_access.sql
-- Description: Grant SELECT and INSERT permissions on public.notifications to anon and authenticated
-- ==============================================================================

ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Anyone can view notifications" ON public.notifications;
CREATE POLICY "Anyone can view notifications"
    ON public.notifications FOR SELECT
    TO anon, authenticated
    USING (true);

DROP POLICY IF EXISTS "Authenticated can insert notifications" ON public.notifications;
CREATE POLICY "Authenticated can insert notifications"
    ON public.notifications FOR INSERT
    TO authenticated
    WITH CHECK (true);

DROP POLICY IF EXISTS "Anon can insert notifications" ON public.notifications;
CREATE POLICY "Anon can insert notifications"
    ON public.notifications FOR INSERT
    TO anon
    WITH CHECK (true);

GRANT SELECT, INSERT ON public.notifications TO anon, authenticated;
