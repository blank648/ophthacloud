import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { emrService } from '@/services/emrService';
import type {
  CreateConsultationRequest,
  SaveSectionRequest,
  SectionCode,
  SignConsultationRequest,
} from '@/types/emr';

export const emrKeys = {
  all: ['emr'] as const,
  consultation: (id: string) => ['emr', 'consultation', id] as const,
  list: (patientId: string, params: { page: number; size: number }) =>
    ['emr', 'consultations', patientId, params] as const,
};

export function useConsultation(id: string | undefined) {
  return useQuery({
    queryKey: id ? emrKeys.consultation(id) : ['emr', 'consultation', 'none'],
    queryFn: () => emrService.getConsultation(id!),
    enabled: !!id,
  });
}

export function useConsultations(patientId: string | undefined, params: { page: number; size: number }) {
  return useQuery({
    queryKey: patientId ? emrKeys.list(patientId, params) : ['emr', 'consultations', 'none'],
    queryFn: () => emrService.listConsultations(patientId!, params),
    enabled: !!patientId,
  });
}

export function useCreateConsultation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateConsultationRequest) => emrService.createConsultation(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: emrKeys.all });
    },
  });
}

export function useSaveSection(consultationId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ sectionCode, data }: { sectionCode: SectionCode; data: SaveSectionRequest }) =>
      emrService.saveSection(consultationId, sectionCode, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: emrKeys.consultation(consultationId) });
    },
  });
}

export function useMarkSectionComplete(consultationId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (sectionCode: SectionCode) =>
      emrService.markSectionComplete(consultationId, sectionCode),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: emrKeys.consultation(consultationId) });
    },
  });
}

export function useSignConsultation(consultationId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: SignConsultationRequest) => emrService.signConsultation(consultationId, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: emrKeys.consultation(consultationId) });
    },
  });
}
