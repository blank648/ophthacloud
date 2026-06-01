export type InvestigationCategoryType =
  | 'OCT'
  | 'VISUAL_FIELD'
  | 'TOPOGRAPHY'
  | 'FUNDUS_PHOTO'
  | 'BIOMETRY'
  | 'SPECULAR_MICROSCOPY'
  | 'ELECTRORETINOGRAPHY'
  | 'BLOOD_TEST'
  | 'OTHER';

export type InvestigationStatusType = 'ORDERED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';

export interface InvestigationFileDto {
  id: string;
  fileName: string;
  mimeType: string;
  fileSizeBytes: number;
  fileType: string;
  laterality: string;
  downloadUrl: string;
  createdAt: string;
}

export interface InvestigationDto {
  id: string;
  patientId: string;
  consultationId?: string;
  orderedById: string;
  orderedByName: string;
  category: InvestigationCategoryType;
  name: string;
  device?: string;
  status: InvestigationStatusType;
  orderedAt: string;
  performedAt?: string;
  resultData?: any;
  interpretation?: string;
  isUrgent?: boolean;
  notes?: string;
  performedById?: string;
  createdAt: string;
  updatedAt: string;
  files: InvestigationFileDto[];
}

export interface CreateInvestigationRequest {
  patientId: string;
  consultationId?: string;
  category: InvestigationCategoryType;
  name: string;
  device?: string;
  isUrgent?: boolean;
  notes?: string;
}

export interface UpdateInvestigationResultRequest {
  status: InvestigationStatusType;
  performedAt?: string;
  resultData?: any;
  interpretation?: string;
}
