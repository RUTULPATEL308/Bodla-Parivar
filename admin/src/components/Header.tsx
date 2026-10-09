'use client';

import React, { useState } from 'react';
import { Globe, LogOut, User } from 'lucide-react';

export default function Header({ email, onSignOut }: { email: string; onSignOut: () => void }) {
  const [lang, setLang] = useState<'gu' | 'en'>('gu');

  return (
    <header className="h-16 bg-cream-surface border-b border-brand-border px-6 flex items-center justify-between sticky top-0 z-30">
      <div>
        <h2 className="text-base font-semibold text-brand-text">
          {lang === 'gu' ? 'બોદલા ગ્રામીણ વ્યવસ્થાપન પેનલ' : 'Bodla Village Management Portal'}
        </h2>
        <p className="text-xs text-brand-muted">
          {lang === 'gu' ? 'મહેસાણા જિલ્લો, ગુજરાત' : 'Mehsana District, Gujarat'}
        </p>
      </div>

      <div className="flex items-center gap-4">
        {/* Language Toggle */}
        <button
          onClick={() => setLang(lang === 'gu' ? 'en' : 'gu')}
          className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-brand-border hover:bg-saffron-container text-xs font-medium text-brand-text transition"
        >
          <Globe className="w-3.5 h-3.5 text-saffron" />
          <span>{lang === 'gu' ? 'English' : 'ગુજરાતી'}</span>
        </button>

        {/* Admin Profile Pill */}
        <div className="flex items-center gap-2.5 pl-3 border-l border-brand-border">
          <div className="w-8 h-8 rounded-full bg-saffron-container flex items-center justify-center text-saffron">
            <User className="w-4 h-4" />
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
          className="flex items-center gap-2 border border-brand-border px-3 py-2 text-xs font-medium text-brand-text transition hover:bg-saffron-container"
        >
          <LogOut className="h-4 w-4" />
          <span className="hidden sm:inline">Sign out</span>
        </button>
      </div>
    </header>
  );
}
