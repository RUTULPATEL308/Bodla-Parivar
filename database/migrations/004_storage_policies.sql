-- ==============================================================================
-- Migration: 004_storage_policies.sql
-- Project: બોદલા પરિવાર (Bodla Parivar)
-- Description: Supabase Storage buckets, size limits, MIME validations, and RLS
-- ==============================================================================

-- Create Storage Buckets
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES 
    ('profiles', 'profiles', true, 5242880, ARRAY['image/jpeg', 'image/png', 'image/webp']),
    ('notices', 'notices', true, 10485760, ARRAY['image/jpeg', 'image/png', 'image/webp', 'application/pdf']),
    ('businesses', 'businesses', true, 8388608, ARRAY['image/jpeg', 'image/png', 'image/webp']),
    ('offerings', 'offerings', true, 8388608, ARRAY['image/jpeg', 'image/png', 'image/webp']),
    ('complaints', 'complaints', false, 10485760, ARRAY['image/jpeg', 'image/png', 'image/webp']),
    ('gallery', 'gallery', true, 15728640, ARRAY['image/jpeg', 'image/png', 'image/webp']),
    ('places', 'places', true, 8388608, ARRAY['image/jpeg', 'image/png', 'image/webp'])
ON CONFLICT (id) DO UPDATE SET
    public = EXCLUDED.public,
    file_size_limit = EXCLUDED.file_size_limit,
    allowed_mime_types = EXCLUDED.allowed_mime_types;

-- Storage Policies: Public Read for public content
CREATE POLICY "Public Read Profiles"
    ON storage.objects FOR SELECT
    TO anon, authenticated
    USING (bucket_id = 'profiles');

CREATE POLICY "Public Read Notices"
    ON storage.objects FOR SELECT
    TO anon, authenticated
    USING (bucket_id = 'notices');

CREATE POLICY "Public Read Businesses"
    ON storage.objects FOR SELECT
    TO anon, authenticated
    USING (bucket_id = 'businesses');

CREATE POLICY "Public Read Offerings"
    ON storage.objects FOR SELECT
    TO anon, authenticated
    USING (bucket_id = 'offerings');

CREATE POLICY "Public Read Gallery"
    ON storage.objects FOR SELECT
    TO anon, authenticated
    USING (bucket_id = 'gallery');

CREATE POLICY "Public Read Places"
    ON storage.objects FOR SELECT
    TO anon, authenticated
    USING (bucket_id = 'places');

-- Complaints photos are private: only uploader or staff can read
CREATE POLICY "Complaint Photo Access"
    ON storage.objects FOR SELECT
    TO authenticated
    USING (
        bucket_id = 'complaints'
        AND (
            (storage.foldername(name))[1] = auth.uid()::text
            OR public.is_staff()
        )
    );

-- User Upload Policy: authenticated users can upload within their user ID folder
CREATE POLICY "User Upload Profile Photo"
    ON storage.objects FOR INSERT
    TO authenticated
    WITH CHECK (
        bucket_id = 'profiles'
        AND (storage.foldername(name))[1] = auth.uid()::text
    );

CREATE POLICY "User Upload Complaint Photo"
    ON storage.objects FOR INSERT
    TO authenticated
    WITH CHECK (
        bucket_id = 'complaints'
        AND (storage.foldername(name))[1] = auth.uid()::text
    );

CREATE POLICY "Staff Upload Everything"
    ON storage.objects FOR ALL
    TO authenticated
    USING (public.is_staff())
    WITH CHECK (public.is_staff());
