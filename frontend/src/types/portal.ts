export interface PortalProfileDto {
  id: string;
  mrn: string;
  firstName: string;
  lastName: string;
  dateOfBirth: string;
  gender: string;
  phone: string;
  email: string;
  hasPortalAccess: boolean;
}

export interface PortalAppointmentDto {
  id: string;
  doctorName: string;
  startAt: string;
  endAt: string;
  status: string;
  chiefComplaint: string;
  patientNotes: string;
  room: string;
  durationMinutes: number;
}

export interface PortalPrescriptionSummaryDto {
  id: string;
  type: string;
  status: string;
  issuedByName: string;
  issuedDate: string;
  validUntil: string;
}

export interface PortalPrescriptionLineDto {
  eye: string;
  lensType?: string;
  sphere: number;
  cylinder: number;
  axis?: number;
  add: number;
  pd?: number;
}

export interface PortalPrescriptionDetailDto {
  id: string;
  type: string;
  status: string;
  issuedByName: string;
  issuedDate: string;
  validUntil: string;
  lines: PortalPrescriptionLineDto[];
  qrVerificationToken: string;
}

export interface PortalInvestigationDto {
  id: string;
  category: string;
  status: string;
  orderedByName: string;
  orderedAt: string;
  completedAt: string;
  resultSummary: string;
}

export interface PortalOpticalOrderDto {
  id: string;
  orderNumber: string;
  stage: string;
  orderType: string;
  createdAt: string;
  updatedAt?: string;
}

export interface PortalNotificationDto {
  id: string;
  channel: string;
  status: string;
  subject: string;
  bodyPreview: string;
  sentAt: string;
  createdAt?: string;
}

export interface UpdateConsentsRequest {
  dataProcessingConsent: boolean;
  communicationConsent: boolean;
  researchConsent?: boolean;
}
