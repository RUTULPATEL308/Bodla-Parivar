import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const supabaseClient = createClient(
      Deno.env.get("SUPABASE_URL") ?? "",
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? ""
    );

    const { title_gu, title_en, body_gu, body_en, type, reference_id } = await req.json();

    if (!title_gu || !body_gu) {
      return new Response(
        JSON.stringify({ error: "Missing required notification fields" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // 1. Insert notification record into database
    const { data: notification, error: notifError } = await supabaseClient
      .from("notifications")
      .insert({
        title_gu,
        title_en: title_en || title_gu,
        body_gu,
        body_en: body_en || body_gu,
        type: type || "NOTICE",
        reference_id: reference_id || null,
      })
      .select()
      .single();

    if (notifError) throw notifError;

    // 2. Fetch active device push tokens
    const { data: tokens, error: tokenError } = await supabaseClient
      .from("device_tokens")
      .select("token, platform")
      .eq("active", true);

    if (tokenError) throw tokenError;

    return new Response(
      JSON.stringify({
        success: true,
        notification_id: notification.id,
        dispatched_count: tokens?.length || 0,
      }),
      { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  } catch (error) {
    return new Response(
      JSON.stringify({ error: (error as Error).message }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }
});
