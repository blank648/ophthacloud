import React, { useEffect, useState } from 'react';
import Keycloak from 'keycloak-js';

const PopupLogin: React.FC = () => {
  const [status, setStatus] = useState<string>('Se inițializează conexiunea securizată...');

  useEffect(() => {
    async function performLogin() {
      try {
        const keycloakUrl = import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8180';
        const realm = import.meta.env.VITE_KEYCLOAK_REALM || 'ophthacloud';
        const clientId = import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'ophthacloud-frontend';

        // We create a fresh local instance for the popup to avoid state conflicts
        const kc = new Keycloak({
          url: keycloakUrl,
          realm,
          clientId,
        });

        setStatus('Inițializare Keycloak în curs...');
        
        try {
          const authenticated = await kc.init({
            onLoad: 'check-sso',
            pkceMethod: 'S256',
            checkLoginIframe: false,
          });

          if (authenticated && kc.token && kc.refreshToken) {
            setStatus('Autentificare reușită! Se finalizează...');
            
            // Save the tokens directly to localStorage so the main window can pick them up
            localStorage.setItem('kc_token', kc.token);
            localStorage.setItem('kc_refresh_token', kc.refreshToken);

            // Emit event to main window using Tauri IPC
            const { emit } = await import('@tauri-apps/api/event');
            await emit('login-success', { authenticated: true });

            setStatus('Se închide fereastra...');
            // Close the popup window
            const { getCurrentWebviewWindow } = await import('@tauri-apps/api/webviewWindow');
            const currentWindow = getCurrentWebviewWindow();
            if (currentWindow) {
              await currentWindow.close();
            }
          } else {
            setStatus('Autentificare finalizată, dar token-ul lipsește.');
          }
        } catch (initErr) {
          console.error('Keycloak init error:', initErr);
          setStatus('Eroare Keycloak Init: ' + (initErr instanceof Error ? initErr.message : JSON.stringify(initErr)));
        }
      } catch (err) {
        console.error('Popup login error:', err);
        setStatus('Eroare internă în timpul autentificării.');
      }
    }

    performLogin();
  }, []);

  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-background p-6">
      <div className="w-12 h-12 rounded-full border-4 border-primary/20 border-t-primary animate-spin mb-4" />
      <h2 className="text-xl font-bold text-foreground text-center mb-2">OphthaCloud</h2>
      <p className="text-clinical-sm text-muted-foreground text-center">
        {status}
      </p>
    </div>
  );
};

export default PopupLogin;
