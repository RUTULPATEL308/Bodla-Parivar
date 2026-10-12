# Offering Fixes and Winner Name Flow

Status: planned. Applies to `composeApp` (mobile), `admin`, and a new migration `019_winner_display_name.sql`.

## Problem 1: Deleted offerings still visible on mobile
Cause: `OfferingRepositoryImpl.refreshOfferings()` only does `insertOrReplace`. Rows deleted on the server stay in the local SQLDelight cache.
Fix:
- After a successful full fetch, delete cached offerings whose id is not in the server response (inside the same transaction).
- Do not delete anything when the fetch fails (offline), so offline use still works.
- Also remove the row immediately on a realtime DELETE event.
- Skip ids that still have a pending item in `SyncQueue`.

## Problem 2: Mixed languages in labels
Rule (owner decision): strictly one language. Gujarati mode shows only Gujarati; English mode shows only English. No labels like `"૧ વાર બોલો (Call 1/3)"`, `"LIVE બોલી"` or `"Thayi Boli"` in brackets.
Fix:
- Move every offering label out of `OfferingScreens.kt` into `core/localization/Strings.kt` as `fun x(lang)`.
- Admin panel has the same problem (confirmed by owner). `Header.tsx` has a language toggle that only changes a local `useState`, so no page reacts. Fix: shared language context saved in localStorage, a `lib/i18n.ts` dictionary, and convert the offerings page first.
- Bilingual data sent on purpose (notification `title_gu`/`title_en`, offering `*_gu`/`*_en` fields) is fine; only UI labels must follow the selected language.
- Add a check to the review checklist: no inline `if (lang == ...)` strings in screens.

## Problem 2b: Bid pop-up shows the wrong language
Cause: the admin writes the pop-up text on the server (`notifications.title_gu/en`, `body_gu/en`). The English title copies the Gujarati offering title when the English field is empty, and `MainScreen.kt` falls back to the Gujarati text when English is missing.
Decision (owner): the phone builds the pop-up text itself, in the user's language.
Fix:
- Add a `payload jsonb` column to `notifications` (for example `{ "offering_id": "...", "amount": 5000, "call": 2 }`).
- Admin sends type, `reference_id` and payload for bid events; text fields stay as a fallback for older app versions only.
- Mobile composes the message from `Strings.kt` (one language), using the offering title in the current language from the local cache.
- If the offering has no title in the current language, show the other language title rather than an empty one. Admin form should warn when one language is empty.
- Bidder name is NOT shown in the pop-up (owner decision): amount only.
- Same approach is used later for push notifications (see `NOTIFICATIONS.md`): the device stores its preferred language so push text matches.

## Problem 3: Winner name flow

### Rules
1. When bidding closes (3rd call), only the winner is asked for a name. Nobody else sees any prompt.
2. **Winner is an app user** (bid has a profile): the user gets a prompt to enter a public display name. They have **2 minutes**. 
3. If the user does not confirm within 2 minutes, the **admin is asked** to enter the name.
4. **Winner is an in-person bidder** (bid entered by admin, no profile): the admin gets the prompt immediately, pre-filled with the bidder name typed on the bid.
5. After the name is confirmed, **everyone** sees only the public display name.
6. **Admin sees both**: the public display name and the winner's profile name (private).

### State machine (server-side, column `winner_name_status`)
```text
NOT_STARTED
    | bidding closed
    v
 winner has profile? --yes--> AWAITING_WINNER (deadline = now + 2 minutes)
    |                               |  winner submits name in time
    no                              |-----------------------------> CONFIRMED (set_by WINNER)
    |                               |  deadline passes
    v                               v
AWAITING_ADMIN <--------------------+
    | admin enters name
    v
CONFIRMED (set_by ADMIN)
```

### Database (migration 019)
Public table `offerings` (readable by everyone) gets only public fields:
- `winner_name_status` text: NOT_STARTED | AWAITING_WINNER | AWAITING_ADMIN | CONFIRMED
- `winner_display_name` text, null until CONFIRMED
- `winner_name_deadline_at` timestamptz (shown only so the winner's countdown works)

New staff-only table `offering_winner_private`:
- `offering_id`, `winner_profile_id` (null for in-person), `bidder_name` (typed on bid), `winner_profile_full_name`, `display_name_set_by` (WINNER | ADMIN), timestamps
- RLS: staff can read/write all; a user can read only the row where `winner_profile_id` is their own profile (this is how the app knows "I am the winner" without exposing it to others).

Existing public column `winning_bidder_name` exposes the winner's name to everyone. Plan: stop returning it to mobile, and stop using it for public display (decision needed on dropping the column).

Functions (SECURITY DEFINER, `search_path = ''`):
- `start_winner_name_flow(offering_id)`: called when bidding closes; sets status and deadline.
- `submit_winner_display_name(offering_id, name)`: only the winning profile; only while AWAITING_WINNER and `now() <= deadline` (server clock, not phone clock); trims, 2-60 characters, no control characters; sets CONFIRMED.
- `admin_set_winner_display_name(offering_id, name)`: staff only; only when AWAITING_ADMIN, or AWAITING_WINNER after the deadline; sets CONFIRMED.
- Expired AWAITING_WINNER is treated as AWAITING_ADMIN on read; optionally a scheduled job flips it so admin gets a realtime update.
- If the winning bid is deleted after closing, reset the flow (extend trigger from migration 017).
- Write an audit log row for every name set or change.

### Mobile behaviour
- Show the prompt only if the user's own private row says status is AWAITING_WINNER.
- Countdown is calculated from `winner_name_deadline_at` using server time, so closing and reopening the app does not reset it.
- Everyone else sees "Winner is being confirmed" until status is CONFIRMED, then the display name.
- After timeout the winner sees a short message that the admin will enter the name.
- All text via `Strings.kt`, one language only.

### Admin behaviour
- Offering page shows a clear card when status is AWAITING_ADMIN: input for public winner name, pre-filled with the bidder name for in-person winners.
- Always shows two fields for the winner: Public display name and Profile name (private), plus who set the name (winner or admin).
- Realtime update when the winner submits or the deadline passes.

### Edge cases to test
- Winner closes the app and returns within 2 minutes: countdown continues.
- Winner submits at the exact deadline, or after: rejected by server.
- Two admins submit at once: last write recorded in audit log.
- Winner bid deleted after closing.
- Winner has no profile name set.
- Offline winner: submit fails with a clear message; no local-only fake confirmation.
