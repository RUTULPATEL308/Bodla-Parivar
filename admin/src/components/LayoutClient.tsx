'use client';

import { usePathname } from 'next/navigation';
import AdminShell from '@/components/AdminShell';

const PUBLIC_PATHS = ['/delete-account'];

export default function LayoutClient({
  children,
}: {
  children: React.ReactNode;
}) {
  const pathname = usePathname();
  const isPublicPath = PUBLIC_PATHS.includes(pathname);

  return (
    <>
      {isPublicPath ? (
        <>{children}</>
      ) : (
        <AdminShell>{children}</AdminShell>
      )}
    </>
  );
}
