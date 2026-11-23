ALTER TABLE public.village_settings
    ADD COLUMN IF NOT EXISTS print_share_preferences JSONB NOT NULL
    DEFAULT '{"print":true,"share":true,"columnKeys":{}}'::jsonb;

DROP POLICY IF EXISTS "Staff update village settings" ON public.village_settings;
CREATE POLICY "Staff update village settings" ON public.village_settings
    FOR UPDATE TO authenticated
    USING (public.is_staff())
    WITH CHECK (public.is_staff());

CREATE OR REPLACE FUNCTION public.update_print_share_preferences(p_preferences JSONB)
RETURNS JSONB
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
    IF jsonb_typeof(p_preferences) <> 'object'
       OR jsonb_typeof(p_preferences -> 'print') <> 'boolean'
       OR jsonb_typeof(p_preferences -> 'share') <> 'boolean'
       OR jsonb_typeof(p_preferences -> 'columnKeys') <> 'object' THEN
        RAISE EXCEPTION 'Invalid print/share preferences';
    END IF;

    SELECT id INTO settings_id FROM public.village_settings ORDER BY created_at ASC LIMIT 1;
    IF settings_id IS NULL THEN
        INSERT INTO public.village_settings (print_share_preferences)
        VALUES (p_preferences) RETURNING id INTO settings_id;
    ELSE
        UPDATE public.village_settings SET print_share_preferences = p_preferences WHERE id = settings_id;
    END IF;
    RETURN p_preferences;
END;
$$;

REVOKE ALL ON FUNCTION public.update_print_share_preferences(JSONB) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.update_print_share_preferences(JSONB) TO authenticated;
