CREATE OR REPLACE FUNCTION public.refresh_offering_winner_after_bid_status_change()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
  next_bidder_name TEXT;
  next_bid_amount NUMERIC(12, 2);
BEGIN
  IF OLD.status IS NOT DISTINCT FROM NEW.status THEN
    RETURN NEW;
  END IF;

  SELECT b.bidder_name, b.amount
    INTO next_bidder_name, next_bid_amount
    FROM public.offering_bids b
    WHERE b.offering_id = NEW.offering_id
      AND b.status <> 'REJECTED'
    ORDER BY b.amount DESC, b.created_at ASC
    LIMIT 1;

  UPDATE public.offerings o
    SET amount = next_bid_amount,
        winning_bid_amount = CASE
          WHEN o.bid_closed_at IS NULL THEN o.winning_bid_amount
          ELSE next_bid_amount
        END,
        winning_bidder_name = CASE
          WHEN o.bid_closed_at IS NULL THEN o.winning_bidder_name
          ELSE next_bidder_name
        END
  WHERE o.id = NEW.offering_id;

  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_refresh_offering_winner_after_bid_status_change ON public.offering_bids;
CREATE TRIGGER trg_refresh_offering_winner_after_bid_status_change
  AFTER UPDATE OF status ON public.offering_bids
  FOR EACH ROW
  EXECUTE FUNCTION public.refresh_offering_winner_after_bid_status_change();

UPDATE public.offerings o
SET amount = (
  SELECT b.amount
  FROM public.offering_bids b
  WHERE b.offering_id = o.id
    AND b.status <> 'REJECTED'
  ORDER BY b.amount DESC, b.created_at ASC
  LIMIT 1
)
WHERE EXISTS (
  SELECT 1
  FROM public.offerings offering
  WHERE offering.id = o.id
);

NOTIFY pgrst, 'reload schema';
