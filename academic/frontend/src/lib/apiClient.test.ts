import { afterEach, describe, expect, it, vi } from 'vitest';
import { apiRequest, clearAccessToken, getAccessToken, setAccessToken, setUnauthorizedHandler, withMockFallback } from './apiClient';

afterEach(() => { clearAccessToken(); setUnauthorizedHandler(null); vi.unstubAllGlobals(); });
const unauthorizedResponse = () => new Response(JSON.stringify({
  code: 'UNAUTHORIZED', message: 'Autentificare necesară.', fieldErrors: {}, requestId: 'test',
}), { status: 401 });

describe('HTTP session handling', () => {
  it('sends bearer tokens in the Authorization header', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response('{}'));
    vi.stubGlobal('fetch', fetchMock);
    setAccessToken('test-token');
    await apiRequest('/me');
    expect(fetchMock.mock.calls[0][1].headers.get('Authorization')).toBe('Bearer test-token');
  });

  it('does not interpret rejected public login as session expiry', async () => {
    const expired = vi.fn();
    setUnauthorizedHandler(expired);
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(unauthorizedResponse()));
    await expect(apiRequest('/auth/login')).rejects.toMatchObject({ status: 401 });
    expect(expired).not.toHaveBeenCalled();
  });

  it('handles concurrent protected 401 responses once', async () => {
    const expired = vi.fn();
    setUnauthorizedHandler(expired);
    setAccessToken('expired-token');
    vi.stubGlobal('fetch', vi.fn().mockImplementation(async () => unauthorizedResponse()));
    await Promise.allSettled([apiRequest('/me'), apiRequest('/me')]);
    expect(expired).toHaveBeenCalledTimes(1);
    expect(getAccessToken()).toBeNull();
  });

  it('does not clear a new session when an older request returns 401', async () => {
    let resolveRequest!: (response: Response) => void;
    vi.stubGlobal('fetch', vi.fn().mockImplementation(() => new Promise<Response>((resolve) => { resolveRequest = resolve; })));
    setAccessToken('old-token');
    const request = apiRequest('/me');
    setAccessToken('new-token');
    resolveRequest(unauthorizedResponse());
    await expect(request).rejects.toMatchObject({ status: 401 });
    expect(getAccessToken()).toBe('new-token');
  });

  it('never substitutes mock success when forced live fails', async () => {
    const mock = vi.fn();
    await expect(withMockFallback(mock, async () => { throw new Error('offline'); }, true)).rejects.toThrow('offline');
    expect(mock).not.toHaveBeenCalled();
  });

  it('accepts no-content responses without parsing JSON', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(null, { status: 204 })));
    await expect(apiRequest('/me')).resolves.toBeUndefined();
  });
});
