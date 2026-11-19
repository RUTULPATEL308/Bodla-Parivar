import { NextRequest, NextResponse } from 'next/server';
import { createClient } from '@supabase/supabase-js';

/**
 * Delete a notice (pending or any status) by its ID.
 * This endpoint is used by admin interfaces to remove a pending notice completely.
 */
export async function POST(request: NextRequest) {
  // This endpoint already exists and does not need further changes.
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

  // Delete the notice record.
  // Existing implementation unchanged.
  const { error } = await supabase.from('notices').delete().eq('id', noticeId);

  if (error) {
    console.error('Delete notice error:', error);
    return NextResponse.json({ error: error.message }, { status: 500 });
  }

  return NextResponse.json({ success: true, noticeId });
}
