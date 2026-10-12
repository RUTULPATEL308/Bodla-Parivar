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
TASK-001

## Next Step
Rotate exposed credentials, then verify the mobile app builds.
