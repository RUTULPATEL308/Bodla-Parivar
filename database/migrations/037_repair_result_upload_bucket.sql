-- Repair the private Result Upload bucket and its policies if migration 034's
-- Storage statements were skipped or the bucket was removed from the dashboard.
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
    'result-uploads',
    'result-uploads',
    FALSE,
    10485760,
    ARRAY['image/jpeg', 'image/png', 'image/webp', 'application/pdf']
)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    public = FALSE,
    file_size_limit = EXCLUDED.file_size_limit,
    allowed_mime_types = EXCLUDED.allowed_mime_types;

DROP POLICY IF EXISTS "Residents upload own results while enabled" ON storage.objects;
CREATE POLICY "Residents upload own results while enabled" ON storage.objects
    FOR INSERT TO authenticated
    WITH CHECK (
        bucket_id = 'result-uploads'
        AND (storage.foldername(name))[1] = auth.uid()::text
        AND EXISTS (
            SELECT 1 FROM public.village_settings
            WHERE result_upload_enabled = TRUE
        )
    );

DROP POLICY IF EXISTS "Residents remove own result files" ON storage.objects;
CREATE POLICY "Residents remove own result files" ON storage.objects
    FOR DELETE TO authenticated
    USING (
        bucket_id = 'result-uploads'
        AND (storage.foldername(name))[1] = auth.uid()::text
    );

DROP POLICY IF EXISTS "Staff access result upload files" ON storage.objects;
CREATE POLICY "Staff access result upload files" ON storage.objects
    FOR SELECT TO authenticated
    USING (bucket_id = 'result-uploads' AND public.is_staff());
