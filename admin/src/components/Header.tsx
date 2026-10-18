'use client';

import React from 'react';
import { Globe, LogOut, User, Shield, Moon, Sun } from 'lucide-react';
import { useAdminLanguage } from '@/components/AdminLanguageContext';
import { useTheme } from '@/components/ThemeContext';

export default function Header({ email, onSignOut }: { email: string; onSignOut: () => void }) {
  const { lang, toggleLanguage } = useAdminLanguage();
  const { theme, toggleTheme } = useTheme();

  return (
    <header className="sticky top-0 z-30 flex min-h-16 items-center justify-between gap-3 border-b border-slate-200/80 bg-white/95 px-4 py-2.5 backdrop-blur-md sm:px-6 shadow-[0_1px_2px_rgba(0,0,0,0.03)] dark:border-slate-800/80 dark:bg-slate-900/95">
      <div>
        <h2 className="text-sm font-bold text-navy-900 sm:text-base flex items-center gap-2 dark:text-white">
          <span>{lang === 'gu' ? 'બોદલા ગ્રામીણ વ્યવસ્થાપન પેનલ' : 'Bodla Village Management Portal'}</span>
        </h2>
        <p className="text-xs text-slate-500 font-medium dark:text-slate-400">
          {lang === 'gu' ? 'મહેસાણા જિલ્લો, ગુજરાત' : 'Mehsana District, Gujarat'}
        </p>
      </div>

      <div className="flex shrink-0 items-center gap-2 sm:gap-3 md:gap-4">
        {/* Dark / Light Mode Toggle */}
        <button
          onClick={toggleTheme}
          className="flex h-9 w-9 items-center justify-center rounded-xl border border-slate-200 bg-slate-50 text-slate-600 transition hover:bg-slate-100 dark:border-slate-800 dark:bg-slate-800 dark:text-slate-300 dark:hover:bg-slate-700 active:scale-95"
          title={theme === 'dark' ? 'Switch to Light Mode' : 'Switch to Dark Mode'}
          aria-label="Toggle color theme"
        >
          {theme === 'dark' ? <Sun className="h-4 w-4 text-amber-400" /> : <Moon className="h-4 w-4 text-slate-700" />}
        </button>

        {/* Language Toggle */}
        <button
          onClick={toggleLanguage}
          className="flex h-9 items-center gap-1.5 rounded-xl border border-slate-200 bg-slate-50 px-3 text-xs font-semibold text-slate-700 shadow-sm transition hover:bg-slate-100 hover:border-slate-300 active:scale-95 dark:border-slate-800 dark:bg-slate-800 dark:text-slate-300 dark:hover:bg-slate-700"
          title="Toggle UI Language"
        >
          <Globe aria-hidden="true" className="h-3.5 w-3.5 text-navy-600 dark:text-sky-400" />
          <span>{lang === 'gu' ? 'English' : 'ગુજરાતી'}</span>
        </button>

        {/* Admin Profile Pill */}
        <div className="flex items-center gap-2.5 pl-3 border-l border-slate-200 dark:border-slate-800">
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-navy-50 text-navy-900 border border-navy-100 font-bold text-xs shadow-sm dark:bg-slate-800 dark:text-white dark:border-slate-700">
            <User aria-hidden="true" className="h-4 w-4 text-navy-700 dark:text-sky-400" />
          </div>
          <div className="hidden sm:block text-left">
            <span className="block max-w-44 truncate text-xs font-bold text-slate-800 leading-tight dark:text-slate-200">
              {email || 'Staff Account'}
            </span>
            <span className="inline-flex items-center gap-1 text-[10px] text-emerald-700 font-bold dark:text-emerald-400">
              <Shield className="w-2.5 h-2.5 text-emerald-600 dark:text-emerald-400" /> AUTHORIZED STAFF
            </span>
          </div>
        </div>

        {/* Sign out */}
        <button
          type="button"
          onClick={onSignOut}
          title="Sign out"
          aria-label="Sign out"
          className="flex h-9 items-center gap-1.5 rounded-xl border border-slate-200 bg-white px-3 text-xs font-semibold text-slate-600 shadow-sm transition hover:bg-rose-50 hover:text-rose-700 hover:border-rose-200 active:scale-95 dark:border-slate-800 dark:bg-slate-800 dark:text-slate-300 dark:hover:bg-rose-950/60 dark:hover:text-rose-400"
        >
          <LogOut aria-hidden="true" className="h-3.5 w-3.5" />
          <span className="hidden sm:inline">Sign out</span>
        </button>
      </div>
    </header>
  );
}
