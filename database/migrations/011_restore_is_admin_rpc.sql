CREATE OR REPLACE FUNCTION public.is_admin()
RETURNS boolean
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
    SELECT EXISTS (
        SELECT 1
        FROM public.user_roles ur
        JOIN public.roles r ON r.id = ur.role_id
        JOIN public.profiles p ON p.id = ur.user_id
        WHERE p.auth_user_id = (SELECT auth.uid())
          AND r.name IN ('ADMIN', 'SUPER_ADMIN')
    );
$$;

REVOKE ALL ON FUNCTION public.is_admin() FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.is_admin() TO authenticated;

NOTIFY pgrst, 'reload schema';