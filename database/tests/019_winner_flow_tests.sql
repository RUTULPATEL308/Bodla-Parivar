-- ==============================================================================
-- Tests: 019_winner_flow_tests.sql
-- Description: Verification script for migration 019 inside a BEGIN ... ROLLBACK block
-- ==============================================================================

BEGIN;

DO $$
DECLARE
  v_server_time TIMESTAMPTZ;
  v_offering_id UUID := gen_random_uuid();
  v_offering_inperson_id UUID := gen_random_uuid();
  v_winner_auth_id UUID := gen_random_uuid();
  v_winner_profile_id UUID := gen_random_uuid();
  v_other_auth_id UUID := gen_random_uuid();
  v_other_profile_id UUID := gen_random_uuid();
  v_staff_auth_id UUID := gen_random_uuid();
  v_staff_profile_id UUID := gen_random_uuid();
  v_admin_role_id UUID;
  v_bid_id UUID := gen_random_uuid();
  v_inperson_bid_id UUID := gen_random_uuid();
  v_call_count INT;
  v_status TEXT;
  v_deadline TIMESTAMPTZ;
  v_display_name TEXT;
  v_private_record RECORD;
  v_audit_record RECORD;
BEGIN
  RAISE NOTICE '--- Starting 019 Winner Flow Tests ---';

  -- 1. Test get_server_time()
  v_server_time := public.get_server_time();
  ASSERT v_server_time IS NOT NULL, 'get_server_time() returned NULL';
  RAISE NOTICE 'Test 1 Passed: get_server_time() works: %', v_server_time;

  -- Setup roles & test users
  SELECT id INTO v_admin_role_id FROM public.roles WHERE name = 'ADMIN';
  IF v_admin_role_id IS NULL THEN
    INSERT INTO public.roles (name, description) VALUES ('ADMIN', 'Admin') RETURNING id INTO v_admin_role_id;
  END IF;

  -- Create profiles
  INSERT INTO public.profiles (id, auth_user_id, full_name, preferred_language, status)
  VALUES
    (v_winner_profile_id, v_winner_auth_id, 'Original Winner Full Name', 'gu', 'ACTIVE'),
    (v_other_profile_id, v_other_auth_id, 'Other Resident', 'gu', 'ACTIVE'),
    (v_staff_profile_id, v_staff_auth_id, 'Staff Admin Member', 'gu', 'ACTIVE');

  INSERT INTO public.user_roles (user_id, role_id)
  VALUES (v_staff_profile_id, v_admin_role_id);

  -- 2. Create Active Offering
  INSERT INTO public.offerings (
    id, title_gu, title_en, status, amount, bid_call_count
  ) VALUES (
    v_offering_id, 'ટેસ્ટ ચઢાવો', 'Test Offering', 'ACTIVE', 100, 0
  );

  -- Create Bid with profile
  INSERT INTO public.offering_bids (
    id, offering_id, bidder_name, bidder_profile_id, amount, status
  ) VALUES (
    v_bid_id, v_offering_id, 'Registered Bidder', v_winner_profile_id, 500, 'APPROVED'
  );

  -- Impersonate Staff to call bids
  EXECUTE format('SET LOCAL "request.jwt.claim.sub" = %L', v_staff_auth_id::text);

  -- First call
  v_call_count := public.call_offering_bid(v_offering_id, 'Ignored Name');
  ASSERT v_call_count = 1, 'First call expected 1';

  -- Second call
  v_call_count := public.call_offering_bid(v_offering_id, 'Ignored Name');
  ASSERT v_call_count = 2, 'Second call expected 2';

  -- Third call (transition to COMPLETED and starts winner flow)
  v_call_count := public.call_offering_bid(v_offering_id, 'Ignored Name');
  ASSERT v_call_count = 3, 'Third call expected 3';

  SELECT status, winner_name_status, winner_name_deadline_at, winning_bid_amount
    INTO v_status, v_status, v_deadline, v_display_name
    FROM public.offerings WHERE id = v_offering_id;

  ASSERT v_status = 'AWAITING_WINNER', format('Expected AWAITING_WINNER but got %s', v_status);
  ASSERT v_deadline > NOW(), 'Deadline should be in future';

  -- Check private record snapshot
  SELECT * INTO v_private_record FROM public.offering_winner_private WHERE offering_id = v_offering_id;
  ASSERT v_private_record.winner_profile_id = v_winner_profile_id, 'Winner profile id mismatch';
  ASSERT v_private_record.profile_full_name = 'Original Winner Full Name', 'Snapshot profile name mismatch';
  ASSERT v_private_record.bidder_name = 'Registered Bidder', 'Bidder name mismatch';
  RAISE NOTICE 'Test 2 Passed: call_offering_bid 3rd call initiates AWAITING_WINNER with deadline & private snapshot';

  -- 3. Impersonate Other User - Should fail to submit winner display name
  EXECUTE format('SET LOCAL "request.jwt.claim.sub" = %L', v_other_auth_id::text);
  BEGIN
    PERFORM public.submit_winner_display_name(v_offering_id, 'Unauthorized Name');
    RAISE EXCEPTION 'Other user should not be able to submit display name!';
  EXCEPTION WHEN OTHERS THEN
    IF SQLERRM ~ 'Only the winning bidder can set their display name' THEN
      RAISE NOTICE 'Test 3 Passed: Non-winner correctly rejected';
    ELSE
      RAISE EXCEPTION 'Unexpected error when testing other user: %', SQLERRM;
    END IF;
  END;

  -- 4. Impersonate Winner - Validation tests
  EXECUTE format('SET LOCAL "request.jwt.claim.sub" = %L', v_winner_auth_id::text);

  -- 4a. Short name
  BEGIN
    PERFORM public.submit_winner_display_name(v_offering_id, 'A');
    RAISE EXCEPTION 'Short name should be rejected!';
  EXCEPTION WHEN OTHERS THEN
    IF SQLERRM ~ 'Display name must be between 2 and 60 characters' THEN
      RAISE NOTICE 'Test 4a Passed: Short name rejected';
    ELSE
      RAISE EXCEPTION 'Unexpected error: %', SQLERRM;
    END IF;
  END;

  -- 4b. Valid name with inner whitespace collapse
  PERFORM public.submit_winner_display_name(v_offering_id, '   રતુલભાઈ    પટેલ   ');

  SELECT winner_name_status, winner_display_name INTO v_status, v_display_name
    FROM public.offerings WHERE id = v_offering_id;
  ASSERT v_status = 'CONFIRMED', format('Expected CONFIRMED, got %s', v_status);
  ASSERT v_display_name = 'રતુલભાઈ પટેલ', format('Whitespace not collapsed properly: %s', v_display_name);

  -- Check audit log
  SELECT * INTO v_audit_record FROM public.audit_logs
    WHERE entity_id = v_offering_id AND action = 'WINNER_NAME_SET';
  ASSERT v_audit_record.id IS NOT NULL, 'Audit log was not inserted';
  ASSERT (v_audit_record.new_data->>'set_by') = 'WINNER', 'Audit set_by mismatch';
  ASSERT NOT (v_audit_record.new_data ? 'profile_full_name'), 'Audit data leaked profile full name!';
  RAISE NOTICE 'Test 4b Passed: Winner submission, whitespace cleanup, and audit logging verified';

  -- 5. Test admin-provided name for an in-person bidder is published immediately
  INSERT INTO public.offerings (
    id, title_gu, title_en, status, amount, bid_call_count
  ) VALUES (
    v_offering_inperson_id, 'ઈન-પર્સન ચઢાવો', 'In Person Offering', 'ACTIVE', 100, 2
  );

  INSERT INTO public.offering_bids (
    id, offering_id, bidder_name, bidder_profile_id, amount, status
  ) VALUES (
    v_inperson_bid_id, v_offering_inperson_id, 'Offline Cash Bidder', NULL, 1000, 'APPROVED'
  );

  -- Staff completes call
  EXECUTE format('SET LOCAL "request.jwt.claim.sub" = %L', v_staff_auth_id::text);
  PERFORM public.call_offering_bid(v_offering_inperson_id, 'શ્રી રમેશભાઈ પટેલ પરિવાર');

  SELECT winner_name_status, winner_display_name INTO v_status, v_display_name
    FROM public.offerings WHERE id = v_offering_inperson_id;
  ASSERT v_status = 'CONFIRMED' AND v_display_name = 'શ્રી રમેશભાઈ પટેલ પરિવાર', 'Admin-entered name was not published';
  RAISE NOTICE 'Test 5 Passed: Admin-entered in-person winner name is immediately published';

  -- 6. Trigger check: Delete winning bid on closed offering
  DELETE FROM public.offering_bids WHERE id = v_inperson_bid_id;

  SELECT winner_name_status, winner_display_name INTO v_status, v_display_name
    FROM public.offerings WHERE id = v_offering_inperson_id;
  ASSERT v_status = 'NOT_STARTED' AND v_display_name IS NULL, 'Trigger failed to reset offering when all bids deleted';
  ASSERT NOT EXISTS (SELECT 1 FROM public.offering_winner_private WHERE offering_id = v_offering_inperson_id), 'Private row should be deleted';
  RAISE NOTICE 'Test 6 Passed: Trigger resets offering and purges private record when bids deleted';

  RAISE NOTICE '--- All 019 Winner Flow Tests Passed Successfully ---';
END;
$$;

ROLLBACK;
