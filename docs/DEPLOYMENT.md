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
1. Open `iosApp/iosApp.xcodeproj` in Xcode on macOS. The Kotlin framework is built by the Xcode Run Script phase using Gradle.
2. Select your Apple Developer Signing Team under Signing & Capabilities. A free Apple ID can be used for local device testing; TestFlight distribution requires Apple Developer Program membership.
3. To install directly, connect an iPhone to the Mac, select it as the run destination, and click Run. For cloud Macs without USB device access, use TestFlight.
4. To create an archive in terminal or Xcode:
   ```bash
   xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS' -archivePath build/iosApp.xcarchive archive
   ```
5. Export and upload to TestFlight via Xcode Organizer.

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
