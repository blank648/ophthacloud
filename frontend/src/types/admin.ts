export type StaffRole = 'SUPER_ADMIN' | 'CLINIC_ADMIN' | 'DOCTOR' | 'OPTOMETRIST' | 'NURSE' | 'RECEPTIONIST' | 'OPTICAL_TECHNICIAN' | 'MANAGER' | 'PATIENT';

export interface StaffMember {
  id: string;
  keycloakUserId: string;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  role: StaffRole;
  specialization?: string;
  licenseNumber?: string;
  isActive: boolean;
  avatarUrl?: string;
  lastLoginAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateStaffMemberRequest {
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  role: StaffRole;
  specialization?: string;
  licenseNumber?: string;
  sendInviteEmail: boolean;
}

export interface UpdateStaffMemberRequest {
  firstName?: string;
  lastName?: string;
  phone?: string;
  specialization?: string;
  licenseNumber?: string;
  isActive?: boolean;
}

export interface ClinicSettings {
  workingHours: string;
  bookingSlotMinutes: number;
  bookingAdvanceDays: number;
  currency: string;
  vatRateDefault: number;
  portalEnabled: boolean;
  portalAppointmentBooking: boolean;
  invoicePrefix: string;
  orderNumberPrefix: string;
  prescriptionPrefix: string;
  quietHoursStart: string;
  quietHoursEnd: string;
  maxSmsPerPatient: number;
  name: string;
  cui: string;
  phone: string;
  email: string;
  address: string;
}

export interface UpdateClinicSettingsRequest {
  workingHours?: string;
  bookingSlotMinutes?: number;
  bookingAdvanceDays?: number;
  vatRateDefault?: number;
  portalEnabled?: boolean;
  portalAppointmentBooking?: boolean;
  invoicePrefix?: string;
  orderNumberPrefix?: string;
  prescriptionPrefix?: string;
  quietHoursStart?: string;
  quietHoursEnd?: string;
  maxSmsPerPatient?: number;
  name?: string;
  cui?: string;
  phone?: string;
  email?: string;
  address?: string;
}

export interface PermissionMatrixDto {
  role: StaffRole;
  moduleCode: string;
  canView: boolean;
  canCreate: boolean;
  canEdit: boolean;
  canDelete: boolean;
  canSign: boolean;
  canExport: boolean;
}

export interface UpdatePermissionsRequest {
  role: StaffRole;
  permissions: {
    moduleCode: string;
    canView: boolean;
    canCreate: boolean;
    canEdit: boolean;
    canDelete: boolean;
    canSign: boolean;
    canExport: boolean;
  }[];
}

export interface Equipment {
  id: string;
  name: string;
  brand?: string;
  type: string;
  location?: string;
  dicomEnabled: boolean;
  lastSyncAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateEquipmentRequest {
  name: string;
  brand?: string;
  type: string;
  location?: string;
  dicomEnabled: boolean;
}

export interface AuditLogDto {
  id: string;
  actorId: string;
  actorName: string;
  actorRole: string;
  action: string;
  entityType: string;
  entityId?: string;
  changedFields?: any;
  ipAddress?: string;
  occurredAt: string;
}

