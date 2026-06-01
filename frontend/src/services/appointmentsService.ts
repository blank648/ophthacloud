import { apiGet, apiPost, apiPut, apiPatch, apiDelete } from '@/lib/apiClient';
import type {
  AppointmentDto,
  AppointmentSummaryDto,
  AppointmentTypeDto,
  AppointmentTypeRequest,
  CreateAppointmentRequest,
  UpdateAppointmentRequest,
  UpdateStatusRequest,
  AppointmentStatus,
  ListAppointmentsParams,
} from '@/types/appointments';

const BASE = '/api/v1/appointments';
const TYPES = '/api/v1/appointment-types';

export const appointmentsService = {
  listAppointments(params: ListAppointmentsParams) {
    return apiGet<AppointmentSummaryDto[]>(BASE, params as unknown as Record<string, unknown>);
  },

  createAppointment(data: CreateAppointmentRequest) {
    return apiPost<AppointmentDto>(BASE, data);
  },

  getAppointment(id: string) {
    return apiGet<AppointmentDto>(`${BASE}/${id}`);
  },

  updateAppointment(id: string, data: UpdateAppointmentRequest) {
    return apiPut<AppointmentDto>(`${BASE}/${id}`, data);
  },

  updateAppointmentStatus(id: string, status: AppointmentStatus, reason?: string) {
    const body: UpdateStatusRequest = { status, reason };
    return apiPatch<AppointmentDto>(`${BASE}/${id}/status`, body);
  },

  deleteAppointment(id: string) {
    return apiDelete<void>(`${BASE}/${id}`);
  },

  listAppointmentTypes() {
    return apiGet<AppointmentTypeDto[]>(TYPES);
  },

  createAppointmentType(data: AppointmentTypeRequest) {
    return apiPost<AppointmentTypeDto>(TYPES, data);
  },
};
