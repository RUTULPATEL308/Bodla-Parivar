-- 057_notice_pending_approval_policy.sql
-- Allow staff (ADMIN, SUPER_ADMIN, MODERATOR, CONTENT_MANAGER) to read all notice statuses
-- so the pending notices approval panel works in the admin portal.

DROP POLICY IF EXISTS "Staff can read all notices" ON public.notices;
CREATE POLICY "Staff can read all notices"
    ON public.notices FOR SELECT
    TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM public.profiles p
            JOIN public.user_roles ur ON ur.user_id = p.id
            JOIN public.roles r ON r.id = ur.role_id
            WHERE p.auth_user_id = auth.uid()
              AND p.status = 'ACTIVE'
              AND r.name IN ('ADMIN', 'SUPER_ADMIN', 'MODERATOR', 'CONTENT_MANAGER')
        )
    );

-- Allow notice authors to read their own PENDING notices (so they can see it was submitted)
DROP POLICY IF EXISTS "Authors can read their own pending notices" ON public.notices;
CREATE POLICY "Authors can read their own pending notices"
    ON public.notices FOR SELECT
    TO authenticated
    USING (
        published_by = (
            SELECT id FROM public.profiles WHERE auth_user_id = auth.uid() LIMIT 1
        )
    );
