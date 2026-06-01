import { apiGet, apiGetPaged, apiPatch } from '@/lib/apiClient';
import type { PagedApiResponse } from '@/types/api';

export interface NotificationRuleDto {
  id: string;
  name: string;
  configData: {
    trigger_type?: string;
    channels?: string[];
    template_email_subject?: string;
    template_email_body?: string;
    [key: string]: any;
  } | null;
  isActive: boolean;
}

export interface RecallProtocolDto {
  id: string;
  name: string;
  icd10Code: string;
  recallIntervalMonths: number;
  description: string;
  isActive: boolean;
}

export interface NotificationLogDto {
  id: string;
  patientName: string;
  channel: string;
  status: string;
  recipientAddress: string;
  subject: string;
  bodyPreview: string;
  sentAt?: string;
  externalMessageId?: string;
}

export const notificationsService = {
  listRules(params?: { page?: number; size?: number }) {
    return apiGetPaged<NotificationRuleDto>('/api/v1/notifications/rules', params);
  },
  
  toggleRule(id: string) {
    return apiPatch<NotificationRuleDto>(`/api/v1/notifications/rules/${id}/toggle`);
  },
  
  listRecallProtocols(params?: { page?: number; size?: number }) {
    return apiGetPaged<RecallProtocolDto>('/api/v1/notifications/recall-protocols', params);
  },
  
  listNotificationLogs(params?: { page?: number; size?: number; patientId?: string; status?: string }) {
    return apiGetPaged<NotificationLogDto>('/api/v1/notifications/log', params);
  }
};
