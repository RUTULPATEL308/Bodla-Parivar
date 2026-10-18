# Remediation log — 2026-10-18

| File | Change | Verification |
| --- | --- | --- |
| `admin/src/app/api/notifications/send/route.ts` | Added bounded, typed notification validation; malformed/oversized input and internal error disclosure are handled safely. Authorization and Firebase dispatch ordering were retained. | Validation tests, admin lint, TypeScript, and production build passed. |
| `admin/src/lib/notificationValidation.ts`, `admin/test/notification-validation.test.ts` | Added reusable input validation and 4 Node tests. | `npm test` passed. |
| `package.json`, `package-lock.json` | Updated Expo SDK 58 patch from 58.0.11 to 58.0.14 using Expo CLI compatibility tooling. | Local Expo dependency check passed; npm audit remains open. |
| `database/tests/034_effective_security_catalog_tests.sql` | Added a read-only staging/local catalog check for restrictive notices/audit policies and explicit `SECURITY DEFINER` search paths. | Not run: no approved database target. |
| `admin/load-tests/public-read.k6.js` | Added staged profiles, endpoint tags, p95/p99 endpoint thresholds, and status/error counters. | Script configuration guard checked; shared smoke exercised separately. |
| `admin/load-tests/shared-smoke.k6.js` | Restricted to two one-VU, read-only iterations and counters. | 6/6 checks passed. |
| `admin/load-tests/run-load-progression.ps1` | Added safe Windows staging/local progression with target/key guards and reports. | PowerShell parser passed. |

No migration, RLS policy, grant, service role, production database, or high-volume hosted load test was changed or applied.
