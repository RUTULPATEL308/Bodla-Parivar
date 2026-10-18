-- Run after migration 032 on local/staging. Catalog-only assertions; no data writes.
DO $$
DECLARE
  has_unrestricted_insert_policy BOOLEAN;
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_class c
    JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE n.nspname = 'public' AND c.relname = 'audit_logs' AND c.relrowsecurity
  ) THEN
    RAISE EXCEPTION 'RLS must be enabled on public.audit_logs';
  END IF;

  SELECT EXISTS (
    SELECT 1 FROM pg_policies
    WHERE schemaname = 'public'
      AND tablename = 'audit_logs'
      AND cmd IN ('INSERT', 'ALL')
      AND (roles @> ARRAY['authenticated']::name[] OR roles @> ARRAY['public']::name[])
  ) INTO has_unrestricted_insert_policy;
  IF has_unrestricted_insert_policy THEN
    RAISE EXCEPTION 'Authenticated/public INSERT policy still exists on audit_logs';
  END IF;

  IF has_table_privilege('anon', 'public.audit_logs', 'INSERT') THEN
    RAISE EXCEPTION 'anon still has INSERT privilege on audit_logs';
  END IF;
  IF has_table_privilege('authenticated', 'public.audit_logs', 'INSERT') THEN
    RAISE EXCEPTION 'authenticated still has INSERT privilege on audit_logs';
  END IF;
END;
$$;
