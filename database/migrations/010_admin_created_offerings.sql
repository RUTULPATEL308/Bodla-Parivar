-- Offerings are created/managed by staff; signed-in residents may contribute bids.
ALTER TABLE public.offerings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.offering_bids ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users can submit offerings" ON public.offerings;
DROP POLICY IF EXISTS "Allow portal insert offerings" ON public.offerings;
DROP POLICY IF EXISTS "Allow portal update offerings" ON public.offerings;

DROP POLICY IF EXISTS "Allow all insert offering_bids" ON public.offering_bids;
DROP POLICY IF EXISTS "Anon can place bids" ON public.offering_bids;
DROP POLICY IF EXISTS "Authenticated users can place bids" ON public.offering_bids;
DROP POLICY IF EXISTS "Allow all update offering_bids" ON public.offering_bids;
DROP POLICY IF EXISTS "Authenticated can manage bids" ON public.offering_bids;

CREATE POLICY "Authenticated users can place bids"
    ON public.offering_bids FOR INSERT
    TO authenticated
    WITH CHECK (
        EXISTS (
            SELECT 1
            FROM public.offerings o
            WHERE o.id = offering_id
              AND o.status = 'ACTIVE'
              AND o.bid_closed_at IS NULL
              AND o.bid_call_count < 3
        )
    );