# Offline-First Synchronization Architecture

## 1. Architectural Philosophy

For a rural community application like **બોદલા પરિવાર**, intermittent network connectivity is expected. Therefore, the application is designed **offline-first**:
- The local SQLite database (powered by **SQLDelight**) is the **immediate single source of truth** for UI rendering.
- Reads are instantaneous and never blocked by network latency.
- Public content (Notices, Events, Emergency Contacts, Places, Approved Businesses) is proactively cached.
- Local user mutations (Complaints, Offerings, Profile edits) are enqueued in an offline `SyncQueue`.

---

## 2. Data Flow Architecture

```text
       UI (Compose Multiplatform)
                 │
                 ▼
       ViewModel (StateFlow)
                 │
                 ▼
        UseCase / Interactor
                 │
                 ▼
            Repository
                 │
                 ├───────────────────────────────┐
                 ▼                               ▼
       Local SQLite (SQLDelight)        SyncQueue (Pending Actions)
         [Read / Immediate Display]      [PENDING_UPLOAD]
                                                 │
                                                 ▼
                                        Sync Engine (Worker)
                                                 │
                                                 ▼
                                         Supabase / PostgREST
                                                 │
                                                 ▼
                                          Server Ack / Sync
```

---

## 3. Sync States & Queue Schema

Every offline mutation stores:
- `id`: Unique UUID
- `entity_type`: `COMPLAINT`, `OFFERING`, `PROFILE`
- `action`: `CREATE`, `UPDATE`, `DELETE`
- `payload_json`: Serialized JSON payload
- `status`: `PENDING_UPLOAD`, `SYNCED`, `CONFLICT`, `FAILED`
- `retry_count`: Incremented on network failure
- `created_at` / `updated_at`: Timestamps

### Conflict Resolution Strategy
- **Server is Authoritative**: The server's timestamp and state govern in conflicts.
- **Idempotency**: All offline actions generate a client UUID so duplicate network submissions cannot produce duplicate rows in PostgreSQL.
- **Exponential Backoff**: Failed sync requests are automatically retried with jitter (1s, 2s, 4s, 8s, up to 60s).

---

## 4. Background Sync Execution

- **Android**: Managed via `androidx.work.WorkManager` with `NetworkType.CONNECTED` constraint.
- **iOS**: Managed via `BGAppRefreshTask` / `BGProcessingTask`.
- **Immediate Trigger**: The app also runs an opportunistic sync trigger when the application returns to the foreground.
