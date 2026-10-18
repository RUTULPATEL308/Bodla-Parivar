'use client';

import React from 'react';
import Link from 'next/link';
import NextImage from 'next/image';
import { usePathname } from 'next/navigation';
import { useAdminFeatures, useAdminLanguage } from '@/components/AdminLanguageContext';
import {
  LayoutDashboard,
  Users,
  Bell,
  Send,
  Sparkles,
  WalletCards,
  AlertCircle,
  BookOpen,
  ClipboardList,
  PhoneCall,
  Settings,
  ShieldCheck,
  ChevronRight
} from 'lucide-react';

interface NavItem {
  name: string;
  nameGu: string;
  href: string;
  icon: React.ElementType;
}

const navItems: NavItem[] = [
  { name: 'Dashboard', nameGu: 'ડેશબોર્ડ', href: '/', icon: LayoutDashboard },
  { name: 'Users & Roles', nameGu: 'વપરાશકર્તાઓ', href: '/users', icon: Users },
  { name: 'Notices', nameGu: 'સૂચનાઓ', href: '/notices', icon: Bell },
  { name: 'Offerings', nameGu: 'ચઢાવો', href: '/offerings', icon: WalletCards },
  { name: 'Navratri', nameGu: 'નવરાત્રી', href: '/navratri', icon: Sparkles },
  { name: 'Notifications', nameGu: 'સૂચના પ્રસારણ', href: '/notifications', icon: Send },
  { name: 'Complaints', nameGu: 'ફરિયાદો', href: '/complaints', icon: AlertCircle },
  { name: 'Emergency Contacts', nameGu: 'કટોકટી સંપર્ક', href: '/emergency', icon: PhoneCall },
  { name: 'Village History', nameGu: 'ગામનો ઇતિહાસ', href: '/history', icon: BookOpen },
  { name: 'Roles & Permissions', nameGu: 'પરવાનગીઓ', href: '/roles', icon: ShieldCheck },
  { name: 'Audit Logs', nameGu: 'ઓડિટ લોગ', href: '/audit-logs', icon: ClipboardList },
  { name: 'Village Settings', nameGu: 'સેટિંગ્સ', href: '/settings', icon: Settings },
];

export default function Sidebar() {
  const pathname = usePathname();
  const { lang } = useAdminLanguage();
  const { features, isFullStaff } = useAdminFeatures();
  const isGujarati = lang === 'gu';

  const featureByPath: Record<string, string> = {
    '/users': 'members',
    '/notices': 'notices',
    '/offerings': 'offerings',
    '/navratri': 'navratri',
    '/notifications': 'notifications',
    '/complaints': 'complaints',
    '/emergency': 'emergency_contacts',
    '/history': 'village_history',
  };

  const visibleItems = navItems.filter((item) => {
    if (isFullStaff) return true;
    if (item.href === '/') return true;
    if (item.href === '/roles' || item.href === '/audit-logs' || item.href === '/settings') return false;
    const feature = featureByPath[item.href];
    return Boolean(feature && features.includes(feature as never));
  });

  return (
    <aside className="hidden w-72 shrink-0 border-r border-slate-200/80 bg-white md:flex md:min-h-screen md:flex-col shadow-sm dark:border-slate-800/80 dark:bg-slate-900 transition-colors">
      {/* Brand Header */}
      <div className="p-5 border-b border-slate-100 bg-gradient-to-b from-slate-50 to-white dark:border-slate-800 dark:from-slate-850 dark:to-slate-900">
        <div className="flex flex-col items-center gap-2">
          <NextImage
            src="/bodla_parivar_logo.png"
            alt="બોદલા પરિવાર"
            width={1069}
            height={258}
            className="h-auto w-full object-contain drop-shadow-sm dark:brightness-110"
            priority
          />
          <div className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-navy-50 text-navy-800 text-[11px] font-bold tracking-wide dark:bg-sky-950/80 dark:text-sky-300">
            <span className="w-1.5 h-1.5 rounded-full bg-amber-500 animate-pulse" />
            {isFullStaff ? 'ADMIN SUITE' : 'COMMUNITY TOOLS'}
          </div>
        </div>
      </div>

      {/* Navigation Links */}
      <nav aria-label={isFullStaff ? 'Main administration' : 'My assigned community tools'} className="flex-1 space-y-1 overflow-y-auto p-3">
        {visibleItems.map((item) => {
          const isActive = pathname === item.href;
          const Icon = item.icon;

          return (
            <Link
              key={item.href}
              href={item.href}
              className={`group flex items-center justify-between rounded-xl px-3.5 py-2.5 text-xs font-semibold transition-all duration-200 ${
                isActive
                  ? 'bg-navy-900 text-white shadow-sm dark:bg-sky-600 dark:text-white'
                  : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900 dark:text-slate-400 dark:hover:bg-slate-800 dark:hover:text-white'
              }`}
            >
              <div className="flex items-center gap-3">
                <Icon
                  className={`h-4 w-4 transition-colors ${
                    isActive ? 'text-white' : 'text-slate-500 group-hover:text-navy-900 dark:text-slate-400 dark:group-hover:text-sky-400'
                  }`}
                />
                <span>{isGujarati ? item.nameGu : item.name}</span>
              </div>
              <ChevronRight
                className={`h-3.5 w-3.5 opacity-0 transition-opacity ${
                  isActive ? 'opacity-100 text-white' : 'group-hover:opacity-60'
                }`}
              />
            </Link>
          );
        })}
      </nav>

      {/* Footer System Status */}
      <div className="p-4 border-t border-slate-100 bg-slate-50/50 dark:border-slate-800 dark:bg-slate-850">
        <div className="flex items-center justify-between text-[11px]">
          <span className="text-slate-500 font-medium dark:text-slate-400">System Status</span>
          <span className="inline-flex items-center gap-1 font-semibold text-emerald-600 dark:text-emerald-400">
            <span className="h-1.5 w-1.5 rounded-full bg-emerald-500" /> Operational
          </span>
        </div>
        <p className="mt-1 text-[10px] text-slate-400 dark:text-slate-500">Bodla Parivar Console v2.0</p>
      </div>
    </aside>
  );
}
