BEGIN;

CREATE OR REPLACE FUNCTION public.is_current_user_offering_winner(p_offering_id UUID)
RETURNS BOOLEAN
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
  SELECT auth.uid() IS NOT NULL
    AND EXISTS (
      SELECT 1
      FROM public.offering_winner_private wp
      JOIN public.profiles p ON p.id = wp.winner_profile_id
      WHERE wp.offering_id = p_offering_id
        AND p.auth_user_id = auth.uid()
    );
$$;

REVOKE ALL ON FUNCTION public.is_current_user_offering_winner(UUID) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.is_current_user_offering_winner(UUID) TO authenticated;

NOTIFY pgrst, 'reload schema';

COMMIT;