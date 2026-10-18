# Production readiness and load-test audit

## Verified architecture

- composeApp/ is a Kotlin Multiplatform application using Compose, Ktor, SQLDelight, and a local sync queue. Android and iOS clients call Supabase Auth/REST directly; the Supabase publishable key is a client identifier, while authorization depends on the JWT and database RLS.
- admin/ is a Next.js App Router application. Most user-facing data access uses the browser Supabase client; sensitive member administration and push dispatch use server routes with a server-only service-role key.
- database/migrations/ is the schema and policy source of truth. database/tests/ contains SQL checks. Supabase Edge Functions live in server/supabase/functions/.
- GitHub Actions defines separate admin lint/build and mobile test/release workflows.

## Prioritized findings

| Severity | Finding | Evidence and action |
| --- | --- | --- |
| High | Any caller accepted by the Edge Function runtime could write with service-role privileges and supply a forged actor ID; the function itself did not verify identity or staff access. | server/supabase/functions/audit-logger/index.ts previously created a service-role client and inserted request JSON without checking authorization. It now validates the JWT, active profile, staff role, payload size, and derives user_id from the verified profile. |
| High | Any authenticated user could forge audit records directly. | The initial RLS policy named System can insert audit logs used WITH CHECK (TRUE). Migration 032 drops that policy and revokes INSERT from anon and authenticated; security-definer database procedures and trusted server code remain able to write. |
| High | Draft, future, and expired notices were exposed through anonymous REST reads after applying migrations. | Migration 026 changed the public notice policy to USING (true), and migration 030 did not restore the publication window. Migration 031 restores the published/time-bounded filter while staff retain their separate management policy. |
| Medium | Ktor INFO logging could record request headers and response bodies, including bearer tokens or personal data. | KtorClientFactory enabled LogLevel.INFO without a redaction configuration. The request/response logger was removed. Add an explicitly redacted diagnostic logger only if operationally needed. |
| Medium | The previous shared smoke script was configured for 1,000 VUs and the public-read script used a tight request loop. | The smoke script is now limited to one VU and pauses between reads. Capacity scenarios are restricted to local/staging, staged, tagged by endpoint, and bounded to 1,000 VUs. |
| High | Suspended profiles retained role-derived access because shared role helpers and service-role API routes did not check profile status. | Migration 033 makes is_staff(), is_admin(), and current_user_roles() require ACTIVE profiles. The admin user and notification routes now apply the same active-profile check. |

## Load-test failure diagnosis

The reported run (44,322 HTTP requests, 73.13% failures, overall p95 10 seconds, successful-response p95 2.27 seconds, interrupted) is not enough evidence to identify whether failures came from PostgREST status errors, gateway rate limiting, connection/timeouts, the Supabase database, the network, or the generator. The earlier script's 10-second request timeout can place failed requests near 10 seconds, but this does not establish why they failed. No matching response-code/error breakdown or Supabase CPU, memory, connection, disk, and rate-limit metrics were provided. Therefore there is no confirmed root cause and no justified index change.

Do not repeat high-volume tests on the shared hosted project. Use a dedicated staging project and collect the full k6 summary/log plus Supabase metrics over the exact test interval. The updated scenarios retain p95 < 2s, p99 < 5s, and failed requests < 1%, and tag measurements per endpoint. A 1,000-VU run is meaningful only if all stages complete, the generator has enough CPU/network capacity, and infrastructure telemetry is captured. The script does not assert that one VU equals one real person or a fixed number of requests per second.

The repository contains indexes for major filter/order columns, but a source-only index review cannot establish their selectivity, use, redundancy, table sizes, or query-plan value. No additional index is proposed without staging EXPLAIN (ANALYZE, BUFFERS) and actual row/resource data.

## Apply and rollback

Review and apply migrations 031, 032, and 033 to local/staging first, after the existing migrations. Then run database/tests/031_public_notice_rls_tests.sql, database/tests/032_audit_log_access_tests.sql, and database/tests/033_active_staff_access_tests.sql against that database. Confirm an anonymous read returns only current published notices, authenticated non-staff users cannot read private notices or insert audit records, and staff still manage notices and read audit logs. Confirm suspended-role behavior with test accounts. Do not apply remotely until staging verification and backup procedures are complete.

Rollback should preserve the security boundary. For 031, re-create the published-only policy shown in that file. For 032, do not restore unrestricted audit inserts; if a workflow needs direct inserts, define a narrower policy for that verified workflow. For 033, only restore the prior role helpers if an equivalent account-disable check is in place. Re-deploy the protected Edge Function with any rollback. Migration SQL is additive and forward-only by design.

## Validation completed in this workspace

- Admin ESLint: passed with four existing img optimization warnings after two hook-rule errors in the existing portal changes were repaired.
- Admin TypeScript (tsc --noEmit): passed.
- Admin production build: passed after switching Google-hosted font imports to the bundled local DM Sans font. Next.js still warns that multiple lockfiles make its inferred workspace root ambiguous.
- k6 inspect: both scripts parse and expose the intended scenario configuration; no load requests were sent. The shared script is capped at one VU; the local script is capped at 1,000 VUs.
- Kotlin Gradle :composeApp:allTests: Gradle completed successfully, but reported allTests NO-SOURCE; Android test tasks were up-to-date and iOS targets were skipped on Windows. This is not evidence that the Kotlin test suite ran during this check.
- No Supabase database, psql, or local Supabase API was available, so SQL tests, RLS behavior, query plans, and live performance were not verified here.

## Remaining steps for measured 1,000-VU capacity

1. Apply migrations and seed representative non-sensitive data in staging; run the SQL policy test.
2. Run the k6 profile progressively at 10, 25, 50, 100, 250, 500, and 1,000 VUs, stopping at the first breached threshold or resource limit.
3. Capture per-endpoint status/error counts, timeout data, dropped iterations, completed stages, generator CPU/network, and Supabase API/database resource graphs.
4. Use the failing endpoint/query with staging EXPLAIN (ANALYZE, BUFFERS) and table statistics before proposing a database index or capacity change.
5. Repeat the same completed workload after each measured change. Current repository evidence does not verify production or 1,000-VU capacity.
