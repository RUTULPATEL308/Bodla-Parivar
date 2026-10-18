'use client';

import React, { createContext, startTransition, useContext, useEffect, useState } from 'react';

type Theme = 'light' | 'dark';

interface ThemeContextType {
  theme: Theme;
  toggleTheme: () => void;
  setTheme: (theme: Theme) => void;
}

const ThemeContext = createContext<ThemeContextType>({
  theme: 'light',
  toggleTheme: () => {},
  setTheme: () => {},
});

export function ThemeProvider({ children }: { children: React.ReactNode }) {
  const [theme, setThemeState] = useState<Theme>('light');

  useEffect(() => {
    try {
      const stored = localStorage.getItem('bodla-theme') as Theme | null;
      if (stored === 'dark' || stored === 'light') {
        startTransition(() => setThemeState(stored));
        document.documentElement.classList.toggle('dark', stored === 'dark');
      } else if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
        startTransition(() => setThemeState('dark'));
        document.documentElement.classList.add('dark');
      }
    } catch {
      // Ignore localStorage issues
    }
  }, []);

  const setTheme = (next: Theme) => {
    setThemeState(next);
    try {
      localStorage.setItem('bodla-theme', next);
    } catch {
      // Ignore
    }
    document.documentElement.classList.toggle('dark', next === 'dark');
  };

  const toggleTheme = () => {
    setTheme(theme === 'light' ? 'dark' : 'light');
  };

  return (
    <ThemeContext.Provider value={{ theme, toggleTheme, setTheme }}>
      {children}
    </ThemeContext.Provider>
  );
}

export function useTheme() {
  return useContext(ThemeContext);
}
