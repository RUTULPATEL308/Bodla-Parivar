'use client';

import { createContext, useContext, useEffect, useState } from 'react';

type AdminLanguage = 'gu' | 'en';
export type PrintLanguage = AdminLanguage | 'both';
export type AdminFeature = 'members' | 'notices' | 'offerings' | 'navratri' | 'complaints' | 'emergency_contacts' | 'village_history' | 'events' | 'directory' | 'gallery' | 'notifications' | 'result_uploads';

const AdminLanguageContext = createContext<{
  lang: AdminLanguage;
  toggleLanguage: () => void;
  printLanguage: PrintLanguage;
  setPrintLanguage: (language: PrintLanguage) => void;
  features: AdminFeature[];
  setFeatures: (features: AdminFeature[]) => void;
  isFullStaff: boolean;
  setIsFullStaff: (value: boolean) => void;
} | null>(null);

export function AdminLanguageProvider({ children }: { children: React.ReactNode }) {
  const [lang, setLang] = useState<AdminLanguage>(() => {
    if (typeof window === 'undefined') return 'gu';
    const stored = window.localStorage.getItem('bodla-admin-language');
    return stored === 'en' ? 'en' : 'gu';
  });
  const [printLanguage, setPrintLanguage] = useState<PrintLanguage>('gu');
  const [features, setFeatures] = useState<AdminFeature[]>([]);
  const [isFullStaff, setIsFullStaff] = useState(false);
  useEffect(() => {
    document.documentElement.lang = lang;
  }, [lang]);
  const toggleLanguage = () => setLang(current => {
    const next = current === 'gu' ? 'en' : 'gu';
    window.localStorage.setItem('bodla-admin-language', next);
    document.documentElement.lang = next;
    return next;
  });

  return (
    <AdminLanguageContext.Provider value={{ lang, toggleLanguage, printLanguage, setPrintLanguage, features, setFeatures, isFullStaff, setIsFullStaff }}>
      {children}
    </AdminLanguageContext.Provider>
  );
}

export function useAdminLanguage() {
  const context = useContext(AdminLanguageContext);
  if (!context) throw new Error('useAdminLanguage must be used within AdminLanguageProvider');
  return context;
}

export function usePrintLanguage() {
  const context = useAdminLanguage();
  return { printLanguage: context.printLanguage, setPrintLanguage: context.setPrintLanguage };
}

export function useAdminFeatures() {
  const context = useAdminLanguage();
  return { features: context.features, setFeatures: context.setFeatures, isFullStaff: context.isFullStaff, setIsFullStaff: context.setIsFullStaff };
}
