'use client';

import React, { useState } from 'react';
import StatusBadge from '@/components/StatusBadge';
import { AlertCircle, CheckCircle, Clock } from 'lucide-react';
import { Complaint } from '@/lib/types';

export default function AdminComplaintsPage() {
  const [complaints, setComplaints] = useState<Complaint[]>([
    {
      id: 'comp_1',
      user_id: 'usr_1',
      title: '[DEMO DATA] પંચાયત રોડ પર શેરી લાઈટ બંધ છે',
      description: 'છેલ્લા બે દિવસથી પંચાયત રોડ પર લાઈટ બંધ હોવાથી અંધારું રહે છે.',
      status: 'IN_PROGRESS',
      priority: 'NORMAL',
      resolution_note: 'લાઈટમેનને સોંપવામાં આવ્યું છે. સાંજ સુધીમાં ચાલુ થશે.',
      created_at: '2026-09-26'
    },
    {
      id: 'comp_2',
      user_id: 'usr_2',
      title: '[DEMO DATA] તળાવ પાસે કચરાના ઢગલાની સફાઈ બાબત',
      description: 'તળાવ પાસે કચરો જમા થવાથી દુર્ગંધ આવે છે.',
      status: 'SUBMITTED',
      priority: 'HIGH',
      created_at: '2026-09-28'
    }
  ]);

  const updateStatus = (id: string, newStatus: Complaint['status']) => {
    setComplaints(prev =>
      prev.map(c => (c.id === id ? { ...c, status: newStatus } : c))
    );
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-brand-text font-gujarati flex items-center gap-2">
          <AlertCircle className="w-7 h-7 text-saffron" />
          <span>ફરિયાદ નિવારણ વ્યવસ્થાપન (Complaints Resolution)</span>
        </h1>
        <p className="text-xs text-brand-muted mt-1">
          Review, assign, track, and resolve citizen grievances with resolution notes
        </p>
      </div>

      <div className="bg-cream-surface border border-brand-border rounded-2xl overflow-hidden shadow-sm">
        <table className="w-full text-left text-sm text-brand-text">
          <thead className="bg-cream text-xs text-brand-muted uppercase border-b border-brand-border">
            <tr>
              <th className="py-3 px-4">ફરિયાદ / Grievance</th>
              <th className="py-3 px-4">પ્રાથમિકતા / Priority</th>
              <th className="py-3 px-4">સ્થિતિ / Status</th>
              <th className="py-3 px-4">તારીખ / Date</th>
              <th className="py-3 px-4 text-right">સ્થિતિ બદલો / Update</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-brand-border">
            {complaints.map(comp => (
              <tr key={comp.id} className="hover:bg-cream/40 transition">
                <td className="py-3.5 px-4 max-w-xs">
                  <div className="font-semibold text-brand-text">{comp.title}</div>
                  <div className="text-xs text-brand-muted truncate">{comp.description}</div>
                  {comp.resolution_note && (
                    <div className="mt-1 text-xs text-leaf font-medium">
                      નોંધ: {comp.resolution_note}
                    </div>
                  )}
                </td>
                <td className="py-3.5 px-4">
                  <span className={`text-xs font-bold px-2 py-0.5 rounded ${
                    comp.priority === 'HIGH' || comp.priority === 'URGENT'
                      ? 'bg-rose-100 text-rose-700'
                      : 'bg-stone-100 text-stone-700'
                  }`}>
                    {comp.priority}
                  </span>
                </td>
                <td className="py-3.5 px-4">
                  <StatusBadge status={comp.status} />
                </td>
                <td className="py-3.5 px-4 text-xs text-brand-muted">{comp.created_at}</td>
                <td className="py-3.5 px-4 text-right">
                  <select
                    value={comp.status}
                    onChange={e => updateStatus(comp.id, e.target.value as Complaint['status'])}
                    className="px-2 py-1 text-xs rounded-lg border border-brand-border bg-white focus:outline-none focus:border-saffron"
                  >
                    <option value="SUBMITTED">SUBMITTED</option>
                    <option value="UNDER_REVIEW">UNDER_REVIEW</option>
                    <option value="IN_PROGRESS">IN_PROGRESS</option>
                    <option value="RESOLVED">RESOLVED</option>
                    <option value="REJECTED">REJECTED</option>
                  </select>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
