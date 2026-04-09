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

export const statusLabels: Record<AppointmentStatus, string> = {
  booked: 'Programat',
  confirmed: 'Confirmat',
  checked_in: 'Prezent',
  in_progress: 'În desfășurare',
  completed: 'Finalizat',
  no_show: 'Neprezentare',
  cancelled: 'Anulat',
};

export const statusStyles: Record<AppointmentStatus, { bg: string; text: string }> = {
  booked: { bg: '#EFF6FF', text: '#1D4ED8' },
  confirmed: { bg: '#ECFDF5', text: '#065F46' },
  checked_in: { bg: '#F0FDF4', text: '#15803D' },
  in_progress: { bg: '#FEF9C3', text: '#854D0E' },
  completed: { bg: '#ECFDF5', text: '#1A7F5A' },
  no_show: { bg: '#FEF2F2', text: '#991B1B' },
  cancelled: { bg: '#F3F4F6', text: '#6B7280' },
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

export const patients: Patient[] = [
  {
    id: 'OC-004821', firstName: 'Ion', lastName: 'Marinescu', name: 'Ion Marinescu',
    cnp: '1620315123456', age: 62, dob: '15.03.1964', gender: 'M',
    phone: '+40 722 345 678', email: 'ion.marinescu@email.ro',
    address: 'Str. Victoriei 42, Sector 1, București, 010061',
    primaryDiagnosis: 'Glaucom cu unghi deschis', icdCode: 'H40.1',
    clinicalFlags: ['GLAUCOM', 'DIABET'], lastVisit: '15.01.2026', nextAppointment: '29.03.2026',
    status: 'active', preferredLanguage: 'RO',
    emergencyContact: { name: 'Maria Marinescu', relationship: 'Soție', phone: '+40 733 456 789' },
    medicalHistory: {
      ophthalmicHistory: {
        firstVisitAge: 45, firstVisitReason: 'Screening IOP',
        conditions: ['Glaucom cu unghi deschis'], surgeries: [{ type: 'Trabeculoplastie SLT', year: 2022 }],
        glassesUse: true, contactLensUse: false, ocularAllergies: 'Niciuna',
      },
      systemicComorbidities: {
        diabetes: { type: 'II', durationYears: 12, hba1c: 7.2 },
        hypertension: true, hypertensionMed: 'Amlodipine 5mg',
        hyperlipidemia: true, migraineWithAura: false, osteoporosis: false,
      },
      medications: [
        { name: 'Timolol 0.5%', dose: '1 pic', frequency: 'x2/zi OD+OS', riskFlag: undefined },
        { name: 'Latanoprost 0.005%', dose: '1 pic', frequency: 'seara OD+OS', riskFlag: undefined },
        { name: 'Metformin', dose: '850mg', frequency: 'x2/zi', riskFlag: undefined },
        { name: 'Aspenter', dose: '75mg', frequency: 'x1/zi', riskFlag: '⚠ Risc chirurgical' },
      ],
      familyHistory: {
        glaucoma: { relative: 'Tată', ageAtDiagnosis: 55 },
        amd: false, diabeticRetinopathy: false, strabismus: false,
      },
    },
    consents: [
      { id: 'C1', name: 'Îngrijiri medicale', description: 'Consimțământ pentru tratament medical', mandatory: true, granted: true, dateSigned: '15.01.2024', expiresAt: '15.07.2025' },
      { id: 'C2', name: 'Procesare date GDPR', description: 'Conform Regulamentului (UE) 2016/679', mandatory: true, granted: true, dateSigned: '15.01.2024', expiresAt: '15.07.2025' },
      { id: 'C3', name: 'Marketing SMS/Email', description: 'Comunicări promoționale și informative', mandatory: false, granted: true, dateSigned: '15.01.2024' },
      { id: 'C4', name: 'Studii clinice', description: 'Participare la studii de cercetare', mandatory: false, granted: false },
      { id: 'C5', name: 'Telemedicină', description: 'Consultații la distanță prin video', mandatory: false, granted: true, dateSigned: '20.03.2025' },
    ],
    documents: [
      { id: 'DOC1', filename: 'Scrisoare_medicala_2026-01.pdf', type: 'scrisoare', date: '15.01.2026', size: '245 KB' },
      { id: 'DOC2', filename: 'OCT_disc_optic_OD_OS.dcm', type: 'dicom', date: '15.01.2026', size: '8.2 MB' },
      { id: 'DOC3', filename: 'Camp_vizual_24-2.dcm', type: 'dicom', date: '15.01.2026', size: '4.1 MB' },
      { id: 'DOC4', filename: 'Reteta_REC-20260115-012.pdf', type: 'reteta', date: '15.01.2026', size: '180 KB' },
      { id: 'DOC5', filename: 'Consimtamant_GDPR.pdf', type: 'consimtamant', date: '15.01.2024', size: '120 KB' },
    ],
    consultations: [
      { id: 'CONS-001', date: '15.01.2026', doctorName: 'Dr. Alexandru Popescu', diagnosis: 'Glaucom cu unghi deschis', icdCode: 'H40.1', type: 'Follow-up', duration: 25, summary: 'IOP OD: 26mmHg, OS: 22mmHg. Câmp vizual stabil. Se menține tratamentul topic. Control 3 luni.', followUpDate: '15.04.2026', signed: true },
      { id: 'CONS-002', date: '10.10.2025', doctorName: 'Dr. Alexandru Popescu', diagnosis: 'Glaucom cu unghi deschis', icdCode: 'H40.1', type: 'Follow-up', duration: 20, summary: 'IOP OD: 24mmHg, OS: 21mmHg. OCT RNFL stabil. Tratament continuat.', signed: true },
      { id: 'CONS-003', date: '15.07.2025', doctorName: 'Dr. Alexandru Popescu', diagnosis: 'Glaucom cu unghi deschis', icdCode: 'H40.1', type: 'Follow-up', duration: 30, summary: 'IOP OD: 23mmHg, OS: 20mmHg. VF: MD stabil -4.2 dB.', signed: true },
      { id: 'CONS-004', date: '20.04.2025', doctorName: 'Dr. Alexandru Popescu', diagnosis: 'Glaucom cu unghi deschis + Retinopatie diabetică NPDR', icdCode: 'H40.1', type: 'Consultație inițială', duration: 45, summary: 'Pacient transferat din altă clinică. Evaluare completă glaucom + retinopatie diabetică. SLT efectuat 2022.', signed: true },
    ],
    prescriptions: [
      { id: 'REC-20260115-012', date: '15.01.2026', doctorName: 'Dr. Alexandru Popescu', status: 'active', validUntil: '15.01.2027',
        od: { sph: -1.50, cyl: -0.75, axis: 90, add: 2.00 }, os: { sph: -1.25, cyl: -0.50, axis: 85, add: 2.00 },
        pdDistance: 63.5, pdNear: 60, lensType: 'Progresiv', material: 'Policarbonat', treatments: ['Anti-reflex', 'UV', 'Blue light filter'],
        notes: 'Miopie stabilă. Progresive recomandate pentru distanță + aproape.' },
      { id: 'REC-20250420-008', date: '20.04.2025', doctorName: 'Dr. Alexandru Popescu', status: 'expired', validUntil: '20.04.2026',
        od: { sph: -1.25, cyl: -0.75, axis: 88 }, os: { sph: -1.00, cyl: -0.50, axis: 82 },
        pdDistance: 63.5, lensType: 'Monofocal', material: 'CR-39', treatments: ['Anti-reflex'] },
    ],
  },
  {
    id: 'OC-003247', firstName: 'Maria', lastName: 'Constantin', name: 'Maria Constantin',
    cnp: '2790822234567', age: 47, dob: '22.08.1979', gender: 'F',
    phone: '+40 733 456 789', email: 'maria.constantin@email.ro',
    address: 'Bd. Unirii 18, Sector 3, București, 030167',
    primaryDiagnosis: 'Prezbiopie + Miopie', icdCode: 'H52.4',
    clinicalFlags: ['NORMAL'], lastVisit: '10.02.2026', status: 'active', preferredLanguage: 'RO',
    medicalHistory: {
      ophthalmicHistory: { firstVisitAge: 25, firstVisitReason: 'Miopie', conditions: ['Miopie', 'Prezbiopie'], surgeries: [], glassesUse: true, contactLensUse: true },
      systemicComorbidities: { hypertension: false, hyperlipidemia: false, migraineWithAura: false, osteoporosis: false },
      medications: [],
      familyHistory: { amd: false, diabeticRetinopathy: false, strabismus: false },
    },
    prescriptions: [
      { id: 'REC-20260210-019', date: '10.02.2026', doctorName: 'Dr. Ioana Mihailescu', status: 'active', validUntil: '10.02.2027',
        od: { sph: -3.00, cyl: -0.25, axis: 175, add: 1.50 }, os: { sph: -2.75, cyl: -0.50, axis: 5, add: 1.50 },
        pdDistance: 61, pdNear: 58, lensType: 'Progresiv', material: 'Index înalt 1.67', treatments: ['Anti-reflex', 'Fotocromatice'],
        notes: 'Progresive adaptare bună. Control la 12 luni.' },
    ],
    consultations: [
      { id: 'CONS-010', date: '10.02.2026', doctorName: 'Dr. Ioana Mihailescu', diagnosis: 'Prezbiopie + Miopie moderată', icdCode: 'H52.4', type: 'Follow-up', duration: 20, summary: 'Refracție stabilă. Add crescut la +1.50. Progresive noi recomandate.', signed: true },
    ],
  },
  {
    id: 'OC-005410', firstName: 'Andrei', lastName: 'Popescu', name: 'Andrei Popescu',
    cnp: '1920414345678', age: 34, dob: '14.04.1992', gender: 'M',
    phone: '+40 744 567 890', email: 'andrei.popescu@email.ro',
    address: 'Str. Libertății 7, Timișoara, 300077',
    primaryDiagnosis: 'Astigmatism + Keratoconus suspect', icdCode: 'H52.2',
    clinicalFlags: ['KERATOCONUS'], lastVisit: '05.03.2026', status: 'active', preferredLanguage: 'RO',
    medicalHistory: {
      ophthalmicHistory: { firstVisitAge: 18, firstVisitReason: 'Vedere neclară', conditions: ['Astigmatism', 'Keratoconus suspect'], surgeries: [], glassesUse: true, contactLensUse: true },
      systemicComorbidities: { hypertension: false, hyperlipidemia: false, migraineWithAura: true, osteoporosis: false },
      medications: [],
      familyHistory: { amd: false, diabeticRetinopathy: false, strabismus: false },
    },
    prescriptions: [
      { id: 'REC-20260305-024', date: '05.03.2026', doctorName: 'Dr. Alexandru Popescu', status: 'active', validUntil: '05.03.2027',
        od: { sph: -1.75, cyl: -2.50, axis: 15 }, os: { sph: -1.50, cyl: -1.75, axis: 170 },
        pdDistance: 65, lensType: 'Monofocal', material: 'Trivex', treatments: ['Anti-reflex', 'Anti-zgârieturi'],
        notes: 'Keratoconus suspect OD. Monitorizare topografie la 6 luni. Contraindicat LASIK.' },
    ],
    consultations: [
      { id: 'CONS-020', date: '05.03.2026', doctorName: 'Dr. Alexandru Popescu', diagnosis: 'Astigmatism + Keratoconus suspect', icdCode: 'H18.6', type: 'Follow-up', duration: 35, summary: 'Topografie: K1 46.5D, K2 49.2D OD. Keratoconus suspect. Monitorizare. Contraindicat chirurgie refractivă.', signed: true },
    ],
  },
  {
    id: 'OC-002765', firstName: 'Elena', lastName: 'Dumitrescu', name: 'Elena Dumitrescu',
    cnp: '2550718456789', age: 71, dob: '18.07.1955', gender: 'F',
    phone: '+40 755 678 901', email: 'elena.d@email.ro',
    address: 'Str. Mihai Eminescu 33, Cluj-Napoca, 400010',
    primaryDiagnosis: 'AMD uscat + Cataractă senilă', icdCode: 'H35.31',
    clinicalFlags: ['AMD', 'POST-OP'], lastVisit: '08.01.2026', status: 'active', preferredLanguage: 'RO',
    medicalHistory: {
      ophthalmicHistory: { firstVisitAge: 50, firstVisitReason: 'Prezbiopie', conditions: ['AMD uscat', 'Cataractă senilă nucleară gr.2'], surgeries: [], glassesUse: true, contactLensUse: false },
      systemicComorbidities: { hypertension: true, hypertensionMed: 'Perindopril 5mg', hyperlipidemia: true, migraineWithAura: false, osteoporosis: true },
      medications: [
        { name: 'Perindopril', dose: '5mg', frequency: 'x1/zi' },
        { name: 'Atorvastatin', dose: '20mg', frequency: 'x1/zi' },
        { name: 'Preservision AREDS2', dose: '1 caps', frequency: 'x2/zi' },
      ],
      familyHistory: { amd: true, diabeticRetinopathy: false, strabismus: false },
    },
    prescriptions: [
      { id: 'REC-20260108-005', date: '08.01.2026', doctorName: 'Dr. Ioana Mihailescu', status: 'active', validUntil: '08.01.2027',
        od: { sph: +1.75, cyl: -0.50, axis: 90, add: 2.50 }, os: { sph: +2.00, cyl: -0.75, axis: 85, add: 2.50 },
        pdDistance: 60, pdNear: 57, lensType: 'Progresiv', material: 'CR-39', treatments: ['Anti-reflex', 'UV'],
        notes: 'AMD uscat — monitorizare OCT la 6 luni. Suplimentare AREDS2.' },
    ],
    consultations: [
      { id: 'CONS-030', date: '08.01.2026', doctorName: 'Dr. Ioana Mihailescu', diagnosis: 'AMD uscat + Cataractă nucleară gr.2', icdCode: 'H35.31', type: 'Follow-up', duration: 30, summary: 'OCT macular: drusen moi bilateral. Fără fluid. Cataractă nucleară gr.2 bilateral. Pre-op biometrie programată.', followUpDate: '08.07.2026', signed: true },
    ],
  },
  {
    id: 'OC-008123', firstName: 'Mihai', lastName: 'Georgescu', name: 'Mihai Georgescu',
    cnp: '5180903567890', age: 8, dob: '03.09.2018', gender: 'M',
    phone: '+40 766 789 012', email: 'parent.georgescu@email.ro',
    address: 'Str. Primăverii 15, Sector 1, București, 011973',
    primaryDiagnosis: 'Ambliopie + Strabism convergent', icdCode: 'H53.01',
    clinicalFlags: ['PEDIATRIC'], lastVisit: '20.03.2026', status: 'active', preferredLanguage: 'RO',
    emergencyContact: { name: 'Ana Georgescu', relationship: 'Mamă', phone: '+40 766 789 012' },
    medicalHistory: {
      ophthalmicHistory: { firstVisitAge: 4, firstVisitReason: 'Screening pediatric', conditions: ['Ambliopie OD', 'Esotropie'], surgeries: [], glassesUse: true, contactLensUse: false },
      systemicComorbidities: { hypertension: false, hyperlipidemia: false, migraineWithAura: false, osteoporosis: false },
      medications: [],
      familyHistory: { amd: false, diabeticRetinopathy: false, strabismus: true },
    },
    prescriptions: [
      { id: 'REC-20260320-031', date: '20.03.2026', doctorName: 'Dr. Alexandru Popescu', status: 'active', validUntil: '20.09.2026',
        od: { sph: +3.00, cyl: -1.00, axis: 90 }, os: { sph: +1.50, cyl: -0.50, axis: 85 },
        pdDistance: 52, lensType: 'Monofocal', material: 'Policarbonat', treatments: ['Anti-zgârieturi', 'UV'],
        notes: 'Ambliopie OD în tratament. Ocluzie OS 4h/zi. Control la 3 luni.' },
    ],
    consultations: [
      { id: 'CONS-040', date: '20.03.2026', doctorName: 'Dr. Alexandru Popescu', diagnosis: 'Ambliopie OD + Esotropie', icdCode: 'H53.01', type: 'Follow-up', duration: 25, summary: 'VA OD: 6/12 (ameliorare de la 6/18). VA OS: 6/6. Esotropie 12DP la distanță. Ocluzie continuă. Control 3 luni.', followUpDate: '20.06.2026', signed: true },
    ],
  },
];

// ===== Resources =====

export const doctors = [
  { id: 'DR-001', name: 'Dr. Alexandru Popescu', specialty: 'Oftalmolog', consultations: 148, revenue: 52400 },
  { id: 'DR-002', name: 'Dr. Ioana Mihailescu', specialty: 'Oftalmolog', consultations: 132, revenue: 47800 },
  { id: 'DR-003', name: 'Dr. Vlad Niculescu', specialty: 'Optometrist', consultations: 165, revenue: 38200 },
];

export const resources = [
  { id: 'DR-001', name: 'Dr. Alexandru Popescu', type: 'doctor' as const, specialty: 'Oftalmolog' },
  { id: 'DR-002', name: 'Dr. Ioana Mihailescu', type: 'doctor' as const, specialty: 'Oftalmolog' },
  { id: 'OPT-001', name: 'Optometrist Radu', type: 'optometrist' as const, specialty: 'Optometrie' },
  { id: 'OPT-002', name: 'Optician Gheorghe', type: 'optician' as const, specialty: 'Optician' },
  { id: 'EQ-001', name: 'OCT Topcon Triton', type: 'equipment' as const, specialty: 'Echipament' },
  { id: 'EQ-002', name: 'Câmp Vizual Nidek', type: 'equipment' as const, specialty: 'Echipament' },
];

// ===== Appointments =====

export const todayAppointments: Appointment[] = [
  { id: 'APT-001', patientId: 'OC-004821', patientName: 'Ion Marinescu', patientAge: 62, doctorId: 'DR-001', doctorName: 'Dr. Alexandru Popescu', type: 'initial', date: '29.03.2026', time: '09:00', duration: 45, status: 'confirmed', clinicalFlags: ['GLAUCOM', 'DIABET'] },
  { id: 'APT-002', patientId: 'OC-003247', patientName: 'Maria Constantin', patientAge: 47, doctorId: 'DR-001', doctorName: 'Dr. Alexandru Popescu', type: 'followup', date: '29.03.2026', time: '09:45', duration: 25, status: 'completed', clinicalFlags: ['NORMAL'] },
  { id: 'APT-003', patientId: 'OC-005410', patientName: 'Andrei Popescu', patientAge: 34, doctorId: 'EQ-001', doctorName: 'Echipament Tomey', type: 'investigation', date: '29.03.2026', time: '10:30', duration: 20, status: 'booked', clinicalFlags: ['KERATOCONUS'], resource: 'OCT Topcon Triton' },
  { id: 'APT-004', patientId: 'OC-002765', patientName: 'Elena Dumitrescu', patientAge: 71, doctorId: 'DR-002', doctorName: 'Dr. Ioana Mihailescu', type: 'followup', date: '29.03.2026', time: '11:00', duration: 30, status: 'checked_in', clinicalFlags: ['AMD'] },
  { id: 'APT-005', patientId: 'OC-008123', patientName: 'Mihai Georgescu', patientAge: 8, doctorId: 'DR-001', doctorName: 'Dr. Alexandru Popescu', type: 'followup', date: '29.03.2026', time: '11:30', duration: 25, status: 'booked', clinicalFlags: ['PEDIATRIC'] },
  { id: 'APT-006', patientId: 'OC-004821', patientName: 'Ion Marinescu', patientAge: 62, doctorId: 'DR-001', doctorName: 'Dr. Alexandru Popescu', type: 'telemedicine', date: '29.03.2026', time: '14:00', duration: 20, status: 'confirmed', clinicalFlags: ['GLAUCOM'] },
  { id: 'APT-007', patientId: 'OC-003247', patientName: 'Maria Constantin', patientAge: 47, doctorId: 'DR-002', doctorName: 'Dr. Ioana Mihailescu', type: 'optical_counseling', date: '29.03.2026', time: '14:30', duration: 20, status: 'booked', clinicalFlags: ['NORMAL'] },
  { id: 'APT-008', patientId: 'OC-005410', patientName: 'Andrei Popescu', patientAge: 34, doctorId: 'DR-001', doctorName: 'Dr. Alexandru Popescu', type: 'followup', date: '29.03.2026', time: '15:00', duration: 30, status: 'booked', clinicalFlags: ['KERATOCONUS'] },
];

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

export const opticalOrders: OpticalOrder[] = [
  { id: 'ORD-2026-0041', patientId: 'OC-004821', patientName: 'Ion Marinescu', prescriptionId: 'REC-20260115-012', frameType: 'Silhouette SPX', lensType: 'Progresive Varilux Comfort', status: 'lab', createdDate: '20.03.2026', slaStatus: 'on_time', priority: 'normal', labName: 'OptiLab SRL', totalPrice: 1850 },
  { id: 'ORD-2026-0039', patientId: 'OC-003247', patientName: 'Maria Constantin', prescriptionId: 'REC-20260210-019', frameType: 'Ray-Ban RB5154', lensType: 'Progresive Nikon Presio', status: 'done', createdDate: '12.02.2026', slaStatus: 'on_time', priority: 'normal', labName: 'OptiLab SRL', totalPrice: 2200 },
  { id: 'ORD-2026-0038', patientId: 'OC-005410', patientName: 'Andrei Popescu', prescriptionId: 'REC-20260305-024', frameType: 'Oakley OX8046', lensType: 'Monofocale Zeiss', status: 'fitting', createdDate: '08.03.2026', slaStatus: 'on_time', priority: 'normal', labName: 'Zeiss Vision RO', totalPrice: 980 },
  { id: 'ORD-2026-0040', patientId: 'OC-002765', patientName: 'Elena Dumitrescu', prescriptionId: 'REC-20260108-005', frameType: 'Tom Ford FT5401', lensType: 'Progresive Hoya', status: 'qc', createdDate: '15.01.2026', slaStatus: 'overdue', priority: 'urgent', labName: 'Hoya RO', totalPrice: 1650 },
  { id: 'ORD-2026-0042', patientId: 'OC-008123', patientName: 'Mihai Georgescu', prescriptionId: 'REC-20260320-031', frameType: 'Nano Vista Kids', lensType: 'Monofocale Essilor', status: 'new', createdDate: '22.03.2026', slaStatus: 'on_time', priority: 'normal', labName: 'Essilor RO', totalPrice: 450 },
];

// ===== Charts Data =====

export const iopTrendData = [
  { month: 'Apr', 'Ion Marinescu': 24, 'Andrei Popescu': 16, 'Elena Dumitrescu': 16, 'Maria Constantin': 18, 'Mihai Georgescu': 15 },
  { month: 'Mai', 'Ion Marinescu': 25, 'Andrei Popescu': 15, 'Elena Dumitrescu': 17, 'Maria Constantin': 17, 'Mihai Georgescu': 16 },
  { month: 'Iun', 'Ion Marinescu': 23, 'Andrei Popescu': 16, 'Elena Dumitrescu': 16, 'Maria Constantin': 19, 'Mihai Georgescu': 15 },
  { month: 'Iul', 'Ion Marinescu': 26, 'Andrei Popescu': 15, 'Elena Dumitrescu': 15, 'Maria Constantin': 18, 'Mihai Georgescu': 14 },
  { month: 'Aug', 'Ion Marinescu': 24, 'Andrei Popescu': 16, 'Elena Dumitrescu': 16, 'Maria Constantin': 17, 'Mihai Georgescu': 15 },
  { month: 'Sep', 'Ion Marinescu': 22, 'Andrei Popescu': 15, 'Elena Dumitrescu': 17, 'Maria Constantin': 18, 'Mihai Georgescu': 16 },
  { month: 'Oct', 'Ion Marinescu': 23, 'Andrei Popescu': 16, 'Elena Dumitrescu': 16, 'Maria Constantin': 19, 'Mihai Georgescu': 15 },
  { month: 'Nov', 'Ion Marinescu': 25, 'Andrei Popescu': 15, 'Elena Dumitrescu': 15, 'Maria Constantin': 17, 'Mihai Georgescu': 14 },
  { month: 'Dec', 'Ion Marinescu': 24, 'Andrei Popescu': 16, 'Elena Dumitrescu': 16, 'Maria Constantin': 18, 'Mihai Georgescu': 16 },
  { month: 'Ian', 'Ion Marinescu': 26, 'Andrei Popescu': 15, 'Elena Dumitrescu': 17, 'Maria Constantin': 19, 'Mihai Georgescu': 15 },
  { month: 'Feb', 'Ion Marinescu': 23, 'Andrei Popescu': 16, 'Elena Dumitrescu': 16, 'Maria Constantin': 18, 'Mihai Georgescu': 14 },
  { month: 'Mar', 'Ion Marinescu': 26, 'Andrei Popescu': 15, 'Elena Dumitrescu': 15, 'Maria Constantin': 17, 'Mihai Georgescu': 15 },
];

export const revenueData = [
  { month: 'Apr', medical: 42000, optical: 28000 },
  { month: 'Mai', medical: 45000, optical: 31000 },
  { month: 'Iun', medical: 38000, optical: 26000 },
  { month: 'Iul', medical: 41000, optical: 29000 },
  { month: 'Aug', medical: 35000, optical: 24000 },
  { month: 'Sep', medical: 48000, optical: 33000 },
  { month: 'Oct', medical: 52000, optical: 36000 },
  { month: 'Nov', medical: 46000, optical: 30000 },
  { month: 'Dec', medical: 39000, optical: 27000 },
  { month: 'Ian', medical: 50000, optical: 34000 },
  { month: 'Feb', medical: 47000, optical: 32000 },
  { month: 'Mar', medical: 53000, optical: 37000 },
];

export const serviceMixData = [
  { name: 'Consultații', value: 420, color: '#13759C' },
  { name: 'Investigații', value: 185, color: '#8B5CF6' },
  { name: 'Proceduri', value: 62, color: '#EF4444' },
  { name: 'Optic', value: 302, color: '#F59E0B' },
];

export const topDiagnoses = [
  { name: 'H52.4 — Prezbiopie', count: 187, pct: 31 },
  { name: 'H52.1 — Miopie', count: 144, pct: 24 },
  { name: 'H40.x — Glaucom suspect', count: 108, pct: 18 },
  { name: 'H35.3 — AMD', count: 84, pct: 14 },
  { name: 'H52.2 — Astigmatism', count: 78, pct: 13 },
];

// ===== Patient VA/IOP chart data (for patient profile) =====

export const patientVAData = [
  { month: 'Apr 25', vaOD: 6/9, vaOS: 6/6, iopOD: 24, iopOS: 20 },
  { month: 'Iul 25', vaOD: 6/9, vaOS: 6/6, iopOD: 23, iopOS: 20 },
  { month: 'Oct 25', vaOD: 6/12, vaOS: 6/9, iopOD: 24, iopOS: 21 },
  { month: 'Ian 26', vaOD: 6/12, vaOS: 6/9, iopOD: 26, iopOS: 22 },
];

// ===== Service Catalog =====

export interface ServiceItem {
  code: string;
  name: string;
  category: 'medical' | 'investigation' | 'procedure' | 'optical';
  duration: number;
  priceMin: number;
  priceMax: number;
}

export const serviceCatalog: ServiceItem[] = [
  { code: 'MED001', name: 'Consultație inițială oftalmolog', category: 'medical', duration: 45, priceMin: 150, priceMax: 250 },
  { code: 'MED002', name: 'Consultație follow-up oftalmolog', category: 'medical', duration: 25, priceMin: 80, priceMax: 120 },
  { code: 'MED003', name: 'Consultație optometrist', category: 'medical', duration: 30, priceMin: 60, priceMax: 100 },
  { code: 'INV001', name: 'OCT macular', category: 'investigation', duration: 15, priceMin: 50, priceMax: 80 },
  { code: 'INV002', name: 'OCT disc optic', category: 'investigation', duration: 15, priceMin: 40, priceMax: 60 },
  { code: 'INV003', name: 'Câmp vizual', category: 'investigation', duration: 20, priceMin: 60, priceMax: 100 },
  { code: 'INV004', name: 'Topografie corneană', category: 'investigation', duration: 15, priceMin: 40, priceMax: 60 },
  { code: 'INV005', name: 'Angiografie fluoresceină', category: 'investigation', duration: 30, priceMin: 150, priceMax: 250 },
  { code: 'PROC001', name: 'Injecție IVT (anti-VEGF)', category: 'procedure', duration: 30, priceMin: 300, priceMax: 500 },
  { code: 'PROC002', name: 'Procedură laser', category: 'procedure', duration: 20, priceMin: 200, priceMax: 400 },
  { code: 'SRV001', name: 'Consiliere optică', category: 'optical', duration: 20, priceMin: 30, priceMax: 50 },
  { code: 'PKG001', name: 'Pachet anual (4 cons. + 2 inv.)', category: 'medical', duration: 0, priceMin: 800, priceMax: 800 },
];

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

export const notificationRules: NotificationRule[] = [
  { id: 'NR-01', type: 'Confirmare programare', channel: 'SMS', timing: 'Imediat după booking', template: 'Ați fost programat la {{doctor}}, {{data_ora}}, Clinica {{clinica}}.', enabled: true, category: 'appointment' },
  { id: 'NR-02', type: 'Reamintire −3 zile', channel: 'SMS', timing: '3 zile înainte', template: 'Vă reamintim programarea de pe {{data}} la ora {{ora}}.', enabled: true, category: 'appointment' },
  { id: 'NR-03', type: 'Reamintire −24 ore', channel: 'SMS', timing: '24 ore înainte', template: 'Programare mâine ora {{ora}}. Clinica {{adresa}}.', enabled: true, category: 'appointment' },
  { id: 'NR-04', type: 'Reamintire −2 ore', channel: 'SMS', timing: '2 ore înainte', template: 'Programare în 2 ore! Clinica {{adresa}}.', enabled: true, category: 'appointment' },
  { id: 'NR-05', type: 'Follow-up 7 zile', channel: 'SMS', timing: '7 zile după vizită', template: 'Cum vă adaptați? Aveți întrebări? Răspundeți sau apelați: {{tel}}.', enabled: true, category: 'post_service' },
  { id: 'NR-06', type: 'Studiu satisfacție', channel: 'Email', timing: '14 zile după vizită', template: 'Evaluați experiența dvs. la Clinica {{clinica}}: {{link_nps}}', enabled: true, category: 'post_service' },
  { id: 'NR-07', type: 'Rețetă semnată', channel: 'SMS+Email', timing: 'La semnare', template: 'Rețeta dvs. este gata. Descărcați din portal.', enabled: true, category: 'prescription' },
  { id: 'NR-08', type: 'Rețetă expiră', channel: 'Email', timing: '1 lună înainte', template: 'Rețeta dvs. expiră în 30 de zile. Solicitați reînnoire.', enabled: true, category: 'prescription' },
  { id: 'NR-09', type: 'Comandă trimisă lab', channel: 'SMS', timing: 'La transmitere', template: 'Ochelarii dvs. sunt în producție. Estimare: {{data_estimata}}.', enabled: true, category: 'order' },
  { id: 'NR-10', type: 'Ochelari gata', channel: 'SMS+Email', timing: 'La finalizare', template: 'Ochelarii dvs. sunt gata! Veniți la {{adresa}}.', enabled: true, category: 'order' },
];

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

export const auditLog: AuditEntry[] = [
  { id: 'AUD-001', timestamp: '29.03.2026 08:32:14', user: 'Dr. Alexandru Popescu', role: 'Doctor', action: 'Vizualizare', module: 'Pacienți', entity: 'Ion Marinescu (OC-004821)', ip: '192.168.1.***', details: 'Acces dosar medical' },
  { id: 'AUD-002', timestamp: '29.03.2026 08:45:22', user: 'Dr. Alexandru Popescu', role: 'Doctor', action: 'Editare', module: 'EMR', entity: 'Consultație CONS-001', ip: '192.168.1.***', details: 'Actualizare IOP OD: 26mmHg' },
  { id: 'AUD-003', timestamp: '29.03.2026 09:10:05', user: 'Ana Recepție', role: 'Recepție', action: 'Creare', module: 'Programări', entity: 'APT-008', ip: '192.168.1.***', details: 'Programare nouă pentru Andrei Popescu' },
  { id: 'AUD-004', timestamp: '29.03.2026 09:15:33', user: 'Dr. Alexandru Popescu', role: 'Doctor', action: 'Semnătură digitală', module: 'EMR', entity: 'Consultație CONS-001', ip: '192.168.1.***', details: 'Semnare digitală consultație' },
  { id: 'AUD-005', timestamp: '29.03.2026 09:30:11', user: 'Dr. Alexandru Popescu', role: 'Doctor', action: 'Creare', module: 'Rețete', entity: 'REC-20260329-047', ip: '192.168.1.***', details: 'Rețetă nouă emisă' },
  { id: 'AUD-006', timestamp: '28.03.2026 16:22:45', user: 'Optician Gheorghe', role: 'Optician', action: 'Editare', module: 'ERP Optic', entity: 'ORD-2026-0041', ip: '192.168.1.***', details: 'Status actualizat: Lab în lucru' },
  { id: 'AUD-007', timestamp: '28.03.2026 14:10:00', user: 'Manager Clinică', role: 'Manager', action: 'Export', module: 'Rapoarte', entity: 'Raport revenue lunar', ip: '192.168.1.***', details: 'Export PDF rapoarte comerciale' },
  { id: 'AUD-008', timestamp: '28.03.2026 10:05:18', user: 'Ion Marinescu', role: 'Pacient', action: 'Vizualizare', module: 'Portal', entity: 'Dosar medical propriu', ip: '86.105.2.***', details: 'Acces portal pacient' },
];

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

export const stockItems: StockItem[] = [
  { id: 'STK-001', brand: 'Ray-Ban', model: 'RB5154', category: 'rame', size: '51-21-145', color: 'Negru', costPrice: 320, retailPrice: 680, currentStock: 8, minStock: 3 },
  { id: 'STK-002', brand: 'Silhouette', model: 'SPX 1578', category: 'rame', size: '53-17-140', color: 'Auriu', costPrice: 450, retailPrice: 950, currentStock: 2, minStock: 3 },
  { id: 'STK-003', brand: 'Oakley', model: 'OX8046', category: 'rame', size: '55-18-143', color: 'Mat Negru', costPrice: 280, retailPrice: 590, currentStock: 5, minStock: 2 },
  { id: 'STK-004', brand: 'Essilor', model: 'Varilux Comfort', category: 'lentile', costPrice: 180, retailPrice: 420, currentStock: 12, minStock: 5 },
  { id: 'STK-005', brand: 'Zeiss', model: 'SmartLife Individual', category: 'lentile', costPrice: 250, retailPrice: 580, currentStock: 4, minStock: 5 },
  { id: 'STK-006', brand: 'Hoya', model: 'iD MyStyle', category: 'lentile', costPrice: 220, retailPrice: 500, currentStock: 7, minStock: 4 },
  { id: 'STK-007', brand: 'Acuvue', model: 'Oasys 1-Day', category: 'lentile_contact', costPrice: 22, retailPrice: 45, currentStock: 30, minStock: 10 },
  { id: 'STK-008', brand: 'Bausch+Lomb', model: 'SofLens Daily', category: 'lentile_contact', costPrice: 18, retailPrice: 38, currentStock: 25, minStock: 10 },
];

// Helper to find patient by ID
export const getPatientById = (id: string): Patient | undefined => patients.find(p => p.id === id);
