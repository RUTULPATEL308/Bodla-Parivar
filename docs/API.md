# REST API Specification (/api/v1)

## 1. Design Principles

1. All endpoints follow REST conventions prefixed with `/api/v1/` or query PostgREST schemas directly with Supabase JWT.
2. Standardized JSON response envelope across all endpoints:

### Success Response
```json
{
  "success": true,
  "data": {},
  "error": null,
  "timestamp": "2026-09-28T18:46:00Z"
}
```

### Error Response
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Localized safe error message"
  },
  "timestamp": "2026-09-28T18:46:00Z"
}
```

---

## 2. Authentication Endpoints

- `POST /api/v1/auth/register` — Create new village account (Full name, Email, Password, Phone optional)
- `POST /api/v1/auth/login` — Authenticate and receive JWT tokens
- `POST /api/v1/auth/logout` — Invalidate user session
- `POST /api/v1/auth/refresh` — Refresh access token using refresh token
- `POST /api/v1/auth/forgot-password` — Send password reset link
- `GET /api/v1/auth/me` — Fetch current authenticated user's profile
- `PATCH /api/v1/auth/me` — Update current user's profile fields

---

## 3. Notices Endpoints

- `GET /api/v1/notices` — Fetch published notices with pagination and search
- `GET /api/v1/notices/:id` — Fetch notice details by UUID
- `POST /api/v1/notices` — (Admin/Staff only) Create notice
- `PATCH /api/v1/notices/:id` — (Admin/Staff only) Update or pin notice
- `DELETE /api/v1/notices/:id` — (Admin/Staff only) Archive or delete notice

---

## 4. Events Endpoints

- `GET /api/v1/events` — Fetch upcoming events
- `GET /api/v1/events/:id` — Fetch event details
- `POST /api/v1/events` — (Staff only) Create new village event
- `PATCH /api/v1/events/:id` — (Staff only) Update event details
- `DELETE /api/v1/events/:id` — (Staff only) Cancel or delete event

---

## 5. Businesses Endpoints

- `GET /api/v1/businesses` — Fetch approved village businesses
- `GET /api/v1/businesses/:id` — Fetch business details
- `POST /api/v1/businesses` — Submit new business for admin verification
- `PATCH /api/v1/businesses/:id` — Update business details (Owner or Staff)
- `POST /api/v1/businesses/:id/report` — Report inappropriate listing

---

## 6. Offerings (ચઢાવો) Endpoints — NOT AUCTION

- `GET /api/v1/offerings` — Fetch approved and active offerings
- `GET /api/v1/offerings/:id` — Fetch offering details
- `POST /api/v1/offerings` — Submit voluntary village offering (enters `PENDING_APPROVAL`)
- `PATCH /api/v1/offerings/:id` — Update offering (Staff approval / status change)
- `POST /api/v1/offerings/:id/report` — Report inappropriate offering

---

## 7. Complaints Endpoints

- `POST /api/v1/complaints` — Submit citizen grievance (Title, Description, Category, Photo)
- `GET /api/v1/complaints/my` — Fetch complaints registered by current user
- `GET /api/v1/complaints/:id` — Fetch complaint details and resolution history
- `PATCH /api/v1/complaints/:id` — (Staff only) Update status and add resolution note

---

## 8. Emergency Contacts Endpoints

- `GET /api/v1/emergency` — Fetch verified emergency helplines ordered by display sequence
