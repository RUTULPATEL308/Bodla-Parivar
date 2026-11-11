'use client';

import { LoaderCircle, Save, Settings, Trash2, Mail, AlertTriangle } from 'lucide-react';
import { supabase } from '@/lib/supabase';
import Link from 'next/link';
import { useState, useEffect } from 'react';

const DELETE_ACCOUNT_URL = 'https://yourdomain.com/delete-account'; // Host delete-account.html online

type Profile = {
  id: string;
  email: string;
  full_name?: string;
  phone?: string;
  role?: string;
  created_at?: string;
};

export default function ProfilePage() {
  const [profile, setProfile] = useState<Profile | null>(null);
  const [loading, setLoading] = useState(true);
  const [deleting, setDeleting] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    void supabase
      .from('profiles')
      .select('*')
      .maybeSingle()
      .then(({ data, error: loadError }) => {
        if (!active) return;
        if (loadError) setError(loadError.message);
        else setProfile(data ?? null);
        setLoading(false);
      });
    return () => { active = false; };
  }, []);

  const handleDeleteAccount = async () => {
    if (!confirm('Are you sure? This will permanently delete your account and all associated data. This cannot be undone.')) return;
    setDeleting(true);
    try {
      // Open the delete account page in a new tab/window
      window.open(DELETE_ACCOUNT_URL, '_blank');
      setError('');
    } catch (err: any) {
      setError(err.message || 'Failed to open delete account page.');
    } finally {
      setDeleting(false);
    }
  };

  if (loading) {
    return (
      <main className="mx-auto max-w-2xl py-12 px-4">
        <div className="flex items-center gap-3 py-20 text-base text-slate-500">
          <LoaderCircle className="h-5 w-5 animate-spin" /> Loading profile…
        </div>
      </main>
    );
  }

  return (
    <main className="mx-auto max-w-2xl space-y-6 px-4 py-8">
      <header className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <h1 className="text-2xl font-bold text-brand-text">Profile</h1>
        <p className="text-base text-brand-muted">Manage your account and personal information.</p>
      </header>

      {error && (
        <p role="alert" className="rounded-xl border border-rose-200 bg-rose-50 p-4 text-base text-rose-800">
          {error}
        </p>
      )}

      <section className="rounded-3xl border border-brand-border bg-cream-surface p-5 shadow-sm sm:p-7">
        <h2 className="text-lg font-bold text-brand-text mb-4">Account Information</h2>
        <div className="space-y-3">
          <div className="flex items-center gap-3">
            <Mail className="h-5 w-5 text-saffron" aria-hidden="true" />
            <div>
              <p className="text-sm font-semibold text-brand-text">Email</p>
              <p className="text-sm text-brand-text truncate">{profile?.email || 'Not available'}</p>
            </div>
          </div>
          <div className="flex items-center gap-3">
            <span className="h-5 w-5 rounded-full bg-saffron/10 flex items-center justify-center text-saffron text-xs font-medium" aria-hidden="true">
              {profile?.role?.charAt(0)?.toUpperCase() ?? 'U'}
            </span>
            <div>
              <p className="text-sm font-semibold text-brand-text">Role</p>
              <p className="text-sm text-brand-text">{profile?.role || 'Member'}</p>
            </div>
          </div>
          {profile?.full_name && (
            <div className="flex items-center gap-3">
              <span className="h-5 w-5 rounded-full bg-saffron/10 flex items-center justify-center text-saffron text-xs font-medium" aria-hidden="true">👤</span>
              <div>
                <p className="text-sm font-semibold text-brand-text">Full Name</p>
                <p className="text-sm text-brand-text">{profile.full_name}</p>
              </div>
            </div>
          )}
          {profile?.phone && (
            <div className="flex items-center gap-3">
              <span className="h-5 w-5 rounded-full bg-saffron/10 flex items-center justify-center text-saffron text-xs font-medium" aria-hidden="true">📞</span>
              <div>
                <p className="text-sm font-semibold text-brand-text">Phone</p>
                <p className="text-sm text-brand-text">{profile.phone}</p>
              </div>
            </div>
          )}
        </div>
      </section>

      <section className="rounded-3xl border border-rose-200 bg-rose-50 p-5 shadow-sm sm:p-7">
        <h2 className="text-lg font-bold text-rose-900 mb-4 flex items-center gap-2">
          <AlertTriangle className="h-5 w-5 text-rose-600" aria-hidden="true" />
          Delete Account
        </h2>
        <p className="text-base text-rose-700 mb-4">
          Permanently delete your Bodla Parivar account and all associated data. This action cannot be undone.
        </p>
        <ul className="list-disc list-inside text-sm text-rose-700 mb-4 space-y-1">
          <li>Your account profile (name, email, phone)</li>
          <li>Authentication credentials</li>
          <li>Community notices you created</li>
          <li>Offerings you submitted or bid on</li>
          <li>Complaints you filed</li>
          <li>Push notification preferences</li>
        </ul>
        <button
          onClick={handleDeleteAccount}
          disabled={deleting}
          className="inline-flex min-h-12 w-full items-center justify-center gap-2 rounded-xl bg-rose-600 px-5 py-3 text-base font-semibold text-white shadow-sm hover:bg-rose-700 disabled:cursor-wait disabled:opacity-60"
        >
          {deleting ? <LoaderCircle aria-hidden="true" className="h-4 w-4 animate-spin" /> : <Trash2 aria-hidden="true" className="h-5 w-5" />}
          {deleting ? 'Processing…' : 'Delete My Account'}
        </button>
        <p className="mt-3 text-xs text-rose-600">
          You will be redirected to a confirmation page. Your data will be deleted within 7 business days.
        </p>
      </section>

      <section className="rounded-3xl border border-brand-border bg-cream-surface p-5 shadow-sm sm:p-7">
        <h2 className="text-lg font-bold text-brand-text mb-4">Need Help?</h2>
        <p className="text-base text-brand-text mb-3">
          If you have questions about account deletion or need assistance, contact us:
        </p>
        <p className="text-base text-brand-text">
          Email: <a href="mailto:yourname@gmail.com" className="text-saffron hover:underline font-medium">yourname@gmail.com</a>
        </p>
      </section>
    </main>
  );
}
