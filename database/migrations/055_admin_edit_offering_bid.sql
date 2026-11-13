-- ==============================================================================
-- Migration: 055_admin_edit_offering_bid.sql
-- Project: બોદલા પરિવાર (Bodla Parivar)
-- Description: Allow admin to edit bidder name and amount on closed bids
-- ==============================================================================

-- RPC function: admin_edit_offering_bid
-- Allows staff/admin to edit bidder_name and amount on a bid
-- Only when the offering's bidding is closed (status = 'COMPLETED')
CREATE OR REPLACE FUNCTION public.admin_edit_offering_bid(
    p_bid_id UUID,
    p_new_bidder_name TEXT,
    p_new_amount NUMERIC(12, 2)
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_offering_id UUID;
    v_offering_status TEXT;
    v_is_staff BOOLEAN;
BEGIN
    -- Verify the bid exists and get its offering_id
    SELECT ob.offering_id
    INTO v_offering_id
    FROM public.offering_bids ob
    WHERE ob.id = p_bid_id;

    IF NOT FOUND THEN
        RETURN jsonb_build_object('success', false, 'error', 'Bid not found');
    END IF;

    -- Verify the offering bidding is closed (status = 'COMPLETED')
    SELECT o.status
    INTO v_offering_status
    FROM public.offerings o
    WHERE o.id = v_offering_id;

    IF NOT FOUND THEN
        RETURN jsonb_build_object('success', false, 'error', 'Offering not found');
    END IF;

    IF v_offering_status != 'COMPLETED' THEN
        RETURN jsonb_build_object('success', false, 'error', 'Can only edit bids when bidding is closed');
    END IF;

    -- Verify the user is staff/admin
    SELECT public.is_staff()
    INTO v_is_staff;

    IF NOT v_is_staff THEN
        RETURN jsonb_build_object('success', false, 'error', 'Staff access required');
    END IF;

    -- Validate bidder name is not blank
    IF p_new_bidder_name IS NULL OR trim(p_new_bidder_name) = '' THEN
        RETURN jsonb_build_object('success', false, 'error', 'Bidder name cannot be blank');
    END IF;

    -- Validate amount is positive
    IF p_new_amount IS NULL OR p_new_amount <= 0 THEN
        RETURN jsonb_build_object('success', false, 'error', 'Amount must be positive');
    END IF;

    -- Update the bid
    UPDATE public.offering_bids
    SET
        bidder_name = trim(p_new_bidder_name),
        amount = p_new_amount,
        updated_at = NOW()
    WHERE id = p_bid_id;

    -- If this bid was the winning bid, update the offering's winning_bid_amount
    IF EXISTS (
        SELECT 1 FROM public.offerings o
        WHERE o.id = v_offering_id
          AND o.bid_closed_at IS NOT NULL
          AND o.winning_bid_amount = (
            SELECT amount FROM public.offering_bids WHERE id = p_bid_id
          )
    ) THEN
        UPDATE public.offerings
        SET winning_bid_amount = p_new_amount,
            updated_at = NOW()
        WHERE id = v_offering_id;
    END IF;

    RETURN jsonb_build_object('success', true, 'message', 'Bid updated successfully');
END;
$$;

REVOKE ALL ON FUNCTION public.admin_edit_offering_bid(UUID, TEXT, NUMERIC(12,2)) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.admin_edit_offering_bid(UUID, TEXT, NUMERIC(12,2)) TO authenticated;

-- Notify PostgREST to reload schema
NOTIFY pgrst, 'reload schema';

-- Add comment for documentation
COMMENT ON FUNCTION public.admin_edit_offering_bid(UUID, TEXT, NUMERIC(12,2)) IS
'Allows staff to edit bidder name and amount on a closed bid. Only when offering status is COMPLETED.';
