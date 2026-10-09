'use client';

import React, { FormEvent, useEffect, useRef, useState } from 'react';
import type { Session } from '@supabase/supabase-js';
import Image from 'next/image';
import { LogIn, LogOut } from 'lucide-react';
import Header from '@/components/Header';
import MemberPortal from '@/components/MemberPortal';
import Sidebar from '@/components/Sidebar';
import { isSupabaseConfigured, supabase } from '@/lib/supabase';

type AccessState = 'checking' | 'signed-out' | 'staff' | 'member' | 'unauthorized' | 'password-recovery';
type AuthMode = 'login' | 'signup' | 'recover' | 'update-password';

function removeAuthRedirectParams() {
  const url = new URL(window.location.href);
  const hash = new URLSearchParams(url.hash.slice(1));
  const hasAuthHash = hash.has('access_token') || hash.has('refresh_token');
  const hasAuthCode = url.searchParams.has('code');

  if (!hasAuthHash && !hasAuthCode) return;

  if (hasAuthHash) url.hash = '';
  if (hasAuthCode) url.searchParams.delete('code');
  window.history.replaceState(window.history.state, '', `${url.pathname}${url.search}${url.hash}`);
}

function readableAuthError(error: unknown) {
  const message = error instanceof Error ? error.message.toLowerCase() : '';
  if (message.includes('email rate limit') || message.includes('rate limit')) {
    return 'Too many email requests. Wait for the limit to reset, then try once. For development, use a configured SMTP provider or disable email confirmation in Supabase Auth.';
  }
  if (message.includes('invalid login credentials')) {
    return 'Email or password is incorrect. Check your details or use Forgot password.';
  }
  if (message.includes('email not confirmed')) {
    return 'Confirm your email using the latest message from Supabase, then sign in.';
  }
  return error instanceof Error ? error.message : 'Authentication failed. Please try again.';
}

export default function AdminShell({ children }: { children: React.ReactNode }) {
  const [accessState, setAccessState] = useState<AccessState>('checking');
  const [authMode, setAuthMode] = useState<AuthMode>('login');
  const [email, setEmail] = useState('');
  const [fullName, setFullName] = useState('');
  const [password, setPassword] = useState('');
  const [signedInEmail, setSignedInEmail] = useState('');
  const [signedInName, setSignedInName] = useState('');
  const [authError, setAuthError] = useState<string | null>(null);
  const [authNotice, setAuthNotice] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const authEventVersion = useRef(0);

  async function resolveAccess(session: Session | null) {
    if (!session) {
      setSignedInEmail('');
      setAccessState('signed-out');
      return;
    }

    removeAuthRedirectParams();
    try {
      const { data: sessionData, error: sessionError } = await supabase.auth.getSession();
      if (sessionError) throw sessionError;
      if (!sessionData.session) {
        setSignedInEmail('');
        setSignedInName('');
        setAuthMode('login');
        setAuthError('Your sign-in session is no longer active. Please sign in again.');
        setAccessState('signed-out');
        return;
      }

      const { data: { user }, error: userError } = await supabase.auth.getUser();
      if (userError) throw userError;
      if (!user) {
        setAuthError('Supabase could not verify this session. Please sign in again.');
        setAccessState('signed-out');
        return;
      }

      setSignedInEmail(user.email ?? session.user.email ?? '');
      const metadataName = typeof user.user_metadata.full_name === 'string' ? user.user_metadata.full_name : '';
      const { data: profile, error: profileError } = await supabase
        .from('profiles')
        .select('full_name')
        .eq('auth_user_id', user.id)
        .maybeSingle();
      if (profileError) throw profileError;
      setSignedInName(profile?.full_name || metadataName);

      const { data, error } = await supabase.rpc('is_staff');
      if (error) throw error;
      setAccessState(data === true ? 'staff' : 'member');
    } catch (error) {
      const message = error instanceof Error ? error.message.toLowerCase() : '';
      if (message.includes('auth session missing')) {
        setSignedInEmail('');
        setSignedInName('');
        setAuthMode('login');
        setAuthError('Your sign-in session expired before it could be verified. Please sign in again.');
        setAccessState('signed-out');
      } else {
        setAuthError(readableAuthError(error));
        setAccessState('unauthorized');
      }
    }
  }

  useEffect(() => {
    let active = true;
    if (!isSupabaseConfigured) {
      setAuthError('Supabase is not configured for the admin app.');
      setAccessState('signed-out');
    }

    const { data: { subscription } } = supabase.auth.onAuthStateChange((event, session) => {
      if (!active) return;
      if (event === 'SIGNED_OUT') {
        authEventVersion.current += 1;
        setSignedInEmail('');
        setSignedInName('');
        setPassword('');
        setAuthMode('login');
        setAccessState('signed-out');
        return;
      }
      if (event === 'PASSWORD_RECOVERY') {
        authEventVersion.current += 1;
        if (session) removeAuthRedirectParams();
        setEmail(session?.user.email ?? '');
        setPassword('');
        setAuthMode('update-password');
        setAccessState('password-recovery');
        return;
      }
      if (event === 'TOKEN_REFRESHED') {
        if (session) removeAuthRedirectParams();
        return;
      }
      if (event === 'INITIAL_SESSION' || event === 'SIGNED_IN' || event === 'USER_UPDATED') {
        const eventVersion = ++authEventVersion.current;
        window.setTimeout(() => {
          if (active && eventVersion === authEventVersion.current) void resolveAccess(session);
        }, 0);
      }
    });

    return () => {
      active = false;
      subscription.unsubscribe();
    };
  }, []);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setAuthError(null);
    setAuthNotice(null);
    setIsSubmitting(true);

    try {
      if (authMode === 'login') {
        const { error } = await supabase.auth.signInWithPassword({ email: email.trim(), password });
        if (error) throw error;
      } else if (authMode === 'signup') {
        const { data, error } = await supabase.auth.signUp({
          email: email.trim(),
          password,
          options: {
            data: { full_name: fullName.trim(), preferred_language: 'gu' },
            emailRedirectTo: window.location.origin,
          },
        });
        if (error) throw error;

        if (!data.session) {
          setAuthNotice('Your account has been created. Check your email to confirm it, then sign in. Staff access must be granted separately by an administrator.');
        }
      } else if (authMode === 'recover') {
        const { error } = await supabase.auth.resetPasswordForEmail(email.trim(), {
          redirectTo: window.location.origin,
        });
        if (error) throw error;
        setAuthNotice('If an account exists for this email, a password recovery message has been sent.');
      } else {
        const { error } = await supabase.auth.updateUser({ password });
        if (error) throw error;
        await supabase.auth.signOut();
        setAuthMode('login');
        setAuthNotice('Password updated. Sign in with your new password.');
      }
    } catch (error) {
      setAuthError(readableAuthError(error));
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleSignOut = async () => {
    await supabase.auth.signOut();
    setPassword('');
    setAuthError(null);
  };

  if (accessState === 'checking') {
    return <main className="flex min-h-screen flex-1 items-center justify-center text-sm text-brand-muted">Checking account access…</main>;
  }

  if (accessState === 'unauthorized') {
    return (
      <main className="flex min-h-screen flex-1 items-center justify-center bg-cream px-5 py-10">
        <section className="w-full max-w-md border border-brand-border bg-cream-surface p-7 shadow-sm">
          <p className="text-xs font-semibold uppercase text-saffron">Bodla Parivar Admin</p>
          <h1 className="mt-2 text-xl font-bold text-brand-text">Staff access required</h1>
          <p className="mt-2 text-sm text-brand-muted">{authError || 'This account is signed in, but it does not have an admin or staff role.'}</p>
          <button onClick={handleSignOut} className="mt-6 inline-flex items-center gap-2 bg-saffron px-4 py-2.5 text-sm font-semibold text-white hover:bg-saffron-dark">
            <LogOut className="h-4 w-4" /> Sign out
          </button>
        </section>
      </main>
    );
  }

  if (accessState === 'member') {
    return <MemberPortal email={signedInEmail} fullName={signedInName} onSignOut={handleSignOut} />;
  }

  if (accessState === 'signed-out' || accessState === 'password-recovery') {
    const isLogin = authMode === 'login';
    const isSignup = authMode === 'signup';
    const isRecoveryRequest = authMode === 'recover';
    const isPasswordUpdate = authMode === 'update-password';
    return (
      <main className="flex min-h-screen flex-1 items-center justify-center bg-cream px-5 py-10">
        <section className="w-full max-w-md border border-brand-border bg-cream-surface p-7 shadow-sm">
          <Image
            src="/bodla_parivar_logo.png"
            alt="Bodla Parivar"
            width={1069}
            height={258}
            priority
            className="mx-auto mb-3 h-auto w-full max-w-xs object-contain"
          />
          <div className="mb-7">
            <h1 className="text-center text-xl font-bold text-brand-text">Admin Console</h1>
            <p className="mt-1 text-sm text-brand-muted">
              {isLogin ? 'Sign in with your community account.' : isSignup ? 'Create a community account.' : isRecoveryRequest ? 'Request a password reset message.' : 'Choose a new password.'}
            </p>
          </div>

          {(isLogin || isSignup) ? (
            <div className="mb-5 grid grid-cols-2 border-b border-brand-border" role="tablist" aria-label="Authentication">
              {(['login', 'signup'] as const).map((mode) => (
                <button
                  key={mode}
                  type="button"
                  role="tab"
                  aria-selected={authMode === mode}
                  onClick={() => { setAuthMode(mode); setAuthError(null); setAuthNotice(null); }}
                  className={`border-b-2 px-3 py-2.5 text-sm font-semibold ${authMode === mode ? 'border-saffron text-saffron' : 'border-transparent text-brand-muted hover:text-brand-text'}`}
                >
                  {mode === 'login' ? 'Sign in' : 'Create account'}
                </button>
              ))}
            </div>
          ) : (
            <button type="button" onClick={() => { setAuthMode('login'); setAuthError(null); setAuthNotice(null); }} className="mb-5 text-sm font-semibold text-saffron hover:text-saffron-dark">
              Back to sign in
            </button>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            {isSignup && (
              <label className="block text-sm font-medium text-brand-text">
                Full name
                <input required autoComplete="name" value={fullName} onChange={(event) => setFullName(event.target.value)} className="mt-1.5 w-full border border-brand-border bg-white px-3 py-2.5 text-sm outline-none focus:border-saffron" />
              </label>
            )}
            {!isPasswordUpdate && (
              <label className="block text-sm font-medium text-brand-text">
                Email
                <input required type="email" autoComplete="email" value={email} onChange={(event) => setEmail(event.target.value)} className="mt-1.5 w-full border border-brand-border bg-white px-3 py-2.5 text-sm outline-none focus:border-saffron" />
              </label>
            )}
            {!isRecoveryRequest && (
              <label className="block text-sm font-medium text-brand-text">
                {isPasswordUpdate ? 'New password' : 'Password'}
                <input required type="password" minLength={6} autoComplete={isLogin ? 'current-password' : 'new-password'} value={password} onChange={(event) => setPassword(event.target.value)} className="mt-1.5 w-full border border-brand-border bg-white px-3 py-2.5 text-sm outline-none focus:border-saffron" />
              </label>
            )}

            {!isSupabaseConfigured && <p role="alert" className="text-sm text-rose-700">Supabase is not configured for the admin app.</p>}
            {authError && <p role="alert" className="text-sm text-rose-700">{authError}</p>}
            {authNotice && <p role="status" className="border border-leaf/30 bg-leaf/10 p-3 text-sm text-brand-text">{authNotice}</p>}

            <button type="submit" disabled={isSubmitting || !isSupabaseConfigured} className="inline-flex w-full items-center justify-center gap-2 bg-saffron px-4 py-3 text-sm font-semibold text-white hover:bg-saffron-dark disabled:opacity-50">
              <LogIn className="h-4 w-4" />
              {isSubmitting ? 'Please wait…' : isLogin ? 'Sign in' : isSignup ? 'Create account' : isRecoveryRequest ? 'Send recovery email' : 'Update password'}
            </button>
          </form>
          {isLogin && (
            <button type="button" onClick={() => { setAuthMode('recover'); setAuthError(null); setAuthNotice(null); }} className="mt-4 text-sm font-medium text-saffron hover:text-saffron-dark">
              Forgot password?
            </button>
          )}
          <p className="mt-5 text-xs leading-relaxed text-brand-muted">Creating an account does not grant staff access. An administrator must assign an authorized role before the console can be opened.</p>
        </section>
      </main>
    );
  }

  return (
    <div className="flex min-h-screen flex-1">
      <Sidebar />
      <div className="flex min-w-0 flex-1 flex-col">
        <Header email={signedInEmail} onSignOut={handleSignOut} />
        <main className="flex-1 overflow-y-auto p-6 md:p-8">{children}</main>
      </div>
    </div>
  );
}