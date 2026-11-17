import React, { ReactNode } from 'react';

export const Card = ({ children }: { children: ReactNode }) => (
  <div className="rounded-lg border bg-white p-4 shadow-sm">
    {children}
  </div>
);

export const CardHeader = ({ children }: { children: ReactNode }) => (
  <div className="mb-2 border-b pb-2 text-xl font-semibold">
    {children}
  </div>
);

export const CardContent = ({ children }: { children: ReactNode }) => (
  <div className="text-gray-700">
    {children}
  </div>
);

export const CardTitle = ({ children }: { children: ReactNode }) => (
  <h3 className="text-lg font-medium leading-6 text-gray-900">
    {children}
  </h3>
);
