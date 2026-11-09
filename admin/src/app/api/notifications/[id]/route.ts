import { NextRequest, NextResponse } from 'next/server';
import { createClient } from '@supabase/supabase-js';

export async function DELETE(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  try {
    const supabaseUrl = process.env.NEXT_PUBLIC_SUPABASE_URL;
    const anonKey = process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY;
    const serviceRoleKey = process.env.SUPABASE_SERVICE_ROLE_KEY;
    if (!supabaseUrl || !anonKey || !serviceRoleKey) {
      return NextResponse.json({ error: 'Server configuration is missing.' }, { status: 500 });
    }

    const accessToken = request.headers.get('authorization')?.replace(/^Bearer\s+/i, '');
    if (!accessToken) {
      return NextResponse.json({ error: 'Sign in to delete notifications.' }, { status: 401 });
    }

    const authClient = createClient(supabaseUrl, anonKey, {
      auth: { persistSession: false, autoRefreshToken: false },
    });
    const { data: authData, error: authError } = await authClient.auth.getUser(accessToken);
    if (authError || !authData.user) {
      return NextResponse.json({ error: 'Your session is invalid. Sign in again.' }, { status: 401 });
    }

    const supabase = createClient(supabaseUrl, serviceRoleKey, {
      auth: { persistSession: false, autoRefreshToken: false },
    });
    const { data: profile, error: profileError } = await supabase
      .from('profiles')
      .select('id')
      .eq('auth_user_id', authData.user.id)
      .eq('status', 'ACTIVE')
      .maybeSingle();
    if (profileError || !profile) {
      return NextResponse.json({ error: 'An active resident profile was not found.' }, { status: 403 });
    }

    const { id } = await params;
    const { data: notification, error: notificationError } = await supabase
      .from('notifications')
      .select('id,created_by,reference_id')
      .eq('id', id)
      .maybeSingle();
    if (notificationError) {
      return NextResponse.json({ error: 'Notification could not be found.' }, { status: 500 });
    }
    if (!notification) {
      return NextResponse.json({ error: 'Notification not found.' }, { status: 404 });
    }
    if (notification.created_by !== profile.id) {
      return NextResponse.json({ error: 'You can delete only notifications posted by you.' }, { status: 403 });
    }

    const { error: deleteError } = await supabase
      .from('notifications')
      .delete()
      .eq('id', notification.id);
    if (deleteError) {
      return NextResponse.json({ error: 'Notification could not be deleted.' }, { status: 500 });
    }

    if (notification.reference_id) {
      await supabase
        .from('notices')
        .delete()
        .eq('id', notification.reference_id)
        .eq('published_by', profile.id);
    }

    return NextResponse.json({ success: true });
  } catch (error) {
    console.error('Notification delete failed:', error);
    return NextResponse.json({ error: 'Notification could not be deleted.' }, { status: 500 });
  }
}
