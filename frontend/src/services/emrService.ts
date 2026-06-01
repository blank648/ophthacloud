import { apiGet, apiGetPaged, apiPost, apiPut, apiPatch } from '@/lib/apiClient';
import type { PagedApiResponse } from '@/types/api';
import type {
  ConsultationDto,
  ConsultationSectionDto,
  CreateConsultationRequest,
  SaveSectionRequest,
  SectionCode,
  SignConsultationRequest,
} from '@/types/emr';

const BASE = '/api/v1/emr/consultations';

export const emrService = {
  createConsultation(data: CreateConsultationRequest) {
    return apiPost<ConsultationDto>(BASE, data);
  },

  getConsultation(id: string) {
    return apiGet<ConsultationDto>(`${BASE}/${id}`);
  },

  saveSection(consultationId: string, sectionCode: SectionCode, data: SaveSectionRequest) {
    return apiPut<ConsultationSectionDto>(
      `${BASE}/${consultationId}/sections/${sectionCode}`,
      {
        ...data,
        sectionData: JSON.stringify(data.sectionData)
      }
    );
  },

  markSectionComplete(consultationId: string, sectionCode: SectionCode) {
    return apiPatch<ConsultationSectionDto>(
      `${BASE}/${consultationId}/sections/${sectionCode}/complete`
    );
  },

  signConsultation(id: string, data: SignConsultationRequest) {
    return apiPost<ConsultationDto>(`${BASE}/${id}/sign`, data);
  },

  listConsultations(patientId: string, params: { page: number; size: number }) {
    return apiGetPaged<ConsultationDto>(BASE, { patientId, ...params });
  },
};
