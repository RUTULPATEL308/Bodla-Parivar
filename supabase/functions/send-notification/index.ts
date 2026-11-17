// Supabase Edge Function: send-notification
// Antim Yatra and Besanu notices publish directly to all users.
// General community notices require admin review and ONLY broadcast when status === "PUBLISHED" upon admin approval.

import { serve } from "https://deno.land/std@0.203.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL") ?? "";
const SUPABASE_SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
const SUPABASE_ANON_KEY = Deno.env.get("SUPABASE_ANON_KEY") ?? "";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  if (req.method !== "POST") {
    return new Response("Method Not Allowed", { status: 405, headers: corsHeaders });
  }

  try {
    const authHeader = req.headers.get("Authorization");
    const supabaseClient = createClient(
      SUPABASE_URL,
      SUPABASE_SERVICE_ROLE_KEY || SUPABASE_ANON_KEY
    );

    let callingProfileId: string | null = null;
    let isStaff = false;

    if (authHeader) {
      const token = authHeader.replace("Bearer ", "");
      const { data: { user }, error: userError } = await supabaseClient.auth.getUser(token);
      if (user && !userError) {
        const { data: profile } = await supabaseClient
          .from("profiles")
          .select("id, status")
          .eq("auth_user_id", user.id)
          .maybeSingle();

        if (profile) {
          callingProfileId = profile.id;

          const { data: userRoles } = await supabaseClient
            .from("user_roles")
            .select("roles(name)")
            .eq("user_id", profile.id);

          const roleNames = (userRoles || []).map((r: any) => r.roles?.name?.toUpperCase());
          isStaff = roleNames.some((name: string) =>
            ["ADMIN", "SUPER_ADMIN", "MODERATOR", "CONTENT_MANAGER"].includes(name)
          );
        }
      }
    }

    const payload = await req.json();
    const {
      title_gu,
      title_en,
      body_gu,
      body_en,
      description_gu,
      description_en,
      image_url,
      attachment_url,
      is_besanu,
      is_antim_yatra,
      published_by,
      ...rest
    } = payload;

    const authorProfileId = callingProfileId || published_by || null;

    // Check if notice is Besanu (બેસણું) or Antim Yatra (અંતિમ યાત્રા)
    const isDirectFuneralNotice = Boolean(
      is_besanu ||
      is_antim_yatra ||
      (title_gu && (title_gu.includes("બેસણું") || title_gu.includes("અંતિમ યાત્રા") || title_gu.includes("અંતિમયાત્રા"))) ||
      (title_en && (title_en.toLowerCase().includes("besanu") || title_en.toLowerCase().includes("antim"))) ||
      (body_gu && (body_gu.includes("બેસણું") || body_gu.includes("અંતિમ યાત્રા") || body_gu.includes("અંતિમયાત્રા")))
    );

    // Besanu & Antim Yatra are directly PUBLISHED without admin approval.
    // Staff members also publish directly.
    // All normal community notices default to PENDING for admin review.
    const initialStatus = (isDirectFuneralNotice || isStaff) ? "PUBLISHED" : "PENDING";

    const finalTitleGu = title_gu || payload.title || "સૂચના";
    const finalTitleEn = title_en || payload.title || "Notice";
    const finalDescGu = description_gu || body_gu || payload.body || "";
    const finalDescEn = description_en || body_en || payload.body || finalDescGu;

    // Insert the notice into the database
    const { data: insertedNotice, error: insertError } = await supabaseClient
      .from("notices")
      .insert({
        title_gu: finalTitleGu,
        title_en: finalTitleEn,
        description_gu: finalDescGu,
        description_en: finalDescEn,
        image_url: image_url || null,
        attachment_url: attachment_url || null,
        status: initialStatus,
        published_by: authorProfileId,
        publish_at: new Date().toISOString(),
        is_pinned: false,
      })
      .select()
      .single();

    if (insertError) {
      console.error("Notice insert error:", insertError);
      return new Response(JSON.stringify({ error: insertError.message }), {
        status: 400,
        headers: { ...corsHeaders, "Content-Type": "application/json" }
      });
    }

    // Direct notice: also insert in-app notification entry and send FCM
    if (initialStatus === "PUBLISHED") {
      await supabaseClient.from("notifications").insert({
          full_screen_intent: ".NoticeAlertActivity",
        title_gu: finalTitleGu,
        title_en: finalTitleEn,
        body_gu: finalDescGu,
        body_en: finalDescEn,
        type: "NOTICE",
        reference_id: insertedNotice.id,
        image_url: image_url || null,
        attachment_url: attachment_url || null,
      });
    }

    return new Response(
      JSON.stringify({
        success: true,
        notice_created: true,
        notification_created: initialStatus === "PUBLISHED",
        notice: insertedNotice,
        notice_id: insertedNotice.id,
        status: initialStatus,
        requires_admin_approval: initialStatus === "PENDING",
        push_sent: initialStatus === "PUBLISHED"
      }),
      {
        headers: { ...corsHeaders, "Content-Type": "application/json" },
        status: 200,
      }
    );
  } catch (e) {
    console.error("Unexpected error in send-notification edge function:", e);
    return new Response(JSON.stringify({ error: String(e) }), {
      status: 500,
      headers: { ...corsHeaders, "Content-Type": "application/json" }
    });
  }
});
