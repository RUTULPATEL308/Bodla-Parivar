import { NextRequest, NextResponse } from 'next/server';
import { createClient } from '@supabase/supabase-js';
import { getFirebaseAdminApp, getMessaging } from '@/lib/firebaseAdmin';

// Approve a pending notice and trigger push notification
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

  // Update status to PUBLISHED (matches DB RLS policy: status = 'PUBLISHED')
  const { data: updated, error: updateError } = await supabase
    .from('notices')
    .update({ 
      status: 'PUBLISHED',
      publish_at: new Date().toISOString()
    })
    .eq('id', noticeId)
    .select()
    .single();

  if (updateError) {
    console.error('Approve error:', updateError);
    return NextResponse.json({ error: updateError.message }, { status: 500 });
  }

  // Create an in-app notification entry for the approved notice
  const pushTitleGu = (updated.title_gu || updated.title_en || 'સૂચના').slice(0, 80);
  const pushTitleEn = (updated.title_en || updated.title_gu || 'Notice').slice(0, 120);
  const pushBodyGu = (updated.description_gu || updated.description_en || '').slice(0, 240);
  const pushBodyEn = (updated.description_en || updated.description_gu || '').slice(0, 300);

  const { data: notification } = await supabase
    .from('notifications')
    .insert({
      title_gu: pushTitleGu,
      title_en: pushTitleEn,
      body_gu: pushBodyGu,
      body_en: pushBodyEn,
      type: 'NOTICE',
      reference_id: noticeId,
      image_url: updated.image_url || null,
      attachment_url: updated.attachment_url || null,
      payload: { image_url: updated.image_url || null, attachment_url: updated.attachment_url || null },
    })
    .select()
    .single();

  // Send Push Notification via Firebase Admin SDK
  let pushSent = false;
  try {
    const adminApp = getFirebaseAdminApp();
    const messaging = getMessaging(adminApp);

    // DATA-ONLY payload: no "notification" key so onMessageReceived() is ALWAYS
    // called even when the app is in background/killed.  The Android service
    // builds its own notification + launches NoticeAlertActivity.
    const payload = {
      data: {
        title_gu: pushTitleGu,
        title_en: pushTitleEn,
        body_gu: pushBodyGu,
        body_en: pushBodyEn,
        type: 'NOTICE',
        sender_profile_id: updated.published_by ? String(updated.published_by) : '',
        notification_id: notification?.id ? String(notification.id) : '',
        reference_id: String(noticeId),
        image_url: updated.image_url ?? '',
      },
      android: {
        priority: 'high' as const,
      },
      apns: {
        payload: { aps: { 'content-available': 1, sound: 'default', 'mutable-content': 1 } },
        headers: { 'apns-priority': '10' },
      },
    };

    // Broadcast to topic 'all_users'
    await messaging.send({
      topic: 'all_users',
      ...payload,
    });
    pushSent = true;
  } catch (pushErr) {
    console.error('FCM push dispatch failed upon approval:', pushErr);
  }

  return NextResponse.json({ success: true, notice: updated, push_sent: pushSent });
}
