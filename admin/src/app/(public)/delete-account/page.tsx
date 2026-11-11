'use client';

import { useEffect, useState } from 'react';
import { supabase } from '@/lib/supabase';
import Link from 'next/link';
import { AlertTriangle, ShieldCheck, Clock, Loader2, Trash2, Mail } from 'lucide-react';

export default function DeleteAccountPage() {
  const [isLoading, setIsLoading] = useState(true);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [userEmail, setUserEmail] = useState<string | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [deleted, setDeleted] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    async function loadSession() {
      const { data: { session }, error } = await supabase.auth.getSession();
      if (!active) return;
      setIsAuthenticated(!!session && !error);
      if (!error && session?.user) {
        const { data: profile, error: profileError } = await supabase
          .from('profiles')
          .select('email')
          .eq('auth_user_id', session.user.id)
          .maybeSingle();
        if (!profileError && profile?.email) {
          setUserEmail(profile.email);
        }
      }
      setIsLoading(false);
    }
    void loadSession();
    return () => { active = false; };
  }, []);

  const handleDelete = async () => {
    setDeleting(true);
    setError(null);
    try {
      const { error: rpcError } = await supabase.rpc('delete_account');
      if (rpcError) throw rpcError;
      setDeleted(true);
    } catch (err: any) {
      setError(err.message || 'Failed to delete account. Please try again.');
    } finally {
      setDeleting(false);
    }
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-slate-50 flex font-sans antialiased selection:bg-amber-100 selection:text-amber-900">
        <div className="mx-auto max-w-2xl px-4 py-8 sm:px-6 lg:px-8">
          <div className="bg-white rounded-3xl border border-slate-200 p-6 sm:p-10 shadow-sm dark:border-slate-800 dark:bg-slate-900 dark:text-slate-100 flex items-center justify-center py-20">
            <Loader2 className="h-6 w-6 animate-spin text-saffron" />
            <span className="ml-2 text-slate-500 dark:text-slate-400">Loading your account…</span>
          </div>
        </div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return (
      <div className="min-h-screen bg-slate-50 flex font-sans antialiased selection:bg-amber-100 selection:text-amber-900">
        <div className="mx-auto max-w-2xl px-4 py-8 sm:px-6 lg:px-8">
          <div className="bg-white rounded-3xl border border-slate-200 p-6 sm:p-10 shadow-sm dark:border-slate-800 dark:bg-slate-900 dark:text-slate-100">
            <div className="text-center">
              <div className="flex h-12 w-12 items-center justify-center rounded-full bg-rose-100 text-rose-600 dark:bg-rose-900/30 dark:text-rose-400 mb-4">
                <AlertTriangle className="h-6 w-6" />
              </div>
              <h1 className="text-2xl font-bold text-slate-900 dark:text-white">Sign In Required</h1>
              <p className="mt-2 text-slate-600 dark:text-slate-400">
                You must be signed in to delete your account. Please sign in first.
              </p>
              <div className="mt-6">
                <Link href="/" className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-saffron underline underline-offset-4 hover:text-amber-600 dark:hover:text-amber-400">
                  ← Back to Home
                </Link>
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (deleted) {
    return (
      <div className="min-h-screen bg-slate-50 flex font-sans antialiased selection:bg-amber-100 selection:text-amber-900">
        <div className="mx-auto max-w-2xl px-4 py-8 sm:px-6 lg:px-8">
          <div className="bg-white rounded-3xl border border-slate-200 p-6 sm:p-10 shadow-sm dark:border-slate-800 dark:bg-slate-900 dark:text-slate-100">
            <div className="text-center py-12">
              <div className="flex h-12 w-12 items-center justify-center rounded-full bg-emerald-100 text-emerald-600 dark:bg-emerald-900/30 dark:text-emerald-400 mb-4">
                <ShieldCheck className="h-6 w-6" />
              </div>
              <h1 className="text-2xl font-bold text-slate-900 dark:text-white">Account Deleted</h1>
              <p className="mt-2 text-slate-600 dark:text-slate-400">
                Your Bodla Parivar account and all associated data have been permanently deleted.
              </p>
              <div className="mt-8">
                <Link href="/" className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-saffron underline underline-offset-4 hover:text-amber-600 dark:hover:text-amber-400">
                  ← Back to Home
                </Link>
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-50 flex font-sans antialiased selection:bg-amber-100 selection:text-amber-900">
      <div className="mx-auto max-w-2xl px-4 py-8 sm:px-6 lg:px-8">
        <div className="bg-white rounded-3xl border border-slate-200 p-6 sm:p-10 shadow-sm dark:border-slate-800 dark:bg-slate-900 dark:text-slate-100">
          <div className="text-center mb-8">
            <div className="flex h-12 w-12 items-center justify-center rounded-full bg-rose-100 text-rose-600 dark:bg-rose-900/30 dark:text-rose-400 mb-4">
              <Trash2 className="h-6 w-6" />
            </div>
            <h1 className="text-2xl font-bold text-slate-900 dark:text-white">Delete My Account</h1>
            <p className="mt-2 text-slate-600 dark:text-slate-400">
              Bodla Parivar — Community Platform for Bodla Village, Mehsana, Gujarat
            </p>
          </div>

          <div className="mb-8">
            <h2 className="text-lg font-bold text-slate-900 dark:text-white mb-3">Permanently Delete Your Account</h2>
            <p className="text-slate-600 dark:text-slate-400 mb-6">
              Once deleted, your account and all associated data cannot be recovered. This action is irreversible.
            </p>

            <div className="space-y-2 text-sm text-slate-600 dark:text-slate-400 list-disc list-inside">
              <li>Account profile (name, email, phone)</li>
              <li>Authentication credentials</li>
              <li>Community notices you created</li>
              <li>Offerings you submitted or bid on</li>
              <li>Complaints you filed</li>
              <li>Push notification preferences</li>
            </div>
          </div>

          <button
            onClick={handleDelete}
            disabled={deleting}
            className="inline-flex min-h-12 w-full items-center justify-center gap-2 rounded-xl bg-rose-600 px-5 py-3 text-base font-semibold text-white shadow-sm hover:bg-rose-700 disabled:cursor-wait disabled:opacity-60"
          >
            {deleting ? <Loader2 className="h-4 w-4 animate-spin" /> : <Trash2 className="h-5 w-5" />}
            {deleting ? 'Deleting…' : 'Delete My Account'}
          </button>

          {error && (
            <div className="mt-4 rounded-xl border border-rose-200 bg-rose-50 p-4 text-base text-rose-800">
              {error}
            </div>
          )}

          <div className="mt-8 pt-8 border-t border-slate-200 dark:border-slate-700 text-center">
            <p className="text-sm text-slate-600 dark:text-slate-400 mb-3">
              Have questions? Email us:
            </p>
            <a
              href={`mailto:${userEmail ? userEmail : 'yourname@gmail.com'}?subject=Account Deletion Question - Bodla Parivar`}
              className="text-sm font-medium text-saffron underline underline-offset-4 hover:text-amber-600 dark:hover:text-amber-400"
            >
              <Mail className="h-4 w-4" />
              {userEmail ? userEmail : 'yourname@gmail.com'}
            </a>
          </div>

          <div className="mt-6">
            <Link href="/" className="text-sm font-medium text-slate-600 dark:text-slate-400 hover:text-saffron dark:hover:text-amber-400">
              ← Back to Home
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
