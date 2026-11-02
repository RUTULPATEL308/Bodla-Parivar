-- Read submissions through a staff-checked function when table SELECT RLS
-- returns an empty list despite staff having access to the count RPC.
CREATE OR REPLACE FUNCTION public.result_upload_admin_list()
RETURNS TABLE (
    id UUID,
    user_id UUID,
    student_name TEXT,
    standard TEXT,
    school_name TEXT,
    file_path TEXT,
    file_name TEXT,
    content_type TEXT,
    created_at TIMESTAMPTZ,
    submitted_by TEXT,
    submitter_phone TEXT
)
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    IF NOT public.is_staff() THEN
        RAISE EXCEPTION 'Staff access required';
    END IF;

    RETURN QUERY
    SELECT r.id, r.user_id, r.student_name, r.standard, r.school_name,
           r.file_path, r.file_name, r.content_type, r.created_at,
           p.full_name, p.phone
    FROM public.result_uploads AS r
    LEFT JOIN public.profiles AS p ON p.id = r.user_id
    ORDER BY r.created_at DESC;
END;
$$;

REVOKE ALL ON FUNCTION public.result_upload_admin_list() FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.result_upload_admin_list() TO authenticated;
