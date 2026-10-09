-- Include deleted bid fields in Realtime payloads so clients can remove rows immediately.
ALTER TABLE offering_bids REPLICA IDENTITY FULL;

-- Define the role check here as well so this migration works on databases where
-- the helper-function migration has not been applied yet.
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
    JOIN public.roles r ON ur.role_id = r.id
    JOIN public.profiles p ON ur.user_id = p.id
    WHERE p.auth_user_id = auth.uid()
      AND r.name IN ('ADMIN', 'SUPER_ADMIN', 'MODERATOR', 'CONTENT_MANAGER')
  );
$$;

-- Replace the broad authenticated policy with the existing staff role check.
DROP POLICY IF EXISTS "Authenticated can manage bids" ON offering_bids;
DROP POLICY IF EXISTS "Staff can manage bids" ON offering_bids;
CREATE POLICY "Staff can manage bids" ON offering_bids
  FOR ALL TO authenticated
  USING (public.is_staff())
  WITH CHECK (public.is_staff());