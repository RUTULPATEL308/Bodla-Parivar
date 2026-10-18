# Security audit — 2026-10-18

## Confirmed findings

| Severity | Finding | Evidence | Status |
| --- | --- | --- | --- |
| High | Root Expo dependency tree has known vulnerabilities. | After updating Expo 58.0.11 to 58.0.14, `npm audit --omit=dev` still reports 15 high and 7 moderate findings through Expo/Metro/React Native. | Open; the only audit-proposed aggregate fix is incompatible Expo 44.0.6. See `DEPENDENCY_AUDIT.md`. |
| Medium | Notification API disclosed internal exception messages. | `admin/src/app/api/notifications/send/route.ts` returned `error.message` from its outer catch. | Fixed in source; admin lint/typecheck/build passed afterwards. |
| Medium | Notification API did not cap request body size. | The route parsed arbitrary JSON before validation. | Fixed with a 16 KiB byte limit and malformed JSON handling. |
| Medium | Current database security cannot be verified from migrations alone. | Migrations include permissive historical policies and later corrective migrations, including 026 (`notices USING (true)`) and 031/032/033 corrections. No target schema or migration history was available. | Open; run policy tests on staging and inspect `pg_policies`/grants. |

## Access-control review

The server-side member-management route validates the bearer token with Supabase Auth, requires an active profile, calculates roles using a server-only service-role client, and restricts role changes such as super-admin assignment and removal. The notification route similarly verifies an active staff/feature-permitted profile before using Firebase or privileged database access.

The audited code does not establish that the deployed database matches the latest policies. Migration 032 removes resident audit-log insert access. Migration 033 ensures suspended profiles do not pass `is_staff`, `is_admin`, or `current_user_roles`. Apply and test those migrations in staging before production use.

Several `SECURITY DEFINER` functions in older migrations require deployment verification for `SET search_path = ''`. Later role and offering procedures set an empty search path, but this cannot be confirmed for the live function definitions without database access.

## Secrets review

`.env.local` and service-account patterns are ignored by Git. No sensitive-named files were tracked by `git ls-files`. Source contains a Supabase publishable key in the mobile endpoint configuration; publishable/anonymous keys are intended for clients and RLS must remain the security boundary. No secret values are reproduced here.

An attempt to search historical diffs for the secret marker could not complete because the local Git diff configuration refers to an unavailable external diff program. Treat historical secret exposure as unverified. Review Git hosting secret alerts and rotate any key that is known or suspected to have been committed.

## Required verification

1. Apply migrations 031–033 to staging in order and run `database/tests/*.sql` using an isolated test project.
2. Verify effective RLS, grants, functions, and storage policies in the Supabase SQL editor. Test anonymous, resident, suspended staff, admin, and super-admin accounts.
3. Upgrade the root Expo dependency set in a branch and rerun Android/iOS tests. Do not use an unreviewed forced audit fix.
4. Configure an origin allowlist for Edge Functions if browser invocation is required only from known web origins; `audit-logger` currently permits all origins but requires a verified staff bearer token.
