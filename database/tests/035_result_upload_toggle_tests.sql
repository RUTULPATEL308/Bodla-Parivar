-- Run after migration 035. Catalog assertions; no settings rows are changed.
DO $$
DECLARE
  toggle_definition TEXT;
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_policies
    WHERE schemaname = 'public' AND tablename = 'village_settings'
      AND policyname = 'Staff insert village settings'
      AND cmd = 'INSERT' AND with_check ILIKE '%is_staff%'
  ) THEN
    RAISE EXCEPTION 'Staff INSERT policy must explicitly check is_staff()';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_policies
    WHERE schemaname = 'public' AND tablename = 'village_settings'
      AND policyname = 'Staff update village settings'
      AND cmd = 'UPDATE' AND qual ILIKE '%is_staff%'
      AND with_check ILIKE '%is_staff%'
  ) THEN
    RAISE EXCEPTION 'Staff UPDATE policy must explicitly check is_staff()';
  END IF;

  SELECT pg_get_functiondef('public.set_result_upload_enabled(boolean)'::regprocedure)
  INTO toggle_definition;
  IF toggle_definition NOT ILIKE '%is_staff%' THEN
    RAISE EXCEPTION 'Result Upload toggle RPC must check staff access';
  END IF;
END;
$$;
