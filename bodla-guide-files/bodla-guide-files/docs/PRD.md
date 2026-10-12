# Product Requirements Document (PRD)

> PRD = WHAT + WHY. How it is built lives in `ARCHITECTURE.md`.

## Product
**બોદલા પરિવાર (Bodla Parivar)**: a digital home for the village of બોદલા (Bodla), Mehsana, Gujarat.

## Problem
Village announcements, events, local businesses and community contributions (ચઢાવો) are spread across WhatsApp groups, word of mouth and in-person gatherings. Residents miss information and the Gram Panchayat has no single trusted channel.

## Target Users
| User | Needs |
|---|---|
| Resident (mobile app) | Read notices and events, find local businesses, take part in live offerings, even with poor connectivity |
| Staff / Admin (web console) | Publish notices and events, run offerings and live bidding, moderate businesses, manage users |

## Goal
One trusted, Gujarati-first app that connects residents and village administration.

## MVP (v1)
1. **Authentication**: email/password signup and login (Supabase Auth), roles via RBAC.
2. **Notices (સૂચનાઓ)**: categorized, pinned, published by staff; read by residents.
3. **Events (કાર્યક્રમો)**: create and list village events.
4. **Directory (ડિરેક્ટરી)**: verified businesses with Call / WhatsApp / Map actions.
5. **Offerings (ચઢાવો) with live bidding**: staff create offerings; signed-in residents bid while bidding is open; staff can record in-person bids, run bid rounds, and close with a winner. After closing, only the winner is asked for a public display name (2 minutes, then admin enters it); everyone sees the display name; admin also sees the winner's profile name. Deleted offerings disappear from phones. Labels follow the app language strictly. Details in `docs/OFFERING_WINNER_FLOW.md`.
6. **Offline-first sync**: local SQLite cache on mobile, background sync.
7. **Web admin console**: users and roles, notices, events, offerings, audit logs.
8. **Push notifications (Android + iOS)**: delivered even when the app is closed, plus an in-app notification list. Triggers: new notice, new event and event reminder (1 day and 1 hour before), offering bid call / live bidding updates, manual broadcast from admin. Details in `docs/NOTIFICATIONS.md`.

## Out of Scope (v1)
- Complaints (ફરિયાદ નિવારણ): moved to a later version
- Emergency contacts screen: moved to a later version
- Payments / online money transfer
- Phone OTP login
- Per-user notification preferences and quiet hours (later)
- SMS / WhatsApp notifications
- AI chatbot, social feed, gamification

## Success Criteria
A resident can:
1. Create an account and log in on Android.
2. Read notices and events, including offline after one sync.
3. Find a business and call or open it on the map.
4. Place a bid on an active offering and see live updates.
5. Receive a push notification on Android and iOS, with the app closed, when a notice or event is published or a bid call happens.

Staff can:
5. Publish a notice and an event.
6. Create an offering, run bidding, and close it with a winner.
7. See every sensitive action in the audit log.

## Open Questions
- Bidding vs "non-auction" wording (see `DECISIONS.md`, ADR-003).
- Admin hosting provider (see ADR-004).
- Firebase and Apple Developer accounts are not created yet (see ADR-006).
