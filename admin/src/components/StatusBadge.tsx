import React from 'react';

interface StatusBadgeProps {
  status: string;
}

export default function StatusBadge({ status }: StatusBadgeProps) {
  const normalized = status.toUpperCase();

  let styles = 'bg-stone-100 text-stone-700 border-stone-200';

  if (['APPROVED', 'PUBLISHED', 'ACTIVE', 'RESOLVED'].includes(normalized)) {
    styles = 'bg-emerald-50 text-emerald-700 border-emerald-200';
  } else if (['PENDING', 'PENDING_APPROVAL', 'UNDER_REVIEW', 'DRAFT'].includes(normalized)) {
    styles = 'bg-amber-50 text-amber-700 border-amber-200';
  } else if (['REJECTED', 'CANCELLED', 'EXPIRED', 'SUSPENDED'].includes(normalized)) {
    styles = 'bg-rose-50 text-rose-700 border-rose-200';
  }

  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium border ${styles}`}>
      {status.replace(/_/g, ' ')}
    </span>
  );
}
