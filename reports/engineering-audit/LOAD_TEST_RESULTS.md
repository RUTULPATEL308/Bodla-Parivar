# Load test results — 2026-10-18

## Actual runs

| Workload | Result | Evidence |
| --- | --- | --- |
| Shared read smoke | Passed | 1 VU, 2 iterations, 6 HTTP 200 checks, 0% failures. Notices p95 458 ms; events p95 446 ms; emergency contacts p95 401 ms. |
| Staged 1–1,000 VU load | Not run | The available hosted project is the shared Bodla project. The runner blocks high-volume tests against it, and no approved local/staging Supabase target was configured. |

The runner in `admin/load-tests/run-load-progression.ps1` requires explicit `TARGET_ENV=local` or `staging`, rejects the shared host, uses only a publishable/anon key, performs a smoke stage, then stops sequential stages at the first failed threshold. It writes redacted JSON/log/CSV artifacts beneath `admin/load-tests/results`.

Acceptance thresholds remain: endpoint p95 < 2 seconds, endpoint p99 < 5 seconds, and HTTP failures < 1%. No readiness claim is made for 1,000 VUs.
