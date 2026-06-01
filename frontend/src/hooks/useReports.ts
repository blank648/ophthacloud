import { useQuery } from '@tanstack/react-query';
import { reportsService } from '@/services/reportsService';

export const reportsKeys = {
  all: ['reports'] as const,
  kpis: () => ['reports', 'kpis'] as const,
  appointments: (from: string, to: string, groupBy: string) => ['reports', 'appointments', from, to, groupBy] as const,
  revenue: (from: string, to: string, groupBy: string) => ['reports', 'revenue', from, to, groupBy] as const,
  iopTrends: (patientId: string, from: string, to: string) => ['reports', 'iopTrends', patientId, from, to] as const,
  demographics: () => ['reports', 'demographics'] as const,
};

export function useDashboardKpis() {
  return useQuery({
    queryKey: reportsKeys.kpis(),
    queryFn: () => reportsService.getDashboardKpis(),
    refetchInterval: 15 * 60 * 1000, // 15 minutes
  });
}

export function useAppointmentStatistics(from: string | undefined, to: string | undefined, groupBy: string = 'month') {
  return useQuery({
    queryKey: reportsKeys.appointments(from ?? '', to ?? '', groupBy),
    queryFn: () => reportsService.getAppointmentStatistics(from!, to!, groupBy),
    enabled: !!from && !!to,
  });
}

export function useRevenueStatistics(from: string | undefined, to: string | undefined, groupBy: string = 'month') {
  return useQuery({
    queryKey: reportsKeys.revenue(from ?? '', to ?? '', groupBy),
    queryFn: () => reportsService.getRevenueStatistics(from!, to!, groupBy),
    enabled: !!from && !!to,
  });
}

export function useIopTrends(patientId: string | undefined, from: string | undefined, to: string | undefined) {
  return useQuery({
    queryKey: reportsKeys.iopTrends(patientId ?? '', from ?? '', to ?? ''),
    queryFn: () => reportsService.getIopTrends(patientId!, from!, to!),
    enabled: !!patientId && !!from && !!to,
  });
}

export function usePatientDemographics() {
  return useQuery({
    queryKey: reportsKeys.demographics(),
    queryFn: () => reportsService.getPatientDemographics(),
  });
}
