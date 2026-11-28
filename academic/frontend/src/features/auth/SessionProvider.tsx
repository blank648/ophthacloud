import { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react';
import type { PropsWithChildren } from 'react';
import { clearAccessToken, setAccessToken, setUnauthorizedHandler } from '../../lib/apiClient';
import type { CurrentUserResponse, LoginRequest, PreferencesRequest } from '../../types/api';
import { normalizePreferences, persistPreferences, readPreferences } from '../accessibility/preferences';
import * as authService from './authService';

interface SessionContextValue {
  user: CurrentUserResponse | null;
  preferences: PreferencesRequest;
  notice: string;
  preferenceNotice: string;
  signIn: (request: LoginRequest) => Promise<void>;
  signOut: () => void;
  updatePreferences: (preferences: PreferencesRequest) => void;
}

const SessionContext = createContext<SessionContextValue | null>(null);

// Cerinta E1-IO-01: sesiune numai in memorie, preferinte disponibile si inainte de login.
export function SessionProvider({ children }: PropsWithChildren) {
  const [user, setUser] = useState<CurrentUserResponse | null>(null);
  const [preferences, setPreferences] = useState(readPreferences);
  const [notice, setNotice] = useState('');
  const [preferenceNotice, setPreferenceNotice] = useState('');
  const [expiresAt, setExpiresAt] = useState<number | null>(null);
  const generation = useRef(0);
  const currentUser = useRef(user);
  const currentPreferences = useRef(preferences);
  const changedBeforeLogin = useRef(false);
  const preferenceQueue = useRef(Promise.resolve());

  const endSession = useCallback((message = '', resetChoices = true) => {
    generation.current += 1;
    currentUser.current = null;
    clearAccessToken();
    setUser(null);
    setExpiresAt(null);
    setNotice(message);
    setPreferenceNotice('');
    if (resetChoices) changedBeforeLogin.current = false;
  }, []);

  const applyPreferences = useCallback((next: PreferencesRequest) => {
    currentPreferences.current = next;
    setPreferences(next);
  }, []);

  const queuePreferences = useCallback((next: PreferencesRequest) => {
    const sessionGeneration = generation.current;
    setPreferenceNotice('Se salvează preferințele…');
    // Cererile PATCH sunt ordonate pentru ca un raspuns lent sa nu suprascrie ultima alegere.
    preferenceQueue.current = preferenceQueue.current.then(async () => {
      if (sessionGeneration !== generation.current) return;
      try {
        await authService.savePreferences(next);
        if (sessionGeneration === generation.current) setPreferenceNotice('Preferințele au fost salvate.');
      } catch {
        if (sessionGeneration === generation.current) {
          setPreferenceNotice('Preferințele sunt active pe acest dispozitiv. Salvarea în cont nu a reușit. Modifică din nou setarea pentru a reîncerca.');
        }
      }
    });
  }, []);

  const updatePreferences = useCallback((next: PreferencesRequest) => {
    const normalized = normalizePreferences(next);
    applyPreferences(normalized);
    if (currentUser.current?.role === 'PATIENT') queuePreferences(normalized);
    else changedBeforeLogin.current = true;
  }, [applyPreferences, queuePreferences]);

  const signIn = useCallback(async (request: LoginRequest) => {
    const sessionGeneration = ++generation.current;
    setNotice('');
    try {
      const response = await authService.login(request);
      if (sessionGeneration !== generation.current) return;
      setAccessToken(response.accessToken);
      const account = await authService.getCurrentUser();
      if (sessionGeneration !== generation.current) return;
      currentUser.current = account;
      setUser(account);
      setExpiresAt(Date.now() + response.expiresIn * 1000);
      if (account.role === 'PATIENT') {
        if (changedBeforeLogin.current) queuePreferences(currentPreferences.current);
        else applyPreferences(normalizePreferences(account));
      }
      changedBeforeLogin.current = false;
    } catch (error) {
      if (sessionGeneration === generation.current) endSession('', false);
      throw error;
    }
  }, [applyPreferences, endSession, queuePreferences]);

  useEffect(() => {
    document.documentElement.style.setProperty('--font-scale', String(preferences.uiFontScale));
    document.documentElement.dataset.highContrast = String(preferences.highContrast);
    persistPreferences(preferences);
  }, [preferences]);

  useEffect(() => {
    setUnauthorizedHandler(() => endSession('Sesiunea a expirat. Autentifică-te din nou pentru a continua.'));
    return () => { setUnauthorizedHandler(null); };
  }, [endSession]);

  useEffect(() => {
    if (expiresAt === null) return;
    const checkExpiry = () => {
      if (Date.now() >= expiresAt) endSession('Sesiunea a expirat. Autentifică-te din nou pentru a continua.');
    };
    const timer = window.setTimeout(checkExpiry, Math.max(0, expiresAt - Date.now()));
    window.addEventListener('focus', checkExpiry);
    return () => { window.clearTimeout(timer); window.removeEventListener('focus', checkExpiry); };
  }, [expiresAt, endSession]);

  return <SessionContext.Provider value={{ user, preferences, notice, preferenceNotice, signIn,
    signOut: () => endSession(), updatePreferences }}>{children}</SessionContext.Provider>;
}

export function useSession(): SessionContextValue {
  const session = useContext(SessionContext);
  if (!session) throw new Error('useSession requires SessionProvider');
  return session;
}
