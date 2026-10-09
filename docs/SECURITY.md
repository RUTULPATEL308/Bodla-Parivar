# Security & Data Protection Specification

## 1. Core Security Principles

1. **Principle of Least Privilege**: Users only access the specific records they own or those explicitly published to the community.
2. **Defense in Depth**: Security checks exist at three layers:
   - Client-side input validation and sanitized UI
   - API / Network boundary validation
   - PostgreSQL engine Row Level Security (RLS) policies
3. **Zero Secrets in Source**: No private credentials, passwords, or service-role keys exist in code repositories.

---

## 2. Row Level Security (RLS) Policy Matrix

| Table | Anonymous / Unauthenticated | Authenticated USER | MODERATOR / CONTENT_MANAGER | SUPER_ADMIN / ADMIN |
| :--- | :--- | :--- | :--- | :--- |
| `village_settings` | Read | Read | Read | Full Access |
| `notices` | Read (Published only) | Read (Published only) | Full Access | Full Access |
| `events` | Read (Published only) | Read (Published only) | Full Access | Full Access |
| `businesses` | Read (Approved only) | Read Approved + Create/Edit Own | Read + Moderate | Full Access |
| `offerings` | Read (Approved/Active) | Read + Submit Own | Read + Moderate | Full Access |
| `complaints` | No Access | Read/Create Own | Read + Assign + Update Status | Full Access |
| `emergency_contacts` | Read Active | Read Active | Read Active | Full Access |
| `audit_logs` | No Access | No Access | No Access | Read Only |

---

## 3. File Upload & Storage Security

All media uploads (photos, attachments) to Supabase Storage enforce strict limits:
- **Profile Photos**: JPEG/PNG/WebP, max 5 MB, stored under authenticated user's folder (`auth.uid()`).
- **Notice Attachments**: JPEG/PNG/WebP/PDF, max 10 MB, restricted to staff upload.
- **Complaint Photos**: JPEG/PNG/WebP, max 10 MB, stored under private bucket accessible only by uploader and village administration.
- **Offering Images**: JPEG/PNG/WebP, max 8 MB.
- **MIME Sniffing Prevention**: Content-Type validation is enforced both client-side and at the storage bucket policy level.

---

## 4. Error Sanitization

Under no circumstances are raw PostgreSQL or stack trace errors exposed to end users:
- Client receives safe, localized error messages:
  - English: *"Something went wrong. Please try again."*
  - Gujarati: *"કંઈક ખોટું થયું છે. કૃપા કરીને ફરી પ્રયાસ કરો."*
- Detailed diagnostic errors are logged securely to Edge Function telemetry and admin audit logs.
