-- ==============================================================================
-- Migration: 026_relax_notice_rls.sql
-- Description: Relax RLS policy on public.notices to ensure all published notices sync properly
-- ==============================================================================

ALTER TABLE public.notices ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Published notices are viewable by all" ON public.notices;
CREATE POLICY "Published notices are viewable by all"
    ON public.notices FOR SELECT
    TO anon, authenticated
    USING (true);

GRANT SELECT ON public.notices TO anon, authenticated;
GRANT SELECT ON public.notice_categories TO anon, authenticated;
