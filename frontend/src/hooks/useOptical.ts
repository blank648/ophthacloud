import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { opticalService } from '@/services/opticalService';
import type {
  CreateOrderRequest,
  InvoiceStatus,
  OrderStage,
  QcResultDto,
  UpdateStageRequest,
} from '@/types/optical';

export const opticalKeys = {
  all: ['optical'] as const,
  orders: (params: { stage?: OrderStage; page: number; size: number }) =>
    ['optical', 'orders', params] as const,
  order: (id: string) => ['optical', 'order', id] as const,
  invoices: (params: { status?: InvoiceStatus; page: number; size: number }) =>
    ['optical', 'invoices', params] as const,
  services: () => ['optical', 'services'] as const,
};

export function useOrders(params: { stage?: OrderStage } = {}) {
  return useQuery({
    queryKey: ['optical', 'orders', params.stage] as const,
    queryFn: () => opticalService.listOrders(params),
  });
}

export function useOrder(id: string | undefined) {
  return useQuery({
    queryKey: id ? opticalKeys.order(id) : ['optical', 'order', 'none'],
    queryFn: () => opticalService.getOrder(id!),
    enabled: !!id,
  });
}

export function useCreateOrder() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateOrderRequest) => opticalService.createOrder(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: opticalKeys.all });
    },
  });
}

export function useUpdateOrderStage() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateStageRequest }) =>
      opticalService.updateOrderStage(id, data),
    onSuccess: (_res, vars) => {
      qc.invalidateQueries({ queryKey: opticalKeys.order(vars.id) });
      qc.invalidateQueries({ queryKey: opticalKeys.all });
    },
  });
}

export function useSubmitQc() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: QcResultDto }) =>
      opticalService.submitQcResult(id, data),
    onSuccess: (_res, vars) => {
      qc.invalidateQueries({ queryKey: opticalKeys.order(vars.id) });
      qc.invalidateQueries({ queryKey: opticalKeys.all });
    },
  });
}

export function useInvoices(params: { status?: InvoiceStatus; page?: number; size?: number } = {}) {
  const normalized = { status: params.status, page: params.page ?? 0, size: params.size ?? 20 };
  return useQuery({
    queryKey: opticalKeys.invoices(normalized),
    queryFn: () => opticalService.listInvoices(normalized),
  });
}

export function useCreateInvoice() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: { opticalOrderId?: string; patientId: string; items?: any[] }) =>
      opticalService.createInvoice(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: opticalKeys.all });
    },
  });
}

export function usePayInvoice() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, paymentMethod }: { id: string; paymentMethod: string }) =>
      opticalService.payInvoice(id, paymentMethod),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: opticalKeys.all });
    },
  });
}

export function useServices() {
  return useQuery({
    queryKey: opticalKeys.services(),
    queryFn: () => opticalService.listServices(),
  });
}
