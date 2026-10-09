# Admin Console Specification & User Guide

## 1. Overview

The **બોદલા પરિવાર Web Admin Console** is built using **Next.js 14 (App Router)**, **TypeScript**, and **Tailwind CSS**. It provides village administrators and moderators with a secure, responsive portal to govern content, review citizen submissions, and monitor community health.

---

## 2. Navigation & Feature Modules

1. **Dashboard (`/`)**: High-level KPI metrics (Total Members, Pending Approvals, Verified Businesses, Active Jobs, Active Offerings, Open Complaints), quick shortcuts, and pending review queue.
2. **Users & Roles (`/users`, `/roles`)**: Community member directory, verification badges, and granular RBAC assignments.
3. **Notices (`/notices`)**: Creation, pinning, scheduling, and archiving of bilingual village notices.
4. **Events (`/events`)**: Scheduling and tracking cultural festivals, Gram Sabhas, and medical camps.
5. **Businesses (`/businesses`)**: Moderation and verification of village enterprise listings.
6. **Jobs (`/jobs`)**: Review and expiration of agricultural and commercial job listings.
7. **Offerings / ચઢાવો (`/offerings`)**: Community devotional and seva offerings management (Strictly non-auction approval flow).
8. **Complaints (`/complaints`)**: Grievance management, worker assignment, status progression, and resolution notes.
9. **Emergency (`/emergency`)**: Vetting of public emergency helpline numbers.
10. **Audit Logs (`/audit-logs`)**: Immutable log of sensitive administrative actions.
11. **Village Settings (`/settings`)**: Registry parameters for Bodla village (GPS coordinates, description, official contacts).

---

## 3. Granular RBAC Permissions

The console uses permissions mapped from database roles:
- `manage_users`: View, verify, or suspend member accounts.
- `manage_notices`: Create, publish, pin, and archive announcements.
- `manage_offerings`: Approve, activate, or conclude village offerings.
- `manage_complaints`: Review citizen grievances and update resolution status.
- `manage_emergency`: Modify emergency contact helplines.
- `view_audit_logs`: Inspect audit trails (Super Admin only).
- `manage_settings`: Update core village parameters.
