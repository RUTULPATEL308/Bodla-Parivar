-- Allow any authenticated user to upload broadcast media to the notices bucket.
-- (Staff already have full access via "Staff Upload Everything".)
DROP POLICY IF EXISTS "Authenticated Upload Notices" ON storage.objects;
CREATE POLICY "Authenticated Upload Notices"
    ON storage.objects FOR INSERT
    TO authenticated
    WITH CHECK (
        bucket_id = 'notices'
        AND (storage.foldername(name))[1] = auth.uid()::text
    );

-- Allow uploader to update/delete their own files in notices bucket
DROP POLICY IF EXISTS "Authenticated Update Own Notices" ON storage.objects;
CREATE POLICY "Authenticated Update Own Notices"
    ON storage.objects FOR UPDATE
    TO authenticated
    USING (
        bucket_id = 'notices'
        AND (storage.foldername(name))[1] = auth.uid()::text
    )
    WITH CHECK (
        bucket_id = 'notices'
        AND (storage.foldername(name))[1] = auth.uid()::text
    );

DROP POLICY IF EXISTS "Authenticated Delete Own Notices" ON storage.objects;
CREATE POLICY "Authenticated Delete Own Notices"
    ON storage.objects FOR DELETE
    TO authenticated
    USING (
        bucket_id = 'notices'
        AND (storage.foldername(name))[1] = auth.uid()::text
    );