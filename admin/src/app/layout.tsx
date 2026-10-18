import type { Metadata } from 'next';
import localFont from 'next/font/local';
import './globals.css';
import AdminShell from '@/components/AdminShell';

const gujaratiFont = localFont({
  src: '../../public/fonts/AnekGujarati-Gujarati.woff2',
  weight: '400 600 700',
  display: 'swap',
  variable: '--font-anek-gujarati',
});

const latinFont = localFont({
  src: '../../public/fonts/DMSans-Latin.woff2',
  display: 'swap',
  variable: '--font-inter',
  weight: '400 500 600 700 800',
});

export const metadata: Metadata = {
  title: 'બોદલા પરિવાર — Admin Console',
  description: 'Official digital community management console for Bodla village, Mehsana, Gujarat',
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="gu" className={`${gujaratiFont.variable} ${latinFont.variable}`}>
      <body className="bg-slate-50 min-h-screen text-slate-900 flex font-sans antialiased selection:bg-amber-100 selection:text-amber-900">
        <AdminShell>{children}</AdminShell>
      </body>
    </html>
  );
}
