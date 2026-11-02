-- Resident student result uploads with an administrator-controlled feature switch.
ALTER TABLE public.village_settings
    ADD COLUMN IF NOT EXISTS result_upload_enabled BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS public.result_uploads (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    student_name TEXT NOT NULL CHECK (char_length(trim(student_name)) BETWEEN 2 AND 120),
    standard TEXT NOT NULL CHECK (char_length(trim(standard)) BETWEEN 1 AND 40),
    school_name TEXT NOT NULL CHECK (char_length(trim(school_name)) BETWEEN 2 AND 160),
    file_path TEXT NOT NULL,
    file_name TEXT NOT NULL,
    content_type TEXT NOT NULL CHECK (content_type IN ('image/jpeg', 'image/png', 'image/webp', 'application/pdf')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_result_uploads_created_at ON public.result_uploads (created_at DESC);
ALTER TABLE public.result_uploads ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.result_uploads FROM anon, authenticated;
GRANT SELECT, INSERT ON public.result_uploads TO authenticated;

DROP POLICY IF EXISTS "Staff read result uploads" ON public.result_uploads;
CREATE POLICY "Staff read result uploads" ON public.result_uploads
    FOR SELECT TO authenticated USING (public.is_staff());

DROP POLICY IF EXISTS "Residents submit results while enabled" ON public.result_uploads;
CREATE POLICY "Residents submit results while enabled" ON public.result_uploads
    FOR INSERT TO authenticated
    WITH CHECK (
        user_id = public.current_profile_id()
        AND EXISTS (SELECT 1 FROM public.village_settings WHERE result_upload_enabled = TRUE)
    );

INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES ('result-uploads', 'result-uploads', FALSE, 10485760,
        ARRAY['image/jpeg', 'image/png', 'image/webp', 'application/pdf'])
ON CONFLICT (id) DO UPDATE SET public = FALSE, file_size_limit = 10485760,
    allowed_mime_types = EXCLUDED.allowed_mime_types;

DROP POLICY IF EXISTS "Residents upload own results while enabled" ON storage.objects;
CREATE POLICY "Residents upload own results while enabled" ON storage.objects
    FOR INSERT TO authenticated
    WITH CHECK (
        bucket_id = 'result-uploads'
        AND (storage.foldername(name))[1] = auth.uid()::text
        AND EXISTS (SELECT 1 FROM public.village_settings WHERE result_upload_enabled = TRUE)
    );

DROP POLICY IF EXISTS "Residents remove own result files" ON storage.objects;
CREATE POLICY "Residents remove own result files" ON storage.objects
    FOR DELETE TO authenticated
    USING (bucket_id = 'result-uploads' AND (storage.foldername(name))[1] = auth.uid()::text);

DROP POLICY IF EXISTS "Staff access result upload files" ON storage.objects;
CREATE POLICY "Staff access result upload files" ON storage.objects
    FOR SELECT TO authenticated
    USING (bucket_id = 'result-uploads' AND public.is_staff());

COMMENT ON TABLE public.result_uploads IS 'Private student result documents submitted by residents; staff-only reads.';
