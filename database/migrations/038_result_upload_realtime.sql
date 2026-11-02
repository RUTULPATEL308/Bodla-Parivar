-- Publish result submissions so the admin dashboard can receive new rows live.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_publication WHERE pubname = 'supabase_realtime')
       AND NOT EXISTS (
           SELECT 1
           FROM pg_publication_tables
           WHERE pubname = 'supabase_realtime'
             AND schemaname = 'public'
             AND tablename = 'result_uploads'
       ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.result_uploads;
    END IF;
END $$;
