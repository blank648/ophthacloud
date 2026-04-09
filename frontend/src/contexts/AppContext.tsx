import React, { createContext, useContext, useState, useCallback, useEffect } from 'react';

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

export const useApp = () => {
  const ctx = useContext(AppContext);
  if (!ctx) throw new Error('useApp must be inside AppProvider');
  return ctx;
};

export const AppProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [state, setState] = useState<AppState>({
    role: 'doctor',
    darkMode: false,
    sidebarCollapsed: false,
    isLoggedIn: false,
    currentClinic: 'Clinica Oftalmologică Visiomed',
  });

  useEffect(() => {
    document.documentElement.classList.toggle('dark', state.darkMode);
  }, [state.darkMode]);

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
