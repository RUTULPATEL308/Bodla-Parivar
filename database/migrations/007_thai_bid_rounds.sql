ALTER TABLE public.offerings
  ADD COLUMN IF NOT EXISTS bid_call_count SMALLINT NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS winning_bidder_name TEXT,
  ADD COLUMN IF NOT EXISTS winning_bid_amount NUMERIC(12, 2),
  ADD COLUMN IF NOT EXISTS bid_closed_at TIMESTAMPTZ;

ALTER TABLE public.offering_bids
  ADD COLUMN IF NOT EXISTS bidder_profile_id UUID REFERENCES public.profiles(id) ON DELETE SET NULL;

CREATE OR REPLACE FUNCTION public.set_offering_bidder_profile()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
  profile_id UUID;
  profile_name TEXT;
  profile_phone TEXT;
BEGIN
  IF auth.uid() IS NULL THEN
    RAISE EXCEPTION 'Sign in before placing a bid';
  END IF;

  IF public.is_staff() THEN
    IF NULLIF(BTRIM(NEW.bidder_name), '') IS NULL THEN
      RAISE EXCEPTION 'Enter the in-person bidder name';
    END IF;
    NEW.bidder_profile_id := NULL;
    RETURN NEW;
  END IF;

  SELECT p.id, p.full_name, p.phone
    INTO profile_id, profile_name, profile_phone
    FROM public.profiles p
    WHERE p.auth_user_id = auth.uid();

  IF NOT FOUND THEN
    RAISE EXCEPTION 'A profile is required before placing a bid';
  END IF;

  NEW.bidder_profile_id := profile_id;
  NEW.bidder_name := profile_name;
  NEW.bidder_phone := profile_phone;
  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_set_offering_bidder_profile ON public.offering_bids;
CREATE TRIGGER trg_set_offering_bidder_profile
  BEFORE INSERT ON public.offering_bids
  FOR EACH ROW EXECUTE FUNCTION public.set_offering_bidder_profile();

CREATE OR REPLACE FUNCTION public.refresh_offering_winner_after_bid_delete()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
  UPDATE public.offerings o
    SET winning_bidder_name = (
          SELECT b.bidder_name FROM public.offering_bids b
          WHERE b.offering_id = OLD.offering_id
          ORDER BY b.amount DESC, b.created_at ASC
          LIMIT 1
        ),
        winning_bid_amount = (
          SELECT MAX(b.amount) FROM public.offering_bids b
          WHERE b.offering_id = OLD.offering_id
        )
    WHERE o.id = OLD.offering_id
      AND o.bid_closed_at IS NOT NULL
      AND o.winning_bid_amount = OLD.amount;

  RETURN OLD;
END;
$$;

DROP TRIGGER IF EXISTS trg_refresh_offering_winner_after_bid_delete ON public.offering_bids;
CREATE TRIGGER trg_refresh_offering_winner_after_bid_delete
  AFTER DELETE ON public.offering_bids
  FOR EACH ROW EXECUTE FUNCTION public.refresh_offering_winner_after_bid_delete();

DROP POLICY IF EXISTS "Anon can place bids" ON public.offering_bids;
DROP POLICY IF EXISTS "Allow all update offering_bids" ON public.offering_bids;
DROP POLICY IF EXISTS "Authenticated users can place bids" ON public.offering_bids;
CREATE POLICY "Authenticated users can place bids" ON public.offering_bids
  FOR INSERT TO authenticated
  WITH CHECK (
    EXISTS (
      SELECT 1 FROM public.offerings o
      WHERE o.id = offering_id
        AND o.status = 'ACTIVE'
        AND o.bid_closed_at IS NULL
        AND o.bid_call_count < 3
    )
  );

DROP POLICY IF EXISTS "Staff can manage bids" ON public.offering_bids;
DROP POLICY IF EXISTS "Staff can manage offering bids" ON public.offering_bids;
CREATE POLICY "Staff can manage offering bids" ON public.offering_bids
  FOR ALL TO authenticated
  USING (public.is_staff())
  WITH CHECK (public.is_staff());

DROP FUNCTION IF EXISTS public.call_offering_bid(UUID);
DROP FUNCTION IF EXISTS public.finalize_offering_bid(UUID, TEXT);

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
    SELECT 1 FROM public.offerings o
    WHERE o.id = p_offering_id AND o.bid_call_count = 2
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
            SELECT MAX(b.amount) FROM public.offering_bids b
            WHERE b.offering_id = p_offering_id
          )
          ELSE o.winning_bid_amount
        END
    WHERE o.id = p_offering_id
      AND o.status = 'ACTIVE'
      AND o.bid_call_count < 3
      AND EXISTS (
        SELECT 1 FROM public.offering_bids b
        WHERE b.offering_id = p_offering_id
      )
    RETURNING o.bid_call_count INTO updated_call_count;

  IF NOT FOUND THEN
    RAISE EXCEPTION 'Offering is closed or has no bids';
  END IF;

  RETURN updated_call_count;
END;
$$;

REVOKE ALL ON FUNCTION public.call_offering_bid(UUID, TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.call_offering_bid(UUID, TEXT) TO authenticated;