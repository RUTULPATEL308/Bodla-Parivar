'use client';

import { useMemo, useState } from 'react';
import Link from 'next/link';
import {
  AlertCircle,
  Bell,
  BookOpen,
  ClipboardList,
  Megaphone,
  PhoneCall,
  Settings,
  ShieldCheck,
  Sparkles,
  Users,
  WalletCards,
} from 'lucide-react';
import { useAdminLanguage } from '@/components/AdminLanguageContext';
import { useAdminFeatures } from '@/components/AdminLanguageContext';

const adminActions = [
  { title: 'Members', titleGu: 'સભ્યો', description: 'Manage community accounts and assigned roles.', descriptionGu: 'સભ્યોના ખાતા અને ભૂમિકાઓ સંચાલિત કરો.', href: '/users', icon: Users },
  { title: 'Notices', titleGu: 'સૂચનાઓ', description: 'Publish important updates for residents.', descriptionGu: 'રહેવાસીઓ માટે જરૂરી સૂચનાઓ પ્રકાશિત કરો.', href: '/notices', icon: Bell },
  { title: 'Offerings', titleGu: 'ચઢાવો', description: 'Review offerings and approval requests.', descriptionGu: 'ચઢાવા અને મંજૂરીની વિનંતીઓ તપાસો.', href: '/offerings', icon: WalletCards },
  { title: 'Navratri', titleGu: 'નવરાત્રી', description: 'Manage bookings and event reports.', descriptionGu: 'બુકિંગ અને કાર્યક્રમના અહેવાલો સંચાલિત કરો.', href: '/navratri', icon: Sparkles },
  { title: 'Notifications', titleGu: 'સૂચના પ્રસારણ', description: 'Send an update to community members.', descriptionGu: 'સમુદાયના સભ્યોને અપડેટ મોકલો.', href: '/notifications', icon: Megaphone },
  { title: 'Complaints', titleGu: 'ફરિયાદો', description: 'Review and resolve resident requests.', descriptionGu: 'રહેવાસીઓની ફરિયાદો તપાસો અને ઉકેલો.', href: '/complaints', icon: AlertCircle },
  { title: 'Emergency contacts', titleGu: 'તાત્કાલિક સંપર્ક', description: 'Keep trusted local help numbers current.', descriptionGu: 'સ્થાનિક મદદના વિશ્વસનીય નંબર અપડેટ રાખો.', href: '/emergency', icon: PhoneCall },
  { title: 'Village history', titleGu: 'ગામનો ઇતિહાસ', description: 'Care for Bodla’s stories and records.', descriptionGu: 'બોદલાની વાર્તાઓ અને દસ્તાવેજો સાચવો.', href: '/history', icon: BookOpen },
  { title: 'Roles & permissions', titleGu: 'ભૂમિકાઓ અને પરવાનગીઓ', description: 'Review what each staff role can access.', descriptionGu: 'દરેક સ્ટાફ ભૂમિકા કઈ સુવિધા વાપરી શકે તે જુઓ.', href: '/roles', icon: ShieldCheck },
  { title: 'Audit logs', titleGu: 'ઑડિટ લૉગ', description: 'See recent administration activity.', descriptionGu: 'તાજેતરની વહીવટી પ્રવૃત્તિ જુઓ.', href: '/audit-logs', icon: ClipboardList },
  { title: 'Village settings', titleGu: 'ગામની સેટિંગ્સ', description: 'Update village information and contacts.', descriptionGu: 'ગામની માહિતી અને સંપર્કો અપડેટ કરો.', href: '/settings', icon: Settings },
];

export default function AdminDashboard() {
  const { lang } = useAdminLanguage();
  const { features, isFullStaff } = useAdminFeatures();
  const [query, setQuery] = useState('');
  const isGujarati = lang === 'gu';
  const visibleActions = useMemo(() => {
    const normalizedQuery = query.trim().toLocaleLowerCase();
    const featureByPath: Record<string, string> = {
      '/users': 'members', '/notices': 'notices', '/offerings': 'offerings', '/navratri': 'navratri',
      '/notifications': 'notifications', '/complaints': 'complaints', '/emergency': 'emergency_contacts', '/history': 'village_history',
    };
    const allowed = adminActions.filter((action) => action.href === '/' || isFullStaff || features.includes(featureByPath[action.href] as never));
    if (!normalizedQuery) return allowed;
    return allowed.filter((action) =>
      `${action.title} ${action.titleGu} ${action.description} ${action.descriptionGu}`
        .toLocaleLowerCase()
        .includes(normalizedQuery),
    );
  }, [query, features, isFullStaff]);

  return (
    <div className="mx-auto max-w-7xl space-y-6 sm:space-y-8">
      <section className="rounded-3xl border border-brand-border bg-cream-surface p-5 shadow-sm sm:p-8">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <span className="inline-flex rounded-full bg-saffron-container px-3 py-1 text-xs font-bold text-saffron">
              {isGujarati ? 'બોદલા પરિવાર • વહીવટ' : 'Bodla Parivar • Administration'}
            </span>
            <h1 className="mt-4 font-gujarati text-2xl font-bold text-brand-text sm:text-3xl">
              {isGujarati ? 'નમસ્કાર, સ્વાગત છે' : 'Welcome to your dashboard'}
            </h1>
            <p className="mt-2 max-w-2xl text-base text-brand-muted">
              {isGujarati ? 'ગામના તમામ વહીવટી કામ એક જ જગ્યાએથી સંચાલિત કરો.' : 'Manage village services and community updates from one place.'}
            </p>
          </div>
          <div className="rounded-2xl border border-brand-border bg-white/70 px-4 py-3 text-sm">
            <p className="text-xs font-medium text-brand-muted">{isGujarati ? 'તમારી ભૂમિકા' : 'Your workspace'}</p>
            <p className="mt-1 font-semibold text-saffron">{isFullStaff ? (isGujarati ? 'અધિકૃત સ્ટાફ' : 'Authorized staff') : (isGujarati ? 'આપેલ સુવિધા ઍક્સેસ' : 'Assigned feature access')}</p>
          </div>
        </div>
        <label className="mt-6 block">
          <span className="sr-only">{isGujarati ? 'સુવિધાઓ શોધો' : 'Search dashboard features'}</span>
          <input
            type="search"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder={isGujarati ? 'સુવિધા શોધો…' : 'Search a feature…'}
            className="w-full rounded-2xl border border-brand-border bg-white px-4 py-3 text-base outline-none transition placeholder:text-brand-muted/80 focus:border-saffron"
          />
        </label>
      </section>

      <section aria-labelledby="dashboard-features-title">
        <div className="mb-3 flex items-end justify-between gap-4 px-1">
          <div>
            <h2 id="dashboard-features-title" className="font-gujarati text-xl font-bold text-brand-text">
              {isGujarati ? 'ઝડપી સુવિધાઓ' : 'Quick services'}
            </h2>
            <p className="mt-1 text-sm text-brand-muted">
              {isGujarati ? 'તમારી ભૂમિકા પ્રમાણે પરવાનગીવાળી સુવિધાઓ ખોલો.' : 'Open the tools available to your staff role.'}
            </p>
          </div>
          <span className="hidden rounded-full bg-white px-3 py-1 text-sm text-brand-muted sm:inline-flex">
            {visibleActions.length} {isGujarati ? 'સુવિધાઓ' : 'features'}
          </span>
        </div>

        {visibleActions.length > 0 ? (
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 sm:gap-4 xl:grid-cols-4">
            {visibleActions.map(({ title, titleGu, description, descriptionGu, href, icon: Icon }) => (
              <Link
                key={href}
                href={href}
                className="group flex min-h-40 flex-col rounded-2xl border border-brand-border bg-cream-surface p-4 shadow-sm transition hover:-translate-y-0.5 hover:border-saffron hover:shadow-md focus-visible:outline-offset-4 sm:min-h-44 sm:p-5"
              >
                <span className="mb-3 inline-flex h-12 w-12 items-center justify-center rounded-2xl bg-saffron-container text-saffron transition group-hover:bg-saffron group-hover:text-white">
                  <Icon aria-hidden="true" className="h-6 w-6" />
                </span>
                <h3 className="font-semibold text-brand-text">{isGujarati ? titleGu : title}</h3>
                <p className="mt-1 text-sm leading-5 text-brand-muted">{isGujarati ? descriptionGu : description}</p>
              </Link>
            ))}
          </div>
        ) : (
          <p role="status" className="rounded-2xl border border-brand-border bg-cream-surface p-6 text-center text-brand-muted">
            {isGujarati ? 'આ શોધ માટે કોઈ સુવિધા મળી નથી.' : 'No dashboard features match your search.'}
          </p>
        )}
      </section>

      <section className="flex flex-col gap-4 rounded-2xl border border-brand-border bg-cream-surface p-5 sm:flex-row sm:items-center sm:justify-between sm:p-6">
        <div>
          <h2 className="font-semibold text-brand-text">{isGujarati ? 'રહેવાસી એપ્લિકેશન જુઓ' : 'See the resident experience'}</h2>
          <p className="mt-1 text-sm text-brand-muted">{isGujarati ? 'રહેવાસીઓને દેખાતી સૂચનાઓ અને સેવાઓ તપાસો.' : 'Review the notices and services residents can access.'}</p>
        </div>
          <Link href={isFullStaff || features.includes('notices') ? '/notices' : '/'} className="inline-flex min-h-11 items-center justify-center rounded-xl bg-saffron px-4 py-2 text-sm font-semibold text-white transition hover:bg-saffron-dark">
          {isGujarati ? 'સૂચનાઓ ખોલો' : 'Open notices'}
        </Link>
      </section>
    </div>
  );
}
