-- Let the mobile app choose the correct dashboard for the signed-in user's roles.
CREATE OR REPLACE FUNCTION public.current_user_roles()
RETURNS TEXT[]
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT COALESCE(array_agg(r.name ORDER BY r.name), ARRAY[]::TEXT[])
    FROM public.profiles p
    JOIN public.user_roles ur ON ur.user_id = p.id
    JOIN public.roles r ON r.id = ur.role_id
    WHERE p.auth_user_id = auth.uid();
$$;

REVOKE ALL ON FUNCTION public.current_user_roles() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.current_user_roles() TO authenticated;
