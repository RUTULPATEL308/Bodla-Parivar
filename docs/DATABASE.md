# બોદલા પરિવાર — Database Architecture & Schema Specification

## 1. Overview
The **બોદલા પરિવાર** database runs on PostgreSQL 15+ hosted on Supabase, leveraging relational schemas, strict constraints, Row Level Security (RLS), and database triggers for auditability and real-time updates.

---

## 2. Entity Relationship Model

```text
auth.users (Supabase Managed)
    │
    ▼
profiles (Application Profile)
    │
    ├──── user_roles ──────────── roles (SUPER_ADMIN, ADMIN, MODERATOR, USER...)
    ├──── businesses ──────────── business_categories
    ├──── jobs ────────────────── job_categories
    ├──── complaints ──────────── complaint_categories
    ├──── offerings ───────────── offering_categories (Strictly non-auction)
    ├──── favorites
    ├──── reports
    └──── audit_logs

notice_categories ───────────< notices
event_categories ────────────< events
place_categories ────────────< places
gallery_albums ──────────────< gallery_media
notifications ───────────────< notification_reads
```

---

## 3. Core Tables Reference

### `profiles`
Extends `auth.users` with village community fields:
- `id UUID PRIMARY KEY DEFAULT gen_random_uuid()`
- `auth_user_id UUID UNIQUE NOT NULL`
- `full_name TEXT NOT NULL`
- `phone TEXT`, `email TEXT`, `profile_photo_url TEXT`
- `address TEXT`, `bio TEXT`
- `preferred_language TEXT NOT NULL DEFAULT 'gu'` (`gu` or `en`)
- `is_verified BOOLEAN DEFAULT FALSE`
- `status TEXT DEFAULT 'ACTIVE'`

### `village_settings`
Single-record village registry:
- `village_name_gu TEXT NOT NULL DEFAULT 'બોદલા'`
- `village_name_en TEXT NOT NULL DEFAULT 'Bodla'`
- `district TEXT NOT NULL DEFAULT 'Mehsana'`
- `state TEXT NOT NULL DEFAULT 'Gujarat'`
- `country TEXT NOT NULL DEFAULT 'India'`
- `latitude DECIMAL DEFAULT 23.5880`
- `longitude DECIMAL DEFAULT 72.3693`

### `offerings` (ચઢાવો) — NON-AUCTION SPECIFICATION
Represents voluntary village religious and community offerings:
- `id UUID PRIMARY KEY DEFAULT gen_random_uuid()`
- `category_id UUID REFERENCES offering_categories(id)`
- `title_gu TEXT NOT NULL`, `title_en TEXT NOT NULL`
- `description_gu TEXT`, `description_en TEXT`
- `amount NUMERIC(12, 2)`, `quantity NUMERIC(12, 2)`
- `location_gu TEXT`, `location_en TEXT`
- `status TEXT NOT NULL DEFAULT 'PENDING_APPROVAL'` (`DRAFT`, `PENDING_APPROVAL`, `APPROVED`, `ACTIVE`, `COMPLETED`, `CANCELLED`, `EXPIRED`, `REJECTED`)
- `start_at TIMESTAMPTZ`, `end_at TIMESTAMPTZ`

### `complaints`
Citizen grievance tracking:
- `id UUID PRIMARY KEY DEFAULT gen_random_uuid()`
- `user_id UUID REFERENCES profiles(id) NOT NULL`
- `title TEXT NOT NULL`, `description TEXT NOT NULL`
- `status TEXT NOT NULL DEFAULT 'SUBMITTED'` (`SUBMITTED`, `UNDER_REVIEW`, `ASSIGNED`, `IN_PROGRESS`, `RESOLVED`, `REJECTED`, `CLOSED`)
- `priority TEXT DEFAULT 'NORMAL'` (`LOW`, `NORMAL`, `HIGH`, `URGENT`)
- `resolution_note TEXT`

### `emergency_contacts`
Strictly verified official emergency helplines:
- `id UUID PRIMARY KEY DEFAULT gen_random_uuid()`
- `name_gu TEXT NOT NULL`, `name_en TEXT NOT NULL`
- `organization TEXT`, `phone TEXT NOT NULL`, `category TEXT NOT NULL`
- `is_active BOOLEAN DEFAULT TRUE`

### `audit_logs`
Immutable record of administrative actions:
- `id UUID PRIMARY KEY DEFAULT gen_random_uuid()`
- `user_id UUID REFERENCES profiles(id)`
- `action TEXT NOT NULL` (e.g. `ADMIN_APPROVED_OFFERING`, `ADMIN_PUBLISHED_NOTICE`)
- `old_data JSONB`, `new_data JSONB`
- `created_at TIMESTAMPTZ DEFAULT NOW()`

---

## 4. Migrations

All SQL scripts are in `database/migrations/`:
- `001_initial_schema.sql`: Table definitions and foreign keys
- `002_indexes_and_performance.sql`: B-Tree and GIN indexes
- `003_functions_and_triggers.sql`: Automated `updated_at` and profile sync triggers
- `004_storage_policies.sql`: Storage bucket policies and MIME validation
