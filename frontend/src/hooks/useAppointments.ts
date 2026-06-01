import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { appointmentsService } from '@/services/appointmentsService';
import type {
  CreateAppointmentRequest,
  AppointmentStatus,
} from '@/types/appointments';

export const appointmentsKeys = {
  all: ['appointments'] as const,
  range: (from: string, to: string) => ['appointments', 'range', from, to] as const,
  types: ['appointment-types'] as const,
};

export function useAppointments(from: string | undefined, to: string | undefined) {
  return useQuery({
    queryKey: appointmentsKeys.range(from ?? '', to ?? ''),
    queryFn: () => appointmentsService.listAppointments({ from: from!, to: to! }),
    enabled: !!from && !!to,
  });
}

export function useCreateAppointment() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateAppointmentRequest) =>
      appointmentsService.createAppointment(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: appointmentsKeys.all });
    },
  });
}

export function useUpdateAppointmentStatus() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      id,
      status,
      reason,
    }: {
      id: string;
      status: AppointmentStatus;
      reason?: string;
    }) => appointmentsService.updateAppointmentStatus(id, status, reason),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: appointmentsKeys.all });
    },
  });
}

export function useDeleteAppointment() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => appointmentsService.deleteAppointment(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: appointmentsKeys.all });
    },
  });
}

export function useAppointmentTypes() {
  return useQuery({
    queryKey: appointmentsKeys.types,
    queryFn: () => appointmentsService.listAppointmentTypes(),
  });
}
