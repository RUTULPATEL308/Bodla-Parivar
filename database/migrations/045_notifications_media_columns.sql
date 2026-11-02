-- ==============================================================================
-- Migration: 045_notifications_media_columns.sql
-- Description: Add image_url, attachment_url, and payload columns to notifications table
-- ==============================================================================

ALTER TABLE public.notifications ADD COLUMN IF NOT EXISTS image_url TEXT;
ALTER TABLE public.notifications ADD COLUMN IF NOT EXISTS attachment_url TEXT;
ALTER TABLE public.notifications ADD COLUMN IF NOT EXISTS payload JSONB DEFAULT '{}'::jsonb;

GRANT SELECT ON public.notifications TO anon, authenticated;
GRANT INSERT, UPDATE, DELETE ON public.notifications TO authenticated;
