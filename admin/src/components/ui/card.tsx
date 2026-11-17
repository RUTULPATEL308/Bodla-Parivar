import React, { ReactNode } from 'react';

// Simple reusable Card components styled with Tailwind. Adjust classes as needed.
export const Card = ({ children, className = '' }: { children: ReactNode; className?: string }) => (
  <div className={`rounded-lg border bg-white p-4 shadow-sm ${className}`}>
    {children}
  </div>
);

export const CardHeader = ({ children, className = '' }: { children: ReactNode; className?: string }) => (
  <div className={`mb-2 border-b pb-2 text-xl font-semibold ${className}`}>
    {children}
  </div>
);

export const CardContent = ({ children, className = '' }: { children: ReactNode; className?: string }) => (
  <div className={`text-gray-700 ${className}`}>
    {children}
  </div>
);

export const CardTitle = ({ children, className = '' }: { children: ReactNode; className?: string }) => (
  <h3 className={`text-lg font-medium leading-6 text-gray-900 ${className}`}>
    {children}
  </h3>
);
