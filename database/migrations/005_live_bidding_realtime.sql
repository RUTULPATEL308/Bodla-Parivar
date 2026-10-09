-- ==============================================================================
-- Migration: 005_live_bidding_realtime.sql
-- Description: Enable live bidding infrastructure - indexes, realtime, RLS
-- ==============================================================================

-- Performance index on offering_id for fast bid lookups per offering
CREATE INDEX IF NOT EXISTS idx_offering_bids_offering_id ON offering_bids(offering_id);
CREATE INDEX IF NOT EXISTS idx_offering_bids_created_at ON offering_bids(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_offering_bids_status ON offering_bids(status);

-- Ensure the offerings table is included in realtime publication
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_publication_tables
    WHERE pubname = 'supabase_realtime'
    AND schemaname = 'public'
    AND tablename = 'offerings'
  ) THEN
    ALTER PUBLICATION supabase_realtime ADD TABLE offerings;
  END IF;
END $$ LANGUAGE plpgsql;

-- Ensure the offering_bids table is in realtime publication (idempotent)
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_publication_tables
    WHERE pubname = 'supabase_realtime'
    AND schemaname = 'public'
    AND tablename = 'offering_bids'
  ) THEN
    ALTER PUBLICATION supabase_realtime ADD TABLE offering_bids;
  END IF;
END $$ LANGUAGE plpgsql;

-- Add updated_at auto-trigger for offering_bids
CREATE OR REPLACE FUNCTION update_offering_bids_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_offering_bids_updated_at ON offering_bids;
CREATE TRIGGER trg_offering_bids_updated_at
  BEFORE UPDATE ON offering_bids
  FOR EACH ROW EXECUTE FUNCTION update_offering_bids_updated_at();

-- RLS: Anyone (anon) can place bids from mobile app
DROP POLICY IF EXISTS "Anon can place bids" ON offering_bids;
CREATE POLICY "Anon can place bids" ON offering_bids
  FOR INSERT TO anon
  WITH CHECK (true);

DROP POLICY IF EXISTS "Anyone can view bids" ON offering_bids;
CREATE POLICY "Anyone can view bids" ON offering_bids
  FOR SELECT TO anon, authenticated
  USING (true);

-- Admin can approve/reject bids
DROP POLICY IF EXISTS "Authenticated can manage bids" ON offering_bids;
CREATE POLICY "Authenticated can manage bids" ON offering_bids
  FOR ALL TO authenticated
  USING (true)
  WITH CHECK (true);
