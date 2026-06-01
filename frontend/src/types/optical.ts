export type OrderStage =
  | 'RECEIVED'
  | 'SENT_TO_LAB'
  | 'QC_CHECK'
  | 'READY_FOR_FITTING'
  | 'COMPLETED'
  | 'CANCELLED';

export type InvoiceStatus = 'DRAFT' | 'ISSUED' | 'PAID';

export interface OrderItemDto {
  id?: string;
  productCode: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  notes?: string;
}

export interface OpticalOrderDto {
  id: string;
  orderNumber: string;
  patientId: string;
  patientName?: string;
  prescriptionId?: string;
  consultationId?: string;
  orderType?: 'GLASSES' | 'CONTACTS';
  stage: OrderStage;
  assignedToId?: string;
  assignedToName?: string;
  labName?: string;
  labReference?: string;
  sentToLabAt?: string;
  qcPassedAt?: string;
  readyAt?: string;
  completedAt?: string;
  cancelledAt?: string;
  cancellationReason?: string;
  items: OrderItemDto[];
  totalAmount: number;
  depositPaid?: number;
  currency?: string;
  notes?: string;
  qcResult?: QcResultDto;
  createdAt?: string;
  updatedAt?: string;
  expectedReadyAt?: string;
}

export interface CreateOrderRequest {
  patientId: string;
  prescriptionId?: string;
  items: OrderItemDto[];
  notes?: string;
  expectedReadyAt?: string;
}

export interface UpdateStageRequest {
  newStage: OrderStage;
  cancellationReason?: string;
}

export interface QcResultDto {
  valuesChecked: boolean;
  opticalCenterOd: boolean;
  opticalCenterOs: boolean;
  pupillaryDistance: boolean;
  segmentHeight: boolean;
  treatmentsApplied: boolean;
  assemblyQuality: boolean;
  finalCleaning: boolean;
}

export interface InvoiceLineDto {
  description: string;
  quantity: number;
  unitPrice: number;
  vatRate: number;
  total: number;
}

export interface InvoiceDto {
  id: string;
  invoiceNumber: string;
  orderId?: string;
  patientId: string;
  patientName?: string;
  status: InvoiceStatus;
  lines: InvoiceLineDto[];
  subtotal: number;
  vatTotal: number;
  total: number;
  issuedAt?: string;
  paidAt?: string;
  paymentMethod?: string;
}

export interface ServiceCatalogDto {
  id: string;
  name: string;
  category: string;
  sku?: string;
  brand?: string;
  unitPrice: number;
  vatRate: number;
  currency: string;
  isActive: boolean;
  notes?: string;
}

