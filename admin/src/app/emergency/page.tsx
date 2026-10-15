'use client';

import { FormEvent, useEffect, useState } from 'react';
import { LoaderCircle, PhoneCall, Plus, Save, ShieldCheck, Trash2, X } from 'lucide-react';
import { supabase } from '@/lib/supabase';
import { EmergencyContact } from '@/lib/types';

type ContactCategory = EmergencyContact['category'];
type ContactDraft = Omit<EmergencyContact, 'id' | 'is_active'> & { is_active: boolean };

const emptyDraft: ContactDraft = {
  name_gu: '', name_en: '', organization: '', phone: '', alternate_phone: '',
  category: 'HELPLINE', display_order: 0, is_active: true,
};

const categoryLabels: Record<ContactCategory, string> = {
  AMBULANCE: 'Ambulance', POLICE: 'Police', HOSPITAL: 'Hospital',
  FIRE: 'Fire service', GOVERNMENT: 'Government', HELPLINE: 'Helpline',
};

export default function AdminEmergencyPage() {
  const [contacts, setContacts] = useState<EmergencyContact[]>([]);
  const [draft, setDraft] = useState<ContactDraft>(emptyDraft);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [formOpen, setFormOpen] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  useEffect(() => {
    let active = true;
    void supabase
      .from('emergency_contacts')
      .select('*')
      .order('display_order', { ascending: true })
      .order('name_en', { ascending: true })
      .then(({ data, error: loadError }) => {
        if (!active) return;
        if (loadError) setError(`Could not load emergency contacts: ${loadError.message}`);
        else setContacts((data ?? []) as EmergencyContact[]);
        setLoading(false);
      });
    return () => { active = false; };
  }, []);

  const resetForm = () => {
    setDraft(emptyDraft);
    setEditingId(null);
    setFormOpen(false);
  };

  const startEdit = (contact: EmergencyContact) => {
    setDraft({
      name_gu: contact.name_gu,
      name_en: contact.name_en,
      organization: contact.organization ?? '',
      phone: contact.phone,
      alternate_phone: contact.alternate_phone ?? '',
      category: contact.category,
      display_order: contact.display_order ?? 0,
      is_active: contact.is_active ?? true,
    });
    setEditingId(contact.id);
    setFormOpen(true);
    setError('');
    setNotice('');
  };

  const saveContact = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSaving(true);
    setError('');
    setNotice('');
    const values = {
      ...draft,
      organization: draft.organization?.trim() || null,
      alternate_phone: draft.alternate_phone?.trim() || null,
      name_gu: draft.name_gu.trim(),
      name_en: draft.name_en.trim(),
      phone: draft.phone.trim(),
    };
    const result = editingId
      ? await supabase.from('emergency_contacts').update(values).eq('id', editingId).select('*').single()
      : await supabase.from('emergency_contacts').insert(values).select('*').single();

    if (result.error) {
      setError(`Could not save contact: ${result.error.message}`);
    } else {
      const savedContact = result.data as EmergencyContact;
      setContacts((current) => {
        const next = editingId
          ? current.map((contact) => contact.id === editingId ? savedContact : contact)
          : [...current, savedContact];
        return next.sort((a, b) => (a.display_order ?? 0) - (b.display_order ?? 0) || a.name_en.localeCompare(b.name_en));
      });
      setNotice(editingId ? 'Emergency contact updated.' : 'Emergency contact added.');
      resetForm();
    }
    setSaving(false);
  };

  const deleteContact = async (contact: EmergencyContact) => {
    if (!window.confirm(`Delete ${contact.name_en}? This removes it from the residents’ emergency list.`)) return;
    setError('');
    setNotice('');
    const { error: deleteError } = await supabase.from('emergency_contacts').delete().eq('id', contact.id);
    if (deleteError) setError(`Could not delete contact: ${deleteError.message}`);
    else {
      setContacts((current) => current.filter((item) => item.id !== contact.id));
      setNotice('Emergency contact deleted.');
      if (editingId === contact.id) resetForm();
    }
  };

  const toggleActive = async (contact: EmergencyContact) => {
    setError('');
    setNotice('');
    const isActive = !contact.is_active;
    const { data, error: updateError } = await supabase
      .from('emergency_contacts')
      .update({ is_active: isActive })
      .eq('id', contact.id)
      .select('*')
      .single();
    if (updateError) setError(`Could not update visibility: ${updateError.message}`);
    else {
      setContacts((current) => current.map((item) => item.id === contact.id ? data as EmergencyContact : item));
      setNotice(isActive ? 'Contact is visible to residents.' : 'Contact is hidden from residents.');
    }
  };

  const fieldClass = 'mt-1.5 min-h-12 w-full rounded-xl border border-brand-border bg-white px-3 py-2.5 text-base font-normal text-brand-text focus:border-saffron focus:outline-none focus:ring-2 focus:ring-saffron/30';

  return (
    <main className="mx-auto max-w-6xl space-y-6">
      <header className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="flex items-center gap-3 text-2xl font-bold text-brand-text sm:text-3xl">
            <PhoneCall aria-hidden="true" className="h-7 w-7 text-saffron" /> Emergency contacts
          </h1>
          <p className="mt-2 text-base text-brand-muted">Keep residents’ emergency numbers accurate and verified.</p>
        </div>
        <button
          type="button"
          onClick={() => { setDraft(emptyDraft); setEditingId(null); setFormOpen(true); setError(''); setNotice(''); }}
          className="inline-flex min-h-14 items-center justify-center gap-2 rounded-xl bg-saffron px-5 py-3 text-base font-semibold text-white shadow-sm hover:bg-saffron-dark focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-saffron"
        >
          <Plus aria-hidden="true" className="h-5 w-5" /> Add emergency contact
        </button>
      </header>

      {error && <p role="alert" className="rounded-xl border border-rose-200 bg-rose-50 p-4 text-base text-rose-800">{error}</p>}
      {notice && <p role="status" className="rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-base font-semibold text-emerald-800">{notice}</p>}

      {formOpen && (
        <form onSubmit={saveContact} className="space-y-5 rounded-2xl border border-brand-border bg-cream-surface p-5 shadow-sm sm:p-7" aria-labelledby="contact-form-title">
          <div className="flex items-center justify-between gap-3">
            <h2 id="contact-form-title" className="text-xl font-bold text-brand-text">{editingId ? 'Edit emergency contact' : 'Add emergency contact'}</h2>
            <button type="button" onClick={resetForm} aria-label="Close contact form" className="inline-flex h-11 w-11 items-center justify-center rounded-xl text-brand-muted hover:bg-cream focus-visible:outline focus-visible:outline-2 focus-visible:outline-saffron">
              <X aria-hidden="true" className="h-5 w-5" />
            </button>
          </div>
          <div className="grid gap-4 sm:grid-cols-2">
            <label className="text-sm font-semibold text-brand-text">Service name (Gujarati)
              <input required maxLength={160} value={draft.name_gu} onChange={(event) => setDraft({ ...draft, name_gu: event.target.value })} className={fieldClass} />
            </label>
            <label className="text-sm font-semibold text-brand-text">Service name (English)
              <input required maxLength={160} value={draft.name_en} onChange={(event) => setDraft({ ...draft, name_en: event.target.value })} className={fieldClass} />
            </label>
            <label className="text-sm font-semibold text-brand-text">Organization
              <input maxLength={160} value={draft.organization ?? ''} onChange={(event) => setDraft({ ...draft, organization: event.target.value })} className={fieldClass} />
            </label>
            <label className="text-sm font-semibold text-brand-text">Phone number
              <input required type="tel" inputMode="tel" autoComplete="tel" maxLength={32} value={draft.phone} onChange={(event) => setDraft({ ...draft, phone: event.target.value })} className={fieldClass} />
            </label>
            <label className="text-sm font-semibold text-brand-text">Alternate phone (optional)
              <input type="tel" inputMode="tel" maxLength={32} value={draft.alternate_phone ?? ''} onChange={(event) => setDraft({ ...draft, alternate_phone: event.target.value })} className={fieldClass} />
            </label>
            <label className="text-sm font-semibold text-brand-text">Category
              <select value={draft.category} onChange={(event) => setDraft({ ...draft, category: event.target.value as ContactCategory })} className={fieldClass}>
                {(Object.keys(categoryLabels) as ContactCategory[]).map((category) => <option key={category} value={category}>{categoryLabels[category]}</option>)}
              </select>
            </label>
            <label className="text-sm font-semibold text-brand-text">Display order
              <input type="number" min="0" step="1" value={draft.display_order} onChange={(event) => setDraft({ ...draft, display_order: Number(event.target.value) })} className={fieldClass} />
            </label>
            <label className="flex min-h-12 items-center gap-3 self-end rounded-xl border border-brand-border px-3 py-2 text-base font-semibold text-brand-text">
              <input type="checkbox" checked={draft.is_active} onChange={(event) => setDraft({ ...draft, is_active: event.target.checked })} className="h-5 w-5 accent-amber-600" />
              Visible to residents
            </label>
          </div>
          <div className="flex flex-col gap-3 sm:flex-row">
            <button type="submit" disabled={saving} className="inline-flex min-h-14 items-center justify-center gap-2 rounded-xl bg-saffron px-5 py-3 text-base font-semibold text-white hover:bg-saffron-dark disabled:opacity-60 sm:min-w-44">
              {saving ? <LoaderCircle aria-hidden="true" className="h-5 w-5 animate-spin" /> : <Save aria-hidden="true" className="h-5 w-5" />}
              {saving ? 'Saving…' : editingId ? 'Save changes' : 'Add contact'}
            </button>
            <button type="button" onClick={resetForm} className="min-h-14 rounded-xl border border-brand-border px-5 py-3 text-base font-semibold text-brand-text hover:bg-cream">Cancel</button>
          </div>
        </form>
      )}

      <section aria-label="Emergency contact list" className="overflow-hidden rounded-2xl border border-brand-border bg-white shadow-sm">
        {loading ? (
          <p role="status" className="flex items-center gap-3 p-6 text-base text-brand-muted"><LoaderCircle aria-hidden="true" className="h-5 w-5 animate-spin" /> Loading emergency contacts…</p>
        ) : contacts.length === 0 ? (
          <div className="p-8 text-center">
            <ShieldCheck aria-hidden="true" className="mx-auto h-10 w-10 text-brand-muted" />
            <p className="mt-3 text-lg font-semibold text-brand-text">No emergency contacts yet</p>
            <p className="mt-1 text-base text-brand-muted">Add verified numbers so residents can find help quickly.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[760px] text-left text-base text-brand-text">
              <thead className="bg-cream text-sm text-brand-muted">
                <tr>
                  <th scope="col" className="px-4 py-3">Service</th>
                  <th scope="col" className="px-4 py-3">Phone</th>
                  <th scope="col" className="px-4 py-3">Category</th>
                  <th scope="col" className="px-4 py-3">Visibility</th>
                  <th scope="col" className="px-4 py-3">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-brand-border">
                {contacts.map((contact) => (
                  <tr key={contact.id}>
                    <td className="px-4 py-4">
                      <p className="font-semibold">{contact.name_gu}</p>
                      <p className="text-sm text-brand-muted">{contact.name_en}{contact.organization ? ` · ${contact.organization}` : ''}</p>
                    </td>
                    <td className="px-4 py-4">
                      <a href={`tel:${contact.phone}`} className="font-semibold text-saffron underline decoration-saffron/40 underline-offset-2">{contact.phone}</a>
                      {contact.alternate_phone && <p className="mt-1 text-sm text-brand-muted">Alt: {contact.alternate_phone}</p>}
                    </td>
                    <td className="px-4 py-4">{categoryLabels[contact.category]}</td>
                    <td className="px-4 py-4">
                      <button type="button" onClick={() => void toggleActive(contact)} aria-pressed={contact.is_active} className={`inline-flex min-h-11 items-center gap-2 rounded-full px-3 text-sm font-semibold ${contact.is_active ? 'bg-emerald-50 text-emerald-800' : 'bg-slate-100 text-slate-700'}`}>
                        <ShieldCheck aria-hidden="true" className="h-4 w-4" /> {contact.is_active ? 'Visible' : 'Hidden'}
                      </button>
                    </td>
                    <td className="px-4 py-4">
                      <div className="flex items-center gap-2">
                        <button type="button" onClick={() => startEdit(contact)} className="min-h-11 rounded-lg border border-brand-border px-3 text-sm font-semibold hover:bg-cream">Edit</button>
                        <button type="button" onClick={() => void deleteContact(contact)} aria-label={`Delete ${contact.name_en}`} className="inline-flex min-h-11 min-w-11 items-center justify-center rounded-lg text-rose-700 hover:bg-rose-50 focus-visible:outline focus-visible:outline-2 focus-visible:outline-rose-600">
                          <Trash2 aria-hidden="true" className="h-5 w-5" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </main>
  );
}
