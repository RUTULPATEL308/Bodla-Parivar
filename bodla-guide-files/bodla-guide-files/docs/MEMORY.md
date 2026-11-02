# Project Memory (current state)

_Last updated: 2026-10-10_

## Current Status
Backend and admin panel work. Mobile app, offline sync and live bidding are not yet verified.

## Completed / Working
- Supabase database with RLS set up (migrations 001-018)
- Admin panel runs and connects to Supabase

## Not Yet Verified
- Mobile app build, login and screens
- Offline sync engine
- Live bidding end to end (admin + mobile)

## Known Issues
- SECURITY INCIDENT: the repo is PUBLIC and git history contains a database password (`.env.example`, `scratch/insert-test.js`) and a Supabase secret key (`scratch/apply-rls.js`). Treat both as compromised until rotated.
- `README.md` and `OFFERINGS.md` contradict the bidding implementation.
- README doc links point to a local `d:/Project-Work/...` path.
- Only one git commit; no tests exist.
- Notifications: table, Edge Function and env settings exist, but migration 016 allows anonymous inserts (spam risk). Firebase and Apple Developer accounts are not created.

## Offerings: reported problems (2026-10-10)
- Deleted offerings still show on mobile (cache never cleaned)
- Mixed Gujarati/English labels in offering screens
- Winner name prompt is shown from local state only and never saved or shared; timer is 60s instead of 2 min
- Spec: `docs/OFFERING_WINNER_FLOW.md`, tasks TASK-B01 to B08

## Environment notes
- Only one Supabase project exists today; staging project being created (TASK-B04a).
- Mobile `Endpoints.kt` hardcodes the project URL and publishable key.
- Duplicate migration prefix: two files start with `002_`. Apply order: 001, 002_indexes, 002_offering_bids, 003, policies/001, 004, seed, 005-018.

- Migration 019 applied on staging; admin and mobile winner UI pending (B06a, B06b, B07). Old mobile prompt does not appear because the server no longer fills `winning_bidder_name`.

## Current Task
Result Upload feature

## Result Upload (2026-10-20)
- Added migration 034 for an admin-controlled `village_settings.result_upload_enabled` switch, private `result_uploads` rows, and a private Storage bucket limited to JPEG/PNG/WebP/PDF at 10 MB.
- Admin portal has a settings switch and Result Uploads list with private signed-file access and UTF-8 spreadsheet export.
- Mobile Android/iOS has a Gujarati form for student name, standard, school, and image/PDF; staff can also review submissions in the app, and uploads are rejected when the switch is off.
- Still needs migration applied and end-to-end verification against a Supabase staging project.
- Follow-up: migration 035 moves switch control to the Result Upload page and uses an `is_staff()`-guarded RPC, with explicit settings INSERT/UPDATE RLS checks and a default row for projects without the seed.
- Follow-up: the mobile app now reads the oldest `village_settings` row explicitly, matching the admin page. Migration 036 keeps the feature flag synchronized across any duplicate legacy settings rows.
- Follow-up: Storage returned 404 `bucket not found` during mobile upload. Migration 037 idempotently repairs the private bucket and its policies; apply it in the same Supabase project as the admin portal.
- Follow-up: migration 038 publishes result_uploads for Realtime; the admin page subscribes to changes and has a manual refresh, while mobile navigates home on successful submission.
- Follow-up: migration 039 adds a staff-only result count RPC so the admin page can distinguish absent rows from SELECT RLS invisibility.
- Follow-up: count RPC reports saved rows while direct dashboard SELECT returns none; migration 040 supplies a staff-checked SECURITY DEFINER list RPC for the dashboard. `is_staff()` includes ADMIN and SUPER_ADMIN.
- Follow-up: mobile staff Result Upload now uses the list RPC, can toggle the shared setting, open signed documents, and share an Excel-readable CSV export on Android/iOS.
- Follow-up: web toggle failed with `UPDATE requires a WHERE clause (21000)`; migration 041 adds `WHERE id IS NOT NULL` while keeping all duplicate settings rows synchronized.

## Next Step
Rotate exposed credentials, then verify the mobile app builds.
