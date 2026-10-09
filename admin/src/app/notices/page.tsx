'use client';

import React, { useState } from 'react';
import StatusBadge from '@/components/StatusBadge';
import { Bell, Plus, Pin, Calendar } from 'lucide-react';
import { Notice } from '@/lib/types';

export default function AdminNoticesPage() {
  const [notices, setNotices] = useState<Notice[]>([
    {
      id: 'not_1',
      title_gu: '[DEMO DATA] ગ્રામ પંચાયત સામાન્ય સભાનું આયોજન',
      title_en: '[DEMO DATA] Gram Panchayat General Meeting Scheduled',
      description_gu: 'ગામના વિકાસકાર્યો અંગે ચર્ચા કરવા સામાન્ય સભા મળશે.',
      description_en: 'General meeting to discuss village development projects.',
      status: 'PUBLISHED',
      is_pinned: true,
      publish_at: '2026-09-28',
      created_at: '2026-09-28'
    },
    {
      id: 'not_2',
      title_gu: '[DEMO DATA] પીવાના પાણીના વિતરણ સમયમાં ફેરફાર',
      title_en: '[DEMO DATA] Drinking Water Distribution Timetable',
      description_gu: 'પાઇપલાઇન સમારકામના કારણે આવતીકાલે સમય સવારે ૬ થી ૮ રહેશે.',
      description_en: 'Due to pipeline repair, water supply will run 6am to 8am.',
      status: 'PUBLISHED',
      is_pinned: false,
      publish_at: '2026-09-27',
      created_at: '2026-09-27'
    }
  ]);

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-brand-text font-gujarati flex items-center gap-2">
            <Bell className="w-7 h-7 text-saffron" />
            <span>ગ્રામ સૂચનાઓ વ્યવસ્થાપન (Notices Management)</span>
          </h1>
          <p className="text-xs text-brand-muted mt-1">
            Create, publish, pin, and archive village notices in Gujarati & English
          </p>
        </div>

        <button
          onClick={() => alert('New Notice Modal')}
          className="px-4 py-2 rounded-xl bg-saffron text-white text-sm font-semibold hover:bg-saffron-dark transition shadow-sm flex items-center gap-2"
        >
          <Plus className="w-4 h-4" />
          <span>નવી સૂચના બનાવો</span>
        </button>
      </div>

      <div className="bg-cream-surface border border-brand-border rounded-2xl overflow-hidden shadow-sm">
        <table className="w-full text-left text-sm text-brand-text">
          <thead className="bg-cream text-xs text-brand-muted uppercase border-b border-brand-border">
            <tr>
              <th className="py-3 px-4">સૂચનાનું શીર્ષક / Title</th>
              <th className="py-3 px-4">સ્થિતિ / Status</th>
              <th className="py-3 px-4">પિન કરેલ / Pinned</th>
              <th className="py-3 px-4">પ્રકાશન તારીખ / Date</th>
              <th className="py-3 px-4 text-right">સંચાલન / Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-brand-border">
            {notices.map(notice => (
              <tr key={notice.id} className="hover:bg-cream/40 transition">
                <td className="py-3.5 px-4">
                  <div className="font-semibold text-brand-text flex items-center gap-2">
                    {notice.is_pinned && <Pin className="w-3.5 h-3.5 text-saffron fill-saffron" />}
                    <span>{notice.title_gu}</span>
                  </div>
                  <div className="text-xs text-brand-muted">{notice.title_en}</div>
                </td>
                <td className="py-3.5 px-4">
                  <StatusBadge status={notice.status} />
                </td>
                <td className="py-3.5 px-4 text-xs font-semibold">
                  {notice.is_pinned ? (
                    <span className="text-saffron">હા (Pinned)</span>
                  ) : (
                    <span className="text-brand-muted">ના</span>
                  )}
                </td>
                <td className="py-3.5 px-4 text-xs text-brand-muted">{notice.publish_at}</td>
                <td className="py-3.5 px-4 text-right">
                  <div className="flex items-center justify-end gap-2">
                    <button
                      onClick={() => alert(`Editing ${notice.title_en}`)}
                      className="px-2.5 py-1 rounded-lg border border-brand-border text-xs font-semibold text-brand-text hover:bg-saffron-container"
                    >
                      સુધારો
                    </button>
                    <button
                      onClick={() => alert(`Archived ${notice.title_en}`)}
                      className="px-2.5 py-1 rounded-lg border border-brand-border text-xs font-semibold text-rose-600 hover:bg-rose-50"
                    >
                      આર્કાઇવ
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
