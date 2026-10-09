-- ==============================================================================
-- Bodla Parivar: Enable Live Sync for Web Portal & Mobile App
-- Run this in your Supabase SQL Editor:
-- https://supabase.com/dashboard/project/kjoykvxrnyykdnzektoj/sql
-- ==============================================================================

-- 1. Enable Realtime Broadcasting on key tables
--    (IF NOT EXISTS is not supported by ALTER PUBLICATION, so we use DO blocks)
DO $$ BEGIN
  ALTER PUBLICATION supabase_realtime ADD TABLE offerings;
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
  ALTER PUBLICATION supabase_realtime ADD TABLE offering_bids;
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
  ALTER PUBLICATION supabase_realtime ADD TABLE notices;
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
  ALTER PUBLICATION supabase_realtime ADD TABLE events;
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
  ALTER PUBLICATION supabase_realtime ADD TABLE businesses;
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

-- 2. OFFERINGS: Allow Web Portal and Mobile App to read all active offerings
DROP POLICY IF EXISTS "Active offerings are viewable by all" ON offerings;
DROP POLICY IF EXISTS "Allow portal select offerings" ON offerings;
CREATE POLICY "Allow portal select offerings"
    ON offerings FOR SELECT
    TO anon, authenticated
  USING (status IN ('APPROVED', 'ACTIVE', 'COMPLETED'));

-- 3. OFFERINGS: Only staff can create or update offerings.
-- Remove legacy broad policies; staff access comes from the RLS migration.
DROP POLICY IF EXISTS "Users can submit offerings" ON offerings;
DROP POLICY IF EXISTS "Allow portal insert offerings" ON offerings;
DROP POLICY IF EXISTS "Allow portal update offerings" ON offerings;

-- 5. OFFERING_BIDS: Allow read, write, update for portal and mobile
ALTER TABLE offering_bids ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Allow all select offering_bids" ON offering_bids;
CREATE POLICY "Allow all select offering_bids"
    ON offering_bids FOR SELECT
    TO anon, authenticated
    USING (true);

DROP POLICY IF EXISTS "Allow all insert offering_bids" ON offering_bids;
DROP POLICY IF EXISTS "Anon can place bids" ON offering_bids;
DROP POLICY IF EXISTS "Authenticated users can place bids" ON offering_bids;
CREATE POLICY "Authenticated users can place bids"
  ON offering_bids FOR INSERT TO authenticated
  WITH CHECK (
    EXISTS (
      SELECT 1 FROM offerings o
      WHERE o.id = offering_id
        AND o.status = 'ACTIVE'
        AND o.bid_closed_at IS NULL
        AND o.bid_call_count < 3
    )
  );

DROP POLICY IF EXISTS "Allow all update offering_bids" ON offering_bids;
DROP POLICY IF EXISTS "Authenticated can manage bids" ON offering_bids;

-- 6. NOTICES: Allow read & write for portal and mobile
DROP POLICY IF EXISTS "Allow portal select notices" ON notices;
CREATE POLICY "Allow portal select notices"
    ON notices FOR SELECT
    TO anon, authenticated
    USING (true);

DROP POLICY IF EXISTS "Allow portal insert notices" ON notices;
CREATE POLICY "Allow portal insert notices"
    ON notices FOR INSERT
    TO anon, authenticated
    WITH CHECK (true);

-- 7. EVENTS: Allow read & write for portal and mobile
DROP POLICY IF EXISTS "Allow portal select events" ON events;
CREATE POLICY "Allow portal select events"
    ON events FOR SELECT
    TO anon, authenticated
    USING (true);

DROP POLICY IF EXISTS "Allow portal insert events" ON events;
CREATE POLICY "Allow portal insert events"
    ON events FOR INSERT
    TO anon, authenticated
    WITH CHECK (true);
