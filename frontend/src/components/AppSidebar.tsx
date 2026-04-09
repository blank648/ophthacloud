import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useApp } from '@/contexts/AppContext';
import OphthaLogo from '@/components/OphthaLogo';
import {
  LayoutDashboard, Users, CalendarDays, Stethoscope, Eye, FileText,
  Glasses, Bell, BarChart3, Settings2, Package, FlaskConical, CreditCard,
  Shield, ChevronLeft, ChevronRight, Moon, Sun, UserCircle
} from 'lucide-react';

interface NavItem {
  label: string;
  icon: React.ElementType;
  path: string;
  roles?: string[];
}

interface NavGroup {
  label: string;
  items: NavItem[];
}

const navGroups: NavGroup[] = [
  {
    label: 'PRINCIPAL',
    items: [
      { label: 'Dashboard', icon: LayoutDashboard, path: '/dashboard' },
      { label: 'Pacienți', icon: Users, path: '/patients' },
      { label: 'Programări', icon: CalendarDays, path: '/appointments' },
    ],
  },
  {
    label: 'CLINIC',
    items: [
      { label: 'Consultație EMR', icon: Stethoscope, path: '/consultation' },
      { label: 'Investigații', icon: Eye, path: '/investigations' },
      { label: 'Rețete', icon: FileText, path: '/prescriptions' },
    ],
  },
  {
    label: 'OPTIC & COMERCIAL',
    items: [
      { label: 'ERP Optic', icon: Glasses, path: '/optical' },
      { label: 'Stocuri', icon: Package, path: '/inventory' },
      { label: 'Laborator', icon: FlaskConical, path: '/lab' },
      { label: 'Facturare & POS', icon: CreditCard, path: '/billing' },
    ],
  },
  {
    label: 'ADMINISTRARE',
    items: [
      { label: 'Rapoarte & KPI', icon: BarChart3, path: '/reports' },
      { label: 'Notificări', icon: Bell, path: '/notifications' },
      { label: 'Portal Pacient', icon: UserCircle, path: '/portal' },
      { label: 'Setări & Admin', icon: Settings2, path: '/settings' },
      { label: 'Jurnal Audit', icon: Shield, path: '/audit' },
    ],
  },
];

const AppSidebar: React.FC = () => {
  const { sidebarCollapsed, toggleSidebar, darkMode, toggleDarkMode, role } = useApp();
  const location = useLocation();

  const roleLabels: Record<string, string> = {
    doctor: 'Medic Oftalmolog',
    receptionist: 'Recepție',
    manager: 'Manager Clinic',
    optician: 'Optician',
    patient: 'Pacient',
  };

  return (
    <aside
      className="fixed left-0 top-0 h-screen flex flex-col z-40 transition-all duration-200 ease-in-out"
      style={{
        width: sidebarCollapsed ? 64 : 240,
        backgroundColor: 'hsl(var(--color-bg-sidebar))',
      }}
    >
      {/* Logo */}
      <div className="h-14 flex items-center px-4 border-b border-white/10">
        <OphthaLogo size={28} showText={!sidebarCollapsed} />
      </div>

      {/* Clinic name */}
      {!sidebarCollapsed && (
        <div className="px-4 py-2">
          <p className="text-[11px] text-primary-400 truncate">Clinica Oftalmologică Visiomed</p>
        </div>
      )}

      {/* Navigation */}
      <nav className="flex-1 overflow-y-auto py-2 px-2">
        {navGroups.map((group) => (
          <div key={group.label} className="mb-4">
            {!sidebarCollapsed && (
              <p className="px-3 pt-4 pb-2 text-[11px] font-semibold tracking-[0.10em] uppercase text-primary-400">
                {group.label}
              </p>
            )}
            {group.items.map((item) => {
              const isActive = location.pathname === item.path || location.pathname.startsWith(item.path + '/');
              return (
                <Link
                  key={item.path}
                  to={item.path}
                  className={`flex items-center gap-3 h-[44px] px-3 mx-1 rounded-md transition-all duration-150 group
                    ${isActive
                      ? 'bg-[hsl(var(--color-bg-sidebar-active))] text-white border-l-[3px] border-white'
                      : 'text-white/70 hover:bg-[hsl(var(--color-bg-sidebar-hover))] hover:text-white border-l-[3px] border-transparent'
                    }`}
                  title={sidebarCollapsed ? item.label : undefined}
                >
                  <item.icon className={`w-5 h-5 shrink-0 ${isActive ? 'text-white' : 'text-primary-300'}`} />
                  {!sidebarCollapsed && (
                    <span className={`text-[13px] truncate ${isActive ? 'font-semibold text-white' : 'font-medium'}`}>
                      {item.label}
                    </span>
                  )}
                </Link>
              );
            })}
          </div>
        ))}
      </nav>

      {/* Bottom section */}
      <div className="border-t border-white/10 p-3 space-y-2">
        {/* Dark mode toggle */}
        <button
          onClick={toggleDarkMode}
          className="flex items-center gap-3 w-full h-9 px-3 rounded-md text-white/70 hover:bg-[hsl(var(--color-bg-sidebar-hover))] hover:text-white transition-colors"
        >
          {darkMode ? <Sun className="w-4 h-4 text-primary-300" /> : <Moon className="w-4 h-4 text-primary-300" />}
          {!sidebarCollapsed && <span className="text-[13px]">{darkMode ? 'Light Mode' : 'Dark Mode'}</span>}
        </button>

        {/* User info */}
        {!sidebarCollapsed && (
          <div className="flex items-center gap-3 px-3 py-2">
            <div className="w-8 h-8 rounded-full bg-primary-700 flex items-center justify-center text-white text-[11px] font-semibold">
              AP
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-[13px] text-white font-medium truncate">Dr. Alex Popescu</p>
              <span className="inline-block px-1.5 py-0.5 rounded text-[10px] font-semibold bg-primary-600 text-white">
                {roleLabels[role]}
              </span>
            </div>
          </div>
        )}

        {/* Collapse toggle */}
        <button
          onClick={toggleSidebar}
          className="flex items-center justify-center w-full h-8 rounded-md text-white/50 hover:text-white hover:bg-[hsl(var(--color-bg-sidebar-hover))] transition-colors"
        >
          {sidebarCollapsed ? <ChevronRight className="w-4 h-4" /> : <ChevronLeft className="w-4 h-4" />}
        </button>
      </div>
    </aside>
  );
};

export default AppSidebar;
