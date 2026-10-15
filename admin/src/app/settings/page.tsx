'use client';

import { FormEvent, useEffect, useState } from 'react';
import { LoaderCircle, Save, Settings } from 'lucide-react';
import { supabase } from '@/lib/supabase';

type VillageSettings = {
  id?: string;
  village_name_gu: string;
  village_name_en: string;
  district: string;
  state: string;
  country: string;
  description_gu: string;
  description_en: string;
  contact_phone: string;
  contact_email: string;
  latitude: number | '';
  longitude: number | '';
};

const emptySettings: VillageSettings = {
  village_name_gu: '', village_name_en: '', district: '', state: '', country: 'India',
  description_gu: '', description_en: '', contact_phone: '', contact_email: '', latitude: '', longitude: '',
};

export default function AdminSettingsPage() {
  const [settings, setSettings] = useState<VillageSettings>(emptySettings);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    let active = true;
    void supabase
      .from('village_settings')
      .select('*')
      .order('created_at', { ascending: true })
      .limit(1)
      .maybeSingle()
      .then(({ data, error: loadError }) => {
        if (!active) return;
        if (loadError) setError(loadError.message);
        else if (data) setSettings({ ...emptySettings, ...data });
        setLoading(false);
      });
    return () => { active = false; };
  }, []);

  const update = <K extends keyof VillageSettings>(key: K, value: VillageSettings[K]) => {
    setSettings((current) => ({ ...current, [key]: value }));
    setSaved(false);
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSaving(true);
    setSaved(false);
    setError('');
    const { id, ...formValues } = settings;
    const values = {
      ...formValues,
      latitude: formValues.latitude === '' ? null : formValues.latitude,
      longitude: formValues.longitude === '' ? null : formValues.longitude,
    };
    const result = id
      ? await supabase.from('village_settings').update(values).eq('id', id).select('*').single()
      : await supabase.from('village_settings').insert(values).select('*').single();
    if (result.error) setError(result.error.message);
    else {
      setSettings({ ...emptySettings, ...result.data });
      setSaved(true);
    }
    setSaving(false);
  };

  const textField = (label: string, key: keyof VillageSettings, required = false) => (
    <label className="block space-y-1.5 text-sm font-semibold text-brand-text">
      {label}
      <input
        type="text"
        value={String(settings[key] ?? '')}
        onChange={(event) => update(key, event.target.value as VillageSettings[typeof key])}
        className="w-full rounded-xl border border-brand-border bg-white px-3 py-2.5 text-base font-normal text-brand-text"
        required={required}
      />
    </label>
  );

  if (loading) return <p role="status" className="flex items-center gap-3 py-10 text-base text-brand-muted"><LoaderCircle className="h-5 w-5 animate-spin" /> Loading village settings…</p>;

  return (
    <main className="mx-auto max-w-4xl space-y-6">
      <header>
        <h1 className="flex items-center gap-3 text-2xl font-bold text-brand-text sm:text-3xl">
          <Settings aria-hidden="true" className="h-7 w-7 text-saffron" /> Village settings
        </h1>
        <p className="mt-2 text-base text-brand-muted">Edit the information residents see in the app and portal.</p>
      </header>

      {error && <p role="alert" className="rounded-xl border border-rose-200 bg-rose-50 p-4 text-base text-rose-800">{error}</p>}
      {saved && <p role="status" className="rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-base font-semibold text-emerald-800">Village settings saved.</p>}

      <form onSubmit={handleSubmit} className="space-y-6 rounded-3xl border border-brand-border bg-cream-surface p-5 shadow-sm sm:p-7">
        <div className="grid gap-5 sm:grid-cols-2">
          {textField('Village name (Gujarati)', 'village_name_gu', true)}
          {textField('Village name (English)', 'village_name_en', true)}
          {textField('District', 'district')}
          {textField('State', 'state')}
          {textField('Country', 'country')}
          {textField('Contact phone', 'contact_phone')}
          {textField('Contact email', 'contact_email')}
          <label className="block space-y-1.5 text-sm font-semibold text-brand-text">
            Latitude
            <input type="number" step="0.0000001" value={settings.latitude} onChange={(event) => update('latitude', event.target.value === '' ? '' : Number(event.target.value))} className="w-full rounded-xl border border-brand-border bg-white px-3 py-2.5 text-base font-normal" />
          </label>
          <label className="block space-y-1.5 text-sm font-semibold text-brand-text">
            Longitude
            <input type="number" step="0.0000001" value={settings.longitude} onChange={(event) => update('longitude', event.target.value === '' ? '' : Number(event.target.value))} className="w-full rounded-xl border border-brand-border bg-white px-3 py-2.5 text-base font-normal" />
          </label>
        </div>
        <label className="block space-y-1.5 text-sm font-semibold text-brand-text">
          Village description (Gujarati)
          <textarea rows={4} value={settings.description_gu} onChange={(event) => update('description_gu', event.target.value)} className="w-full rounded-xl border border-brand-border bg-white px-3 py-2.5 text-base font-normal" />
        </label>
        <label className="block space-y-1.5 text-sm font-semibold text-brand-text">
          Village description (English)
          <textarea rows={4} value={settings.description_en} onChange={(event) => update('description_en', event.target.value)} className="w-full rounded-xl border border-brand-border bg-white px-3 py-2.5 text-base font-normal" />
        </label>
        <button type="submit" disabled={saving} className="inline-flex min-h-14 w-full items-center justify-center gap-2 rounded-xl bg-saffron px-5 py-3 text-base font-semibold text-white shadow-sm hover:bg-saffron-dark disabled:cursor-wait disabled:opacity-60 sm:w-auto">
          {saving ? <LoaderCircle aria-hidden="true" className="h-5 w-5 animate-spin" /> : <Save aria-hidden="true" className="h-5 w-5" />}
          {saving ? 'Saving…' : 'Save settings'}
        </button>
      </form>
    </main>
  );
}
