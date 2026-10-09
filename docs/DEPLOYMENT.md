# Deployment & Release Engineering Guide

## 1. Mobile Deployment (Android & iOS)

### Android (Google Play Store)
1. Generate Release Keystore:
   ```bash
   keytool -genkey -v -keystore release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias bodlaparivar
   ```
2. Build Android App Bundle (AAB):
   ```bash
   ./gradlew :composeApp:bundleRelease
   ```
3. Artifact is generated at `composeApp/build/outputs/bundle/release/composeApp-release.aab`.
4. Upload to Google Play Console under Internal Testing track.

### iOS (Apple App Store)
1. Open `iosApp/iosApp.xcodeproj` in Xcode on macOS.
2. Select your Apple Developer Signing Team.
3. In terminal or Xcode, create archive:
   ```bash
   xcodebuild -scheme iosApp -archivePath build/iosApp.xcarchive archive
   ```
4. Export and upload to TestFlight via Xcode Organizer.

---

## 2. Supabase Backend Deployment

1. Install Supabase CLI:
   ```bash
   npm install -g supabase
   ```
2. Link your remote Supabase project:
   ```bash
   supabase link --project-ref your-project-id
   ```
3. Push migrations to remote database:
   ```bash
   supabase db push
   ```
4. Deploy Edge Functions:
   ```bash
   supabase functions deploy send-notification
   supabase functions deploy audit-logger
   ```

---

## 3. Next.js Web Admin Deployment

The admin portal can be deployed seamlessly to Vercel, Netlify, or Docker:

```bash
cd admin
npm install
npm run build
npm run start
```

### Environment Variables required in Production:
- `NEXT_PUBLIC_SUPABASE_URL`
- `NEXT_PUBLIC_SUPABASE_ANON_KEY`
- `SUPABASE_SERVICE_ROLE_KEY`
