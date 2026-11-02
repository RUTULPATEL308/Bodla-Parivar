-- Run after migration 034. Catalog assertions; does not create resident data.
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_class c
    JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE n.nspname = 'public' AND c.relname = 'result_uploads' AND c.relrowsecurity
  ) THEN
    RAISE EXCEPTION 'RLS must be enabled on result_uploads';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_policies
    WHERE schemaname = 'public' AND tablename = 'result_uploads'
      AND cmd = 'INSERT' AND qual IS NULL
      AND with_check ILIKE '%result_upload_enabled%'
  ) THEN
    RAISE EXCEPTION 'Result submissions must require the Result Upload switch';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_policies
    WHERE schemaname = 'storage' AND tablename = 'objects'
      AND policyname = 'Residents upload own results while enabled'
      AND with_check ILIKE '%result_upload_enabled%'
  ) THEN
    RAISE EXCEPTION 'Result documents must require the Result Upload switch';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM storage.buckets WHERE id = 'result-uploads' AND public = FALSE) THEN
    RAISE EXCEPTION 'Result upload bucket must remain private';
  END IF;

  IF has_table_privilege('anon', 'public.result_uploads', 'SELECT') THEN
    RAISE EXCEPTION 'Anonymous users must not read result submissions';
  END IF;
END;
$$;
