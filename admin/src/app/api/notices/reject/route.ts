import { NextRequest, NextResponse } from 'next/server';
import { createClient } from '@supabase/supabase-js';

// Reject / Delete a pending notice
export async function POST(request: NextRequest) {
  const supabaseUrl = process.env.NEXT_PUBLIC_SUPABASE_URL;
  const serviceRoleKey = process.env.SUPABASE_SERVICE_ROLE_KEY;

  if (!supabaseUrl || !serviceRoleKey) {
    return NextResponse.json({ error: 'Supabase configuration missing' }, { status: 500 });
  }

  let noticeId: string | undefined;
  try {
    const body = await request.json();
    noticeId = body.noticeId;
  } catch {
    return NextResponse.json({ error: 'Invalid JSON request body' }, { status: 400 });
  }

  if (!noticeId) {
    return NextResponse.json({ error: 'noticeId is required' }, { status: 400 });
  }

  const supabase = createClient(supabaseUrl, serviceRoleKey, {
    auth: { persistSession: false, autoRefreshToken: false },
  });

  // 1. Fetch notice media URLs to clean up storage if present
  const { data: notice } = await supabase
    .from('notices')
    .select('image_url, attachment_url')
    .eq('id', noticeId)
    .maybeSingle();

  // 2. Delete notice from notices table
  const { error: deleteError } = await supabase
    .from('notices')
    .delete()
    .eq('id', noticeId);

  if (deleteError) {
    console.error('Reject/Delete error:', deleteError);
    return NextResponse.json({ error: deleteError.message }, { status: 500 });
  }

  // 3. Clean up storage files if present
  if (notice) {
    const bucketMarkers = [
      { bucket: 'notices', marker: '/storage/v1/object/public/notices/' },
      { bucket: 'antim-yatra-posters', marker: '/storage/v1/object/public/antim-yatra-posters/' },
    ];
    for (const url of [notice.image_url, notice.attachment_url]) {
      if (!url) continue;
      for (const { bucket, marker } of bucketMarkers) {
        const markerIndex = url.indexOf(marker);
        if (markerIndex < 0) continue;
        const path = decodeURIComponent(url.slice(markerIndex + marker.length).split('?')[0]);
        await supabase.storage.from(bucket).remove([path]);
        break;
      }
    }
  }

  return NextResponse.json({ success: true, message: 'Notice rejected and deleted.' });
}
