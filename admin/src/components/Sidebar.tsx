'use client';

import React from 'react';
import Link from 'next/link';
import NextImage from 'next/image';
import { usePathname } from 'next/navigation';
import { useAdminFeatures } from '@/components/AdminLanguageContext';
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
  ShieldCheck
} from 'lucide-react';

interface NavItem {
  name: string;
  nameGu: string;
  href: string;
  icon: React.ElementType;
}

const navItems: NavItem[] = [
  { name: 'Dashboard', nameGu: 'ડેશબોર્ડ', href: '/', icon: LayoutDashboard },
  { name: 'Users', nameGu: 'વપરાશકર્તાઓ', href: '/users', icon: Users },
  { name: 'Notices', nameGu: 'સૂચનાઓ', href: '/notices', icon: Bell },
  { name: 'Offerings', nameGu: 'ચઢાવો', href: '/offerings', icon: WalletCards },
  { name: 'Navratri', nameGu: 'નવરાત્રી', href: '/navratri', icon: Sparkles },
  { name: 'Notifications', nameGu: 'સૂચના પ્રસારણ', href: '/notifications', icon: Send },
  { name: 'Complaints', nameGu: '', href: '/complaints', icon: AlertCircle },
  { name: 'Emergency contacts', nameGu: '', href: '/emergency', icon: PhoneCall },
  { name: 'Village history', nameGu: '', href: '/history', icon: BookOpen },
  { name: 'Roles', nameGu: '', href: '/roles', icon: ShieldCheck },
  { name: 'Audit logs', nameGu: '', href: '/audit-logs', icon: ClipboardList },
  { name: 'Settings', nameGu: '', href: '/settings', icon: Settings },
];

export default function Sidebar() {
  const pathname = usePathname();
  const { features, isFullStaff } = useAdminFeatures();
  const featureByPath: Record<string, string> = {
    '/users': 'members', '/notices': 'notices', '/offerings': 'offerings', '/navratri': 'navratri',
    '/notifications': 'notifications', '/complaints': 'complaints', '/emergency': 'emergency_contacts', '/history': 'village_history',
  };
  const visibleItems = navItems.filter((item) => {
    if (isFullStaff) return true;
    if (item.href === '/') return true;
    if (item.href === '/roles' || item.href === '/audit-logs' || item.href === '/settings') return false;
    const feature = featureByPath[item.href];
    return Boolean(feature && features.includes(feature as never));
  });

  return (
    <aside className="hidden w-72 shrink-0 border-r border-brand-border bg-cream-surface md:flex md:min-h-screen md:flex-col">
      {/* Brand Header */}
      <div className="p-5 border-b border-brand-border">
        <div className="flex flex-col items-center gap-2">
          <NextImage
            src="/bodla_parivar_logo.png"
            alt="બોદલા પરિવાર"
            width={1069}
            height={258}
            className="h-auto w-full object-contain"
          />
          <p className="text-xs text-brand-muted">Admin Console</p>
        </div>
      </div>

      {/* Navigation Links */}
      <nav aria-label="Main administration" className="flex-1 space-y-1 overflow-y-auto p-3">
        {visibleItems.map((item) => {
          const isActive = pathname === item.href;
          const Icon = item.icon;

          return (
            <Link
              key={item.href}
              href={item.href}
              aria-current={isActive ? 'page' : undefined}
              className={`flex min-h-14 items-center gap-3 rounded-xl px-3 py-3 text-base font-semibold transition-all ${
                isActive
                  ? 'bg-saffron text-white shadow-sm'
                  : 'text-brand-text hover:bg-saffron-container hover:text-saffron'
              }`}
            >
              <Icon className="w-5 h-5 shrink-0" />
              <div className="truncate">
                <span className="block leading-none">{item.name}</span>
                {item.nameGu && <span className={`mt-1 block text-xs ${isActive ? 'text-white/90' : 'text-brand-muted'}`}>{item.nameGu}</span>}
              </div>
            </Link>
          );
        })}
      </nav>

      {/* Footer Info */}
      <div className="p-4 border-t border-brand-border text-xs text-brand-muted text-center">
        બોદલા, મહેસાણા, ગુજરાત
      </div>
    </aside>
  );
}
