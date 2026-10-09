-- ==============================================================================
-- Policy: 001_row_level_security.sql
-- Project: બોદલા પરિવાર (Bodla Parivar)
-- Description: Complete Row Level Security policies for data confidentiality & RBAC
-- ==============================================================================

-- 1. Enable RLS on ALL tables
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE roles ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_roles ENABLE ROW LEVEL SECURITY;
ALTER TABLE village_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE notice_categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE notices ENABLE ROW LEVEL SECURITY;
ALTER TABLE event_categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE events ENABLE ROW LEVEL SECURITY;
ALTER TABLE business_categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE businesses ENABLE ROW LEVEL SECURITY;
ALTER TABLE job_categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE jobs ENABLE ROW LEVEL SECURITY;
ALTER TABLE offering_categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE offerings ENABLE ROW LEVEL SECURITY;
ALTER TABLE complaint_categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE complaints ENABLE ROW LEVEL SECURITY;
ALTER TABLE emergency_contacts ENABLE ROW LEVEL SECURITY;
ALTER TABLE place_categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE places ENABLE ROW LEVEL SECURITY;
ALTER TABLE gallery_albums ENABLE ROW LEVEL SECURITY;
ALTER TABLE gallery_media ENABLE ROW LEVEL SECURITY;
ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE notification_reads ENABLE ROW LEVEL SECURITY;
ALTER TABLE device_tokens ENABLE ROW LEVEL SECURITY;
ALTER TABLE favorites ENABLE ROW LEVEL SECURITY;
ALTER TABLE reports ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_logs ENABLE ROW LEVEL SECURITY;

-- ------------------------------------------------------------------------------
-- PROFILES POLICIES
-- ------------------------------------------------------------------------------
-- Anyone authenticated can view active profiles
CREATE POLICY "Public profiles are viewable by authenticated users"
    ON profiles FOR SELECT
    TO authenticated
    USING (status = 'ACTIVE');

-- Users can update their own profile
CREATE POLICY "Users can update own profile"
    ON profiles FOR UPDATE
    TO authenticated
    USING (auth_user_id = auth.uid())
    WITH CHECK (auth_user_id = auth.uid());

-- Admins can view and manage all profiles
CREATE POLICY "Admins can manage all profiles"
    ON profiles FOR ALL
    TO authenticated
    USING (public.is_admin());

-- ------------------------------------------------------------------------------
-- VILLAGE SETTINGS POLICIES
-- ------------------------------------------------------------------------------
-- Anyone (even unauthenticated) can view village configuration
CREATE POLICY "Village settings are publicly readable"
    ON village_settings FOR SELECT
    TO anon, authenticated
    USING (TRUE);

CREATE POLICY "Admins can update village settings"
    ON village_settings FOR ALL
    TO authenticated
    USING (public.is_admin());

-- ------------------------------------------------------------------------------
-- NOTICES POLICIES
-- ------------------------------------------------------------------------------
-- Published notices are readable by everyone
CREATE POLICY "Published notices are viewable by all"
    ON notices FOR SELECT
    TO anon, authenticated
    USING (
        status = 'PUBLISHED'
        AND (publish_at IS NULL OR publish_at <= NOW())
        AND (expires_at IS NULL OR expires_at > NOW())
    );

-- Staff/Admins can read and manage all notices
CREATE POLICY "Staff can manage notices"
    ON notices FOR ALL
    TO authenticated
    USING (public.is_staff());

-- Notice categories are viewable by all
CREATE POLICY "Notice categories are publicly readable"
    ON notice_categories FOR SELECT
    TO anon, authenticated
    USING (is_active = TRUE);

CREATE POLICY "Admins can manage notice categories"
    ON notice_categories FOR ALL
    TO authenticated
    USING (public.is_admin());

-- ------------------------------------------------------------------------------
-- EVENTS POLICIES
-- ------------------------------------------------------------------------------
CREATE POLICY "Published events are viewable by all"
    ON events FOR SELECT
    TO anon, authenticated
    USING (status = 'PUBLISHED');

CREATE POLICY "Staff can manage events"
    ON events FOR ALL
    TO authenticated
    USING (public.is_staff());

CREATE POLICY "Event categories are publicly readable"
    ON event_categories FOR SELECT
    TO anon, authenticated
    USING (is_active = TRUE);

-- ------------------------------------------------------------------------------
-- BUSINESSES POLICIES
-- ------------------------------------------------------------------------------
-- Approved businesses are viewable by everyone
CREATE POLICY "Approved businesses are viewable by all"
    ON businesses FOR SELECT
    TO anon, authenticated
    USING (status = 'APPROVED');

-- Owners can view their own businesses regardless of status
CREATE POLICY "Owners can view own businesses"
    ON businesses FOR SELECT
    TO authenticated
    USING (owner_id = public.current_profile_id());

-- Authenticated users can register/submit a business
CREATE POLICY "Users can insert own business"
    ON businesses FOR INSERT
    TO authenticated
    WITH CHECK (owner_id = public.current_profile_id());

-- Owners can update their own business
CREATE POLICY "Owners can update own business"
    ON businesses FOR UPDATE
    TO authenticated
    USING (owner_id = public.current_profile_id())
    WITH CHECK (owner_id = public.current_profile_id());

CREATE POLICY "Staff can manage all businesses"
    ON businesses FOR ALL
    TO authenticated
    USING (public.is_staff());

CREATE POLICY "Business categories are publicly readable"
    ON business_categories FOR SELECT
    TO anon, authenticated
    USING (is_active = TRUE);

-- ------------------------------------------------------------------------------
-- JOBS POLICIES
-- ------------------------------------------------------------------------------
CREATE POLICY "Published jobs are viewable by all"
    ON jobs FOR SELECT
    TO anon, authenticated
    USING (status = 'PUBLISHED' AND (expires_at IS NULL OR expires_at > NOW()));

CREATE POLICY "Users can create job postings"
    ON jobs FOR INSERT
    TO authenticated
    WITH CHECK (posted_by = public.current_profile_id());

CREATE POLICY "Users can view and update own jobs"
    ON jobs FOR ALL
    TO authenticated
    USING (posted_by = public.current_profile_id());

CREATE POLICY "Staff can manage all jobs"
    ON jobs FOR ALL
    TO authenticated
    USING (public.is_staff());

CREATE POLICY "Job categories are publicly readable"
    ON job_categories FOR SELECT
    TO anon, authenticated
    USING (is_active = TRUE);

-- ------------------------------------------------------------------------------
-- OFFERINGS (ચઢાવો) POLICIES - NOT AUCTIONS
-- ------------------------------------------------------------------------------
-- Approved & Active offerings are viewable by all
CREATE POLICY "Active offerings are viewable by all"
    ON offerings FOR SELECT
    TO anon, authenticated
    USING (status IN ('APPROVED', 'ACTIVE', 'COMPLETED'));

-- Users can view their own offerings
CREATE POLICY "Users can view own offerings"
    ON offerings FOR SELECT
    TO authenticated
    USING (created_by = public.current_profile_id());

-- Staff can review and manage all offerings
CREATE POLICY "Staff can manage offerings"
    ON offerings FOR ALL
    TO authenticated
    USING (public.is_staff());

CREATE POLICY "Offering categories are publicly readable"
    ON offering_categories FOR SELECT
    TO anon, authenticated
    USING (is_active = TRUE);

-- ------------------------------------------------------------------------------
-- COMPLAINTS POLICIES
-- ------------------------------------------------------------------------------
-- Users can only view their own complaints
CREATE POLICY "Users can view own complaints"
    ON complaints FOR SELECT
    TO authenticated
    USING (user_id = public.current_profile_id());

-- Users can submit a new complaint
CREATE POLICY "Users can submit complaints"
    ON complaints FOR INSERT
    TO authenticated
    WITH CHECK (user_id = public.current_profile_id());

-- Staff/Admins can view and manage all complaints
CREATE POLICY "Staff can manage complaints"
    ON complaints FOR ALL
    TO authenticated
    USING (public.is_staff());

CREATE POLICY "Complaint categories are publicly readable"
    ON complaint_categories FOR SELECT
    TO authenticated
    USING (is_active = TRUE);

-- ------------------------------------------------------------------------------
-- EMERGENCY CONTACTS POLICIES
-- ------------------------------------------------------------------------------
-- Verified emergency contacts are viewable by anyone
CREATE POLICY "Emergency contacts are publicly viewable"
    ON emergency_contacts FOR SELECT
    TO anon, authenticated
    USING (is_active = TRUE);

CREATE POLICY "Admins can manage emergency contacts"
    ON emergency_contacts FOR ALL
    TO authenticated
    USING (public.is_admin());

-- ------------------------------------------------------------------------------
-- PLACES & GALLERY POLICIES
-- ------------------------------------------------------------------------------
CREATE POLICY "Places are viewable by all"
    ON places FOR SELECT
    TO anon, authenticated
    USING (status = 'PUBLISHED');

CREATE POLICY "Staff can manage places"
    ON places FOR ALL
    TO authenticated
    USING (public.is_staff());

CREATE POLICY "Place categories are publicly readable"
    ON place_categories FOR SELECT
    TO anon, authenticated
    USING (is_active = TRUE);

CREATE POLICY "Gallery albums are viewable by all"
    ON gallery_albums FOR SELECT
    TO anon, authenticated
    USING (status = 'PUBLISHED');

CREATE POLICY "Gallery media are viewable by all"
    ON gallery_media FOR SELECT
    TO anon, authenticated
    USING (TRUE);

CREATE POLICY "Staff can manage gallery"
    ON gallery_albums FOR ALL
    TO authenticated
    USING (public.is_staff());

-- ------------------------------------------------------------------------------
-- FAVORITES POLICIES
-- ------------------------------------------------------------------------------
CREATE POLICY "Users can manage own favorites"
    ON favorites FOR ALL
    TO authenticated
    USING (user_id = public.current_profile_id())
    WITH CHECK (user_id = public.current_profile_id());

-- ------------------------------------------------------------------------------
-- REPORTS POLICIES
-- ------------------------------------------------------------------------------
CREATE POLICY "Users can submit reports"
    ON reports FOR INSERT
    TO authenticated
    WITH CHECK (reported_by = public.current_profile_id());

CREATE POLICY "Staff can view and resolve reports"
    ON reports FOR ALL
    TO authenticated
    USING (public.is_staff());

-- ------------------------------------------------------------------------------
-- AUDIT LOGS POLICIES
-- ------------------------------------------------------------------------------
CREATE POLICY "Admins can view audit logs"
    ON audit_logs FOR SELECT
    TO authenticated
    USING (public.is_admin());

CREATE POLICY "System can insert audit logs"
    ON audit_logs FOR INSERT
    TO authenticated
    WITH CHECK (TRUE);
