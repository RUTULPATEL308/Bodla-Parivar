export type TranslationLanguage = 'en' | 'gu';

export async function translateText(
  text: string,
  source: TranslationLanguage = 'en',
  target: TranslationLanguage = 'gu'
): Promise<string> {
  const response = await fetch('/api/translate', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ text, source, target }),
  });
  const result = await response.json();
  if (!response.ok) throw new Error(result.error || 'Translation failed.');
  return result.translation as string;
}
