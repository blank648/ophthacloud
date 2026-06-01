import type { ReactNode } from 'react';
import { usePermissions } from '@/lib/permissions';
import type { PermissionAction } from '@/stores/authStore';

interface PermissionGuardProps {
  module: string;
  action: PermissionAction;
  children: ReactNode;
  fallback?: ReactNode;
}

export function PermissionGuard({ module, action, children, fallback = null }: PermissionGuardProps) {
  const { hasPermission } = usePermissions();
  if (!hasPermission(module, action)) {
    return <>{fallback}</>;
  }
  return <>{children}</>;
}
