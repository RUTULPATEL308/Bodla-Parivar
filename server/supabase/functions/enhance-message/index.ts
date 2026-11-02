import { serve } from "https://deno.land/std@0.168.0/http/server.ts";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

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
    const model = Deno.env.get("AI_MODEL") || (provider === "gemini" ? "gemini-2.0-flash" : "gpt-4o-mini");

    const systemPrompt = "You are a helpful assistant for a Gujarati community app (Bodla village). Refine the following Gujarati text for a community notice/notification. Preserve the original core intent and essential details (names, dates, places, numbers). Preserve WhatsApp-style formatting markers (*bold*, _italic_, ~strikethrough~) and URLs. Do NOT add formal letter salutations, greetings, openings, or signatures. Do NOT prepend any greeting. If title is requested, return a crisp announcement title of 5-8 words. If body/message is requested, return a warm, readable community message. Output JSON: {\"enhanced_gu\":\"...\",\"enhanced_en\":\"...\"}. Return raw JSON only with no markdown or extra commentary.";

    const response = await fetch(`${baseUrl}/chat/completions`, {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${apiKey}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        model,
        temperature: 0.4,
        messages: [
          { role: "system", content: systemPrompt },
          { role: "user", content: `Type: ${type || "body"}\nText: ${text}` },
        ],
      }),
    });

    if (!response.ok) {
      const errText = await response.text();
      console.error("AI upstream error:", response.status, errText);
      return new Response(JSON.stringify({ error: "AI service error" }), {
        status: 502,
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

    // Aggressive cleanup: strip any salutation prefix.
    const originalTrimmed = text.trim();
    // If enhanced text contains the original text, start from there (most reliable)
    if (enhancedGu.includes(originalTrimmed) && enhancedGu !== originalTrimmed) {
      enhancedGu = enhancedGu.slice(enhancedGu.indexOf(originalTrimmed)).trim();
    } else {
      // Otherwise, strip everything up to "ke," or "ke " if starts with salutation
      const guKe = "\u0A95\u0AC7"; // કે
      if (enhancedGu.includes(guKe + ",") || enhancedGu.includes(guKe + " ")) {
        const idx1 = enhancedGu.indexOf(guKe + ",");
        const idx2 = enhancedGu.indexOf(guKe + " ");
        let cutIdx = -1;
        if (idx1 !== -1 && idx2 !== -1) cutIdx = Math.min(idx1, idx2);
        else if (idx1 !== -1) cutIdx = idx1;
        else if (idx2 !== -1) cutIdx = idx2;
        if (cutIdx !== -1) {
          enhancedGu = enhancedGu.slice(cutIdx + guKe.length).trim();
          // remove leading comma/space
          enhancedGu = enhancedGu.replace(/^[\s,;:]+/, "").trim();
        }
      }
      // If still starts with salutation char (savinya = starts with U+0AB8), use original
      if (enhancedGu.charCodeAt(0) === 0x0AB8) {
        enhancedGu = originalTrimmed;
      }
    }
    if (!enhancedGu) enhancedGu = originalTrimmed;

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