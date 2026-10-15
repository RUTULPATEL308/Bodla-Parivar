'use client';

import { createContext, useContext, useEffect, useState } from 'react';

type AdminLanguage = 'gu' | 'en';

const AdminLanguageContext = createContext<{
  lang: AdminLanguage;
  toggleLanguage: () => void;
} | null>(null);

export function AdminLanguageProvider({ children }: { children: React.ReactNode }) {
  const [lang, setLang] = useState<AdminLanguage>('gu');
  useEffect(() => {
    document.documentElement.lang = lang;
  }, [lang]);
  const toggleLanguage = () => setLang(current => current === 'gu' ? 'en' : 'gu');

  return (
    <AdminLanguageContext.Provider value={{ lang, toggleLanguage }}>
      {children}
    </AdminLanguageContext.Provider>
  );
}

export function useAdminLanguage() {
  const context = useContext(AdminLanguageContext);
  if (!context) throw new Error('useAdminLanguage must be used within AdminLanguageProvider');
  return context;
}
