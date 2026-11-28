import type { PreferencesRequest } from '../../types/api';

export const FONT_SCALE_STEPS = [1, 1.2, 1.5, 1.8, 2] as const;
export const DEFAULT_PREFERENCES: PreferencesRequest = { uiFontScale: 1, highContrast: false };
const STORAGE_KEY = 'ophthacloud-accessibility';

export function normalizePreferences(preferences: PreferencesRequest): PreferencesRequest {
  return {
    uiFontScale: FONT_SCALE_STEPS.find((step) => step === preferences.uiFontScale) ?? 1,
    highContrast: preferences.highContrast === true,
  };
}

export function readPreferences(): PreferencesRequest {
  try {
    const saved: PreferencesRequest | null = JSON.parse(localStorage.getItem(STORAGE_KEY) ?? 'null');
    return saved ? normalizePreferences(saved) : DEFAULT_PREFERENCES;
  } catch {
    return DEFAULT_PREFERENCES;
  }
}

export function persistPreferences(preferences: PreferencesRequest): void {
  try {
    // Doar setari vizuale; tokenurile si datele pacientului nu se persista in browser.
    localStorage.setItem(STORAGE_KEY, JSON.stringify(preferences));
  } catch {
    // Setarile raman functionale in memorie daca browserul blocheaza stocarea.
  }
}
