import { NextRequest, NextResponse } from 'next/server';

export async function POST(request: NextRequest) {
  try {
    const body = await request.json();
    const text = (body.text || '').trim();
    const type = body.type || 'body';
    if (!text) {
      return NextResponse.json({ error: 'Please provide text to enhance.' }, { status: 400 });
    }

    const provider = (process.env.AI_PROVIDER || 'gemini').toLowerCase();
    const apiKey = process.env.AI_API_KEY || process.env.GEMINI_API_KEY || process.env.OPENAI_API_KEY;
    if (!apiKey) {
      return NextResponse.json({ error: 'AI Enhancement service is not configured.' }, { status: 500 });
    }

    const defaultBaseUrl = provider === 'gemini'
      ? 'https://generativelanguage.googleapis.com/v1beta/openai'
      : 'https://api.openai.com/v1';
    const baseUrl = (process.env.AI_BASE_URL || defaultBaseUrl).replace(/\/$/, '');
    const model = process.env.AI_MODEL || (provider === 'gemini' ? 'gemini-2.0-flash' : 'gpt-4o-mini');

    const systemPrompt = "You are a helpful assistant for a Gujarati community app (Bodla village). Refine the following Gujarati text for a community notice/notification. Preserve the original core intent and essential details (names, dates, places, numbers). Preserve any WhatsApp-style formatting markers (*bold*, _italic_, ~strikethrough~, code) and URLs. Do NOT add formal letter salutations, greetings, or openings. Do NOT prepend any greeting. Just refine the message text directly and return only the refined message. If title, return a crisp, catchy announcement title (max 15-20 words). If body/message, return a warm community message. Output JSON: {\"enhanced_gu\": \"...\", \"enhanced_en\": \"...\"}. No markdown, no extra commentary, raw JSON only.";

    const openAiResponse = await fetch(`${baseUrl}/chat/completions`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${apiKey}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        model,
        temperature: 0.4,
        messages: [
          { role: 'system', content: systemPrompt },
          { role: 'user', content: `Type: ${type}\nText: ${text}` },
        ],
      }),
    });

    if (!openAiResponse.ok) {
      const errText = await openAiResponse.text();
      console.error('AI enhance API failed:', errText);
      return NextResponse.json({ error: 'Failed to enhance message using AI.' }, { status: 502 });
    }

    const result = await openAiResponse.json();
    const content = result.choices?.[0]?.message?.content;
    if (!content) {
      return NextResponse.json({ error: 'Empty response from AI.' }, { status: 502 });
    }

    const parsed = JSON.parse(content);
    let enhancedGu = parsed.enhanced_gu || text.trim();
    const enhancedEn = parsed.enhanced_en || '';

    // Aggressive cleanup: if enhanced contains original text, start from there
    const originalTrimmed = text.trim();
    if (enhancedGu.includes(originalTrimmed) && enhancedGu !== originalTrimmed) {
      enhancedGu = enhancedGu.slice(enhancedGu.indexOf(originalTrimmed)).trim();
    } else {
      const guKe = '\u0A95\u0AC7'; // કે
      if (enhancedGu.includes(guKe + ',') || enhancedGu.includes(guKe + ' ')) {
        const idx1 = enhancedGu.indexOf(guKe + ',');
        const idx2 = enhancedGu.indexOf(guKe + ' ');
        let cutIdx = -1;
        if (idx1 !== -1 && idx2 !== -1) cutIdx = Math.min(idx1, idx2);
        else if (idx1 !== -1) cutIdx = idx1;
        else if (idx2 !== -1) cutIdx = idx2;
        if (cutIdx !== -1) {
          enhancedGu = enhancedGu.slice(cutIdx + guKe.length).trim().replace(/^[\s,;:]+/, '').trim();
        }
      }
      // If still starts with salutation char (savinya = U+0AB8), use original
      if (enhancedGu.charCodeAt(0) === 0x0AB8) {
        enhancedGu = originalTrimmed;
      }
    }
    if (!enhancedGu) enhancedGu = originalTrimmed;

    return NextResponse.json({
      success: true,
      enhanced_gu: enhancedGu,
      enhanced_en: enhancedEn,
    });
  } catch (error: unknown) {
    console.error('AI message enhancement error:', error);
    const msg = error instanceof Error ? error.message : 'Unknown enhancement error';
    return NextResponse.json({ error: msg }, { status: 500 });
  }
}