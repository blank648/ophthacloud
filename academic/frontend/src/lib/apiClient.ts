import type { ApiErrorResponse } from '../types/api';

export const USE_MOCKS = import.meta.env.VITE_USE_MOCKS !== 'false';
export const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL as string | undefined) || '/api';

let inMemoryAccessToken: string | null = null;
let onUnauthorizedCallback: (() => void) | null = null;
let unauthorizedHandled = false;

export function setAccessToken(token: string | null): void {
  inMemoryAccessToken = token;
  if (token) {
    unauthorizedHandled = false;
  }
}

export function getAccessToken(): string | null {
  return inMemoryAccessToken;
}

export function clearAccessToken(): void {
  inMemoryAccessToken = null;
}

export function setUnauthorizedHandler(handler: (() => void) | null): void {
  onUnauthorizedCallback = handler;
}

export class ApiClientError extends Error {
  readonly status: number;
  readonly payload: ApiErrorResponse;

  constructor(status: number, payload: ApiErrorResponse) {
    super(payload.message);
    this.name = 'ApiClientError';
    this.status = status;
    this.payload = payload;
  }
}

export async function apiRequest<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  if (!headers.has('Content-Type') && init.body) {
    headers.set('Content-Type', 'application/json');
  }
  if (inMemoryAccessToken && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${inMemoryAccessToken}`);
  }

  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  const requestToken = inMemoryAccessToken;
  const response = await fetch(`${API_BASE_URL}${normalizedPath}`, {
    ...init,
    headers,
  });

  // Un login respins nu inseamna sesiune expirata. Ignoram si cererile unei sesiuni vechi.
  if (response.status === 401 && requestToken && requestToken === inMemoryAccessToken && !unauthorizedHandled) {
    unauthorizedHandled = true;
    clearAccessToken();
    onUnauthorizedCallback?.();
  }

  if (!response.ok) {
    let errorPayload: ApiErrorResponse;
    try {
      errorPayload = (await response.json()) as ApiErrorResponse;
    } catch {
      errorPayload = {
        code: `HTTP_${response.status}`,
        message: response.statusText || 'Eroare de comunicare cu serverul.',
        fieldErrors: {},
        requestId: 'client-fallback',
      };
    }
    throw new ApiClientError(response.status, errorPayload);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}

export async function withMockFallback<T>(
  mockProvider: () => T | Promise<T>,
  liveRequest: () => Promise<T>,
  forceLive = false
): Promise<T> {
  if (USE_MOCKS && !forceLive) {
    return await mockProvider();
  }
  return await liveRequest();
}
