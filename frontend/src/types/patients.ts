export type Gender = 'MALE' | 'FEMALE' | 'OTHER' | 'UNKNOWN';
export type Laterality = 'OD' | 'OS' | 'BILATERAL';

export interface ActiveDiagnosis {
  icd10Code: string;
  icd10Name: string;
  laterality: Laterality;
}

export interface PatientSummaryDto {
  id: string;
  mrn: string;
  firstName: string;
  lastName: string;
  dateOfBirth?: string;
  age?: number;
  gender?: Gender;
  phone?: string;
  email?: string;
  hasPortalAccess?: boolean;
  isActive?: boolean;
  activeDiagnoses?: ActiveDiagnosis[];
  lastConsultationDate?: string;
  nextAppointmentAt?: string;
  createdAt?: string;
}

export interface MedicalHistoryDto {
  hasDiabetes: boolean;
  hasGlaucomaHistory: boolean;
  activeDiagnoses: ActiveDiagnosis[];
  knownAllergies: string | null;
  currentMedications: string | null;
}

export interface PatientStatisticsDto {
  totalConsultations: number;
  totalPrescriptions: number;
  totalInvestigations: number;
  totalOpticalOrders: number;
  lastVisitDate?: string;
}

export interface PatientDto extends Omit<PatientSummaryDto, 'activeDiagnoses'> {
  cnp?: string;
  phoneAlt?: string;
  address?: string;
  city?: string;
  county?: string;
  bloodType?: string;
  occupation?: string;
  employer?: string;
  insuranceProvider?: string;
  insuranceNumber?: string;
  emergencyContactName?: string;
  emergencyContactPhone?: string;
  referringDoctor?: string;
  portalInvitedAt?: string;
  notes?: string;
  avatarUrl?: string;
  updatedAt?: string;
  medicalHistory?: MedicalHistoryDto;
  statistics?: PatientStatisticsDto;
}

export interface CreatePatientRequest {
  firstName: string;
  lastName: string;
  dateOfBirth: string;
  gender: Gender;
  phone?: string;
  phoneAlt?: string;
  email?: string;
  cnp?: string;
  address?: string;
  city?: string;
  county?: string;
  bloodType?: string;
  occupation?: string;
  employer?: string;
  insuranceProvider?: string;
  insuranceNumber?: string;
  emergencyContactName?: string;
  emergencyContactPhone?: string;
  referringDoctor?: string;
  notes?: string;
}

export type UpdatePatientRequest = Partial<CreatePatientRequest>;

export interface ListPatientsParams {
  q?: string;
  page: number;
  size: number;
  sort?: string;
  direction?: 'asc' | 'desc';
}
