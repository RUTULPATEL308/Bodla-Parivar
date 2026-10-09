# બોદલા પરિવાર (Bodla Parivar) — System Architecture

## 1. System Overview

**બોદલા પરિવાર** is a digital community platform for the village **બોદલા (Bodla), Mehsana, Gujarat, India**.
The ecosystem comprises:

1. **Mobile Application**: Cross-platform client for Android and iOS engineered with **Kotlin Multiplatform (KMP)** and **Compose Multiplatform (CMP)**.
2. **Web Admin Console**: Modern administration portal built with **Next.js 14**, **TypeScript**, and **Tailwind CSS**.
3. **Backend & Database Layer**: Cloud database and services powered by **Supabase (PostgreSQL 15+)**, Row Level Security (RLS), Edge Functions, and Supabase Storage.

```text
┌─────────────────────────────────────────────────────────────────┐
│                     USER & CLIENT LAYER                         │
├───────────────────────────────┬─────────────────────────────────┤
│  Android / iOS Mobile App     │     Next.js Web Admin Console   │
│  (Compose Multiplatform)      │     (TypeScript + Tailwind)     │
└───────────────┬───────────────┴─────────────────┬───────────────┘
                │                                 │
                ▼                                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                      EDGE & GATEWAY LAYER                       │
│  Supabase PostgREST API / Edge Functions / Auth / Push Dispatch │
└───────────────────────────────┬─────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                     POSTGRESQL DATABASE                         │
│  Row Level Security (RLS) | RBAC | Triggers | Immutable Audits  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. Mobile Clean Architecture

The mobile app follows Clean Architecture principles divided across Presentation, Domain, and Data layers:

```text
Presentation Layer (Compose Multiplatform)
   │  Screens, ViewModels, UI State, Flow collectors
   ▼
Domain Layer (Pure Kotlin)
   │  Entities/Models, Use Cases, Repository Contracts
   ▼
Data Layer (Multiplatform)
   │  Offline-first Repository Implementations, Mappers
   ├──────────────────────────┬──────────────────────────┐
   ▼                          ▼                          ▼
SQLDelight (Local DB)    Ktor Client (REST)       TokenStorage
```

### Key Technologies
- **UI Toolkit**: Compose Multiplatform (Material 3)
- **Concurrency**: Kotlin Coroutines & Reactive Flow
- **Dependency Injection**: Koin
- **Local Persistence**: SQLDelight (SQLite with compile-time verification)
- **Networking**: Ktor Client with `kotlinx.serialization`
- **Typography**: Anek Gujarati (first-class Unicode) & DM Sans

---

## 3. Offline-First Synchronization Strategy

The mobile client functions seamlessly without internet connectivity:

```text
User creates action (e.g. Complaint, Offering)
                 │
                 ▼
       Write to Local SQLite (SQLDelight)
      Status: PENDING_UPLOAD
                 │
                 ▼
       Network Connectivity Detected
                 │
                 ▼
       SyncEngine Worker batches mutations
                 │
                 ▼
     Supabase API accepts & commits
                 │
                 ▼
     Status updated to: SYNCED
```

---

## 4. Security & Access Control

1. **Authentication**: Supabase Auth (Email/Password in V1, Phone OTP ready).
2. **Row Level Security (RLS)**: Enforced at the PostgreSQL engine level; no unauthorized client query can access or mutate unauthorized rows.
3. **Role-Based Access Control (RBAC)**: Managed via `roles` and `user_roles` with `is_admin()` and `is_staff()` database functions.
4. **Zero Client Secrets**: Service-role keys are strictly forbidden in mobile applications. All public requests use the anonymous public key with user-scoped JWT sessions.
