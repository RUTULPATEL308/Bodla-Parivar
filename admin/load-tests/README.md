# Supabase public-read load tests

These k6 scripts measure anonymous, read-only Supabase REST requests for `notices`, `events`, and `emergency_contacts`. They do not measure browser rendering, sign-in, admin activity, bidding, or real people. Each virtual user requests each endpoint sequentially; one VU therefore makes at most one request at a time. `public-read.k6.js` then waits 30 seconds before repeating. Database resource usage and query plans must be collected separately; k6 alone cannot identify database saturation.

## Safe targets

Run capacity tests against a dedicated local or staging Supabase project with representative non-sensitive data and the same migrations/configuration as production. Hosted production and the shared Bodla project are blocked. Do not use a service-role key. Set variables in the current PowerShell window; these values are not written into this repository:

The app's `admin/.env.local` is not automatically imported when you run k6 directly. For an actual local Supabase CLI stack, use `TARGET_ENV=local` and its loopback API URL/public anon key. The progression PowerShell runner can read `NEXT_PUBLIC_SUPABASE_URL` and `NEXT_PUBLIC_SUPABASE_ANON_KEY` from `admin/.env.local` when you explicitly set `TARGET_ENV=local`; it still rejects hosted URLs. For staging, set the staging URL/key and project reference explicitly.

```powershell
$env:TARGET_ENV = 'staging'
$env:SUPABASE_URL = 'https://YOUR-STAGING-PROJECT.supabase.co'
$env:SUPABASE_PUBLISHABLE_KEY = 'YOUR-STAGING-PUBLISHABLE-KEY'
$env:MAX_VUS = '100'
$env:TEST_PROFILE = 'smoke'
& 'C:\Program Files\k6\k6.exe' run .\admin\load-tests\public-read.k6.js
```

Start with `smoke` at up to 5 VUs. Then run the Windows progression runner below, which runs one endpoint smoke and sequential 1, 10, 25, 50, 100, 250, 500, 750, and 1,000 VU steps. Each step ramps to the target over one minute, holds for one minute, and ramps down; it stops on a failed threshold. Do not run the progression against a shared or production project. Run against dedicated staging only with its operator-approved project reference. The thresholds are endpoint p95 < 2 seconds, endpoint p99 < 5 seconds, and failed HTTP requests < 1%.

Available profiles:

- `smoke`: ramps 1 then up to 5 VUs and runs for one minute before ramping down.
- `stage`: ramps to `MAX_VUS`, holds for `HOLD_DURATION`, then ramps down. The progression runner uses a one minute hold.
- `load` (default): staged 1/10/25/50/100/250/500/750/MAX_VUS ramp, then a hold set by `HOLD_DURATION` (default 2 minutes).
- `sustained`: ramps directly to `MAX_VUS` and holds for `HOLD_DURATION`.
- `spike`: ramps to 25, quickly rises to `MAX_VUS`, holds for one minute, then recovers.
- `stress`: steps through the same VU levels with longer holds, ending at `MAX_VUS`.

Repeatable Windows progression from the repository root (PowerShell):

```powershell
$env:TARGET_ENV = 'staging'
$env:SUPABASE_URL = 'https://YOUR-STAGING-PROJECT.supabase.co'
$env:SUPABASE_PUBLISHABLE_KEY = 'YOUR-STAGING-PUBLISHABLE-KEY'
$env:STAGING_SUPABASE_PROJECT_REF = 'YOUR-STAGING-PROJECT-REF'
& .\admin\load-tests\run-load-progression.ps1
```

The runner requires the configured URL host to match `STAGING_SUPABASE_PROJECT_REF.supabase.co` (or loopback for local), rejects the Bodla shared project, refuses service-role JWTs, runs lint/type-check/build unless `-SkipChecks` is supplied, and saves per-step k6 JSON/logs plus `progression.csv` under `admin/load-tests/results`. The publishable key value is never written to results. Staging checks are a guardrail; verify the project reference in Supabase before running because host matching alone cannot establish that a project is non-production.

Run once at a time and watch Supabase database CPU, memory, connections, disk I/O, API errors, and rate limiting. Keep the k6 generator on a separate host with sufficient CPU/network capacity. k6 reports endpoint-tagged latency and adds custom counters tagged by endpoint/status and endpoint/k6 error code; JSON summaries retain these series. Compare the same script, target, data, and generator. A run that does not finish its stages is not a completed capacity result.

## Limited shared-project smoke check

`shared-smoke.k6.js` is pinned to the configured Bodla Supabase host and is only for low-rate endpoint verification. It runs exactly two iterations with one VU, requests each endpoint once per iteration, and waits two seconds after each request. Each iteration therefore takes about six seconds of sleeps plus network time (about 6–7 seconds in the observed smoke); this explains iteration time without indicating slow HTTP requests. It is not a load or capacity test. Use only a publishable/anon key:

```powershell
$env:TARGET_ENV = 'shared-smoke'
$env:SUPABASE_URL = 'https://YOUR-BODLA-PROJECT.supabase.co'
$env:SUPABASE_PUBLISHABLE_KEY = 'YOUR-PUBLISHABLE-OR-ANON-KEY'
& 'C:\Program Files\k6\k6.exe' run .\admin\load-tests\shared-smoke.k6.js
```

## Interpreting the reported 2,000-VU run

The historical 100-VU result (notices p95 4.01 seconds; events 636 ms; emergency contacts 364 ms; 0% HTTP failures) identifies notices as the slow endpoint in that run, but does not identify the layer. The low-rate smoke performed during this work passed all 6 HTTP checks; notices p95 was 458 ms, events 446 ms, and emergency contacts 401 ms. Six requests are too few to infer load capacity or resolve the historical spike. No new high-volume run was made against the shared project.

For the same time window as a staging run, compare API Edge logs (request URL, response status and latency) with Postgres logs and Database reports. If the API request is slow while the corresponding database query is quick, investigate API/network/generator latency; if the database query is slow, inspect its plan, waits, and resource charts. Compare CPU, memory, disk IOPS, and connections. Query plans are not present in this repository, and no database index or policy change is justified from latency percentiles alone. The notices table has a UUID primary key, a `(status, publish_at DESC)` index and other indexes; its public RLS policy filters published rows by `publish_at` and `expires_at`. Migration 026 changed public SELECT to `USING (true)`, and migration 031 restores published/time-bounded access. Confirm the migrations actually applied in the target before benchmarking.

Supabase's [API log guide](https://supabase.com/docs/guides/troubleshooting/discovering-and-interpreting-api-errors-in-the-logs-7xREI9) describes matching PostgREST requests in `edge_logs` and database errors in Postgres logs. The [performance debugging guide](https://supabase.com/docs/guides/database/debugging-performance) describes query plans; plan exposure is disabled by default, so enable it only in a non-production environment. [Supabase Reports](https://supabase.com/docs/guides/observability/reports) shows database CPU, memory, IOPS, and connections. Capture those logs/metrics alongside one staging test before changing indexes or RLS.

For a staging-only plan check in Supabase SQL Editor, run as `postgres` and switch to `anon` so row security is applied. This approximates the API's anonymous role; it does not reproduce all PostgREST request overhead:

```sql
begin;
set local role anon;
explain (analyze, buffers, verbose)
select id from public.notices limit 5;
rollback;
```

Compare the plan and execution time with an admin-role query, then correlate with the same request timestamps in API and Postgres logs. Do not enable plan exposure on the shared or production project for this diagnosis.
