import React, { useEffect, useState } from 'react';
import { useApp, UserRole } from '@/contexts/AppContext';
import { logout as keycloakLogout } from '@/lib/auth';
import { Search, Bell, MapPin, ChevronDown, LogOut, Sun, Moon, Menu } from 'lucide-react';
import { useNotificationLogs } from '@/hooks/useNotifications';
import CommandPalette from '@/components/CommandPalette';

const roleLabels: Record<UserRole, string> = {
  doctor: 'Doctor',
  receptionist: 'Recepție',
  manager: 'Manager',
  optician: 'Optician',
  patient: 'Pacient',
};

const roleColors: Record<UserRole, string> = {
  doctor: '#13759C',
  receptionist: '#10B981',
  manager: '#8B5CF6',
  optician: '#F59E0B',
  patient: '#06B6D4',
};

const PageHeader: React.FC<{ breadcrumbs?: { label: string; path?: string }[] }> = ({ breadcrumbs = [] }) => {
  const { role, setRole, logout, darkMode, toggleDarkMode, toggleSidebar } = useApp();
  const [paletteOpen, setPaletteOpen] = useState(false);
  const { data: logsData } = useNotificationLogs();
  const unreadCount = logsData?.data?.content?.filter((log: any) => log.status === 'FAILED').length || 0;

  // ⌘K / Ctrl+K shortcut
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault();
        setPaletteOpen(true);
      }
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, []);

  return (
    <>
      <header className="h-14 bg-card border-b border-border shadow-sm sticky top-0 z-30 flex items-center justify-between px-4">
        {/* Left: sidebar toggle + breadcrumbs */}
        <div className="flex items-center gap-3">
          <button
            onClick={toggleSidebar}
            className="w-8 h-8 rounded-md flex items-center justify-center text-muted-foreground hover:bg-muted hover:text-foreground transition-colors"
            title="Comută bara laterală"
          >
            <Menu className="w-4 h-4" />
          </button>
          <div className="flex items-center gap-1 text-clinical-sm text-muted-foreground">
            {breadcrumbs.map((b, i) => (
              <React.Fragment key={i}>
                {i > 0 && <span className="mx-1">/</span>}
                <span className={i === breadcrumbs.length - 1 ? 'text-foreground font-semibold' : ''}>
                  {b.label}
                </span>
              </React.Fragment>
            ))}
          </div>
        </div>

        {/* Center: Blank space where role switcher used to be */}
        <div className="flex-1"></div>

        {/* Right: Actions */}
        <div className="flex items-center gap-2">
          <button
            onClick={() => setPaletteOpen(true)}
            className="hidden md:flex items-center gap-2 px-2.5 h-8 rounded-md border border-border bg-muted/40 hover:bg-muted text-muted-foreground transition-colors"
            title="Caută (Ctrl/⌘+K)"
          >
            <Search className="w-3.5 h-3.5" />
            <span className="text-clinical-xs">Caută...</span>
            <kbd className="text-[9px] font-clinical border border-border rounded px-1 py-px">⌘K</kbd>
          </button>
          <button
            onClick={() => setPaletteOpen(true)}
            className="md:hidden w-8 h-8 rounded-md flex items-center justify-center text-muted-foreground hover:bg-muted transition-colors"
          >
            <Search className="w-4 h-4" />
          </button>
          <button
            onClick={toggleDarkMode}
            className="w-8 h-8 rounded-md flex items-center justify-center text-muted-foreground hover:bg-muted hover:text-foreground transition-colors"
            title={darkMode ? 'Mod luminos' : 'Mod întunecat'}
          >
            {darkMode ? <Sun className="w-4 h-4" /> : <Moon className="w-4 h-4" />}
          </button>
          <button className="w-8 h-8 rounded-md flex items-center justify-center text-muted-foreground hover:bg-muted relative transition-colors" title="Notificări">
            <Bell className="w-4 h-4" />
            {unreadCount > 0 && (
              <span className="absolute -top-0.5 -right-0.5 w-4 h-4 bg-destructive text-white text-[9px] font-bold rounded-full flex items-center justify-center">
                {unreadCount}
              </span>
            )}
          </button>
          <div className="hidden md:flex items-center gap-1 px-2 py-1 rounded-md text-clinical-sm text-muted-foreground hover:bg-muted cursor-pointer transition-colors">
            <MapPin className="w-3.5 h-3.5" />
            <span>București</span>
            <ChevronDown className="w-3 h-3" />
          </div>
          <button
            onClick={keycloakLogout}
            className="w-8 h-8 rounded-md flex items-center justify-center text-muted-foreground hover:bg-muted hover:text-destructive transition-colors"
            title="Deconectare"
          >
            <LogOut className="w-4 h-4" />
          </button>
        </div>
      </header>
      <CommandPalette open={paletteOpen} onClose={() => setPaletteOpen(false)} />
    </>
  );
};

export default PageHeader;
