/**
 * Transliteration utility for converting Gujlish (Roman English phonetic Gujarati)
 * into written Gujarati script (ગુજરાતી લિપિ).
 */

const transliterationCache = new Map<string, string>();

const gujaratiFallback: Record<string, string> = {
  a: 'અ', aa: 'આ', i: 'ઇ', ee: 'ઈ', u: 'ઉ', oo: 'ઉ', e: 'એ', ai: 'ઐ',
  o: 'ઓ', au: 'ઔ', am: 'અમ', an: 'અન', and: 'અને',
  kem: 'કેમ', cho: 'છો', chho: 'છો', hu: 'હું', huj: 'હું',
  naam: 'નામ', name: 'નામ', maro: 'મારો', mara: 'મારા', mari: 'મારી',
  swarg: 'સ્વર્ગ', swargiya: 'સ્વર્ગીય', swargiy: 'સ્વર્ગીય', sw: 'સ્વ.',
  antim: 'અંતિમ', yatra: 'યાત્રા', besanu: 'બેસણું',   samachar: 'સમાચાર',
  avsan: 'અવસાન', death: 'અવસાન', tarikh: 'તારીખ',
  vaar: 'વાર', somvar: 'સોમવાર', mangalvar: 'મંગળવાર', budhvar: 'બુધવાર',
  guruvar: 'ગુરુવાર', shukrvar: 'શુક્રવાર', shanivar: 'શનિવાર', ravivar: 'રવિવાર',
  parivar: 'પરિવાર', sabhyo: 'સભ્યો', no: 'નો', ni: 'ની', nu: 'નું',
  ghar: 'ઘર', gharni: 'ઘરની', gaam: 'ગામ', badha: 'બધા', aavjo: 'આવજો',
  samuh: 'સમૂહ', badhaine: 'બધાને', family: 'પરિવાર',
  smashan: 'સ્મશાન', ghat: 'ઘાટ', sarnaamu: 'સરનામું', sthaan: 'સ્થળ',
  mobile: 'મોબાઇલ', number: 'નમ્બર', mata: 'માતા', pita: 'પિતા',
};

function transliterationFallback(text: string) {
  const normalized = text.toLowerCase().replace(/[^a-z]/g, '');
  if (gujaratiFallback[normalized]) return gujaratiFallback[normalized];

  const rules: Array<[RegExp, string]> = [
    [/aa/g, 'ા'], [/ee/g, 'ી'], [/oo/g, 'ૂ'], [/chh/g, 'છ'], [/bh/g, 'ભ'],
    [/dh/g, 'ધ'], [/kh/g, 'ખ'], [/gh/g, 'ઘ'], [/th/g, 'થ'], [/ph/g, 'ફ'],
    [/sh/g, 'શ'], [/ch/g, 'ચ'], [/ng/g, 'ંગ'], [/ai/g, 'ૈ'], [/au/g, 'ૌ'],
  ];
  const consonants: Record<string, string> = {
    k: 'ક', g: 'ગ', c: 'ક', j: 'જ', t: 'ત', d: 'દ', n: 'ન',
    p: 'પ', b: 'બ', m: 'મ', y: 'ય', r: 'ર', l: 'લ', v: 'વ', w: 'વ',
    s: 'સ', h: 'હ',
  };
  let result = normalized;
  rules.forEach(([pattern, replacement]) => {
    result = result.replace(pattern, replacement);
  });
  return Array.from(result).map((letter) => consonants[letter] ?? letter).join('');
}

/**
 * Transliterate a single word or phrase from Gujlish to Gujarati.
 * E.g., 'kem cho' -> 'કેમ છો', 'mandir' -> 'મંદિર'
 */
export async function transliterateGujlish(text: string): Promise<string> {
  const trimmed = text.trim();
  if (!trimmed) return text;

  // Check cache first
  if (transliterationCache.has(trimmed)) {
    return transliterationCache.get(trimmed)!;
  }

  try {
    const controller = new AbortController();
    const timeout = window.setTimeout(() => controller.abort(), 4000);
    const res = await fetch(`/api/transliterate?text=${encodeURIComponent(trimmed)}&num=1`, {
      signal: controller.signal,
      cache: 'no-store',
    });
    window.clearTimeout(timeout);
    if (!res.ok) throw new Error('Transliteration failed');
    const data = await res.json();
    if (data.success && data.candidates && data.candidates.length > 0) {
      const topMatch = data.candidates[0].matches[0];
      if (topMatch) {
        transliterationCache.set(trimmed, topMatch);
        return topMatch;
      }
    }
  } catch (error) {
    console.warn('Gujlish transliteration fallback error:', error);
  }

  const fallback = transliterationFallback(trimmed);
  if (fallback && /[\u0A80-\u0AFF]/.test(fallback)) {
    transliterationCache.set(trimmed, fallback);
    return fallback;
  }
  return text;
}

/**
 * Fetch multiple suggestions for a given word to let user choose
 */
export async function getGujlishSuggestions(text: string): Promise<string[]> {
  const trimmed = text.trim();
  if (!trimmed) return [];

  try {
    const res = await fetch(`/api/transliterate?text=${encodeURIComponent(trimmed)}&num=5`);
    if (!res.ok) return [];
    const data = await res.json();
    if (data.success && data.candidates?.[0]?.matches) {
      return data.candidates[0].matches;
    }
  } catch (error) {
    console.warn('Gujlish suggestion fetch error:', error);
  }

  return [];
}
