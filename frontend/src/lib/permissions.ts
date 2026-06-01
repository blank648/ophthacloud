import { useAuthStore, type PermissionAction } from '@/stores/authStore';

export function usePermissions() {
  const userInfo = useAuthStore((s) => s.userInfo);

  const hasPermission = (module: string, action: PermissionAction): boolean => {
    if (!userInfo) return false;
    return userInfo.permissions[module]?.includes(action) ?? false;
  };

  const hasAnyPermission = (module: string, actions: PermissionAction[]): boolean =>
    actions.some((a) => hasPermission(module, a));

  return { hasPermission, hasAnyPermission, userInfo };
}
