-- Optional module access for individual members. Existing profiles start with
-- no extra grants and retain the standard USER experience.
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'profiles' AND column_name = 'status') THEN
    ALTER TABLE public.profiles ADD COLUMN status TEXT NOT NULL DEFAULT 'ACTIVE';
  END IF;
END;
$$;
CREATE TABLE IF NOT EXISTS public.profile_feature_permissions (
    profile_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    feature_key TEXT NOT NULL CHECK (feature_key IN (
        'members', 'notices', 'offerings', 'navratri', 'complaints',
        'emergency_contacts', 'village_history', 'events', 'directory',
        'gallery', 'notifications'
    )),
    granted_by UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    granted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (profile_id, feature_key)
);

GRANT SELECT, INSERT, UPDATE, DELETE ON public.profile_feature_permissions TO authenticated;
ALTER TABLE public.profile_feature_permissions ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Members can read own feature permissions" ON public.profile_feature_permissions;
DROP POLICY IF EXISTS "Admins manage profile feature permissions" ON public.profile_feature_permissions;
CREATE POLICY "Members can read own feature permissions"
    ON public.profile_feature_permissions FOR SELECT TO authenticated
    USING (profile_id IN (SELECT id FROM public.profiles WHERE auth_user_id = auth.uid()));

DROP POLICY IF EXISTS "Admins manage profile feature permissions" ON public.profile_feature_permissions;
CREATE POLICY "Admins manage profile feature permissions"
    ON public.profile_feature_permissions FOR ALL TO authenticated
    USING (public.is_admin())
    WITH CHECK (public.is_admin());

CREATE OR REPLACE FUNCTION public.current_user_feature_permissions()
RETURNS TEXT[]
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT COALESCE(array_agg(pfp.feature_key ORDER BY pfp.feature_key), ARRAY[]::TEXT[])
    FROM public.profile_feature_permissions pfp
    JOIN public.profiles p ON p.id = pfp.profile_id
    WHERE p.auth_user_id = auth.uid() AND COALESCE(p.status, 'ACTIVE') = 'ACTIVE';
$$;

REVOKE ALL ON FUNCTION public.current_user_feature_permissions() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.current_user_feature_permissions() TO authenticated;

-- Feature-specific role helper used by RLS policies and RPCs. This deliberately
-- does not make a feature grant equivalent to ADMIN/SUPER_ADMIN.
CREATE OR REPLACE FUNCTION public.has_current_user_feature(target_feature TEXT)
RETURNS BOOLEAN
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT EXISTS (
        SELECT 1
        FROM public.profile_feature_permissions pfp
        JOIN public.profiles p ON p.id = pfp.profile_id
        WHERE p.auth_user_id = auth.uid()
          AND COALESCE(p.status, 'ACTIVE') = 'ACTIVE'
          AND pfp.feature_key = target_feature
    );
$$;

REVOKE ALL ON FUNCTION public.has_current_user_feature(TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.has_current_user_feature(TEXT) TO authenticated;

CREATE OR REPLACE FUNCTION public.is_staff()
RETURNS BOOLEAN
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT EXISTS (
        SELECT 1
        FROM public.user_roles ur
        JOIN public.roles r ON r.id = ur.role_id
        JOIN public.profiles p ON p.id = ur.user_id
        WHERE p.auth_user_id = auth.uid()
          AND r.name IN ('ADMIN', 'SUPER_ADMIN', 'MODERATOR', 'CONTENT_MANAGER')
    );
$$;

REVOKE ALL ON FUNCTION public.is_staff() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.is_staff() TO authenticated;

CREATE OR REPLACE FUNCTION public.is_admin()
RETURNS BOOLEAN
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT EXISTS (
        SELECT 1
        FROM public.user_roles ur
        JOIN public.roles r ON r.id = ur.role_id
        JOIN public.profiles p ON p.id = ur.user_id
        WHERE p.auth_user_id = auth.uid()
          AND r.name IN ('ADMIN', 'SUPER_ADMIN')
    );
$$;

REVOKE ALL ON FUNCTION public.is_admin() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.is_admin() TO authenticated;

CREATE OR REPLACE FUNCTION public.can_manage_feature(target_feature TEXT)
RETURNS BOOLEAN
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT public.is_staff() OR public.has_current_user_feature(target_feature);
$$;
REVOKE ALL ON FUNCTION public.can_manage_feature(TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.can_manage_feature(TEXT) TO authenticated;

DROP POLICY IF EXISTS "Authenticated can insert notifications" ON public.notifications;
DROP POLICY IF EXISTS "Anon can insert notifications" ON public.notifications;
DROP POLICY IF EXISTS "Anyone can view notifications" ON public.notifications;
CREATE POLICY "Authenticated can insert notifications" ON public.notifications FOR INSERT TO authenticated
    WITH CHECK (public.is_staff() OR public.has_current_user_feature('notifications'));
CREATE POLICY "Anyone can view notifications" ON public.notifications FOR SELECT TO anon, authenticated
    USING (true);

CREATE OR REPLACE FUNCTION public.set_notice_status(p_notice_id UUID, p_status TEXT)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
  IF NOT public.can_manage_feature('notices') THEN RAISE EXCEPTION 'Notice feature access required'; END IF;
  IF p_status NOT IN ('DRAFT', 'PENDING', 'PUBLISHED', 'EXPIRED', 'ARCHIVED') THEN RAISE EXCEPTION 'Invalid notice status'; END IF;
  UPDATE public.notices SET status = p_status WHERE id = p_notice_id;
END;
$$;

-- Preserve active/public reads while granting the selected members access to
-- private management rows for their module.
DROP POLICY IF EXISTS "Staff can manage notices" ON public.notices;
CREATE POLICY "Staff can manage notices" ON public.notices FOR ALL TO authenticated
    USING (public.is_staff() OR public.has_current_user_feature('notices'))
    WITH CHECK (public.is_staff() OR public.has_current_user_feature('notices'));

DROP POLICY IF EXISTS "Staff manage Navratri options" ON public.navratri_options;
DROP POLICY IF EXISTS "Public can read active Navratri options" ON public.navratri_options;
DROP POLICY IF EXISTS "Active Navratri options are public" ON public.navratri_options;
CREATE POLICY "Staff manage Navratri options" ON public.navratri_options FOR ALL TO authenticated
    USING (public.is_staff() OR public.has_current_user_feature('navratri'))
    WITH CHECK (public.is_staff() OR public.has_current_user_feature('navratri'));
CREATE POLICY "Residents can read active Navratri options" ON public.navratri_options FOR SELECT TO anon, authenticated USING (is_active = TRUE);

DROP POLICY IF EXISTS "Staff manage Navratri bookings" ON public.navratri_bookings;
DROP POLICY IF EXISTS "Staff view all Navratri bookings" ON public.navratri_bookings;
DROP POLICY IF EXISTS "Residents can create Navratri bookings" ON public.navratri_bookings;
DROP POLICY IF EXISTS "Public can submit cash Navratri bookings" ON public.navratri_bookings;
CREATE POLICY "Staff manage Navratri bookings" ON public.navratri_bookings FOR ALL TO authenticated
    USING (public.is_staff() OR public.has_current_user_feature('navratri'))
    WITH CHECK (public.is_staff() OR public.has_current_user_feature('navratri'));
CREATE POLICY "Residents can create Navratri bookings" ON public.navratri_bookings FOR INSERT TO anon, authenticated
    WITH CHECK (
        payment_method = 'CASH'
        AND payment_status = 'PENDING'
        AND status = 'PENDING'
        AND (user_id IS NULL OR user_id = auth.uid())
        AND EXISTS (SELECT 1 FROM public.navratri_options option
            WHERE option.id = option_id AND option.option_type = 'BOOKING' AND option.is_active = TRUE)
    );

DROP POLICY IF EXISTS "Staff can manage emergency contacts" ON public.emergency_contacts;
DROP POLICY IF EXISTS "Public read active emergency contacts" ON public.emergency_contacts;
DROP POLICY IF EXISTS "Allow public read emergency contacts" ON public.emergency_contacts;
DROP POLICY IF EXISTS "Active emergency contacts are viewable by all" ON public.emergency_contacts;
CREATE POLICY "Staff can manage emergency contacts" ON public.emergency_contacts FOR ALL TO authenticated
    USING (public.is_staff() OR public.has_current_user_feature('emergency_contacts'))
    WITH CHECK (public.is_staff() OR public.has_current_user_feature('emergency_contacts'));
GRANT SELECT ON public.emergency_contacts TO anon, authenticated;
GRANT INSERT, UPDATE, DELETE ON public.emergency_contacts TO authenticated;

DROP POLICY IF EXISTS "Staff can manage bids" ON public.offering_bids;
DROP POLICY IF EXISTS "Authenticated can manage bids" ON public.offering_bids;
DROP POLICY IF EXISTS "Staff can manage offering bids" ON public.offering_bids;
DROP POLICY IF EXISTS "Allow all select offering_bids" ON public.offering_bids;
DROP POLICY IF EXISTS "Allow all insert offering_bids" ON public.offering_bids;
DROP POLICY IF EXISTS "Allow all update offering_bids" ON public.offering_bids;
DROP POLICY IF EXISTS "Anyone can view bids" ON public.offering_bids;
DROP POLICY IF EXISTS "Authenticated users can place bids" ON public.offering_bids;
CREATE POLICY "Residents can read offering bids" ON public.offering_bids FOR SELECT TO authenticated USING (
    public.is_staff() OR public.has_current_user_feature('offerings') OR bidder_profile_id IN (
        SELECT id FROM public.profiles WHERE auth_user_id = auth.uid()
    )
);
CREATE POLICY "Residents can place bids" ON public.offering_bids FOR INSERT TO authenticated WITH CHECK (
    bidder_profile_id IN (SELECT id FROM public.profiles WHERE auth_user_id = auth.uid())
    AND EXISTS (SELECT 1 FROM public.offerings o WHERE o.id = offering_id AND o.status = 'ACTIVE' AND o.bid_closed_at IS NULL AND o.bid_call_count < 3)
);
CREATE POLICY "Staff can manage offering bids" ON public.offering_bids FOR ALL TO authenticated
    USING (public.is_staff() OR public.has_current_user_feature('offerings'))
    WITH CHECK (public.is_staff() OR public.has_current_user_feature('offerings'));

DROP POLICY IF EXISTS "Staff can delete offerings" ON public.offerings;
CREATE POLICY "Staff can delete offerings" ON public.offerings FOR DELETE TO authenticated
    USING (public.is_staff() OR public.has_current_user_feature('offerings'));

DROP POLICY IF EXISTS "Staff can manage offering rows" ON public.offerings;
CREATE POLICY "Staff can manage offering rows" ON public.offerings FOR ALL TO authenticated
    USING (public.is_staff() OR public.has_current_user_feature('offerings'))
    WITH CHECK (public.is_staff() OR public.has_current_user_feature('offerings'));
DROP POLICY IF EXISTS "Public can read active emergency contacts" ON public.emergency_contacts;
DROP POLICY IF EXISTS "Public read active emergency contacts" ON public.emergency_contacts;
CREATE POLICY "Public can read active emergency contacts" ON public.emergency_contacts FOR SELECT TO anon, authenticated USING (is_active = TRUE);
GRANT SELECT ON public.emergency_contacts TO anon, authenticated;
GRANT INSERT, UPDATE, DELETE ON public.emergency_contacts TO authenticated;

DROP POLICY IF EXISTS "Staff can update complaints" ON public.complaints;
CREATE POLICY "Staff can update complaints" ON public.complaints FOR UPDATE TO authenticated
    USING (public.is_staff() OR public.has_current_user_feature('complaints'))
    WITH CHECK (public.is_staff() OR public.has_current_user_feature('complaints'));

DROP POLICY IF EXISTS "Staff can view all complaints" ON public.complaints;
CREATE POLICY "Staff can view all complaints" ON public.complaints FOR SELECT TO authenticated
    USING (public.is_staff() OR public.has_current_user_feature('complaints'));

ALTER TABLE public.complaints ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Residents can read own complaints" ON public.complaints;
DROP POLICY IF EXISTS "Residents can create own complaints" ON public.complaints;
DROP POLICY IF EXISTS "Staff can update complaints" ON public.complaints;
DROP POLICY IF EXISTS "Staff can view all complaints" ON public.complaints;
CREATE POLICY "Staff can update complaints" ON public.complaints FOR UPDATE TO authenticated
    USING (public.is_staff() OR public.has_current_user_feature('complaints'))
    WITH CHECK (public.is_staff() OR public.has_current_user_feature('complaints'));
CREATE POLICY "Staff can view all complaints" ON public.complaints FOR SELECT TO authenticated
    USING (public.is_staff() OR public.has_current_user_feature('complaints'));
CREATE POLICY "Residents can read own complaints" ON public.complaints FOR SELECT TO authenticated
    USING (user_id IN (SELECT id FROM public.profiles WHERE auth_user_id = auth.uid()));
CREATE POLICY "Residents can create own complaints" ON public.complaints FOR INSERT TO authenticated
    WITH CHECK (user_id IN (SELECT id FROM public.profiles WHERE auth_user_id = auth.uid()));
GRANT SELECT, INSERT, UPDATE ON public.complaints TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.offerings, public.offering_bids, public.offering_winner_private TO authenticated;

DROP POLICY IF EXISTS "Members can view profiles" ON public.profiles;
DROP POLICY IF EXISTS "Profiles are viewable by owner or admin" ON public.profiles;
CREATE POLICY "Members can view profiles" ON public.profiles FOR SELECT TO authenticated
    USING (
        auth_user_id = auth.uid()
        OR public.is_admin()
        OR public.has_current_user_feature('members')
    );

DROP POLICY IF EXISTS "Staff can manage offering_winner_private" ON public.offering_winner_private;
CREATE POLICY "Staff can manage offering_winner_private" ON public.offering_winner_private FOR ALL TO authenticated
    USING (public.is_staff() OR public.has_current_user_feature('offerings'))
    WITH CHECK (public.is_staff() OR public.has_current_user_feature('offerings'));

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
  IF NOT public.can_manage_feature('offerings') THEN RAISE EXCEPTION 'Offering feature access required'; END IF;
  SELECT b.amount INTO top_bid_amount FROM public.offering_bids b
    WHERE b.offering_id = p_offering_id AND b.status <> 'REJECTED'
    ORDER BY b.amount DESC, b.created_at ASC LIMIT 1;
  UPDATE public.offerings o
    SET bid_call_count = o.bid_call_count + 1,
        status = CASE WHEN o.bid_call_count = 2 THEN 'COMPLETED' ELSE o.status END,
        bid_closed_at = CASE WHEN o.bid_call_count = 2 THEN NOW() ELSE o.bid_closed_at END,
        winning_bid_amount = CASE WHEN o.bid_call_count = 2 THEN top_bid_amount ELSE o.winning_bid_amount END
    WHERE o.id = p_offering_id AND o.status = 'ACTIVE' AND o.bid_call_count < 3
      AND EXISTS (SELECT 1 FROM public.offering_bids b WHERE b.offering_id = p_offering_id AND b.status <> 'REJECTED')
    RETURNING o.bid_call_count INTO updated_call_count;
  IF NOT FOUND THEN RAISE EXCEPTION 'Offering is closed or has no eligible bids'; END IF;
  IF updated_call_count = 3 THEN PERFORM public.start_winner_name_flow(p_offering_id); END IF;
  RETURN updated_call_count;
END;
$$;
REVOKE ALL ON FUNCTION public.call_offering_bid(UUID, TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.call_offering_bid(UUID, TEXT) TO authenticated;

CREATE OR REPLACE FUNCTION public.admin_set_winner_display_name(p_offering_id UUID, p_name TEXT)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
  v_offering RECORD;
  v_profile_id UUID;
  v_cleaned TEXT;
BEGIN
  IF NOT public.can_manage_feature('offerings') THEN RAISE EXCEPTION 'Offering feature access required'; END IF;
  SELECT p.id INTO v_profile_id FROM public.profiles p WHERE p.auth_user_id = auth.uid();
  SELECT id, winner_name_status, winner_name_deadline_at, winner_display_name INTO v_offering
    FROM public.offerings WHERE id = p_offering_id;
  IF NOT FOUND THEN RAISE EXCEPTION 'Offering not found'; END IF;
  IF NOT (v_offering.winner_name_status = 'AWAITING_ADMIN'
      OR (v_offering.winner_name_status = 'AWAITING_WINNER' AND NOW() > v_offering.winner_name_deadline_at)
      OR v_offering.winner_name_status = 'CONFIRMED') THEN
    RAISE EXCEPTION 'Cannot override while awaiting winner submission';
  END IF;
  v_cleaned := REGEXP_REPLACE(BTRIM(COALESCE(p_name, '')), '\s+', ' ', 'g');
  IF v_cleaned ~ '[\x00-\x1F\x7F]' OR CHAR_LENGTH(v_cleaned) < 2 OR CHAR_LENGTH(v_cleaned) > 60 THEN
    RAISE EXCEPTION 'Display name must be 2 to 60 valid characters';
  END IF;
  UPDATE public.offerings SET winner_display_name = v_cleaned, winner_name_status = 'CONFIRMED' WHERE id = p_offering_id;
  UPDATE public.offering_winner_private SET display_name_set_by = 'ADMIN', display_name_set_at = NOW(), updated_at = NOW()
    WHERE offering_id = p_offering_id;
  INSERT INTO public.audit_logs (user_id, action, entity_type, entity_id, old_data, new_data)
    VALUES (v_profile_id, 'WINNER_NAME_SET', 'OFFERING', p_offering_id,
      jsonb_build_object('winner_display_name', v_offering.winner_display_name, 'status', v_offering.winner_name_status),
      jsonb_build_object('winner_display_name', v_cleaned, 'status', 'CONFIRMED', 'set_by', 'ADMIN'));
END;
$$;
REVOKE ALL ON FUNCTION public.admin_set_winner_display_name(UUID, TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.admin_set_winner_display_name(UUID, TEXT) TO authenticated;


