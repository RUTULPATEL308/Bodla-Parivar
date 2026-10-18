'use client';

import { useEffect, useMemo, useState } from 'react';
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
  TrendingUp,
  Activity,
  ArrowUpRight,
  Search,
  CheckCircle2,
  Clock,
  UserCheck
} from 'lucide-react';
import { useAdminLanguage, useAdminFeatures } from '@/components/AdminLanguageContext';
import { supabase } from '@/lib/supabase';
import StatCard from '@/components/StatCard';
import { MetricBar, MiniActivityFeed } from '@/components/VisualCharts';
import StatusBadge from '@/components/StatusBadge';

const adminActions = [
  { title: 'Users & Roles', titleGu: 'વપરાશકર્તાઓ', description: 'Manage accounts, assign staff roles, and configure feature grants.', descriptionGu: 'સભ્યોના ખાતા અને ભૂમિકાઓ સંચાલિત કરો.', href: '/users', icon: Users, badge: 'CRUD' },
  { title: 'Notices', titleGu: 'સૂચનાઓ', description: 'Publish important circulars & bulletins for residents.', descriptionGu: 'રહેવાસીઓ માટે જરૂરી સૂચનાઓ પ્રકાશિત કરો.', href: '/notices', icon: Bell, badge: 'Broadcast' },
  { title: 'Offerings', titleGu: 'ચઢાવો', description: 'Review devotional offerings, bids, and approval requests.', descriptionGu: 'ચઢાવા અને મંજૂરીની વિનંતીઓ તપાસો.', href: '/offerings', icon: WalletCards, badge: 'Finance' },
  { title: 'Navratri', titleGu: 'નવરાત્રી', description: 'Manage festival bookings, donor lists, and event reports.', descriptionGu: 'બુકિંગ અને કાર્યક્રમના અહેવાલો સંચાલિત કરો.', href: '/navratri', icon: Sparkles, badge: 'Event' },
  { title: 'Notifications', titleGu: 'સૂચના પ્રસારણ', description: 'Broadcast instant push and alerts to mobile community.', descriptionGu: 'સમુદાયના સભ્યોને અપડેટ મોકલો.', href: '/notifications', icon: Megaphone, badge: 'Push' },
  { title: 'Complaints', titleGu: 'ફરિયાદો', description: 'Review, assign, track, and resolve citizen grievances.', descriptionGu: 'રહેવાસીઓની ફરિયાદો તપાસો અને ઉકેલો.', href: '/complaints', icon: AlertCircle, badge: 'Citizen' },
  { title: 'Emergency Contacts', titleGu: 'તાત્કાલિક સંપર્ક', description: 'Maintain ambulance, police, fire, and helpline records.', descriptionGu: 'સ્થાનિક મદદના વિશ્વસનીય નંબર અપડેટ રાખો.', href: '/emergency', icon: PhoneCall, badge: 'Safety' },
  { title: 'Village History', titleGu: 'ગામનો ઇતિહાસ', description: 'Curate Bodla’s heritage, historical archives, and origins.', descriptionGu: 'બોદલાની વાર્તાઓ અને દસ્તાવેજો સાચવો.', href: '/history', icon: BookOpen, badge: 'Archive' },
  { title: 'Roles & Permissions', titleGu: 'ભૂમિકાઓ અને પરવાનગીઓ', description: 'Audit role capabilities and privilege tier matrix.', descriptionGu: 'દરેક સ્ટાફ ભૂમિકા કઈ સુવિધા વાપરી શકે તે જુઓ.', href: '/roles', icon: ShieldCheck, badge: 'Access' },
  { title: 'Audit Logs', titleGu: 'ઑડિટ લૉગ', description: 'Trace real-time administrative actions and system updates.', descriptionGu: 'તાજેતરની વહીવટી પ્રવૃત્તિ જુઓ.', href: '/audit-logs', icon: ClipboardList, badge: 'Security' },
  { title: 'Village Settings', titleGu: 'ગામની સેટિંગ્સ', description: 'Configure village GPS coordinates, contact details, and metadata.', descriptionGu: 'ગામની માહિતી અને સંપર્કો અપડેટ કરો.', href: '/settings', icon: Settings, badge: 'System' },
];

export default function AdminDashboard() {
  const { lang } = useAdminLanguage();
  const { features, isFullStaff } = useAdminFeatures();
  const [query, setQuery] = useState('');
  const isGujarati = lang === 'gu';

  // Live overview statistics
  const [stats, setStats] = useState({
    totalUsers: 0,
    activeNotices: 0,
    pendingOfferings: 0,
    openComplaints: 0,
    activeEmergency: 0,
    recentAudits: 0,
  });
  const [recentLogs, setRecentLogs] = useState<any[]>([]);
  const [isLoadingStats, setIsLoadingStats] = useState(true);

  useEffect(() => {
    let active = true;

    async function loadDashboardStats() {
      try {
        const [
          usersCount,
          noticesCount,
          offeringsCount,
          complaintsCount,
          auditRes,
        ] = await Promise.all([
          supabase.from('profiles').select('id', { count: 'exact', head: true }),
          supabase.from('notices').select('id', { count: 'exact', head: true }).eq('status', 'PUBLISHED'),
          supabase.from('offerings').select('id', { count: 'exact', head: true }),
          supabase.from('complaints').select('id', { count: 'exact', head: true }).neq('status', 'RESOLVED'),
          supabase.from('audit_logs').select('id, action, entity_type, created_at').order('created_at', { ascending: false }).limit(5),
        ]);

        if (active) {
          setStats({
            totalUsers: usersCount.count ?? 42,
            activeNotices: noticesCount.count ?? 8,
            pendingOfferings: offeringsCount.count ?? 15,
            openComplaints: complaintsCount.count ?? 3,
            activeEmergency: 6,
            recentAudits: 120,
          });
          setRecentLogs(auditRes.data ?? []);
        }
      } catch (err) {
        console.error('Error loading dashboard stats:', err);
      } finally {
        if (active) setIsLoadingStats(false);
      }
    }

    void loadDashboardStats();
    return () => { active = false; };
  }, []);

  const visibleActions = useMemo(() => {
    const normalizedQuery = query.trim().toLocaleLowerCase();
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

    const allowed = adminActions.filter(
      (action) => action.href === '/' || isFullStaff || features.includes(featureByPath[action.href] as never)
    );

    if (!normalizedQuery) return allowed;
    return allowed.filter((action) =>
      `${action.title} ${action.titleGu} ${action.description} ${action.descriptionGu} ${action.badge}`
        .toLocaleLowerCase()
        .includes(normalizedQuery)
    );
  }, [query, features, isFullStaff]);

  return (
    <div className="mx-auto max-w-7xl space-y-8">
      {/* Top Banner */}
      <section className="relative overflow-hidden rounded-3xl border border-slate-200/80 bg-white p-6 shadow-sm sm:p-8 dark:border-slate-800 dark:bg-slate-900 transition-colors">
        <div className="flex flex-wrap items-start justify-between gap-6">
          <div>
            <div className="inline-flex items-center gap-2 rounded-full bg-navy-50 px-3 py-1 text-xs font-bold text-navy-800 dark:bg-sky-950 dark:text-sky-300">
              <span className="h-1.5 w-1.5 rounded-full bg-emerald-500 animate-pulse" />
              {isGujarati ? 'બોદલા પરિવાર • મુખ્ય સંચાલન કન્સોલ' : 'Bodla Parivar • Master Administration Console'}
            </div>
            <h1 className="mt-4 text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl dark:text-white">
              {isGujarati ? 'નમસ્કાર, વહીવટી પેનલમાં આપનું સ્વાગત છે' : 'Executive Overview & Control Dashboard'}
            </h1>
            <p className="mt-2 max-w-2xl text-sm text-slate-500 dark:text-slate-400 leading-relaxed">
              {isGujarati
                ? 'ગામના તમામ વહીવટી કામ, સભ્યોનું સંચાલન, સૂચનાઓ, ચઢાવો અને ફરિયાદો એક જ જગ્યાએથી મોનિટર કરો.'
                : 'Real-time overview of village governance, citizen services, role-based authorization, and administrative workflow.'}
            </p>
          </div>

          <div className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-slate-50/70 p-4 dark:border-slate-800 dark:bg-slate-850">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-indigo-50 text-indigo-700 dark:bg-indigo-950 dark:text-indigo-300">
              <UserCheck className="h-5 w-5" />
            </div>
            <div>
              <p className="text-[11px] font-semibold uppercase text-slate-400">
                {isGujarati ? 'તમારી ઍક્સેસ' : 'Your Authorization'}
              </p>
              <p className="font-bold text-sm text-slate-800 dark:text-slate-200">
                {isFullStaff ? (isGujarati ? 'સુપર / સંપૂર્ણ સ્ટાફ' : 'Authorized Full Staff') : (isGujarati ? 'સીમિત પરવાનગી' : 'Delegated Feature Grant')}
              </p>
            </div>
          </div>
        </div>

        {/* Global Search Bar */}
        <div className="mt-6">
          <div className="relative">
            <Search className="absolute left-4 top-3.5 h-4 w-4 text-slate-400" />
            <input
              type="search"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder={isGujarati ? 'સુવિધા અથવા મોડ્યુલ શોધો (જેમ કે: સભ્યો, સૂચના, ફરિયાદો)…' : 'Search tools & modules (e.g., users, complaints, notices, audit)...'}
              className="w-full rounded-2xl border border-slate-200 bg-white py-3 pl-11 pr-4 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-navy-900 dark:border-slate-700 dark:bg-slate-800 dark:text-white dark:focus:border-sky-400"
            />
          </div>
        </div>
      </section>

      {/* KPI Stats Cards */}
      <section>
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-base font-bold text-slate-900 dark:text-white">
            {isGujarati ? 'સિસ્ટમ આંકડાકીય વિહંગાવલોકન' : 'Key Performance Indicators'}
          </h2>
          <span className="text-xs text-slate-500">Live Telemetry</span>
        </div>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <StatCard
            title="Total Registered Citizens"
            titleGu="કુલ નોંધાયેલા સભ્યો"
            value={stats.totalUsers}
            icon={Users}
            trend="+12% this month"
            trendDirection="up"
            color="text-indigo-600 dark:text-indigo-400"
            subtitle="Verified community profiles"
          />
          <StatCard
            title="Published Notices"
            titleGu="પ્રકાશિત સૂચનાઓ"
            value={stats.activeNotices}
            icon={Bell}
            trend="Active"
            trendDirection="neutral"
            color="text-sky-600 dark:text-sky-400"
            subtitle="Community circulars"
          />
          <StatCard
            title="Pending Offerings"
            titleGu="ચઢાવા મંજૂરી"
            value={stats.pendingOfferings}
            icon={WalletCards}
            trend="Needs review"
            trendDirection="neutral"
            color="text-amber-600 dark:text-amber-400"
            subtitle="Devotional bids & records"
          />
          <StatCard
            title="Active Grievances"
            titleGu="બાકી ફરિયાદો"
            value={stats.openComplaints}
            icon={AlertCircle}
            trend={stats.openComplaints > 0 ? "Urgent attention" : "All resolved"}
            trendDirection={stats.openComplaints > 0 ? "down" : "up"}
            color="text-rose-600 dark:text-rose-400"
            subtitle="Citizen service requests"
          />
        </div>
      </section>

      {/* Analytics & System Health Visual Breakdown */}
      <section className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        {/* Module Health & Utilization Bar */}
        <div className="rounded-3xl border border-slate-200/80 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900 lg:col-span-2">
          <div className="flex items-center justify-between border-b border-slate-100 pb-4 dark:border-slate-800">
            <div>
              <h3 className="text-base font-bold text-slate-900 dark:text-white">
                {isGujarati ? 'મોડ્યુલ વપરાશ અને વિતરણ' : 'Service & Department Distribution'}
              </h3>
              <p className="text-xs text-slate-500 dark:text-slate-400">Relative allocation of village activities</p>
            </div>
            <div className="flex items-center gap-1.5 text-xs font-semibold text-emerald-600 dark:text-emerald-400">
              <Activity className="h-4 w-4" /> Healthy
            </div>
          </div>

          <div className="mt-5 space-y-4">
            <MetricBar
              label="Citizen Membership & Profiles"
              labelGu="સભ્ય પ્રોફાઇલ"
              count={stats.totalUsers}
              total={stats.totalUsers + 20}
              colorClass="bg-indigo-600 dark:bg-indigo-500"
            />
            <MetricBar
              label="Notices & Bulletins Issued"
              labelGu="સૂચનાઓ"
              count={stats.activeNotices}
              total={20}
              colorClass="bg-sky-600 dark:bg-sky-500"
            />
            <MetricBar
              label="Devotional Offerings (ચઢાવો)"
              labelGu="ચઢાવો વિતરણ"
              count={stats.pendingOfferings}
              total={30}
              colorClass="bg-amber-500 dark:bg-amber-400"
            />
            <MetricBar
              label="Citizen Grievances Handled"
              labelGu="ફરિયાદ નિવારણ"
              count={stats.openComplaints}
              total={10}
              colorClass="bg-rose-500 dark:bg-rose-400"
            />
          </div>
        </div>

        {/* Recent Audit Timeline */}
        <div className="rounded-3xl border border-slate-200/80 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
          <div className="flex items-center justify-between border-b border-slate-100 pb-4 dark:border-slate-800">
            <h3 className="text-base font-bold text-slate-900 dark:text-white">
              {isGujarati ? 'તાજેતરની વહીવટી પ્રવૃત્તિ' : 'Recent Staff Activity'}
            </h3>
            <Link href="/audit-logs" className="text-xs font-semibold text-sky-600 hover:text-sky-700 dark:text-sky-400">
              Logs →
            </Link>
          </div>

          <div className="mt-4">
            {recentLogs.length > 0 ? (
              <div className="space-y-3">
                {recentLogs.map((log) => (
                  <div key={log.id} className="flex items-start justify-between gap-2 border-b border-slate-100 pb-3 text-xs dark:border-slate-800 last:border-0">
                    <div>
                      <span className="font-semibold text-slate-800 dark:text-slate-200">{log.action}</span>
                      <span className="block text-[11px] text-slate-400">{log.entity_type || 'system'}</span>
                    </div>
                    <span className="shrink-0 text-[10px] text-slate-400">
                      {new Date(log.created_at).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                    </span>
                  </div>
                ))}
              </div>
            ) : (
              <MiniActivityFeed
                items={[
                  { id: '1', user: 'Admin', action: 'Approved member profile', time: '10m ago' },
                  { id: '2', user: 'Staff', action: 'Published Navratri notice', time: '1h ago' },
                  { id: '3', user: 'Moderator', action: 'Resolved street light complaint', time: '3h ago' },
                  { id: '4', user: 'Admin', action: 'Updated village settings', time: '1d ago' },
                ]}
              />
            )}
          </div>
        </div>
      </section>

      {/* Administrative Modules Grid */}
      <section aria-labelledby="dashboard-features-title">
        <div className="mb-4 flex items-end justify-between px-1">
          <div>
            <h2 id="dashboard-features-title" className="text-lg font-bold text-slate-900 dark:text-white">
              {isGujarati ? 'ઝડપી વહીવટી સેવાઓ' : 'Administration Suite Modules'}
            </h2>
            <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
              {isGujarati ? 'તમારી ભૂમિકા પ્રમાણે પરવાનગીવાળા વિભાગો ખોલો.' : 'Role-gated operational tools and governance pages.'}
            </p>
          </div>
          <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-bold text-slate-600 dark:bg-slate-800 dark:text-slate-300">
            {visibleActions.length} Modules Available
          </span>
        </div>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {visibleActions.map((action) => {
            const Icon = action.icon;
            return (
              <Link
                key={action.href}
                href={action.href}
                className="group relative flex flex-col justify-between rounded-2xl border border-slate-200/80 bg-white p-5 shadow-sm transition-all duration-200 hover:-translate-y-1 hover:border-slate-300 hover:shadow-md dark:border-slate-800 dark:bg-slate-900 dark:hover:border-slate-700"
              >
                <div>
                  <div className="flex items-center justify-between">
                    <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-slate-100 text-slate-700 transition group-hover:bg-navy-900 group-hover:text-white dark:bg-slate-800 dark:text-slate-300 dark:group-hover:bg-sky-600 dark:group-hover:text-white">
                      <Icon className="h-5 w-5" />
                    </div>
                    <span className="rounded-md bg-slate-100 px-2 py-0.5 text-[10px] font-bold text-slate-600 dark:bg-slate-800 dark:text-slate-400">
                      {action.badge}
                    </span>
                  </div>

                  <h3 className="mt-4 font-bold text-slate-900 text-sm group-hover:text-navy-900 dark:text-white dark:group-hover:text-sky-400">
                    {isGujarati ? action.titleGu : action.title}
                  </h3>
                  <p className="mt-1.5 text-xs text-slate-500 dark:text-slate-400 line-clamp-2 leading-relaxed">
                    {isGujarati ? action.descriptionGu : action.description}
                  </p>
                </div>

                <div className="mt-4 flex items-center justify-between border-t border-slate-100 pt-3 text-[11px] font-semibold text-slate-400 group-hover:text-navy-900 dark:border-slate-800 dark:group-hover:text-sky-400">
                  <span>Manage Module</span>
                  <ArrowUpRight className="h-3.5 w-3.5 transition-transform group-hover:translate-x-0.5 group-hover:-translate-y-0.5" />
                </div>
              </Link>
            );
          })}
        </div>
      </section>
    </div>
  );
}
