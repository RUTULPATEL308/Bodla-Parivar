-- Ensure offering and bid changes are published to Supabase Realtime.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_publication_tables
        WHERE pubname = 'supabase_realtime'
          AND schemaname = 'public'
          AND tablename = 'offerings'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.offerings;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_publication_tables
        WHERE pubname = 'supabase_realtime'
          AND schemaname = 'public'
          AND tablename = 'offering_bids'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.offering_bids;
    END IF;
END;
$$;

ALTER TABLE public.offering_bids REPLICA IDENTITY FULL;

NOTIFY pgrst, 'reload schema';
