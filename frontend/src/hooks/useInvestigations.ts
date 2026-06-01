import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { investigationsService } from '@/services/investigationsService';
import type { CreateInvestigationRequest, UpdateInvestigationResultRequest, InvestigationCategoryType, InvestigationStatusType } from '@/types/investigations';

export const investigationsKeys = {
  all: ['investigations'] as const,
  list: (patientId: string, params: { page: number; size: number; category?: InvestigationCategoryType; status?: InvestigationStatusType }) =>
    ['investigations', 'list', patientId, params] as const,
  detail: (id: string) => ['investigations', id] as const,
};

export function useInvestigations(
  patientId: string | undefined,
  params: { page: number; size: number; category?: InvestigationCategoryType; status?: InvestigationStatusType } = { page: 0, size: 20 }
) {
  return useQuery({
    queryKey: patientId ? investigationsKeys.list(patientId, params) : ['investigations', 'none'],
    queryFn: () => investigationsService.listInvestigations({ patientId: patientId!, ...params }),
    enabled: !!patientId,
  });
}

export function useInvestigation(id: string | undefined) {
  return useQuery({
    queryKey: id ? investigationsKeys.detail(id) : ['investigations', 'detail', 'none'],
    queryFn: () => investigationsService.getInvestigation(id!),
    enabled: !!id,
  });
}

export function useCreateInvestigation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateInvestigationRequest) => investigationsService.createInvestigation(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: investigationsKeys.all });
    },
  });
}

export function useUpdateInvestigationResult() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateInvestigationResultRequest }) => investigationsService.updateInvestigationResult(id, data),
    onSuccess: (_res, { id }) => {
      qc.invalidateQueries({ queryKey: investigationsKeys.detail(id) });
      qc.invalidateQueries({ queryKey: investigationsKeys.all });
    },
  });
}

export function useUploadInvestigationFile() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, file, fileType, laterality }: { id: string; file: File; fileType?: string; laterality?: string }) =>
      investigationsService.uploadFile(id, file, fileType, laterality),
    onSuccess: (_res, { id }) => {
      qc.invalidateQueries({ queryKey: investigationsKeys.detail(id) });
      qc.invalidateQueries({ queryKey: investigationsKeys.all });
    },
  });
}

