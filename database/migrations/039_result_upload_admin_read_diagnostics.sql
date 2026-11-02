-- Give staff a narrow, RLS-protected way to verify whether submissions exist.
-- This helps distinguish missing records from row-level SELECT visibility issues.
CREATE OR REPLACE FUNCTION public.result_upload_admin_count()
RETURNS BIGINT
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    IF NOT public.is_staff() THEN
        RAISE EXCEPTION 'Staff access required';
    END IF;

    RETURN (SELECT count(*) FROM public.result_uploads);
END;
$$;

REVOKE ALL ON FUNCTION public.result_upload_admin_count() FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.result_upload_admin_count() TO authenticated;
