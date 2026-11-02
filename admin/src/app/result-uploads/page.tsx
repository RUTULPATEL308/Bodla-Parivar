'use client';

import { useEffect, useState } from 'react';
import { Download, ExternalLink, GraduationCap, LoaderCircle, RefreshCw } from 'lucide-react';
import { supabase } from '@/lib/supabase';

type ResultUpload = {
  id: string;
  user_id: string;
  student_name: string;
  standard: string;
  school_name: string;
  file_path: string;
  file_name: string;
  content_type: string;
  created_at: string;
  submitted_by: string | null;
  submitter_phone: string | null;
};

const columns = ['Student Name', 'Standard', 'School Name', 'Submitted By', 'Phone', 'File', 'Submitted At'];
const csvCell = (value: unknown) => `"${String(value ?? '').replaceAll('"', '""')}"`;

export default function ResultUploadsPage() {
  const [rows, setRows] = useState<ResultUpload[]>([]);
  const [loading, setLoading] = useState(true);
  const [featureEnabled, setFeatureEnabled] = useState(false);
  const [featureLoading, setFeatureLoading] = useState(true);
  const [featureSaving, setFeatureSaving] = useState(false);
  const [error, setError] = useState('');
  const [downloadError, setDownloadError] = useState('');
  const [serverCount, setServerCount] = useState<number | null>(null);

  const refreshUploads = async () => {
    setLoading(true);
    const countResult = await supabase.rpc('result_upload_admin_count');
    if (!countResult.error && typeof countResult.data === 'number') setServerCount(countResult.data);
    const { data, error: queryError } = await supabase.rpc('result_upload_admin_list');
    setError(queryError ? `Could not load result uploads: ${queryError.message}${queryError.code ? ` (${queryError.code})` : ''}` : '');
    setRows((data ?? []) as unknown as ResultUpload[]);
    setLoading(false);
  };

  useEffect(() => {
    let active = true;
    let channel: ReturnType<typeof supabase.channel> | undefined;
    const loadUploads = async () => {
      const countResult = await supabase.rpc('result_upload_admin_count');
      if (active && !countResult.error && typeof countResult.data === 'number') setServerCount(countResult.data);
      const { data, error: queryError } = await supabase.rpc('result_upload_admin_list');
      if (!active) return;
      setError(queryError ? `Could not load result uploads: ${queryError.message}${queryError.code ? ` (${queryError.code})` : ''}` : '');
      setRows((data ?? []) as unknown as ResultUpload[]);
      setLoading(false);
    };
    void Promise.all([
      supabase.from('village_settings').select('result_upload_enabled').order('created_at', { ascending: true }).limit(1).maybeSingle(),
      loadUploads(),
    ]).then(([settingsResult]) => {
        if (!active) return;
        setFeatureEnabled(settingsResult.data?.result_upload_enabled ?? false);
        setFeatureLoading(false);
        if (settingsResult.error) setError(`Could not load Result Upload settings: ${settingsResult.error.message}${settingsResult.error.code ? ` (${settingsResult.error.code})` : ''}`);
      });
    channel = supabase.channel(`result_uploads_admin_${Date.now()}`)
      .on('postgres_changes', { event: '*', schema: 'public', table: 'result_uploads' }, () => {
        void loadUploads();
      })
      .subscribe();
    const onFocus = () => { void loadUploads(); };
    window.addEventListener('focus', onFocus);
    return () => {
      active = false;
      window.removeEventListener('focus', onFocus);
      if (channel) void supabase.removeChannel(channel);
    };
  }, []);

  const toggleFeature = async () => {
    const nextValue = !featureEnabled;
    setFeatureSaving(true);
    setError('');
    const { data, error: toggleError } = await supabase.rpc('set_result_upload_enabled', { p_enabled: nextValue });
    if (toggleError) {
      setError(`Could not update Result Upload: ${toggleError.message}${toggleError.code ? ` (${toggleError.code})` : ''}`);
    } else {
      setFeatureEnabled(data === true);
    }
    setFeatureSaving(false);
  };

  const downloadExcel = () => {
    setDownloadError('');
    try {
      // Excel opens UTF-8 CSV directly; this keeps export dependency-free and Gujarati-safe.
      const data = [columns, ...rows.map((row) => [row.student_name, row.standard, row.school_name, row.submitted_by, row.submitter_phone, row.file_name, new Date(row.created_at).toLocaleString()])];
      const csv = '\uFEFF' + data.map((record) => record.map(csvCell).join(',')).join('\r\n');
      const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }));
      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = `result-uploads-${new Date().toISOString().slice(0, 10)}.csv`;
      anchor.click();
      URL.revokeObjectURL(url);
    } catch {
      setDownloadError('Could not create the spreadsheet. Please try again.');
    }
  };

  const openFile = async (path: string) => {
    const { data, error: fileError } = await supabase.storage.from('result-uploads').createSignedUrl(path, 60);
    if (fileError || !data?.signedUrl) {
      setError('Could not open this file. Please try again.');
      return;
    }
    window.open(data.signedUrl, '_blank', 'noopener,noreferrer');
  };

  return (
    <main className="mx-auto max-w-6xl space-y-6">
      <header className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="flex items-center gap-3 text-2xl font-bold text-brand-text sm:text-3xl"><GraduationCap className="h-7 w-7 text-saffron" />Result Uploads</h1>
          <p className="mt-2 text-base text-brand-muted">Review student result documents submitted from the mobile app.</p>
        </div>
        <div className="flex flex-wrap items-center gap-3">
          <button type="button" onClick={() => void refreshUploads()} disabled={loading} className="inline-flex min-h-12 items-center justify-center gap-2 rounded-xl border border-brand-border bg-white px-4 py-3 text-sm font-semibold text-brand-text disabled:cursor-not-allowed disabled:opacity-50"><RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />Refresh</button>
          <button type="button" role="switch" aria-checked={featureEnabled} onClick={() => void toggleFeature()} disabled={featureLoading || featureSaving} className={`inline-flex min-h-12 items-center gap-3 rounded-xl border px-4 py-3 text-sm font-semibold disabled:opacity-60 ${featureEnabled ? 'border-emerald-200 bg-emerald-50 text-emerald-800' : 'border-brand-border bg-white text-brand-muted'}`}>
            <span className={`relative h-5 w-9 rounded-full transition ${featureEnabled ? 'bg-emerald-600' : 'bg-stone-300'}`}><span className={`absolute top-0.5 h-4 w-4 rounded-full bg-white shadow transition-all ${featureEnabled ? 'left-[18px]' : 'left-0.5'}`} /></span>
            {featureLoading ? 'Loading…' : featureSaving ? 'Saving…' : featureEnabled ? 'Result Upload On' : 'Result Upload Off'}
          </button>
          <button type="button" onClick={downloadExcel} disabled={loading || rows.length === 0} className="inline-flex min-h-12 items-center justify-center gap-2 rounded-xl bg-saffron px-4 py-3 text-sm font-semibold text-white hover:bg-saffron-dark disabled:cursor-not-allowed disabled:opacity-50"><Download className="h-4 w-4" />Download Excel</button>
        </div>
      </header>
      {error && <p role="alert" className="rounded-xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-800">{error}</p>}
      {downloadError && <p role="alert" className="rounded-xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-800">{downloadError}</p>}
      <section className="overflow-x-auto rounded-2xl border border-brand-border bg-cream-surface shadow-sm">
        {loading ? <p role="status" className="flex items-center gap-3 p-8 text-brand-muted"><LoaderCircle className="h-5 w-5 animate-spin" />Loading submissions…</p> : rows.length === 0 ? <p className="p-8 text-center text-brand-muted">No result submissions yet.</p> : (
          <table className="w-full min-w-[760px] text-left text-sm text-brand-text">
            <thead className="border-b border-brand-border bg-cream text-xs uppercase text-brand-muted"><tr><th className="px-4 py-3">Student</th><th className="px-4 py-3">Standard</th><th className="px-4 py-3">School</th><th className="px-4 py-3">Submitted by</th><th className="px-4 py-3">Date</th><th className="px-4 py-3">Document</th></tr></thead>
            <tbody className="divide-y divide-brand-border">{rows.map((row) => <tr key={row.id} className="hover:bg-cream/40"><td className="px-4 py-3.5 font-semibold">{row.student_name}</td><td className="px-4 py-3.5">{row.standard}</td><td className="px-4 py-3.5">{row.school_name}</td><td className="px-4 py-3.5">{row.submitted_by ?? '—'}<div className="text-xs text-brand-muted">{row.submitter_phone ?? ''}</div></td><td className="px-4 py-3.5 text-xs">{new Date(row.created_at).toLocaleString()}</td><td className="px-4 py-3.5"><button type="button" onClick={() => void openFile(row.file_path)} className="inline-flex items-center gap-1.5 font-semibold text-saffron-dark hover:underline"><ExternalLink className="h-4 w-4" />{row.file_name}</button></td></tr>)}</tbody>
          </table>
        )}
      </section>
      {!loading && rows.length === 0 && !error && <p className="text-center text-sm text-brand-muted">{serverCount === null ? 'Could not verify the submission count. Apply migration 039, then refresh.' : serverCount > 0 ? `Supabase has ${serverCount} submission(s), but this list returned none. Check the staff read policy.` : 'Supabase confirms there are no saved submission rows yet. Update the mobile app and submit again.'}</p>}
    </main>
  );
}
