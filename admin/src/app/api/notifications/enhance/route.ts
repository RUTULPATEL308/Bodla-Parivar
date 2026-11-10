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
    const model = process.env.AI_MODEL || (provider === 'gemini' ? 'gemini-1.5-flash' : 'gpt-4o-mini');

    const systemPrompt = "You are a helpful and articulate human resident of Bodla village writing community announcements. Read the user's raw message carefully, understand its exact topic (e.g. sports, donations, meetings, personal news, festival greetings, water/electricity updates, urgent alerts, etc.), and rewrite it in natural, warm, human-like Gujarati. Avoid rigid templates or repeating the same opening/closing lines. Make it sound genuine, polite, and engaging. Preserve essential details (names, dates, times, places, numbers, URLs). Output JSON: {\"enhanced_gu\": \"...\", \"enhanced_en\": \"...\"}. No markdown, no extra commentary, raw JSON only.";

    let openAiResponse = await fetch(`${baseUrl}/chat/completions`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${apiKey}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        model,
        temperature: 0.6,
        messages: [
          { role: 'system', content: systemPrompt },
          { role: 'user', content: `Type: ${type}\nText: ${text}` },
        ],
      }),
    });

    if (!openAiResponse.ok && provider === 'gemini' && model === 'gemini-1.5-flash') {
      openAiResponse = await fetch(`${baseUrl}/chat/completions`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${apiKey}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          model: 'gemini-2.0-flash',
          temperature: 0.6,
          messages: [
            { role: 'system', content: systemPrompt },
            { role: 'user', content: `Type: ${type}\nText: ${text}` },
          ],
        }),
      });
    }

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
