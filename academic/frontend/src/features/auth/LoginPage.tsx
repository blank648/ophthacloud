import { useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { Eye, EyeOff, LoaderCircle } from 'lucide-react';
import { ApiClientError } from '../../lib/apiClient';
import { AccountInfo } from '../../components/AccountInfo';
import mascot from '../../assets/brand/ophthacloud-mascot.svg';
import { useSession } from './SessionProvider';

export function LoginPage() {
  const { signIn, notice } = useSession();
  const [showPassword, setShowPassword] = useState(false);
  const [pending, setPending] = useState(false);
  const [canSubmit, setCanSubmit] = useState(false);
  const [supportOpen, setSupportOpen] = useState(false);
  const [error, setError] = useState('');
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const emailRef = useRef<HTMLInputElement>(null);
  const passwordRef = useRef<HTMLInputElement>(null);
  const errorRef = useRef<HTMLDivElement>(null);
  const submitting = useRef(false);

  function updateValidity(): void {
    const email = emailRef.current;
    const password = passwordRef.current;
    setCanSubmit(Boolean(email?.value.trim() && email.validity.valid && password?.value));
  }

  function validateField(name: 'email' | 'password'): void {
    const field = name === 'email' ? emailRef.current : passwordRef.current;
    if (!field) return;
    let message = '';
    if (!field.value.trim() && name === 'email') message = 'Introdu adresa de e-mail.';
    else if (name === 'email' && !field.validity.valid) message = 'Introdu o adresă de e-mail validă.';
    else if (name === 'password' && !field.value) message = 'Introdu parola.';
    setFieldErrors((previous) => ({ ...previous, [name]: message }));
    updateValidity();
  }

  // Cerinta E1-IO-01: formular accesibil conectat la adaptorul mock/live existent.
  async function handleSubmit(event: FormEvent<HTMLFormElement>): Promise<void> {
    event.preventDefault();
    if (submitting.current) return;
    const email = emailRef.current!;
    const password = passwordRef.current!;
    if (!email.value.trim() || !email.validity.valid || !password.value) {
      validateField('email'); validateField('password');
      (!email.validity.valid ? email : password).focus();
      return;
    }
    submitting.current = true;
    setPending(true);
    setError('');
    try {
      await signIn({ email: email.value.trim(), password: password.value });
    } catch (failure) {
      setError(failure instanceof ApiClientError && failure.status === 401
        ? 'Adresa de e-mail sau parola este incorectă. Verifică datele și încearcă din nou.'
        : 'Nu ne putem conecta momentan. Verifică legătura la internet și încearcă din nou.');
      requestAnimationFrame(() => errorRef.current?.focus());
    } finally {
      submitting.current = false;
      setPending(false);
    }
  }

  return <>
    <div className="login-column">
      <section className="panel" aria-labelledby="login-title">
        <div className="title-row"><h1 id="login-title">Intră în cont</h1></div>
        {notice && <p className="notice" role="status">{notice}</p>}
        <form onSubmit={handleSubmit} onInput={updateValidity} onChange={updateValidity} noValidate aria-busy={pending}>
          <div className="field email-field">
            <AccountInfo />
            <input ref={emailRef} id="email" name="email" type="email" autoComplete="username" autoCapitalize="none"
              spellCheck={false} required readOnly={pending} placeholder="nume@exemplu.ro"
              onBlur={() => validateField('email')} aria-invalid={Boolean(fieldErrors.email)}
              aria-describedby={fieldErrors.email ? 'email-error' : undefined} />
            {fieldErrors.email && <p className="field-error" id="email-error">{fieldErrors.email}</p>}
          </div>
          <div className="field">
            <label htmlFor="password">Parolă <span className="visually-hidden">(obligatoriu)</span></label>
            <div className="password">
              <input ref={passwordRef} id="password" name="password" type={showPassword ? 'text' : 'password'}
                autoComplete="current-password" required readOnly={pending} onBlur={() => validateField('password')}
                aria-invalid={Boolean(fieldErrors.password)} aria-describedby={fieldErrors.password ? 'password-error' : undefined} />
              <button type="button" className="icon-button" aria-label={showPassword ? 'Ascunde parola' : 'Arată parola'}
                aria-pressed={showPassword} onClick={() => setShowPassword(!showPassword)}>
                {showPassword ? <EyeOff aria-hidden="true" /> : <Eye aria-hidden="true" />}
              </button>
            </div>
            {fieldErrors.password && <p className="field-error" id="password-error">{fieldErrors.password}</p>}
          </div>
          {error && <div ref={errorRef} tabIndex={-1} className="error-message" role="alert">{error}</div>}
          <button className="primary" type="submit" disabled={pending || !canSubmit}>
            {pending && <LoaderCircle className="spinner" aria-hidden="true" />}
            <span>{pending ? 'Se verifică datele…' : 'Intră în cont'}</span>
          </button>
        </form>
        <div className="help-row"><span>Ai nevoie de ajutor?</span>
          <button type="button" aria-expanded={supportOpen} aria-controls="support" onClick={() => setSupportOpen(!supportOpen)}>Contactează recepția</button>
        </div>
        <p id="support" className="support" hidden={!supportOpen}>Pentru datele de acces sau resetarea parolei, contactează recepția clinicii tale.</p>
      </section>
    </div>
    <div className="mascot-illustration" aria-hidden="true">
      <img src={mascot} width="914" height="961" alt="" draggable={false} decoding="async" />
    </div>
  </>;
}
