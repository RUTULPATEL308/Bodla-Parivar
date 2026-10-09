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
              AND b.amount = o.winning_bid_amount
          )
        THEN (
          SELECT b.amount
          FROM public.offering_bids b
          WHERE b.offering_id = o.id
          ORDER BY b.amount DESC, b.created_at ASC
          LIMIT 1
        )
        ELSE o.amount
      END,
      winning_bid_amount = (
        SELECT b.amount
        FROM public.offering_bids b
        WHERE b.offering_id = o.id
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
                AND b.amount = o.winning_bid_amount
            )
          )
        THEN (
          SELECT b.bidder_name
          FROM public.offering_bids b
          WHERE b.offering_id = o.id
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
        AND b.amount = o.winning_bid_amount
    );

NOTIFY pgrst, 'reload schema';