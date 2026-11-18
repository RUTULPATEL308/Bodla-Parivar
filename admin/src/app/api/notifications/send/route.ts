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

    const bodyResult = await readNotificationJson(request);
    if (isNotificationValidationFailure(bodyResult)) {
      return NextResponse.json({ error: bodyResult.error }, { status: bodyResult.status });
    }
    const body = bodyResult;
    const validated = validateNotificationInput(body);
    if (!validated.ok) return NextResponse.json({ error: validated.error }, { status: validated.status });
    const { target, title_gu, title_en, notification_title_gu, notification_title_en, body_gu, body_en, image_url, attachment_url, type, reference_id } = validated.value;

    const isDirectFuneralNotice = Boolean(
      body.is_besanu ||
      body.is_antim_yatra ||
      (title_gu && (title_gu.includes("બેસણું") || title_gu.includes("અંતિમ યાત્રા") || title_gu.includes("અંતિમયાત્રા") || title_gu.includes("અંજલિ"))) ||
      (body_gu && (body_gu.includes("બેસણું") || body_gu.includes("અંતિમ યાત્રા") || body_gu.includes("અંતિમયાત્રા") || body_gu.includes("અંજલિ")))
    );

    const supabase = createClient(supabaseUrl, serviceRoleKey, {
      auth: { persistSession: false, autoRefreshToken: false },
    });

    let isStaff = false;
    let profileId: string | null = null;

    const accessToken = request.headers.get('authorization')?.replace(/^Bearer\s+/i, '');
    if (accessToken) {
      const authClient = createClient(supabaseUrl, anonKey, {
        auth: { persistSession: false, autoRefreshToken: false },
      });
      const { data: { user } } = await authClient.auth.getUser(accessToken);
      if (user) {
        const { data: profile } = await supabase
          .from('profiles')
          .select('id')
          .eq('auth_user_id', user.id)
          .eq('status', 'ACTIVE')
          .maybeSingle();

        if (profile) {
          profileId = profile.id;
          const { data: roleLinks } = await supabase
            .from('user_roles')
            .select('role_id')
            .eq('user_id', profile.id);
          const roleIds = (roleLinks ?? []).map((row) => row.role_id);
          const { data: roles } = roleIds.length
            ? await supabase.from('roles').select('name').in('id', roleIds)
            : { data: [] };
          isStaff = (roles ?? []).some((role) => ['ADMIN', 'SUPER_ADMIN', 'MODERATOR', 'CONTENT_MANAGER'].includes(role.name));
        }
      }
    }

    if (!isStaff && !isDirectFuneralNotice && !accessToken) {
      return NextResponse.json({ error: 'Sign in to send notifications.' }, { status: 401 });
    }

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

    const isDirectFuneralNotice = Boolean(
      body.is_besanu ||
      body.is_antim_yatra ||
      (title_gu && (title_gu.includes("બેસણું") || title_gu.includes("અંતિમ યાત્રા") || title_gu.includes("અંતિમયાત્રા") || title_gu.includes("અંજલિ"))) ||
      (body_gu && (body_gu.includes("બેસણું") || body_gu.includes("અંતિમ યાત્રા") || body_gu.includes("અંતિમયાત્રા") || body_gu.includes("અંજલિ")))
    );

    let noticeId = reference_id || null;
    if (type === 'NOTICE' && !noticeId) {
      const { data: notice, error: noticeError } = await supabase
        .from('notices')
        .insert({
          title_gu,
          title_en: title_en || title_gu,
          description_gu: body_gu,
          description_en: body_en || body_gu,
          status: (isStaff || isDirectFuneralNotice) ? 'PUBLISHED' : 'PENDING',
          is_pinned: false,
          publish_at: new Date().toISOString(),
          published_by: profile?.id || null,
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

    if (!isStaff && !isDirectFuneralNotice) {
      return NextResponse.json({
        success: true,
        pending_approval: true,
        message: 'Notice submitted successfully and is pending admin approval. It will not be published or broadcasted until approved by an administrator.'
      });
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
        created_by: profile?.id || null,
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
    // DATA-ONLY payload: no "notification" key so onMessageReceived() is ALWAYS
    // called even when the app is in background/killed.  The Android service
    // builds its own notification + launches NoticeAlertActivity.
    const pushTitleGu = String(notification_title_gu ?? title_gu).slice(0, 80);
    const pushTitleEn = String(notification_title_en || title_en || title_gu).slice(0, 120);
    const pushBodyGu = String(body_gu).slice(0, 240);
    const pushBodyEn = String(body_en || body_gu).slice(0, 300);
    const aps: { 'content-available': number; sound: string; 'mutable-content'?: number } = {
      'content-available': 1,
      sound: 'default',
    };
    if (image_url) {
      aps['mutable-content'] = 1;
    }
    const payload = {
      data: {
        title_gu: pushTitleGu,
        title_en: pushTitleEn,
        body_gu: pushBodyGu,
        body_en: pushBodyEn,
        type: String(type),
        sender_profile_id: profile.id,
        notification_id: notification?.id ? String(notification.id) : '',
        reference_id: noticeId ? String(noticeId) : '',
        image_url: image_url ?? '',
        is_besanu: body.is_besanu ? 'true' : '',
        is_antim_yatra: body.is_antim_yatra ? 'true' : '',
      },
      android: {
        priority: 'high' as const,
      },
      apns: {
        payload: { aps },
        headers: { 'apns-priority': '10' },
      },
    };

    if (target === 'topic') {
      // Broadcast to topic 'all_users'
      const response = await messaging.send({
        topic: 'all_users',
        ...payload,
      });
      fcmResult.messageId = response;

      // Dual dispatch to all registered active device tokens for 100% physical device delivery:
      try {
        const { data: deviceTokens } = await supabase
          .from('device_tokens')
          .select('token')
          .eq('active', true);

        const tokens = (deviceTokens || []).map((t) => t.token).filter(Boolean);
        if (tokens.length > 0) {
          const batchRes = await messaging.sendEachForMulticast({
            tokens,
            ...payload,
          });
          fcmResult.successCount = batchRes.successCount;
        }
      } catch (e) {
        console.warn('Multicast dual dispatch note:', e);
      }
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
