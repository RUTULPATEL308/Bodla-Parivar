-- Allow only notice owners or administrators to delete notices.
-- Keep existing staff management policy limited to insert/update operations.
DROP POLICY IF EXISTS "Staff can manage notices" ON public.notices;
DROP POLICY IF EXISTS "Staff can insert notices" ON public.notices;
DROP POLICY IF EXISTS "Staff can update notices" ON public.notices;

CREATE POLICY "Staff can insert notices" ON public.notices
    FOR INSERT TO authenticated
    WITH CHECK (
        EXISTS (
            SELECT 1
            FROM public.user_roles ur
            JOIN public.roles r ON r.id = ur.role_id
            JOIN public.profiles p ON p.id = ur.user_id
            WHERE p.auth_user_id = auth.uid()
              AND COALESCE(p.status, 'ACTIVE') = 'ACTIVE'
              AND r.name IN ('ADMIN', 'SUPER_ADMIN', 'MODERATOR', 'CONTENT_MANAGER')
        )
        OR published_by IN (
            SELECT p.id FROM public.profiles p WHERE p.auth_user_id = auth.uid()
        )
    );

CREATE POLICY "Staff can update notices" ON public.notices
    FOR UPDATE TO authenticated
    USING (
        EXISTS (
            SELECT 1
            FROM public.user_roles ur
            JOIN public.roles r ON r.id = ur.role_id
            JOIN public.profiles p ON p.id = ur.user_id
            WHERE p.auth_user_id = auth.uid()
              AND COALESCE(p.status, 'ACTIVE') = 'ACTIVE'
              AND r.name IN ('ADMIN', 'SUPER_ADMIN', 'MODERATOR', 'CONTENT_MANAGER')
        )
    )
    WITH CHECK (
        EXISTS (
            SELECT 1
            FROM public.user_roles ur
            JOIN public.roles r ON r.id = ur.role_id
            JOIN public.profiles p ON p.id = ur.user_id
            WHERE p.auth_user_id = auth.uid()
              AND COALESCE(p.status, 'ACTIVE') = 'ACTIVE'
              AND r.name IN ('ADMIN', 'SUPER_ADMIN', 'MODERATOR', 'CONTENT_MANAGER')
        )
    );

DROP POLICY IF EXISTS "Notice owners can delete their notices" ON public.notices;
CREATE POLICY "Notice owners can delete their notices" ON public.notices
    FOR DELETE TO authenticated
    USING (
        published_by IN (
            SELECT p.id FROM public.profiles p WHERE p.auth_user_id = auth.uid()
        )
    );

DROP POLICY IF EXISTS "Admins can delete all notices" ON public.notices;
CREATE POLICY "Admins can delete all notices" ON public.notices
    FOR DELETE TO authenticated
    USING (
        EXISTS (
            SELECT 1
            FROM public.user_roles ur
            JOIN public.roles r ON r.id = ur.role_id
            JOIN public.profiles p ON p.id = ur.user_id
            WHERE p.auth_user_id = auth.uid()
              AND COALESCE(p.status, 'ACTIVE') = 'ACTIVE'
              AND r.name IN ('ADMIN', 'SUPER_ADMIN')
        )
    );
