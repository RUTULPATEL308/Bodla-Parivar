'use client';

import React, { useState } from 'react';
import { History, ShieldAlert } from 'lucide-react';
import { AuditLog } from '@/lib/types';

export default function AdminAuditLogsPage() {
  const [logs] = useState<AuditLog[]>([
    {
      id: 'log_1',
      action: 'ADMIN_APPROVED_OFFERING',
      entity_type: 'OFFERING',
      entity_id: 'off_1',
      old_data: { status: 'PENDING_APPROVAL' },
      new_data: { status: 'APPROVED' },
      created_at: '2026-09-28 14:15:00'
    },
    {
      id: 'log_2',
      action: 'ADMIN_PUBLISHED_NOTICE',
      entity_type: 'NOTICE',
      entity_id: 'not_1',
      old_data: { status: 'DRAFT' },
      new_data: { status: 'PUBLISHED', is_pinned: true },
      created_at: '2026-09-28 11:30:00'
    },
    {
      id: 'log_3',
      action: 'ADMIN_VERIFIED_BUSINESS',
      entity_type: 'BUSINESS',
      entity_id: 'biz_1',
      old_data: { is_verified: false },
      new_data: { is_verified: true, status: 'APPROVED' },
      created_at: '2026-09-27 16:45:00'
    },
    {
      id: 'log_4',
      action: 'ADMIN_RESOLVED_COMPLAINT',
      entity_type: 'COMPLAINT',
      entity_id: 'comp_1',
      old_data: { status: 'IN_PROGRESS' },
      new_data: { status: 'RESOLVED', resolution_note: 'Completed' },
      created_at: '2026-09-27 09:12:00'
    }
  ]);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-brand-text font-gujarati flex items-center gap-2">
          <History className="w-7 h-7 text-saffron" />
          <span>ઓડિટ લોગ્સ (Immutable Administrative Audit Trail)</span>
        </h1>
        <p className="text-xs text-brand-muted mt-1">
          Cryptographically recorded actions taken by administrators and moderators
        </p>
      </div>

      <div className="bg-cream-surface border border-brand-border rounded-2xl overflow-hidden shadow-sm">
        <table className="w-full text-left text-sm text-brand-text">
          <thead className="bg-cream text-xs text-brand-muted uppercase border-b border-brand-border">
            <tr>
              <th className="py-3 px-4">ક્રિયા / Action</th>
              <th className="py-3 px-4">પ્રકાર / Entity</th>
              <th className="py-3 px-4">ફેરફાર / Changes</th>
              <th className="py-3 px-4 text-right">સમય / Timestamp</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-brand-border font-mono text-xs">
            {logs.map(log => (
              <tr key={log.id} className="hover:bg-cream/40 transition">
                <td className="py-3.5 px-4 font-bold text-saffron">{log.action}</td>
                <td className="py-3.5 px-4 text-brand-text font-semibold">{log.entity_type}</td>
                <td className="py-3.5 px-4 text-brand-muted">
                  <span className="text-rose-600 font-semibold">{JSON.stringify(log.old_data)}</span>
                  <span className="mx-2 text-stone-400">→</span>
                  <span className="text-emerald-700 font-semibold">{JSON.stringify(log.new_data)}</span>
                </td>
                <td className="py-3.5 px-4 text-right text-brand-muted">{log.created_at}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
