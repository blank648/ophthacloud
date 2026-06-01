import { apiGet } from '@/lib/apiClient';

export interface DashboardKpisDto {
  todayAppointments: { count: number; completed: number; pending: number };
  weekRevenue: { amount: number; currency: string; trendPercent: number };
  activePatients: { count: number; newThisMonth: number };
  pendingOrders: { count: number; overdueCount: number };
  pendingRecalls: { count: number };
  lowStockItems: { count: number };
  upcomingAppointments: { startAt: string; patientName: string; status: string; typeName: string }[];
}

export interface AppointmentSeriesData {
  period: string;
  total: number;
  completed: number;
  noShow: number;
  cancelled: number;
}

export interface AppointmentStatisticsDto {
  groupBy: string;
  series: AppointmentSeriesData[];
  byStatus: Record<string, number>;
  byType: Record<string, number>;
}

export interface RevenueSeriesData {
  period: string;
  total: number;
}

export interface RevenueStatisticsDto {
  groupBy: string;
  series: RevenueSeriesData[];
}

export interface IopTrendData {
  date: string;
  consultationId: string;
  od: { iop: number; method: string };
  os: { iop: number; method: string };
}

export interface IopTrendDto {
  patientId: string;
  patientName: string;
  series: IopTrendData[];
}

export interface DiagnosisData {
  icd10Code: string;
  description: string;
  count: number;
}

export interface RegistrationTrendData {
  period: string;
  count: number;
}

export interface PatientDemographicsDto {
  ageDistribution: Record<string, number>;
  genderDistribution: Record<string, number>;
  topDiagnoses: DiagnosisData[];
  registrationTrend?: RegistrationTrendData[];
}

export const reportsService = {
  getDashboardKpis: async (): Promise<DashboardKpisDto> => {
    return apiGet<DashboardKpisDto>('/api/v1/reports/dashboard-kpis');
  },

  getAppointmentStatistics: async (from: string, to: string, groupBy: string = 'month'): Promise<AppointmentStatisticsDto> => {
    return apiGet<AppointmentStatisticsDto>(`/api/v1/reports/appointments`, { from, to, groupBy });
  },

  getRevenueStatistics: async (from: string, to: string, groupBy: string = 'month'): Promise<RevenueStatisticsDto> => {
    return apiGet<RevenueStatisticsDto>(`/api/v1/reports/revenue`, { from, to, groupBy });
  },

  getIopTrends: async (patientId: string, from: string, to: string): Promise<IopTrendDto> => {
    return apiGet<IopTrendDto>(`/api/v1/reports/patients/${patientId}/iop-trends`, { from, to });
  },

  getPatientDemographics: async (): Promise<PatientDemographicsDto> => {
    return apiGet<PatientDemographicsDto>('/api/v1/reports/patients/demographics');
  }
};
