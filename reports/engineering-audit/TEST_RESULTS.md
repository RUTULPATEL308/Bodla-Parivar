# Test results — 2026-10-18

| Check | Result | Notes |
| --- | --- | --- |
| Admin lint | Passed with 4 warnings | Warnings are existing `img` optimization warnings in member views/portal. |
| Admin TypeScript | Passed | `npx tsc --noEmit`. |
| Notification validation tests | Passed | 4 Node tests cover malformed JSON, body limits, invalid fields/targets, and normalization. |
| Admin production build | Passed | Next.js production build completed and generated 19 routes. |
| Android unit tests | Passed | `:composeApp:testDebugUnitTest` completed successfully; 31 tasks, mostly up-to-date. |
| Admin production dependency audit | Passed | 0 vulnerabilities from `npm audit --omit=dev`. |
| Root production dependency audit | Failed | 15 high, 7 moderate vulnerabilities remain after the Expo 58.0.14 patch update; see `DEPENDENCY_AUDIT.md`. |
| iOS simulator build | Not run | Requires macOS/Xcode. |
| Supabase SQL/RLS tests | Not run | No local Supabase stack, database credentials, or approved staging project. |
| High-volume k6 stages | Not run | No approved local/staging target. |

Gradle issued non-fatal warnings that Android Gradle Plugin 8.13.2 exceeds the Kotlin plugin's maximum tested 8.2 and that iOS targets are disabled on Windows. It also reports deprecated features that will be incompatible with Gradle 9.

The notification route's Supabase/Auth/Firebase authorization and failure paths are not integration-tested here because no isolated Supabase/Auth test project or Firebase test credential is configured. Static review confirms authentication and active-profile/role checks occur before body handling, database insertion, and Firebase dispatch.

## Commands executed in this continuation

| Command | Exit status |
| --- | --- |
| `admin/npm test` | 0 |
| `admin/npm run lint` | 0 (4 pre-existing warnings) |
| `admin/npx tsc --noEmit` | 0 |
| `admin/npm run build` | 0 |
| `./gradlew.bat :composeApp:testDebugUnitTest --no-daemon` | 0 |
| `./node_modules/.bin/expo.cmd install --check` | 0 |
| `npm audit --omit=dev` | 1 (expected: 22 unresolved findings) |
| `k6 inspect` with a local loopback target | 0 |
| `k6 inspect` with an unapproved staging host | non-zero (expected guard rejection) |
| PowerShell parser for `run-load-progression.ps1` | 0 |
