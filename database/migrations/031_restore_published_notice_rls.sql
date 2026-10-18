-- Restore the intended public notice visibility after migration 026 relaxed it.
-- Public and resident clients may read only notices that are currently published.
-- Staff and authorized notice managers retain access through their separate policy.
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

-- Rollback: this migration repairs a permissive policy introduced by 026.
-- Reverting to USING (true) would expose draft and expired notices, so no
-- automatic rollback is provided. Restore the previous published-only policy
-- shown above if this migration needs to be reapplied after a manual rollback.
