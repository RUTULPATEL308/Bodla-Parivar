-- Repair projects where Auth users exist but profile rows were never created.
INSERT INTO public.roles (name, description)
VALUES ('USER', 'Registered village community member')
ON CONFLICT (name) DO NOTHING;

CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    default_name text;
    default_lang text;
BEGIN
    default_name := COALESCE(
        NULLIF(NEW.raw_user_meta_data->>'full_name', ''),
        NULLIF(NEW.email, ''),
        'Bodla Parivar Member'
    );
    default_lang := CASE
        WHEN NEW.raw_user_meta_data->>'preferred_language' = 'en' THEN 'en'
        ELSE 'gu'
    END;

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
    ON CONFLICT (auth_user_id) DO UPDATE
        SET email = EXCLUDED.email,
            phone = COALESCE(EXCLUDED.phone, public.profiles.phone);

    INSERT INTO public.user_roles (user_id, role_id)
    SELECT p.id, r.id
    FROM public.profiles p
    JOIN public.roles r ON r.name = 'USER'
    WHERE p.auth_user_id = NEW.id
    ON CONFLICT (user_id, role_id) DO NOTHING;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

INSERT INTO public.profiles (
    auth_user_id,
    full_name,
    email,
    phone,
    preferred_language,
    status,
    is_verified
)
SELECT
    u.id,
    COALESCE(
        NULLIF(u.raw_user_meta_data->>'full_name', ''),
        NULLIF(u.email, ''),
        'Bodla Parivar Member'
    ),
    u.email,
    u.phone,
    CASE
        WHEN u.raw_user_meta_data->>'preferred_language' = 'en' THEN 'en'
        ELSE 'gu'
    END,
    'ACTIVE',
    FALSE
FROM auth.users u
ON CONFLICT (auth_user_id) DO UPDATE
    SET email = EXCLUDED.email,
        phone = COALESCE(EXCLUDED.phone, public.profiles.phone);

INSERT INTO public.user_roles (user_id, role_id)
SELECT p.id, r.id
FROM public.profiles p
JOIN public.roles r ON r.name = 'USER'
ON CONFLICT (user_id, role_id) DO NOTHING;

NOTIFY pgrst, 'reload schema';