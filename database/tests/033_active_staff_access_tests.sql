-- Run after migration 033 on local/staging. Verifies suspension guards exist.
DO $$
DECLARE
  staff_definition TEXT;
  admin_definition TEXT;
  roles_definition TEXT;
BEGIN
  SELECT pg_get_functiondef('public.is_staff()'::regprocedure) INTO staff_definition;
  SELECT pg_get_functiondef('public.is_admin()'::regprocedure) INTO admin_definition;
  SELECT pg_get_functiondef('public.current_user_roles()'::regprocedure) INTO roles_definition;

  IF staff_definition NOT ILIKE '%status%' OR staff_definition NOT ILIKE '%ACTIVE%' THEN
    RAISE EXCEPTION 'is_staff() must reject inactive profiles';
  END IF;
  IF admin_definition NOT ILIKE '%status%' OR admin_definition NOT ILIKE '%ACTIVE%' THEN
    RAISE EXCEPTION 'is_admin() must reject inactive profiles';
  END IF;
  IF roles_definition NOT ILIKE '%status%' OR roles_definition NOT ILIKE '%ACTIVE%' THEN
    RAISE EXCEPTION 'current_user_roles() must hide roles for inactive profiles';
  END IF;
END;
$$;
