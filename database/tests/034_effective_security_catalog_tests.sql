-- Run as a database administrator after migrations 031, 032, and 033 on an
-- isolated local or staging project. This script only reads system catalogs.
DO $$
DECLARE
  unsafe_notice_policy TEXT;
  unsafe_definer_functions TEXT;
BEGIN
  -- Migration 031 must supersede migration 026's USING (true) policy.
  SELECT string_agg(policyname, ', ' ORDER BY policyname)
    INTO unsafe_notice_policy
  FROM pg_policies
  WHERE schemaname = 'public'
    AND tablename = 'notices'
    AND cmd IN ('SELECT', 'ALL')
    AND COALESCE(qual, '') ~* '^\\(?true\\)?$';
  IF unsafe_notice_policy IS NOT NULL THEN
    RAISE EXCEPTION 'Unrestricted public.notices policy remains: %', unsafe_notice_policy;
  END IF;

  -- Migration 032 must leave resident roles unable to forge audit history.
  IF has_table_privilege('anon', 'public.audit_logs', 'INSERT')
     OR has_table_privilege('authenticated', 'public.audit_logs', 'INSERT') THEN
    RAISE EXCEPTION 'anon/authenticated still have INSERT on public.audit_logs';
  END IF;

  -- SECURITY DEFINER routines in public must explicitly set search_path.
  SELECT string_agg(p.oid::regprocedure::text, ', ' ORDER BY p.oid::regprocedure::text)
    INTO unsafe_definer_functions
  FROM pg_proc p
  JOIN pg_namespace n ON n.oid = p.pronamespace
  WHERE n.nspname = 'public'
    AND p.prosecdef
    AND NOT EXISTS (
      SELECT 1 FROM unnest(COALESCE(p.proconfig, ARRAY[]::text[])) AS setting
      WHERE setting LIKE 'search_path=%'
    );
  IF unsafe_definer_functions IS NOT NULL THEN
    RAISE EXCEPTION 'SECURITY DEFINER function(s) missing explicit search_path: %', unsafe_definer_functions;
  END IF;

  -- Suspension enforcement created by migration 033 must still be present.
  IF EXISTS (
    SELECT 1
    FROM (VALUES
      ('public.is_staff()'::regprocedure),
      ('public.is_admin()'::regprocedure),
      ('public.current_user_roles()'::regprocedure)
    ) AS required_routine(routine)
    WHERE pg_get_functiondef(routine) !~* 'status'
       OR pg_get_functiondef(routine) !~* 'ACTIVE'
  ) THEN
    RAISE EXCEPTION 'A role routine no longer rejects suspended profiles';
  END IF;
END;
$$;

-- Save this result with the test run. It proves the effective deployed policy
-- and grant state, including which roles have access; it does not reveal data.
SELECT schemaname, tablename, policyname, roles, cmd, qual, with_check
FROM pg_policies
WHERE schemaname IN ('public', 'storage')
  AND tablename IN ('notices', 'audit_logs', 'profiles', 'offerings', 'offering_bids', 'emergency_contacts', 'objects')
ORDER BY schemaname, tablename, policyname;
