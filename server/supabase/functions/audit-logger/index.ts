import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};
const jsonHeaders = { ...corsHeaders, "Content-Type": "application/json" };
const maxBodyBytes = 16 * 1024;
const allowedRoles = new Set(["ADMIN", "SUPER_ADMIN", "MODERATOR", "CONTENT_MANAGER"]);

function jsonResponse(body: Record<string, unknown>, status: number) {
  return new Response(JSON.stringify(body), { status, headers: jsonHeaders });
}

serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (req.method !== "POST") return jsonResponse({ error: "Method not allowed." }, 405);

  const authorization = req.headers.get("authorization") ?? "";
  const token = authorization.match(/^Bearer\s+(.+)$/i)?.[1];
  if (!token) return jsonResponse({ error: "Authentication required." }, 401);

  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY");
  const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!supabaseUrl || !anonKey || !serviceRoleKey) {
    return jsonResponse({ error: "Audit service is not configured." }, 500);
  }

  const authClient = createClient(supabaseUrl, anonKey, {
    auth: { persistSession: false, autoRefreshToken: false },
  });
  const { data: authData, error: authError } = await authClient.auth.getUser(token);
  if (authError || !authData.user) return jsonResponse({ error: "Authentication required." }, 401);

  const adminClient = createClient(supabaseUrl, serviceRoleKey, {
    auth: { persistSession: false, autoRefreshToken: false },
  });
  const { data: profile, error: profileError } = await adminClient
    .from("profiles")
    .select("id")
    .eq("auth_user_id", authData.user.id)
    .eq("status", "ACTIVE")
    .maybeSingle();
  if (profileError) return jsonResponse({ error: "Could not authorize audit request." }, 500);
  if (!profile) return jsonResponse({ error: "Staff access required." }, 403);

  const { data: assignments, error: assignmentsError } = await adminClient
    .from("user_roles")
    .select("role_id")
    .eq("user_id", profile.id);
  if (assignmentsError) return jsonResponse({ error: "Could not authorize audit request." }, 500);
  const roleIds = (assignments ?? []).map((assignment) => assignment.role_id);
  const { data: roles, error: rolesError } = roleIds.length
    ? await adminClient.from("roles").select("name").in("id", roleIds)
    : { data: [], error: null };
  if (rolesError) return jsonResponse({ error: "Could not authorize audit request." }, 500);
  if (!(roles ?? []).some((role) => allowedRoles.has(role.name))) {
    return jsonResponse({ error: "Staff access required." }, 403);
  }

  const declaredLength = Number(req.headers.get("content-length") ?? 0);
  if (declaredLength > maxBodyBytes) return jsonResponse({ error: "Audit request is too large." }, 413);

  let payload: Record<string, unknown>;
  try {
    const body = await req.text();
    if (new TextEncoder().encode(body).byteLength > maxBodyBytes) {
      return jsonResponse({ error: "Audit request is too large." }, 413);
    }
    payload = JSON.parse(body);
    if (!payload || typeof payload !== "object" || Array.isArray(payload)) throw new Error("Invalid body");
  } catch {
    return jsonResponse({ error: "Invalid JSON body." }, 400);
  }

  const action = typeof payload.action === "string" ? payload.action.trim() : "";
  const entityType = typeof payload.entity_type === "string" ? payload.entity_type.trim() : null;
  const entityId = typeof payload.entity_id === "string" ? payload.entity_id.trim() : null;
  if (!action || action.length > 100) return jsonResponse({ error: "Action must be 1 to 100 characters." }, 400);
  if (entityType && entityType.length > 100) return jsonResponse({ error: "Entity type is too long." }, 400);
  if (entityId && !/^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(entityId)) {
    return jsonResponse({ error: "Entity ID must be a UUID." }, 400);
  }

  const { data, error } = await adminClient
    .from("audit_logs")
    .insert({
      user_id: profile.id,
      action,
      entity_type: entityType,
      entity_id: entityId,
      old_data: payload.old_data ?? null,
      new_data: payload.new_data ?? null,
      ip_address: req.headers.get("cf-connecting-ip") ?? req.headers.get("x-forwarded-for")?.split(",")[0]?.trim() ?? null,
      user_agent: req.headers.get("user-agent"),
    })
    .select("id")
    .single();

  if (error) return jsonResponse({ error: "Could not record audit event." }, 500);
  return jsonResponse({ success: true, log_id: data.id }, 200);
});
