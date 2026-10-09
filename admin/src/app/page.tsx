'use client';

import React, { useState } from 'react';
import {
  Users,
  CheckCircle,
  Building2,
  Briefcase,
  HeartHandshake,
  AlertCircle,
  Calendar,
  Clock,
  ArrowRight
} from 'lucide-react';
import StatCard from '@/components/StatCard';
import StatusBadge from '@/components/StatusBadge';
import Link from 'next/link';

export default function AdminDashboard() {
  const [selectedFilter, setSelectedFilter] = useState('ALL');

  // Dashboard Metrics
  const stats = [
    { title: 'Total Members', titleGu: 'કુલ સભ્યો', value: '452', icon: Users, trend: '+12 this week' },
    { title: 'Pending Approvals', titleGu: 'મંજૂરી માટે બાકી', value: '6', icon: Clock, trend: 'Action needed' },
    { title: 'Verified Businesses', titleGu: 'ચકાસાયેલ વ્યવસાયો', value: '28', icon: Building2, trend: '98% verified' },
    { title: 'Active Jobs', titleGu: 'સક્રિય નોકરીઓ', value: '9', icon: Briefcase },
    { title: 'Active Offerings', titleGu: 'સક્રિય ચઢાવો', value: '5', icon: HeartHandshake }, // ચઢાવો
    { title: 'Open Complaints', titleGu: 'ખુલ્લી ફરિયાદો', value: '3', icon: AlertCircle, trend: '2 under review' },
    { title: 'Upcoming Events', titleGu: 'આગામી કાર્યક્રમો', value: '4', icon: Calendar },
    { title: 'Published Notices', titleGu: 'પ્રકાશિત સૂચનાઓ', value: '18', icon: CheckCircle }
  ];

  // Demo Pending Approval Items (Businesses, Offerings, Jobs)
  const pendingApprovals = [
    {
      id: 'appr_1',
      type: 'OFFERING',
      titleGu: '[DEMO DATA] શ્રી મહાદેવ મંદિર ધ્વજારોહણ સેવા',
      titleEn: 'Shri Mahadev Temple Dhwajarohan Seva',
      submittedBy: 'હર્ષદભાઈ પટેલ',
      date: '2026-09-28',
      status: 'PENDING_APPROVAL',
      amount: '₹5,100'
    },
    {
      id: 'appr_2',
      type: 'BUSINESS',
      titleGu: '[DEMO DATA] કિસાન એગ્રો સાધનો & રિપેરિંગ',
      titleEn: 'Kisan Agro Equipment & Repairing',
      submittedBy: 'રમેશભાઈ ચૌધરી',
      date: '2026-09-27',
      status: 'PENDING_APPROVAL',
      amount: '—'
    },
    {
      id: 'appr_3',
      type: 'JOB',
      titleGu: '[DEMO DATA] ડ્રાઈવર & સહાયક જરૂર છે',
      titleEn: 'Driver & Assistant Required',
      submittedBy: 'સરદાર ટ્રાન્સપોર્ટ',
      date: '2026-09-27',
      status: 'PENDING',
      amount: '₹18,000/mo'
    }
  ];

  return (
    <div className="space-y-8">
      {/* Top Banner */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-cream-surface border border-brand-border rounded-2xl p-6 shadow-sm">
        <div>
          <div className="inline-block px-2.5 py-0.5 rounded-full bg-saffron-container text-saffron font-bold text-xs mb-2">
            બોદલા ગ્રામ પંચાયત & સમુદાય
          </div>
          <h1 className="text-2xl font-bold text-brand-text font-gujarati">
            બોદલા પરિવાર એડમિન ડેશબોર્ડ
          </h1>
          <p className="text-sm text-brand-muted mt-1">
            Village Community Platform Administration — Bodla, Mehsana, Gujarat
          </p>
        </div>

        <div className="flex flex-wrap gap-2">
          <Link
            href="/users"
            className="px-4 py-2 rounded-xl border border-brand-border bg-cream-surface text-brand-text text-sm font-semibold hover:bg-saffron-container hover:text-saffron transition"
          >
            <span>Registered users</span>
          </Link>
          <Link
            href="/offerings"
            className="px-4 py-2 rounded-xl border border-brand-border bg-cream-surface text-brand-text text-sm font-semibold hover:bg-saffron-container hover:text-saffron transition"
          >
            <span>Live bidding</span>
          </Link>
          <Link
            href="/notices"
            className="px-4 py-2 rounded-xl bg-saffron text-white text-sm font-semibold hover:bg-saffron-dark transition shadow-sm flex items-center gap-1.5"
          >
            <span>+ નવી સૂચના</span>
          </Link>
          <Link
            href="/events"
            className="px-4 py-2 rounded-xl border border-brand-border bg-cream-surface text-brand-text text-sm font-semibold hover:bg-saffron-container hover:text-saffron transition"
          >
            <span>+ નવો કાર્યક્રમ</span>
          </Link>
        </div>
      </div>

      {/* Metrics Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {stats.map((stat, i) => (
          <StatCard
            key={i}
            title={stat.title}
            titleGu={stat.titleGu}
            value={stat.value}
            icon={stat.icon}
            trend={stat.trend}
          />
        ))}
      </div>

      {/* Pending Approvals Table Section */}
      <div className="bg-cream-surface border border-brand-border rounded-2xl p-6 shadow-sm">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
          <div>
            <h2 className="text-lg font-bold text-brand-text font-gujarati">
              મંજૂરી માટે બાકી વિનંતીઓ (Pending Approvals)
            </h2>
            <p className="text-xs text-brand-muted">
              Review and moderate community submissions (Businesses, Offerings, Jobs)
            </p>
          </div>

          <div className="flex gap-2 text-xs">
            {['ALL', 'OFFERING', 'BUSINESS', 'JOB'].map((filter) => (
              <button
                key={filter}
                onClick={() => setSelectedFilter(filter)}
                className={`px-3 py-1.5 rounded-lg border font-medium transition ${
                  selectedFilter === filter
                    ? 'bg-saffron text-white border-saffron'
                    : 'bg-white border-brand-border text-brand-text hover:bg-saffron-container'
                }`}
              >
                {filter === 'ALL' ? 'તમામ (All)' : filter}
              </button>
            ))}
          </div>
        </div>

        {/* Responsive Table */}
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm text-brand-text">
            <thead className="bg-cream text-xs text-brand-muted uppercase border-y border-brand-border">
              <tr>
                <th className="py-3 px-4">પ્રકાર / Type</th>
                <th className="py-3 px-4">શીર્ષક / Title</th>
                <th className="py-3 px-4">સબમિટ કરનાર / Member</th>
                <th className="py-3 px-4">રકમ / Value</th>
                <th className="py-3 px-4">સ્થિતિ / Status</th>
                <th className="py-3 px-4 text-right">ક્રિયા / Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-brand-border">
              {pendingApprovals
                .filter((item) => selectedFilter === 'ALL' || item.type === selectedFilter)
                .map((row) => (
                  <tr key={row.id} className="hover:bg-cream/40 transition">
                    <td className="py-3.5 px-4 font-semibold text-xs text-saffron">
                      {row.type === 'OFFERING' ? 'ચઢાવો (Offering)' : row.type}
                    </td>
                    <td className="py-3.5 px-4">
                      <div className="font-medium text-brand-text">{row.titleGu}</div>
                      <div className="text-xs text-brand-muted">{row.titleEn}</div>
                    </td>
                    <td className="py-3.5 px-4 text-xs text-brand-muted">{row.submittedBy}</td>
                    <td className="py-3.5 px-4 text-xs font-semibold text-brand-text">{row.amount}</td>
                    <td className="py-3.5 px-4">
                      <StatusBadge status={row.status} />
                    </td>
                    <td className="py-3.5 px-4 text-right">
                      <div className="flex items-center justify-end gap-2">
                        <button
                          onClick={() => alert(`Approved ${row.titleEn}`)}
                          className="px-2.5 py-1 rounded-lg bg-leaf text-white text-xs font-semibold hover:bg-leaf-dark transition"
                        >
                          મંજૂર કરો
                        </button>
                        <button
                          onClick={() => alert(`Rejected ${row.titleEn}`)}
                          className="px-2.5 py-1 rounded-lg border border-brand-border text-rose-600 text-xs font-semibold hover:bg-rose-50 transition"
                        >
                          નામંજૂર
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
