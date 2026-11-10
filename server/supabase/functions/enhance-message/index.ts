import { serve } from "https://deno.land/std@0.168.0/http/server.ts";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

async function fetchWithTransientRetries(url: string, init: RequestInit): Promise<Response> {
  for (let attempt = 0; ; attempt++) {
    const response = await fetch(url, init);
    if (response.ok || (response.status !== 429 && response.status !== 503) || attempt === 2) {
      return response;
    }

    await response.body?.cancel();
    await new Promise((resolve) => setTimeout(resolve, 500 * (attempt + 1)));
  }
}

serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });

  try {
    const { text, type } = await req.json();
    if (!text || typeof text !== "string" || !text.trim()) {
      return new Response(JSON.stringify({ error: "text is required" }), {
        status: 400,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    const provider = (Deno.env.get("AI_PROVIDER") || "gemini").toLowerCase();
    const apiKey = Deno.env.get("AI_API_KEY") || Deno.env.get("GEMINI_API_KEY") || Deno.env.get("OPENAI_API_KEY");
    if (!apiKey) {
      return new Response(JSON.stringify({ error: "AI not configured" }), {
        status: 500,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    const defaultBaseUrl = provider === "gemini"
      ? "https://generativelanguage.googleapis.com/v1beta/openai"
      : "https://api.openai.com/v1";
    const baseUrl = (Deno.env.get("AI_BASE_URL") || defaultBaseUrl).replace(/\/$/, "");
    const model = Deno.env.get("AI_MODEL") || (provider === "gemini" ? "gemini-1.5-flash" : "gpt-4o-mini");

    const systemPrompt = "You are a helpful and articulate human resident of Bodla village writing community announcements. Read the user's raw message carefully, understand its exact topic (e.g. sports, donations, meetings, personal news, festival greetings, water/electricity updates, urgent alerts, etc.), and rewrite it in natural, warm, human-like Gujarati. Avoid rigid templates or repeating the same opening/closing lines. Make it sound genuine, polite, and engaging. Preserve essential details (names, dates, times, places, numbers, URLs). Output JSON: {\"enhanced_gu\":\"...\",\"enhanced_en\":\"...\"}. Return raw JSON only with no markdown or extra commentary.";

    let response = await fetchWithTransientRetries(`${baseUrl}/chat/completions`, {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${apiKey}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        model,
        temperature: 0.6,
        messages: [
          { role: "system", content: systemPrompt },
          { role: "user", content: `Type: ${type || "body"}\nText: ${text}` },
        ],
      }),
    });

    if (!response.ok && provider === "gemini" && model === "gemini-1.5-flash") {
      const fallbackModel = "gemini-2.0-flash";
      response = await fetchWithTransientRetries(`${baseUrl}/chat/completions`, {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${apiKey}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          model: fallbackModel,
          temperature: 0.6,
          messages: [
            { role: "system", content: systemPrompt },
            { role: "user", content: `Type: ${type || "body"}\nText: ${text}` },
          ],
        }),
      });
    }

    if (!response.ok) {
      const errText = await response.text();
      console.error("AI upstream error:", response.status, errText);
      const isBusy = response.status === 429 || response.status === 503;
      return new Response(JSON.stringify({
        error: isBusy ? "AI service is busy. Please try again shortly." : "AI service error",
      }), {
        status: isBusy ? response.status : 502,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    const data = await response.json();
    const content = data?.choices?.[0]?.message?.content ?? "";
    let enhancedGu = text;
    let enhancedEn = "";
    try {
      const parsed = JSON.parse(content);
      enhancedGu = parsed.enhanced_gu || text;
      enhancedEn = parsed.enhanced_en || "";
    } catch {
      enhancedGu = content || text;
    }

    return new Response(JSON.stringify({ enhanced_gu: enhancedGu, enhanced_en: enhancedEn }), {
      status: 200,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  } catch (e) {
    console.error("enhance-message error:", e);
    return new Response(JSON.stringify({ error: "Internal error" }), {
      status: 500,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }
});
