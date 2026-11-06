-- Store mobile app distribution links in Supabase instead of application code.
ALTER TABLE public.village_settings
    ADD COLUMN IF NOT EXISTS android_app_download_url TEXT,
    ADD COLUMN IF NOT EXISTS ios_app_download_url TEXT;

DROP POLICY IF EXISTS "Village settings are readable" ON public.village_settings;
CREATE POLICY "Village settings are readable"
    ON public.village_settings
    FOR SELECT
    TO anon, authenticated
    USING (TRUE);
