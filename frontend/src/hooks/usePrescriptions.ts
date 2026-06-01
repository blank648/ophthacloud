import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { prescriptionsService } from '@/services/prescriptionsService';
import type { CreatePrescriptionRequest } from '@/types/prescriptions';

export const prescriptionsKeys = {
  all: ['prescriptions'] as const,
  list: (patientId: string, params: { page: number; size: number }) =>
    ['prescriptions', 'list', patientId, params] as const,
  detail: (id: string) => ['prescriptions', id] as const,
};

export function usePrescriptions(
  patientId: string | undefined,
  params: { page: number; size: number } = { page: 0, size: 20 }
) {
  return useQuery({
    queryKey: patientId ? prescriptionsKeys.list(patientId, params) : ['prescriptions', 'none'],
    queryFn: () => prescriptionsService.listPrescriptions(patientId!, params),
    enabled: !!patientId,
  });
}

export function usePrescription(id: string | undefined) {
  return useQuery({
    queryKey: id ? prescriptionsKeys.detail(id) : ['prescriptions', 'detail', 'none'],
    queryFn: () => prescriptionsService.getPrescription(id!),
    enabled: !!id,
  });
}

export function useCreatePrescription() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: CreatePrescriptionRequest) => prescriptionsService.createPrescription(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: prescriptionsKeys.all });
    },
  });
}

export function useSignPrescription() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => prescriptionsService.signPrescription(id),
    onSuccess: (_res, id) => {
      qc.invalidateQueries({ queryKey: prescriptionsKeys.detail(id) });
      qc.invalidateQueries({ queryKey: prescriptionsKeys.all });
    },
  });
}
