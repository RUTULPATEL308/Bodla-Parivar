'use client';

import React from 'react';
import { Globe, LogOut, User } from 'lucide-react';
import { useAdminLanguage } from '@/components/AdminLanguageContext';

export default function Header({ email, onSignOut }: { email: string; onSignOut: () => void }) {
  const { lang, toggleLanguage } = useAdminLanguage();

  return (
    <header className="sticky top-0 z-30 flex min-h-16 items-center justify-between gap-3 border-b border-brand-border bg-cream-surface px-3 py-2 sm:px-4 md:px-6">
      <div>
        <h2 className="text-sm font-semibold text-brand-text sm:text-base">
          {lang === 'gu' ? 'બોદલા ગ્રામીણ વ્યવસ્થાપન પેનલ' : 'Bodla Village Management Portal'}
        </h2>
        <p className="text-xs text-brand-muted">
          {lang === 'gu' ? 'મહેસાણા જિલ્લો, ગુજરાત' : 'Mehsana District, Gujarat'}
        </p>
      </div>

      <div className="flex shrink-0 items-center gap-2 sm:gap-3 md:gap-4">
        {/* Language Toggle */}
        <button
          onClick={toggleLanguage}
          className="flex min-h-11 items-center gap-1.5 rounded-xl border border-brand-border px-2.5 py-2 text-sm font-semibold text-brand-text transition hover:bg-saffron-container sm:px-3"
        >
          <Globe aria-hidden="true" className="h-4 w-4 text-saffron" />
          <span>{lang === 'gu' ? 'English' : 'ગુજરાતી'}</span>
        </button>

        {/* Admin Profile Pill */}
        <div className="flex items-center gap-2.5 pl-3 border-l border-brand-border">
          <div className="flex h-10 w-10 items-center justify-center rounded-full bg-saffron-container text-saffron">
            <User aria-hidden="true" className="h-5 w-5" />
          </div>
          <div className="hidden sm:block text-left">
            <span className="block max-w-48 truncate text-xs font-semibold text-brand-text leading-tight">{email || 'Staff account'}</span>
            <span className="block text-[11px] text-leaf font-medium">AUTHORIZED STAFF</span>
          </div>
        </div>
        <button
          type="button"
          onClick={onSignOut}
          title="Sign out"
          aria-label="Sign out"
          className="flex min-h-11 items-center gap-2 rounded-xl border border-brand-border px-3 py-2 text-sm font-semibold text-brand-text transition hover:bg-saffron-container"
        >
          <LogOut aria-hidden="true" className="h-4 w-4" />
          <span>Sign out</span>
        </button>
      </div>
    </header>
  );
}
