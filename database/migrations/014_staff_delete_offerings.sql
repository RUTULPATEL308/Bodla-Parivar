-- Explicitly authorize staff to permanently delete offerings.
ALTER TABLE public.offerings ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Staff can delete offerings" ON public.offerings;
CREATE POLICY "Staff can delete offerings"
    ON public.offerings FOR DELETE
    TO authenticated
    USING (public.is_staff());