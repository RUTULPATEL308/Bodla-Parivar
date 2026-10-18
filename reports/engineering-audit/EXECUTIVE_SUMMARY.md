# Engineering audit — 2026-10-18

## Scope and outcome

The repository contains a Kotlin Multiplatform Compose mobile app (Android and iOS source sets), a Next.js 16 admin portal, Supabase/PostgreSQL migrations and policies, and Supabase Edge Functions. The audit reviewed tracked source, local configuration shape without printing values, SQL migrations, RLS/storage policies, CI workflows, load-test scripts, and available build tooling.

The strongest confirmed risk is dependency health in the repository-root Expo dependency tree: `npm audit --omit=dev` reported 15 high and 7 moderate findings. The admin application dependency tree reported zero production findings. Do not apply `npm audit fix --force`; it proposes an incompatible Expo downgrade and needs a planned dependency update.

A confirmed information-disclosure issue in the admin notification API was fixed: unhandled errors were returned verbatim to callers. The route now returns a generic message and rejects malformed or oversized JSON bodies before dispatching a notification.

## Current release position

The admin production build and TypeScript checks pass. Android unit tests pass. The project is not verified for production release because iOS, Supabase migration/RLS tests, external authentication tests, and representative staging load tests were not runnable in this workspace.

See the other files in this directory for evidence, results, and required actions.
