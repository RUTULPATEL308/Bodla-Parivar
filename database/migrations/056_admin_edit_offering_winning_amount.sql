-- ==============================================================================
-- Migration: 056_admin_edit_offering_winning_amount.sql
-- Project: બોદલા પરિવાર (Bodla Parivar)
-- Description: Allow admin to edit winning bid amount directly when bidding is closed
-- ==============================================================================

-- RPC function: admin_edit_offering_winning_amount
-- Allows staff/admin to edit the winning_bid_amount directly on the offerings table
-- Only when bidding is closed (status = 'COMPLETED')
CREATE OR REPLACE FUNCTION public.admin_edit_offering_winning_amount(
    p_offering_id UUID,
    p_new_amount NUMERIC(12, 2)
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_offering_status TEXT;
    v_is_staff BOOLEAN;
    v_current_amount NUMERIC(12, 2);
BEGIN
    -- Verify the offering exists and is in COMPLETED status
    SELECT o.status, o.winning_bid_amount
    INTO v_offering_status, v_current_amount
    FROM public.offerings o
    WHERE o.id = p_offering_id;

    IF NOT FOUND THEN
        RETURN jsonb_build_object('success', false, 'error', 'Offering not found');
    END IF;

    IF v_offering_status != 'COMPLETED' THEN
        RETURN jsonb_build_object('success', false, 'error', 'Can only edit winning amount when bidding is closed');
    END IF;

    -- Verify the user is staff/admin
    SELECT public.is_staff()
    INTO v_is_staff;

    IF NOT v_is_staff THEN
        RETURN jsonb_build_object('success', false, 'error', 'Staff access required');
    END IF;

    -- Validate amount is positive
    IF p_new_amount IS NULL OR p_new_amount <= 0 THEN
        RETURN jsonb_build_object('success', false, 'error', 'Amount must be positive');
    END IF;

    -- Update the winning bid amount
    UPDATE public.offerings
    SET winning_bid_amount = p_new_amount,
        updated_at = NOW()
    WHERE id = p_offering_id;

    RETURN jsonb_build_object('success', true, 'message', 'Winning bid amount updated successfully', 'new_amount', p_new_amount);
END;
$$;

REVOKE ALL ON FUNCTION public.admin_edit_offering_winning_amount(UUID, NUMERIC(12,2)) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.admin_edit_offering_winning_amount(UUID, NUMERIC(12,2)) TO authenticated;

-- Notify PostgREST to reload schema
NOTIFY pgrst, 'reload schema';

-- Add comment for documentation
COMMENT ON FUNCTION public.admin_edit_offering_winning_amount(UUID, NUMERIC(12,2)) IS
'Allows staff to edit the winning bid amount directly on the offerings table. Only when bidding status is COMPLETED.';
