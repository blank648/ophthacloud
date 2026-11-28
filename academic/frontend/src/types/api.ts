export type UserRole = 'PATIENT' | 'RECEPTIONIST' | 'DOCTOR' | 'ADMIN';
export type TargetEye = 'OD' | 'OS' | 'AO';
export type AppointmentStatus = 'SCHEDULED' | 'COMPLETED' | 'NO_SHOW';
export type AdherenceStatus = 'PENDING' | 'CONFIRMED_ON_TIME' | 'CONFIRMED_LATE' | 'MISSED';

export interface PagedResponse<T> {
  items: T[];
  page: number;
  size: number;
  total: number;
}

export interface ApiErrorResponse {
  code: string;
  message: string;
  fieldErrors: Record<string, string>;
  requestId: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface PreferencesRequest {
  uiFontScale: number;
  highContrast: boolean;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  role: UserRole;
}

export interface CurrentUserResponse {
  userId: string;
  email: string;
  role: UserRole;
  patientId: string | null;
  uiFontScale: number;
  highContrast: boolean;
}

export interface CreatePatientRequest {
  email: string;
  cnp: string;
  lastName: string;
  firstName: string;
  phone?: string;
  birthDate: string;
  sex: string;
}

export interface CreatePatientResponse {
  patientId: string;
  userId: string;
  initialCredentials: {
    email: string;
    temporaryPassword: string;
  };
}

export interface PatientSummary {
  patientId: string;
  cnp: string;
  lastName: string;
  firstName: string;
  phone: string | null;
  birthDate: string;
  sex: string;
}

export interface DoctorSummary {
  doctorId: string;
  email: string;
  fullName: string;
}

export interface AppointmentSummary {
  appointmentId: string;
  patientId: string;
  patientName: string;
  doctorId: string;
  doctorName: string;
  scheduledAt: string;
  durationMin: number;
  status: AppointmentStatus;
}

export interface CreateAppointmentRequest {
  patientId: string;
  doctorId: string;
  scheduledAt: string;
  durationMin: number;
}

export interface CreateAppointmentResponse {
  appointmentId: string;
  status: AppointmentStatus;
}

export interface CreateConsultationRequest {
  appointmentId: string;
  vaOd: string;
  vaOs: string;
  iopOd: number;
  iopOs: number;
  biomicroscopyNotes?: string | null;
  diagnosisIcd10: string;
}

export interface ConsultationDto {
  consultationId: string;
  appointmentId: string;
  patientId: string;
  doctorId: string;
  vaOd: string;
  vaOs: string;
  iopOd: number;
  iopOs: number;
  biomicroscopyNotes: string | null;
  diagnosisIcd10: string;
}

export interface CreateTreatmentPlanRequest {
  consultationId: string;
  medicationName: string;
  targetEye: TargetEye;
  dropsPerDose: number;
  scheduleHours: string[];
  startDate: string;
  endDate: string;
  replacesPlanId?: string;
}

export interface TreatmentPlanSnapshotDto {
  planId: string;
  consultationId?: string;
  patientId: string;
  medicationName: string;
  targetEye: TargetEye;
  dropsPerDose: number;
  scheduleHours: string[];
  startDate: string;
  endDate: string;
  activatedAt: string;
  stoppedAt: string | null;
  active: boolean;
}

export interface NextDoseDto {
  planId: string;
  medicationName: string;
  targetEye: TargetEye;
  dropsPerDose: number;
  scheduledFor: string;
  localTime: string;
}

export interface ScheduledDoseDto {
  planId: string;
  medicationName: string;
  targetEye: TargetEye;
  dropsPerDose: number;
  scheduledFor: string;
  localTime: string;
  status: AdherenceStatus;
  confirmedAt: string | null;
}

export interface TodayAdherenceResponse {
  clinicDate: string;
  nextDose: NextDoseDto | null;
  doses: ScheduledDoseDto[];
  confirmedToday: number;
  expectedToday: number;
  dailyProgressPercent: number;
  currentStreak: number;
  totalPoints: number;
  rewardsSwitchEnabled: boolean;
}

export interface ConfirmDoseRequest {
  planId: string;
  scheduledFor: string;
}

export interface ConfirmDoseResponse {
  status: AdherenceStatus;
  confirmedAt: string;
  confirmedToday: number;
  expectedToday: number;
  dailyProgressPercent: number;
  currentStreak: number;
  forgivenessShields: number;
  totalPoints: number;
  rewardsSwitchEnabled: boolean;
}

export interface AdherenceHistoryItemDto {
  planId: string;
  medicationName: string;
  scheduledFor: string;
  confirmedAt: string | null;
  status: AdherenceStatus;
}

export interface AdherenceSummaryResponse {
  from: string;
  to: string;
  expected: number;
  confirmedOnTime: number;
  confirmedLate: number;
  missed: number;
  pending: number;
  adherencePercent: number | null;
  history: PagedResponse<AdherenceHistoryItemDto>;
}
