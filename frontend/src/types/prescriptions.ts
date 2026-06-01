export type PrescriptionStatus = 'ACTIVE' | 'EXPIRED' | 'CANCELLED' | 'SUPERSEDED';

export type PrescriptionType = 'DISTANCE' | 'NEAR' | 'PROGRESSIVE' | 'CONTACT_LENS' | 'POST_OP' | 'TINTED';

export type LensType = 'SINGLE_VISION' | 'BIFOCAL' | 'PROGRESSIVE' | 'OFFICE' | 'CONTACT';

export type Eye = 'OD' | 'OS';

export interface PrescriptionLineDto {
  id?: string;
  eye: Eye;
  sph?: number;
  cyl?: number;
  axis?: number;
  addPower?: number;
  vaSc?: string;
  vaCc?: string;
  bcva?: string;
}

export interface PrescriptionDto {
  id: string;
  patientId: string;
  consultationId?: string;
  prescriptionNumber: string;
  prescriptionType: PrescriptionType;
  status: PrescriptionStatus;
  issuedById: string;
  issuedByName: string;
  issuedAt: string;
  validFrom: string;
  validUntil: string;
  pdBinocular?: number;
  pdOd?: number;
  pdOs?: number;
  prismOd?: string;
  prismOs?: string;
  lensType?: LensType;
  lensMaterial?: string;
  lensCoating?: string;
  frameRecommendation?: string;
  clinicalNotes?: string;
  patientInstructions?: string;
  supersededById?: string;
  qrCodeToken?: string;
  qrVerifyUrl?: string;
  signedAt?: string;
  createdAt: string;
  updatedAt: string;
  lines: PrescriptionLineDto[];
}

export interface CreatePrescriptionRequest {
  patientId: string;
  consultationId?: string;
  prescriptionType: PrescriptionType;
  validFrom: string;
  validUntil: string;
  pdBinocular?: number;
  pdOd?: number;
  pdOs?: number;
  prismOd?: string;
  prismOs?: string;
  lensType?: LensType;
  lensMaterial?: string;
  lensCoating?: string;
  frameRecommendation?: string;
  clinicalNotes?: string;
  patientInstructions?: string;
  lines: PrescriptionLineDto[];
}

export interface PrescriptionPdfResponse {
  downloadUrl: string;
  expiresAt: string;
}
