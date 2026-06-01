import Keycloak from 'keycloak-js';
import { useAuthStore, type UserInfo, type PermissionAction } from '@/stores/authStore';

const keycloakUrl = import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8180';
const realm = import.meta.env.VITE_KEYCLOAK_REALM || 'ophthacloud';
const clientId = import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'ophthacloud-frontend';

export const keycloak = new Keycloak({
  url: keycloakUrl,
  realm,
  clientId,
});
(window as any).keycloak = keycloak;

interface KeycloakJwtPayload {
  sub: string;
  tenant_id?: string;
  realm_access?: { roles: string[] };
  permissions?: Record<string, string[]>;
  name?: string;
  email?: string;
  given_name?: string;
  family_name?: string;
  preferred_username?: string;
  exp: number;
}

function parseToken(token: string): KeycloakJwtPayload | null {
  try {
    const payload = token.split('.')[1];
    return JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')));
  } catch {
    return null;
  }
}

async function buildUserInfo(token: string): Promise<UserInfo | null> {
  const claims = parseToken(token);
  if (!claims) return null;

  let profile: any = {};
  try {
    const apiUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
    const res = await fetch(`${apiUrl}/api/v1/profile/me`, {
      headers: { Authorization: `Bearer ${token}` }
    });
    if (res.ok) {
      const json = await res.json();
      profile = json.data || {};
    }
  } catch (e) {
    console.warn("Could not load user profile from backend API", e);
  }

  return {
    staffId: profile.id || claims.staff_id || claims.sub || '',
    tenantId: (claims as any).tenant_id || '',
    role: profile.role || (claims as any).staff_role || claims.realm_access?.roles?.[0] || '',
    name: (profile.firstName && profile.lastName) 
      ? `${profile.firstName} ${profile.lastName}`
      : claims.preferred_username || claims.name || 'Utilizator',
    email: profile.email || claims.email || '',
    givenName: profile.firstName || claims.given_name || '',
    familyName: profile.lastName || claims.family_name || '',
    // Normalize regardless of format (GUIDE_05 §3.1 uses boolean-objects;
    // some Keycloak mappers may emit string-arrays instead).
    permissions: normalizePermissions(
      typeof (claims as any).permissions === 'string'
        ? parsePermissionsSafe((claims as any).permissions)
        : (claims as any).permissions
    ),
  };
}

function parsePermissionsSafe(permsStr: string) {
  try {
    return JSON.parse(permsStr);
  } catch {
    return {};
  }
}

/**
 * Normalizes the `permissions` claim from the JWT into a canonical
 * `Record<module, string[]>` format used by `hasPermission()`.
 *
 * Keycloak emits two possible shapes (GUIDE_05 §3.1):
 *   - Object-of-booleans: { emr: { view: true, create: true, edit: true, sign: true } }
 *   - String-array:       { emr: ["VIEW", "CREATE", "EDIT", "SIGN"] }
 *
 * Both are normalised to: { emr: ["VIEW", "CREATE", "EDIT", "SIGN"] }
 */
function normalizePermissions(
  raw: unknown
): Record<string, string[]> {
  if (!raw || typeof raw !== 'object' || Array.isArray(raw)) return {};

  const result: Record<string, string[]> = {};

  const ACTION_MAP: Record<string, string> = {
    view:   'VIEW',
    create: 'CREATE',
    edit:   'EDIT',
    delete: 'DELETE',
    sign:   'SIGN',
    export: 'EXPORT',
  };

  for (const [moduleCode, value] of Object.entries(raw as Record<string, unknown>)) {
    if (Array.isArray(value)) {
      // Already in string-array format — uppercase just in case
      result[moduleCode] = value.map((a) => String(a).toUpperCase());
    } else if (value && typeof value === 'object') {
      // Boolean-object format from GUIDE_05 §3.1
      const granted: string[] = [];
      for (const [key, allowed] of Object.entries(value as Record<string, unknown>)) {
        if (allowed === true) {
          const action = ACTION_MAP[key.toLowerCase()] || key.toUpperCase();
          granted.push(action);
        }
      }
      result[moduleCode] = granted;
    }
  }

  return result;
}

export async function initKeycloak(): Promise<boolean> {
  const store = useAuthStore.getState();
  store.setLoading(true);
  try {
    const authenticated = await keycloak.init({
      onLoad: 'check-sso',
      silentCheckSsoRedirectUri:
        window.location.origin + '/silent-check-sso.html',
      pkceMethod: 'S256',
      checkLoginIframe: false,
    });

    if (authenticated && keycloak.token) {
      store.setToken(keycloak.token, keycloak.refreshToken ?? null);
      const userInfo = await buildUserInfo(keycloak.token);
      store.setUserInfo(userInfo);
    }

    keycloak.onTokenExpired = () => {
      keycloak.updateToken(30).then((refreshed) => {
        if (refreshed && keycloak.token) {
          useAuthStore.getState().setToken(keycloak.token, keycloak.refreshToken ?? null);
          // Profile data typically doesn't change on token refresh, no need to await buildUserInfo again
          // We just update the token.
        }
      });
    };

    return authenticated;
  } catch (e) {
    console.error('Keycloak init failed', e);
    return false;
  } finally {
    store.setLoading(false);
  }
}

export async function getToken(): Promise<string | null> {
  const localToken = localStorage.getItem('kc_token');
  if (localToken) return localToken;

  try {
    await keycloak.updateToken(30);
    if (keycloak.token) {
      useAuthStore.getState().setToken(keycloak.token, keycloak.refreshToken ?? null);
    }
    return keycloak.token ?? null;
  } catch {
    return useAuthStore.getState().token;
  }
}

export function getUserInfo(): UserInfo | null {
  return useAuthStore.getState().userInfo;
}

export function hasPermission(module: string, action: PermissionAction): boolean {
  const info = useAuthStore.getState().userInfo;
  if (!info) return false;
  return info.permissions[module]?.includes(action) ?? false;
}

export function login(): Promise<void> {
  return keycloak.login();
}

export function logout(): Promise<void> {
  useAuthStore.getState().clearAuth();
  return keycloak.logout({ redirectUri: window.location.origin });
}
