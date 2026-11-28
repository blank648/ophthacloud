import { useEffect, useRef } from 'react';
import { LogOut } from 'lucide-react';
import { Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { AccessibilityBar } from './components/AccessibilityBar';
import { LoginPage } from './features/auth/LoginPage';
import { SessionProvider, useSession } from './features/auth/SessionProvider';
import logoHead from './assets/brand/ophthacloud-logo-head.svg';
import type { UserRole } from './types/api';

const ROLE_HOME: Record<UserRole, string> = {
  PATIENT: '/pacient', RECEPTIONIST: '/receptie', DOCTOR: '/medic', ADMIN: '/administrare',
};
const ROLE_LABEL: Record<UserRole, string> = {
  PATIENT: 'Pacient', RECEPTIONIST: 'Recepție', DOCTOR: 'Medic', ADMIN: 'Administrare',
};

function Workspace({ role }: { role: UserRole }) {
  const { user } = useSession();
  if (!user) return <Navigate to="/autentificare" replace />;
  if (user.role !== role) return <Navigate to={ROLE_HOME[user.role]} replace />;
  return <section className="workspace" aria-labelledby="workspace-title">
    <h1 id="workspace-title">{ROLE_LABEL[role]}</h1>
    <p>Te-ai autentificat cu succes.</p>
    <p className="muted">Spațiul de lucru este în pregătire. Setările de afișare rămân disponibile în bara de sus.</p>
  </section>;
}

function AppShell() {
  const { user, signOut } = useSession();
  const location = useLocation();
  const mainRef = useRef<HTMLElement>(null);
  const previousPath = useRef(location.pathname);
  useEffect(() => {
    document.title = `${user ? ROLE_LABEL[user.role] : 'Autentificare'} · OphthaCloud`;
    if (previousPath.current !== location.pathname) mainRef.current?.focus();
    previousPath.current = location.pathname;
  }, [location.pathname, user]);

  return <div className={`app-shell ${user ? 'workspace-shell' : 'login-shell'}`}>
    {user && <a className="skip-link" href="#main-content">Sari la conținut</a>}
    <header className="page-width header">
      <div className="brand">
        <img className="brand-logo" src={logoHead} width="40" height="40" alt="" draggable={false} />
        <span>OphthaCloud</span>
      </div>
      <div className="header-actions">
        {user && <div className="account-controls"><span className="account-role">{ROLE_LABEL[user.role]}</span>
          <button className="secondary-button" type="button" onClick={signOut}><LogOut aria-hidden="true" />Ieși din cont</button>
        </div>}
        <AccessibilityBar />
      </div>
    </header>
    <main className={`page-width main-content ${user ? '' : 'login-composition'}`} id="main-content" ref={mainRef} tabIndex={-1}>
      <Routes>
        <Route path="/autentificare" element={user ? <Navigate to={ROLE_HOME[user.role]} replace /> : <LoginPage />} />
        {(Object.keys(ROLE_HOME) as UserRole[]).map((role) => <Route key={role} path={ROLE_HOME[role]} element={<Workspace role={role} />} />)}
        <Route path="*" element={<Navigate to={user ? ROLE_HOME[user.role] : '/autentificare'} replace />} />
      </Routes>
    </main>
  </div>;
}

export default function App() {
  return <SessionProvider><AppShell /></SessionProvider>;
}
