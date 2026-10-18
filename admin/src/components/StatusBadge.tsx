import React from 'react';

interface StatusBadgeProps {
  status: string;
  className?: string;
}

export default function StatusBadge({ status, className = '' }: StatusBadgeProps) {
  const normalized = (status || '').toUpperCase();

  let styles = 'bg-slate-100 text-slate-700 border-slate-200 dark:bg-slate-800 dark:text-slate-300 dark:border-slate-700';

  if (['APPROVED', 'PUBLISHED', 'ACTIVE', 'RESOLVED', 'SUCCESS', 'COMPLETED'].includes(normalized)) {
    styles = 'bg-emerald-50 text-emerald-700 border-emerald-200/80 dark:bg-emerald-950/60 dark:text-emerald-300 dark:border-emerald-800/80';
  } else if (['PENDING', 'PENDING_APPROVAL', 'UNDER_REVIEW', 'DRAFT', 'IN_PROGRESS', 'ASSIGNED'].includes(normalized)) {
    styles = 'bg-amber-50 text-amber-700 border-amber-200/80 dark:bg-amber-950/60 dark:text-amber-300 dark:border-amber-800/80';
  } else if (['REJECTED', 'CANCELLED', 'EXPIRED', 'SUSPENDED', 'FAILED', 'DELETED'].includes(normalized)) {
    styles = 'bg-rose-50 text-rose-700 border-rose-200/80 dark:bg-rose-950/60 dark:text-rose-300 dark:border-rose-800/80';
  } else if (['SUPER_ADMIN', 'ADMIN'].includes(normalized)) {
    styles = 'bg-indigo-50 text-indigo-700 border-indigo-200/80 dark:bg-indigo-950/60 dark:text-indigo-300 dark:border-indigo-800/80';
  } else if (['MODERATOR', 'CONTENT_MANAGER'].includes(normalized)) {
    styles = 'bg-sky-50 text-sky-700 border-sky-200/80 dark:bg-sky-950/60 dark:text-sky-300 dark:border-sky-800/80';
  }

  return (
    <span data-print-status="true" className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold border ${styles} ${className}`}>
      <span className="h-1.5 w-1.5 rounded-full bg-current opacity-70" />
      {normalized.replace(/_/g, ' ')}
    </span>
  );
}
