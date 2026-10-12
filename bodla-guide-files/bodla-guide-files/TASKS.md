# Tasks

Loop per task: Read -> Plan -> Implement -> Test -> Review -> Commit -> Update docs.

## Phase 0: Security and cleanup (do first)
- [ ] TASK-001 URGENT (public repo): rotate the DB password and the Supabase secret key (`sb_secret_...`); review Supabase logs, `audit_logs`, `profiles`, `user_roles` for unknown changes
- [ ] TASK-002 Replace `.env.example` values with placeholders
- [ ] TASK-002b Make `scratch/apply-rls.js` and `scratch/insert-test.js` read secrets from environment variables (or delete the scripts)
- [ ] TASK-003 Remove secrets from git history (git filter-repo / BFG) and force-push
- [ ] TASK-003b Enable GitHub secret scanning and push protection on the public repo
- [ ] TASK-004 Fix README doc links (local `d:/` paths) and bidding wording per ADR-003
- [ ] TASK-005 Remove `.idea/`, `.gradle/`, `build/`, `inst_user_settings.tmp` from git if tracked

## Phase 1: Verify the foundation
- [x] Database and RLS set up
- [x] Admin panel runs and connects to Supabase
- [ ] TASK-006 Run `npm run lint` and `npm run build` in `admin/`
- [ ] TASK-007 Build mobile app (`./gradlew :composeApp:assembleDebug`) and fix errors
- [ ] TASK-008 Run mobile app on emulator, confirm login

## Phase 2: Notices and Events
- [ ] TASK-009 Admin: notices CRUD works with RLS
- [ ] TASK-010 Mobile: notices list and detail
- [ ] TASK-011 Admin and mobile: events

## Phase 3: Directory
- [ ] TASK-012 Admin: approve businesses
- [ ] TASK-013 Mobile: directory with Call / WhatsApp / Map

## Phase 4: Offerings and live bidding
- [ ] TASK-014 Admin: create offering, record in-person bid
- [ ] TASK-015 Mobile: view offering and place bid
- [ ] TASK-016 Verify realtime updates across two devices
- [ ] TASK-017 Verify bid delete refreshes winner; close bidding

## Phase 4b: Offering fixes (see docs/OFFERING_WINNER_FLOW.md)
- [ ] TASK-B01 Mobile: remove cached offerings missing from server in `refreshOfferings()`; handle realtime DELETE
- [ ] TASK-B02 Mobile: move all offering labels to `Strings.kt`; strictly one language per mode
- [ ] TASK-B03a Admin: add a shared language setting (context + saved in localStorage) so the Header toggle really changes the UI (currently it only changes a local variable)
- [ ] TASK-B03b Admin: create `lib/i18n.ts` dictionary (gu/en) and convert the offerings page labels (about 70 hardcoded Gujarati lines)
- [ ] TASK-B03c Admin: convert remaining pages (notices, complaints, emergency, audit logs, sidebar) one page per task
- [ ] TASK-B04a Before applying 019: create a second free Supabase project as staging (or at minimum export key tables and check backups); apply 001-019 on staging first
- [ ] TASK-B04b Mobile: stop hardcoding SUPABASE_URL and key in `Endpoints.kt`; read from build config (debug -> staging, release -> production)
- [ ] TASK-B04c Add `.env*.local` to `.gitignore` (staging file `admin/.env.development.local` is not currently covered)
- [ ] TASK-B04d Record any migration that fails on an empty database and fix it in a new numbered migration (do not edit applied ones)
- [ ] TASK-B09a Migration: add `payload jsonb` to `notifications` (can be part of migration 019 or a separate 020)
- [ ] TASK-B09b Admin: send structured payload for bid events (new bid, bid call, close); warn when an offering has a title in only one language
- [ ] TASK-B09c Mobile: build the in-app bid pop-up text from payload + `Strings.kt` in the user's language; remove the Gujarati fallback for English mode
- [ ] TASK-B04 Migration 019: winner fields, `offering_winner_private` table, RLS, RPC functions, audit logging
- [ ] TASK-B05 Decide and apply handling of public `winning_bidder_name` column
- [ ] TASK-B06a Mobile data layer: new offering fields, SQLDelight migration, repository functions (isCurrentUserWinner, submitWinnerDisplayName, server time offset)
- [ ] TASK-B06b Mobile UI: winner prompt with server-based 2-minute countdown; public winner display name; remove old local winner logic
- [ ] TASK-B07 Admin: winner name card, shows display name and profile name, fills in after timeout or for in-person winners
- [ ] TASK-B08 Test all cases in `docs/TEST_PLAN.md` (Offering winner flow)

## Phase 5: Offline sync
- [ ] TASK-018 Verify SQLDelight cache and SyncEngine
- [ ] TASK-019 Test airplane-mode flow

## Phase 5b: Push notifications (see docs/NOTIFICATIONS.md)
- [ ] TASK-N01 Create Firebase project; add Android and iOS apps
- [ ] TASK-N02 Create Apple Developer account and APNs key; upload to Firebase
- [ ] TASK-N03 New migration: remove anonymous/resident INSERT on `notifications`; restrict read as needed; RLS on `device_tokens`
- [ ] TASK-N04 Review and finish `send-notification` edge function (staff check, FCM HTTP v1, invalid-token cleanup); store secrets in Supabase
- [ ] TASK-N05 Mobile (Android): permission, token registration, receive and tap handling
- [ ] TASK-N06 Mobile (iOS): APNs setup, permission, token registration (needs physical iPhone)
- [ ] TASK-N07 Mobile: in-app notification list screen (realtime, unread state)
- [ ] TASK-N08 Triggers: notice published, event published, bid call
- [ ] TASK-N09 Event reminders: scheduled job (e.g. Supabase pg_cron) sends at 1 day and 1 hour before start; no duplicate sends; skip cancelled/changed events
- [ ] TASK-N10 Admin: manual broadcast page with confirm step
- [ ] TASK-N11 Test with app open, in background and closed, on both platforms

## Phase 6: Tests and release
- [ ] TASK-020 Add admin tests (Playwright for key flows)
- [ ] TASK-021 Complete `docs/TEST_PLAN.md` checklist
- [ ] TASK-022 Decide admin hosting (ADR-004), deploy preview, QA, then production
- [ ] TASK-023 Add monitoring and backups
