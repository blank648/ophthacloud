import React, { useState } from 'react';
import { Shield } from 'lucide-react';
import { useAuthStore } from '@/stores/authStore';

export default function LoginPage() {
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setErrorMsg(null);

    try {
      const { keycloak } = await import('@/lib/auth');
      
      const tokenEndpoint = `${keycloak.authServerUrl}/realms/${keycloak.realm}/protocol/openid-connect/token`;

      const params = new URLSearchParams();
      params.append('client_id', keycloak.clientId || 'ophthacloud-frontend');
      params.append('grant_type', 'password');
      params.append('scope', 'openid ophthacloud-claims');
      params.append('username', username);
      params.append('password', password);

      const response = await fetch(tokenEndpoint, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: params.toString(),
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.error_description || data.error || 'Autentificare eșuată');
      }

      // Save tokens directly
      useAuthStore.getState().setToken(data.access_token, data.refresh_token);
      
      // Reload the application so main.tsx handles Keycloak initialization cleanly
      window.location.href = '/';
      
    } catch (err: any) {
      console.error("Login error:", err);
      setErrorMsg(err.message || 'Eroare la autentificare');
    } finally {
      setIsLoading(false);
    }
  };

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
        
        {errorMsg && (
          <div className="w-full p-3 bg-red-50 text-red-600 border border-red-200 rounded-md text-sm break-words">
            {errorMsg}
          </div>
        )}

        <form onSubmit={handleLogin} className="w-full space-y-4">
          <div className="space-y-2">
            <label className="text-sm font-medium text-foreground">Utilizator</label>
            <input
              type="text"
              required
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              className="w-full h-10 px-3 rounded-md border border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent"
              placeholder="Nume utilizator"
              disabled={isLoading}
            />
          </div>
          
          <div className="space-y-2">
            <label className="text-sm font-medium text-foreground">Parolă</label>
            <input
              type="password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full h-10 px-3 rounded-md border border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent"
              placeholder="••••••••"
              disabled={isLoading}
            />
          </div>

          <button
            type="submit"
            disabled={isLoading}
            className="w-full h-11 rounded-md bg-primary hover:bg-primary-600 text-white font-semibold text-clinical-base transition-colors flex items-center justify-center disabled:opacity-50"
          >
            {isLoading ? 'Se autentifică...' : 'Login'}
          </button>
        </form>
      </div>
    </div>
  );
}
