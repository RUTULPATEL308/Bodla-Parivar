/**
 * Transliteration utility for converting Gujlish (Roman English phonetic Gujarati)
 * into written Gujarati script (ગુજરાતી લિપિ).
 */

const transliterationCache = new Map<string, string>();

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
    const res = await fetch(`/api/transliterate?text=${encodeURIComponent(trimmed)}&num=1`);
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
