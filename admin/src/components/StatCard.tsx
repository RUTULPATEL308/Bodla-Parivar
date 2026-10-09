import React from 'react';

interface StatCardProps {
  title: string;
  titleGu: string;
  value: string | number;
  icon: React.ElementType;
  trend?: string;
  color?: string;
}

export default function StatCard({
  title,
  titleGu,
  value,
  icon: Icon,
  trend,
  color = 'text-saffron'
}: StatCardProps) {
  return (
    <div className="bg-cream-surface border border-brand-border rounded-2xl p-5 shadow-sm hover:shadow-md transition">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs font-medium text-brand-muted">{title}</p>
          <p className="text-[11px] text-brand-muted/80">{titleGu}</p>
        </div>
        <div className="w-10 h-10 rounded-xl bg-saffron-container flex items-center justify-center text-saffron">
          <Icon className="w-5 h-5" />
        </div>
      </div>
      <div className="mt-4 flex items-baseline justify-between">
        <h3 className="text-2xl font-bold text-brand-text">{value}</h3>
        {trend && (
          <span className="text-xs font-semibold text-leaf">
            {trend}
          </span>
        )}
      </div>
    </div>
  );
}
