'use client';

import React, { useState } from 'react';
import { Settings, Save } from 'lucide-react';

export default function AdminSettingsPage() {
  const [settings, setSettings] = useState({
    village_name_gu: 'બોદલા',
    village_name_en: 'Bodla',
    district: 'Mehsana',
    state: 'Gujarat',
    country: 'India',
    description_gu: 'બોદલા એ ગુજરાત રાજ્યના મહેસાણા જિલ્લામાં આવેલું એક ગૌરવશાળી અને આદર્શ ગામ છે.',
    description_en: 'Bodla is a distinguished village located in the Mehsana district of Gujarat, India.',
    contact_phone: '+91 2762 000000',
    contact_email: 'panchayat@bodlaparivar.in',
    latitude: 23.5880,
    longitude: 72.3693
  });

  const [saved, setSaved] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setSaved(true);
    setTimeout(() => setSaved(false), 3000);
  };

  return (
    <div className="space-y-6 max-w-4xl">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-brand-text font-gujarati flex items-center gap-2">
            <Settings className="w-7 h-7 text-saffron" />
            <span>ગામ સેટિંગ્સ (Village Configuration)</span>
          </h1>
          <p className="text-xs text-brand-muted mt-1">
            Official village registry parameters for બોદલા (Bodla), Mehsana, Gujarat
          </p>
        </div>

        {saved && (
          <span className="text-xs font-semibold text-leaf bg-emerald-50 px-3 py-1.5 rounded-lg border border-emerald-200">
            ✓ સેટિંગ્સ સેવ થઈ ગઈ છે (Saved)
          </span>
        )}
      </div>

      <form onSubmit={handleSubmit} className="bg-cream-surface border border-brand-border rounded-2xl p-6 shadow-sm space-y-6">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-xs font-bold text-brand-text mb-1">ગામનું નામ (ગુજરાતી)</label>
            <input
              type="text"
              value={settings.village_name_gu}
              onChange={e => setSettings({ ...settings, village_name_gu: e.target.value })}
              className="w-full px-3 py-2 rounded-xl border border-brand-border bg-white text-sm"
              required
            />
          </div>

          <div>
            <label className="block text-xs font-bold text-brand-text mb-1">Village Name (English)</label>
            <input
              type="text"
              value={settings.village_name_en}
              onChange={e => setSettings({ ...settings, village_name_en: e.target.value })}
              className="w-full px-3 py-2 rounded-xl border border-brand-border bg-white text-sm"
              required
            />
          </div>

          <div>
            <label className="block text-xs font-bold text-brand-text mb-1">જિલ્લો / District</label>
            <input
              type="text"
              value={settings.district}
              onChange={e => setSettings({ ...settings, district: e.target.value })}
              className="w-full px-3 py-2 rounded-xl border border-brand-border bg-white text-sm"
            />
          </div>

          <div>
            <label className="block text-xs font-bold text-brand-text mb-1">રાજ્ય / State</label>
            <input
              type="text"
              value={settings.state}
              onChange={e => setSettings({ ...settings, state: e.target.value })}
              className="w-full px-3 py-2 rounded-xl border border-brand-border bg-white text-sm"
            />
          </div>

          <div>
            <label className="block text-xs font-bold text-brand-text mb-1">અક્ષાંશ (Latitude)</label>
            <input
              type="number"
              step="0.0001"
              value={settings.latitude}
              onChange={e => setSettings({ ...settings, latitude: parseFloat(e.target.value) })}
              className="w-full px-3 py-2 rounded-xl border border-brand-border bg-white text-sm"
            />
          </div>

          <div>
            <label className="block text-xs font-bold text-brand-text mb-1">રેખાંશ (Longitude)</label>
            <input
              type="number"
              step="0.0001"
              value={settings.longitude}
              onChange={e => setSettings({ ...settings, longitude: parseFloat(e.target.value) })}
              className="w-full px-3 py-2 rounded-xl border border-brand-border bg-white text-sm"
            />
          </div>
        </div>

        <div>
          <label className="block text-xs font-bold text-brand-text mb-1">ગામ પરિચય (ગુજરાતી વર્ણન)</label>
          <textarea
            rows={3}
            value={settings.description_gu}
            onChange={e => setSettings({ ...settings, description_gu: e.target.value })}
            className="w-full px-3 py-2 rounded-xl border border-brand-border bg-white text-sm"
          />
        </div>

        <div>
          <label className="block text-xs font-bold text-brand-text mb-1">Village Overview (English Description)</label>
          <textarea
            rows={3}
            value={settings.description_en}
            onChange={e => setSettings({ ...settings, description_en: e.target.value })}
            className="w-full px-3 py-2 rounded-xl border border-brand-border bg-white text-sm"
          />
        </div>

        <button
          type="submit"
          className="px-6 py-2.5 rounded-xl bg-saffron text-white text-sm font-semibold hover:bg-saffron-dark transition shadow-sm flex items-center gap-2"
        >
          <Save className="w-4 h-4" />
          <span>સેટિંગ્સ સાચવો (Save Settings)</span>
        </button>
      </form>
    </div>
  );
}
