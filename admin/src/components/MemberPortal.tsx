'use client';

import React, { useEffect, useState, FormEvent } from 'react';
import Image from 'next/image';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import {
  User,
  Bell,
  Calendar,
  HeartHandshake,
  MessageSquareWarning,
  Settings,
  History,
  PhoneCall,
  Clock,
  Sparkles,
  LogOut,
  Sun,
  Moon,
  Search,
  Send,
  Eye,
  Edit3,
  Save,
  Check,
  LayoutDashboard,
  ChevronRight,
  Globe,
} from 'lucide-react';
import { supabase } from '@/lib/supabase';
import type { AdminFeature } from '@/components/AdminLanguageContext';
import { useAdminLanguage } from '@/components/AdminLanguageContext';
import { useTheme } from '@/components/ThemeContext';
import StatusBadge from '@/components/StatusBadge';
import EmptyState from '@/components/EmptyState';
import LoadingState from '@/components/LoadingState';
import ConfirmationModal from '@/components/ConfirmationModal';
import MemberFeatureViews, { type MemberSection } from '@/components/MemberFeatureViews';

interface UserPortalProps {
  email: string;
  fullName: string;
  onSignOut: () => void;
  features: AdminFeature[];
}

interface NoticeItem {
  id: string;
  title_gu: string;
  title_en: string;
  description_gu?: string;
  description_en?: string;
  image_url?: string | null;
  attachment_url?: string | null;
  created_at: string;
  is_pinned?: boolean;
}

interface EventItem {
  id: string;
  title_gu: string;
  title_en: string;
  start_at: string;
  location_en: string | null;
  location_gu: string | null;
}

interface UserComplaint {
  id: string;
  title: string;
  description: string;
  status: string;
  priority: string;
  created_at: string;
  resolution_note?: string;
}

type TabKey = MemberSection;

const featureTools: { feature: AdminFeature; title: string; description: string; href?: string; tab?: TabKey; icon: React.ElementType }[] = [
  { feature: 'members', title: 'Members', description: 'View community members available to your account.', href: '/users', icon: User },
  { feature: 'notices', title: 'Notice management', description: 'Create and manage community notices.', href: '/notices', icon: Bell },
  { feature: 'offerings', title: 'Offerings', description: 'Browse community offerings and place bids.', tab: 'offerings', icon: HeartHandshake },
  { feature: 'navratri', title: 'Navratri', description: 'Manage festival bookings and options.', href: '/navratri', icon: Sparkles },
  { feature: 'events', title: 'Village events', description: 'Browse upcoming community events.', tab: 'events', icon: Calendar },
  { feature: 'complaints', title: 'Complaints', description: 'Review and follow up on complaints.', href: '/complaints', icon: MessageSquareWarning },
  { feature: 'emergency_contacts', title: 'Emergency contacts', description: 'Maintain local emergency information.', href: '/emergency', icon: PhoneCall },
  { feature: 'village_history', title: 'Village history', description: 'Update community history and records.', href: '/history', icon: History },
  { feature: 'notifications', title: 'Notifications', description: 'Send updates to the community.', href: '/notifications', icon: Send },
];

export default function MemberPortal({ email, fullName, onSignOut, features }: UserPortalProps) {
  const pathname = usePathname();
  const { lang, toggleLanguage } = useAdminLanguage();
  const { theme, toggleTheme } = useTheme();
  const sectionByPath: Record<string, TabKey> = {
    '/offerings': 'offerings', '/notices': 'notices', '/events': 'events',
    '/complaints': 'complaints', '/emergency': 'emergency', '/history': 'history',
    '/navratri': 'navratri', '/notifications': 'notifications', '/users': 'directory',
  };
  const [selectedTab, setSelectedTab] = useState<TabKey>('overview');
  const activeTab = sectionByPath[pathname] ?? selectedTab;
  const setActiveTab = setSelectedTab;
  const [searchQuery, setSearchQuery] = useState('');
  const assignedTools = featureTools.filter((tool) => features.includes(tool.feature));
  const dashboardServices: { title: string; subtitle: string; icon: React.ElementType; action: () => void; keywords: string }[] = [
    { title: 'નવરાત્રી', subtitle: 'Navratri services', icon: Sparkles, action: () => setActiveTab('navratri'), keywords: 'navratri festival' },
    { title: 'સૂચનાઓ', subtitle: 'Community notices', icon: Bell, action: () => setActiveTab('notices'), keywords: 'notice announcement update' },
    { title: 'ચઢાવો', subtitle: 'Community offerings', icon: HeartHandshake, action: () => setActiveTab('offerings'), keywords: 'offering donation bid' },
    { title: 'તાત્કાલિક સંપર્ક', subtitle: 'Emergency contacts', icon: PhoneCall, action: () => setActiveTab('emergency'), keywords: 'emergency help phone' },
    { title: 'ગેલેરી', subtitle: 'Village photos and videos', icon: Eye, action: () => setActiveTab('gallery'), keywords: 'gallery photos video' },
    { title: 'ફરિયાદો', subtitle: 'Send a request or follow up', icon: MessageSquareWarning, action: () => setActiveTab('complaints'), keywords: 'complaint grievance request' },
  ].filter((service) => `${service.title} ${service.subtitle} ${service.keywords}`.toLowerCase().includes(searchQuery.toLowerCase()));

  // Data states
  const [notices, setNotices] = useState<NoticeItem[]>([]);
  const [events, setEvents] = useState<EventItem[]>([]);
  const [complaints, setComplaints] = useState<UserComplaint[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // Profile edit state
  const [profileName, setProfileName] = useState(fullName);
  const [phone, setPhone] = useState('');
  const [isUpdatingProfile, setIsUpdatingProfile] = useState(false);
  const [profileMessage, setProfileMessage] = useState<string | null>(null);

  // New complaint form state
  const [complaintTitle, setComplaintTitle] = useState('');
  const [complaintDesc, setComplaintDesc] = useState('');
  const [complaintPriority, setComplaintPriority] = useState<'LOW' | 'NORMAL' | 'HIGH' | 'URGENT'>('NORMAL');
  const [isSubmittingComplaint, setIsSubmittingComplaint] = useState(false);
  const [complaintSuccess, setComplaintSuccess] = useState(false);

  // Sign out modal
  const [isSignOutModalOpen, setIsSignOutModalOpen] = useState(false);

  // Load user data
  useEffect(() => {
    let active = true;

    async function loadUserData() {
      setIsLoading(true);
      try {
        const [noticeRes, eventRes, profileRes] = await Promise.all([
          supabase
            .from('notices')
            .select('id, title_gu, title_en, description_gu, description_en, image_url, attachment_url, created_at, is_pinned')
            .eq('status', 'PUBLISHED')
            .order('is_pinned', { ascending: false })
            .order('created_at', { ascending: false })
            .limit(10),
          supabase
            .from('events')
            .select('id, title_gu, title_en, start_at, location_en, location_gu')
            .eq('status', 'PUBLISHED')
            .order('start_at', { ascending: true })
            .limit(8),
          supabase
            .from('profiles')
            .select('full_name, phone')
            .maybeSingle(),
        ]);

        if (active) {
          setNotices((noticeRes.data ?? []) as NoticeItem[]);
          setEvents((eventRes.data ?? []) as EventItem[]);
          if (profileRes.data) {
            if (profileRes.data.full_name) setProfileName(profileRes.data.full_name);
            if (profileRes.data.phone) setPhone(profileRes.data.phone);
          }
        }

        // Fetch citizen grievances / complaints
        const { data: compData } = await supabase
          .from('complaints')
          .select('id, title, description, status, priority, created_at, resolution_note')
          .order('created_at', { ascending: false })
          .limit(15);

        if (active && compData) {
          setComplaints(compData as UserComplaint[]);
        }
      } catch (err) {
        console.error('Error loading resident portal data:', err);
      } finally {
        if (active) setIsLoading(false);
      }
    }

    void loadUserData();
    return () => { active = false; };
  }, []);

  const handleUpdateProfile = async (e: FormEvent) => {
    e.preventDefault();
    setIsUpdatingProfile(true);
    setProfileMessage(null);

    try {
      const { data: { user } } = await supabase.auth.getUser();
      if (!user) throw new Error('Not authenticated');

      const { error } = await supabase
        .from('profiles')
        .update({
          full_name: profileName.trim(),
          phone: phone.trim() || null,
        })
        .eq('auth_user_id', user.id);

      if (error) throw error;
      setProfileMessage('Profile updated successfully!');
      setTimeout(() => setProfileMessage(null), 3500);
    } catch (err) {
      setProfileMessage(err instanceof Error ? err.message : 'Failed to update profile');
    } finally {
      setIsUpdatingProfile(false);
    }
  };

  const handleCreateComplaint = async (e: FormEvent) => {
    e.preventDefault();
    if (!complaintTitle.trim() || !complaintDesc.trim()) return;

    setIsSubmittingComplaint(true);
    try {
      const { data: { user } } = await supabase.auth.getUser();
      const { data, error } = await supabase
        .from('complaints')
        .insert({
          user_id: user?.id,
          title: complaintTitle.trim(),
          description: complaintDesc.trim(),
          priority: complaintPriority,
          status: 'SUBMITTED',
        })
        .select()
        .single();

      if (error) throw error;
      if (data) {
        setComplaints([data as UserComplaint, ...complaints]);
      }
      setComplaintTitle('');
      setComplaintDesc('');
      setComplaintPriority('NORMAL');
      setComplaintSuccess(true);
      setTimeout(() => setComplaintSuccess(false), 4000);
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Failed to submit grievance');
    } finally {
      setIsSubmittingComplaint(false);
    }
  };

  // Filtered notices
  const filteredNotices = notices.filter(n =>
    [n.title_en, n.title_gu, n.description_en ?? '', n.description_gu ?? '']
      .join(' ')
      .toLowerCase()
      .includes(searchQuery.toLowerCase())
  );

  return (
    <div className="flex min-h-screen w-full min-w-0 flex-1 bg-slate-50 text-slate-900 transition-colors duration-300 dark:bg-slate-950 dark:text-slate-100">
      <aside className="hidden w-72 shrink-0 border-r border-slate-200/80 bg-white shadow-sm dark:border-slate-800/80 dark:bg-slate-900 md:flex md:flex-col">
        <div className="border-b border-slate-100 bg-gradient-to-b from-slate-50 to-white p-5 dark:border-slate-800 dark:from-slate-850 dark:to-slate-900">
          <Image src="/bodla_parivar_logo.png" alt="Bodla Parivar" width={1069} height={258} className="h-auto w-full object-contain dark:brightness-110" priority />
          <div className="mt-3 flex justify-center"><span className="inline-flex items-center gap-1.5 rounded-full bg-navy-50 px-2.5 py-1 text-[11px] font-bold tracking-wide text-navy-800 dark:bg-sky-950/80 dark:text-sky-300"><span className="h-1.5 w-1.5 animate-pulse rounded-full bg-amber-500" /> COMMUNITY PORTAL</span></div>
        </div>
        <nav aria-label="Member portal" className="flex-1 space-y-1 overflow-y-auto p-3">
          {[
            { key: 'overview', label: 'Dashboard', icon: LayoutDashboard },
            { key: 'directory', label: 'Community Members', icon: User },
            { key: 'notices', label: 'Notices', icon: Bell },
            { key: 'events', label: 'Village Events', icon: Calendar },
            { key: 'offerings', label: 'Offerings', icon: HeartHandshake },
            { key: 'complaints', label: 'My Requests', icon: MessageSquareWarning },
            { key: 'profile', label: 'My Profile', icon: Settings },
          ].map(({ key, label, icon: Icon }) => {
            const active = activeTab === key;
            return <button key={key} type="button" onClick={() => setActiveTab(key as TabKey)} aria-current={active ? 'page' : undefined} className={`group flex w-full items-center justify-between rounded-xl px-3.5 py-2.5 text-left text-xs font-semibold transition ${active ? 'bg-navy-900 text-white shadow-sm dark:bg-sky-600' : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900 dark:text-slate-400 dark:hover:bg-slate-800 dark:hover:text-white'}`}><span className="flex items-center gap-3"><Icon className={`h-4 w-4 ${active ? 'text-white' : 'text-slate-500 group-hover:text-navy-900 dark:text-slate-400 dark:group-hover:text-sky-400'}`} />{label}</span><ChevronRight className={`h-3.5 w-3.5 ${active ? 'opacity-100' : 'opacity-0 group-hover:opacity-60'}`} /></button>;
          })}
        </nav>
        <div className="border-t border-slate-100 bg-slate-50/70 p-4 dark:border-slate-800 dark:bg-slate-850"><div className="flex items-center justify-between text-[11px]"><span className="font-medium text-slate-500 dark:text-slate-400">Community</span><span className="inline-flex items-center gap-1 font-semibold text-emerald-600 dark:text-emerald-400"><span className="h-1.5 w-1.5 rounded-full bg-emerald-500" />Connected</span></div><p className="mt-1 text-[10px] text-slate-400 dark:text-slate-500">Bodla, Mehsana · Gujarat</p></div>
      </aside>
      <div className="flex min-h-screen min-w-0 flex-1 flex-col">
      {/* Top Header */}
      <header className="sticky top-0 z-30 flex min-h-16 items-center justify-between gap-4 border-b border-slate-200/80 bg-white/95 px-4 py-2.5 shadow-[0_1px_2px_rgba(0,0,0,0.03)] backdrop-blur-md sm:px-6 dark:border-slate-800/80 dark:bg-slate-900/95">
        <div className="flex items-center gap-3">
          <Image
            src="/bodla_parivar_logo.png"
            alt="Bodla Parivar"
            width={1069}
            height={258}
            className="h-9 w-auto object-contain dark:brightness-110"
            priority
          />
          <div className="hidden sm:block border-l border-[#efe5d3] pl-3 dark:border-slate-800">
            <span className="text-xs text-slate-600 dark:text-slate-300">Bodla, Mehsana</span>
          </div>
        </div>

        <div className="flex items-center gap-2 sm:gap-3">
          <button type="button" onClick={toggleLanguage} aria-label="Toggle portal language" title="Toggle portal language" className="flex h-9 items-center gap-1.5 rounded-xl border border-slate-200 bg-slate-50 px-2.5 text-xs font-semibold text-slate-700 transition hover:bg-slate-100 dark:border-slate-800 dark:bg-slate-800 dark:text-slate-200 dark:hover:bg-slate-700"><Globe className="h-3.5 w-3.5 text-navy-700 dark:text-sky-400" /><span>{lang === 'gu' ? 'English' : 'ગુજરાતી'}</span></button>
          <button onClick={() => setActiveTab('notifications')} aria-label="Open notifications" className="flex h-9 w-9 items-center justify-center rounded-full bg-blue-50 text-blue-700 transition hover:bg-blue-100 dark:bg-slate-800 dark:text-sky-300"><Bell className="h-4 w-4" /></button>
          {/* Dark / Light Mode Toggle */}
          <button
            onClick={toggleTheme}
            className="flex h-9 w-9 items-center justify-center rounded-xl border border-slate-200 bg-slate-50 text-slate-600 transition hover:bg-slate-100 dark:border-slate-800 dark:bg-slate-800 dark:text-slate-300 dark:hover:bg-slate-700"
            title={theme === 'dark' ? 'Switch to Light Mode' : 'Switch to Dark Mode'}
            aria-label="Toggle theme"
          >
            {theme === 'dark' ? <Sun className="h-4 w-4 text-amber-400" /> : <Moon className="h-4 w-4 text-slate-700" />}
          </button>

          {/* User profile dropdown pill */}
          <button onClick={() => setActiveTab('profile')} aria-label="Open profile" className="flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-3 py-1.5 dark:border-slate-800 dark:bg-slate-850">
            <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-indigo-50 text-indigo-700 font-bold text-xs dark:bg-indigo-950 dark:text-indigo-300">
              {profileName ? profileName.charAt(0).toUpperCase() : 'U'}
            </div>
            <div className="hidden sm:block text-left text-xs">
              <span className="block font-bold text-slate-800 dark:text-slate-200 leading-tight">
                {profileName || 'Resident Member'}
              </span>
              <span className="text-[10px] text-slate-500 dark:text-slate-400 truncate max-w-[140px] block">
                {email}
              </span>
            </div>
          </button>

          {/* Sign Out Button */}
          <button
            onClick={() => setIsSignOutModalOpen(true)}
            className="flex h-9 items-center gap-1.5 rounded-xl border border-slate-200 bg-white px-3 text-xs font-semibold text-slate-600 shadow-sm transition hover:bg-rose-50 hover:text-rose-700 hover:border-rose-200 dark:border-slate-800 dark:bg-slate-850 dark:text-slate-300 dark:hover:bg-rose-950/60 dark:hover:text-rose-400"
          >
            <LogOut className="h-3.5 w-3.5" />
            <span className="hidden sm:inline">Sign out</span>
          </button>
        </div>
      </header>

      {/* Main Layout Body */}
      <div className="flex w-full min-w-0 flex-1 flex-col gap-6 px-4 pb-24 pt-5 sm:px-6 sm:pt-7 md:pb-8 lg:px-8 2xl:px-10">

        {/* Navigation Tabs Bar */}
        <div className="hidden overflow-x-auto rounded-2xl border border-slate-200 bg-white p-1.5 shadow-sm dark:border-slate-800 dark:bg-slate-900 scrollbar-none">
          {[
            { key: 'overview', label: 'Overview', icon: Sparkles },
            { key: 'profile', label: 'My Profile', icon: User },
            { key: 'notices', label: 'Notices & Circulars', icon: Bell, count: notices.length },
            { key: 'events', label: 'Village Events', icon: Calendar, count: events.length },
            { key: 'complaints', label: 'My Grievances', icon: MessageSquareWarning, count: complaints.length },
            { key: 'history', label: 'Activity & History', icon: History },
            { key: 'settings', label: 'Settings', icon: Settings },
          ].map((tab) => {
            const Icon = tab.icon;
            const isActive = activeTab === tab.key;
            return (
              <button
                key={tab.key}
                onClick={() => setActiveTab(tab.key as TabKey)}
                className={`flex shrink-0 items-center gap-2 rounded-xl px-4 py-2.5 text-xs font-semibold transition-all ${
                  isActive
                    ? 'bg-navy-900 text-white shadow-sm dark:bg-sky-600 dark:text-white'
                    : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900 dark:text-slate-400 dark:hover:bg-slate-800 dark:hover:text-white'
                }`}
              >
                <Icon className="h-4 w-4" />
                <span>{tab.label}</span>
                {tab.count !== undefined && tab.count > 0 && (
                  <span
                    className={`ml-1 rounded-full px-1.5 py-0.2 text-[10px] font-bold ${
                      isActive
                        ? 'bg-white/20 text-white'
                        : 'bg-slate-200 text-slate-700 dark:bg-slate-800 dark:text-slate-300'
                    }`}
                  >
                    {tab.count}
                  </span>
                )}
              </button>
            );
          })}
        </div>

        {/* Tab 1: OVERVIEW */}
        {activeTab === 'overview' && (
          <div className="space-y-8">
            <section className="relative isolate overflow-hidden rounded-3xl border border-slate-200 bg-gradient-to-br from-white via-white to-sky-50 px-6 py-8 text-slate-900 shadow-sm dark:border-slate-800 dark:from-slate-900 dark:via-slate-900 dark:to-slate-800 dark:text-white sm:px-9 sm:py-10">
              <div aria-hidden="true" className="absolute -right-12 -top-24 -z-10 h-72 w-72 rounded-full border-[36px] border-sky-100 dark:border-sky-900/40" />
              <div aria-hidden="true" className="absolute -bottom-28 right-1/4 -z-10 h-56 w-56 rounded-full bg-amber-200/40 blur-2xl dark:bg-amber-400/10" />
              <p className="text-xs font-bold uppercase tracking-[0.2em] text-sky-700 dark:text-sky-400">Bodla Parivar · Community Portal</p>
              <h1 className="mt-3 text-2xl font-bold sm:text-3xl">નમસ્કાર, {profileName || 'સભ્ય'}!</h1>
              <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-600 dark:text-slate-300 sm:text-base">બોડલા પરિવારના સમુદાયમાં આપનું સ્વાગત છે. અહીંથી સમાચાર, કાર્યક્રમો અને સભ્ય સેવાઓ મેળવો.</p>
              <div className="mt-6 flex flex-wrap gap-3 text-xs font-medium text-slate-700 dark:text-slate-200">
                <span className="rounded-full border border-slate-200 bg-white px-3 py-1.5 shadow-sm dark:border-slate-700 dark:bg-slate-800">{notices.length} community notices</span>
                <span className="rounded-full border border-slate-200 bg-white px-3 py-1.5 shadow-sm dark:border-slate-700 dark:bg-slate-800">{events.length} upcoming events</span>
              </div>
            </section>

            <label className="mx-auto flex h-12 w-full max-w-3xl items-center gap-3 rounded-2xl border border-[#e8dfd2] bg-white px-4 text-slate-500 shadow-sm ring-1 ring-black/[0.02] dark:border-slate-700 dark:bg-slate-900">
              <Search className="h-4 w-4 shrink-0" />
              <input value={searchQuery} onChange={(event) => setSearchQuery(event.target.value)} placeholder="સુવિધા શોધો (ગુજરાતી / અંગ્રેજી)" className="w-full bg-transparent text-sm text-slate-900 outline-none placeholder:text-slate-500 dark:text-white" />
            </label>

            {notices.find((notice) => notice.is_pinned) && (
              <button onClick={() => setActiveTab('notices')} className="w-full rounded-2xl border border-amber-300 bg-amber-50/80 p-4 text-left dark:border-amber-900 dark:bg-amber-950/30">
                <span className="text-xs font-bold text-amber-800 dark:text-amber-300">મહત્વપૂર્ણ સૂચના · Important notice</span>
                <span className="mt-1 block font-semibold text-slate-900 dark:text-white">{notices.find((notice) => notice.is_pinned)?.title_en || notices.find((notice) => notice.is_pinned)?.title_gu}</span>
              </button>
            )}

            <section aria-labelledby="quick-services-heading">
              <h2 id="quick-services-heading" className="mb-3 text-base font-semibold text-slate-900 dark:text-white">ઝડપી સેવાઓ</h2>
              <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4 2xl:grid-cols-6">
                {dashboardServices.map(({ title, subtitle, icon: Icon, action }, index) => (
                  <button key={title} onClick={action} className="group flex min-h-32 flex-col items-center justify-center rounded-2xl border border-slate-200 bg-white p-4 text-center shadow-sm transition hover:-translate-y-1 hover:border-sky-300 hover:shadow-lg dark:border-slate-700 dark:bg-slate-900">
                    <span className={`mb-2 flex h-11 w-11 items-center justify-center rounded-xl ${['bg-amber-100 text-amber-700', 'bg-sky-100 text-sky-700', 'bg-rose-100 text-rose-700', 'bg-indigo-100 text-indigo-700', 'bg-emerald-100 text-emerald-700', 'bg-violet-100 text-violet-700'][index % 6]}`}><Icon className="h-5 w-5" /></span>
                    <span className="text-sm font-semibold text-slate-900 dark:text-white">{title}</span>
                    <span className="mt-1 text-[11px] text-slate-500 dark:text-slate-400">{subtitle}</span>
                  </button>
                ))}
              </div>
              {dashboardServices.length === 0 && <p className="rounded-xl border border-[#eee4d5] bg-[#fffdf8] p-4 text-sm text-slate-600 dark:border-slate-700 dark:bg-slate-900">No services match your search.</p>}
            </section>

            <button onClick={() => setActiveTab('history')} className="flex w-full items-center gap-4 rounded-2xl border border-[#e8dfd2] bg-[#fffdf8] p-4 text-left transition hover:border-amber-300 dark:border-slate-700 dark:bg-slate-900">
              <span className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-sky-100 text-2xl dark:bg-sky-950">📖</span>
              <span className="min-w-0 flex-1"><span className="block font-semibold text-slate-900 dark:text-white">બોડલા ગામનો ઇતિહાસ</span><span className="mt-1 block text-xs text-slate-600 dark:text-slate-400">ગામના મૂળ અને પ્રગતિની વાતો</span></span>
              <span className="text-xl text-navy-900 dark:text-sky-400">→</span>
            </button>

            {assignedTools.length > 0 && (
              <section aria-labelledby="assigned-tools-heading" className="rounded-3xl border border-slate-200/80 bg-white p-5 shadow-sm sm:p-6 dark:border-slate-800 dark:bg-slate-900">
                <div className="mb-4 flex flex-wrap items-end justify-between gap-2">
                  <div>
                    <p className="text-[11px] font-bold uppercase tracking-wider text-sky-700 dark:text-sky-400">Your account access</p>
                    <h2 id="assigned-tools-heading" className="mt-1 text-lg font-bold text-slate-900 dark:text-white">Your assigned tools</h2>
                  </div>
                  <p className="text-xs text-slate-500 dark:text-slate-400">Open a tool to continue in the web app.</p>
                </div>
                <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-3">
                  {assignedTools.map(({ feature, title, description, href, tab, icon: Icon }) => {
                    const content = (
                      <>
                        <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-white text-navy-900 shadow-sm dark:bg-slate-900 dark:text-sky-300">
                          <Icon aria-hidden="true" className="h-5 w-5" />
                        </span>
                        <span className="min-w-0 text-left">
                          <span className="block text-sm font-bold text-slate-900 dark:text-white">{title}</span>
                          <span className="mt-1 block text-xs leading-relaxed text-slate-500 dark:text-slate-400">{description}</span>
                        </span>
                      </>
                    );
                    const className = 'group flex min-h-24 items-center gap-4 rounded-2xl border border-slate-200 bg-slate-50/70 p-4 transition hover:border-sky-300 hover:bg-sky-50/70 dark:border-slate-700 dark:bg-slate-800/50 dark:hover:border-sky-700 dark:hover:bg-sky-950/30';

                    return tab ? (
                      <button key={feature} type="button" onClick={() => setActiveTab(tab)} className={className}>{content}</button>
                    ) : (
                      <Link key={feature} href={href ?? '/'} className={className}>{content}</Link>
                    );
                  })}
                </div>
              </section>
            )}

            {/* Split Sections: Recent Notices & Upcoming Events */}
            <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
              {/* Notices list */}
              <div className="rounded-3xl border border-slate-200/80 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
                <div className="flex items-center justify-between border-b border-slate-100 pb-4 dark:border-slate-800">
                  <div className="flex items-center gap-2">
                    <Bell className="h-5 w-5 text-navy-900 dark:text-sky-400" />
                    <h2 className="text-base font-bold text-slate-900 dark:text-white">Recent Notices</h2>
                  </div>
                  <button
                    onClick={() => setActiveTab('notices')}
                    className="text-xs font-semibold text-sky-600 hover:text-sky-700 dark:text-sky-400"
                  >
                    View all →
                  </button>
                </div>

                <div className="mt-4 space-y-3">
                  {isLoading ? (
                    <LoadingState message="Loading notices…" />
                  ) : notices.length === 0 ? (
                    <EmptyState title="No notices" description="There are no active notices published yet." />
                  ) : (
                    notices.slice(0, 4).map((notice) => (
                      <div
                        key={notice.id}
                        className="rounded-2xl border border-slate-100 bg-slate-50/60 p-4 transition hover:bg-slate-50 dark:border-slate-800/60 dark:bg-slate-800/40 dark:hover:bg-slate-800/70"
                      >
                        <div className="flex items-start justify-between gap-2">
                          <h4 className="font-semibold text-slate-900 text-sm dark:text-white">
                            {notice.title_en || notice.title_gu}
                          </h4>
                          {notice.is_pinned && (
                            <span className="shrink-0 rounded-full bg-amber-50 px-2 py-0.5 text-[10px] font-bold text-amber-700 dark:bg-amber-950/80 dark:text-amber-400">
                              PINNED
                            </span>
                          )}
                        </div>
                        {notice.title_gu && notice.title_en && (
                          <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">{notice.title_gu}</p>
                        )}
                        <time className="mt-2 block text-[11px] text-slate-400">
                          {new Date(notice.created_at).toLocaleDateString(undefined, { dateStyle: 'medium' })}
                        </time>
                      </div>
                    ))
                  )}
                </div>
              </div>

              {/* Events list */}
              <div className="rounded-3xl border border-slate-200/80 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
                <div className="flex items-center justify-between border-b border-slate-100 pb-4 dark:border-slate-800">
                  <div className="flex items-center gap-2">
                    <Calendar className="h-5 w-5 text-navy-900 dark:text-sky-400" />
                    <h2 className="text-base font-bold text-slate-900 dark:text-white">Upcoming Community Events</h2>
                  </div>
                  <button
                    onClick={() => setActiveTab('events')}
                    className="text-xs font-semibold text-sky-600 hover:text-sky-700 dark:text-sky-400"
                  >
                    View all →
                  </button>
                </div>

                <div className="mt-4 space-y-3">
                  {isLoading ? (
                    <LoadingState message="Loading events…" />
                  ) : events.length === 0 ? (
                    <EmptyState title="No events" description="No scheduled events right now." />
                  ) : (
                    events.slice(0, 4).map((event) => (
                      <div
                        key={event.id}
                        className="rounded-2xl border border-slate-100 bg-slate-50/60 p-4 transition hover:bg-slate-50 dark:border-slate-800/60 dark:bg-slate-800/40 dark:hover:bg-slate-800/70"
                      >
                        <h4 className="font-semibold text-slate-900 text-sm dark:text-white">
                          {event.title_en || event.title_gu}
                        </h4>
                        <div className="mt-2 flex flex-wrap items-center gap-3 text-xs text-slate-500 dark:text-slate-400">
                          <span className="flex items-center gap-1">
                            <Clock className="h-3.5 w-3.5 text-slate-400" />
                            {new Date(event.start_at).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' })}
                          </span>
                          {event.location_en && (
                            <span className="flex items-center gap-1">
                              <PhoneCall className="h-3.5 w-3.5 text-slate-400" />
                              {event.location_en}
                            </span>
                          )}
                        </div>
                      </div>
                    ))
                  )}
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Tab 2: PROFILE MANAGEMENT */}
        {activeTab === 'profile' && (
          <div className="max-w-3xl space-y-6">
            <div className="rounded-3xl border border-slate-200/80 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
              <div className="flex items-center gap-3 border-b border-slate-100 pb-5 dark:border-slate-800">
                <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-indigo-50 text-indigo-700 font-bold text-lg dark:bg-indigo-950 dark:text-indigo-300">
                  {profileName ? profileName.charAt(0).toUpperCase() : 'U'}
                </div>
                <div>
                  <h2 className="text-lg font-bold text-slate-900 dark:text-white">Profile Information</h2>
                  <p className="text-xs text-slate-500 dark:text-slate-400">Update your community contact details</p>
                </div>
              </div>

              {profileMessage && (
                <div
                  className={`mt-4 rounded-xl p-3 text-xs font-semibold ${
                    profileMessage.includes('success')
                      ? 'bg-emerald-50 text-emerald-800 dark:bg-emerald-950/80 dark:text-emerald-300'
                      : 'bg-rose-50 text-rose-800 dark:bg-rose-950/80 dark:text-rose-300'
                  }`}
                >
                  {profileMessage}
                </div>
              )}

              <form onSubmit={handleUpdateProfile} className="mt-6 space-y-4">
                <div>
                  <label className="block text-xs font-semibold uppercase text-slate-600 dark:text-slate-300">
                    Full Name
                  </label>
                  <input
                    type="text"
                    required
                    value={profileName}
                    onChange={(e) => setProfileName(e.target.value)}
                    className="mt-1.5 w-full rounded-xl border border-slate-200 bg-white px-3.5 py-2.5 text-sm text-slate-900 outline-none transition focus:border-navy-900 dark:border-slate-700 dark:bg-slate-800 dark:text-white dark:focus:border-sky-400"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold uppercase text-slate-600 dark:text-slate-300">
                    Email Address (Read-only)
                  </label>
                  <input
                    type="email"
                    disabled
                    value={email}
                    className="mt-1.5 w-full rounded-xl border border-slate-200 bg-slate-100 px-3.5 py-2.5 text-sm text-slate-500 cursor-not-allowed dark:border-slate-800 dark:bg-slate-850 dark:text-slate-400"
                  />
                  <p className="mt-1 text-[11px] text-slate-400">Email is linked to your authentication account.</p>
                </div>

                <div>
                  <label className="block text-xs font-semibold uppercase text-slate-600 dark:text-slate-300">
                    Mobile Phone Number
                  </label>
                  <input
                    type="tel"
                    placeholder="+91 98765 43210"
                    value={phone}
                    onChange={(e) => setPhone(e.target.value)}
                    className="mt-1.5 w-full rounded-xl border border-slate-200 bg-white px-3.5 py-2.5 text-sm text-slate-900 outline-none transition focus:border-navy-900 dark:border-slate-700 dark:bg-slate-800 dark:text-white dark:focus:border-sky-400"
                  />
                </div>

                <div className="pt-2">
                  <button
                    type="submit"
                    disabled={isUpdatingProfile}
                    className="inline-flex items-center gap-2 rounded-xl bg-navy-900 px-5 py-2.5 text-xs font-semibold text-white shadow-sm transition hover:bg-navy-800 disabled:opacity-50 dark:bg-sky-600 dark:hover:bg-sky-500"
                  >
                    <Save className="h-4 w-4" />
                    {isUpdatingProfile ? 'Saving…' : 'Save Changes'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}

        {/* Tab 3: NOTICES */}
        {activeTab === 'notices' && (
          <div className="space-y-6">
            <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <h2 className="text-xl font-bold text-slate-900 dark:text-white">Community Notices</h2>
                <p className="text-xs text-slate-500 dark:text-slate-400">Official circulars and announcements from village leadership</p>
              </div>

              {/* Search box */}
              <div className="relative w-full max-w-xs">
                <Search className="absolute left-3 top-3 h-4 w-4 text-slate-400" />
                <input
                  type="text"
                  placeholder="Search notices…"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="w-full rounded-xl border border-slate-200 bg-white py-2 pl-9 pr-3 text-xs text-slate-900 outline-none transition focus:border-navy-900 dark:border-slate-800 dark:bg-slate-900 dark:text-white"
                />
              </div>
            </div>

            {isLoading ? (
              <LoadingState message="Loading community notices…" />
            ) : filteredNotices.length === 0 ? (
              <EmptyState title="No notices found" description="No notices match your search criteria." />
            ) : (
              <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                {filteredNotices.map((n) => (
                  <div
                    key={n.id}
                    className="flex flex-col justify-between rounded-2xl border border-slate-200/80 bg-white p-5 shadow-sm dark:border-slate-800 dark:bg-slate-900"
                  >
                    <div>
                      <div className="flex items-start justify-between gap-2">
                        <h3 className="font-bold text-slate-900 dark:text-white">{n.title_en || n.title_gu}</h3>
                        {n.is_pinned && (
                          <span className="shrink-0 rounded-full bg-amber-50 px-2 py-0.5 text-[10px] font-bold text-amber-700 dark:bg-amber-950/80 dark:text-amber-400">
                            PINNED
                          </span>
                        )}
                      </div>
                      {n.title_gu && n.title_en && (
                        <p className="mt-1 text-xs font-semibold text-amber-600 dark:text-amber-400">{n.title_gu}</p>
                      )}
                      <p className="mt-3 text-xs text-slate-600 dark:text-slate-300 leading-relaxed">
                        {n.description_en || n.description_gu || 'No detailed description available.'}
                      </p>
                      {n.image_url && <img src={n.image_url} alt="Notice attachment" className="mt-3 max-h-72 w-full rounded-xl object-contain" />}
                      {n.attachment_url && <a href={n.attachment_url} target="_blank" rel="noreferrer" className="mt-3 inline-flex rounded-lg bg-blue-50 px-3 py-2 text-xs font-semibold text-blue-800 dark:bg-blue-950 dark:text-sky-300">Open video or document</a>}
                    </div>
                    <div className="mt-4 border-t border-slate-100 pt-3 text-[11px] text-slate-400 dark:border-slate-800">
                      Published on {new Date(n.created_at).toLocaleDateString(undefined, { dateStyle: 'long' })}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* Tab 4: EVENTS */}
        {activeTab === 'events' && (
          <div className="space-y-6">
            <div>
              <h2 className="text-xl font-bold text-slate-900 dark:text-white">Village Events & Gatherings</h2>
              <p className="text-xs text-slate-500 dark:text-slate-400">Upcoming cultural, religious, and village assembly dates</p>
            </div>

            {isLoading ? (
              <LoadingState message="Loading events…" />
            ) : events.length === 0 ? (
              <EmptyState title="No upcoming events" description="Stay tuned for upcoming community celebrations and meetings." />
            ) : (
              <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
                {events.map((ev) => (
                  <div
                    key={ev.id}
                    className="flex flex-col justify-between rounded-2xl border border-slate-200/80 bg-white p-5 shadow-sm dark:border-slate-800 dark:bg-slate-900"
                  >
                    <div>
                      <span className="inline-flex rounded-md bg-purple-50 px-2 py-1 text-[10px] font-bold text-purple-700 dark:bg-purple-950/80 dark:text-purple-300">
                        EVENT
                      </span>
                      <h3 className="mt-3 font-bold text-slate-900 dark:text-white">{ev.title_en || ev.title_gu}</h3>
                      {ev.title_gu && ev.title_en && (
                        <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">{ev.title_gu}</p>
                      )}
                    </div>
                    <div className="mt-4 space-y-1.5 border-t border-slate-100 pt-3 text-xs text-slate-600 dark:border-slate-800 dark:text-slate-300">
                      <div className="flex items-center gap-1.5">
                        <Clock className="h-3.5 w-3.5 text-slate-400" />
                        <span>{new Date(ev.start_at).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' })}</span>
                      </div>
                      {ev.location_en && (
                        <div className="flex items-center gap-1.5">
                          <PhoneCall className="h-3.5 w-3.5 text-slate-400" />
                          <span>{ev.location_en}</span>
                        </div>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* Tab 5: COMPLAINTS / CITIZEN GRIEVANCES */}
        {activeTab === 'complaints' && (
          <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
            {/* Create new complaint */}
            <div className="lg:col-span-1 rounded-3xl border border-slate-200/80 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
              <h3 className="text-base font-bold text-slate-900 dark:text-white">Submit New Grievance</h3>
              <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">Directly inform the administration of an issue</p>

              {complaintSuccess && (
                <div className="mt-4 rounded-xl bg-emerald-50 p-3 text-xs font-semibold text-emerald-800 dark:bg-emerald-950/80 dark:text-emerald-300">
                  Grievance submitted successfully! The administration will review it soon.
                </div>
              )}

              <form onSubmit={handleCreateComplaint} className="mt-4 space-y-4">
                <div>
                  <label className="block text-xs font-semibold uppercase text-slate-600 dark:text-slate-300">
                    Subject / Title
                  </label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. Street light repair near temple"
                    value={complaintTitle}
                    onChange={(e) => setComplaintTitle(e.target.value)}
                    className="mt-1.5 w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs text-slate-900 outline-none focus:border-navy-900 dark:border-slate-700 dark:bg-slate-800 dark:text-white dark:focus:border-sky-400"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold uppercase text-slate-600 dark:text-slate-300">
                    Priority
                  </label>
                  <select
                    value={complaintPriority}
                    onChange={(e) => setComplaintPriority(e.target.value as any)}
                    className="mt-1.5 w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs text-slate-900 outline-none focus:border-navy-900 dark:border-slate-700 dark:bg-slate-800 dark:text-white"
                  >
                    <option value="LOW">Low</option>
                    <option value="NORMAL">Normal</option>
                    <option value="HIGH">High</option>
                    <option value="URGENT">Urgent</option>
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-semibold uppercase text-slate-600 dark:text-slate-300">
                    Description & Details
                  </label>
                  <textarea
                    required
                    rows={4}
                    placeholder="Provide specific details and location..."
                    value={complaintDesc}
                    onChange={(e) => setComplaintDesc(e.target.value)}
                    className="mt-1.5 w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs text-slate-900 outline-none focus:border-navy-900 dark:border-slate-700 dark:bg-slate-800 dark:text-white dark:focus:border-sky-400"
                  />
                </div>

                <button
                  type="submit"
                  disabled={isSubmittingComplaint}
                  className="w-full rounded-xl bg-navy-900 py-2.5 text-xs font-semibold text-white shadow-sm transition hover:bg-navy-800 disabled:opacity-50 dark:bg-sky-600 dark:hover:bg-sky-500"
                >
                  {isSubmittingComplaint ? 'Submitting…' : 'Submit Grievance'}
                </button>
              </form>
            </div>

            {/* Complaints list */}
            <div className="lg:col-span-2 space-y-4">
              <div className="flex items-center justify-between">
                <h3 className="text-base font-bold text-slate-900 dark:text-white">My Submitted Grievances</h3>
                <span className="text-xs text-slate-500">{complaints.length} records</span>
              </div>

              {complaints.length === 0 ? (
                <EmptyState
                  title="No grievances submitted"
                  description="You haven't filed any requests or complaints yet."
                />
              ) : (
                <div className="space-y-3">
                  {complaints.map((c) => (
                    <div
                      key={c.id}
                      className="rounded-2xl border border-slate-200/80 bg-white p-5 shadow-sm dark:border-slate-800 dark:bg-slate-900"
                    >
                      <div className="flex flex-wrap items-start justify-between gap-2">
                        <div>
                          <h4 className="font-bold text-slate-900 text-sm dark:text-white">{c.title}</h4>
                          <span className="mt-1 inline-block text-[11px] text-slate-400">
                            Filed on {new Date(c.created_at).toLocaleDateString()}
                          </span>
                        </div>
                        <div className="flex items-center gap-2">
                          <span
                            className={`rounded-full px-2 py-0.5 text-[10px] font-bold ${
                              c.priority === 'URGENT' || c.priority === 'HIGH'
                                ? 'bg-rose-50 text-rose-700 dark:bg-rose-950 dark:text-rose-300'
                                : 'bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-300'
                            }`}
                          >
                            {c.priority}
                          </span>
                          <StatusBadge status={c.status} />
                        </div>
                      </div>

                      <p className="mt-3 text-xs text-slate-600 dark:text-slate-300">{c.description}</p>

                      {c.resolution_note && (
                        <div className="mt-3 rounded-xl border border-emerald-100 bg-emerald-50/70 p-3 text-xs text-emerald-900 dark:border-emerald-900/60 dark:bg-emerald-950/40 dark:text-emerald-300">
                          <span className="font-bold">Admin Resolution:</span> {c.resolution_note}
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        )}

        {activeTab === 'history' && <MemberFeatureViews section="history" fullName={profileName} />}

        {/* Tab 7: SETTINGS */}
        {activeTab === 'settings' && (
          <div className="max-w-2xl space-y-6">
            <div className="rounded-3xl border border-slate-200/80 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
              <h2 className="text-lg font-bold text-slate-900 dark:text-white">Account Preferences</h2>
              <p className="text-xs text-slate-500 dark:text-slate-400">Customize your portal experience</p>

              <div className="mt-6 space-y-5 divide-y divide-slate-100 dark:divide-slate-800">
                <div className="flex items-center justify-between pt-4">
                  <div>
                    <h4 className="font-semibold text-sm text-slate-900 dark:text-white">Theme Mode</h4>
                    <p className="text-xs text-slate-500 dark:text-slate-400">Switch between light and dark themes</p>
                  </div>
                  <button
                    onClick={toggleTheme}
                    className="flex items-center gap-2 rounded-xl border border-slate-200 bg-slate-50 px-3 py-1.5 text-xs font-semibold text-slate-700 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200"
                  >
                    {theme === 'dark' ? <Moon className="h-4 w-4" /> : <Sun className="h-4 w-4" />}
                    <span>{theme === 'dark' ? 'Dark Mode' : 'Light Mode'}</span>
                  </button>
                </div>

                <div className="flex items-center justify-between pt-4">
                  <div>
                    <h4 className="font-semibold text-sm text-slate-900 dark:text-white">Email Notifications</h4>
                    <p className="text-xs text-slate-500 dark:text-slate-400">Receive alerts when new village notices are issued</p>
                  </div>
                  <span className="rounded-full bg-emerald-50 px-2.5 py-0.5 text-xs font-bold text-emerald-700 dark:bg-emerald-950 dark:text-emerald-300">
                    Enabled
                  </span>
                </div>

                <div className="flex items-center justify-between pt-4">
                  <div>
                    <h4 className="font-semibold text-sm text-slate-900 dark:text-white">Security & Password</h4>
                    <p className="text-xs text-slate-500 dark:text-slate-400">Reset or update your credentials</p>
                  </div>
                  <button
                    onClick={() => alert('Password reset links are sent to your verified email address.')}
                    className="rounded-xl border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200"
                  >
                    Reset Password
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}

        {['navratri', 'offerings', 'emergency', 'gallery', 'notifications', 'directory'].includes(activeTab) && <MemberFeatureViews section={activeTab} fullName={profileName} />}
      </div>

      <nav aria-label="Member dashboard navigation" className="fixed inset-x-0 bottom-0 z-30 border-t border-slate-200 bg-white/95 px-2 pb-[max(0.5rem,env(safe-area-inset-bottom))] pt-2 shadow-[0_-4px_18px_rgba(15,23,42,0.08)] backdrop-blur dark:border-slate-800 dark:bg-slate-900/95 md:hidden">
        <div className="mx-auto grid max-w-2xl grid-cols-5 gap-1">
          {[
            { key: 'overview', label: 'Home', icon: Sparkles },
            { key: 'directory', label: 'Members', icon: User },
            { key: 'notices', label: 'Notices', icon: Bell },
            { key: 'offerings', label: 'Offerings', icon: HeartHandshake },
            { key: 'profile', label: 'Profile', icon: User },
          ].map(({ key, label, icon: Icon }) => (
            <button key={key} onClick={() => setActiveTab(key as TabKey)} aria-current={activeTab === key ? 'page' : undefined} className={`flex min-h-12 flex-col items-center justify-center gap-1 rounded-xl text-[10px] font-medium transition sm:text-xs ${activeTab === key ? 'bg-blue-50 text-blue-800 dark:bg-sky-950 dark:text-sky-300' : 'text-slate-500 hover:bg-slate-100 dark:text-slate-400 dark:hover:bg-slate-800'}`}>
              <Icon className="h-4 w-4" />
              <span>{label}</span>
            </button>
          ))}
        </div>
      </nav>

      {/* Confirmation Modal for Sign out */}
      <ConfirmationModal
        isOpen={isSignOutModalOpen}
        onClose={() => setIsSignOutModalOpen(false)}
        onConfirm={onSignOut}
        title="Sign Out"
        description="Are you sure you want to end your current resident session?"
        confirmText="Sign Out"
        isDestructive
      />
      </div>
    </div>
  );
}
