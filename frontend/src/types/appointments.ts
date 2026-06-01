export type AppointmentStatus =
  | 'BOOKED'
  | 'CONFIRMED'
  | 'CHECKED_IN'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'NO_SHOW'
  | 'CANCELLED';

export type AppointmentChannel = 'IN_PERSON' | 'VIDEO' | 'PHONE';

export interface AppointmentTypeDto {
  id: string;
  code: string;
  name: string;
  durationMinutes: number;
  color: string;
  active: boolean;
  price?: number;
}

export interface AppointmentTypeRequest {
  code: string;
  name: string;
  durationMinutes: number;
  color: string;
  active?: boolean;
  price?: number;
}

export interface AppointmentDto {
  id: string;
  patientId: string;
  doctorId: string;
  appointmentTypeId: string;
  startAt: string;
  endAt: string;
  durationMinutes: number;
  status: AppointmentStatus;
  channel: AppointmentChannel;
  chiefComplaint?: string;
  patientNotes?: string;
  internalNotes?: string;
  room?: string;
  createdAt: string;
  updatedAt?: string;
}

export interface AppointmentSummaryDto extends AppointmentDto {
  patientName: string;
  patientMrn: string;
  activeDiagnosisFlags?: string[];
  appointmentTypeName: string;
  appointmentTypeColor: string;
  doctorName?: string;
}

export interface CreateAppointmentRequest {
  patientId: string;
  doctorId: string;
  appointmentTypeId: string;
  startAt: string;
  durationMinutes?: number;
  channel: AppointmentChannel;
  chiefComplaint?: string;
  patientNotes?: string;
  internalNotes?: string;
  room?: string;
}

export type UpdateAppointmentRequest = Partial<CreateAppointmentRequest>;

export interface UpdateStatusRequest {
  status: AppointmentStatus;
  cancellationReason?: string;
}

export interface ListAppointmentsParams {
  from: string;
  to: string;
  doctorId?: string;
  patientId?: string;
  status?: AppointmentStatus;
}
