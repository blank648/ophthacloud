import axios, { AxiosError, type AxiosInstance, type InternalAxiosRequestConfig } from 'axios';
import { useAuthStore } from '@/stores/authStore';
import { getToken, keycloak } from '@/lib/auth';
import type { ApiResponse, PagedApiResponse, ApiErrorResponse } from '@/types/api';

const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

export const apiClient: AxiosInstance = axios.create({
  baseURL,
  headers: { 'Content-Type': 'application/json' },
});

apiClient.interceptors.request.use(async (config: InternalAxiosRequestConfig) => {
  const token = useAuthStore.getState().token || (await getToken());
  if (token && config.headers) {
    config.headers.set('Authorization', `Bearer ${token}`);
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiErrorResponse>) => {
    const status = error.response?.status;
    const body = error.response?.data;

    if (status === 401) {
      try {
        const refreshed = await keycloak.updateToken(30);
        if (refreshed && error.config) {
          const token = keycloak.token;
          if (token && error.config.headers) {
            useAuthStore.getState().setToken(token, keycloak.refreshToken ?? null);
            (error.config.headers as { set: (k: string, v: string) => void }).set(
              'Authorization',
              `Bearer ${token}`
            );
            return apiClient.request(error.config);
          }
        }
      } catch {
        useAuthStore.getState().clearAuth();
        await keycloak.login();
      }
    }

    if (status === 403) {
      const url = error.config?.url || '';
      const isBackgroundRequest =
        url.includes('/reports/dashboard-kpis') ||
        url.includes('/appointments') ||
        url.includes('/optical/invoices') ||
        url.includes('/notifications');
      // Dispatch a custom event instead of hard-redirecting.
      // This lets React Router (via AuthGuard/listener) handle navigation
      // without a full page reload that loses React state.
      // The promise still rejects so the caller can show a toast.
      if (typeof window !== 'undefined' && window.location.pathname !== '/unauthorized' && !isBackgroundRequest) {
        window.dispatchEvent(new CustomEvent('ophthacloud:forbidden'));
      }
    }

    const errBody = body?.error;
    const code = errBody?.code || `HTTP_${status ?? 'ERR'}`;
    const wrapped = new Error(errBody?.message || error.message) as Error & {
      code?: string;
      status?: number;
      fieldErrors?: { field: string; code: string; message: string }[];
    };
    wrapped.code = code;
    wrapped.status = status;
    wrapped.fieldErrors = errBody?.details;
    return Promise.reject(wrapped);
  }
);

export async function apiGet<T>(url: string, params?: Record<string, unknown>): Promise<T> {
  const res = await apiClient.get<ApiResponse<T>>(url, { params });
  return res.data.data;
}

export async function apiGetPaged<T>(url: string, params?: Record<string, unknown>): Promise<PagedApiResponse<T>> {
  const res = await apiClient.get<PagedApiResponse<T>>(url, { params });
  return res.data;
}

export async function apiPost<T>(url: string, body?: unknown): Promise<T> {
  const res = await apiClient.post<ApiResponse<T>>(url, body);
  return res.data.data;
}

export async function apiPut<T>(url: string, body?: unknown): Promise<T> {
  const res = await apiClient.put<ApiResponse<T>>(url, body);
  return res.data.data;
}

export async function apiPatch<T>(url: string, body?: unknown): Promise<T> {
  const res = await apiClient.patch<ApiResponse<T>>(url, body);
  return res.data.data;
}

export async function apiDelete<T>(url: string): Promise<T> {
  const res = await apiClient.delete<ApiResponse<T>>(url);
  return res.data.data;
}
