'use client';

import React, { useState } from 'react';
import { jsPDF } from 'jspdf';
import html2canvas from 'html2canvas';
import { Printer, Share2, Loader2, Check } from 'lucide-react';
import { useAdminLanguage } from '@/components/AdminLanguageContext';

type PrintControlsProps = {
  orientation?: 'portrait' | 'landscape';
};

export default function PrintControls({ orientation = 'portrait' }: PrintControlsProps) {
  const { lang, printLanguage, setPrintLanguage } = useAdminLanguage();
  const gu = lang === 'gu';
  const [isGenerating, setIsGenerating] = useState(false);
  const [shareSuccess, setShareSuccess] = useState(false);

  function makeDocument() {
    document.documentElement.dataset.printLanguage = printLanguage;
    document.getElementById('admin-print-root')?.remove();

    const printRoot = document.createElement('main');
    printRoot.id = 'admin-print-root';
    printRoot.className = 'admin-document';
    printRoot.setAttribute('aria-hidden', 'true');
    printRoot.dataset.printOrientation = orientation;
    if (orientation === 'landscape') printRoot.style.setProperty('page', 'landscape');
    printRoot.style.backgroundColor = '#ffffff';

    // ── Professional Compact Header ──────────────────────────
    const header = document.createElement('header');
    header.className = 'document-heading';
    header.style.cssText = 'border-bottom: 2px solid #0F2744; padding-bottom: 6px; margin-bottom: 8px; display: flex; justify-content: space-between; align-items: flex-end;';

    const leftCol = document.createElement('div');
    const orgTitle = document.createElement('div');
    orgTitle.style.cssText = 'font-size: 11pt; font-weight: 800; color: #0F2744; letter-spacing: -0.01em; line-height: 1.1;';
    orgTitle.textContent = 'બોદલા પરિવાર • BODLA PARIVAR';

    const orgSubtitle = document.createElement('div');
    orgSubtitle.style.cssText = 'font-size: 7.5pt; color: #64748B; font-weight: 500; margin-top: 1px;';
    orgSubtitle.textContent = 'બોદલા ગ્રામીણ સમુદાય ટ્રસ્ટ • Mehsana, Gujarat';

    const currentTitle = document.querySelector('main h1')?.textContent?.trim() || document.title;
    const docTitle = document.createElement('div');
    docTitle.style.cssText = 'font-size: 9.5pt; font-weight: 700; color: #D97706; margin-top: 3px; text-transform: uppercase; letter-spacing: 0.02em;';
    docTitle.textContent = currentTitle;

    leftCol.append(orgTitle, orgSubtitle, docTitle);

    const rightCol = document.createElement('div');
    rightCol.style.cssText = 'text-align: right; font-size: 7.5pt; color: #64748B; line-height: 1.25;';
    const now = new Date();
    const formattedDate = new Intl.DateTimeFormat('en-IN', {
      dateStyle: 'medium',
      timeStyle: 'short',
    }).format(now);

    const dateEl = document.createElement('div');
    dateEl.innerHTML = `<strong>Date:</strong> ${formattedDate}`;

    const refEl = document.createElement('div');
    refEl.style.cssText = 'font-family: monospace; font-size: 7pt; color: #94A3B8;';
    refEl.textContent = `REF: ${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}-${Math.random().toString(36).substring(2, 6).toUpperCase()}`;

    rightCol.append(dateEl, refEl);
    header.append(leftCol, rightCol);
    printRoot.append(header);

    // ── Append Print Areas ──────────────────────────────────────
    document.querySelectorAll('[data-print-area]').forEach((area) => {
      const clone = area.cloneNode(true) as HTMLElement;
      clone.querySelectorAll('.no-print, button, input, select, textarea, [data-print-exclude]')
        .forEach((control) => control.remove());
      clone.querySelectorAll('svg, [data-print-icon]')
        .forEach((icon) => icon.remove());
      if (clone.matches('.no-print, button, input, select, textarea, [data-print-exclude]')) return;
      printRoot.append(clone);
    });

    // ── Professional Footer ─────────────────────────────────────
    const footer = document.createElement('footer');
    footer.style.cssText = 'margin-top: 10px; padding-top: 5px; border-top: 1px solid #E2E8F0; display: flex; justify-content: space-between; font-size: 7pt; color: #94A3B8;';
    const copyNotice = document.createElement('span');
    copyNotice.textContent = 'Official confidential community record • Bodla Parivar Portal';
    const sealNotice = document.createElement('span');
    sealNotice.textContent = 'Authorized Digital Copy';
    footer.append(copyNotice, sealNotice);
    printRoot.append(footer);

    document.body.append(printRoot);
    return printRoot;
  }

  function printPage() {
    const printRoot = makeDocument();
    const cleanup = () => {
      printRoot.remove();
      delete document.documentElement.dataset.printLanguage;
      window.removeEventListener('afterprint', cleanup);
    };
    window.addEventListener('afterprint', cleanup, { once: true });
    void (document.fonts?.ready ?? Promise.resolve()).then(() => window.print());
    window.setTimeout(cleanup, 60_000);
  }

  async function sharePage() {
    setIsGenerating(true);
    const documentRoot = makeDocument();
    // Render document in a dedicated high-fidelity container matching exact A4 dimensions with tight spacing
    documentRoot.classList.add('pdf-export');
    const pageWidth = orientation === 'landscape' ? 297 : 210;
    const pageHeight = orientation === 'landscape' ? 210 : 297;
    documentRoot.style.cssText = `display:block;position:fixed;left:-9999px;top:0;width:${pageWidth}mm;min-height:${pageHeight}mm;padding:8mm 8mm;background:#ffffff;color:#0F172A;z-index:-100;box-sizing:border-box;`;

    try {
      await document.fonts.ready;
      // Slight delay to ensure layout computation
      await new Promise((resolve) => setTimeout(resolve, 200));

      const canvas = await html2canvas(documentRoot, {
        scale: 2,
        backgroundColor: '#ffffff',
        useCORS: true,
        logging: false,
      });

      const pdf = new jsPDF({
        orientation,
        unit: 'mm',
        format: 'a4',
        compress: true,
      });

      const pdfWidth = pageWidth;
      const pdfHeight = pageHeight;
      const pageMargin = 10;
      const contentWidth = pdfWidth - pageMargin * 2;
      const contentHeight = pdfHeight - pageMargin * 2;

      const pxPerMm = canvas.width / contentWidth;
      const pageSlicePx = Math.floor(contentHeight * pxPerMm);

      let totalPages = Math.ceil(canvas.height / pageSlicePx);
      if (totalPages === 0) totalPages = 1;

      for (let page = 0; page < totalPages; page++) {
        if (page > 0) pdf.addPage();
        const yStart = page * pageSlicePx;
        const currentSliceHeight = Math.min(pageSlicePx, canvas.height - yStart);

        const sliceCanvas = document.createElement('canvas');
        sliceCanvas.width = canvas.width;
        sliceCanvas.height = currentSliceHeight;
        const ctx = sliceCanvas.getContext('2d');
        if (!ctx) throw new Error('Failed to create canvas rendering context');

        ctx.fillStyle = '#ffffff';
        ctx.fillRect(0, 0, sliceCanvas.width, sliceCanvas.height);
        ctx.drawImage(
          canvas,
          0,
          yStart,
          canvas.width,
          currentSliceHeight,
          0,
          0,
          canvas.width,
          currentSliceHeight
        );

        const renderedMmHeight = (currentSliceHeight / canvas.width) * contentWidth;
        const imgData = sliceCanvas.toDataURL('image/png');
        pdf.addImage(imgData, 'PNG', pageMargin, pageMargin, contentWidth, renderedMmHeight, undefined, 'FAST');
      }

      const blob = pdf.output('blob');
      const sanitizedTitle = (document.querySelector('main h1')?.textContent?.trim() || document.title || 'bodla-report')
        .replace(/[^a-zA-Z0-9\u0A80-\u0AFF]+/g, '-')
        .replace(/^-|-$/g, '');
      const filename = `${sanitizedTitle || 'bodla-document'}.pdf`;
      const file = new File([blob], filename, { type: 'application/pdf' });

      // If mobile or browser supports file sharing
      if (navigator.canShare && navigator.canShare({ files: [file] }) && navigator.share) {
        await navigator.share({
          files: [file],
          title: document.title || 'બોદલા પરિવાર દસ્તાવેજ',
          text: 'Bodla Parivar Official Community Document (PDF)',
        });
        setShareSuccess(true);
        setTimeout(() => setShareSuccess(false), 3000);
      } else {
        // Fallback: download the generated PDF
        pdf.save(filename);
        setShareSuccess(true);
        setTimeout(() => setShareSuccess(false), 3000);
      }
    } catch (err) {
      console.error('PDF share failed:', err);
      // Fallback to native print if html2canvas encounters issue
      window.print();
    } finally {
      documentRoot.remove();
      delete document.documentElement.dataset.printLanguage;
      setIsGenerating(false);
    }
  }

  return (
    <div className="no-print flex flex-wrap items-center gap-2">
      <label htmlFor="print-language" className="text-xs font-semibold text-slate-500">
        {gu ? 'પ્રિન્ટ ભાષા:' : 'Language:'}
      </label>
      <select
        id="print-language"
        value={printLanguage}
        onChange={(event) => setPrintLanguage(event.target.value as typeof printLanguage)}
        className="h-9 rounded-lg border border-slate-200 bg-white px-2.5 text-xs font-medium text-slate-700 shadow-sm focus:border-navy-600 focus:outline-none"
      >
        <option value="gu">ગુજરાતી</option>
        <option value="en">English</option>
        <option value="both">{gu ? 'બંને ભાષા' : 'Both languages'}</option>
      </select>
      <button
        type="button"
        onClick={printPage}
        className="inline-flex h-9 items-center gap-1.5 rounded-lg bg-navy-900 px-3 text-xs font-semibold text-white shadow-sm hover:bg-navy-800 transition active:scale-95"
      >
        <Printer aria-hidden="true" className="h-3.5 w-3.5 text-amber-400" />
        {gu ? 'પ્રિન્ટ કરો' : 'Print Document'}
      </button>
      <button
        type="button"
        disabled={isGenerating}
        onClick={() => void sharePage()}
        className={`inline-flex h-9 items-center gap-1.5 rounded-lg border px-3 text-xs font-semibold shadow-sm transition active:scale-95 ${
          shareSuccess
            ? 'border-emerald-300 bg-emerald-50 text-emerald-800'
            : 'border-slate-200 bg-white text-slate-700 hover:bg-slate-50 hover:text-navy-900'
        }`}
        aria-label={gu ? 'PDF શેર કરો' : 'Share PDF'}
      >
        {isGenerating ? (
          <>
            <Loader2 aria-hidden="true" className="h-3.5 w-3.5 animate-spin text-navy-600" />
            <span>{gu ? 'પીડીએફ બને છે...' : 'Preparing PDF...'}</span>
          </>
        ) : shareSuccess ? (
          <>
            <Check aria-hidden="true" className="h-3.5 w-3.5 text-emerald-600" />
            <span>{gu ? 'શેર થયું!' : 'Shared!'}</span>
          </>
        ) : (
          <>
            <Share2 aria-hidden="true" className="h-3.5 w-3.5 text-navy-700" />
            <span>{gu ? 'PDF શેર કરો' : 'Share PDF'}</span>
          </>
        )}
      </button>
    </div>
  );
}
