'use client';

import { FormEvent, useCallback, useEffect, useState } from 'react';
import { Check, LoaderCircle, Plus, Save, Sparkles, X } from 'lucide-react';
import { supabase } from '@/lib/supabase';
import PrintControls from '@/components/PrintControls';

type NavratriOption = {
  id: string;
  slug: string;
  title_gu: string;
  title_en: string;
  description_gu: string | null;
  description_en: string | null;
  report_url: string | null;
  option_type: 'BOOKING' | 'REPORT';
  cash_only: boolean;
  is_active: boolean;
  display_order: number;
  form_fields: BookingField[];
};

type BookingField = {
  key: string;
  label_gu: string;
  label_en: string;
  type: 'TEXT' | 'NUMBER' | 'DATE' | 'TIME' | 'NOTES' | 'PHONE' | 'AMOUNT';
  required: boolean;
};

type NavratriBooking = {
  id: string;
  option_id: string;
  customer_name: string;
  phone: string | null;
  amount: number | null;
  quantity: number;
  notes: string | null;
  payment_method: 'CASH';
  payment_status: 'PENDING' | 'PAID';
  status: 'PENDING' | 'CONFIRMED' | 'COMPLETED' | 'CANCELLED';
  form_data: Record<string, string>;
  created_at: string;
  option?: { title_gu: string; title_en: string; slug: string } | null;
};

const blankOption = (): Omit<NavratriOption, 'id'> => ({
  slug: '', title_gu: '', title_en: '', description_gu: '', description_en: '', report_url: null,
  option_type: 'BOOKING', cash_only: true, is_active: true, display_order: 10,
  form_fields: [],
});

export default function NavratriAdminPage() {
  const [options, setOptions] = useState<NavratriOption[]>([]);
  const [bookings, setBookings] = useState<NavratriBooking[]>([]);
  const [draft, setDraft] = useState<NavratriOption | Omit<NavratriOption, 'id'>>(blankOption());
  const [editingId, setEditingId] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [reportFile, setReportFile] = useState<File | null>(null);

  const load = useCallback(async () => {
    const [optionsResult, bookingsResult] = await Promise.all([
      supabase.from('navratri_options').select('*').order('display_order'),
      supabase.from('navratri_bookings').select('*,option:navratri_options(title_gu,title_en,slug)').order('created_at', { ascending: false }),
    ]);
    if (optionsResult.error) setError(optionsResult.error.message);
    else setOptions((optionsResult.data ?? []) as NavratriOption[]);
    if (bookingsResult.error) setError((current) => current ? `${current} · ${bookingsResult.error.message}` : bookingsResult.error.message);
    else setBookings((bookingsResult.data ?? []) as NavratriBooking[]);
    if (!optionsResult.error && !bookingsResult.error) setError('');
    setLoading(false);
  }, []);

  useEffect(() => {
    let active = true;
    void Promise.all([
      supabase.from('navratri_options').select('*').order('display_order'),
      supabase.from('navratri_bookings').select('*,option:navratri_options(title_gu,title_en,slug)').order('created_at', { ascending: false }),
    ]).then(([optionsResult, bookingsResult]) => {
      if (!active) return;
      if (optionsResult.error) setError(optionsResult.error.message);
      else setOptions((optionsResult.data ?? []) as NavratriOption[]);
      if (bookingsResult.error) setError((current) => current ? `${current} · ${bookingsResult.error.message}` : bookingsResult.error.message);
      else setBookings((bookingsResult.data ?? []) as NavratriBooking[]);
      setLoading(false);
    });
    return () => { active = false; };
  }, []);

  const editOption = (option: NavratriOption) => {
    setEditingId(option.id);
    setDraft({ ...option });
    setReportFile(null);
    setMessage('');
  };

  const resetDraft = () => {
    setDraft(blankOption());
    setReportFile(null);
    setEditingId(null);
  };

  const saveOption = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSaving(true);
    setError('');
    setMessage('');
    const slug = draft.slug.trim().toLowerCase().replace(/[^a-z0-9-]+/g, '-').replace(/^-|-$/g, '');
    const values = {
      slug,
      title_gu: draft.title_gu.trim(),
      title_en: draft.title_en.trim(),
      description_gu: draft.description_gu?.trim() || null,
      description_en: draft.description_en?.trim() || null,
      option_type: draft.option_type,
      cash_only: draft.option_type === 'BOOKING',
      form_fields: draft.option_type === 'BOOKING' ? draft.form_fields : [],
      is_active: draft.is_active,
      display_order: Number(draft.display_order) || 0,
    };
    let reportUrl = draft.report_url;
    let uploadedPath: string | null = null;
    if (reportFile) {
      const safeName = reportFile.name.replace(/[^a-zA-Z0-9._-]/g, '_');
      uploadedPath = `reports/${slug}/${Date.now()}-${safeName}`;
      const { error: uploadError } = await supabase.storage.from('navratri').upload(uploadedPath, reportFile, { contentType: reportFile.type, upsert: false });
      if (uploadError) {
        setError(uploadError.message);
        setSaving(false);
        return;
      }
      reportUrl = supabase.storage.from('navratri').getPublicUrl(uploadedPath).data.publicUrl;
    }
    const result = editingId
      ? await supabase.from('navratri_options').update({ ...values, report_url: reportUrl }).eq('id', editingId)
      : await supabase.from('navratri_options').insert({ ...values, report_url: reportUrl });
    if (result.error) setError(result.error.message);
    else {
      setMessage(editingId ? 'Navratri option updated.' : 'Navratri option created.');
      resetDraft();
      await load();
    }
    if (result.error && uploadedPath) await supabase.storage.from('navratri').remove([uploadedPath]);
    setSaving(false);
  };

  const updateBooking = async (booking: NavratriBooking, updates: Partial<Pick<NavratriBooking, 'status' | 'payment_status'>>) => {
    setError('');
    const { error: updateError } = await supabase.from('navratri_bookings').update(updates).eq('id', booking.id);
    if (updateError) setError(updateError.message);
    else await load();
  };

  const updateFormField = (index: number, updates: Partial<BookingField>) => {
    const nextFields = [...draft.form_fields];
    nextFields[index] = { ...nextFields[index], ...updates };
    setDraft({ ...draft, form_fields: nextFields });
  };

  return (
    <main className="mx-auto max-w-7xl space-y-6">
      <header className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="flex items-center gap-2 text-2xl font-bold text-brand-text"><Sparkles className="h-6 w-6 text-saffron" /> Navratri management</h1>
          <p className="mt-1 text-sm text-brand-muted">Manage public Navratri options, the Aheval report, and cash booking requests.</p>
        </div>
        <div className="no-print flex flex-wrap items-center gap-2"><PrintControls /><button type="button" onClick={resetDraft} className="inline-flex items-center justify-center gap-2 rounded-lg bg-saffron px-4 py-2 text-sm font-semibold text-white"><Plus className="h-4 w-4" /> New option</button></div>
      </header>

      {(error || message) && <p role={error ? 'alert' : 'status'} className={`rounded-lg border p-3 text-sm ${error ? 'border-rose-200 bg-rose-50 text-rose-800' : 'border-emerald-200 bg-emerald-50 text-emerald-800'}`}>{error || message}</p>}

      <section className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_minmax(340px,0.9fr)]">
        <div className="rounded-xl border border-brand-border bg-white p-4 shadow-sm sm:p-5">
          <h2 className="mb-4 text-lg font-bold text-brand-text">Public options</h2>
          {loading ? <p className="text-sm text-brand-muted">Loading options…</p> : options.length === 0 ? <p className="text-sm text-brand-muted">No options found. Apply migration 027 to seed the Navratri options.</p> : (
            <ul className="divide-y divide-brand-border">
              {options.map((option) => (
                <li key={option.id} className="flex items-start justify-between gap-3 py-3">
                  <div className="min-w-0">
                    <p className="font-semibold text-brand-text">{option.title_en} <span className="font-normal text-brand-muted">/ {option.title_gu}</span></p>
                    <p className="mt-1 text-xs text-brand-muted">{option.option_type === 'REPORT' ? 'Aheval/report content' : option.cash_only ? 'Booking · cash only' : 'Booking'} · order {option.display_order}</p>
                    {!option.is_active && <span className="mt-1 inline-block rounded bg-slate-100 px-2 py-0.5 text-xs text-slate-600">Hidden</span>}
                  </div>
                  <button type="button" onClick={() => editOption(option)} className="shrink-0 rounded border border-brand-border px-3 py-1.5 text-xs font-semibold hover:bg-cream">Edit</button>
                </li>
              ))}
            </ul>
          )}
        </div>

        <form onSubmit={saveOption} className="space-y-3 rounded-xl border border-brand-border bg-white p-4 shadow-sm sm:p-5">
          <div className="flex items-center justify-between gap-3">
            <h2 className="text-lg font-bold text-brand-text">{editingId ? 'Edit option' : 'Create option'}</h2>
            {editingId && <button type="button" onClick={resetDraft} aria-label="Cancel editing" className="rounded p-1 hover:bg-cream"><X className="h-4 w-4" /></button>}
          </div>
          <label className="block text-xs font-semibold">Option key<input required value={draft.slug} disabled={Boolean(editingId)} onChange={(e) => setDraft({ ...draft, slug: e.target.value })} placeholder="e.g. prasad" className="mt-1 w-full rounded border border-brand-border px-3 py-2 text-sm font-normal disabled:bg-slate-50" /></label>
          <div className="grid gap-3 sm:grid-cols-2">
            <label className="block text-xs font-semibold">Title (Gujarati)<input required value={draft.title_gu} onChange={(e) => setDraft({ ...draft, title_gu: e.target.value })} className="mt-1 w-full rounded border border-brand-border px-3 py-2 text-sm font-normal" /></label>
            <label className="block text-xs font-semibold">Title (English)<input required value={draft.title_en} onChange={(e) => setDraft({ ...draft, title_en: e.target.value })} className="mt-1 w-full rounded border border-brand-border px-3 py-2 text-sm font-normal" /></label>
          </div>
          {draft.option_type === 'REPORT' && <div className="space-y-2 rounded-lg border border-brand-border p-3">
            <label className="block text-xs font-semibold">Upload Aheval report (PDF, image, or Word file)<input type="file" accept="application/pdf,image/jpeg,image/png,image/webp,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document" onChange={(event) => setReportFile(event.target.files?.[0] ?? null)} className="mt-1 block w-full text-sm font-normal" /></label>
            {reportFile && <p className="text-xs text-brand-muted">Selected: {reportFile.name}</p>}
            {draft.report_url && <div className="flex items-center gap-3 text-xs"><a href={draft.report_url} target="_blank" rel="noreferrer" className="text-blue-700 underline">View current report file</a><button type="button" onClick={() => setDraft({ ...draft, report_url: null })} className="text-rose-700">Remove file</button></div>}
          </div>}
          <div className="grid gap-3 sm:grid-cols-2">
            <label className="block text-xs font-semibold">Description / report (Gujarati)<textarea rows={4} value={draft.description_gu ?? ''} onChange={(e) => setDraft({ ...draft, description_gu: e.target.value })} className="mt-1 w-full rounded border border-brand-border px-3 py-2 text-sm font-normal" /></label>
            <label className="block text-xs font-semibold">Description / report (English)<textarea rows={4} value={draft.description_en ?? ''} onChange={(e) => setDraft({ ...draft, description_en: e.target.value })} className="mt-1 w-full rounded border border-brand-border px-3 py-2 text-sm font-normal" /></label>
          </div>
          <div className="grid gap-3 sm:grid-cols-2">
            <label className="block text-xs font-semibold">Option type<select value={draft.option_type} onChange={(e) => setDraft({ ...draft, option_type: e.target.value as NavratriOption['option_type'] })} className="mt-1 w-full rounded border border-brand-border px-3 py-2 text-sm font-normal"><option value="BOOKING">Booking</option><option value="REPORT">Report</option></select></label>
            <label className="block text-xs font-semibold">Display order<input type="number" min="0" value={draft.display_order} onChange={(e) => setDraft({ ...draft, display_order: Number(e.target.value) })} className="mt-1 w-full rounded border border-brand-border px-3 py-2 text-sm font-normal" /></label>
          </div>
          <div className="flex flex-wrap gap-5 text-sm">
            {draft.option_type === 'BOOKING' && <span className="text-xs font-semibold text-saffron">Cash-only booking</span>}
            <label className="inline-flex items-center gap-2"><input type="checkbox" checked={draft.is_active} onChange={(e) => setDraft({ ...draft, is_active: e.target.checked })} /> Visible to residents</label>
          </div>
          {draft.option_type === 'BOOKING' && (
            <div className="space-y-2 rounded-lg border border-brand-border p-3">
              <div className="flex items-center justify-between"><h3 className="text-sm font-semibold">Extra booking fields</h3><button type="button" onClick={() => setDraft({ ...draft, form_fields: [...draft.form_fields, { key: `field_${Date.now()}`, label_gu: '', label_en: '', type: 'TEXT', required: false }] })} className="rounded border border-brand-border px-2 py-1 text-xs font-semibold">Add field</button></div>
              <p className="text-xs text-brand-muted">Name is always collected. Add only the fields this booking needs. Cash bookings never use online payments.</p>
              {draft.form_fields.map((field, index) => <div key={field.key} className="grid gap-2 rounded border border-brand-border p-2 sm:grid-cols-[1fr_1fr_120px_auto_auto] sm:items-center">
                <input aria-label="Gujarati field label" required value={field.label_gu} onChange={(event) => updateFormField(index, { label_gu: event.target.value })} placeholder="Gujarati label" className="rounded border border-brand-border px-2 py-1.5 text-xs" />
                <input aria-label="English field label" required value={field.label_en} onChange={(event) => updateFormField(index, { label_en: event.target.value })} placeholder="English label" className="rounded border border-brand-border px-2 py-1.5 text-xs" />
                <select aria-label="Field type" value={field.type} onChange={(event) => {
                  const type = event.target.value as BookingField['type'];
                  updateFormField(index, { type, ...(type === 'PHONE' ? { key: 'phone' } : type === 'AMOUNT' ? { key: 'amount' } : {}) });
                }} className="rounded border border-brand-border px-2 py-1.5 text-xs"><option value="TEXT">Text</option><option value="PHONE">Phone number</option><option value="AMOUNT">Amount</option><option value="NUMBER">Number</option><option value="DATE">Date</option><option value="TIME">Time</option><option value="NOTES">Long text</option></select>
                <label className="inline-flex items-center gap-1 text-xs"><input type="checkbox" checked={field.required} onChange={(event) => updateFormField(index, { required: event.target.checked })} /> Required</label>
                <button type="button" onClick={() => setDraft({ ...draft, form_fields: draft.form_fields.filter((_, fieldIndex) => fieldIndex !== index) })} className="rounded px-2 py-1 text-xs text-rose-700 hover:bg-rose-50">Remove</button>
              </div>)}
            </div>
          )}
          <button type="submit" disabled={saving} className="inline-flex items-center gap-2 rounded-lg bg-saffron px-4 py-2 text-sm font-semibold text-white disabled:opacity-50">{saving ? <LoaderCircle className="h-4 w-4 animate-spin" /> : editingId ? <Save className="h-4 w-4" /> : <Check className="h-4 w-4" />}{saving ? 'Saving…' : 'Save option'}</button>
        </form>
      </section>

      <section className="overflow-hidden rounded-xl border border-brand-border bg-white shadow-sm">
        <div className="no-print flex items-center justify-between gap-3 border-b border-brand-border p-4"><h2 className="text-lg font-bold text-brand-text">Booking requests <span className="ml-1 text-sm font-normal text-brand-muted">({bookings.length})</span></h2><PrintControls /></div>
        {loading ? <p className="p-4 text-sm text-brand-muted">Loading bookings…</p> : bookings.length === 0 ? <p className="p-4 text-sm text-brand-muted">No bookings have been submitted.</p> : (
          <div data-print-area className="overflow-x-auto">
            <table className="w-full min-w-[850px] text-left text-sm">
              <thead className="bg-cream text-xs uppercase text-brand-muted"><tr><th className="p-3">Resident / option</th><th className="p-3">Phone</th><th className="p-3">Qty / notes</th><th className="no-print p-3">Payment</th><th className="no-print p-3">Booking status</th><th className="p-3">Submitted</th></tr></thead>
              <tbody className="divide-y divide-brand-border">{bookings.map((booking) => <tr key={booking.id}>
                <td className="p-3"><p className="font-semibold">{booking.customer_name}</p><p className="text-xs text-brand-muted"><span data-print-lang="en">{booking.option?.title_en ?? 'Navratri'}</span><span data-print-lang="gu">{booking.option?.title_gu}</span></p></td>
                <td className="p-3">{booking.phone || '—'}</td>
                <td className="max-w-xs p-3">{booking.amount != null ? `₹${booking.amount.toLocaleString('en-IN')}` : booking.quantity}{Object.entries(booking.form_data ?? {}).filter(([key]) => !['phone', 'amount'].includes(key)).map(([key, value]) => <p key={key} className="mt-1 whitespace-pre-wrap text-xs text-brand-muted">{key}: {value}</p>)}{booking.notes && <p className="mt-1 whitespace-pre-wrap text-xs text-brand-muted">{booking.notes}</p>}</td>
                <td className="no-print p-3"><p>Cash</p><select aria-label={`Payment status for ${booking.customer_name}`} value={booking.payment_status} onChange={(e) => void updateBooking(booking, { payment_status: e.target.value as NavratriBooking['payment_status'] })} className="mt-1 rounded border border-brand-border px-2 py-1 text-xs"><option value="PENDING">Pending</option><option value="PAID">Paid</option></select></td>
                <td className="no-print p-3"><select aria-label={`Status for ${booking.customer_name}`} value={booking.status} onChange={(e) => void updateBooking(booking, { status: e.target.value as NavratriBooking['status'] })} className="rounded border border-brand-border px-2 py-1 text-xs"><option>PENDING</option><option>CONFIRMED</option><option>COMPLETED</option><option>CANCELLED</option></select></td>
                <td className="p-3 text-xs text-brand-muted">{new Date(booking.created_at).toLocaleString()}</td>
              </tr>)}</tbody>
            </table>
          </div>
        )}
      </section>
    </main>
  );
}
