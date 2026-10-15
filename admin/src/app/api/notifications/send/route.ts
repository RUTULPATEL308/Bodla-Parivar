import { NextRequest, NextResponse } from 'next/server';
import { createClient } from '@supabase/supabase-js';
import { getFirebaseAdminApp, getMessaging } from '@/lib/firebaseAdmin';
import type { SendResponse } from 'firebase-admin/messaging';

export async function POST(request: NextRequest) {
  try {
    const supabaseUrl = process.env.NEXT_PUBLIC_SUPABASE_URL;
    const anonKey = process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY;
    const serviceRoleKey = process.env.SUPABASE_SERVICE_ROLE_KEY;

    if (!supabaseUrl || !anonKey || !serviceRoleKey) {
      return NextResponse.json(
        { error: 'Server environment configuration missing for Supabase.' },
        { status: 500 }
      );
    }

    const accessToken = request.headers.get('authorization')?.replace(/^Bearer\s+/i, '');
    if (!accessToken) return NextResponse.json({ error: 'Sign in to send notifications.' }, { status: 401 });

    const authClient = createClient(supabaseUrl, anonKey, {
      auth: { persistSession: false, autoRefreshToken: false },
    });
    const { data: { user }, error: authError } = await authClient.auth.getUser(accessToken);
    if (authError || !user) return NextResponse.json({ error: 'Your session is invalid. Sign in again.' }, { status: 401 });

    const supabase = createClient(supabaseUrl, serviceRoleKey, {
      auth: { persistSession: false, autoRefreshToken: false },
    });
    const { data: profile, error: profileError } = await supabase
      .from('profiles')
      .select('id')
      .eq('auth_user_id', user.id)
      .maybeSingle();
    if (profileError || !profile) return NextResponse.json({ error: 'Staff profile was not found.' }, { status: 403 });

    const { data: roleLinks, error: roleLinkError } = await supabase
      .from('user_roles')
      .select('role_id')
      .eq('user_id', profile.id);
    if (roleLinkError) return NextResponse.json({ error: 'Could not verify staff access.' }, { status: 500 });
    const roleIds = (roleLinks ?? []).map((row) => row.role_id);
    const { data: roles, error: rolesError } = roleIds.length
      ? await supabase.from('roles').select('name').in('id', roleIds)
      : { data: [], error: null };
    if (rolesError) return NextResponse.json({ error: 'Could not verify staff access.' }, { status: 500 });
    if (!(roles ?? []).some((role) => ['ADMIN', 'SUPER_ADMIN', 'MODERATOR', 'CONTENT_MANAGER'].includes(role.name))) {
      return NextResponse.json({ error: 'Only staff can send notifications.' }, { status: 403 });
    }

    const body = await request.json();
    const {
      title_gu,
      title_en,
      body_gu,
      body_en,
      type = 'NOTICE',
      target = 'topic', // 'topic' | 'tokens'
      reference_id,
    } = body;

    const supportedTypes = ['NOTICE', 'EVENT', 'COMPLAINT', 'OFFERING', 'BUSINESS', 'JOB', 'SYSTEM_BROADCAST', 'BID_CALL', 'BID_APPROVED'];
    if (typeof type !== 'string' || !supportedTypes.includes(type)) {
      return NextResponse.json({ error: 'Unsupported notification type.' }, { status: 400 });
    }
    if (target !== 'topic' && target !== 'tokens') {
      return NextResponse.json({ error: 'Unsupported notification target.' }, { status: 400 });
    }

    if (typeof title_gu !== 'string' || !title_gu.trim() || typeof body_gu !== 'string' || !body_gu.trim()) {
      return NextResponse.json(
        { error: 'Title and Body in Gujarati are required.' },
        { status: 400 }
      );
    }

    if (reference_id !== undefined && (typeof reference_id !== 'string' || !/^[0-9a-f-]{36}$/i.test(reference_id))) {
      return NextResponse.json({ error: 'Invalid notification reference.' }, { status: 400 });
    }

    // Save to the in-app history before contacting Firebase so history survives push outages.
    const { data: notification, error: dbError } = await supabase
      .from('notifications')
      .insert({
        title_gu,
        title_en: title_en || title_gu,
        body_gu,
        body_en: body_en || body_gu,
        type,
        reference_id: reference_id || null,
        created_by: profile.id,
      })
      .select()
      .single();

    if (dbError) {
      console.error('Supabase notification insert failed:', dbError);
      return NextResponse.json({ error: 'Notification could not be saved to history.' }, { status: 500 });
    }

    const adminApp = getFirebaseAdminApp();
    const messaging = getMessaging(adminApp);

    let fcmResult: {
      success: boolean;
      mode: string;
      messageId?: string;
      successCount?: number;
      failureCount?: number;
    } = { success: true, mode: target };

    // 3. Dispatch Push Notification via Firebase Cloud Messaging
    const pushTitleGu = String(title_gu).slice(0, 80);
    const pushTitleEn = String(title_en || title_gu).slice(0, 120);
    const pushBodyGu = String(body_gu).slice(0, 240);
    const pushBodyEn = String(body_en || body_gu).slice(0, 300);
    const payload = {
      notification: {
        title: pushTitleGu,
        body: pushBodyGu,
      },
      data: {
        title_gu: pushTitleGu,
        title_en: pushTitleEn,
        body_gu: pushBodyGu,
        body_en: pushBodyEn,
        type: String(type),
        notification_id: notification?.id ? String(notification.id) : '',
        reference_id: reference_id ? String(reference_id) : '',
      },
      android: {
        priority: 'high' as const,
        notification: { channelId: 'bodla_parivar_channel', sound: 'default' },
      },
      apns: { payload: { aps: { sound: 'default' } } },
    };

    if (target === 'topic') {
      // Broadcast to topic 'all_users'
      const response = await messaging.send({
        topic: 'all_users',
        ...payload,
      });
      fcmResult.messageId = response;
    } else {
      // Send to registered device tokens
      const { data: deviceTokens, error: tokenError } = await supabase
        .from('device_tokens')
        .select('token')
        .eq('active', true);

      if (tokenError) {
        console.warn('Failed to query device tokens:', tokenError);
      }

      const tokens = (deviceTokens || []).map((t) => t.token).filter(Boolean);

      if (tokens.length > 0) {
        const batchResponse = await messaging.sendEachForMulticast({
          tokens,
          ...payload,
        });
        fcmResult.successCount = batchResponse.successCount;
        fcmResult.failureCount = batchResponse.failureCount;

        // Clean up invalid tokens if any
        if (batchResponse.failureCount > 0) {
          const badTokens: string[] = [];
          batchResponse.responses.forEach((resp: SendResponse, idx: number) => {
            if (!resp.success && resp.error?.code === 'messaging/registration-token-not-registered') {
              badTokens.push(tokens[idx]!);
            }
          });
          if (badTokens.length > 0) {
            await supabase
              .from('device_tokens')
              .update({ active: false })
              .in('token', badTokens);
          }
        }
      } else {
        // Fallback to topic broadcast if no specific tokens stored yet
        const response = await messaging.send({
          topic: 'all_users',
          ...payload,
        });
        fcmResult.messageId = response;
        fcmResult.mode = 'topic_fallback';
      }
    }

    return NextResponse.json({
      success: true,
      notification: notification || null,
      fcm: fcmResult,
    });
  } catch (error: any) {
    console.error('Error dispatching notification:', error);
    return NextResponse.json(
      { error: error?.message || 'Failed to dispatch notification.' },
      { status: 500 }
    );
  }
}
