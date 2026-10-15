-- Public clients can read active emergency contacts; staff manage the directory.
ALTER TABLE public.emergency_contacts ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Active emergency contacts are viewable by all" ON public.emergency_contacts;
CREATE POLICY "Active emergency contacts are viewable by all"
    ON public.emergency_contacts FOR SELECT
    TO anon, authenticated
    USING (is_active = TRUE);

DROP POLICY IF EXISTS "Staff can manage emergency contacts" ON public.emergency_contacts;
CREATE POLICY "Staff can manage emergency contacts"
    ON public.emergency_contacts FOR ALL
    TO authenticated
    USING (public.is_staff())
    WITH CHECK (public.is_staff());

GRANT SELECT ON public.emergency_contacts TO anon, authenticated;
GRANT INSERT, UPDATE, DELETE ON public.emergency_contacts TO authenticated;
