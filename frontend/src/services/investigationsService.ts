import { apiGet, apiGetPaged, apiPost, apiPut, apiClient } from '@/lib/apiClient';
import type { PagedApiResponse, ApiResponse } from '@/types/api';
import type {
  InvestigationDto,
  CreateInvestigationRequest,
  UpdateInvestigationResultRequest,
  InvestigationFileDto,
  InvestigationCategoryType,
  InvestigationStatusType
} from '@/types/investigations';

const BASE = '/api/v1/investigations';

export const investigationsService = {
  createInvestigation(data: CreateInvestigationRequest) {
    return apiPost<InvestigationDto>(BASE, data);
  },

  getInvestigation(id: string) {
    return apiGet<InvestigationDto>(`${BASE}/${id}`);
  },

  listInvestigations(params: {
    patientId: string;
    category?: InvestigationCategoryType;
    status?: InvestigationStatusType;
    page: number;
    size: number;
  }) {
    return apiGetPaged<InvestigationDto>(BASE, params as Record<string, unknown>);
  },

  updateInvestigationResult(id: string, data: UpdateInvestigationResultRequest) {
    return apiPut<InvestigationDto>(`${BASE}/${id}`, data);
  },

  uploadFile(id: string, file: File, fileType?: string, laterality?: string) {
    const formData = new FormData();
    formData.append('file', file);
    if (fileType) formData.append('fileType', fileType);
    if (laterality) formData.append('laterality', laterality);
    return apiClient.post<ApiResponse<InvestigationFileDto>>(`${BASE}/${id}/files`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    }).then(res => res.data.data);
  },
};

