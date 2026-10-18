import React from 'react';
import { Inbox, SearchX, AlertCircle } from 'lucide-react';

interface EmptyStateProps {
  title: string;
  description: string;
  actionText?: string;
  onAction?: () => void;
  icon?: React.ElementType;
}

export default function EmptyState({
  title,
  description,
  actionText,
  onAction,
  icon: Icon = Inbox,
}: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-slate-300 bg-slate-50/50 p-8 text-center dark:border-slate-800 dark:bg-slate-900/50">
      <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-slate-100 text-slate-500 shadow-inner dark:bg-slate-800 dark:text-slate-400">
        <Icon className="h-6 w-6" />
      </div>
      <h3 className="mt-4 text-base font-bold text-slate-900 dark:text-white">{title}</h3>
      <p className="mt-1.5 max-w-sm text-xs text-slate-500 dark:text-slate-400 leading-relaxed">
        {description}
      </p>
      {actionText && onAction && (
        <button
          type="button"
          onClick={onAction}
          className="mt-5 inline-flex items-center gap-2 rounded-xl bg-navy-900 px-4 py-2 text-xs font-semibold text-white shadow-sm transition hover:bg-navy-800 dark:bg-sky-600 dark:hover:bg-sky-500"
        >
          {actionText}
        </button>
      )}
    </div>
  );
}
