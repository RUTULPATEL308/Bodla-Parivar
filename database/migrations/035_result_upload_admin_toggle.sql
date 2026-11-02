-- Make the Result Upload control independent from the general village settings form.
-- The admin console admits active staff roles, so these policies align database
-- writes with that access check instead of rejecting staff at the RLS layer.
DROP POLICY IF EXISTS "Admins can update village settings" ON public.village_settings;
DROP POLICY IF EXISTS "Staff insert village settings" ON public.village_settings;
DROP POLICY IF EXISTS "Staff update village settings" ON public.village_settings;

CREATE POLICY "Staff insert village settings" ON public.village_settings
    FOR INSERT TO authenticated
    WITH CHECK (public.is_staff());

CREATE POLICY "Staff update village settings" ON public.village_settings
    FOR UPDATE TO authenticated
    USING (public.is_staff())
    WITH CHECK (public.is_staff());

-- Keep the settings editor on the UPDATE path even on projects that skipped the seed file.
INSERT INTO public.village_settings (result_upload_enabled)
SELECT FALSE
WHERE NOT EXISTS (SELECT 1 FROM public.village_settings);

CREATE OR REPLACE FUNCTION public.set_result_upload_enabled(p_enabled BOOLEAN)
RETURNS BOOLEAN
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    settings_id UUID;
BEGIN
    IF NOT public.is_staff() THEN
        RAISE EXCEPTION 'Staff access required';
    END IF;

    SELECT id INTO settings_id
    FROM public.village_settings
    ORDER BY created_at ASC
    LIMIT 1;

    IF settings_id IS NULL THEN
        INSERT INTO public.village_settings (result_upload_enabled)
        VALUES (p_enabled);
    ELSE
        UPDATE public.village_settings
        SET result_upload_enabled = p_enabled
        WHERE id = settings_id;
    END IF;

    RETURN p_enabled;
END;
$$;

REVOKE ALL ON FUNCTION public.set_result_upload_enabled(BOOLEAN) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.set_result_upload_enabled(BOOLEAN) TO authenticated;
