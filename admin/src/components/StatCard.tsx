import React from 'react';

interface StatCardProps {
  title: string;
  titleGu?: string;
  value: string | number;
  icon: React.ElementType;
  trend?: string;
  trendDirection?: 'up' | 'down' | 'neutral';
  color?: string;
  subtitle?: string;
}

export default function StatCard({
  title,
  titleGu,
  value,
  icon: Icon,
  trend,
  trendDirection = 'up',
  color = 'text-sky-600 dark:text-sky-400',
  subtitle
}: StatCardProps) {
  return (
    <div className="group relative overflow-hidden rounded-2xl border border-slate-200/80 bg-white p-5 shadow-sm transition-all duration-300 hover:-translate-y-0.5 hover:shadow-md dark:border-slate-800 dark:bg-slate-900">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs font-semibold tracking-wide text-slate-500 uppercase dark:text-slate-400">{title}</p>
          {titleGu && <p className="text-[11px] font-medium text-slate-400 dark:text-slate-500">{titleGu}</p>}
        </div>
        <div className={`flex h-11 w-11 items-center justify-center rounded-xl bg-slate-100 transition-colors group-hover:bg-slate-200/80 dark:bg-slate-800 dark:group-hover:bg-slate-700/80 ${color}`}>
          <Icon className="h-5 w-5" />
        </div>
      </div>
      <div className="mt-4 flex items-baseline justify-between">
        <h3 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-white">{value}</h3>
        {trend && (
          <span
            className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-semibold ${
              trendDirection === 'down'
                ? 'bg-rose-50 text-rose-700 dark:bg-rose-950/60 dark:text-rose-400'
                : 'bg-emerald-50 text-emerald-700 dark:bg-emerald-950/60 dark:text-emerald-400'
            }`}
          >
            {trend}
          </span>
        )}
      </div>
      {subtitle && (
        <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">{subtitle}</p>
      )}
    </div>
  );
}
