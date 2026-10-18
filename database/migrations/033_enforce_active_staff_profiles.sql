-- A suspended profile must not retain role-based administrative privileges.
CREATE OR REPLACE FUNCTION public.is_staff()
RETURNS BOOLEAN
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
        WHERE p.auth_user_id = auth.uid()
          AND COALESCE(p.status, 'ACTIVE') = 'ACTIVE'
          AND r.name IN ('ADMIN', 'SUPER_ADMIN', 'MODERATOR', 'CONTENT_MANAGER')
    );
$$;
REVOKE ALL ON FUNCTION public.is_staff() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.is_staff() TO authenticated;

CREATE OR REPLACE FUNCTION public.is_admin()
RETURNS BOOLEAN
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
        WHERE p.auth_user_id = auth.uid()
          AND COALESCE(p.status, 'ACTIVE') = 'ACTIVE'
          AND r.name IN ('ADMIN', 'SUPER_ADMIN')
    );
$$;
REVOKE ALL ON FUNCTION public.is_admin() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.is_admin() TO authenticated;

CREATE OR REPLACE FUNCTION public.current_user_roles()
RETURNS TEXT[]
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
    SELECT COALESCE(array_agg(r.name ORDER BY r.name), ARRAY[]::TEXT[])
    FROM public.profiles p
    JOIN public.user_roles ur ON ur.user_id = p.id
    JOIN public.roles r ON r.id = ur.role_id
    WHERE p.auth_user_id = auth.uid()
      AND COALESCE(p.status, 'ACTIVE') = 'ACTIVE';
$$;
REVOKE ALL ON FUNCTION public.current_user_roles() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.current_user_roles() TO authenticated;

-- Rollback: restore the previous function definitions only if suspended-role
-- enforcement is intentionally reverted after replacing it with an equivalent
-- account-disable mechanism. Do not make this rollback automatically.
