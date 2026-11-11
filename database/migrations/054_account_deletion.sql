-- ==============================================================================
-- Migration: 054_account_deletion.sql
-- Project: બોદલા પરિવાર (Bodla Parivar)
-- Description: Account deletion support — soft delete profiles and related user data
-- ==============================================================================

-- 1. Add deletion tracking columns to profiles
ALTER TABLE public.profiles
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS delete_account_requested_at TIMESTAMPTZ;

-- Index for faster lookup of active (non-deleted) profiles
CREATE INDEX IF NOT EXISTS idx_profiles_status_deleted_at
    ON public.profiles (status, deleted_at);

-- 2. Account deletion RPC function — soft deletes profile and related user data
CREATE OR REPLACE FUNCTION public.delete_account()
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_profile_id UUID;
    v_auth_user_id UUID;
BEGIN
    -- Get the current authenticated user's profile
    SELECT p.id, p.auth_user_id
      INTO v_profile_id, v_auth_user_id
      FROM public.profiles p
     WHERE p.auth_user_id = auth.uid()
       AND COALESCE(p.status, 'ACTIVE') = 'ACTIVE';

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Active profile not found for current user';
    END IF;

    -- Log the deletion in audit_logs before soft-deleting
    INSERT INTO public.audit_logs (user_id, action, entity_type, entity_id, old_data, new_data, created_at)
    VALUES (
        v_profile_id,
        'DELETE_ACCOUNT',
        'profile',
        v_profile_id,
        jsonb_build_object('status', COALESCE((SELECT status FROM public.profiles WHERE id = v_profile_id), 'ACTIVE')),
        jsonb_build_object('status', 'DELETED', 'deleted_at', now()),
        now()
    );

    -- Soft-delete the profile
    UPDATE public.profiles
       SET status = 'DELETED',
           deleted_at = now(),
           delete_account_requested_at = now(),
           updated_at = now()
     WHERE id = v_profile_id;

    -- Revoke device tokens for this user
    UPDATE public.device_tokens
       SET active = FALSE,
           updated_at = now()
     WHERE user_id = v_profile_id;

    -- Mark user's complaints as deleted (keep for audit but hide from active view)
    UPDATE public.complaints
       SET status = 'CLOSED',
           resolution_note = 'Account deleted by user',
           updated_at = now()
     WHERE user_id = v_profile_id
       AND status NOT IN ('RESOLVED', 'REJECTED', 'CLOSED');

    -- Mark user's offerings as cancelled
    UPDATE public.offerings
       SET status = 'CANCELLED',
           updated_at = now()
     WHERE created_by = v_profile_id
       AND status NOT IN ('COMPLETED', 'CANCELLED', 'REJECTED');

    -- Mark user's notices as archived
    UPDATE public.notices
       SET status = 'ARCHIVED',
           updated_at = now()
     WHERE published_by = v_profile_id
       AND status NOT IN ('ARCHIVED', 'EXPIRED');

END;
$$;

REVOKE ALL ON FUNCTION public.delete_account() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.delete_account() TO authenticated;

-- Notify PostgREST to reload schema
NOTIFY pgrst, 'reload schema';

-- 3. Update RLS policies to exclude DELETED profiles from public view
DROP POLICY IF EXISTS "Profiles are viewable by owner or admin" ON public.profiles;
CREATE POLICY "Profiles are viewable by owner or admin"
    ON public.profiles FOR SELECT
    TO authenticated
    USING (
        auth_user_id = (SELECT auth.uid())
        OR public.is_admin()
        OR (COALESCE(status, 'ACTIVE') <> 'DELETED')
    );

DROP POLICY IF EXISTS "Users can update own profile" ON public.profiles;
CREATE POLICY "Users can update own profile"
    ON public.profiles FOR UPDATE
    TO authenticated
    USING (
        auth_user_id = (SELECT auth.uid())
        AND COALESCE(status, 'ACTIVE') <> 'DELETED'
    )
    WITH CHECK (
        auth_user_id = (SELECT auth.uid())
    );
