'use client';

import { useEffect, useState } from 'react';
import { Search, Users } from 'lucide-react';
import { supabase } from '@/lib/supabase';

interface RegisteredProfile {
  id: string;
  full_name: string;
  email: string | null;
  phone: string | null;
  status: string;
  created_at: string;
}

export default function UsersPage() {
  const [profiles, setProfiles] = useState<RegisteredProfile[]>([]);
  const [search, setSearch] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isAdmin, setIsAdmin] = useState(false);

  useEffect(() => {
    let active = true;

    async function loadProfiles() {
      const { data: adminAccess, error: accessError } = await supabase.rpc('is_admin');
      if (accessError) {
        if (active) {
          setError(accessError.code === 'PGRST202'
            ? 'The Supabase admin-role check is not installed in the database yet. Run database/migrations/011_restore_is_admin_rpc.sql in Supabase SQL Editor.'
            : accessError.message);
          setIsLoading(false);
        }
        return;
      }
      if (adminAccess !== true) {
        if (active) {
          setIsAdmin(false);
          setIsLoading(false);
        }
        return;
      }

      const { data, error: queryError } = await supabase
        .from('profiles')
        .select('id,full_name,email,phone,status,created_at')
        .order('created_at', { ascending: false });

      if (!active) return;
      setIsAdmin(true);
      if (queryError) setError(queryError.message);
      else setProfiles((data ?? []) as RegisteredProfile[]);
      setIsLoading(false);
    }

    void loadProfiles();
    return () => { active = false; };
  }, []);

  const normalizedSearch = search.trim().toLowerCase();
  const filteredProfiles = profiles.filter((profile) =>
    [profile.full_name, profile.email ?? '', profile.phone ?? '']
      .join(' ')
      .toLowerCase()
      .includes(normalizedSearch)
  );

  if (isLoading) {
    return <p className="text-sm text-brand-muted">Loading registered users…</p>;
  }

  if (!isAdmin) {
    return (
      <section className="border border-brand-border bg-cream-surface p-5">
        <h1 className="text-lg font-bold text-brand-text">Administrator access required</h1>
        <p className="mt-2 text-sm text-brand-muted">Only accounts with the ADMIN or SUPER_ADMIN role can view the member registry.</p>
        {error && <p role="alert" className="mt-3 text-sm text-rose-700">{error}</p>}
      </section>
    );
  }

  return (
    <div className="space-y-6">
      <header className="flex flex-col gap-4 border-b border-brand-border pb-5 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <div className="flex items-center gap-2 text-saffron">
            <Users className="h-5 w-5" />
            <span className="text-xs font-semibold uppercase">Member registry</span>
          </div>
          <h1 className="mt-2 text-2xl font-bold text-brand-text">Registered users</h1>
          <p className="mt-1 text-sm text-brand-muted">{profiles.length} profiles</p>
        </div>
        <label className="relative block w-full sm:max-w-sm">
          <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-brand-muted" />
          <input
            type="search"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Search name, email, or phone"
            className="w-full border border-brand-border bg-white py-2.5 pl-9 pr-3 text-sm outline-none focus:border-saffron"
          />
        </label>
      </header>

      {error && <p role="alert" className="border border-rose-200 bg-rose-50 p-3 text-sm text-rose-700">Could not load users: {error}</p>}

      <div className="overflow-x-auto border-y border-brand-border">
        <table className="w-full min-w-[680px] text-left text-sm">
          <thead className="bg-cream text-xs uppercase text-brand-muted">
            <tr>
              <th className="px-4 py-3 font-semibold">Name</th>
              <th className="px-4 py-3 font-semibold">Email</th>
              <th className="px-4 py-3 font-semibold">Phone</th>
              <th className="px-4 py-3 font-semibold">Status</th>
              <th className="px-4 py-3 font-semibold">Registered</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-brand-border">
            {filteredProfiles.map((profile) => (
              <tr key={profile.id} className="bg-cream-surface hover:bg-white">
                <td className="px-4 py-3 font-medium text-brand-text">{profile.full_name}</td>
                <td className="px-4 py-3 text-brand-muted">{profile.email || '—'}</td>
                <td className="px-4 py-3 text-brand-muted">{profile.phone || '—'}</td>
                <td className="px-4 py-3">
                  <span className="border border-brand-border px-2 py-1 text-xs text-brand-text">{profile.status}</span>
                </td>
                <td className="px-4 py-3 text-brand-muted">{new Date(profile.created_at).toLocaleDateString()}</td>
              </tr>
            ))}
            {!filteredProfiles.length && (
              <tr>
                <td colSpan={5} className="px-4 py-10 text-center text-brand-muted">
                  {profiles.length
                    ? 'No users match this search.'
                    : 'No public profiles exist yet. Supabase Auth users are separate. Run the complete database/migrations/012_backfill_auth_profiles.sql file in Supabase SQL Editor to backfill them here.'}
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
