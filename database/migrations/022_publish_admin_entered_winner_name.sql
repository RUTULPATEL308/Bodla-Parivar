BEGIN;

CREATE OR REPLACE FUNCTION public.call_offering_bid(p_offering_id UUID, p_winner_name TEXT DEFAULT NULL)
RETURNS INTEGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
  updated_call_count SMALLINT;
  top_bid_amount NUMERIC(12, 2);
BEGIN
  IF NOT public.is_staff() THEN
    RAISE EXCEPTION 'Staff access required';
  END IF;

  SELECT b.amount
    INTO top_bid_amount
    FROM public.offering_bids b
    WHERE b.offering_id = p_offering_id
      AND b.status <> 'REJECTED'
    ORDER BY b.amount DESC, b.created_at ASC
    LIMIT 1;

  UPDATE public.offerings o
    SET bid_call_count = o.bid_call_count + 1,
        status = CASE WHEN o.bid_call_count = 2 THEN 'COMPLETED' ELSE o.status END,
        bid_closed_at = CASE WHEN o.bid_call_count = 2 THEN NOW() ELSE o.bid_closed_at END,
        winning_bid_amount = CASE
          WHEN o.bid_call_count = 2 THEN top_bid_amount
          ELSE o.winning_bid_amount
        END
    WHERE o.id = p_offering_id
      AND o.status = 'ACTIVE'
      AND o.bid_call_count < 3
      AND EXISTS (
        SELECT 1
        FROM public.offering_bids b
        WHERE b.offering_id = p_offering_id
          AND b.status <> 'REJECTED'
      )
    RETURNING o.bid_call_count INTO updated_call_count;

  IF NOT FOUND THEN
    RAISE EXCEPTION 'Offering is closed or has no eligible bids';
  END IF;

  IF updated_call_count = 3 THEN
    PERFORM public.start_winner_name_flow(p_offering_id);

    IF NULLIF(BTRIM(p_winner_name), '') IS NOT NULL
       AND EXISTS (
         SELECT 1
         FROM public.offerings o
         WHERE o.id = p_offering_id
           AND o.winner_name_status = 'AWAITING_ADMIN'
       ) THEN
      PERFORM public.admin_set_winner_display_name(p_offering_id, p_winner_name);
    END IF;
  END IF;

  RETURN updated_call_count;
END;
$$;

REVOKE ALL ON FUNCTION public.call_offering_bid(UUID, TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.call_offering_bid(UUID, TEXT) TO authenticated;

NOTIFY pgrst, 'reload schema';

COMMIT;