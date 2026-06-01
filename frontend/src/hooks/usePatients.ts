import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { patientsService } from '@/services/patientsService';
import type {
  CreatePatientRequest,
  UpdatePatientRequest,
  ListPatientsParams,
} from '@/types/patients';

export const patientsKeys = {
  all: ['patients'] as const,
  list: (params: ListPatientsParams) => ['patients', 'list', params] as const,
  detail: (id: string) => ['patients', id] as const,
};

export function usePatients(params: ListPatientsParams) {
  return useQuery({
    queryKey: patientsKeys.list(params),
    queryFn: () => patientsService.listPatients(params),
  });
}

export function usePatient(id: string | undefined) {
  return useQuery({
    queryKey: id ? patientsKeys.detail(id) : ['patients', 'detail', 'none'],
    queryFn: () => patientsService.getPatient(id!),
    enabled: !!id,
  });
}

export function useCreatePatient() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: CreatePatientRequest) => patientsService.createPatient(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: patientsKeys.all });
    },
  });
}

export function useUpdatePatient() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdatePatientRequest }) =>
      patientsService.updatePatient(id, data),
    onSuccess: (_res, vars) => {
      qc.invalidateQueries({ queryKey: patientsKeys.detail(vars.id) });
      qc.invalidateQueries({ queryKey: patientsKeys.all });
    },
  });
}

export function useDeletePatient() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => patientsService.deletePatient(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: patientsKeys.all });
    },
  });
}

export function useInvitePatientToPortal() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => patientsService.inviteToPortal(id),
    onSuccess: (_res, id) => {
      qc.invalidateQueries({ queryKey: patientsKeys.detail(id) });
      qc.invalidateQueries({ queryKey: patientsKeys.all });
    },
  });
}
