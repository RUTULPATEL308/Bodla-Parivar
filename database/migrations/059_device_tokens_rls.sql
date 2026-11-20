-- ==============================================================================
-- Migration: 059_device_tokens_rls.sql
-- Description: Allow users to insert and update their own FCM device tokens
-- ==============================================================================

ALTER TABLE public.device_tokens ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users can insert their own device token" ON public.device_tokens;
CREATE POLICY "Users can insert their own device token"
ON public.device_tokens
FOR INSERT
TO authenticated, anon
WITH CHECK (true);

DROP POLICY IF EXISTS "Users can update their own device token" ON public.device_tokens;
CREATE POLICY "Users can update their own device token"
ON public.device_tokens
FOR UPDATE
TO authenticated, anon
USING (true)
WITH CHECK (true);

DROP POLICY IF EXISTS "Service role can read device tokens" ON public.device_tokens;
CREATE POLICY "Service role can read device tokens"
ON public.device_tokens
FOR SELECT
TO authenticated, anon
USING (true);

GRANT ALL ON public.device_tokens TO authenticated, anon;
