import { afterEach, describe, expect, it, vi } from 'vitest';
import { act, cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import App from './App';
import { clearAccessToken, getAccessToken } from './lib/apiClient';

afterEach(() => { cleanup(); vi.useRealTimers(); localStorage.clear(); clearAccessToken(); });
const renderApp = (path = '/autentificare') => render(<MemoryRouter initialEntries={[path]}><App /></MemoryRouter>);

describe('E1-IO-01 login and accessibility', () => {
  it('keeps login disabled until valid fields and associates validation errors', async () => {
    renderApp();
    const submit = screen.getByRole('button', { name: 'Intră în cont' }) as HTMLButtonElement;
    expect(submit.disabled).toBe(true);
    const email = screen.getByLabelText(/Adresă de e-mail/);
    await userEvent.type(email, 'invalid');
    await userEvent.tab();
    expect(email.getAttribute('aria-describedby')).toBe('email-error');
    expect(submit.disabled).toBe(true);
    await userEvent.clear(email);
    await userEvent.type(email, 'demo@example.ro');
    await userEvent.type(screen.getByLabelText(/^Parolă/), 'example');
    expect(submit.disabled).toBe(false);
  });

  it('rejects incorrect credentials without exposing whether the account exists', async () => {
    renderApp();
    await userEvent.type(screen.getByLabelText(/Adresă de e-mail/), 'missing@example.ro');
    await userEvent.type(screen.getByLabelText(/^Parolă/), 'wrong');
    await userEvent.click(screen.getByRole('button', { name: 'Intră în cont' }));
    expect((await screen.findByRole('alert')).textContent).toContain('Adresa de e-mail sau parola este incorectă');
    expect(getAccessToken()).toBeNull();
  });

  it.each([
    ['pacient.demo@ophthacloud.ro', 'Pacient'],
    ['receptie.demo@ophthacloud.ro', 'Recepție'],
    ['dr.popescu@ophthacloud.ro', 'Medic'],
  ])('routes %s directly to the correct workspace', async (email, heading) => {
    renderApp();
    await userEvent.type(screen.getByLabelText(/Adresă de e-mail/), email);
    await userEvent.type(screen.getByLabelText(/^Parolă/), 'OphthaDemo!2026#19');
    await userEvent.click(screen.getByRole('button', { name: 'Intră în cont' }));
    expect(await screen.findByRole('heading', { name: heading })).toBeTruthy();
    expect(getAccessToken()).toBeTruthy();
    await userEvent.click(screen.getByRole('button', { name: 'Ieși din cont' }));
    expect(await screen.findByRole('heading', { name: 'Intră în cont' })).toBeTruthy();
    expect(getAccessToken()).toBeNull();
  });

  it('preserves explicit pre-login preferences through authentication and logout', async () => {
    renderApp();
    await userEvent.click(screen.getByRole('button', { name: 'Setări de accesibilitate' }));
    await userEvent.click(screen.getByRole('button', { name: 'Mărește textul' }));
    await userEvent.click(screen.getByRole('switch', { name: 'Contrast ridicat' }));
    await userEvent.type(screen.getByLabelText(/Adresă de e-mail/), 'pacient.demo@ophthacloud.ro');
    await userEvent.type(screen.getByLabelText(/^Parolă/), 'OphthaDemo!2026#19');
    await userEvent.click(screen.getByRole('button', { name: 'Intră în cont' }));
    await screen.findByRole('heading', { name: 'Pacient' });
    await waitFor(() => expect(document.documentElement.dataset.highContrast).toBe('true'));
    expect(document.documentElement.style.getPropertyValue('--font-scale')).toBe('1.2');
    await userEvent.click(screen.getByRole('button', { name: 'Ieși din cont' }));
    expect(document.documentElement.style.getPropertyValue('--font-scale')).toBe('1.2');
    expect(Object.keys(localStorage)).toEqual(['ophthacloud-accessibility']);
  });

  it('requires login for protected direct links', async () => {
    renderApp('/medic');
    expect(await screen.findByRole('heading', { name: 'Intră în cont' })).toBeTruthy();
    expect(screen.queryByRole('heading', { name: 'Medic' })).toBeNull();
  });

  it('expires the session without an automatic retry loop', async () => {
    renderApp();
    await userEvent.type(screen.getByLabelText(/Adresă de e-mail/), 'dr.popescu@ophthacloud.ro');
    await userEvent.type(screen.getByLabelText(/^Parolă/), 'OphthaDemo!2026#19');
    await userEvent.click(screen.getByRole('button', { name: 'Intră în cont' }));
    await screen.findByRole('heading', { name: 'Medic' });
    vi.useFakeTimers();
    act(() => { vi.advanceTimersByTime(900_001); window.dispatchEvent(new Event('focus')); });
    expect(screen.getByRole('heading', { name: 'Intră în cont' })).toBeTruthy();
    expect(screen.getByText(/Sesiunea a expirat/)).toBeTruthy();
    expect(getAccessToken()).toBeNull();
  });

  it('supports the complete text scale with explicit bounds', async () => {
    renderApp();
    await userEvent.click(screen.getByRole('button', { name: 'Setări de accesibilitate' }));
    expect((screen.getByRole('button', { name: 'Micșorează textul' }) as HTMLButtonElement).disabled).toBe(true);
    for (let step = 0; step < 4; step++) await userEvent.click(screen.getByRole('button', { name: 'Mărește textul' }));
    expect(document.documentElement.style.getPropertyValue('--font-scale')).toBe('2');
    expect((screen.getByRole('button', { name: 'Mărește textul' }) as HTMLButtonElement).disabled).toBe(true);
  });

  it('uses password icons as actions without changing the entered password', async () => {
    renderApp();
    const password = screen.getByLabelText(/^Parolă/);
    await userEvent.type(password, 'example');
    expect(password.getAttribute('type')).toBe('password');
    await userEvent.click(screen.getByRole('button', { name: 'Arată parola' }));
    expect(password.getAttribute('type')).toBe('text');
    expect(screen.getByRole('button', { name: 'Ascunde parola' }).querySelector('.lucide-eye-off')).toBeTruthy();
    await userEvent.click(screen.getByRole('button', { name: 'Ascunde parola' }));
    expect(password.getAttribute('type')).toBe('password');
    expect((password as HTMLInputElement).value).toBe('example');
  });

  it('opens account information from the keyboard and dismisses it with Escape', async () => {
    renderApp();
    const info = screen.getByRole('button', { name: 'Informații despre cont' });
    act(() => info.focus());
    expect(screen.getByRole('tooltip').textContent).toContain('primite de la clinică');
    await userEvent.keyboard('{Escape}');
    expect(screen.queryByRole('tooltip')).toBeNull();
  });

  it('closes accessibility settings on Escape and returns focus to the trigger', async () => {
    renderApp();
    const trigger = screen.getByRole('button', { name: 'Setări de accesibilitate' });
    await userEvent.click(trigger);
    await userEvent.click(screen.getByRole('button', { name: 'Mărește textul' }));
    await userEvent.keyboard('{Escape}');
    expect(trigger.getAttribute('aria-expanded')).toBe('false');
    expect(document.activeElement).toBe(trigger);
    expect(screen.queryByRole('button', { name: 'Mărește textul' })).toBeNull();
  });

});
