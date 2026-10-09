-- ==============================================================================
-- Migration: 016_notifications_and_realtime_broadcast.sql
-- Description: Expand notification types, enable realtime broadcast, and add RLS
-- ==============================================================================

-- 1. Relax notification type check constraint
ALTER TABLE public.notifications DROP CONSTRAINT IF EXISTS notifications_type_check;
ALTER TABLE public.notifications ADD CONSTRAINT notifications_type_check
  CHECK (type IN ('NOTICE', 'EVENT', 'COMPLAINT', 'OFFERING', 'BUSINESS', 'JOB', 'SYSTEM_BROADCAST', 'BID_CALL', 'BID_APPROVED'));

-- 2. Ensure notifications table is in Supabase Realtime publication
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_publication_tables
    WHERE pubname = 'supabase_realtime'
      AND schemaname = 'public'
      AND tablename = 'notifications'
  ) THEN
    ALTER PUBLICATION supabase_realtime ADD TABLE public.notifications;
  END IF;
END;
$$;

ALTER TABLE public.notifications REPLICA IDENTITY FULL;

-- 3. Row Level Security policies for notifications
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
