# Architecture Decisions

## ADR-001: Kotlin Multiplatform + Compose Multiplatform for mobile
Reason: one codebase for Android and iOS, offline-first via SQLDelight.

## ADR-002: Supabase as backend
Reason: PostgreSQL, Auth, Storage, Realtime and Edge Functions without managing infrastructure. Authorization is enforced with RLS.

## ADR-003: Live bidding is part of Offerings in v1
Status: Accepted (owner decision), wording OPEN
Context: Migrations 002, 005, 007, 015, 017 and 018 implement bidding. `README.md` and `docs/OFFERINGS.md` say Offerings are "strictly non-auction" and forbid bid/auction terminology.
Decision: Bidding is real and ships in v1.
Follow-up: Owner must decide user-facing wording (keep ચઢાવો/Offerings vocabulary, or allow "bid"), then update `OFFERINGS.md` and `README.md` to match.

## ADR-0xx: Single Supabase project is treated as production
Status: Open risk. Only one Supabase project exists. Until a staging project is created, every migration is applied inside a transaction, after a data export, and tested with a throwaway offering.

## ADR-004: Admin hosting
Status: OPEN. Not decided. Candidates: Vercel, Netlify/Cloudflare Pages, own VPS.

## ADR-005: Tool-neutral AI rules
Reason: Team uses an AI IDE other than Cursor/Claude Code. Rules live in `RULES.md`; `AGENTS.md` points to it.

## ADR-006: Push notifications via Firebase Cloud Messaging (FCM) on Android and iOS
Status: Accepted. Accounts not yet created.
Decision: Use FCM for both platforms; iOS goes through APNs, which needs an Apple Developer Program membership (paid, yearly). FCM itself is free.
Reason: One sending path from the Supabase Edge Function. Existing `send-notification` function and `device_tokens` table are the base.
Consequence: iOS push cannot be tested until the Apple account and APNs key exist, and needs a physical iPhone.

## ADR-007: Notifications are sent only by trusted server code
Status: Accepted.
Decision: Clients (mobile and anonymous) can never insert into `notifications`. Only staff or the Edge Function (service role) can send.
Reason: Migration 016 currently allows anonymous inserts, so anyone with the public key could spam the village. Fix in TASK-N03.

## ADR-008: Winner display name is stored and enforced on the server
Status: Accepted.
Decision: Winner name state, 2-minute deadline and validation live in the database (functions + RLS), not in on-screen state. Winner profile name is private (staff-only table); only the display name is public.
Reason: Current code keeps the confirmed name only in `remember` state, so other users never see it, and a phone clock could bypass a timer.

## ADR-009: Strictly one language per UI mode
Status: Accepted.
Decision: Gujarati mode shows only Gujarati, English mode only English. All labels live in `Strings.kt`.

## ADR-010: Notification text is built on the phone from structured data
Status: Accepted.
Decision: Bid pop-ups (and later push) use a `payload` and local `Strings.kt`, not pre-written server text.
Reason: Server text mixed languages when one language field was empty. Owner decision: the pop-up shows the amount only, never the bidder name.
