import { NextRequest, NextResponse } from 'next/server';

export async function POST(request: NextRequest) {
  try {
    const { text, source = 'en', target = 'gu' } = await request.json();
    if (typeof text !== 'string' || !text.trim() || text.length > 5000) {
      return NextResponse.json({ error: 'Text must contain between 1 and 5,000 characters.' }, { status: 400 });
    }
    if (!['en', 'gu'].includes(source) || !['en', 'gu'].includes(target) || source === target) {
      return NextResponse.json({ error: 'Translation is available between English and Gujarati.' }, { status: 400 });
    }

    const url = new URL('https://api.mymemory.translated.net/get');
    url.searchParams.set('q', text);
    url.searchParams.set('langpair', `${source}|${target}`);
    const response = await fetch(url, {
      headers: { Accept: 'application/json' },
      cache: 'no-store',
    });
    const result = await response.json();
    if (!response.ok) {
      return NextResponse.json({ error: 'Translation provider rejected the request.' }, { status: 502 });
    }

    const translation = result.responseData?.translatedText;
    if (typeof translation !== 'string' || result.responseStatus !== 200) {
      return NextResponse.json({ error: 'Translation provider returned no translation.' }, { status: 502 });
    }
    return NextResponse.json({ translation });
  } catch {
    return NextResponse.json({ error: 'Could not translate the text.' }, { status: 500 });
  }
}
