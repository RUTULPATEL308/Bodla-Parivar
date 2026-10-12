# Development Rules (for any AI coding tool)

## Before Coding
- Read `docs/PRD.md`, `docs/ARCHITECTURE.md`, `docs/DESIGN.md`, `TASKS.md`, `docs/MEMORY.md`.
- Inspect the existing implementation first. Reuse before creating.
- Make a plan for any change touching more than 3 files, and wait for approval.

## General
- One task at a time from `TASKS.md`. Do not modify unrelated files.
- Keep functions small. Do not duplicate logic.
- Do not change a decision in `docs/DECISIONS.md` without asking.

## Mobile (composeApp, Kotlin)
- Clean Architecture: presentation / domain / data. No network or DB code in UI.
- Offline-first: write to SQLDelight first, then sync.
- All user-facing text lives in `core/localization/Strings.kt`. No inline `if (lang == ...)` strings in screens. Each mode shows only its own language (no mixed labels). Use the theme tokens from `docs/DESIGN.md`.
- Timers and deadlines that affect other users (like the winner name deadline) must be enforced on the server, never only on the phone.

## Admin (admin, Next.js)
- This Next.js version has breaking changes. Read `admin/node_modules/next/dist/docs/` before writing code (see `admin/AGENTS.md`).
- TypeScript only. Follow `docs/DESIGN.md`. Include loading, empty and error states.

## Database (Supabase)
- Schema changes only as new numbered files in `database/migrations/`. Never edit applied migrations.
- Every new table needs RLS policies and a matching entry in `docs/SECURITY.md`.

## Security
- Never commit secrets. Real values go in `.env.local`; `.env.example` holds placeholders only.
- Service-role keys never go in mobile code or client-side admin code.
- Validate all input. Check authorization server-side or via RLS.
- Never show raw database errors to users.

## Testing
- Add tests for important logic. Run lint/build/tests after each task and fix failures before moving on.

## Git
- Small commits with descriptive messages (`feat:`, `fix:`, `docs:`).
- Use a branch per feature. Update `TASKS.md` and `docs/MEMORY.md` after each task.
