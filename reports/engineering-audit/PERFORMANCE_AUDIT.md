# Performance audit — 2026-10-18

The public read workload requests `notices`, `events`, and `emergency_contacts` sequentially. It is therefore paced: a virtual user is not three concurrent HTTP requests. The shared smoke workload intentionally sleeps two seconds after each request, so its approximately 6–7 second iteration time is mostly deliberate sleep time.

Historical 100-VU evidence showed notices p95 at 4.01 seconds while events were about 636 ms and emergency contacts about 364 ms with zero HTTP failures. This isolates the slow endpoint in that run but does not establish a PostgreSQL, API, network, or generator root cause.

Repository inspection found `notices` has a UUID primary key and indexes including `(status, publish_at DESC)`, `is_pinned` partial, category, and `created_at`. The public policy filters published rows by publish and expiry time. No index or RLS change was made because an actual plan and target metrics were unavailable.

Use the staging-only load runner and collect the same-window API Edge logs, Postgres logs, CPU, memory, disk IOPS, and pool connections. Run `EXPLAIN (ANALYZE, BUFFERS)` as the anonymous role on staging for `select id from public.notices limit 5`. Compare it with `events` and `emergency_contacts` before altering schema.
