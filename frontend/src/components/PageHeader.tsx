import React from 'react';
import { useApp, UserRole } from '@/contexts/AppContext';
import { Search, Bell, MapPin, ChevronDown, LogOut } from 'lucide-react';

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
  const { role, setRole, logout } = useApp();

  return (
    <header className="h-14 bg-card border-b border-border shadow-sm sticky top-0 z-30 flex items-center justify-between px-4">
      {/* Left: Breadcrumbs */}
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

      {/* Center: Role switcher demo */}
      <div className="flex items-center gap-1 bg-muted rounded-full p-0.5">
        {(Object.keys(roleLabels) as UserRole[]).map((r) => (
          <button
            key={r}
            onClick={() => setRole(r)}
            className={`px-3 py-1 rounded-full text-[11px] font-semibold transition-all ${
              role === r
                ? 'text-white shadow-sm'
                : 'text-muted-foreground hover:text-foreground'
            }`}
            style={role === r ? { backgroundColor: roleColors[r] } : {}}
          >
            {roleLabels[r]}
          </button>
        ))}
      </div>

      {/* Right: Actions */}
      <div className="flex items-center gap-2">
        <button className="w-8 h-8 rounded-md flex items-center justify-center text-muted-foreground hover:bg-muted transition-colors">
          <Search className="w-4 h-4" />
        </button>
        <button className="w-8 h-8 rounded-md flex items-center justify-center text-muted-foreground hover:bg-muted relative transition-colors">
          <Bell className="w-4 h-4" />
          <span className="absolute -top-0.5 -right-0.5 w-4 h-4 bg-clinical-danger text-white text-[9px] font-bold rounded-full flex items-center justify-center">3</span>
        </button>
        <div className="flex items-center gap-1 px-2 py-1 rounded-md text-clinical-sm text-muted-foreground hover:bg-muted cursor-pointer transition-colors">
          <MapPin className="w-3.5 h-3.5" />
          <span>București</span>
          <ChevronDown className="w-3 h-3" />
        </div>
        <button
          onClick={logout}
          className="w-8 h-8 rounded-md flex items-center justify-center text-muted-foreground hover:bg-muted hover:text-clinical-danger transition-colors"
          title="Deconectare"
        >
          <LogOut className="w-4 h-4" />
        </button>
      </div>
    </header>
  );
};

export default PageHeader;
