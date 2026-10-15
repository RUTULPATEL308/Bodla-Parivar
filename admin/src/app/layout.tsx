import type { Metadata } from 'next';
import localFont from 'next/font/local';
import './globals.css';
import AdminShell from '@/components/AdminShell';

const gujaratiFont = localFont({
  src: '../../public/fonts/AnekGujarati-Gujarati.woff2',
  weight: '400 700',
  display: 'swap',
  variable: '--font-anek-gujarati',
});

const englishFont = localFont({
  src: '../../public/fonts/DMSans-Latin.woff2',
  weight: '400 700',
  display: 'swap',
  variable: '--font-dm-sans',
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
    <html lang="gu">
      <body className={`${gujaratiFont.variable} ${englishFont.variable} bg-cream min-h-screen text-brand-text flex font-english antialiased`}>
        <AdminShell>{children}</AdminShell>
      </body>
    </html>
  );
}
