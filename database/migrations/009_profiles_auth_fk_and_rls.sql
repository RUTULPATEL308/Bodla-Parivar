-- Preserve profiles.id as the application's internal profile key while
-- linking auth_user_id to Supabase Auth, which is the identity key.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint c
        WHERE c.conrelid = 'public.profiles'::regclass
          AND c.confrelid = 'auth.users'::regclass
          AND c.contype = 'f'
          AND EXISTS (
              SELECT 1
              FROM unnest(c.conkey) AS key_column(attnum)
              JOIN pg_attribute a
                ON a.attrelid = c.conrelid
               AND a.attnum = key_column.attnum
              WHERE a.attname = 'auth_user_id'
          )
    ) THEN
        ALTER TABLE public.profiles
            ADD CONSTRAINT profiles_auth_user_id_fkey
            FOREIGN KEY (auth_user_id)
            REFERENCES auth.users (id)
            ON DELETE CASCADE;
    END IF;
END;
$$;

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Public profiles are viewable by authenticated users" ON public.profiles;
DROP POLICY IF EXISTS "Profiles are viewable by owner or admin" ON public.profiles;
CREATE POLICY "Profiles are viewable by owner or admin"
    ON public.profiles FOR SELECT
    TO authenticated
    USING (auth_user_id = (SELECT auth.uid()) OR public.is_admin());

DROP POLICY IF EXISTS "Users can update own profile" ON public.profiles;
CREATE POLICY "Users can update own profile"
    ON public.profiles FOR UPDATE
    TO authenticated
    USING (auth_user_id = (SELECT auth.uid()))
    WITH CHECK (auth_user_id = (SELECT auth.uid()));