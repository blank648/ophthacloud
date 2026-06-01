import React, { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '@/stores/authStore';
import { login } from '@/lib/auth';
import { toast } from 'sonner';

interface AuthGuardProps {
  children: React.ReactNode;
  /**
   * When true, attempts a Keycloak login redirect on mount if unauthenticated.
   * Defaults to false so the existing demo login flow keeps working.
   */
  enforceKeycloak?: boolean;
}

/**
 * Wraps authenticated routes. While Keycloak init is running, shows a spinner.
 * When `enforceKeycloak` is true and the user is not authenticated, triggers
 * a Keycloak login redirect.
 *
 * Also listens for the `ophthacloud:forbidden` custom event dispatched by
 * apiClient when the backend returns 403, and navigates to /unauthorized via
 * React Router (no full page reload).
 */
export const AuthGuard: React.FC<AuthGuardProps> = ({ children, enforceKeycloak = false }) => {
  const isLoading = useAuthStore((s) => s.isLoading);
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const navigate = useNavigate();

  useEffect(() => {
    const handleForbidden = () => {
      toast.error('Acces interzis', {
        description: 'Nu aveți permisiunea necesară pentru această acțiune. Contactați administratorul.',
      });
      navigate('/unauthorized', { replace: false });
    };

    window.addEventListener('ophthacloud:forbidden', handleForbidden);
    return () => window.removeEventListener('ophthacloud:forbidden', handleForbidden);
  }, [navigate]);

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background">
        <div className="flex flex-col items-center gap-3">
          <div className="w-10 h-10 rounded-full border-4 border-primary/20 border-t-primary animate-spin" />
          <p className="text-clinical-sm text-muted-foreground">Se inițializează sesiunea…</p>
        </div>
      </div>
    );
  }

  if (enforceKeycloak && !isAuthenticated) {
    void login();
    return (
      <div className="min-h-screen flex items-center justify-center bg-background">
        <p className="text-clinical-sm text-muted-foreground">Redirecționare către autentificare…</p>
      </div>
    );
  }

  return <>{children}</>;
};
