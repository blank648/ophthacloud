import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { notificationsService } from '@/services/notificationsService';
import { useAuthStore } from '@/stores/authStore';

export const notificationKeys = {
  all: ['notifications'] as const,
  rules: (page: number, size: number) => ['notifications', 'rules', page, size] as const,
  recallProtocols: (page: number, size: number) => ['notifications', 'recallProtocols', page, size] as const,
  logs: (params: { page: number; size: number; patientId?: string; status?: string }) => 
    ['notifications', 'logs', params] as const,
};

export function useNotificationRules(page = 0, size = 50) {
  return useQuery({
    queryKey: notificationKeys.rules(page, size),
    queryFn: () => notificationsService.listRules({ page, size }),
  });
}

export function useToggleNotificationRule() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => notificationsService.toggleRule(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: notificationKeys.all });
    },
  });
}

export function useRecallProtocols(page = 0, size = 50) {
  return useQuery({
    queryKey: notificationKeys.recallProtocols(page, size),
    queryFn: () => notificationsService.listRecallProtocols({ page, size }),
  });
}

export function useNotificationLogs(params: { page?: number; size?: number; patientId?: string; status?: string } = {}) {
  const normalized = {
    page: params.page ?? 0,
    size: params.size ?? 50,
    patientId: params.patientId,
    status: params.status,
  };
  const userInfo = useAuthStore(s => s.userInfo);
  const hasViewPermission = userInfo?.permissions['notifications']?.includes('VIEW') ?? false;

  return useQuery({
    queryKey: notificationKeys.logs(normalized),
    queryFn: () => notificationsService.listNotificationLogs(normalized),
    enabled: hasViewPermission,
  });
}
