-- 054_add_notice_status.sql
-- Add a status column to the notice table for the admin approval workflow.
-- New notices default to 'PENDING_APPROVAL'.
-- After an admin approves a notice the status will be set to 'APPROVED'.
-- The application logic will only push notifications for rows with status = 'APPROVED'.

ALTER TABLE public.notices
ADD COLUMN IF NOT EXISTS status TEXT NOT NULL DEFAULT 'PENDING_APPROVAL';

