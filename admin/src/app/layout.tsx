import type { Metadata } from 'next';
import './globals.css';
import AdminShell from '@/components/AdminShell';

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
      <head>
        <link rel="preconnect" href="https://fonts.googleapis.com" />
        <link rel="preconnect" href="https://fonts.gstatic.com" crossOrigin="anonymous" />
        <link
          href="https://fonts.googleapis.com/css2?family=Anek+Gujarati:wght@400;500;600;700&family=DM+Sans:ital,opsz,wght@0,9..40,400;0,9..40,500;0,9..40,600;0,9..40,700;1,9..40,400&display=swap"
          rel="stylesheet"
        />
      </head>
      <body className="bg-cream min-h-screen text-brand-text flex font-english antialiased">
        <AdminShell>{children}</AdminShell>
      </body>
    </html>
  );
}
