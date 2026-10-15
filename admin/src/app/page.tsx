'use client';

import Link from 'next/link';
import {
  Bell,
  Megaphone,
  Sparkles,
  Users,
  WalletCards,
  AlertCircle,
  BookOpen,
  ClipboardList,
  PhoneCall,
  Settings,
  ShieldCheck,
} from 'lucide-react';

const adminActions = [
  { title: 'Members', titleGu: 'સભ્યો', description: 'Manage accounts and staff roles.', href: '/users', icon: Users },
  { title: 'Notices', titleGu: 'સૂચનાઓ', description: 'Create, publish, and remove notices.', href: '/notices', icon: Bell },
  { title: 'Offerings', titleGu: 'ચઢાવો', description: 'Review offerings, bids, and winners.', href: '/offerings', icon: WalletCards },
  { title: 'Navratri', titleGu: 'નવરાત્રી', description: 'Manage bookings and the Aheval report.', href: '/navratri', icon: Sparkles },
  { title: 'Notifications', titleGu: 'સૂચનાઓ મોકલો', description: 'Send an update to community members.', href: '/notifications', icon: Megaphone },
  { title: 'Complaints', titleGu: 'Complaints', description: 'Review and resolve resident reports.', href: '/complaints', icon: AlertCircle },
  { title: 'Emergency contacts', titleGu: 'Emergency contacts', description: 'Maintain trusted local help numbers.', href: '/emergency', icon: PhoneCall },
  { title: 'Village history', titleGu: 'Village history', description: 'Manage community history and records.', href: '/history', icon: BookOpen },
  { title: 'Roles & permissions', titleGu: 'Roles & permissions', description: 'Review staff and resident access roles.', href: '/roles', icon: ShieldCheck },
  { title: 'Audit logs', titleGu: 'Audit logs', description: 'Review recorded administration activity.', href: '/audit-logs', icon: ClipboardList },
  { title: 'Village settings', titleGu: 'Village settings', description: 'Update village details and contacts.', href: '/settings', icon: Settings },
];

export default function AdminDashboard() {
  return (
    <div className="mx-auto max-w-7xl space-y-6 sm:space-y-8">
      <header className="rounded-2xl border border-brand-border bg-cream-surface p-6 shadow-sm">
        <p className="mb-2 inline-block rounded-full bg-saffron-container px-2.5 py-0.5 text-xs font-bold text-saffron">
          બોદલા ગ્રામ પંચાયત અને સમુદાય
        </p>
        <h1 className="font-gujarati text-2xl font-bold text-brand-text">બોદલા પરિવાર એડમિન ડેશબોર્ડ</h1>
        <p className="mt-1 text-sm text-brand-muted">Choose an admin area to manage community content and requests.</p>
      </header>

      <section aria-label="Admin features" className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-3">
        {adminActions.map(({ title, titleGu, description, href, icon: Icon }) => (
          <Link key={href} href={href} className="group min-h-36 rounded-2xl border border-brand-border bg-cream-surface p-5 shadow-sm transition hover:border-saffron hover:shadow-md focus-visible:outline-offset-4 sm:p-6">
            <div className="flex items-center gap-3">
              <span className="rounded-xl bg-saffron-container p-3 text-saffron"><Icon aria-hidden="true" className="h-6 w-6" /></span>
              <div>
                <h2 className="text-lg font-semibold text-brand-text">{title}</h2>
                <p className="font-gujarati text-sm text-brand-muted">{titleGu}</p>
              </div>
            </div>
            <p className="mt-4 text-base text-brand-muted">{description}</p>
          </Link>
        ))}
      </section>
    </div>
  );
}
