import { create } from 'zustand';

export type PermissionAction = 'VIEW' | 'CREATE' | 'EDIT' | 'DELETE' | 'SIGN';

export interface UserInfo {
  staffId: string;
  tenantId: string;
  role: string;
  name: string;
  email: string;
  givenName: string;
  familyName: string;
  permissions: Record<string, string[]>;
}

interface AuthState {
  token: string | null;
  refreshToken: string | null;
  userInfo: UserInfo | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  setToken: (token: string | null, refreshToken?: string | null) => void;
  setUserInfo: (userInfo: UserInfo | null) => void;
  setLoading: (loading: boolean) => void;
  clearAuth: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  token: null,
  refreshToken: null,
  userInfo: null,
  isAuthenticated: false,
  isLoading: true,
  setToken: (token, refreshToken = null) =>
    set((s) => ({
      token,
      refreshToken: refreshToken ?? s.refreshToken,
      isAuthenticated: !!token,
    })),
  setUserInfo: (userInfo) => set({ userInfo }),
  setLoading: (isLoading) => set({ isLoading }),
  clearAuth: () => {
    localStorage.removeItem('kc_token');
    set({
      token: null,
      refreshToken: null,
      userInfo: null,
      isAuthenticated: false,
    });
  },
}));
