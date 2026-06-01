import { apiGet, apiGetPaged, apiPost, apiPatch, apiPut } from '@/lib/apiClient';
import type {
  CreateOrderRequest,
  InvoiceDto,
  InvoiceStatus,
  OpticalOrderDto,
  OrderStage,
  QcResultDto,
  UpdateStageRequest,
  ServiceCatalogDto,
} from '@/types/optical';

const ORDERS = '/api/v1/optical/orders';
const INVOICES = '/api/v1/optical/invoices';

export const opticalService = {
  listOrders(params: { stage?: OrderStage }) {
    return apiGet<OpticalOrderDto[]>(ORDERS, params);
  },

  createOrder(data: CreateOrderRequest) {
    return apiPost<OpticalOrderDto>(ORDERS, data);
  },

  getOrder(id: string) {
    return apiGet<OpticalOrderDto>(`${ORDERS}/${id}`);
  },

  updateOrderStage(id: string, data: UpdateStageRequest) {
    return apiPatch<OpticalOrderDto>(`${ORDERS}/${id}/stage`, data);
  },

  submitQcResult(id: string, data: QcResultDto) {
    return apiPut<OpticalOrderDto>(`${ORDERS}/${id}/qc`, data);
  },

  listInvoices(params: { status?: InvoiceStatus; page: number; size: number }) {
    return apiGetPaged<InvoiceDto>(INVOICES, params);
  },

  createInvoice(data: { opticalOrderId?: string; patientId: string; items?: any[] }) {
    return apiPost<InvoiceDto>(INVOICES, data);
  },

  payInvoice(id: string, paymentMethod: string) {
    return apiPatch<InvoiceDto>(`${INVOICES}/${id}/pay`, { paymentMethod });
  },

  listServices() {
    return apiGet<ServiceCatalogDto[]>('/api/v1/optical/services');
  },
};
