import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { portalService } from '@/services/portalService';
import type { UpdateConsentsRequest } from '@/types/portal';

export const portalKeys = {
  all: ['portal'] as const,
  profile: () => ['portal', 'profile'] as const,
  appointments: () => ['portal', 'appointments'] as const,
  prescriptions: (params?: { page?: number; size?: number }) => ['portal', 'prescriptions', params] as const,
  prescriptionDetail: (id: string) => ['portal', 'prescriptions', 'detail', id] as const,
  investigations: (params?: { page?: number; size?: number }) => ['portal', 'investigations', params] as const,
  opticalOrders: () => ['portal', 'opticalOrders'] as const,
  notifications: (params?: { page?: number; size?: number }) => ['portal', 'notifications', params] as const,
};

export function usePortalProfile() {
  return useQuery({
    queryKey: portalKeys.profile(),
    queryFn: () => portalService.getProfile(),
  });
}

export function usePortalAppointments() {
  return useQuery({
    queryKey: portalKeys.appointments(),
    queryFn: () => portalService.getAppointments(),
  });
}

export function usePortalPrescriptions(params?: { page?: number; size?: number }) {
  return useQuery({
    queryKey: portalKeys.prescriptions(params),
    queryFn: () => portalService.getPrescriptions(params),
  });
}

export function usePortalPrescriptionDetail(id: string | undefined) {
  return useQuery({
    queryKey: id ? portalKeys.prescriptionDetail(id) : ['portal', 'prescriptions', 'detail', 'none'],
    queryFn: () => portalService.getPrescriptionDetail(id!),
    enabled: !!id,
  });
}

export function usePortalInvestigations(params?: { page?: number; size?: number }) {
  return useQuery({
    queryKey: portalKeys.investigations(params),
    queryFn: () => portalService.getInvestigations(params),
  });
}

export function usePortalOpticalOrders() {
  return useQuery({
    queryKey: portalKeys.opticalOrders(),
    queryFn: () => portalService.getOpticalOrders(),
  });
}

export function usePortalNotifications(params?: { page?: number; size?: number }) {
  return useQuery({
    queryKey: portalKeys.notifications(params),
    queryFn: () => portalService.getNotifications(params),
  });
}

export function useUpdatePortalConsents() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: UpdateConsentsRequest) => portalService.updateConsents(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: portalKeys.profile() });
      qc.invalidateQueries({ queryKey: portalKeys.all });
    },
  });
}
