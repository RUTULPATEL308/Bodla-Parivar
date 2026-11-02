# Community notice push notifications

Deploy this function to the Supabase project used by the mobile app. Deploying publishes Antim Yatra notices to the app's Notices feed; configure the Firebase service account secret to also send OS push notifications:

```powershell
supabase functions deploy send-notification
supabase secrets set FCM_SERVICE_ACCOUNT_KEY='{"project_id":"...","client_email":"...","private_key":"..."}'
```

The function authenticates the caller, requires an active member profile, limits each member to three community notices per 24 hours, publishes the Antim Yatra entry in the app's Notices feed, and records it in notification history. When `FCM_SERVICE_ACCOUNT_KEY` is configured, it also broadcasts to the `all_users` FCM topic. The notice remains published if FCM is unavailable; the app reports that push delivery was not completed.

## Antim Yatra notification

The mobile app publishes an Antim Yatra entry with these fields:

| Field | Purpose |
|---|---|
| `title_gu` / `title_en` | Title of the notice in the Notices feed, keeps the deceased name (`અંતિમ યાત્રા: <name>`). |
| `notification_title_gu` / `notification_title_en` | Title shown on the lock screen and in the in-app notification list (`અંતિમ યાત્રા / સ્મશાન યાત્રા`). |
| `body_gu` / `body_en` | One-line summary used as the notification body. The full text stays in `description_gu` / `description_en`. |
| `image_url` | Public URL of the generated poster. It is used as the notification picture and is stored in `notifications.payload.image_url` so the in-app notification list shows the same poster. |

The poster is uploaded to the public `antim-yatra-posters` bucket, so `image_url` must start with `<SUPABASE_URL>/storage/v1/object/public/antim-yatra-posters/`. The app scales the poster down before uploading it, because FCM downloads the notification image itself and drops pictures that are too large.

Without the `FCM_SERVICE_ACCOUNT_KEY` secret the function still publishes the notice and the notification history, but no OS notification is sent. Without the function itself deployed, the app falls back to the `publish_antim_yatra_notice` database function, which stores the notice and the notification history but cannot send push.
