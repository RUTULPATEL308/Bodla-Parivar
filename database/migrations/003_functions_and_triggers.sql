-- ==============================================================================
-- Migration: 003_functions_and_triggers.sql
-- Project: બોદલા પરિવાર (Bodla Parivar)
-- Description: Automated updated_at triggers, auth sync, and RBAC utility functions
-- ==============================================================================

-- 1. Automatic updated_at trigger function
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply updated_at triggers
DO $$
DECLARE
    t text;
BEGIN
    FOR t IN
        SELECT table_name
        FROM information_schema.columns
        WHERE column_name = 'updated_at'
          AND table_schema = 'public'
    LOOP
        EXECUTE format('
            DROP TRIGGER IF EXISTS trg_update_timestamp ON %I;
            CREATE TRIGGER trg_update_timestamp
            BEFORE UPDATE ON %I
            FOR EACH ROW
            EXECUTE FUNCTION update_updated_at_column();
        ', t, t);
    END LOOP;
END;
$$;

-- 2. Auth user creation hook (creates profile on Supabase auth.users signup)
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
DECLARE
    default_name TEXT;
    default_lang TEXT;
BEGIN
    default_name := COALESCE(NEW.raw_user_meta_data->>'full_name', NEW.email, 'Bodla Parivar Member');
    default_lang := COALESCE(NEW.raw_user_meta_data->>'preferred_language', 'gu');

    INSERT INTO public.profiles (
        auth_user_id,
        full_name,
        email,
        phone,
        preferred_language,
        status,
        is_verified
    ) VALUES (
        NEW.id,
        default_name,
        NEW.email,
        NEW.phone,
        default_lang,
        'ACTIVE',
        FALSE
    )
    ON CONFLICT (auth_user_id) DO NOTHING;

    -- Assign default USER role
    INSERT INTO public.user_roles (user_id, role_id)
    SELECT p.id, r.id
    FROM public.profiles p, public.roles r
    WHERE p.auth_user_id = NEW.id AND r.name = 'USER'
    ON CONFLICT DO NOTHING;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Trigger on auth.users
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- 3. RBAC Helper Functions
CREATE OR REPLACE FUNCTION public.current_profile_id()
RETURNS UUID AS $$
    SELECT id FROM public.profiles WHERE auth_user_id = auth.uid();
$$ LANGUAGE sql STABLE SECURITY DEFINER;

CREATE OR REPLACE FUNCTION public.has_role(target_user_id UUID, role_name TEXT)
RETURNS BOOLEAN AS $$
    SELECT EXISTS (
        SELECT 1
        FROM public.user_roles ur
        JOIN public.roles r ON ur.role_id = r.id
        WHERE ur.user_id = target_user_id AND r.name = role_name
    );
$$ LANGUAGE sql STABLE SECURITY DEFINER;

CREATE OR REPLACE FUNCTION public.is_admin()
RETURNS BOOLEAN AS $$
    SELECT EXISTS (
        SELECT 1
        FROM public.user_roles ur
        JOIN public.roles r ON ur.role_id = r.id
        JOIN public.profiles p ON ur.user_id = p.id
        WHERE p.auth_user_id = auth.uid()
          AND r.name IN ('ADMIN', 'SUPER_ADMIN')
    );
$$ LANGUAGE sql STABLE SECURITY DEFINER;

CREATE OR REPLACE FUNCTION public.is_staff()
RETURNS BOOLEAN AS $$
    SELECT EXISTS (
        SELECT 1
        FROM public.user_roles ur
        JOIN public.roles r ON ur.role_id = r.id
        JOIN public.profiles p ON ur.user_id = p.id
        WHERE p.auth_user_id = auth.uid()
          AND r.name IN ('ADMIN', 'SUPER_ADMIN', 'MODERATOR', 'CONTENT_MANAGER')
    );
$$ LANGUAGE sql STABLE SECURITY DEFINER;
