'use client';

import React, { useState } from 'react';
import { PhoneCall, ShieldCheck, Plus } from 'lucide-react';
import { EmergencyContact } from '@/lib/types';

export default function AdminEmergencyPage() {
  const [contacts] = useState<EmergencyContact[]>([
    {
      id: 'em_1',
      name_gu: 'ઈમરજન્સી એમ્બ્યુલન્સ સેવા',
      name_en: 'Emergency Ambulance',
      organization: '108 GVK EMRI',
      phone: '108',
      category: 'AMBULANCE',
      display_order: 1,
      is_active: true
    },
    {
      id: 'em_2',
      name_gu: 'પોલીસ કંટ્રોલ રૂમ',
      name_en: 'Police Control Room',
      organization: 'Gujarat Police',
      phone: '112',
      alternate_phone: '100',
      category: 'POLICE',
      display_order: 2,
      is_active: true
    },
    {
      id: 'em_3',
      name_gu: 'ફાયર બ્રિગેડ સેવા',
      name_en: 'Fire Services',
      organization: 'Fire Department Mehsana',
      phone: '101',
      category: 'FIRE',
      display_order: 3,
      is_active: true
    },
    {
      id: 'em_4',
      name_gu: 'અભયમ મહિલા હેલ્પલાઇન',
      name_en: 'Abhayam Women Helpline',
      organization: 'Abhayam 181',
      phone: '181',
      category: 'HELPLINE',
      display_order: 4,
      is_active: true
    }
  ]);

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-brand-text font-gujarati flex items-center gap-2">
            <PhoneCall className="w-7 h-7 text-saffron" />
            <span>તાત્કાલિક સહાય સંપર્કો (Emergency Contacts)</span>
          </h1>
          <p className="text-xs text-brand-muted mt-1">
            Strictly verified and admin-vetted public emergency helplines for Bodla village
          </p>
        </div>

        <button
          onClick={() => alert('Add Helpline Modal')}
          className="px-4 py-2 rounded-xl bg-saffron text-white text-sm font-semibold hover:bg-saffron-dark transition shadow-sm flex items-center gap-2"
        >
          <Plus className="w-4 h-4" />
          <span>નંબર ઉમેરો</span>
        </button>
      </div>

      <div className="bg-cream-surface border border-brand-border rounded-2xl overflow-hidden shadow-sm">
        <table className="w-full text-left text-sm text-brand-text">
          <thead className="bg-cream text-xs text-brand-muted uppercase border-b border-brand-border">
            <tr>
              <th className="py-3 px-4">સેવાનું નામ / Service</th>
              <th className="py-3 px-4">સંસ્થા / Organization</th>
              <th className="py-3 px-4">નંબર / Phone</th>
              <th className="py-3 px-4">કેટેગરી / Category</th>
              <th className="py-3 px-4">ચકાસાયેલ / Status</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-brand-border">
            {contacts.map(c => (
              <tr key={c.id} className="hover:bg-cream/40 transition">
                <td className="py-3.5 px-4">
                  <div className="font-semibold text-brand-text">{c.name_gu}</div>
                  <div className="text-xs text-brand-muted">{c.name_en}</div>
                </td>
                <td className="py-3.5 px-4 text-xs text-brand-muted">{c.organization}</td>
                <td className="py-3.5 px-4 font-mono font-bold text-rose-600">{c.phone}</td>
                <td className="py-3.5 px-4 text-xs font-semibold text-saffron">{c.category}</td>
                <td className="py-3.5 px-4">
                  <span className="inline-flex items-center gap-1 text-xs text-emerald-700 font-semibold bg-emerald-50 px-2 py-0.5 rounded-full border border-emerald-200">
                    <ShieldCheck className="w-3.5 h-3.5" />
                    <span>Verified</span>
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
