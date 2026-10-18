'use client';

import React from 'react';

interface MetricBarProps {
  label: string;
  count: number;
  total: number;
  colorClass: string;
  labelGu?: string;
}

export function MetricBar({ label, count, total, colorClass, labelGu }: MetricBarProps) {
  const percentage = total > 0 ? Math.round((count / total) * 100) : 0;

  return (
    <div className="space-y-1.5">
      <div className="flex items-center justify-between text-xs">
        <span className="font-semibold text-slate-700 dark:text-slate-300">
          {label} {labelGu && <span className="text-[11px] font-normal text-slate-400">({labelGu})</span>}
        </span>
        <span className="font-bold text-slate-900 dark:text-white">
          {count} <span className="font-normal text-slate-400">({percentage}%)</span>
        </span>
      </div>
      <div className="h-2 w-full overflow-hidden rounded-full bg-slate-100 dark:bg-slate-800">
        <div
          className={`h-full rounded-full transition-all duration-700 ${colorClass}`}
          style={{ width: `${percentage}%` }}
        />
      </div>
    </div>
  );
}

interface ActivityItem {
  id: string;
  user: string;
  action: string;
  time: string;
  role?: string;
}

export function MiniActivityFeed({ items }: { items: ActivityItem[] }) {
  return (
    <div className="divide-y divide-slate-100 dark:divide-slate-800">
      {items.map((item) => (
        <div key={item.id} className="flex items-start justify-between gap-3 py-3 text-xs">
          <div>
            <span className="font-semibold text-slate-800 dark:text-slate-200">{item.user}</span>{' '}
            <span className="text-slate-500 dark:text-slate-400">{item.action}</span>
          </div>
          <span className="shrink-0 font-medium text-slate-400 dark:text-slate-500">{item.time}</span>
        </div>
      ))}
    </div>
  );
}
