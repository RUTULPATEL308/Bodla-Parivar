'use client';

import React, { FormEvent, useEffect, useRef, useState } from 'react';
import type { Session } from '@supabase/supabase-js';
import Image from 'next/image';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { AlertCircle, Bell, BookOpen, HeartHandshake, Home, PhoneCall, Send, Sparkles, Users, LogIn, LogOut, Sun, Moon } from 'lucide-react';
import Header from '@/components/Header';
import { AdminLanguageProvider, useAdminFeatures, useAdminLanguage, type AdminFeature } from '@/components/AdminLanguageContext';
import { ThemeProvider, useTheme } from '@/components/ThemeContext';
import MemberPortal from '@/components/MemberPortal';
import Sidebar from '@/components/Sidebar';
import { isSupabaseConfigured, supabase } from '@/lib/supabase';

type AccessState = 'checking' | 'signed-out' | 'staff' | 'feature-member' | 'member' | 'unauthorized' | 'password-recovery';
type AuthMode = 'login' | 'signup' | 'recover' | 'update-password';

const mobileAdminLinks = [
  { label: 'Home', href: '/', icon: Home },
  { label: 'Users', href: '/users', icon: Users, feature: 'members' as AdminFeature },
  { label: 'Notices', href: '/notices', icon: Bell, feature: 'notices' as AdminFeature },
  { label: 'Offerings', href: '/offerings', icon: HeartHandshake, feature: 'offerings' as AdminFeature },
  { label: 'Navratri', href: '/navratri', icon: Sparkles, feature: 'navratri' as AdminFeature },
  { label: 'Complaints', href: '/complaints', icon: AlertCircle, feature: 'complaints' as AdminFeature },
  { label: 'Emergency', href: '/emergency', icon: PhoneCall, feature: 'emergency_contacts' as AdminFeature },
  { label: 'Notifications', href: '/notifications', icon: Send, feature: 'notifications' as AdminFeature },
];

const featureByPath: Record<string, AdminFeature | null> = {
  '/': null,
  '/users': 'members', '/notices': 'notices', '/offerings': 'offerings', '/navratri': 'navratri',
  '/notifications': 'notifications', '/complaints': 'complaints', '/emergency': 'emergency_contacts', '/history': 'village_history',
};

function featureForPath(pathname: string) {
  const matchingPath = Object.keys(featureByPath)
    .sort((left, right) => right.length - left.length)
    .find((path) => pathname === path || (path !== '/' && pathname.startsWith(`${path}/`)));
  return matchingPath ? featureByPath[matchingPath] : undefined;
}

function MobileAdminFooter() {
  const pathname = usePathname();
  const { lang } = useAdminLanguage();
  const { features, isFullStaff } = useAdminFeatures();
  const links = mobileAdminLinks.filter((link) => link.href === '/' || isFullStaff || (link.feature != null && features.includes(link.feature)));

  return (
    <nav aria-label="Admin menu" style={{ gridTemplateColumns: `repeat(${links.length}, minmax(0, 1fr))` }} className="fixed inset-x-0 bottom-0 z-50 grid border-t border-slate-200 bg-white/95 pb-[env(safe-area-inset-bottom)] shadow-[0_-4px_16px_rgba(0,0,0,0.08)] backdrop-blur md:hidden dark:border-slate-800 dark:bg-slate-900/95">
      {links.map(({ label, href, icon: Icon }) => {
        const active = href === '/' ? pathname === '/' : pathname === href || pathname.startsWith(`${href}/`);
        return (
          <Link key={href} href={href} aria-current={active ? 'page' : undefined} className={`flex min-h-16 min-w-0 flex-col items-center justify-center gap-1 px-1 py-2 text-xs font-semibold ${active ? 'text-sky-600 dark:text-sky-400' : 'text-slate-500 dark:text-slate-400'}`}>
            <Icon aria-hidden="true" className="h-6 w-6" />
            <span className="truncate leading-5">{lang === 'gu' ? ({ Home: 'હોમ', Users: 'સભ્યો', Notices: 'સૂચનાઓ', Offerings: 'ચઢાવો', Navratri: 'નવરાત્રી', Complaints: 'ફરિયાદો', Emergency: 'કટોકટી', History: 'ઇતિહાસ', Notifications: 'પ્રસારણ' }[label] ?? label) : label}</span>
          </Link>
        );
      })}
    </nav>
  );
}

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
  return (
    <ThemeProvider>
      <AdminLanguageProvider>
        <AdminShellInner>{children}</AdminShellInner>
      </AdminLanguageProvider>
    </ThemeProvider>
  );
}

function AdminShellInner({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const [accessState, setAccessState] = useState<AccessState>(isSupabaseConfigured ? 'checking' : 'signed-out');
  const [authMode, setAuthMode] = useState<AuthMode>('login');
  const [email, setEmail] = useState('');
  const [fullName, setFullName] = useState('');
  const [password, setPassword] = useState('');
  const [signedInEmail, setSignedInEmail] = useState('');
  const [signedInName, setSignedInName] = useState('');
  const [authError, setAuthError] = useState<string | null>(isSupabaseConfigured ? null : 'Supabase is not configured for the admin app.');
  const [authNotice, setAuthNotice] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const authEventVersion = useRef(0);
  const [grantedFeatures, setGrantedFeatures] = useState<AdminFeature[]>([]);
  const [isFullStaff, setIsFullStaff] = useState(false);
  const { theme, toggleTheme } = useTheme();

  async function resolveAccess(session: Session | null) {
    if (!session) {
      setGrantedFeatures([]);
      setIsFullStaff(false);
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

      const { data: featurePermissions, error: featureError } = await supabase.rpc('current_user_feature_permissions');
      if (featureError) throw featureError;
      setGrantedFeatures(Array.isArray(featurePermissions) ? featurePermissions as AdminFeature[] : []);
      const { data, error } = await supabase.rpc('is_staff');
      if (error) throw error;
      // This SECURITY DEFINER RPC reads assignments through RLS and avoids
      // querying user_roles directly from the client, which can return no rows.
      const { data: roles, error: rolesError } = await supabase.rpc('current_user_roles');
      if (rolesError) throw rolesError;
      const normalizedRoles = Array.isArray(roles)
        ? roles.map((role) => String(role).trim().toUpperCase())
        : [];
      const hasAdministratorRole = normalizedRoles.some((role) => role === 'ADMIN' || role === 'SUPER_ADMIN');
      const isStaff = data === true || hasAdministratorRole;
      setIsFullStaff(isStaff);
      // Offerings are resident-facing: opening /offerings must render the
      // member bidding view even when no staff feature grant is assigned.
      if (!isStaff && Array.isArray(featurePermissions) && !['/', '/offerings'].includes(window.location.pathname)) {
        const feature = featureForPath(window.location.pathname);
        if (!feature || !featurePermissions.includes(feature)) {
          if (featurePermissions.length > 0) {
            setAccessState('feature-member');
          } else {
            setAuthError('This administration module requires an explicit feature grant or a staff role.');
            setAccessState('unauthorized');
          }
          return;
        }
      }
      setAccessState(isStaff ? 'staff' : window.location.pathname === '/offerings' ? 'member' : Array.isArray(featurePermissions) && featurePermissions.length > 0 ? 'feature-member' : 'member');
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
    if (!isSupabaseConfigured) return () => { active = false; };

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
    return (
      <main className="flex min-h-screen flex-1 items-center justify-center text-sm text-slate-500 dark:text-slate-400 dark:bg-slate-950">
        <div className="flex flex-col items-center gap-3">
          <div className="h-8 w-8 animate-spin rounded-full border-2 border-navy-900 border-t-transparent dark:border-sky-400" />
          <span>Checking account access…</span>
        </div>
      </main>
    );
  }

  const currentFeature = featureForPath(pathname);
  const routeNotGranted = accessState === 'feature-member'
    && pathname !== '/'
    && (!currentFeature || !grantedFeatures.includes(currentFeature));

  if (accessState === 'unauthorized' || routeNotGranted) {
    return (
      <main className="flex min-h-screen flex-1 items-center justify-center bg-slate-50 px-5 py-10 dark:bg-slate-950">
        <section className="w-full max-w-md rounded-3xl border border-slate-200 bg-white p-6 shadow-xl sm:p-8 dark:border-slate-800 dark:bg-slate-900">
          <p className="text-xs font-semibold uppercase text-sky-600 dark:text-sky-400">Bodla Parivar Console</p>
          <h1 className="mt-2 text-xl font-bold text-slate-900 dark:text-white">{routeNotGranted ? 'Feature access required' : 'Staff access required'}</h1>
          <p className="mt-2 text-sm text-slate-600 dark:text-slate-400">{authError || (routeNotGranted ? 'This feature is not assigned to your account.' : 'This account is signed in, but it does not have an admin or staff role.')}</p>
          {routeNotGranted ? (
            <Link href="/" className="mt-6 inline-flex items-center gap-2 rounded-xl bg-navy-900 px-4 py-2.5 text-sm font-semibold text-white hover:bg-navy-800 dark:bg-sky-600 dark:hover:bg-sky-500">
              <Home className="h-4 w-4" /> Back to your dashboard
            </Link>
          ) : (
            <button onClick={handleSignOut} className="mt-6 inline-flex items-center gap-2 rounded-xl bg-navy-900 px-4 py-2.5 text-sm font-semibold text-white hover:bg-navy-800 dark:bg-sky-600 dark:hover:bg-sky-500">
              <LogOut className="h-4 w-4" /> Sign out
            </button>
          )}
        </section>
      </main>
    );
  }

  if (accessState === 'member' || (accessState === 'feature-member' && (pathname === '/' || pathname === '/offerings'))) {
    return <MemberPortal email={signedInEmail} fullName={signedInName} onSignOut={handleSignOut} features={grantedFeatures} />;
  }

  if (accessState === 'signed-out' || accessState === 'password-recovery') {
    const isLogin = authMode === 'login';
    const isSignup = authMode === 'signup';
    const isRecoveryRequest = authMode === 'recover';
    const isPasswordUpdate = authMode === 'update-password';
    return (
      <main className="flex min-h-screen flex-1 items-center justify-center bg-slate-50 px-5 py-10 dark:bg-slate-950 transition-colors">
        <section className="w-full max-w-md rounded-3xl border border-slate-200/80 bg-white p-6 shadow-2xl sm:p-8 dark:border-slate-800 dark:bg-slate-900">
          <div className="flex justify-end mb-2">
            <button
              onClick={toggleTheme}
              className="flex h-8 w-8 items-center justify-center rounded-lg border border-slate-200 bg-slate-50 text-slate-600 dark:border-slate-800 dark:bg-slate-800 dark:text-slate-300"
              aria-label="Toggle theme"
            >
              {theme === 'dark' ? <Sun className="h-4 w-4 text-amber-400" /> : <Moon className="h-4 w-4 text-slate-700" />}
            </button>
          </div>

          <Image
            src="/bodla_parivar_logo.png"
            alt="Bodla Parivar"
            width={1069}
            height={258}
            priority
            className="mx-auto mb-3 h-auto w-full max-w-xs object-contain dark:brightness-110"
          />
          <div className="mb-7 text-center">
            <h1 className="text-xl font-bold text-slate-900 dark:text-white">Village Community Portal</h1>
            <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
              {isLogin ? 'Sign in with your community account.' : isSignup ? 'Create a resident community account.' : isRecoveryRequest ? 'Request a password reset message.' : 'Choose a new password.'}
            </p>
          </div>

          {(isLogin || isSignup) ? (
            <div className="mb-5 grid grid-cols-2 border-b border-slate-200 dark:border-slate-800" role="tablist" aria-label="Authentication">
              {(['login', 'signup'] as const).map((mode) => (
                <button
                  key={mode}
                  type="button"
                  role="tab"
                  aria-selected={authMode === mode}
                  onClick={() => { setAuthMode(mode); setAuthError(null); setAuthNotice(null); }}
                  className={`border-b-2 px-3 py-2.5 text-xs font-semibold transition ${authMode === mode ? 'border-navy-900 text-navy-900 dark:border-sky-400 dark:text-sky-400' : 'border-transparent text-slate-500 hover:text-slate-900 dark:text-slate-400 dark:hover:text-white'}`}
                >
                  {mode === 'login' ? 'Sign in' : 'Create account'}
                </button>
              ))}
            </div>
          ) : (
            <button type="button" onClick={() => { setAuthMode('login'); setAuthError(null); setAuthNotice(null); }} className="mb-5 text-xs font-semibold text-sky-600 hover:text-sky-700 dark:text-sky-400">
              Back to sign in
            </button>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            {isSignup && (
              <label className="block text-xs font-semibold uppercase text-slate-700 dark:text-slate-300">
                Full name
                <input required autoComplete="name" value={fullName} onChange={(event) => setFullName(event.target.value)} className="mt-1.5 w-full rounded-xl border border-slate-200 bg-white px-3.5 py-2.5 text-sm text-slate-900 outline-none focus:border-navy-900 dark:border-slate-700 dark:bg-slate-800 dark:text-white dark:focus:border-sky-400" />
              </label>
            )}
            {!isPasswordUpdate && (
              <label className="block text-xs font-semibold uppercase text-slate-700 dark:text-slate-300">
                Email
                <input required type="email" autoComplete="email" value={email} onChange={(event) => setEmail(event.target.value)} className="mt-1.5 w-full rounded-xl border border-slate-200 bg-white px-3.5 py-2.5 text-sm text-slate-900 outline-none focus:border-navy-900 dark:border-slate-700 dark:bg-slate-800 dark:text-white dark:focus:border-sky-400" />
              </label>
            )}
            {!isRecoveryRequest && (
              <label className="block text-xs font-semibold uppercase text-slate-700 dark:text-slate-300">
                {isPasswordUpdate ? 'New password' : 'Password'}
                <input required type="password" minLength={6} autoComplete={isLogin ? 'current-password' : 'new-password'} value={password} onChange={(event) => setPassword(event.target.value)} className="mt-1.5 w-full rounded-xl border border-slate-200 bg-white px-3.5 py-2.5 text-sm text-slate-900 outline-none focus:border-navy-900 dark:border-slate-700 dark:bg-slate-800 dark:text-white dark:focus:border-sky-400" />
              </label>
            )}

            {!isSupabaseConfigured && <p role="alert" className="text-xs text-rose-600 dark:text-rose-400">Supabase is not configured for the admin app.</p>}
            {authError && <p role="alert" className="text-xs text-rose-600 dark:text-rose-400">{authError}</p>}
            {authNotice && <p role="status" className="rounded-xl border border-emerald-200 bg-emerald-50 p-3 text-xs text-emerald-800 dark:border-emerald-800 dark:bg-emerald-950/80 dark:text-emerald-300">{authNotice}</p>}

            <button type="submit" disabled={isSubmitting || !isSupabaseConfigured} className="inline-flex min-h-12 w-full items-center justify-center gap-2 rounded-xl bg-navy-900 px-4 py-3 text-xs font-semibold text-white shadow-sm transition hover:bg-navy-800 disabled:cursor-not-allowed disabled:opacity-50 dark:bg-sky-600 dark:hover:bg-sky-500">
              <LogIn className="h-4 w-4" />
              {isSubmitting ? 'Please wait…' : isLogin ? 'Sign in' : isSignup ? 'Create account' : isRecoveryRequest ? 'Send recovery email' : 'Update password'}
            </button>
          </form>
          {isLogin && (
            <button type="button" onClick={() => { setAuthMode('recover'); setAuthError(null); setAuthNotice(null); }} className="mt-4 text-xs font-medium text-sky-600 hover:text-sky-700 dark:text-sky-400">
              Forgot password?
            </button>
          )}
          <p className="mt-5 text-[11px] leading-relaxed text-slate-400 dark:text-slate-500 text-center">Creating an account provides immediate access to the Resident Portal. Staff access with elevated administration tools is granted by system administrators.</p>
        </section>
      </main>
    );
  }

  return (
    <AdminFeaturesProvider features={grantedFeatures} isFullStaff={isFullStaff}>
      <div className="flex min-h-screen flex-1 flex-col md:flex-row bg-slate-50 dark:bg-slate-950 transition-colors">
        <Sidebar />
        <div className="flex min-w-0 flex-1 flex-col">
          <Header email={signedInEmail} onSignOut={handleSignOut} />
          <main id="main-content" className="flex-1 overflow-y-auto p-4 pb-28 md:p-8">{children}</main>
        </div>
        <MobileAdminFooter />
      </div>
    </AdminFeaturesProvider>
  );
}

function AdminFeaturesProvider({ features, isFullStaff, children }: { features: AdminFeature[]; isFullStaff: boolean; children: React.ReactNode }) {
  const { setFeatures, setIsFullStaff } = useAdminFeatures();
  useEffect(() => { setFeatures(features); setIsFullStaff(isFullStaff); }, [features, isFullStaff, setFeatures, setIsFullStaff]);
  return <>{children}</>;
}
