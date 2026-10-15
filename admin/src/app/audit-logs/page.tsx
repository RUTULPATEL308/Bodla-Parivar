'use client';

import { useEffect, useState } from 'react';
import { History, LoaderCircle } from 'lucide-react';
import { supabase } from '@/lib/supabase';

type AuditEntry = {
  id: string;
  action: string;
  entity_type: string | null;
  entity_id: string | null;
  old_data: unknown;
  new_data: unknown;
  created_at: string;
};

function printable(value: unknown) {
  if (value == null) return '—';
  try { return JSON.stringify(value); } catch { return String(value); }
}

export default function AdminAuditLogsPage() {
  const [logs, setLogs] = useState<AuditEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    void supabase
      .from('audit_logs')
      .select('id,action,entity_type,entity_id,old_data,new_data,created_at')
      .order('created_at', { ascending: false })
      .limit(100)
      .then(({ data, error: queryError }) => {
        if (!active) return;
        if (queryError) setError(queryError.message);
        else setLogs((data ?? []) as AuditEntry[]);
        setLoading(false);
      });
    return () => { active = false; };
  }, []);

  return (
    <main className="mx-auto max-w-6xl space-y-6">
      <header>
        <h1 className="flex items-center gap-3 text-2xl font-bold text-brand-text sm:text-3xl">
          <History aria-hidden="true" className="h-7 w-7 text-saffron" /> Audit logs
        </h1>
        <p className="mt-2 text-base text-brand-muted">Recent recorded changes made by village staff.</p>
      </header>

      {error && <p role="alert" className="rounded-xl border border-rose-200 bg-rose-50 p-4 text-base text-rose-800">Could not load audit logs: {error}</p>}
      {loading ? (
        <p role="status" className="flex items-center gap-3 py-10 text-base text-brand-muted"><LoaderCircle aria-hidden="true" className="h-5 w-5 animate-spin" /> Loading audit history…</p>
      ) : logs.length === 0 ? (
        <p className="rounded-2xl border border-brand-border bg-cream-surface p-6 text-base text-brand-muted">No audit entries have been recorded yet.</p>
      ) : (
        <ol aria-label="Audit history" className="space-y-3">
          {logs.map((log) => (
            <li key={log.id} className="rounded-2xl border border-brand-border bg-cream-surface p-4 shadow-sm sm:p-5">
              <div className="flex flex-col gap-1 sm:flex-row sm:items-start sm:justify-between sm:gap-4">
                <h2 className="break-words text-base font-bold text-brand-text">{log.action.replaceAll('_', ' ')}</h2>
                <time className="shrink-0 text-sm text-brand-muted" dateTime={log.created_at}>{new Date(log.created_at).toLocaleString()}</time>
              </div>
              <p className="mt-2 text-sm text-brand-muted">{log.entity_type || 'Record'}{log.entity_id ? ` · ${log.entity_id}` : ''}</p>
              {(log.old_data != null || log.new_data != null) && (
                <details className="mt-3">
                  <summary className="min-h-11 cursor-pointer py-2 text-sm font-semibold text-saffron">View changes</summary>
                  <div className="grid gap-3 pt-2 sm:grid-cols-2">
                    <pre className="overflow-x-auto whitespace-pre-wrap break-words rounded-xl bg-cream p-3 text-sm text-brand-text">Before: {printable(log.old_data)}</pre>
                    <pre className="overflow-x-auto whitespace-pre-wrap break-words rounded-xl bg-cream p-3 text-sm text-brand-text">After: {printable(log.new_data)}</pre>
                  </div>
                </details>
              )}
            </li>
          ))}
        </ol>
      )}
    </main>
  );
}
