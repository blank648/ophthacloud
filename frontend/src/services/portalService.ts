import { apiGet, apiPut } from '@/lib/apiClient';
import type {
  PortalProfileDto,
  PortalAppointmentDto,
  PortalPrescriptionSummaryDto,
  PortalPrescriptionDetailDto,
  PortalInvestigationDto,
  PortalOpticalOrderDto,
  PortalNotificationDto,
  UpdateConsentsRequest,
} from '@/types/portal';

const BASE = '/api/v1/portal/me';

export const portalService = {
  getProfile() {
    return apiGet<PortalProfileDto>(BASE);
  },

  getAppointments() {
    return apiGet<PortalAppointmentDto[]>(`${BASE}/appointments`);
  },

  getPrescriptions(params?: { page?: number; size?: number }) {
    return apiGet<PortalPrescriptionSummaryDto[]>(`${BASE}/prescriptions`, params as Record<string, unknown>);
  },

  getPrescriptionDetail(id: string) {
    return apiGet<PortalPrescriptionDetailDto>(`${BASE}/prescriptions/${id}`);
  },

  getInvestigations(params?: { page?: number; size?: number }) {
    return apiGet<PortalInvestigationDto[]>(`${BASE}/investigations`, params as Record<string, unknown>);
  },

  getOpticalOrders() {
    return apiGet<PortalOpticalOrderDto[]>(`${BASE}/optical-orders`);
  },

  getNotifications(params?: { page?: number; size?: number }) {
    return apiGet<PortalNotificationDto[]>(`${BASE}/notifications`, params as Record<string, unknown>);
  },

  updateConsents(data: UpdateConsentsRequest) {
    return apiPut<void>(`${BASE}/consents`, data);
  },
};
