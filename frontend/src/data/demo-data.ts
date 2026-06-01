// ===== Types =====

export interface Patient {
  id: string;
  name: string;
  firstName: string;
  lastName: string;
  cnp: string;
  age: number;
  dob: string;
  gender: 'M' | 'F';
  phone: string;
  email: string;
  address: string;
  primaryDiagnosis: string;
  icdCode: string;
  clinicalFlags: string[];
  lastVisit: string;
  nextAppointment?: string;
  status: 'active' | 'inactive';
  preferredLanguage: 'RO' | 'EN';
  emergencyContact?: { name: string; relationship: string; phone: string };
  medicalHistory?: MedicalHistory;
  consents?: Consent[];
  documents?: PatientDocument[];
  consultations?: ConsultationEntry[];
  prescriptions?: Prescription[];
}

export interface MedicalHistory {
  ophthalmicHistory: {
    firstVisitAge?: number;
    firstVisitReason?: string;
    conditions: string[];
    surgeries: { type: string; year: number }[];
    glassesUse: boolean;
    contactLensUse: boolean;
    ocularAllergies?: string;
  };
  systemicComorbidities: {
    diabetes?: { type: 'I' | 'II'; durationYears: number; hba1c: number };
    hypertension: boolean;
    hypertensionMed?: string;
    hyperlipidemia: boolean;
    autoimmune?: string;
    migraineWithAura: boolean;
    osteoporosis: boolean;
  };
  medications: { name: string; dose: string; frequency: string; riskFlag?: string }[];
  familyHistory: {
    glaucoma?: { relative: string; ageAtDiagnosis: number };
    amd: boolean;
    diabeticRetinopathy: boolean;
    strabismus: boolean;
  };
}

export interface Consent {
  id: string;
  name: string;
  description: string;
  mandatory: boolean;
  granted: boolean;
  dateSigned?: string;
  expiresAt?: string;
}

export interface PatientDocument {
  id: string;
  filename: string;
  type: 'scrisoare' | 'reteta' | 'referire' | 'certificat' | 'investigatie' | 'dicom' | 'consimtamant';
  date: string;
  size: string;
}

export interface ConsultationEntry {
  id: string;
  date: string;
  doctorName: string;
  diagnosis: string;
  icdCode: string;
  type: string;
  duration: number;
  summary: string;
  followUpDate?: string;
  signed: boolean;
}

export interface Prescription {
  id: string;
  date: string;
  doctorName: string;
  status: 'active' | 'expired' | 'cancelled' | 'revised';
  validUntil: string;
  od: { sph: number; cyl: number; axis: number; add?: number };
  os: { sph: number; cyl: number; axis: number; add?: number };
  pdDistance: number;
  pdNear?: number;
  lensType?: string;
  material?: string;
  treatments?: string[];
  notes?: string;
}

export interface Appointment {
  id: string;
  patientId: string;
  patientName: string;
  patientAge: number;
  doctorId: string;
  doctorName: string;
  type: AppointmentType;
  date: string;
  time: string;
  duration: number;
  status: AppointmentStatus;
  clinicalFlags: string[];
  notes?: string;
  resource?: string;
}

export type AppointmentType = 'initial' | 'followup' | 'investigation' | 'procedure' | 'optical_fitting' | 'optical_counseling' | 'telemedicine';
export type AppointmentStatus = 'booked' | 'confirmed' | 'checked_in' | 'in_progress' | 'completed' | 'no_show' | 'cancelled';

export const appointmentTypeLabels: Record<AppointmentType, string> = {
  initial: 'Consultație inițială',
  followup: 'Follow-up',
  investigation: 'Investigație',
  procedure: 'Procedură',
  optical_fitting: 'Montaj ochelari',
  optical_counseling: 'Consiliere optică',
  telemedicine: 'Telemedicină',
};

export const appointmentTypeColors: Record<AppointmentType, string> = {
  initial: '#13759C',
  followup: '#10B981',
  investigation: '#8B5CF6',
  procedure: '#EF4444',
  optical_fitting: '#F59E0B',
  optical_counseling: '#06B6D4',
  telemedicine: '#6366F1',
};

export const appointmentTypeDurations: Record<AppointmentType, { min: number; max: number }> = {
  initial: { min: 40, max: 60 },
  followup: { min: 20, max: 30 },
  investigation: { min: 15, max: 30 },
  procedure: { min: 30, max: 45 },
  optical_fitting: { min: 20, max: 30 },
  optical_counseling: { min: 15, max: 20 },
  telemedicine: { min: 15, max: 20 },
};

export const statusLabels: Record<string, string> = {
  BOOKED: 'Programat',
  CONFIRMED: 'Confirmat',
  CHECKED_IN: 'Prezent',
  IN_PROGRESS: 'În desfășurare',
  COMPLETED: 'Finalizat',
  NO_SHOW: 'Neprezentare',
  CANCELLED: 'Anulat',
};

export const statusStyles: Record<string, { bg: string; text: string }> = {
  BOOKED: { bg: '#EFF6FF', text: '#1D4ED8' },
  CONFIRMED: { bg: '#ECFDF5', text: '#065F46' },
  CHECKED_IN: { bg: '#F0FDF4', text: '#15803D' },
  IN_PROGRESS: { bg: '#FEF9C3', text: '#854D0E' },
  COMPLETED: { bg: '#ECFDF5', text: '#1A7F5A' },
  NO_SHOW: { bg: '#FEF2F2', text: '#991B1B' },
  CANCELLED: { bg: '#F3F4F6', text: '#6B7280' },
};

export const clinicalFlagStyles: Record<string, { bg: string; text: string; border: string }> = {
  GLAUCOM: { bg: '#FEE2E2', text: '#991B1B', border: '#FECACA' },
  DIABET: { bg: '#FEF3C7', text: '#92400E', border: '#FDE68A' },
  AMD: { bg: '#EDE9FE', text: '#5B21B6', border: '#DDD6FE' },
  'POST-OP': { bg: '#DBEAFE', text: '#1E40AF', border: '#BFDBFE' },
  KERATOCONUS: { bg: '#FCE7F3', text: '#9D174D', border: '#FBCFE8' },
  NORMAL: { bg: '#ECFDF5', text: '#065F46', border: '#A7F3D0' },
  PEDIATRIC: { bg: '#FEF3C7', text: '#92400E', border: '#FDE68A' },
};

export const prescriptionStatusStyles: Record<string, { bg: string; text: string }> = {
  active: { bg: '#ECFDF5', text: '#1A7F5A' },
  expired: { bg: '#FEF2F2', text: '#991B1B' },
  cancelled: { bg: '#F3F4F6', text: '#6B7280' },
  revised: { bg: '#EFF6FF', text: '#1D4ED8' },
};

// ===== Demo Patients (spec-exact) =====

export const patients: Patient[] = [];

// ===== Resources =====

export const doctors: any[] = [];

export const resources: any[] = [];

// ===== Appointments =====

export const todayAppointments: Appointment[] = [];

// ===== Optical Orders =====

export interface OpticalOrder {
  id: string;
  patientId: string;
  patientName: string;
  prescriptionId: string;
  frameType: string;
  lensType: string;
  status: 'new' | 'lab' | 'qc' | 'fitting' | 'done';
  createdDate: string;
  slaStatus: 'on_time' | 'overdue';
  priority: 'normal' | 'urgent';
  labName?: string;
  totalPrice?: number;
}

export const opticalOrders: OpticalOrder[] = [];

// ===== Charts Data =====

export const iopTrendData: any[] = [];
export const revenueData: any[] = [];
export const serviceMixData: any[] = [];
export const topDiagnoses: any[] = [];

// ===== Patient VA/IOP chart data (for patient profile) =====

export const patientVAData: any[] = [];

// ===== Service Catalog =====

export interface ServiceItem {
  code: string;
  name: string;
  category: 'medical' | 'investigation' | 'procedure' | 'optical';
  duration: number;
  priceMin: number;
  priceMax: number;
}

export const serviceCatalog: ServiceItem[] = [];

// ===== Notification Rules =====

export interface NotificationRule {
  id: string;
  type: string;
  channel: 'SMS' | 'Email' | 'SMS+Email';
  timing: string;
  template: string;
  enabled: boolean;
  category: 'appointment' | 'post_service' | 'prescription' | 'order' | 'recall';
}

export const notificationRules: NotificationRule[] = [];

// ===== Audit Log =====

export interface AuditEntry {
  id: string;
  timestamp: string;
  user: string;
  role: string;
  action: string;
  module: string;
  entity: string;
  ip: string;
  details: string;
}

export const auditLog: AuditEntry[] = [];

// ===== Stock / Inventory =====

export interface StockItem {
  id: string;
  brand: string;
  model: string;
  category: 'rame' | 'lentile' | 'lentile_contact' | 'consumabile';
  size?: string;
  color?: string;
  costPrice: number;
  retailPrice: number;
  currentStock: number;
  minStock: number;
}

export const stockItems: StockItem[] = [];

// Helper to find patient by ID
export const getPatientById = (id: string): Patient | undefined => patients.find(p => p.id === id);
