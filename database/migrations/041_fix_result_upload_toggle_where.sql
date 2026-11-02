-- Repair the toggle RPC: installations with safe-update protection reject
-- UPDATE statements that do not include a WHERE clause.
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
