import React, { ReactNode } from 'react';

type Props = {
  children: ReactNode;
  onClick?: () => void;
  className?: string;
  disabled?: boolean;
};

export const Button = ({ children, onClick, className = '', disabled = false }: Props) => (
  <button
    onClick={onClick}
    disabled={disabled}
    className={`rounded-xl bg-navy-900 px-4 py-2 text-sm font-semibold text-white ${className} ${disabled ? 'opacity-50 cursor-not-allowed' : ''}`}
  >
    {children}
  </button>
);
