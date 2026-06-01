import { apiGet, apiGetPaged, apiPost, apiPut, apiDelete } from '@/lib/apiClient';
import type { PaginatedParams } from '@/types/api';
import type { 
  StaffMember, CreateStaffMemberRequest, UpdateStaffMemberRequest, StaffRole,
  ClinicSettings, UpdateClinicSettingsRequest,
  PermissionMatrixDto, UpdatePermissionsRequest,
  Equipment, CreateEquipmentRequest, AuditLogDto
} from '@/types/admin';

const BASE = '/api/v1/admin';

export const adminService = {
  // --- Staff ---
  listStaff: async (params?: PaginatedParams & { role?: StaffRole }) => {
    return apiGetPaged<StaffMember>(`${BASE}/staff`, params);
  },
  createStaff: async (request: CreateStaffMemberRequest): Promise<StaffMember> => {
    return apiPost<StaffMember>(`${BASE}/staff`, request);
  },
  updateStaff: async (id: string, request: UpdateStaffMemberRequest): Promise<StaffMember> => {
    return apiPut<StaffMember>(`${BASE}/staff/${id}`, request);
  },
  deleteStaff: async (id: string): Promise<void> => {
    return apiDelete(`${BASE}/staff/${id}`);
  },

  // --- Settings ---
  getSettings: async (): Promise<ClinicSettings> => {
    return apiGet<ClinicSettings>(`${BASE}/settings`);
  },
  updateSettings: async (request: UpdateClinicSettingsRequest): Promise<ClinicSettings> => {
    return apiPut<ClinicSettings>(`${BASE}/settings`, request);
  },

  // --- Permissions ---
  getPermissions: async (role: StaffRole): Promise<PermissionMatrixDto[]> => {
    return apiGet<PermissionMatrixDto[]>(`${BASE}/permissions`, { role });
  },
  updatePermissions: async (request: UpdatePermissionsRequest): Promise<PermissionMatrixDto[]> => {
    return apiPut<PermissionMatrixDto[]>(`${BASE}/permissions`, request);
  },

  // --- Equipment ---
  listEquipment: async (): Promise<Equipment[]> => {
    return apiGet<Equipment[]>(`${BASE}/equipment`);
  },
  createEquipment: async (request: CreateEquipmentRequest): Promise<Equipment> => {
    return apiPost<Equipment>(`${BASE}/equipment`, request);
  },
  deleteEquipment: async (id: string): Promise<void> => {
    return apiDelete(`${BASE}/equipment/${id}`);
  },

  // --- Audit Logs ---
  listAuditLogs: async (params?: PaginatedParams & { entityType?: string; entityId?: string; userId?: string; action?: string; from?: string; to?: string }) => {
    return apiGetPaged<AuditLogDto>('/api/v1/audit/log', params);
  }
};

