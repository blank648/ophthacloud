import { useEffect, useLayoutEffect, useRef, useState } from 'react';
import { ChevronDown, Circle, Minus, PersonStanding, Plus } from 'lucide-react';
import { FONT_SCALE_STEPS } from '../features/accessibility/preferences';
import { useSession } from '../features/auth/SessionProvider';

// Cerinta E1-IO-01 / CIC-06.1: aceleasi setari inainte si dupa autentificare.
export function AccessibilityBar() {
  const { preferences, updatePreferences, preferenceNotice } = useSession();
  const [open, setOpen] = useState(false);
  const container = useRef<HTMLElement>(null);
  const trigger = useRef<HTMLButtonElement>(null);
  const panel = useRef<HTMLDivElement>(null);
  const index = FONT_SCALE_STEPS.findIndex((step) => step === preferences.uiFontScale);

  useLayoutEffect(() => {
    if (!open) return;
    // Pastreaza toate optiunile accesibile chiar cand textul mare mareste antetul.
    const fitPanel = () => {
      if (panel.current && trigger.current) {
        panel.current.style.maxHeight = `${Math.max(48, window.innerHeight - trigger.current.getBoundingClientRect().bottom - 28)}px`;
      }
    };
    fitPanel();
    const header = trigger.current?.closest('header');
    const observer = typeof ResizeObserver === 'undefined' ? undefined : new ResizeObserver(fitPanel);
    if (header) observer?.observe(header);
    window.addEventListener('resize', fitPanel);
    return () => { observer?.disconnect(); window.removeEventListener('resize', fitPanel); };
  }, [open]);

  useEffect(() => {
    if (!open) return;
    const dismissOutside = (event: Event) => {
      if (!container.current?.contains(event.target as Node)) setOpen(false);
    };
    const dismissOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') { setOpen(false); trigger.current?.focus(); }
    };
    document.addEventListener('pointerdown', dismissOutside);
    document.addEventListener('focusin', dismissOutside);
    document.addEventListener('keydown', dismissOnEscape);
    return () => {
      document.removeEventListener('pointerdown', dismissOutside);
      document.removeEventListener('focusin', dismissOutside);
      document.removeEventListener('keydown', dismissOnEscape);
    };
  }, [open]);

  function changeSize(direction: number): void {
    const next = FONT_SCALE_STEPS[index + direction];
    if (next !== undefined) updatePreferences({ ...preferences, uiFontScale: next });
  }

  return <section className="settings" ref={container} aria-label="Setări de accesibilitate">
    <button ref={trigger} type="button" className="settings-trigger" aria-expanded={open}
      aria-controls="settings-panel" aria-label="Setări de accesibilitate" onClick={() => setOpen(!open)}>
      <span className="universal-access" aria-hidden="true"><Circle /><PersonStanding /></span>
      <span className="trigger-text">Accesibilitate</span><ChevronDown className="chevron" aria-hidden="true" />
    </button>
    <div className="settings-panel" id="settings-panel" ref={panel} hidden={!open}>
      <h2>Setări de afișare</h2>
      <div className="text-setting">
        <span id="size-name">Dimensiunea textului</span>
        <div className="font-stepper" role="group" aria-labelledby="size-name">
          <button type="button" className="icon-button" aria-label="Micșorează textul" disabled={index === 0}
            onClick={() => changeSize(-1)}><Minus aria-hidden="true" /></button>
          <output aria-live="polite" aria-atomic="true">{Math.round(preferences.uiFontScale * 100)}%</output>
          <button type="button" className="icon-button" aria-label="Mărește textul" disabled={index === FONT_SCALE_STEPS.length - 1}
            onClick={() => changeSize(1)}><Plus aria-hidden="true" /></button>
        </div>
      </div>
      <div className="contrast-row">
        <div className="contrast-copy"><strong id="contrast-name">Contrast ridicat</strong>
          <output aria-live="polite">{preferences.highContrast ? 'Activ' : 'Oprit'}</output></div>
        <button type="button" className="switch" role="switch" aria-checked={preferences.highContrast}
          aria-labelledby="contrast-name" onClick={() => updatePreferences({ ...preferences, highContrast: !preferences.highContrast })}>
          <span className="switch-track" aria-hidden="true"><span className="switch-thumb" /></span>
        </button>
      </div>
      {preferenceNotice && <p className="preference-notice" aria-hidden="true">{preferenceNotice}</p>}
    </div>
    <p className="visually-hidden" role="status">{preferenceNotice}</p>
  </section>;
}
