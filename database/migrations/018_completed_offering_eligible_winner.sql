CREATE OR REPLACE FUNCTION public.refresh_offering_winner_after_bid_delete()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
  next_bidder_name TEXT;
  next_bid_amount NUMERIC(12, 2);
BEGIN
  SELECT b.bidder_name, b.amount
    INTO next_bidder_name, next_bid_amount
    FROM public.offering_bids b
    WHERE b.offering_id = OLD.offering_id
      AND b.status <> 'REJECTED'
    ORDER BY b.amount DESC, b.created_at ASC
    LIMIT 1;

  UPDATE public.offerings o
    SET amount = CASE
          WHEN o.amount = OLD.amount THEN next_bid_amount
          ELSE o.amount
        END,
        winning_bid_amount = CASE
          WHEN o.bid_closed_at IS NULL
            OR o.winning_bid_amount IS NULL
            OR o.winning_bid_amount = OLD.amount
          THEN next_bid_amount
          ELSE o.winning_bid_amount
        END,
        winning_bidder_name = CASE
          WHEN o.bid_closed_at IS NOT NULL
            AND (o.winning_bid_amount IS NULL OR o.winning_bid_amount = OLD.amount)
          THEN next_bidder_name
          ELSE o.winning_bidder_name
        END
    WHERE o.id = OLD.offering_id;

  RETURN OLD;
END;
$$;

UPDATE public.offerings o
  SET amount = CASE
        WHEN o.amount = o.winning_bid_amount
          AND NOT EXISTS (
            SELECT 1
            FROM public.offering_bids b
            WHERE b.offering_id = o.id
              AND b.status <> 'REJECTED'
              AND b.amount = o.winning_bid_amount
          )
        THEN (
          SELECT b.amount
          FROM public.offering_bids b
          WHERE b.offering_id = o.id
            AND b.status <> 'REJECTED'
          ORDER BY b.amount DESC, b.created_at ASC
          LIMIT 1
        )
        ELSE o.amount
      END,
      winning_bid_amount = (
        SELECT b.amount
        FROM public.offering_bids b
        WHERE b.offering_id = o.id
          AND b.status <> 'REJECTED'
        ORDER BY b.amount DESC, b.created_at ASC
        LIMIT 1
      ),
      winning_bidder_name = CASE
        WHEN o.bid_closed_at IS NOT NULL
          AND (
            o.winning_bid_amount IS NULL
            OR NOT EXISTS (
              SELECT 1
              FROM public.offering_bids b
              WHERE b.offering_id = o.id
                AND b.status <> 'REJECTED'
                AND b.amount = o.winning_bid_amount
            )
          )
        THEN (
          SELECT b.bidder_name
          FROM public.offering_bids b
          WHERE b.offering_id = o.id
            AND b.status <> 'REJECTED'
          ORDER BY b.amount DESC, b.created_at ASC
          LIMIT 1
        )
        ELSE o.winning_bidder_name
      END
  WHERE o.bid_closed_at IS NULL
    OR o.winning_bid_amount IS NULL
    OR NOT EXISTS (
      SELECT 1
      FROM public.offering_bids b
      WHERE b.offering_id = o.id
        AND b.status <> 'REJECTED'
        AND b.amount = o.winning_bid_amount
    );

CREATE OR REPLACE FUNCTION public.call_offering_bid(p_offering_id UUID, p_winner_name TEXT DEFAULT NULL)
RETURNS INTEGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
  updated_call_count SMALLINT;
BEGIN
  IF NOT public.is_staff() THEN
    RAISE EXCEPTION 'Staff access required';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM public.offerings o
    WHERE o.id = p_offering_id
      AND o.bid_call_count = 2
  ) AND NULLIF(BTRIM(p_winner_name), '') IS NULL THEN
    RAISE EXCEPTION 'Winner name is required on the third call';
  END IF;

  UPDATE public.offerings o
    SET bid_call_count = o.bid_call_count + 1,
        status = CASE WHEN o.bid_call_count = 2 THEN 'COMPLETED' ELSE o.status END,
        bid_closed_at = CASE WHEN o.bid_call_count = 2 THEN NOW() ELSE o.bid_closed_at END,
        winning_bidder_name = CASE
          WHEN o.bid_call_count = 2 THEN BTRIM(p_winner_name)
          ELSE o.winning_bidder_name
        END,
        winning_bid_amount = CASE
          WHEN o.bid_call_count = 2 THEN (
            SELECT b.amount
            FROM public.offering_bids b
            WHERE b.offering_id = p_offering_id
              AND b.status <> 'REJECTED'
            ORDER BY b.amount DESC, b.created_at ASC
            LIMIT 1
          )
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

  RETURN updated_call_count;
END;
$$;

NOTIFY pgrst, 'reload schema';