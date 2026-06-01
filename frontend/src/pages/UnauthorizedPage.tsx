import React from 'react';
import { useNavigate } from 'react-router-dom';
import { ShieldOff, LogOut } from 'lucide-react';
import { logout } from '@/lib/auth';

const UnauthorizedPage: React.FC = () => {
  const navigate = useNavigate();
  return (
    <div className="min-h-screen flex items-center justify-center bg-background p-6">
      <div className="max-w-md w-full bg-card border border-border rounded-xl shadow-sm p-8 text-center">
        <div className="w-14 h-14 rounded-full bg-destructive/10 text-destructive flex items-center justify-center mx-auto mb-4">
          <ShieldOff className="w-7 h-7" />
        </div>
        <h1 className="text-clinical-xl font-bold text-foreground mb-2">Acces interzis</h1>
        <p className="text-clinical-sm text-muted-foreground mb-6">
          Nu ai permisiunea pentru această acțiune. Dacă tocmai ai primit acces, trebuie să te deconectezi și să te reconectezi pentru a-ți actualiza permisiunile.
        </p>
        <div className="flex gap-3 justify-center">
          <button
            onClick={() => navigate('/dashboard')}
            className="px-4 py-2 rounded-lg bg-muted text-foreground text-clinical-sm font-semibold hover:bg-muted/80"
          >
            Înapoi la Dashboard
          </button>
          <button
            onClick={() => logout()}
            className="px-4 py-2 rounded-lg bg-primary flex items-center gap-2 text-primary-foreground text-clinical-sm font-semibold hover:opacity-90"
          >
            <LogOut className="w-4 h-4" /> Re-Autentificare
          </button>
        </div>
      </div>
    </div>
  );
};

export default UnauthorizedPage;
