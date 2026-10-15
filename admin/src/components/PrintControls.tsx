'use client';

import { Printer } from 'lucide-react';
import { useAdminLanguage } from '@/components/AdminLanguageContext';

export default function PrintControls() {
  const { lang, printLanguage, setPrintLanguage } = useAdminLanguage();
  const gu = lang === 'gu';

  function printPage() {
    document.documentElement.dataset.printLanguage = printLanguage;
    document.documentElement.dataset.printAll = 'true';
    if (document.fonts?.ready) void document.fonts.ready.then(() => window.print());
    else window.print();
    window.setTimeout(() => { delete document.documentElement.dataset.printAll; }, 5000);
  }

  return (
    <div className="no-print flex flex-wrap items-center gap-2">
      <label htmlFor="print-language" className="text-sm font-medium text-brand-muted">
        {gu ? 'પ્રિન્ટ ભાષા' : 'Print language'}
      </label>
      <select
        id="print-language"
        value={printLanguage}
        onChange={(event) => setPrintLanguage(event.target.value as typeof printLanguage)}
        className="min-h-10 rounded-lg border border-brand-border bg-white px-3 text-sm text-brand-text"
      >
        <option value="gu">ગુજરાતી</option>
        <option value="en">English</option>
        <option value="both">{gu ? 'બંને ભાષા' : 'Both languages'}</option>
      </select>
      <button type="button" onClick={printPage} className="inline-flex min-h-10 items-center gap-2 rounded-lg bg-brand-accent px-3 text-sm font-semibold text-white hover:opacity-90">
        <Printer aria-hidden="true" className="h-4 w-4" />
        {gu ? 'પ્રિન્ટ કરો' : 'Print'}
      </button>
    </div>
  );
}
