import { useEffect, useLayoutEffect, useRef, useState } from 'react';
import { Info } from 'lucide-react';

export function AccountInfo() {
  const [open, setOpen] = useState(false);
  const trigger = useRef<HTMLButtonElement>(null);
  const tooltip = useRef<HTMLDivElement>(null);
  const wrapper = useRef<HTMLDivElement>(null);
  const hideTimer = useRef<ReturnType<typeof setTimeout>>();

  function cancelHide(): void { clearTimeout(hideTimer.current); }
  function show(): void { cancelHide(); setOpen(true); }
  function scheduleHide(): void {
    cancelHide();
    hideTimer.current = setTimeout(() => {
      if (document.activeElement !== trigger.current && !tooltip.current?.matches(':hover') && !wrapper.current?.matches(':hover')) setOpen(false);
    }, 160);
  }

  useLayoutEffect(() => {
    if (!open) return;
    const position = () => {
      const note = tooltip.current;
      const button = trigger.current;
      if (!note || !button) return;
      // Pe desktop folosim spatiul liber; pe mobil mesajul ocupa propriul rand.
      note.classList.remove('inline');
      note.style.left = '0px'; note.style.top = '0px';
      const anchor = button.getBoundingClientRect();
      const rect = note.getBoundingClientRect();
      if (anchor.right + 8 + rect.width <= window.innerWidth - 16 && rect.height <= anchor.height) {
        note.style.left = `${anchor.right + 8}px`;
        note.style.top = `${anchor.top + (anchor.height - rect.height) / 2}px`;
      } else {
        note.classList.add('inline'); note.style.left = ''; note.style.top = '';
      }
    };
    position();
    window.addEventListener('resize', position);
    return () => window.removeEventListener('resize', position);
  }, [open]);

  useEffect(() => {
    if (!open) return;
    const dismiss = (event: KeyboardEvent) => { if (event.key === 'Escape') { cancelHide(); setOpen(false); } };
    const dismissOutside = (event: PointerEvent) => {
      const target = event.target as Node;
      if (!wrapper.current?.contains(target) && !tooltip.current?.contains(target)) setOpen(false);
    };
    const dismissFloatingOnScroll = () => {
      if (!tooltip.current?.classList.contains('inline')) setOpen(false);
    };
    document.addEventListener('keydown', dismiss);
    document.addEventListener('pointerdown', dismissOutside);
    document.addEventListener('scroll', dismissFloatingOnScroll, true);
    return () => {
      document.removeEventListener('keydown', dismiss);
      document.removeEventListener('pointerdown', dismissOutside);
      document.removeEventListener('scroll', dismissFloatingOnScroll, true);
    };
  }, [open]);
  useEffect(() => () => clearTimeout(hideTimer.current), []);

  return <>
    <div className="label-row"><label htmlFor="email">Adresă de e-mail <span className="visually-hidden">(obligatoriu)</span></label>
    <div className="info-wrap" ref={wrapper} onPointerEnter={show} onPointerLeave={scheduleHide}>
      <button className="icon-button" type="button" ref={trigger} aria-label="Informații despre cont"
        aria-describedby="account-tooltip" onFocus={show} onBlur={scheduleHide} onClick={show}>
        <Info className="info-symbol" aria-hidden="true" />
      </button>
    </div></div>
    <div id="account-tooltip" className="tooltip" role="tooltip" ref={tooltip} hidden={!open}
      onPointerEnter={cancelHide} onPointerLeave={scheduleHide}>Folosește datele de acces primite de la clinică.</div>
  </>;
}
