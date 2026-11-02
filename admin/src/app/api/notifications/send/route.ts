import { NextRequest, NextResponse } from 'next/server';
import { createClient } from '@supabase/supabase-js';
import { getFirebaseAdminApp, getMessaging } from '@/lib/firebaseAdmin';
import { isNotificationValidationFailure, readNotificationJson, validateNotificationInput } from '@/lib/notificationValidation';
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
      .eq('status', 'ACTIVE')
      .maybeSingle();
    if (profileError || !profile) return NextResponse.json({ error: 'An active resident profile was not found.' }, { status: 403 });

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
    const isStaff = (roles ?? []).some((role) => ['ADMIN', 'SUPER_ADMIN', 'MODERATOR', 'CONTENT_MANAGER'].includes(role.name));

    const bodyResult = await readNotificationJson(request);
    if (isNotificationValidationFailure(bodyResult)) {
      return NextResponse.json({ error: bodyResult.error }, { status: bodyResult.status });
    }
    const body = bodyResult;
    const validated = validateNotificationInput(body);
    if (!validated.ok) return NextResponse.json({ error: validated.error }, { status: validated.status });
    const { target, title_gu, title_en, notification_title_gu, notification_title_en, body_gu, body_en, image_url, attachment_url, type, reference_id } = validated.value;
    if (!isStaff && target === 'tokens') {
      return NextResponse.json({ error: 'Only staff can send direct token notifications.' }, { status: 403 });
    }

    // The picture must live on this project's storage so the push stays on a
    // trusted, publicly reachable host.
    if (image_url) {
      let imageHost = '';
      try {
        imageHost = new URL(image_url).host;
      } catch {
        return NextResponse.json({ error: 'The notification image URL is invalid.' }, { status: 400 });
      }
      let projectHost = '';
      try {
        projectHost = new URL(supabaseUrl).host;
      } catch {
        projectHost = '';
      }
      if (!projectHost || imageHost !== projectHost) {
        return NextResponse.json({ error: 'The notification image must be hosted on the project storage.' }, { status: 400 });
      }
    }
    if (attachment_url) {
      try {
        const attachmentHost = new URL(attachment_url).host;
        const projectHost = new URL(supabaseUrl).host;
        if (attachmentHost !== projectHost) {
          return NextResponse.json({ error: 'The notification attachment must be hosted on the project storage.' }, { status: 400 });
        }
      } catch {
        return NextResponse.json({ error: 'The notification attachment URL is invalid.' }, { status: 400 });
      }
    }

    let noticeId = reference_id || null;
    if (type === 'NOTICE' && !noticeId) {
      const { data: notice, error: noticeError } = await supabase
        .from('notices')
        .insert({
          title_gu,
          title_en: title_en || title_gu,
          description_gu: body_gu,
          description_en: body_en || body_gu,
          status: 'PUBLISHED',
          is_pinned: false,
          publish_at: new Date().toISOString(),
          published_by: profile.id,
          image_url: image_url || null,
          attachment_url: attachment_url || null,
        })
        .select('id')
        .single();
      if (noticeError) {
        console.error('Supabase notice insert failed:', noticeError);
        return NextResponse.json({ error: 'The public notice could not be created.' }, { status: 500 });
      }
      noticeId = notice.id;
    }

    // Save to the in-app history before contacting Firebase so history survives push outages.
    const { data: notification, error: dbError } = await supabase
      .from('notifications')
      .insert({
        title_gu: notification_title_gu ?? title_gu,
        title_en: notification_title_en || title_en || title_gu,
        body_gu,
        body_en: body_en || body_gu,
        type,
        reference_id: noticeId || null,
        created_by: profile.id,
        image_url: image_url || null,
        attachment_url: attachment_url || null,
        payload: { image_url: image_url || null, attachment_url: attachment_url || null },
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
    const pushTitleGu = String(notification_title_gu ?? title_gu).slice(0, 80);
    const pushTitleEn = String(notification_title_en || title_en || title_gu).slice(0, 120);
    const pushBodyGu = String(body_gu).slice(0, 240);
    const pushBodyEn = String(body_en || body_gu).slice(0, 300);
    const pushNotification: { title: string; body: string; image?: string } = {
      title: pushTitleGu,
      body: pushBodyGu,
    };
    const androidNotification: { channelId: string; sound: string; image?: string } = {
      channelId: 'bodla_parivar_channel',
      sound: 'default',
    };
    const aps: { sound: string; 'mutable-content'?: number } = { sound: 'default' };
    const apnsFcmOptions: { image?: string } = {};
    if (image_url) {
      pushNotification.image = image_url;
      androidNotification.image = image_url;
      aps['mutable-content'] = 1;
      apnsFcmOptions.image = image_url;
    }
    const payload = {
      notification: pushNotification,
      data: {
        title_gu: pushTitleGu,
        title_en: pushTitleEn,
        body_gu: pushBodyGu,
        body_en: pushBodyEn,
        type: String(type),
        notification_id: notification?.id ? String(notification.id) : '',
        reference_id: noticeId ? String(noticeId) : '',
        image_url: image_url ?? '',
      },
      android: {
        priority: 'high' as const,
        notification: androidNotification,
      },
      apns: {
        payload: { aps },
        ...(apnsFcmOptions.image ? { fcm_options: apnsFcmOptions } : {}),
      },
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
  } catch (error: unknown) {
    console.error('Error dispatching notification:', error);
    return NextResponse.json(
      { error: 'Failed to dispatch notification.' },
      { status: 500 }
    );
  }
}
