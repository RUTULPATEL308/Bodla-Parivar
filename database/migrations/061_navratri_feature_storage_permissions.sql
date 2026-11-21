-- Allow users with Navratri feature access to manage report files from mobile and web.
-- Recreate the helper here as some deployed databases have the permission table
-- but do not yet have the helper function from migration 030.
CREATE OR REPLACE FUNCTION public.has_current_user_feature(target_feature TEXT)
RETURNS BOOLEAN
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT EXISTS (
        SELECT 1
        FROM public.profile_feature_permissions pfp
        JOIN public.profiles p ON p.id = pfp.profile_id
        WHERE p.auth_user_id = auth.uid()
          AND COALESCE(p.status, 'ACTIVE') = 'ACTIVE'
          AND pfp.feature_key = target_feature
    );
$$;

REVOKE ALL ON FUNCTION public.has_current_user_feature(TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.has_current_user_feature(TEXT) TO authenticated;

DROP POLICY IF EXISTS "Navratri managers manage report files" ON storage.objects;
CREATE POLICY "Navratri managers manage report files"
    ON storage.objects FOR ALL TO authenticated
    USING (
        bucket_id = 'navratri'
        AND (public.is_staff() OR public.has_current_user_feature('navratri'))
    )
    WITH CHECK (
        bucket_id = 'navratri'
        AND (public.is_staff() OR public.has_current_user_feature('navratri'))
    );
