# Test Plan

## Authentication
- User can sign up and log in
- Invalid credentials show a localized error
- Logged-out users cannot open admin pages
- Resident role cannot open staff admin actions

## Notices and Events
- Staff can create, edit, pin and delete a notice
- Residents see only published items
- Same for events

## Directory
- Only approved businesses are visible to residents
- Call, WhatsApp and Map actions open correctly

## Offerings and Live Bidding
- Resident cannot create or edit an offering
- Resident can bid only while bidding is open
- Staff can record an in-person bid
- Bid updates appear live on a second device
- Deleting a bid refreshes the winner
- Closing bidding records winner name and amount

## Offering Fixes and Winner Flow
- Admin deletes an offering: it disappears from the phone after refresh and live (online)
- Offline phone keeps showing cached offerings until it can refresh
- Gujarati mode has no English words in offering screens; English mode has no Gujarati
- Admin adds a bid: pop-up on a Gujarati phone is fully Gujarati and on an English phone fully English, including when the offering has only one title language
- Switching app language while the app is open changes the next pop-up language
- Pop-up shows the amount only; no bidder name appears
- When bidding closes, only the winning app user sees the name prompt
- Winner has 2 minutes (server clock); countdown survives closing and reopening the app
- Late submission is rejected by the server
- After 2 minutes, admin sees the prompt and can enter the name
- In-person winner: admin prompt appears immediately, pre-filled with bidder name
- After confirmation everyone sees the display name and not the profile name
- Admin sees both display name and profile name, and who set it
- Deleting the winning bid after closing resets or updates the flow
- A resident cannot read the private winner table or call the admin function

## Winner flow: single-device test script (staging)
Setup: staging project, test offering with base amount, resident A and resident B accounts, admin signed in on the web panel (browser).
1. Phone: sign in as resident A. Place the highest bid on the offering.
2. Admin web: make calls 1, 2 and 3 (close bidding).
3. Phone (as A): within about 2 seconds the name prompt with a countdown appears. Close and reopen the app: countdown continues, not reset.
4. Enter a name and confirm. Result: success only after refresh shows the winner card with that name.
5. Phone: sign out, sign in as resident B. B sees the winner card with the display name only. No prompt, no profile name.
6. Admin web (after TASK-B07): sees both display name and profile name, set by WINNER.
Timeout case: repeat with a new offering, but as A do not enter a name. After 2 minutes the prompt disappears and the admin card appears; admin enters a name; B then sees it.
In-person case: admin adds the winning bid in person (no profile). Admin gets the prompt, pre-filled with the bidder name; the phone shows no prompt to anyone.
Late submission: leave the prompt open past 2 minutes and press Confirm. The server must reject it and the app shows a localized message.

## Offline Sync (mobile)
- Notices/events visible in airplane mode after one sync
- Data refreshes after reconnecting

## Notifications
- Anonymous or resident users cannot insert into `notifications`
- Publishing a notice or event sends a push to Android and iOS
- Push arrives with the app open, in background and closed
- Event reminders arrive exactly once at 1 day and 1 hour before start
- Cancelled or rescheduled events do not send stale reminders
- Tapping a push opens the correct screen
- Gujarati and English text both display correctly
- Logged-out device stops receiving pushes; invalid tokens are cleaned up
- Manual broadcast requires staff role and a confirm step

## Security
- A user cannot read or change another user's private rows (RLS)
- No service-role key in mobile code or git

## Responsive (admin)
Test at 375px, 768px, 1440px.

## Commands
- Admin: `cd admin && npm run lint && npm run build`
- Mobile: `./gradlew :composeApp:assembleDebug`
