-- ==============================================================================
-- Migration: 001_initial_schema.sql
-- Project: બોદલા પરિવાર (Bodla Parivar)
-- Description: Core tables, foreign keys, and integrity constraints
-- ==============================================================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. PROFILES (Extends Supabase auth.users)
CREATE TABLE IF NOT EXISTS profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    auth_user_id UUID UNIQUE NOT NULL,
    full_name TEXT NOT NULL,
    phone TEXT,
    email TEXT,
    profile_photo_url TEXT,
    address TEXT,
    bio TEXT,
    preferred_language TEXT NOT NULL DEFAULT 'gu' CHECK (preferred_language IN ('gu', 'en')),
    is_verified BOOLEAN DEFAULT FALSE,
    status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 2. ROLES
CREATE TABLE IF NOT EXISTS roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT UNIQUE NOT NULL CHECK (name IN ('USER', 'MODERATOR', 'BUSINESS_OWNER', 'CONTENT_MANAGER', 'ADMIN', 'SUPER_ADMIN')),
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 3. USER_ROLES (Junction table)
CREATE TABLE IF NOT EXISTS user_roles (
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY(user_id, role_id)
);

-- 4. VILLAGE_SETTINGS (Single-record configuration for Bodla village)
CREATE TABLE IF NOT EXISTS village_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    village_name_gu TEXT NOT NULL DEFAULT 'બોદલા',
    village_name_en TEXT NOT NULL DEFAULT 'Bodla',
    district TEXT NOT NULL DEFAULT 'Mehsana',
    state TEXT NOT NULL DEFAULT 'Gujarat',
    country TEXT NOT NULL DEFAULT 'India',
    description_gu TEXT,
    description_en TEXT,
    contact_phone TEXT,
    contact_email TEXT,
    website TEXT,
    logo_url TEXT,
    cover_image_url TEXT,
    latitude DECIMAL(10, 7) DEFAULT 23.5880000,
    longitude DECIMAL(10, 7) DEFAULT 72.3693000,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 5. NOTICE CATEGORIES & NOTICES
CREATE TABLE IF NOT EXISTS notice_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name_gu TEXT NOT NULL,
    name_en TEXT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    display_order INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS notices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id UUID REFERENCES notice_categories(id) ON DELETE SET NULL,
    published_by UUID REFERENCES profiles(id) ON DELETE SET NULL,
    title_gu TEXT NOT NULL,
    title_en TEXT NOT NULL,
    description_gu TEXT,
    description_en TEXT,
    image_url TEXT,
    attachment_url TEXT,
    status TEXT NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'PENDING', 'PUBLISHED', 'EXPIRED', 'ARCHIVED')),
    is_pinned BOOLEAN DEFAULT FALSE,
    publish_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 6. EVENT CATEGORIES & EVENTS
CREATE TABLE IF NOT EXISTS event_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name_gu TEXT NOT NULL,
    name_en TEXT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id UUID REFERENCES event_categories(id) ON DELETE SET NULL,
    created_by UUID REFERENCES profiles(id) ON DELETE SET NULL,
    title_gu TEXT NOT NULL,
    title_en TEXT NOT NULL,
    description_gu TEXT,
    description_en TEXT,
    start_at TIMESTAMPTZ NOT NULL,
    end_at TIMESTAMPTZ,
    location_gu TEXT,
    location_en TEXT,
    latitude DECIMAL(10, 7),
    longitude DECIMAL(10, 7),
    image_url TEXT,
    status TEXT NOT NULL DEFAULT 'PUBLISHED' CHECK (status IN ('DRAFT', 'PUBLISHED', 'CANCELLED', 'COMPLETED', 'ARCHIVED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 7. BUSINESS CATEGORIES & BUSINESSES
CREATE TABLE IF NOT EXISTS business_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name_gu TEXT NOT NULL,
    name_en TEXT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    display_order INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS businesses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    category_id UUID REFERENCES business_categories(id) ON DELETE SET NULL,
    name_gu TEXT NOT NULL,
    name_en TEXT NOT NULL,
    description_gu TEXT,
    description_en TEXT,
    phone TEXT,
    whatsapp TEXT,
    email TEXT,
    address_gu TEXT,
    address_en TEXT,
    latitude DECIMAL(10, 7),
    longitude DECIMAL(10, 7),
    opening_time TIME,
    closing_time TIME,
    logo_url TEXT,
    cover_image_url TEXT,
    status TEXT NOT NULL DEFAULT 'PENDING' CHECK (status IN ('DRAFT', 'PENDING', 'APPROVED', 'REJECTED', 'SUSPENDED')),
    is_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 8. JOB CATEGORIES & JOBS
CREATE TABLE IF NOT EXISTS job_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name_gu TEXT NOT NULL,
    name_en TEXT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    posted_by UUID REFERENCES profiles(id) ON DELETE CASCADE,
    category_id UUID REFERENCES job_categories(id) ON DELETE SET NULL,
    title_gu TEXT NOT NULL,
    title_en TEXT NOT NULL,
    description_gu TEXT,
    description_en TEXT,
    company_name TEXT,
    location_gu TEXT,
    location_en TEXT,
    salary_min NUMERIC(12, 2),
    salary_max NUMERIC(12, 2),
    employment_type TEXT DEFAULT 'FULL_TIME' CHECK (employment_type IN ('FULL_TIME', 'PART_TIME', 'CONTRACT', 'SEASONAL', 'DAILY_WAGE')),
    contact_phone TEXT,
    contact_email TEXT,
    expires_at TIMESTAMPTZ,
    status TEXT NOT NULL DEFAULT 'PENDING' CHECK (status IN ('DRAFT', 'PENDING', 'PUBLISHED', 'EXPIRED', 'CLOSED', 'REJECTED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 9. OFFERINGS (ચઢાવો) - NOT AN AUCTION
CREATE TABLE IF NOT EXISTS offering_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name_gu TEXT NOT NULL,
    name_en TEXT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    display_order INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS offerings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_by UUID REFERENCES profiles(id) ON DELETE SET NULL,
    category_id UUID REFERENCES offering_categories(id) ON DELETE SET NULL,
    title_gu TEXT NOT NULL,
    title_en TEXT NOT NULL,
    description_gu TEXT,
    description_en TEXT,
    amount NUMERIC(12, 2),
    quantity NUMERIC(12, 2),
    location_gu TEXT,
    location_en TEXT,
    latitude DECIMAL(10, 7),
    longitude DECIMAL(10, 7),
    image_url TEXT,
    start_at TIMESTAMPTZ,
    end_at TIMESTAMPTZ,
    status TEXT NOT NULL DEFAULT 'PENDING_APPROVAL' CHECK (status IN ('DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'ACTIVE', 'COMPLETED', 'CANCELLED', 'EXPIRED', 'REJECTED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 10. COMPLAINT CATEGORIES & COMPLAINTS
CREATE TABLE IF NOT EXISTS complaint_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name_gu TEXT NOT NULL,
    name_en TEXT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS complaints (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    category_id UUID REFERENCES complaint_categories(id) ON DELETE SET NULL,
    assigned_to UUID REFERENCES profiles(id) ON DELETE SET NULL,
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    photo_url TEXT,
    latitude DECIMAL(10, 7),
    longitude DECIMAL(10, 7),
    status TEXT NOT NULL DEFAULT 'SUBMITTED' CHECK (status IN ('SUBMITTED', 'UNDER_REVIEW', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'REJECTED', 'CLOSED')),
    priority TEXT NOT NULL DEFAULT 'NORMAL' CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT')),
    resolution_note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMPTZ
);

-- 11. EMERGENCY CONTACTS (Verified & Admin-Controlled Only)
CREATE TABLE IF NOT EXISTS emergency_contacts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name_gu TEXT NOT NULL,
    name_en TEXT NOT NULL,
    organization TEXT,
    phone TEXT NOT NULL,
    alternate_phone TEXT,
    category TEXT NOT NULL CHECK (category IN ('AMBULANCE', 'POLICE', 'HOSPITAL', 'FIRE', 'GOVERNMENT', 'HELPLINE')),
    display_order INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 12. PLACE CATEGORIES & PLACES
CREATE TABLE IF NOT EXISTS place_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name_gu TEXT NOT NULL,
    name_en TEXT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS places (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id UUID REFERENCES place_categories(id) ON DELETE SET NULL,
    name_gu TEXT NOT NULL,
    name_en TEXT NOT NULL,
    description_gu TEXT,
    description_en TEXT,
    address_gu TEXT,
    address_en TEXT,
    phone TEXT,
    latitude DECIMAL(10, 7),
    longitude DECIMAL(10, 7),
    image_url TEXT,
    status TEXT NOT NULL DEFAULT 'PUBLISHED' CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 13. GALLERY ALBUMS & GALLERY MEDIA
CREATE TABLE IF NOT EXISTS gallery_albums (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title_gu TEXT NOT NULL,
    title_en TEXT NOT NULL,
    description_gu TEXT,
    description_en TEXT,
    cover_image_url TEXT,
    created_by UUID REFERENCES profiles(id) ON DELETE SET NULL,
    status TEXT NOT NULL DEFAULT 'PUBLISHED' CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS gallery_media (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    album_id UUID NOT NULL REFERENCES gallery_albums(id) ON DELETE CASCADE,
    media_type TEXT NOT NULL DEFAULT 'IMAGE' CHECK (media_type IN ('IMAGE', 'VIDEO')),
    file_url TEXT NOT NULL,
    thumbnail_url TEXT,
    caption_gu TEXT,
    caption_en TEXT,
    uploaded_by UUID REFERENCES profiles(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 14. NOTIFICATIONS, READS & DEVICE TOKENS
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title_gu TEXT NOT NULL,
    title_en TEXT NOT NULL,
    body_gu TEXT NOT NULL,
    body_en TEXT NOT NULL,
    type TEXT NOT NULL CHECK (type IN ('NOTICE', 'EVENT', 'COMPLAINT', 'OFFERING', 'BUSINESS', 'JOB', 'SYSTEM_BROADCAST')),
    reference_id UUID,
    created_by UUID REFERENCES profiles(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS notification_reads (
    notification_id UUID NOT NULL REFERENCES notifications(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    read_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY(notification_id, user_id)
);

CREATE TABLE IF NOT EXISTS device_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    token TEXT UNIQUE NOT NULL,
    platform TEXT NOT NULL CHECK (platform IN ('ANDROID', 'IOS')),
    device_name TEXT,
    last_seen TIMESTAMPTZ DEFAULT NOW(),
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 15. FAVORITES
CREATE TABLE IF NOT EXISTS favorites (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    entity_type TEXT NOT NULL CHECK (entity_type IN ('BUSINESS', 'EVENT', 'PLACE', 'JOB', 'OFFERING')),
    entity_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, entity_type, entity_id)
);

-- 16. REPORTS (Content Moderation)
CREATE TABLE IF NOT EXISTS reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reported_by UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    entity_type TEXT NOT NULL CHECK (entity_type IN ('BUSINESS', 'OFFERING', 'JOB', 'COMMENT', 'GALLERY')),
    entity_id UUID NOT NULL,
    reason TEXT NOT NULL,
    description TEXT,
    status TEXT NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'UNDER_REVIEW', 'RESOLVED', 'DISMISSED')),
    reviewed_by UUID REFERENCES profiles(id) ON DELETE SET NULL,
    review_note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    reviewed_at TIMESTAMPTZ
);

-- 17. AUDIT LOGS (Immutable record of administrative actions)
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES profiles(id) ON DELETE SET NULL,
    action TEXT NOT NULL,
    entity_type TEXT,
    entity_id UUID,
    old_data JSONB,
    new_data JSONB,
    ip_address TEXT,
    user_agent TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
