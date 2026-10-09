-- A new bid restarts the three-call cycle for an active offering.
CREATE OR REPLACE FUNCTION public.reset_offering_bid_calls_after_bid()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
  UPDATE public.offerings
    SET bid_call_count = 0
    WHERE id = NEW.offering_id
      AND status = 'ACTIVE'
      AND bid_closed_at IS NULL
      AND bid_call_count > 0;

  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_reset_offering_bid_calls_after_bid ON public.offering_bids;
CREATE TRIGGER trg_reset_offering_bid_calls_after_bid
  AFTER INSERT ON public.offering_bids
  FOR EACH ROW EXECUTE FUNCTION public.reset_offering_bid_calls_after_bid();

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
