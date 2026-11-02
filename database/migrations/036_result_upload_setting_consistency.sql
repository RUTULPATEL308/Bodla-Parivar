-- village_settings may contain legacy duplicate rows. Keep the feature flag
-- consistent across them so every client sees the same on/off value.
CREATE OR REPLACE FUNCTION public.set_result_upload_enabled(p_enabled BOOLEAN)
RETURNS BOOLEAN
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    IF NOT public.is_staff() THEN
        RAISE EXCEPTION 'Staff access required';
    END IF;

    UPDATE public.village_settings
    SET result_upload_enabled = p_enabled
    WHERE id IS NOT NULL;

    IF NOT FOUND THEN
        INSERT INTO public.village_settings (result_upload_enabled)
        VALUES (p_enabled);
    END IF;

    RETURN p_enabled;
END;
$$;

REVOKE ALL ON FUNCTION public.set_result_upload_enabled(BOOLEAN) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.set_result_upload_enabled(BOOLEAN) TO authenticated;
