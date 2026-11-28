import authMock from '../../mocks/auth.mock.json';
import { apiRequest, ApiClientError, withMockFallback } from '../../lib/apiClient';
import type { CurrentUserResponse, LoginRequest, LoginResponse, PreferencesRequest } from '../../types/api';

type DemoRole = keyof typeof authMock.meByRole;
let mockRole: DemoRole = 'PATIENT';
const mockPreferences = new Map<string, PreferencesRequest>();

// Cerinta E1-IO-01: acelasi contract pentru mock si API; fara fallback la mock dupa erori live.
export function login(request: LoginRequest): Promise<LoginResponse> {
  return withMockFallback(() => {
    const role = (Object.keys(authMock.meByRole) as DemoRole[]).find(
      (key) => authMock.meByRole[key].email === request.email.trim().toLowerCase(),
    );
    if (!role || request.password !== 'OphthaDemo!2026#19') {
      throw new ApiClientError(401, {
        code: 'UNAUTHORIZED', message: 'Adresa de e-mail sau parola este incorectă.',
        fieldErrors: {}, requestId: 'mock-login',
      });
    }
    mockRole = role;
    return { ...authMock.loginByRole[role], role };
  }, () => apiRequest<LoginResponse>('/auth/login', { method: 'POST', body: JSON.stringify(request) }));
}

export function getCurrentUser(): Promise<CurrentUserResponse> {
  return withMockFallback(() => ({
    ...authMock.meByRole[mockRole], role: mockRole,
    ...mockPreferences.get(authMock.meByRole[mockRole].userId),
  }), () => apiRequest<CurrentUserResponse>('/me'));
}

export function savePreferences(preferences: PreferencesRequest): Promise<CurrentUserResponse> {
  return withMockFallback(() => {
    const user = authMock.meByRole[mockRole];
    mockPreferences.set(user.userId, { ...preferences });
    return { ...user, role: mockRole, ...preferences };
  }, () => apiRequest<CurrentUserResponse>('/me/preferences', {
    method: 'PATCH', body: JSON.stringify(preferences),
  }));
}
