'use client';

import Image from 'next/image';
import { useEffect, useState } from 'react';
import { LogOut } from 'lucide-react';
import { supabase } from '@/lib/supabase';

interface PortalNotice {
  id: string;
  title_gu: string;
  title_en: string;
  created_at: string;
}

interface PortalEvent {
  id: string;
  title_gu: string;
  title_en: string;
  start_at: string;
  location_en: string | null;
}

export default function MemberPortal({
  email,
  fullName,
  onSignOut,
}: {
  email: string;
  fullName: string;
  onSignOut: () => void;
}) {
  const [notices, setNotices] = useState<PortalNotice[]>([]);
  const [events, setEvents] = useState<PortalEvent[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let active = true;
    async function loadCommunityUpdates() {
      const [noticeResult, eventResult] = await Promise.all([
        supabase
          .from('notices')
          .select('id,title_gu,title_en,created_at')
          .eq('status', 'PUBLISHED')
          .order('created_at', { ascending: false })
          .limit(5),
        supabase
          .from('events')
          .select('id,title_gu,title_en,start_at,location_en')
          .eq('status', 'PUBLISHED')
          .gte('start_at', new Date().toISOString())
          .order('start_at', { ascending: true })
          .limit(5),
      ]);

      if (active) {
        setNotices((noticeResult.data ?? []) as PortalNotice[]);
        setEvents((eventResult.data ?? []) as PortalEvent[]);
        setIsLoading(false);
      }
    }

    void loadCommunityUpdates();
    return () => { active = false; };
  }, []);

  return (
    <main className="min-h-screen flex-1 bg-cream px-5 py-6 md:px-10">
      <div className="mx-auto max-w-5xl">
        <header className="flex items-center justify-between gap-4 border-b border-brand-border pb-5">
          <Image src="/bodla_parivar_logo.png" alt="Bodla Parivar" width={1069} height={258} className="h-auto w-40 object-contain" priority />
          <button onClick={onSignOut} className="inline-flex items-center gap-2 border border-brand-border bg-cream-surface px-3 py-2 text-sm font-medium text-brand-text hover:bg-saffron-container">
            <LogOut className="h-4 w-4" /> Sign out
          </button>
        </header>

        <section className="border-b border-brand-border py-7">
          <p className="text-xs font-semibold uppercase text-leaf">Resident account</p>
          <h1 className="mt-2 text-2xl font-bold text-brand-text">Welcome, {fullName || 'Bodla resident'}</h1>
          <p className="mt-1 text-sm text-brand-muted">{email}</p>
        </section>

        <div className="grid gap-8 md:grid-cols-2">
          <section className="py-6">
            <div className="mb-4 flex items-baseline justify-between gap-3">
              <h2 className="text-lg font-bold text-brand-text">Latest notices</h2>
              <span className="text-xs text-brand-muted">{notices.length} published</span>
            </div>
            {isLoading ? <p className="text-sm text-brand-muted">Loading notices…</p> : notices.length ? (
              <ul className="divide-y divide-brand-border border-y border-brand-border">
                {notices.map((notice) => (
                  <li key={notice.id} className="py-4">
                    <p className="font-semibold text-brand-text">{notice.title_en || notice.title_gu}</p>
                    {notice.title_gu && <p className="mt-1 text-sm text-brand-muted">{notice.title_gu}</p>}
                    <time className="mt-2 block text-xs text-brand-muted">{new Date(notice.created_at).toLocaleDateString()}</time>
                  </li>
                ))}
              </ul>
            ) : <p className="border-y border-brand-border py-4 text-sm text-brand-muted">No published notices right now.</p>}
          </section>

          <section className="py-6">
            <div className="mb-4 flex items-baseline justify-between gap-3">
              <h2 className="text-lg font-bold text-brand-text">Upcoming events</h2>
              <span className="text-xs text-brand-muted">{events.length} scheduled</span>
            </div>
            {isLoading ? <p className="text-sm text-brand-muted">Loading events…</p> : events.length ? (
              <ul className="divide-y divide-brand-border border-y border-brand-border">
                {events.map((event) => (
                  <li key={event.id} className="py-4">
                    <p className="font-semibold text-brand-text">{event.title_en || event.title_gu}</p>
                    {event.title_gu && <p className="mt-1 text-sm text-brand-muted">{event.title_gu}</p>}
                    <p className="mt-2 text-xs text-brand-muted">
                      {new Date(event.start_at).toLocaleString()}{event.location_en ? ` · ${event.location_en}` : ''}
                    </p>
                  </li>
                ))}
              </ul>
            ) : <p className="border-y border-brand-border py-4 text-sm text-brand-muted">No upcoming events right now.</p>}
          </section>
        </div>
      </div>
    </main>
  );
}