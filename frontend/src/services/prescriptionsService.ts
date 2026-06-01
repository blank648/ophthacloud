import { apiGet, apiGetPaged, apiPost } from '@/lib/apiClient';
import type { PagedApiResponse } from '@/types/api';
import type {
  CreatePrescriptionRequest,
  PrescriptionDto,
  PrescriptionPdfResponse,
} from '@/types/prescriptions';

const BASE = '/api/v1/prescriptions';

export const prescriptionsService = {
  createPrescription(data: CreatePrescriptionRequest) {
    return apiPost<PrescriptionDto>(BASE, data);
  },

  getPrescription(id: string) {
    return apiGet<PrescriptionDto>(`${BASE}/${id}`);
  },

  listPrescriptions(patientId: string, params: { page: number; size: number }) {
    return apiGetPaged<PrescriptionDto>(BASE, { patientId, ...params });
  },

  signPrescription(id: string) {
    return apiPost<PrescriptionDto>(`${BASE}/${id}/sign`, { signatureConfirmation: true });
  },

  getPrescriptionPdf(id: string) {
    return apiGet<PrescriptionPdfResponse>(`${BASE}/${id}/pdf`);
  },
};
