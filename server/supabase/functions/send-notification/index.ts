import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

const json = (body: Record<string, unknown>, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: { ...corsHeaders, "Content-Type": "application/json" } });

function base64Url(bytes: Uint8Array): string {
  let binary = "";
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary).replaceAll("+", "-").replaceAll("/", "_").replaceAll("=", "");
}

async function getFirebaseAccessToken(serviceAccount: Record<string, string>): Promise<string> {
  const now = Math.floor(Date.now() / 1000);
  const header = base64Url(new TextEncoder().encode(JSON.stringify({ alg: "RS256", typ: "JWT" })));
  const claim = base64Url(new TextEncoder().encode(JSON.stringify({
    iss: serviceAccount.client_email,
    scope: "https://www.googleapis.com/auth/firebase.messaging",
    aud: "https://oauth2.googleapis.com/token",
    iat: now,
    exp: now + 3600,
  })));
  const unsigned = `${header}.${claim}`;
  const pem = serviceAccount.private_key.replace(/\\n/g, "\n").replace(/-----BEGIN PRIVATE KEY-----|-----END PRIVATE KEY-----|\s/g, "");
  const der = Uint8Array.from(atob(pem), (char) => char.charCodeAt(0));
  const key = await crypto.subtle.importKey("pkcs8", der, { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }, false, ["sign"]);
  const signature = new Uint8Array(await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, new TextEncoder().encode(unsigned)));
  const assertion = `${unsigned}.${base64Url(signature)}`;
  const response = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer", assertion }),
  });
  const result = await response.json();
  if (!response.ok || !result.access_token) throw new Error("Firebase authentication failed.");
  return result.access_token as string;
}

serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (req.method !== "POST") return json({ error: "Method not allowed." }, 405);

  try {
    const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
    const anonKey = Deno.env.get("SUPABASE_ANON_KEY") ?? "";
    const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
    const authHeader = req.headers.get("Authorization");
    if (!supabaseUrl || !anonKey || !serviceRoleKey) return json({ error: "Notification service is not configured." }, 500);
    if (!authHeader?.startsWith("Bearer ")) return json({ error: "Sign in to send this notice." }, 401);

    const authClient = createClient(supabaseUrl, anonKey, { global: { headers: { Authorization: authHeader } } });
    const { data: { user }, error: authError } = await authClient.auth.getUser();
    if (authError || !user) return json({ error: "Your session is invalid. Sign in again." }, 401);

    const supabase = createClient(supabaseUrl, serviceRoleKey);
    const { data: profile, error: profileError } = await supabase
      .from("profiles").select("id").eq("auth_user_id", user.id).eq("status", "ACTIVE").maybeSingle();
    if (profileError || !profile) return json({ error: "An active member profile is required to send this notice." }, 403);

    const raw = await req.text();
    if (new TextEncoder().encode(raw).byteLength > 16_384) return json({ error: "Notice is too large." }, 413);
    let input: Record<string, unknown>;
    try {
      input = JSON.parse(raw);
    } catch {
      return json({ error: "Notice must be valid JSON." }, 400);
    }
    const titleGu = typeof input.title_gu === "string" ? input.title_gu.trim() : "";
    const titleEn = typeof input.title_en === "string" ? input.title_en.trim() : titleGu;
    const notificationTitleGu = typeof input.notification_title_gu === "string" && input.notification_title_gu.trim()
      ? input.notification_title_gu.trim()
      : titleGu;
    const notificationTitleEn = typeof input.notification_title_en === "string" && input.notification_title_en.trim()
      ? input.notification_title_en.trim()
      : titleEn;
    const bodyGu = typeof input.body_gu === "string" ? input.body_gu.trim() : "";
    const bodyEn = typeof input.body_en === "string" ? input.body_en.trim() : bodyGu;
    const descriptionGu = typeof input.description_gu === "string" ? input.description_gu.trim() : bodyGu;
    const descriptionEn = typeof input.description_en === "string" ? input.description_en.trim() : bodyEn;
    const imageUrl = typeof input.image_url === "string" ? input.image_url.trim() : "";
    const attachmentUrl = typeof input.attachment_url === "string" ? input.attachment_url.trim() : "";
    if (!titleGu || !bodyGu || titleGu.length > 80 || titleEn.length > 120 || bodyGu.length > 240 || bodyEn.length > 300) {
      return json({ error: "A Gujarati title and body are required; the notice text is too long." }, 400);
    }
    if (notificationTitleGu.length > 80 || notificationTitleEn.length > 120) {
      return json({ error: "The notification title is too long." }, 400);
    }
    if (descriptionGu.length > 4_000 || descriptionEn.length > 4_000) {
      return json({ error: "Notice descriptions must be 4,000 characters or fewer." }, 400);
    }
    if (imageUrl.length > 2_048 || (imageUrl && !imageUrl.startsWith(`${supabaseUrl}/storage/v1/object/public/antim-yatra-posters/`) && !imageUrl.startsWith(`${supabaseUrl}/storage/v1/object/public/notices/`))) {
      return json({ error: "The notification image URL must be hosted on project storage." }, 400);
    }
    if (attachmentUrl.length > 2_048 || (attachmentUrl && !attachmentUrl.startsWith(`${supabaseUrl}/storage/v1/object/public/notices/`))) {
      return json({ error: "The attachment URL must be hosted on project storage." }, 400);
    }

    const since = new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString();
    const { count, error: rateError } = await supabase.from("notices")
      .select("id", { count: "exact", head: true }).eq("published_by", profile.id).gte("created_at", since);
    if (rateError) throw rateError;
    if ((count ?? 0) >= 3) return json({ error: "You have reached the limit of 3 community notices in 24 hours." }, 429);

    const { data: notice, error: noticeError } = await supabase.from("notices").insert({
      title_gu: titleGu,
      title_en: titleEn,
      description_gu: descriptionGu,
      description_en: descriptionEn,
      image_url: imageUrl || null,
      attachment_url: attachmentUrl || null,
      status: "PUBLISHED",
      is_pinned: false,
      publish_at: new Date().toISOString(),
      published_by: profile.id,
    }).select("id").single();
    if (noticeError) throw noticeError;

    const { data: notification, error: insertError } = await supabase.from("notifications").insert({
      title_gu: notificationTitleGu,
      title_en: notificationTitleEn,
      body_gu: bodyGu,
      body_en: bodyEn,
      type: "NOTICE",
      reference_id: notice.id,
      created_by: profile.id,
      image_url: imageUrl || null,
      attachment_url: attachmentUrl || null,
      payload: (imageUrl || attachmentUrl) ? { image_url: imageUrl || null, attachment_url: attachmentUrl || null } : {},
    }).select("id").single();
    if (insertError) {
      console.error("Notice notification history insert failed:", insertError);
      const { error: rollbackError } = await supabase.from("notices").delete().eq("id", notice.id);
      if (rollbackError) {
        console.error("Could not roll back notice after notification insert failed:", rollbackError);
        return json({ error: "The notice was saved, but its notification could not be created. Contact an administrator." }, 500);
      }
      return json({ error: `Could not create notification history: ${insertError.message}` }, 500);
    }

    let pushSent = false;
    const configuredServiceAccount = Deno.env.get("FCM_SERVICE_ACCOUNT_KEY");
    if (configuredServiceAccount) {
      try {
        const serviceAccount = JSON.parse(configuredServiceAccount) as Record<string, string>;
        const projectId = serviceAccount.project_id;
        if (!projectId || !serviceAccount.client_email || !serviceAccount.private_key) {
          throw new Error("Firebase service account is incomplete.");
        }
        const firebaseToken = await getFirebaseAccessToken(serviceAccount);
        const pushResponse = await fetch(`https://fcm.googleapis.com/v1/projects/${encodeURIComponent(projectId)}/messages:send`, {
          method: "POST",
          headers: { Authorization: `Bearer ${firebaseToken}`, "Content-Type": "application/json" },
          body: JSON.stringify({ message: {
            topic: "all_users",
            notification: {
              title: notificationTitleGu,
              body: bodyGu.slice(0, 120),
              ...(imageUrl ? { image: imageUrl } : {}),
            },
            data: {
              title_gu: notificationTitleGu,
              title_en: notificationTitleEn,
              body_gu: bodyGu,
              body_en: bodyEn,
              type: "NOTICE",
              notification_id: String(notification.id),
              reference_id: String(notice.id),
              image_url: imageUrl,
              attachment_url: attachmentUrl,
            },
            android: {
              priority: "HIGH",
              notification: {
                channel_id: "bodla_parivar_channel",
                sound: "default",
                ...(imageUrl ? { image: imageUrl } : {}),
              },
            },
            apns: {
              payload: { aps: { sound: "default", ...(imageUrl ? { mutable_content: true } : {}) } },
              ...(imageUrl ? { fcm_options: { image: imageUrl } } : {}),
            },
          } }),
        });
        if (!pushResponse.ok) throw new Error(`FCM returned ${pushResponse.status}: ${await pushResponse.text()}`);
        pushSent = true;
      } catch (pushError) {
        console.error("Notice published, but FCM broadcast failed:", pushError);
      }
    }

    return json({
      success: true,
      notice_created: true,
      notification_created: true,
      notice_id: notice.id,
      notification_id: notification.id,
      push_sent: pushSent,
    }, 201);
  } catch (error) {
    console.error("Antim Yatra broadcast failed:", error);
    const message = error instanceof Error ? error.message : "Unknown server error.";
    return json({ error: `The notice could not be sent: ${message}` }, 500);
  }
});
