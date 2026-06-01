import React, { createContext, useContext, useState, useCallback, useEffect } from 'react';
import { useAuthStore } from '@/stores/authStore';

export type UserRole = 'doctor' | 'receptionist' | 'manager' | 'optician' | 'patient';

interface AppState {
  role: UserRole;
  darkMode: boolean;
  sidebarCollapsed: boolean;
  isLoggedIn: boolean;
  currentClinic: string;
}

interface AppContextType extends AppState {
  setRole: (role: UserRole) => void;
  toggleDarkMode: () => void;
  toggleSidebar: () => void;
  login: (role: UserRole) => void;
  logout: () => void;
}

const AppContext = createContext<AppContextType | null>(null);

const mapRole = (backendRole: string | undefined): UserRole => {
  if (!backendRole) return 'doctor';
  const roleLower = backendRole.toLowerCase();
  if (roleLower === 'clinic_admin' || roleLower === 'manager' || roleLower === 'super_admin') return 'manager';
  if (roleLower === 'doctor') return 'doctor';
  if (roleLower === 'receptionist') return 'receptionist';
  if (roleLower === 'optician') return 'optician';
  if (roleLower === 'patient') return 'patient';
  return 'doctor';
};

export const useApp = () => {
  const ctx = useContext(AppContext);
  if (!ctx) throw new Error('useApp must be inside AppProvider');
  return ctx;
};

export const AppProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const userInfo = useAuthStore(s => s.userInfo);
  const mappedInitialRole = userInfo && userInfo.role ? mapRole(userInfo.role) : 'doctor';

  const [state, setState] = useState<AppState>({
    role: mappedInitialRole,
    darkMode: false,
    sidebarCollapsed: false,
    isLoggedIn: !!userInfo,
    currentClinic: 'Clinica Oftalmologică Demo SRL',
  });

  useEffect(() => {
    document.documentElement.classList.toggle('dark', state.darkMode);
  }, [state.darkMode]);

  // Synchronize authenticated Keycloak user role with AppContext role automatically
  useEffect(() => {
    if (userInfo && userInfo.role) {
      const mappedRole = mapRole(userInfo.role);
      setState(s => ({ ...s, isLoggedIn: true, role: mappedRole }));
    } else {
      setState(s => ({ ...s, isLoggedIn: false }));
    }
  }, [userInfo]);

  const setRole = useCallback((role: UserRole) => setState(s => ({ ...s, role })), []);
  const toggleDarkMode = useCallback(() => setState(s => ({ ...s, darkMode: !s.darkMode })), []);
  const toggleSidebar = useCallback(() => setState(s => ({ ...s, sidebarCollapsed: !s.sidebarCollapsed })), []);
  const login = useCallback((role: UserRole) => setState(s => ({ ...s, isLoggedIn: true, role })), []);
  const logout = useCallback(() => setState(s => ({ ...s, isLoggedIn: false })), []);

  return (
    <AppContext.Provider value={{ ...state, setRole, toggleDarkMode, toggleSidebar, login, logout }}>
      {children}
    </AppContext.Provider>
  );
};
