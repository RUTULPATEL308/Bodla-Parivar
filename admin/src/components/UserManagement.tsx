'use client';

import { useEffect, useState } from 'react';
import { Pencil, Plus, Search, Trash2, Users, X, Shield, Filter, CheckCircle2, UserCheck, AlertTriangle } from 'lucide-react';
import { supabase } from '@/lib/supabase';
import PrintControls from '@/components/PrintControls';
import StatusBadge from '@/components/StatusBadge';
import ConfirmationModal from '@/components/ConfirmationModal';
import EmptyState from '@/components/EmptyState';
import LoadingState from '@/components/LoadingState';
import type { UserFeature, UserRole } from '@/lib/types';

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
  feature_permissions: UserFeature[];
}

interface UserForm {
  full_name: string;
  email: string;
  phone: string;
  status: 'ACTIVE' | 'SUSPENDED';
  roles: UserRole[];
  feature_permissions: UserFeature[];
}

const featureOptions: { value: UserFeature; label: string; labelGu: string }[] = [
  { value: 'members', label: 'Members & Roles', labelGu: 'સભ્યો અને ભૂમિકાઓ' },
  { value: 'notices', label: 'Notice Management', labelGu: 'સૂચના વ્યવસ્થાપન' },
  { value: 'offerings', label: 'Offering Management', labelGu: 'ચઢાવા વ્યવસ્થાપન' },
  { value: 'navratri', label: 'Navratri Management', labelGu: 'નવરાત્રી વ્યવસ્થાપન' },
  { value: 'complaints', label: 'Complaints', labelGu: 'ફરિયાદો' },
  { value: 'emergency_contacts', label: 'Emergency Contacts', labelGu: 'તાત્કાલિક સંપર્ક' },
  { value: 'village_history', label: 'Village History', labelGu: 'ગામનો ઇતિહાસ' },
  { value: 'notifications', label: 'Notifications', labelGu: 'સૂચનાઓ' },
];

const roleOptions: { value: UserRole; label: string; labelGu: string; description: string }[] = [
  { value: 'USER', label: 'Resident Member', labelGu: 'સભ્ય', description: 'Standard community member' },
  { value: 'BUSINESS_OWNER', label: 'Business Owner', labelGu: 'વ્યવસાય માલિક', description: 'Can register and edit directory listings' },
  { value: 'CONTENT_MANAGER', label: 'Content Manager', labelGu: 'કન્ટેન્ટ મેનેજર', description: 'Can draft and edit announcements' },
  { value: 'MODERATOR', label: 'Moderator', labelGu: 'મોડરેટર', description: 'Reviews submissions and grievances' },
  { value: 'ADMIN', label: 'Administrator', labelGu: 'એડમિન', description: 'Manages members, features, and settings' },
  { value: 'SUPER_ADMIN', label: 'Super Administrator', labelGu: 'સુપર એડમિન', description: 'Full system and authorization control' },
];

const emptyForm: UserForm = {
  full_name: '',
  email: '',
  phone: '',
  status: 'ACTIVE',
  roles: ['USER'],
  feature_permissions: [],
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
  const [roleFilter, setRoleFilter] = useState<string>('ALL');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
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
      setUsers((result.users as ManagedUser[]).map((user) => ({ ...user, feature_permissions: user.feature_permissions ?? [] })));
      setIsAdmin(result.canManageUsers === true);
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
      setUsers((result.users as ManagedUser[]).map((user) => ({ ...user, feature_permissions: user.feature_permissions ?? [] })));
      setIsAdmin(result.canManageUsers === true);
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
  const filteredUsers = users.filter((user) => {
    const matchesSearch = [user.full_name, user.email ?? '', user.phone ?? '', ...user.roles]
      .join(' ')
      .toLowerCase()
      .includes(normalizedSearch);

    const matchesRole = roleFilter === 'ALL' || user.roles.includes(roleFilter as UserRole);
    const matchesStatus = statusFilter === 'ALL' || user.status === statusFilter;

    return matchesSearch && matchesRole && matchesStatus;
  });

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
      feature_permissions: [...user.feature_permissions],
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

  function toggleFeature(feature: UserFeature) {
    setForm((current) => ({
      ...current,
      feature_permissions: current.feature_permissions.includes(feature)
        ? current.feature_permissions.filter((item) => item !== feature)
        : [...current.feature_permissions, feature],
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
        feature_permissions: form.feature_permissions,
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
    return <LoadingState message="Loading registered citizens & staff accounts…" />;
  }

  if (!isAdmin && error) {
    const hasServerKeyError = error?.includes('server-side Supabase API key')
      || error?.includes('SUPABASE_SERVICE_ROLE_KEY')
      || false;
    return (
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <div className="flex items-center gap-3 text-rose-600">
          <AlertTriangle className="h-6 w-6" />
          <h1 className="text-lg font-bold text-slate-900 dark:text-white">
            {hasServerKeyError ? 'Server API Key Configuration Required' : 'Administrator Access Required'}
          </h1>
        </div>
        <p className="mt-2 text-xs text-slate-500 dark:text-slate-400 leading-relaxed">
          {hasServerKeyError
            ? 'Create a secret key under Supabase Project Settings → API Keys, save it as SUPABASE_SERVICE_ROLE_KEY, and restart the web server.'
            : 'Only ADMIN and SUPER_ADMIN accounts can manage registered members and assign role-based permissions.'}
        </p>
        {error && <p role="alert" className="mt-3 rounded-xl bg-rose-50 p-3 text-xs text-rose-800 dark:bg-rose-950 dark:text-rose-300">{error}</p>}
      </section>
    );
  }

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <header className="flex flex-col gap-4 border-b border-slate-200/80 pb-5 sm:flex-row sm:items-end sm:justify-between dark:border-slate-800">
        <div>
          <div className="flex items-center gap-2 text-sky-600 dark:text-sky-400">
            <Users className="h-5 w-5" />
            <span className="text-xs font-semibold uppercase tracking-wider">Citizen & Staff Directory</span>
          </div>
          <h1 className="mt-2 text-2xl font-bold tracking-tight text-slate-900 dark:text-white">
            User & Role Management
          </h1>
          <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
            Create, edit, suspend accounts and configure granular permission access across the portal.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2.5">
          <div className="no-print">
            <PrintControls />
          </div>
          <button
            type="button"
            onClick={openCreate}
            className="inline-flex items-center gap-2 rounded-xl bg-navy-900 px-4 py-2.5 text-xs font-semibold text-white shadow-sm transition hover:bg-navy-800 dark:bg-sky-600 dark:hover:bg-sky-500 active:scale-95"
          >
            <Plus className="h-4 w-4" />
            Add New Member
          </button>
        </div>
      </header>

      {/* Filter and Search Bar */}
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
        {/* Search */}
        <div className="relative sm:col-span-1">
          <Search className="absolute left-3.5 top-3 h-4 w-4 text-slate-400" />
          <input
            type="search"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search by name, email, phone, role…"
            className="w-full rounded-xl border border-slate-200 bg-white py-2 pl-9 pr-3 text-xs text-slate-900 outline-none transition focus:border-navy-900 dark:border-slate-800 dark:bg-slate-900 dark:text-white dark:focus:border-sky-400"
          />
        </div>

        {/* Role Filter */}
        <div className="relative">
          <select
            value={roleFilter}
            onChange={(e) => setRoleFilter(e.target.value)}
            className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs text-slate-900 outline-none transition focus:border-navy-900 dark:border-slate-800 dark:bg-slate-900 dark:text-white"
          >
            <option value="ALL">All Roles ({users.length})</option>
            <option value="SUPER_ADMIN">Super Admins</option>
            <option value="ADMIN">Administrators</option>
            <option value="MODERATOR">Moderators</option>
            <option value="CONTENT_MANAGER">Content Managers</option>
            <option value="BUSINESS_OWNER">Business Owners</option>
            <option value="USER">Resident Members</option>
          </select>
        </div>

        {/* Status Filter */}
        <div className="relative">
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs text-slate-900 outline-none transition focus:border-navy-900 dark:border-slate-800 dark:bg-slate-900 dark:text-white"
          >
            <option value="ALL">All Statuses</option>
            <option value="ACTIVE">Active Only</option>
            <option value="SUSPENDED">Suspended Only</option>
          </select>
        </div>
      </div>

      {error && (
        <div className="rounded-xl border border-rose-200 bg-rose-50 p-3 text-xs text-rose-800 dark:border-rose-900 dark:bg-rose-950 dark:text-rose-300">
          {error}
        </div>
      )}

      {/* Users Table Card */}
      <div className="overflow-hidden rounded-2xl border border-slate-200/80 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <div className="overflow-x-auto">
          <table data-print-area className="w-full text-left text-xs text-slate-800 dark:text-slate-200">
            <thead className="border-b border-slate-200 bg-slate-50/75 text-[11px] font-bold uppercase tracking-wider text-slate-500 dark:border-slate-800 dark:bg-slate-850 dark:text-slate-400">
              <tr>
                <th className="px-5 py-3.5">Member Name / Contact</th>
                <th className="px-5 py-3.5">Assigned Roles</th>
                <th className="px-5 py-3.5">Feature Grants</th>
                <th className="px-5 py-3.5">Account Status</th>
                <th className="px-5 py-3.5">Registered</th>
                <th className="no-print px-5 py-3.5 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
              {filteredUsers.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-8 text-center text-slate-400">
                    No matching users found.
                  </td>
                </tr>
              ) : (
                filteredUsers.map((user) => (
                  <tr
                    key={user.id}
                    className="transition hover:bg-slate-50/70 dark:hover:bg-slate-800/50"
                  >
                    <td className="px-5 py-4">
                      <div className="flex items-center gap-3">
                        <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-xl bg-slate-100 font-bold text-slate-700 dark:bg-slate-800 dark:text-slate-300">
                          {user.full_name ? user.full_name.charAt(0).toUpperCase() : 'U'}
                        </div>
                        <div>
                          <div className="font-bold text-slate-900 dark:text-white">{user.full_name}</div>
                          <div className="text-[11px] text-slate-500 dark:text-slate-400">
                            {user.email || 'No email registered'}
                          </div>
                          {user.phone && (
                            <div className="text-[10px] text-slate-400">{user.phone}</div>
                          )}
                        </div>
                      </div>
                    </td>

                    <td className="px-5 py-4">
                      <div className="flex flex-wrap gap-1">
                        {user.roles.map((r) => (
                          <StatusBadge key={r} status={r} />
                        ))}
                      </div>
                    </td>

                    <td className="px-5 py-4">
                      <div className="flex flex-wrap gap-1 max-w-xs">
                        {user.feature_permissions.length > 0 ? (
                          user.feature_permissions.map((f) => (
                            <span
                              key={f}
                              className="rounded bg-slate-100 px-1.5 py-0.5 text-[10px] font-medium text-slate-600 dark:bg-slate-800 dark:text-slate-300"
                            >
                              {f}
                            </span>
                          ))
                        ) : (
                          <span className="text-[11px] text-slate-400">Standard user access</span>
                        )}
                      </div>
                    </td>

                    <td className="px-5 py-4">
                      <StatusBadge status={user.status} />
                    </td>

                    <td className="px-5 py-4 text-slate-500 text-[11px] dark:text-slate-400">
                      {new Date(user.created_at).toLocaleDateString()}
                    </td>

                    <td className="no-print px-5 py-4 text-right">
                      <div className="inline-flex items-center gap-1">
                        <button
                          type="button"
                          onClick={() => openEdit(user)}
                          className="rounded-lg p-1.5 text-slate-500 hover:bg-slate-100 hover:text-slate-900 dark:text-slate-400 dark:hover:bg-slate-800 dark:hover:text-white transition"
                          title="Edit member"
                        >
                          <Pencil className="h-4 w-4" />
                        </button>
                        <button
                          type="button"
                          onClick={() => setDeletingUser(user)}
                          className="rounded-lg p-1.5 text-rose-500 hover:bg-rose-50 hover:text-rose-700 dark:hover:bg-rose-950/60 dark:hover:text-rose-400 transition"
                          title="Delete member"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Create / Edit Drawer Modal */}
      {(isCreating || editingUser) && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm animate-fade-in">
          <div className="w-full max-w-2xl max-h-[90vh] overflow-y-auto rounded-3xl border border-slate-200 bg-white p-6 shadow-2xl dark:border-slate-800 dark:bg-slate-900">
            <div className="flex items-center justify-between border-b border-slate-100 pb-4 dark:border-slate-800">
              <div>
                <h3 className="text-base font-bold text-slate-900 dark:text-white">
                  {editingUser ? 'Edit Member & Permissions' : 'Register New Member'}
                </h3>
                <p className="text-xs text-slate-500 dark:text-slate-400">
                  {editingUser ? `Updating access for ${editingUser.full_name}` : 'Create account with custom roles'}
                </p>
              </div>
              <button
                onClick={closeForm}
                className="rounded-lg p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-600 dark:hover:bg-slate-800"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <form onSubmit={saveUser} className="mt-5 space-y-4">
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div>
                  <label className="block text-xs font-semibold uppercase text-slate-600 dark:text-slate-300">
                    Full Name
                  </label>
                  <input
                    type="text"
                    required
                    value={form.full_name}
                    onChange={(e) => setForm({ ...form, full_name: e.target.value })}
                    className="mt-1.5 w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs text-slate-900 outline-none focus:border-navy-900 dark:border-slate-700 dark:bg-slate-800 dark:text-white dark:focus:border-sky-400"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold uppercase text-slate-600 dark:text-slate-300">
                    Email Address
                  </label>
                  <input
                    type="email"
                    required
                    value={form.email}
                    onChange={(e) => setForm({ ...form, email: e.target.value })}
                    className="mt-1.5 w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs text-slate-900 outline-none focus:border-navy-900 dark:border-slate-700 dark:bg-slate-800 dark:text-white dark:focus:border-sky-400"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div>
                  <label className="block text-xs font-semibold uppercase text-slate-600 dark:text-slate-300">
                    Phone Number
                  </label>
                  <input
                    type="tel"
                    placeholder="+91..."
                    value={form.phone}
                    onChange={(e) => setForm({ ...form, phone: e.target.value })}
                    className="mt-1.5 w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs text-slate-900 outline-none focus:border-navy-900 dark:border-slate-700 dark:bg-slate-800 dark:text-white dark:focus:border-sky-400"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold uppercase text-slate-600 dark:text-slate-300">
                    Account Status
                  </label>
                  <select
                    value={form.status}
                    onChange={(e) => setForm({ ...form, status: e.target.value as any })}
                    className="mt-1.5 w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs text-slate-900 outline-none focus:border-navy-900 dark:border-slate-700 dark:bg-slate-800 dark:text-white"
                  >
                    <option value="ACTIVE">ACTIVE</option>
                    <option value="SUSPENDED">SUSPENDED</option>
                  </select>
                </div>
              </div>

              {/* Roles selection */}
              <div>
                <label className="block text-xs font-semibold uppercase text-slate-600 dark:text-slate-300 mb-2">
                  Assigned Roles
                </label>
                <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
                  {roleOptions.map((role) => {
                    const isChecked = form.roles.includes(role.value);
                    return (
                      <button
                        type="button"
                        key={role.value}
                        onClick={() => toggleRole(role.value)}
                        className={`flex items-start gap-2.5 rounded-xl border p-3 text-left transition ${
                          isChecked
                            ? 'border-navy-900 bg-navy-50/60 dark:border-sky-400 dark:bg-sky-950/40'
                            : 'border-slate-200 bg-white hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-800 dark:hover:bg-slate-750'
                        }`}
                      >
                        <div
                          className={`mt-0.5 flex h-4 w-4 shrink-0 items-center justify-center rounded border ${
                            isChecked
                              ? 'border-navy-900 bg-navy-900 text-white dark:border-sky-400 dark:bg-sky-400 dark:text-slate-950'
                              : 'border-slate-300 bg-white dark:border-slate-600 dark:bg-slate-700'
                          }`}
                        >
                          {isChecked && <CheckCircle2 className="h-3 w-3" />}
                        </div>
                        <div>
                          <div className="text-xs font-bold text-slate-900 dark:text-white">
                            {role.label} <span className="font-normal text-slate-400">({role.labelGu})</span>
                          </div>
                          <div className="text-[11px] text-slate-500 dark:text-slate-400">
                            {role.description}
                          </div>
                        </div>
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* Feature grants */}
              <div>
                <label className="block text-xs font-semibold uppercase text-slate-600 dark:text-slate-300 mb-2">
                  Granular Feature Grants
                </label>
                <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
                  {featureOptions.map((feature) => {
                    const isChecked = form.feature_permissions.includes(feature.value);
                    return (
                      <button
                        type="button"
                        key={feature.value}
                        onClick={() => toggleFeature(feature.value)}
                        className={`rounded-xl border p-2 text-center text-xs font-semibold transition ${
                          isChecked
                            ? 'border-sky-500 bg-sky-50 text-sky-800 dark:border-sky-400 dark:bg-sky-950/60 dark:text-sky-300'
                            : 'border-slate-200 bg-white text-slate-600 hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-400'
                        }`}
                      >
                        {feature.label}
                      </button>
                    );
                  })}
                </div>
              </div>

              <div className="mt-6 flex items-center justify-end gap-3 border-t border-slate-100 pt-4 dark:border-slate-800">
                <button
                  type="button"
                  onClick={closeForm}
                  disabled={isSaving}
                  className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isSaving}
                  className="inline-flex items-center gap-2 rounded-xl bg-navy-900 px-5 py-2 text-xs font-semibold text-white shadow-sm transition hover:bg-navy-800 disabled:opacity-50 dark:bg-sky-600 dark:hover:bg-sky-500"
                >
                  {isSaving ? 'Saving…' : editingUser ? 'Update Member' : 'Register Member'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Confirmation Modal for Delete */}
      <ConfirmationModal
        isOpen={Boolean(deletingUser)}
        onClose={() => setDeletingUser(null)}
        onConfirm={deleteUser}
        title="Delete Member Account"
        description={`Are you sure you want to delete ${deletingUser?.full_name}? This action revokes all system and portal access.`}
        confirmText="Delete Account"
        isDestructive
        isLoading={isSaving}
      />
    </div>
  );
}
