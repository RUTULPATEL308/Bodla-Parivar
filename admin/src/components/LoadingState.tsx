import React from 'react';

export default function LoadingState({ message = 'Loading data…' }: { message?: string }) {
  return (
    <div className="flex flex-col items-center justify-center p-12 text-center">
      <div className="relative flex h-12 w-12 items-center justify-center">
        <div className="absolute h-12 w-12 rounded-full border-4 border-slate-200 dark:border-slate-800" />
        <div className="absolute h-12 w-12 animate-spin rounded-full border-4 border-navy-900 border-t-transparent dark:border-sky-400 dark:border-t-transparent" />
      </div>
      <p className="mt-4 text-xs font-semibold text-slate-500 tracking-wide dark:text-slate-400">{message}</p>
    </div>
  );
}
