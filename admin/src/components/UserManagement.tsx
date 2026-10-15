'use client';

import { useEffect, useState } from 'react';
import { Pencil, Plus, Search, Trash2, Users, X } from 'lucide-react';
import { supabase } from '@/lib/supabase';
import type { UserRole } from '@/lib/types';

interface ManagedUser {
  id: string;
  auth_user_id: string;
  full_name: string;
  email: string | null;
  phone: string | null;
  status: 'ACTIVE' | 'SUSPENDED' | 'DELETED';
  created_at: string;
  auth_exists: boolean;
  roles: UserRole[];
}

interface UserForm {
  full_name: string;
  email: string;
  phone: string;
  status: 'ACTIVE' | 'SUSPENDED';
  roles: UserRole[];
}

const roleOptions: { value: UserRole; label: string; labelGu: string }[] = [
  { value: 'USER', label: 'Member', labelGu: 'સભ્ય' },
  { value: 'BUSINESS_OWNER', label: 'Business owner', labelGu: 'વ્યવસાય માલિક' },
  { value: 'CONTENT_MANAGER', label: 'Content manager', labelGu: 'કન્ટેન્ટ મેનેજર' },
  { value: 'MODERATOR', label: 'Moderator', labelGu: 'મોડરેટર' },
  { value: 'ADMIN', label: 'Administrator', labelGu: 'એડમિન' },
  { value: 'SUPER_ADMIN', label: 'Super administrator', labelGu: 'સુપર એડમિન' },
];

const emptyForm: UserForm = {
  full_name: '',
  email: '',
  phone: '',
  status: 'ACTIVE',
  roles: ['USER'],
};

async function callUsersApi(method: 'GET' | 'POST' | 'PATCH' | 'DELETE', body?: object) {
  const { data: { session }, error: sessionError } = await supabase.auth.getSession();
  if (sessionError) throw new Error(sessionError.message);
  if (!session?.access_token) throw new Error('Sign in again to manage member accounts.');

  const response = await fetch('/api/admin/users', {
    method,
    headers: {
      Authorization: `Bearer ${session.access_token}`,
      ...(body ? { 'Content-Type': 'application/json' } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  });
  const result = await response.json();
  if (!response.ok) {
    const message = typeof result.error === 'string' ? result.error : 'The user request failed.';
    const normalizedMessage = message.toLowerCase();
    if (normalizedMessage.includes('supabase_service_role_key')) {
      throw new Error(message);
    }
    if (normalizedMessage.includes('unregistered api key') || normalizedMessage.includes('invalid api key')) {
      throw new Error('The server-side Supabase API key is not registered for this project. Set SUPABASE_SERVICE_ROLE_KEY to a key from the same Supabase project as NEXT_PUBLIC_SUPABASE_URL, then restart the web server.');
    }
    throw new Error(message);
  }
  return result;
}

export default function UserManagement() {
  const [users, setUsers] = useState<ManagedUser[]>([]);
  const [search, setSearch] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [isAdmin, setIsAdmin] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [form, setForm] = useState<UserForm>(emptyForm);
  const [editingUser, setEditingUser] = useState<ManagedUser | null>(null);
  const [isCreating, setIsCreating] = useState(false);
  const [deletingUser, setDeletingUser] = useState<ManagedUser | null>(null);
  const [isSaving, setIsSaving] = useState(false);

  async function loadUsers() {
    try {
      const result = await callUsersApi('GET');
      setUsers(result.users as ManagedUser[]);
      setIsAdmin(true);
      setError(null);
    } catch (loadError) {
      setUsers([]);
      setIsAdmin(false);
      setError(loadError instanceof Error ? loadError.message : 'Could not load members.');
    } finally {
      setIsLoading(false);
    }
  }

  useEffect(() => {
    let active = true;
    void callUsersApi('GET').then((result) => {
      if (!active) return;
      setUsers(result.users as ManagedUser[]);
      setIsAdmin(true);
      setError(null);
    }).catch((loadError: unknown) => {
      if (!active) return;
      setUsers([]);
      setIsAdmin(false);
      setError(loadError instanceof Error ? loadError.message : 'Could not load members.');
    }).finally(() => {
      if (active) setIsLoading(false);
    });
    return () => { active = false; };
  }, []);

  const normalizedSearch = search.trim().toLowerCase();
  const filteredUsers = users.filter((user) =>
    [user.full_name, user.email ?? '', user.phone ?? '', ...user.roles]
      .join(' ')
      .toLowerCase()
      .includes(normalizedSearch)
  );

  function openCreate() {
    setForm(emptyForm);
    setEditingUser(null);
    setIsCreating(true);
    setError(null);
  }

  function openEdit(user: ManagedUser) {
    setForm({
      full_name: user.full_name,
      email: user.email ?? '',
      phone: user.phone ?? '',
      status: user.status === 'SUSPENDED' ? 'SUSPENDED' : 'ACTIVE',
      roles: [...user.roles],
    });
    setEditingUser(user);
    setIsCreating(false);
    setError(null);
  }

  function closeForm() {
    if (isSaving) return;
    setEditingUser(null);
    setIsCreating(false);
    setError(null);
  }

  function toggleRole(role: UserRole) {
    setForm((current) => ({
      ...current,
      roles: current.roles.includes(role)
        ? current.roles.filter((assigned) => assigned !== role)
        : [...current.roles, role],
    }));
  }

  async function saveUser(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!form.roles.length) {
      setError('Assign at least one role.');
      return;
    }

    setIsSaving(true);
    setError(null);
    try {
      await callUsersApi(editingUser ? 'PATCH' : 'POST', {
        ...(editingUser ? { id: editingUser.id, status: form.status } : {}),
        full_name: form.full_name,
        email: form.email,
        phone: form.phone,
        roles: form.roles,
      });
      setEditingUser(null);
      setIsCreating(false);
      await loadUsers();
    } catch (saveError) {
      setError(saveError instanceof Error ? saveError.message : 'Could not save this member.');
    } finally {
      setIsSaving(false);
    }
  }

  async function deleteUser() {
    if (!deletingUser) return;
    setIsSaving(true);
    setError(null);
    try {
      await callUsersApi('DELETE', { id: deletingUser.id });
      setDeletingUser(null);
      await loadUsers();
    } catch (deleteError) {
      setError(deleteError instanceof Error ? deleteError.message : 'Could not delete this member.');
    } finally {
      setIsSaving(false);
    }
  }

  if (isLoading && !users.length) {
    return <p className="text-sm text-brand-muted">Loading registered users…</p>;
  }

  if (!isAdmin) {
    const hasServerKeyError = error?.includes('server-side Supabase API key')
      || error?.includes('SUPABASE_SERVICE_ROLE_KEY')
      || false;
    return (
      <section className="border border-brand-border bg-cream-surface p-5">
        <h1 className="text-lg font-bold text-brand-text">
          {hasServerKeyError ? 'Server API key configuration required' : 'Administrator access required'}
        </h1>
        <p className="mt-2 text-sm text-brand-muted">
          {hasServerKeyError
            ? 'Create a secret key under Project Settings → API Keys, save it as SUPABASE_SERVICE_ROLE_KEY, and restart the web server. The publishable key cannot manage users.'
            : 'Only ADMIN and SUPER_ADMIN accounts can manage members and roles.'}
        </p>
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
          <h1 className="mt-2 text-2xl font-bold text-brand-text">User management</h1>
          <p className="mt-1 text-sm text-brand-muted">{users.length} registered profiles</p>
        </div>
        <div className="flex flex-col gap-2 sm:flex-row">
          <label className="relative block w-full sm:w-64">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-brand-muted" />
            <input
              type="search"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              placeholder="Search users or roles"
              className="w-full border border-brand-border bg-white py-2.5 pl-9 pr-3 text-sm outline-none focus:border-saffron"
            />
          </label>
          <button
            type="button"
            onClick={openCreate}
            className="inline-flex min-h-10 items-center justify-center gap-2 bg-saffron px-4 text-sm font-semibold text-white hover:bg-saffron-dark"
          >
            <Plus className="h-4 w-4" /> Add user
          </button>
        </div>
      </header>

      {error && <p role="alert" className="border border-rose-200 bg-rose-50 p-3 text-sm text-rose-700">{error}</p>}

      <div className="overflow-x-auto border-y border-brand-border">
        <table className="w-full min-w-[880px] text-left text-sm">
          <thead className="bg-cream text-xs uppercase text-brand-muted">
            <tr>
              <th className="px-4 py-3 font-semibold">Name</th>
              <th className="px-4 py-3 font-semibold">Email / Phone</th>
              <th className="px-4 py-3 font-semibold">Roles</th>
              <th className="px-4 py-3 font-semibold">Status</th>
              <th className="px-4 py-3 font-semibold">Registered</th>
              <th className="px-4 py-3 text-right font-semibold">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-brand-border">
            {filteredUsers.map((user) => (
              <tr key={user.id} className="bg-cream-surface hover:bg-white">
                <td className="px-4 py-3 font-medium text-brand-text">{user.full_name}</td>
                <td className="px-4 py-3 text-brand-muted">
                  <span className="block">{user.email || '—'}</span>
                  <span className="mt-1 block text-xs">{user.phone || '—'}</span>
                </td>
                <td className="px-4 py-3">
                  <div className="flex max-w-72 flex-wrap gap-1.5">
                    {user.roles.map((role) => (
                      <span key={role} className="border border-brand-border px-2 py-1 text-[11px] text-brand-text">{role}</span>
                    ))}
                    {!user.roles.length && <span className="text-xs text-rose-700">No role assigned</span>}
                  </div>
                </td>
                <td className="px-4 py-3">
                  <span className={`border px-2 py-1 text-xs ${user.status === 'ACTIVE' ? 'border-emerald-200 text-emerald-800' : 'border-amber-200 text-amber-800'}`}>
                    {user.status}
                  </span>
                  {!user.auth_exists && <span className="mt-1 block text-xs text-rose-700">Auth account missing</span>}
                </td>
                <td className="px-4 py-3 text-brand-muted">{new Date(user.created_at).toLocaleDateString()}</td>
                <td className="px-4 py-3">
                  <div className="flex justify-end gap-1">
                    <button
                      type="button"
                      onClick={() => openEdit(user)}
                      title="Edit user and roles"
                      aria-label={`Edit ${user.full_name}`}
                      className="inline-flex h-9 w-9 items-center justify-center text-brand-muted hover:bg-saffron-container hover:text-brand-text"
                    >
                      <Pencil className="h-4 w-4" />
                    </button>
                    <button
                      type="button"
                      onClick={() => { setDeletingUser(user); setError(null); }}
                      title="Delete user"
                      aria-label={`Delete ${user.full_name}`}
                      className="inline-flex h-9 w-9 items-center justify-center text-rose-700 hover:bg-rose-50"
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  </div>
                </td>
              </tr>
            ))}
            {!filteredUsers.length && (
              <tr>
                <td colSpan={6} className="px-4 py-10 text-center text-brand-muted">
                  {users.length ? 'No users match this search.' : 'No registered user profiles found.'}
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {(isCreating || editingUser) && (
        <div className="fixed inset-0 z-50 flex items-center justify-center overflow-y-auto bg-black/40 p-4">
          <section role="dialog" aria-modal="true" aria-labelledby="user-form-title" className="my-auto w-full max-w-xl border border-brand-border bg-cream-surface shadow-xl">
            <header className="flex items-center justify-between border-b border-brand-border px-5 py-4">
              <h2 id="user-form-title" className="text-lg font-bold text-brand-text">{editingUser ? 'Edit user' : 'Invite user'}</h2>
              <button type="button" onClick={closeForm} aria-label="Close" className="inline-flex h-9 w-9 items-center justify-center text-brand-muted hover:bg-cream">
                <X className="h-4 w-4" />
              </button>
            </header>
            <form onSubmit={saveUser} className="space-y-5 p-5">
              <div className="grid gap-4 sm:grid-cols-2">
                <label className="text-sm font-medium text-brand-text">
                  Full name
                  <input required maxLength={120} value={form.full_name} onChange={(event) => setForm({ ...form, full_name: event.target.value })} className="mt-1.5 w-full border border-brand-border bg-white px-3 py-2.5 outline-none focus:border-saffron" />
                </label>
                <label className="text-sm font-medium text-brand-text">
                  Email
                  <input required type="email" value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} className="mt-1.5 w-full border border-brand-border bg-white px-3 py-2.5 outline-none focus:border-saffron" />
                </label>
                <label className="text-sm font-medium text-brand-text">
                  Phone
                  <input type="tel" value={form.phone} onChange={(event) => setForm({ ...form, phone: event.target.value })} className="mt-1.5 w-full border border-brand-border bg-white px-3 py-2.5 outline-none focus:border-saffron" />
                </label>
                {editingUser && (
                  <label className="text-sm font-medium text-brand-text">
                    Account status
                    <select value={form.status} onChange={(event) => setForm({ ...form, status: event.target.value as UserForm['status'] })} className="mt-1.5 w-full border border-brand-border bg-white px-3 py-2.5 outline-none focus:border-saffron">
                      <option value="ACTIVE">Active</option>
                      <option value="SUSPENDED">Suspended</option>
                    </select>
                  </label>
                )}
              </div>

              <fieldset>
                <legend className="text-sm font-semibold text-brand-text">Assigned roles</legend>
                <div className="mt-2 grid gap-2 sm:grid-cols-2">
                  {roleOptions.map((role) => (
                    <label key={role.value} className="flex min-h-11 items-center gap-2 border border-brand-border bg-white px-3 py-2 text-sm text-brand-text">
                      <input type="checkbox" checked={form.roles.includes(role.value)} onChange={() => toggleRole(role.value)} className="h-4 w-4 accent-amber-600" />
                      <span>{role.label}</span>
                      <span className="ml-auto text-xs text-brand-muted">{role.labelGu}</span>
                    </label>
                  ))}
                </div>
              </fieldset>

              {isCreating && <p className="text-xs leading-5 text-brand-muted">An invitation email will be sent. The account becomes active after the user accepts it.</p>}
              {error && <p role="alert" className="border border-rose-200 bg-rose-50 p-3 text-sm text-rose-700">{error}</p>}
              <footer className="flex justify-end gap-2 border-t border-brand-border pt-4">
                <button type="button" disabled={isSaving} onClick={closeForm} className="min-h-10 border border-brand-border px-4 text-sm font-medium text-brand-text hover:bg-cream disabled:opacity-50">Cancel</button>
                <button type="submit" disabled={isSaving || !form.roles.length} className="min-h-10 bg-saffron px-4 text-sm font-semibold text-white hover:bg-saffron-dark disabled:opacity-50">
                  {isSaving ? 'Saving…' : editingUser ? 'Save changes' : 'Send invitation'}
                </button>
              </footer>
            </form>
          </section>
        </div>
      )}

      {deletingUser && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
          <section role="alertdialog" aria-modal="true" aria-labelledby="delete-user-title" className="w-full max-w-md border border-brand-border bg-cream-surface p-5 shadow-xl">
            <h2 id="delete-user-title" className="text-lg font-bold text-brand-text">Delete user account?</h2>
            <p className="mt-2 text-sm leading-6 text-brand-muted">
              This permanently deletes <span className="font-semibold text-brand-text">{deletingUser.full_name}</span> from Supabase Auth and removes their profile and role assignments. This cannot be undone.
            </p>
            {error && <p role="alert" className="mt-3 border border-rose-200 bg-rose-50 p-3 text-sm text-rose-700">{error}</p>}
            <footer className="mt-5 flex justify-end gap-2">
              <button type="button" disabled={isSaving} onClick={() => setDeletingUser(null)} className="min-h-10 border border-brand-border px-4 text-sm font-medium text-brand-text hover:bg-cream disabled:opacity-50">Cancel</button>
              <button type="button" disabled={isSaving} onClick={() => void deleteUser()} className="min-h-10 bg-rose-700 px-4 text-sm font-semibold text-white hover:bg-rose-800 disabled:opacity-50">
                {isSaving ? 'Deleting…' : 'Delete account'}
              </button>
            </footer>
          </section>
        </div>
      )}
    </div>
  );
}
