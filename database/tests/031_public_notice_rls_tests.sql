-- Run after migration 031 on local/staging. Verifies policy predicates only.
DO $$
DECLARE
  public_read_predicate TEXT;
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_class c
    JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE n.nspname = 'public' AND c.relname = 'notices' AND c.relrowsecurity
  ) THEN
    RAISE EXCEPTION 'RLS must be enabled on public.notices';
  END IF;

  SELECT qual INTO public_read_predicate
  FROM pg_policies
  WHERE schemaname = 'public'
    AND tablename = 'notices'
    AND policyname = 'Published notices are viewable by all'
    AND cmd = 'SELECT';

  IF public_read_predicate IS NULL
     OR public_read_predicate NOT ILIKE '%status%PUBLISHED%'
     OR public_read_predicate NOT ILIKE '%publish_at%'
     OR public_read_predicate NOT ILIKE '%expires_at%' THEN
    RAISE EXCEPTION 'Public notice policy must require current published status and publication window; found: %', public_read_predicate;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_policies
    WHERE schemaname = 'public'
      AND tablename = 'notices'
      AND policyname = 'Staff can manage notices'
      AND cmd = 'ALL'
  ) THEN
    RAISE EXCEPTION 'Staff notice management policy is missing';
  END IF;
END;
$$;
