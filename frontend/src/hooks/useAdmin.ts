import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { adminService } from '@/services/adminService';
import type { 
  StaffRole, CreateStaffMemberRequest, UpdateStaffMemberRequest,
  UpdateClinicSettingsRequest, UpdatePermissionsRequest, CreateEquipmentRequest
} from '@/types/admin';
import type { PaginatedParams } from '@/types/api';

export const adminKeys = {
  all: ['admin'] as const,
  staff: (params?: PaginatedParams & { role?: StaffRole }) => ['admin', 'staff', params] as const,
  settings: ['admin', 'settings'] as const,
  permissions: (role: StaffRole) => ['admin', 'permissions', role] as const,
  equipment: ['admin', 'equipment'] as const,
  auditLogs: (params?: any) => ['admin', 'auditLogs', params] as const,
};

// --- Staff ---
export function useStaff(params?: PaginatedParams & { role?: StaffRole }) {
  return useQuery({
    queryKey: adminKeys.staff(params),
    queryFn: () => adminService.listStaff(params),
  });
}

export function useCreateStaff() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateStaffMemberRequest) => adminService.createStaff(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['admin', 'staff'] });
    },
  });
}

export function useUpdateStaff() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string, data: UpdateStaffMemberRequest }) => adminService.updateStaff(id, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['admin', 'staff'] });
    },
  });
}

export function useDeleteStaff() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => adminService.deleteStaff(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['admin', 'staff'] });
    },
  });
}

// --- Settings ---
export function useClinicSettings() {
  return useQuery({
    queryKey: adminKeys.settings,
    queryFn: () => adminService.getSettings(),
  });
}

export function useUpdateClinicSettings() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: UpdateClinicSettingsRequest) => adminService.updateSettings(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: adminKeys.settings });
    },
  });
}

// --- Permissions ---
export function usePermissions(role: StaffRole) {
  return useQuery({
    queryKey: adminKeys.permissions(role),
    queryFn: () => adminService.getPermissions(role),
  });
}

export function useUpdatePermissions() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: UpdatePermissionsRequest) => adminService.updatePermissions(data),
    onSuccess: (_, variables) => {
      qc.invalidateQueries({ queryKey: adminKeys.permissions(variables.role) });
    },
  });
}

// --- Equipment ---
export function useEquipment() {
  return useQuery({
    queryKey: adminKeys.equipment,
    queryFn: () => adminService.listEquipment(),
  });
}

export function useCreateEquipment() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateEquipmentRequest) => adminService.createEquipment(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: adminKeys.equipment });
    },
  });
}

export function useDeleteEquipment() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => adminService.deleteEquipment(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: adminKeys.equipment });
    },
  });
}

// --- Audit Logs ---
export function useAuditLogs(params?: PaginatedParams & { entityType?: string; entityId?: string; userId?: string; action?: string; from?: string; to?: string }) {
  return useQuery({
    queryKey: adminKeys.auditLogs(params),
    queryFn: () => adminService.listAuditLogs(params),
  });
}

