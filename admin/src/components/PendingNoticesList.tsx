import React, { useEffect, useState } from 'react';
import { supabase } from '@/lib/supabase';
import { Card, CardContent, CardHeader, CardTitle } from './ui/card';
import { Check, Clock, AlertCircle, RefreshCw, FileText, Download, ExternalLink, Trash2 } from 'lucide-react';

interface NoticeItem {
  id: string;
  title_gu?: string | null;
  title_en?: string | null;
  description_gu?: string | null;
  description_en?: string | null;
  status: string;
  image_url?: string | null;
  attachment_url?: string | null;
  created_at: string;
}

// Component to list pending notices for admin approval
export default function PendingNoticesList() {
  const [notices, setNotices] = useState<NoticeItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionInProgress, setActionInProgress] = useState<string | null>(null);

  const fetchPending = async () => {
    setLoading(true);
    setError('');
    // Query notices with status PENDING or PENDING_APPROVAL
    const { data, error: fetchErr } = await supabase
      .from('notices')
      .select('id, title_gu, title_en, description_gu, description_en, status, image_url, attachment_url, created_at')
      .in('status', ['PENDING', 'PENDING_APPROVAL'])
      .order('created_at', { ascending: false });

    if (fetchErr) {
      setError(fetchErr.message);
    } else {
      setNotices(data ?? []);
    }
    setLoading(false);
  };

  const deleteNotice = async (id: string) => {
    if (actionInProgress) return;
    setActionInProgress(id);
    try {
      const res = await fetch('/api/notices/delete', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ noticeId: id }),
      });
      const data = await res.json();
      if (!res.ok) {
        alert('Delete failed: ' + (data.error || 'Server error'));
      } else {
        await fetchPending();
      }
    } catch (e: any) {
      alert('Error deleting notice: ' + (e?.message || e));
    } finally {
      setActionInProgress(null);
    }
  };


  useEffect(() => {
    const fetch = async () => {
      await fetchPending();
    };

  const deleteNotice = async (id: string) => {
    if (actionInProgress) return;
    setActionInProgress(id);
    try {
      const res = await fetch('/api/notices/delete', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ noticeId: id }),
      });
      const data = await res.json();
      if (!res.ok) {
        alert('Delete failed: ' + (data.error || 'Server error'));
      } else {
        await fetchPending();
      }
    } catch (e: any) {
      alert('Error deleting notice: ' + (e?.message || e));
    } finally {
      setActionInProgress(null);
    }
  };

    void fetch();
    const channel = supabase
      .channel('admin_pending_notices_sub')
      .on('postgres_changes', { event: '*', schema: 'public', table: 'notices' }, () => {
        void fetch();
      })
      .subscribe();

    return () => {
      void supabase.removeChannel(channel);
    };

  const deleteNotice = async (id: string) => {
    if (actionInProgress) return;
    setActionInProgress(id);
    try {
      const res = await fetch('/api/notices/delete', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ noticeId: id }),
      });
      const data = await res.json();
      if (!res.ok) {
        alert('Delete failed: ' + (data.error || 'Server error'));
      } else {
        await fetchPending();
      }
    } catch (e: any) {
      alert('Error deleting notice: ' + (e?.message || e));
    } finally {
      setActionInProgress(null);
    }
  };

  }, []);

  const approve = async (id: string) => {
    if (actionInProgress) return;
    setActionInProgress(id);
    try {
      const res = await fetch('/api/notices/approve', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ noticeId: id }),
      });
      const data = await res.json();
      if (!res.ok) {
        alert('Approve failed: ' + (data.error || 'Server error'));
      } else {
        await fetchPending();
      }
    } catch (e: any) {
      alert('Error approving notice: ' + (e?.message || e));
    } finally {
      setActionInProgress(null);
    }
  };

  const deleteNotice = async (id: string) => {
    if (actionInProgress) return;
    setActionInProgress(id);
    try {
      const res = await fetch('/api/notices/delete', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ noticeId: id }),
      });
      const data = await res.json();
      if (!res.ok) {
        alert('Delete failed: ' + (data.error || 'Server error'));
      } else {
        await fetchPending();
      }
    } catch (e: any) {
      alert('Error deleting notice: ' + (e?.message || e));
    } finally {
      setActionInProgress(null);
    }
  };


  const reject = async (id: string) => {
    if (actionInProgress) return;
    setActionInProgress(id);
    try {
      const res = await fetch('/api/notices/reject', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ noticeId: id }),
      });
      const data = await res.json();
      if (!res.ok) {
        alert('Reject failed: ' + (data.error || 'Server error'));
      } else {
        await fetchPending();
      }
    } catch (e: any) {
      alert('Error rejecting notice: ' + (e?.message || e));
    } finally {
      setActionInProgress(null);
    }
  };

  const deleteNotice = async (id: string) => {
    if (actionInProgress) return;
    setActionInProgress(id);
    try {
      const res = await fetch('/api/notices/delete', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ noticeId: id }),
      });
      const data = await res.json();
      if (!res.ok) {
        alert('Delete failed: ' + (data.error || 'Server error'));
      } else {
        await fetchPending();
      }
    } catch (e: any) {
      alert('Error deleting notice: ' + (e?.message || e));
    } finally {
      setActionInProgress(null);
    }
  };


  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between pb-2 border-b border-brand-border">
        <div className="flex items-center gap-2">
          <Clock className="h-5 w-5 text-amber-600" />
          <h2 className="text-lg font-bold text-slate-900 dark:text-white">
            મંજૂરી માટે બાકી સૂચનાઓ (Pending Approval)
          </h2>
        </div>
        <button
          onClick={() => void fetchPending()}
          disabled={loading}
          className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold rounded-lg border border-slate-200 bg-white hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-800 text-slate-700 dark:text-slate-200 transition shadow-sm"
        >
          <RefreshCw className={`h-3.5 w-3.5 ${loading ? 'animate-spin' : ''}`} />
          <span>રીફ્રેશ (Refresh)</span>
        </button>
      </div>

      {loading && (
        <div className="flex items-center justify-center p-8 text-sm text-slate-500">
          <RefreshCw className="h-4 w-4 animate-spin mr-2" />
          તપાસી રહ્યું છે...
        </div>
      )}

      {error && (
        <div className="flex items-center gap-2 rounded-xl bg-red-50 p-4 text-sm text-red-700 border border-red-200">
          <AlertCircle className="h-4 w-4 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {!loading && !error && notices.length === 0 && (
        <div className="rounded-2xl border border-dashed border-slate-200 p-8 text-center text-sm text-slate-500 dark:border-slate-800">
          કોઈ નવી બાકી સૂચના નથી (No pending notices to approve).
        </div>
      )}

      <div className="grid gap-4">
        {notices.map((n) => (
          <Card key={n.id} className="border border-amber-200/80 bg-amber-50/20 shadow-sm">
            <CardHeader className="flex flex-row items-start justify-between pb-2">
              <div>
                <CardTitle className="text-base font-bold text-slate-900">
                  {n.title_gu || n.title_en || 'સૂચના'}
                </CardTitle>
                {n.title_en && n.title_gu && (
                  <p className="text-xs text-slate-500 mt-0.5">{n.title_en}</p>
                )}
                <p className="text-xs text-slate-400 mt-1">
                  તારીખ: {new Date(n.created_at).toLocaleString('gu-IN')}
                </p>
              </div>
              <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-semibold bg-amber-100 text-amber-800">
                {n.status}
              </span>
            </CardHeader>
            <CardContent className="space-y-3">
              <p className="text-sm text-slate-700 whitespace-pre-wrap">
                {n.description_gu || n.description_en}
              </p>
              {n.image_url && (
                <div className="pt-2">
                  <p className="text-xs font-semibold text-slate-500 mb-1.5">🖼️ ફોટો / છબી (Image):</p>
                  <a
                    href={n.image_url}
                    target="_blank"
                    rel="noreferrer"
                    className="inline-block"
                  >
                    <img
                      src={n.image_url}
                      alt="Notice preview"
                      className="max-h-48 rounded-lg object-contain border border-slate-200"
                    />
                  </a>
                </div>
              )}
              {n.attachment_url && (
                <div className="pt-2">
                  <p className="text-xs font-semibold text-slate-500 mb-1.5">📄 અપલોડ કરેલ દસ્તાવેજ (Uploaded Document):</p>
                  <div className="flex flex-wrap items-center gap-2">
                    <a
                      href={n.attachment_url}
                      target="_blank"
                      rel="noreferrer"
                      className="inline-flex items-center gap-1.5 rounded-lg border border-slate-300 bg-white px-3 py-1.5 text-xs font-semibold text-blue-700 hover:bg-blue-50 transition shadow-sm"
                    >
                      <ExternalLink className="h-3.5 w-3.5" />
                      <span>દસ્તાવેજ જુઓ (View Document)</span>
                    </a>
                    <a
                      href={n.attachment_url + (n.attachment_url.includes('?') ? '&download' : '?download')}
                      target="_blank"
                      rel="noreferrer"
                      className="inline-flex items-center gap-1.5 rounded-lg border border-slate-300 bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-50 transition shadow-sm"
                    >
                      <Download className="h-3.5 w-3.5 text-emerald-600" />
                      <span>ડાઉનલોડ કરો (Download)</span>
                    </a>
                  </div>
                </div>
              )}
              <div className="flex justify-end pt-2 border-t border-slate-100">
                <button
                  type="button"
                  disabled={actionInProgress === n.id}
                  onClick={() => void approve(n.id)}
                  className="inline-flex items-center gap-1.5 rounded-xl bg-emerald-600 px-4 py-2 text-sm font-semibold text-white hover:bg-emerald-700 disabled:opacity-50 transition shadow-sm"
                >
                  <Check className="h-4 w-4" />
                  <span>
                    {actionInProgress === n.id ? 'મંજૂર થઈ રહ્યું છે...' : 'મંજૂર કરો (Approve & Publish)'}
                  </span>
                </button>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}
