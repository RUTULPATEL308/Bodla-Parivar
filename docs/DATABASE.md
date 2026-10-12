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
- `bid_call_count SMALLINT NOT NULL DEFAULT 0`
- `winning_bid_amount NUMERIC(12, 2)`
- `bid_closed_at TIMESTAMPTZ`
- `winner_name_status TEXT NOT NULL DEFAULT 'NOT_STARTED'` (`NOT_STARTED`, `AWAITING_WINNER`, `AWAITING_ADMIN`, `CONFIRMED`)
- `winner_display_name TEXT`
- `winner_name_deadline_at TIMESTAMPTZ`

### `offering_winner_private`
Private, staff-restricted and winner-only view for tracking confidential winner identity:
- `offering_id UUID PRIMARY KEY REFERENCES offerings(id) ON DELETE CASCADE`
- `winning_bid_id UUID REFERENCES offering_bids(id) ON DELETE SET NULL`
- `winner_profile_id UUID REFERENCES profiles(id) ON DELETE SET NULL`
- `bidder_name TEXT`
- `profile_full_name TEXT`
- `display_name_set_by TEXT` (`WINNER`, `ADMIN`)
- `display_name_set_at TIMESTAMPTZ`
- `created_at TIMESTAMPTZ DEFAULT NOW()`
- `updated_at TIMESTAMPTZ DEFAULT NOW()`

### `notifications`
System announcements and targeted alerts:
- `id UUID PRIMARY KEY DEFAULT gen_random_uuid()`
- `title_gu TEXT NOT NULL`, `title_en TEXT NOT NULL`
- `body_gu TEXT NOT NULL`, `body_en TEXT NOT NULL`
- `type TEXT NOT NULL`
- `reference_id UUID`
- `payload JSONB NOT NULL DEFAULT '{}'::jsonb`
- `created_by UUID REFERENCES profiles(id) ON DELETE SET NULL`
- `created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()`

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

## 4. Key RPC Functions

- `public.get_server_time() RETURNS TIMESTAMPTZ`: Returns the server time (`NOW()`), accessible to `anon` and `authenticated` for countdown clock synchronisation.
- `public.call_offering_bid(p_offering_id UUID, p_winner_name TEXT DEFAULT NULL) RETURNS INTEGER`: Staff-only function to cycle offering calls (1 -> 2 -> 3). On the third call, closes the offering and invokes `start_winner_name_flow`.
- `public.submit_winner_display_name(p_offering_id UUID, p_name TEXT) RETURNS VOID`: Authenticated-only function allowing the validated winner to submit their chosen display name within the 2-minute deadline window.
- `public.admin_set_winner_display_name(p_offering_id UUID, p_name TEXT) RETURNS VOID`: Staff-only function allowing administrators to set or amend the winner display name.

---

## 5. Migrations

All SQL scripts are in `database/migrations/`:
- `001_initial_schema.sql`: Table definitions and foreign keys
- `002_indexes_and_performance.sql`: B-Tree and GIN indexes
- `003_functions_and_triggers.sql`: Automated `updated_at` and profile sync triggers
- `004_storage_policies.sql`: Storage bucket policies and MIME validation
- ...
- `019_winner_display_name.sql`: Server-side winner display name flow, `offering_winner_private` table, and notifications payload column
- `020_current_user_offering_winner_rpc.sql`: Authenticated winner verification without direct private-table reads
- `021_grant_offering_winner_private_read.sql`: Allows authenticated winner/admin reads, still restricted by row-level security
- `022_publish_admin_entered_winner_name.sql`: Publishes the name supplied by staff when closing an in-person winning bid

