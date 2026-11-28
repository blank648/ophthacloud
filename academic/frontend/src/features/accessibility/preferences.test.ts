import { afterEach, expect, it } from 'vitest';
import { DEFAULT_PREFERENCES, normalizePreferences, persistPreferences, readPreferences } from './preferences';

afterEach(() => localStorage.clear());
it('rejects scales that do not match the database-compatible steps', () => {
  expect(normalizePreferences({ uiFontScale: 1.25, highContrast: false }).uiFontScale).toBe(1);
  expect(normalizePreferences({ uiFontScale: 1.8, highContrast: true }).uiFontScale).toBe(1.8);
});
it('recovers from malformed device preferences', () => {
  localStorage.setItem('ophthacloud-accessibility', '{broken');
  expect(readPreferences()).toEqual(DEFAULT_PREFERENCES);
});
it('persists only visual preferences', () => {
  persistPreferences({ uiFontScale: 2, highContrast: true });
  expect(readPreferences()).toEqual({ uiFontScale: 2, highContrast: true });
  expect(localStorage.length).toBe(1);
});
