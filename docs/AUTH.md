# Authentication & Identity Architecture

## 1. Overview

**બોદલા પરિવાર** uses **Supabase Auth** as its core identity provider, coupled with a customized `profiles` table in PostgreSQL.

---

## 2. Supported Providers

### Version 1 (Current)
- **Email + Password**: Secure cryptographic hashing (Argon2 / bcrypt) handled internally by Supabase Auth. Passwords never touch application servers or client caches.

### Future Extensibility (Architecturally Prepared)
- **Phone OTP (SMS)**: Designed for rural accessibility; database schema already supports `phone` indexing and verified status.
- **Google Sign-In**: Native SDK support on Android and web.
- **Apple Sign-In**: Mandatory for iOS release when third-party OAuth is enabled.

---

## 3. Registration Flow

```text
User fills registration form
(Full Name, Email, Password, Optional Phone, Preferred Language)
                 │
                 ▼
Supabase Auth creates auth.users record
                 │
                 ▼
PostgreSQL trigger on_auth_user_created fires
                 │
                 ▼
public.profiles record created automatically
with status='ACTIVE' and preferred_language='gu'
                 │
                 ▼
Default role 'USER' assigned in public.user_roles
```

### Testing email confirmation safely

Keep email confirmation enabled while testing this flow. Use one controlled test inbox and register each test address once; already-confirmed users should sign in instead of registering again. Supabase's built-in sender has a low project-wide email quota, so do not repeatedly submit signup or recovery requests to test the callback.

The `008_auth_profile_role_sync.sql` migration seeds the role catalog, creates a `profiles` row for each new Auth user, assigns the default `USER` role, and backfills existing Auth users. Apply the migration to the Supabase project before testing signup. Passwords remain managed and hashed by Supabase Auth; the application never writes plaintext passwords to `public.profiles`.

---

## 4. Session & Token Management

- **Access Token (JWT)**: Short-lived (1 hour), sent via `Authorization: Bearer <token>` header in Ktor / Fetch requests.
- **Refresh Token**: Long-lived, stored securely in:
  - **Android**: `EncryptedSharedPreferences` via Jetpack Security.
  - **iOS**: Apple Keychain Services.
  - **Web Admin**: persisted and refreshed by the official Supabase JavaScript client. The web app does not manually decode or store access/refresh tokens.
- **Session Recovery**: When launching the app offline, the stored profile state is read from local SQLite while the token refresh waits for network availability.

## 5. Web Auth Callback and Recovery

The Next.js App Router uses one browser Supabase client from `admin/src/lib/supabase.ts`. Its `detectSessionInUrl` option lets Supabase process the email-confirmation URL hash. `AdminShell` has the single `onAuthStateChange` listener; it handles the initial session, sign-in, token refresh, sign-out, and password-recovery events. Once Supabase establishes a session, auth callback parameters are removed from the visible URL. No callback route or manual token storage is used.

For local development, set Supabase Dashboard → **Authentication → URL Configuration**:

- Site URL: `http://localhost:3000`
- Redirect URL allow-list: `http://localhost:3000`

Both email confirmation and password recovery return to the app root. The **Forgot password?** action sends one recovery email per deliberate submission; after opening the link, choose a new password in the app.

The existing `profiles` table has an internal `id` referenced by community foreign keys, plus a unique `auth_user_id`. Keep those IDs separate to avoid breaking relationships. Web profile queries match the authenticated user with `profiles.auth_user_id = auth.users.id`. Migrations `008_auth_profile_role_sync.sql` and `009_profiles_auth_fk_and_rls.sql` seed role rows, create/backfill profiles, add the Auth foreign key, and restrict profile reads/updates with RLS.

For repeatable development tests, apply migrations `008` and `009` once in Supabase SQL Editor and configure a custom SMTP provider under **Authentication → SMTP Settings**. Custom SMTP lets you control delivery and configure appropriate Auth email limits without weakening confirmation security. Supabase's built-in sender is rate limited, so avoid repeatedly submitting signup or recovery forms.
