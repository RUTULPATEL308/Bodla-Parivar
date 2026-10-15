-- Ensure the mobile app and public portal can read currently published notices.
ALTER TABLE public.notices ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Published notices are viewable by all" ON public.notices;
CREATE POLICY "Published notices are viewable by all"
    ON public.notices FOR SELECT
    TO anon, authenticated
    USING (
        status = 'PUBLISHED'
        AND (publish_at IS NULL OR publish_at <= NOW())
        AND (expires_at IS NULL OR expires_at > NOW())
    );

GRANT SELECT ON public.notices TO anon, authenticated;
GRANT SELECT ON public.notice_categories TO anon, authenticated;

-- Ensure staff can update (archive) and delete notices through the admin app.
DROP POLICY IF EXISTS "Staff can manage notices" ON public.notices;
CREATE POLICY "Staff can manage notices"
    ON public.notices FOR ALL
    TO authenticated
    USING (public.is_staff())
    WITH CHECK (public.is_staff());

GRANT INSERT, UPDATE, DELETE ON public.notices TO authenticated;
