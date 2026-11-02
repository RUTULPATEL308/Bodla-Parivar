# Notifications Specification

## Goal
Residents get timely push notifications (Android and iOS, app open or closed) and can review them in an in-app list.

## Triggers (v1)
| Trigger | Type value | Sent when |
|---|---|---|
| New notice | `NOTICE` | Staff publishes a notice |
| New event | `EVENT` | Staff publishes an event |
| Event reminder | `EVENT` | Fixed schedule: 1 day before and 1 hour before the event start (IST) |
| Bid call / live bidding | `BID_CALL`, `BID_APPROVED` | Staff makes a bid call or bidding updates |
| Manual broadcast | `SYSTEM_BROADCAST` | Staff sends from admin console |

## Flow
```text
Staff action in admin (or DB trigger)
        |
        v
Edge Function: send-notification (service role, staff-verified)
        |
        +--> insert row in `notifications`  --> Realtime --> in-app list
        |
        +--> read active `device_tokens`
                |
                v
              FCM --> Android device
                  --> APNs --> iOS device
```

## Content rules
- Preferred approach: notifications carry a structured `payload`; the app builds readable text in the user's language. Server-written `title_gu/en` and `body_gu/en` are kept as a fallback for older app versions.
- For push, `device_tokens` should store the user's preferred language so the sent text matches it.
- Every notification has Gujarati and English title and body. Gujarati is shown first.
- Never include private data (phone numbers, bid bidder details) in push text.
- Tapping a notification opens the related screen using `reference_id`.

## Antim Yatra notices
| Part | Value |
|---|---|
| Lock screen title | `અંતિમ યાત્રા / સ્મશાન યાત્રા` (English: `Antim Yatra / Samsan Yatra`) |
| Notices feed title | `અંતિમ યાત્રા: <deceased name>` so the name stays scannable |
| Notification body | One-line summary, e.g. `શ્રી <name> ની અંતિમ યાત્રા તા. <date>` |
| Notification picture | The generated poster, uploaded to the public `antim-yatra-posters` bucket and passed as `image_url` |

- The poster is the picture for both the lock screen notification and the in-app notification list, so the family sees the same image everywhere.
- The app scales the poster down before uploading it, because FCM downloads the notification image itself and drops pictures that are too large.
- Notices published from the admin console can override the lock screen title and reuse the uploaded picture; the picture URL must be a public URL on the project's own storage.

## Mobile requirements
- Ask notification permission at a sensible moment, with a Gujarati explanation (Android 13+ and iOS require it).
- Register the device token after login; mark old tokens inactive on logout or when FCM reports them invalid.
- Notification list screen with unread state, loading, empty and error states.

## Security requirements
- Only staff or the service role can create notifications. No anonymous or resident inserts.
- Edge Function verifies the caller is staff before sending; service-role key is never in mobile or admin client code.
- `device_tokens`: a user can only write their own tokens.
- Rate-limit broadcasts to prevent accidental spam.

## External setup needed (not done yet)
1. Firebase project, Android app (`google-services.json`) and iOS app (`GoogleService-Info.plist`). Both files are gitignored.
2. Apple Developer Program account, APNs auth key (.p8) uploaded to Firebase.
3. FCM credentials stored as Supabase Edge Function secrets, never in git.

## Known gaps in current code
- Migration 016 lets anonymous users insert and everyone read all notifications.
- The `send-notification` Edge Function uses the FCM HTTP v1 API, but it only pushes while `FCM_SERVICE_ACCOUNT_KEY` is configured as a function secret.
- If the Edge Function is not deployed, the app falls back to the `publish_antim_yatra_notice` database function, which stores the notice and the notification history but cannot send push.
- iOS ignores the FCM picture (`fcm_options.image`) until the app ships a Notification Service Extension.
- Device tokens are not yet registered per user, so the admin console falls back to the `all_users` topic when no tokens are stored.
