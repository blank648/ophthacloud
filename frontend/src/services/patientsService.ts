import { apiGet, apiGetPaged, apiPost, apiPut, apiDelete } from '@/lib/apiClient';
import type { PagedApiResponse } from '@/types/api';
import type {
  PatientSummaryDto,
  PatientDto,
  CreatePatientRequest,
  UpdatePatientRequest,
  ListPatientsParams,
} from '@/types/patients';

const BASE = '/api/v1/patients';

export const patientsService = {
  listPatients(params: ListPatientsParams) {
    return apiGetPaged<PatientSummaryDto>(BASE, params as unknown as Record<string, unknown>);
  },

  createPatient(data: CreatePatientRequest) {
    return apiPost<PatientDto>(BASE, data);
  },

  getPatient(id: string) {
    return apiGet<PatientDto>(`${BASE}/${id}`);
  },

  updatePatient(id: string, data: UpdatePatientRequest) {
    return apiPut<PatientDto>(`${BASE}/${id}`, data);
  },

  deletePatient(id: string) {
    return apiDelete<void>(`${BASE}/${id}`);
  },

  getPatientConsultations(id: string, params: { page: number; size: number }) {
    return apiGetPaged<unknown>(`${BASE}/${id}/consultations`, params as Record<string, unknown>);
  },

  getPatientPrescriptions(id: string, params: { page: number; size: number }) {
    return apiGetPaged<unknown>(`${BASE}/${id}/prescriptions`, params as Record<string, unknown>);
  },

  getPatientAppointments(id: string, params: { page: number; size: number }) {
    return apiGetPaged<unknown>(`${BASE}/${id}/appointments`, params as Record<string, unknown>);
  },

  inviteToPortal(id: string) {
    return apiPost<{ invitedAt: string }>(`${BASE}/${id}/portal-invite`);
  },
};
