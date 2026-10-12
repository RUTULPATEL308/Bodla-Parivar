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
- Need to verify the Edge Function's FCM sending code is complete and uses current FCM HTTP v1 API.
- Mobile app has no confirmed token registration or push handling yet.
