-- ==============================================================================
-- Migration: 019_winner_display_name.sql
-- Description: Server-side winner display name flow and notification payload
-- ==============================================================================

BEGIN;

-- 1. offerings: Add winner display name columns
ALTER TABLE public.offerings
  ADD COLUMN IF NOT EXISTS winner_name_status TEXT NOT NULL DEFAULT 'NOT_STARTED',
  ADD COLUMN IF NOT EXISTS winner_display_name TEXT,
  ADD COLUMN IF NOT EXISTS winner_name_deadline_at TIMESTAMPTZ;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
    WHERE conname = 'offerings_winner_name_status_check'
      AND conrelid = 'public.offerings'::regclass
  ) THEN
    ALTER TABLE public.offerings
      ADD CONSTRAINT offerings_winner_name_status_check
      CHECK (winner_name_status IN ('NOT_STARTED', 'AWAITING_WINNER', 'AWAITING_ADMIN', 'CONFIRMED'));
  END IF;
END;
$$;

-- 2. New table: public.offering_winner_private
CREATE TABLE IF NOT EXISTS public.offering_winner_private (
  offering_id UUID PRIMARY KEY REFERENCES public.offerings(id) ON DELETE CASCADE,
  winning_bid_id UUID REFERENCES public.offering_bids(id) ON DELETE SET NULL,
  winner_profile_id UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
  bidder_name TEXT,
  profile_full_name TEXT,
  display_name_set_by TEXT CHECK (display_name_set_by IN ('WINNER', 'ADMIN')),
  display_name_set_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_offering_winner_private_winner_profile
  ON public.offering_winner_private(winner_profile_id);

CREATE OR REPLACE FUNCTION public.update_updated_at_column()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_update_timestamp ON public.offering_winner_private;
CREATE TRIGGER trg_update_timestamp
  BEFORE UPDATE ON public.offering_winner_private
  FOR EACH ROW
  EXECUTE FUNCTION public.update_updated_at_column();

-- Enable RLS on offering_winner_private
ALTER TABLE public.offering_winner_private ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Staff can manage offering_winner_private" ON public.offering_winner_private;
CREATE POLICY "Staff can manage offering_winner_private"
  ON public.offering_winner_private
  FOR ALL
  TO authenticated
  USING (public.is_staff())
  WITH CHECK (public.is_staff());

DROP POLICY IF EXISTS "Winner can view own private record" ON public.offering_winner_private;
CREATE POLICY "Winner can view own private record"
  ON public.offering_winner_private
  FOR SELECT
  TO authenticated
  USING (
    winner_profile_id IS NOT NULL
    AND EXISTS (
      SELECT 1 FROM public.profiles p
      WHERE p.id = offering_winner_private.winner_profile_id
        AND p.auth_user_id = auth.uid()
    )
  );

-- 3. notifications: Add payload column
ALTER TABLE public.notifications
  ADD COLUMN IF NOT EXISTS payload JSONB NOT NULL DEFAULT '{}'::jsonb;

-- 4. public.get_server_time(): Clock sync helper
CREATE OR REPLACE FUNCTION public.get_server_time()
RETURNS TIMESTAMPTZ
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
  SELECT NOW();
$$;

REVOKE ALL ON FUNCTION public.get_server_time() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.get_server_time() TO anon, authenticated;

-- 5. public.start_winner_name_flow(UUID): Internal transition helper
CREATE OR REPLACE FUNCTION public.start_winner_name_flow(p_offering_id UUID)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
  v_top_bid RECORD;
  v_profile_name TEXT;
BEGIN
  -- Pick winning bid: highest amount, earliest created_at
  SELECT b.id, b.bidder_name, b.bidder_profile_id, b.amount
    INTO v_top_bid
    FROM public.offering_bids b
    WHERE b.offering_id = p_offering_id
      AND b.status <> 'REJECTED'
    ORDER BY b.amount DESC, b.created_at ASC
    LIMIT 1;

  IF v_top_bid.id IS NOT NULL THEN
    IF v_top_bid.bidder_profile_id IS NOT NULL THEN
      SELECT p.full_name
        INTO v_profile_name
        FROM public.profiles p
        WHERE p.id = v_top_bid.bidder_profile_id;
    ELSE
      v_profile_name := NULL;
    END IF;

    -- Snapshot into private winner table
    INSERT INTO public.offering_winner_private (
      offering_id,
      winning_bid_id,
      winner_profile_id,
      bidder_name,
      profile_full_name,
      display_name_set_by,
      display_name_set_at,
      updated_at
    )
    VALUES (
      p_offering_id,
      v_top_bid.id,
      v_top_bid.bidder_profile_id,
      v_top_bid.bidder_name,
      v_profile_name,
      NULL,
      NULL,
      NOW()
    )
    ON CONFLICT (offering_id) DO UPDATE
      SET winning_bid_id = EXCLUDED.winning_bid_id,
          winner_profile_id = EXCLUDED.winner_profile_id,
          bidder_name = EXCLUDED.bidder_name,
          profile_full_name = EXCLUDED.profile_full_name,
          display_name_set_by = NULL,
          display_name_set_at = NULL,
          updated_at = NOW();

    -- Update offerings status & deadline
    IF v_top_bid.bidder_profile_id IS NOT NULL THEN
      UPDATE public.offerings
        SET winner_name_status = 'AWAITING_WINNER',
            winner_name_deadline_at = NOW() + INTERVAL '2 minutes',
            winner_display_name = NULL
        WHERE id = p_offering_id;
    ELSE
      UPDATE public.offerings
        SET winner_name_status = 'AWAITING_ADMIN',
            winner_name_deadline_at = NULL,
            winner_display_name = NULL
        WHERE id = p_offering_id;
    END IF;
  ELSE
    -- No bids remain
    UPDATE public.offerings
      SET winner_name_status = 'NOT_STARTED',
          winner_name_deadline_at = NULL,
          winner_display_name = NULL
      WHERE id = p_offering_id;

    DELETE FROM public.offering_winner_private
      WHERE offering_id = p_offering_id;
  END IF;
END;
$$;

REVOKE ALL ON FUNCTION public.start_winner_name_flow(UUID) FROM PUBLIC;

-- 6. public.call_offering_bid(UUID, TEXT): Backward-compatible admin call
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

  -- Compute winning bid amount on 3rd call transition
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

  -- On third call: transition offering and initiate winner name flow
  IF updated_call_count = 3 THEN
    PERFORM public.start_winner_name_flow(p_offering_id);
  END IF;

  RETURN updated_call_count;
END;
$$;

REVOKE ALL ON FUNCTION public.call_offering_bid(UUID, TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.call_offering_bid(UUID, TEXT) TO authenticated;

-- 7. public.submit_winner_display_name(UUID, TEXT): Winner self-submission
CREATE OR REPLACE FUNCTION public.submit_winner_display_name(p_offering_id UUID, p_name TEXT)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
  v_offering RECORD;
  v_private RECORD;
  v_caller_profile_id UUID;
  v_cleaned TEXT;
BEGIN
  IF auth.uid() IS NULL THEN
    RAISE EXCEPTION 'Authentication required';
  END IF;

  SELECT p.id INTO v_caller_profile_id
    FROM public.profiles p
    WHERE p.auth_user_id = auth.uid();

  IF v_caller_profile_id IS NULL THEN
    RAISE EXCEPTION 'Caller profile not found';
  END IF;

  SELECT id, winner_name_status, winner_name_deadline_at, winner_display_name
    INTO v_offering
    FROM public.offerings
    WHERE id = p_offering_id;

  IF NOT FOUND THEN
    RAISE EXCEPTION 'Offering not found';
  END IF;

  IF v_offering.winner_name_status <> 'AWAITING_WINNER' THEN
    RAISE EXCEPTION 'Offering is not awaiting winner submission (current status: %)', v_offering.winner_name_status;
  END IF;

  IF NOW() > v_offering.winner_name_deadline_at THEN
    RAISE EXCEPTION 'Submission deadline has passed';
  END IF;

  SELECT offering_id, winner_profile_id
    INTO v_private
    FROM public.offering_winner_private
    WHERE offering_id = p_offering_id;

  IF NOT FOUND OR v_private.winner_profile_id IS NULL OR v_private.winner_profile_id <> v_caller_profile_id THEN
    RAISE EXCEPTION 'Only the winning bidder can set their display name';
  END IF;

  -- Validate & clean input
  v_cleaned := REGEXP_REPLACE(BTRIM(COALESCE(p_name, '')), '\s+', ' ', 'g');

  IF v_cleaned ~ '[\x00-\x1F\x7F]' THEN
    RAISE EXCEPTION 'Display name contains invalid characters';
  END IF;

  IF CHAR_LENGTH(v_cleaned) < 2 OR CHAR_LENGTH(v_cleaned) > 60 THEN
    RAISE EXCEPTION 'Display name must be between 2 and 60 characters';
  END IF;

  -- Update offerings
  UPDATE public.offerings
    SET winner_display_name = v_cleaned,
        winner_name_status = 'CONFIRMED'
    WHERE id = p_offering_id;

  -- Update offering_winner_private
  UPDATE public.offering_winner_private
    SET display_name_set_by = 'WINNER',
        display_name_set_at = NOW(),
        updated_at = NOW()
    WHERE offering_id = p_offering_id;

  -- Audit log entry
  INSERT INTO public.audit_logs (
    user_id,
    action,
    entity_type,
    entity_id,
    old_data,
    new_data
  ) VALUES (
    v_caller_profile_id,
    'WINNER_NAME_SET',
    'OFFERING',
    p_offering_id,
    jsonb_build_object(
      'winner_display_name', v_offering.winner_display_name,
      'status', v_offering.winner_name_status
    ),
    jsonb_build_object(
      'winner_display_name', v_cleaned,
      'status', 'CONFIRMED',
      'set_by', 'WINNER'
    )
  );
END;
$$;

REVOKE ALL ON FUNCTION public.submit_winner_display_name(UUID, TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.submit_winner_display_name(UUID, TEXT) TO authenticated;

-- 8. public.admin_set_winner_display_name(UUID, TEXT): Staff administrative override
CREATE OR REPLACE FUNCTION public.admin_set_winner_display_name(p_offering_id UUID, p_name TEXT)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
  v_offering RECORD;
  v_staff_profile_id UUID;
  v_cleaned TEXT;
BEGIN
  IF NOT public.is_staff() THEN
    RAISE EXCEPTION 'Staff access required';
  END IF;

  SELECT p.id INTO v_staff_profile_id
    FROM public.profiles p
    WHERE p.auth_user_id = auth.uid();

  SELECT id, winner_name_status, winner_name_deadline_at, winner_display_name
    INTO v_offering
    FROM public.offerings
    WHERE id = p_offering_id;

  IF NOT FOUND THEN
    RAISE EXCEPTION 'Offering not found';
  END IF;

  -- Allowed conditions: AWAITING_ADMIN, or expired AWAITING_WINNER, or CONFIRMED
  IF NOT (
    v_offering.winner_name_status = 'AWAITING_ADMIN'
    OR (v_offering.winner_name_status = 'AWAITING_WINNER' AND NOW() > v_offering.winner_name_deadline_at)
    OR v_offering.winner_name_status = 'CONFIRMED'
  ) THEN
    RAISE EXCEPTION 'Cannot set winner display name while offering is actively awaiting winner submission';
  END IF;

  -- Validate & clean input
  v_cleaned := REGEXP_REPLACE(BTRIM(COALESCE(p_name, '')), '\s+', ' ', 'g');

  IF v_cleaned ~ '[\x00-\x1F\x7F]' THEN
    RAISE EXCEPTION 'Display name contains invalid characters';
  END IF;

  IF CHAR_LENGTH(v_cleaned) < 2 OR CHAR_LENGTH(v_cleaned) > 60 THEN
    RAISE EXCEPTION 'Display name must be between 2 and 60 characters';
  END IF;

  -- Update offerings
  UPDATE public.offerings
    SET winner_display_name = v_cleaned,
        winner_name_status = 'CONFIRMED'
    WHERE id = p_offering_id;

  -- Update offering_winner_private
  UPDATE public.offering_winner_private
    SET display_name_set_by = 'ADMIN',
        display_name_set_at = NOW(),
        updated_at = NOW()
    WHERE offering_id = p_offering_id;

  -- Audit log entry
  INSERT INTO public.audit_logs (
    user_id,
    action,
    entity_type,
    entity_id,
    old_data,
    new_data
  ) VALUES (
    v_staff_profile_id,
    'WINNER_NAME_SET',
    'OFFERING',
    p_offering_id,
    jsonb_build_object(
      'winner_display_name', v_offering.winner_display_name,
      'status', v_offering.winner_name_status
    ),
    jsonb_build_object(
      'winner_display_name', v_cleaned,
      'status', 'CONFIRMED',
      'set_by', 'ADMIN'
    )
  );
END;
$$;

REVOKE ALL ON FUNCTION public.admin_set_winner_display_name(UUID, TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.admin_set_winner_display_name(UUID, TEXT) TO authenticated;

-- 9. Trigger: public.refresh_offering_winner_after_bid_delete()
CREATE OR REPLACE FUNCTION public.refresh_offering_winner_after_bid_delete()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
  next_bidder_name TEXT;
  next_bid_amount NUMERIC(12, 2);
  v_is_closed BOOLEAN;
  v_was_winning_bid BOOLEAN;
BEGIN
  -- Recompute highest active bid
  SELECT b.bidder_name, b.amount
    INTO next_bidder_name, next_bid_amount
    FROM public.offering_bids b
    WHERE b.offering_id = OLD.offering_id
      AND b.status <> 'REJECTED'
    ORDER BY b.amount DESC, b.created_at ASC
    LIMIT 1;

  -- Check if offering was closed
  SELECT (o.bid_closed_at IS NOT NULL OR o.status = 'COMPLETED')
    INTO v_is_closed
    FROM public.offerings o
    WHERE o.id = OLD.offering_id;

  -- Check if deleted bid was the winning bid
  SELECT EXISTS (
    SELECT 1 FROM public.offering_winner_private wp
    WHERE wp.offering_id = OLD.offering_id
      AND wp.winning_bid_id = OLD.id
  ) OR EXISTS (
    SELECT 1 FROM public.offerings o
    WHERE o.id = OLD.offering_id
      AND o.winning_bid_amount = OLD.amount
  ) INTO v_was_winning_bid;

  -- Maintain amounts and legacy column
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

  -- If a closed offering lost its winning bid, recalculate winner flow
  IF v_is_closed AND v_was_winning_bid THEN
    IF next_bid_amount IS NOT NULL THEN
      PERFORM public.start_winner_name_flow(OLD.offering_id);
    ELSE
      UPDATE public.offerings
        SET winner_name_status = 'NOT_STARTED',
            winner_display_name = NULL,
            winner_name_deadline_at = NULL
        WHERE id = OLD.offering_id;

      DELETE FROM public.offering_winner_private
        WHERE offering_id = OLD.offering_id;
    END IF;
  END IF;

  RETURN OLD;
END;
$$;

DROP TRIGGER IF EXISTS trg_refresh_offering_winner_after_bid_delete ON public.offering_bids;
CREATE TRIGGER trg_refresh_offering_winner_after_bid_delete
  AFTER DELETE ON public.offering_bids
  FOR EACH ROW
  EXECUTE FUNCTION public.refresh_offering_winner_after_bid_delete();

-- 10. Backfill: Already COMPLETED offerings with winning_bidder_name
UPDATE public.offerings o
  SET winner_name_status = 'CONFIRMED',
      winner_display_name = o.winning_bidder_name
  WHERE o.status = 'COMPLETED'
    AND o.winning_bidder_name IS NOT NULL
    AND (o.winner_name_status = 'NOT_STARTED' OR o.winner_name_status IS NULL);

INSERT INTO public.offering_winner_private (
  offering_id,
  winning_bid_id,
  winner_profile_id,
  bidder_name,
  profile_full_name,
  display_name_set_by,
  display_name_set_at,
  created_at,
  updated_at
)
SELECT
  o.id AS offering_id,
  top_bid.id AS winning_bid_id,
  top_bid.bidder_profile_id AS winner_profile_id,
  COALESCE(top_bid.bidder_name, o.winning_bidder_name) AS bidder_name,
  p.full_name AS profile_full_name,
  'ADMIN' AS display_name_set_by,
  COALESCE(o.bid_closed_at, NOW()) AS display_name_set_at,
  NOW(),
  NOW()
FROM public.offerings o
LEFT JOIN LATERAL (
  SELECT b.id, b.bidder_profile_id, b.bidder_name
  FROM public.offering_bids b
  WHERE b.offering_id = o.id
    AND b.status <> 'REJECTED'
  ORDER BY b.amount DESC, b.created_at ASC
  LIMIT 1
) top_bid ON true
LEFT JOIN public.profiles p ON p.id = top_bid.bidder_profile_id
WHERE o.status = 'COMPLETED'
  AND o.winning_bidder_name IS NOT NULL
ON CONFLICT (offering_id) DO NOTHING;

-- 11. Realtime Publication: Ensure offerings is in publication, but NOT offering_winner_private
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
END;
$$;

NOTIFY pgrst, 'reload schema';

COMMIT;
