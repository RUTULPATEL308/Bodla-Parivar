-- ==============================================================================
-- Migration: 002_offering_bids.sql
-- Description: Add offering bids table and policies
-- ==============================================================================

CREATE TABLE IF NOT EXISTS offering_bids (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    offering_id UUID NOT NULL REFERENCES offerings(id) ON DELETE CASCADE,
    bidder_name TEXT NOT NULL,
    bidder_phone TEXT,
    amount NUMERIC(12, 2) NOT NULL,
    status TEXT NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- RLS
ALTER TABLE offering_bids ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Allow all select offering_bids" ON offering_bids;
CREATE POLICY "Allow all select offering_bids" ON offering_bids FOR SELECT TO anon, authenticated USING (true);

DROP POLICY IF EXISTS "Allow all insert offering_bids" ON offering_bids;
CREATE POLICY "Allow all insert offering_bids" ON offering_bids FOR INSERT TO anon, authenticated WITH CHECK (true);

DROP POLICY IF EXISTS "Allow all update offering_bids" ON offering_bids;
CREATE POLICY "Allow all update offering_bids" ON offering_bids FOR UPDATE TO anon, authenticated USING (true) WITH CHECK (true);

-- Enable Realtime
ALTER PUBLICATION supabase_realtime ADD TABLE offering_bids;
