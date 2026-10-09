'use client';

import React from 'react';
import Link from 'next/link';
import NextImage from 'next/image';
import { usePathname } from 'next/navigation';
import {
  LayoutDashboard,
  Users,
  Shield,
  Bell,
  Calendar,
  Building2,
  Briefcase,
  HeartHandshake,
  AlertCircle,
  PhoneCall,
  MapPin,
  Image,
  Send,
  Flag,
  History,
  Settings
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
  { name: 'Roles & Permissions', nameGu: 'ભૂમિકાઓ અને પરવાનગી', href: '/roles', icon: Shield },
  { name: 'Notices', nameGu: 'સૂચનાઓ', href: '/notices', icon: Bell },
  { name: 'Events', nameGu: 'કાર્યક્રમો', href: '/events', icon: Calendar },
  { name: 'Businesses', nameGu: 'વ્યવસાયો', href: '/businesses', icon: Building2 },
  { name: 'Jobs', nameGu: 'નોકરીઓ', href: '/jobs', icon: Briefcase },
  { name: 'Offerings / ચઢાવો', nameGu: 'ચઢાવો', href: '/offerings', icon: HeartHandshake },
  { name: 'Complaints', nameGu: 'ફરિયાદો', href: '/complaints', icon: AlertCircle },
  { name: 'Emergency Contacts', nameGu: 'ઈમરજન્સી સંપર્કો', href: '/emergency', icon: PhoneCall },
  { name: 'Places', nameGu: 'સ્થળો', href: '/places', icon: MapPin },
  { name: 'Gallery', nameGu: 'ગેલેરી', href: '/gallery', icon: Image },
  { name: 'Notifications', nameGu: 'સૂચના પ્રસારણ', href: '/notifications', icon: Send },
  { name: 'Reports', nameGu: 'રિપોર્ટ્સ', href: '/reports', icon: Flag },
  { name: 'Audit Logs', nameGu: 'ઓડિટ લોગ્સ', href: '/audit-logs', icon: History },
  { name: 'Village Settings', nameGu: 'ગામ સેટિંગ્સ', href: '/settings', icon: Settings },
];

export default function Sidebar() {
  const pathname = usePathname();

  return (
    <aside className="w-64 bg-cream-surface border-r border-brand-border min-h-screen flex flex-col shrink-0">
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
      <nav className="flex-1 p-3 space-y-1 overflow-y-auto">
        {navItems.map((item) => {
          const isActive = pathname === item.href;
          const Icon = item.icon;

          return (
            <Link
              key={item.href}
              href={item.href}
              className={`flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-all ${
                isActive
                  ? 'bg-saffron text-white shadow-sm'
                  : 'text-brand-text hover:bg-saffron-container hover:text-saffron'
              }`}
            >
              <Icon className="w-5 h-5 shrink-0" />
              <div className="truncate">
                <span className="block leading-none">{item.name}</span>
                <span className={`text-[11px] block mt-0.5 ${isActive ? 'text-white/80' : 'text-brand-muted'}`}>
                  {item.nameGu}
                </span>
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
