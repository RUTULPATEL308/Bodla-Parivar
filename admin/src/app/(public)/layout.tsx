import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Delete My Account - Bodla Parivar',
  description: 'Request deletion of your Bodla Parivar account and associated data.',
};

export default function PublicLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="gu" className="font-sans antialiased selection:bg-amber-100 selection:text-amber-900">
      <body className="bg-slate-50 min-h-screen text-slate-900 flex font-sans antialiased selection:bg-amber-100 selection:text-amber-900">
        {children}
      </body>
    </html>
  );
}
