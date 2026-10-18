import { NextRequest, NextResponse } from 'next/server';

export async function GET(request: NextRequest) {
  const { searchParams } = new URL(request.url);
  const text = searchParams.get('text');
  const num = searchParams.get('num') || '5';

  if (!text || !text.trim()) {
    return NextResponse.json({ success: true, result: [] });
  }

  try {
    const encoded = encodeURIComponent(text.trim());
    const apiUrl = `https://inputtools.google.com/request?text=${encoded}&itc=gu-t-i0-und&num=${num}&cp=0&cs=1`;
    
    const res = await fetch(apiUrl, {
      headers: {
        'Accept': 'application/json',
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)',
      },
      next: { revalidate: 3600 },
    });

    if (!res.ok) {
      return NextResponse.json({ success: false, error: 'Upstream transliteration failed' }, { status: 502 });
    }

    const data = await res.json();
    // Google input tools format: ["SUCCESS", [["source_word", ["gujarati_match_1", "gujarati_match_2", ...], ...]]]
    if (Array.isArray(data) && data[0] === 'SUCCESS' && Array.isArray(data[1])) {
      const candidates = data[1].map((item: any) => ({
        source: item[0],
        matches: Array.isArray(item[1]) ? (item[1] as string[]) : [],
      }));
      return NextResponse.json({ success: true, candidates });
    }

    return NextResponse.json({ success: false, error: 'Unexpected response format' }, { status: 500 });
  } catch (err: any) {
    return NextResponse.json({ success: false, error: err.message || 'Transliteration error' }, { status: 500 });
  }
}
