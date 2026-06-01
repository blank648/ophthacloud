export type ConsultationStatus = 'DRAFT' | 'IN_PROGRESS' | 'SIGNED';

export type SectionCode = 'A' | 'B' | 'C' | 'D' | 'E' | 'F' | 'G' | 'H' | 'I';

export interface SectionDataOD_OS {
  sph?: number;
  cyl?: number;
  axis?: number;
  add?: number;
  vaSC?: string;
  vaCC?: string;
  bcva?: string;
  seq?: number;
  iop?: number;
  iopMethod?: string;
}

export interface ConsultationSectionDto {
  id?: string;
  sectionCode: SectionCode;
  sectionData: {
    od?: SectionDataOD_OS;
    os?: SectionDataOD_OS;
    [key: string]: any;
  };
  isCompleted: boolean;
  updatedAt?: string;
  updatedBy?: string;
}

export interface ConsultationDto {
  id: string;
  patientId: string;
  appointmentId?: string;
  doctorId: string;
  doctorName?: string;
  status: ConsultationStatus;
  consultationDate: string;
  chiefComplaint?: string;
  sectionsCompleted: number;
  sections: Record<SectionCode, ConsultationSectionDto>;
  createdAt: string;
  updatedAt: string;
  signedAt?: string;
  signedBy?: string;
}

export interface CreateConsultationRequest {
  patientId: string;
  appointmentId?: string;
  consultationDate?: string;
  chiefComplaint?: string;
}

export interface SaveSectionRequest {
  sectionData: Record<string, any>;
  isCompleted: boolean;
}

export interface SignConsultationRequest {
  signatureConfirmation: boolean;
}
