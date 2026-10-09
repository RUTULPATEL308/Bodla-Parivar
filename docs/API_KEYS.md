# API Keys & Secrets Management Guide

## 1. Golden Rules for Security

1. **NEVER hardcode secrets or API keys in source code.**
2. **NEVER place the Supabase `service_role` key inside the mobile application.**
3. **NEVER commit `.env` or production credentials to GitHub.**
4. All configurations must be managed through environment variables or secure storage mechanisms (`EncryptedSharedPreferences` on Android, `Keychain` on iOS).

---

## 2. Configured External Services

### A. Supabase (Database, Auth, Storage, Edge Functions)
- **Purpose**: Backend database, user session management, storage buckets, and serverless functions.
- **Required For**: All persistent operations, user profile sync, image uploads, and admin controls.
- **Where to Obtain**: [supabase.com](https://supabase.com) -> Select Project -> Project Settings -> API.
- **Where to Configure**:
  - Web Admin: `admin/.env.local` (`NEXT_PUBLIC_SUPABASE_URL`, `NEXT_PUBLIC_SUPABASE_ANON_KEY`)
  - Serverless Edge Functions: Supabase Secrets Vault (`SUPABASE_SERVICE_ROLE_KEY`)
  - Mobile App: Injected during CI/CD build via Gradle `BuildConfig` / Gradle properties.
- **Cost / Tier**: Free tier available (Generous storage and database limits).
- **Security Considerations**: The `anon` key is safe for client applications because data access is strictly governed by PostgreSQL Row Level Security (RLS). The `service_role` key bypasses RLS and must strictly be restricted to secure backend environments.
- **Fallback Behavior**: When unconfigured, the mobile app loads cached SQLite records, while the admin console renders demonstrative UI states with safe banners.

---

### B. Maps Provider (Google Maps / OpenStreetMap)
- **Purpose**: Rendering village landmarks, business locations, and festival locations.
- **Required For**: Interactive map viewing on Place details and Business locations.
- **Where to Obtain**: Google Cloud Console (Maps SDK for Android / Maps JavaScript API) or OpenStreetMap tiles.
- **Where to Configure**: `MAP_API_KEY` in environment. Default fallback is `MAP_PROVIDER=osm` (OpenStreetMap).
- **Cost / Tier**: OpenStreetMap is free; Google Maps has a $200 monthly free credit.
- **Security Considerations**: If using Google Maps, restrict the API key to Android SHA-1 fingerprint + package name (`com.bodla.parivar.app`).
- **Fallback Behavior**: If no map key is provided, the application opens coordinates directly in the device's native map application via external `geo:` URI intent.

---

### C. Firebase Cloud Messaging (FCM) & Apple Push Notifications (APNs)
- **Purpose**: Real-time push notification broadcasts for critical notices and complaint updates.
- **Required For**: Pushing alerts to closed/backgrounded mobile devices.
- **Where to Obtain**: Firebase Console (Project Settings -> Cloud Messaging) and Apple Developer Portal.
- **Where to Configure**: `FCM_PROJECT_ID`, `google-services.json`, `GoogleService-Info.plist`.
- **Fallback Behavior**: When push tokens are inactive, in-app notifications still persist in the PostgreSQL database and sync on next app launch.

---

## 3. Environment Template Reference

Always refer to [.env.example](file:///d:/Project-Work/BODLA-PARIVVAR/.env.example) when provisioning new environments.
