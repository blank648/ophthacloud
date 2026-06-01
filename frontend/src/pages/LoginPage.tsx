import React from 'react';
import { login } from '@/lib/auth';
import { Shield } from 'lucide-react';

const LoginPage: React.FC = () => {
  return (
    <div className="min-h-screen flex items-center justify-center bg-background">
      <div className="flex flex-col items-center gap-6 p-8 bg-card border border-border rounded-xl shadow-sm max-w-sm w-full">
        <div className="w-16 h-16 rounded-full bg-primary/10 flex items-center justify-center">
          <Shield className="w-8 h-8 text-primary" />
        </div>
        <div className="text-center space-y-2">
          <h1 className="text-2xl font-bold text-foreground">OphthaCloud</h1>
          <p className="text-clinical-sm text-muted-foreground">Autentificare securizată prin Keycloak</p>
        </div>
        <button
          onClick={() => login()}
          className="w-full h-11 rounded-md bg-primary hover:bg-primary-600 text-white font-semibold text-clinical-base transition-colors"
        >
          Login
        </button>
      </div>
    </div>
  );
};

export default LoginPage;
