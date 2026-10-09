-- ==============================================================================
-- Migration: 002_indexes_and_performance.sql
-- Project: બોદલા પરિવાર (Bodla Parivar)
-- Description: Targeted indexes for query performance and full-text search
-- ==============================================================================

-- Profiles indexes
CREATE INDEX IF NOT EXISTS idx_profiles_auth_user_id ON profiles(auth_user_id);
CREATE INDEX IF NOT EXISTS idx_profiles_phone ON profiles(phone);
CREATE INDEX IF NOT EXISTS idx_profiles_status ON profiles(status);

-- User roles indexes
CREATE INDEX IF NOT EXISTS idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX IF NOT EXISTS idx_user_roles_role_id ON user_roles(role_id);

-- Notices indexes
CREATE INDEX IF NOT EXISTS idx_notices_status_publish ON notices(status, publish_at DESC);
CREATE INDEX IF NOT EXISTS idx_notices_pinned ON notices(is_pinned) WHERE is_pinned = TRUE;
CREATE INDEX IF NOT EXISTS idx_notices_category ON notices(category_id);
CREATE INDEX IF NOT EXISTS idx_notices_created_at ON notices(created_at DESC);

-- Events indexes
CREATE INDEX IF NOT EXISTS idx_events_start_at ON events(start_at ASC);
CREATE INDEX IF NOT EXISTS idx_events_status ON events(status);
CREATE INDEX IF NOT EXISTS idx_events_category ON events(category_id);

-- Businesses indexes
CREATE INDEX IF NOT EXISTS idx_businesses_status ON businesses(status);
CREATE INDEX IF NOT EXISTS idx_businesses_category ON businesses(category_id);
CREATE INDEX IF NOT EXISTS idx_businesses_owner ON businesses(owner_id);
CREATE INDEX IF NOT EXISTS idx_businesses_verified ON businesses(is_verified);

-- Jobs indexes
CREATE INDEX IF NOT EXISTS idx_jobs_status_expires ON jobs(status, expires_at);
CREATE INDEX IF NOT EXISTS idx_jobs_category ON jobs(category_id);
CREATE INDEX IF NOT EXISTS idx_jobs_posted_by ON jobs(posted_by);

-- Offerings indexes (Non-auction lifecycle)
CREATE INDEX IF NOT EXISTS idx_offerings_status ON offerings(status);
CREATE INDEX IF NOT EXISTS idx_offerings_category ON offerings(category_id);
CREATE INDEX IF NOT EXISTS idx_offerings_created_by ON offerings(created_by);
CREATE INDEX IF NOT EXISTS idx_offerings_dates ON offerings(start_at, end_at);

-- Complaints indexes
CREATE INDEX IF NOT EXISTS idx_complaints_user_id ON complaints(user_id);
CREATE INDEX IF NOT EXISTS idx_complaints_status_priority ON complaints(status, priority);
CREATE INDEX IF NOT EXISTS idx_complaints_assigned_to ON complaints(assigned_to);
CREATE INDEX IF NOT EXISTS idx_complaints_created_at ON complaints(created_at DESC);

-- Emergency Contacts indexes
CREATE INDEX IF NOT EXISTS idx_emergency_category ON emergency_contacts(category, display_order ASC);
CREATE INDEX IF NOT EXISTS idx_emergency_active ON emergency_contacts(is_active);

-- Places indexes
CREATE INDEX IF NOT EXISTS idx_places_category ON places(category_id);
CREATE INDEX IF NOT EXISTS idx_places_status ON places(status);

-- Gallery indexes
CREATE INDEX IF NOT EXISTS idx_gallery_albums_status ON gallery_albums(status);
CREATE INDEX IF NOT EXISTS idx_gallery_media_album_id ON gallery_media(album_id);

-- Notifications & tokens indexes
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON notifications(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notification_reads_user ON notification_reads(user_id);
CREATE INDEX IF NOT EXISTS idx_device_tokens_user ON device_tokens(user_id, active);

-- Favorites indexes
CREATE INDEX IF NOT EXISTS idx_favorites_user ON favorites(user_id);
CREATE INDEX IF NOT EXISTS idx_favorites_lookup ON favorites(user_id, entity_type, entity_id);

-- Reports indexes
CREATE INDEX IF NOT EXISTS idx_reports_status ON reports(status);
CREATE INDEX IF NOT EXISTS idx_reports_entity ON reports(entity_type, entity_id);

-- Audit logs indexes
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_user_action ON audit_logs(user_id, action);
CREATE INDEX IF NOT EXISTS idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
