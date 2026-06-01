import React, { useEffect, useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { Search, Users, FileText, Package, Settings2, CalendarDays, Stethoscope, BarChart3, Bell } from 'lucide-react';
import { patients } from '@/data/demo-data';
import { useData } from '@/contexts/DataContext';

interface CommandPaletteProps {
  open: boolean;
  onClose: () => void;
}

interface Item {
  id: string;
  label: string;
  category: string;
  icon: React.ElementType;
  action: () => void;
  hint?: string;
}

const CommandPalette: React.FC<CommandPaletteProps> = ({ open, onClose }) => {
  const navigate = useNavigate();
  const { prescriptions, orders } = useData();
  const [query, setQuery] = useState('');
  const [activeIdx, setActiveIdx] = useState(0);

  useEffect(() => {
    if (!open) {
      setQuery('');
      setActiveIdx(0);
    }
  }, [open]);

  const allItems: Item[] = useMemo(() => {
    const navItems: Item[] = [
      { id: 'nav-dashboard', label: 'Dashboard', category: 'Navigare', icon: BarChart3, action: () => navigate('/dashboard') },
      { id: 'nav-patients', label: 'Pacienți', category: 'Navigare', icon: Users, action: () => navigate('/patients') },
      { id: 'nav-appointments', label: 'Programări', category: 'Navigare', icon: CalendarDays, action: () => navigate('/appointments') },
      { id: 'nav-consultation', label: 'Consultație EMR', category: 'Navigare', icon: Stethoscope, action: () => navigate('/consultation') },
      { id: 'nav-prescriptions', label: 'Rețete', category: 'Navigare', icon: FileText, action: () => navigate('/prescriptions') },
      { id: 'nav-optical', label: 'ERP Optic', category: 'Navigare', icon: Package, action: () => navigate('/optical') },
      { id: 'nav-inventory', label: 'Stocuri', category: 'Navigare', icon: Package, action: () => navigate('/inventory') },
      { id: 'nav-billing', label: 'Facturare', category: 'Navigare', icon: FileText, action: () => navigate('/billing') },
      { id: 'nav-reports', label: 'Rapoarte & KPI', category: 'Navigare', icon: BarChart3, action: () => navigate('/reports') },
      { id: 'nav-notifications', label: 'Notificări', category: 'Navigare', icon: Bell, action: () => navigate('/notifications') },
      { id: 'nav-settings', label: 'Setări & Admin', category: 'Navigare', icon: Settings2, action: () => navigate('/settings') },
    ];

    const patientItems: Item[] = patients.map(p => ({
      id: p.id,
      label: p.name,
      category: 'Pacienți',
      icon: Users,
      hint: `${p.id} · ${p.primaryDiagnosis}`,
      action: () => navigate(`/patients/${p.id}`),
    }));

    const rxItems: Item[] = prescriptions.slice(0, 10).map(rx => ({
      id: rx.id,
      label: rx.id,
      category: 'Rețete',
      icon: FileText,
      hint: `${rx.patientName} · ${rx.date}`,
      action: () => navigate('/prescriptions'),
    }));

    const orderItems: Item[] = orders.slice(0, 10).map(o => ({
      id: o.id,
      label: o.id,
      category: 'Comenzi optice',
      icon: Package,
      hint: `${o.patientName} · ${o.lensType}`,
      action: () => navigate('/optical'),
    }));

    return [...navItems, ...patientItems, ...rxItems, ...orderItems];
  }, [navigate, prescriptions, orders]);

  const filtered = useMemo(() => {
    if (!query.trim()) return allItems.slice(0, 12);
    const q = query.toLowerCase();
    return allItems.filter(i =>
      i.label.toLowerCase().includes(q) ||
      i.category.toLowerCase().includes(q) ||
      i.hint?.toLowerCase().includes(q)
    ).slice(0, 20);
  }, [query, allItems]);

  useEffect(() => {
    if (!open) return;
    const handler = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
      if (e.key === 'ArrowDown') {
        e.preventDefault();
        setActiveIdx(i => Math.min(i + 1, filtered.length - 1));
      }
      if (e.key === 'ArrowUp') {
        e.preventDefault();
        setActiveIdx(i => Math.max(i - 1, 0));
      }
      if (e.key === 'Enter') {
        e.preventDefault();
        const item = filtered[activeIdx];
        if (item) {
          item.action();
          onClose();
        }
      }
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [open, filtered, activeIdx, onClose]);

  if (!open) return null;

  // Group by category
  const grouped: Record<string, Item[]> = {};
  filtered.forEach(item => {
    if (!grouped[item.category]) grouped[item.category] = [];
    grouped[item.category].push(item);
  });

  let runningIdx = -1;

  return (
    <div className="fixed inset-0 z-[90] bg-black/40 backdrop-blur-sm flex items-start justify-center pt-[12vh] px-4" onClick={onClose}>
      <div className="bg-card rounded-2xl shadow-2xl w-full max-w-[600px] overflow-hidden" onClick={e => e.stopPropagation()}>
        <div className="flex items-center gap-3 px-4 border-b border-border">
          <Search className="w-4 h-4 text-muted-foreground" />
          <input
            autoFocus
            value={query}
            onChange={e => { setQuery(e.target.value); setActiveIdx(0); }}
            placeholder="Caută pacient, rețetă, comandă, pagină..."
            className="flex-1 py-3.5 bg-transparent text-clinical-sm focus:outline-none"
          />
          <kbd className="text-[10px] text-muted-foreground border border-border rounded px-1.5 py-0.5 font-clinical">ESC</kbd>
        </div>

        <div className="max-h-[400px] overflow-y-auto py-2">
          {filtered.length === 0 && (
            <p className="text-center text-clinical-sm text-muted-foreground py-8">Niciun rezultat pentru "{query}"</p>
          )}
          {Object.entries(grouped).map(([cat, items]) => (
            <div key={cat} className="mb-2">
              <p className="px-4 py-1 text-[10px] uppercase tracking-wider text-muted-foreground font-semibold">{cat}</p>
              {items.map(item => {
                runningIdx++;
                const isActive = runningIdx === activeIdx;
                return (
                  <button
                    key={item.id}
                    onMouseEnter={() => setActiveIdx(filtered.indexOf(item))}
                    onClick={() => { item.action(); onClose(); }}
                    className={`w-full flex items-center gap-3 px-4 py-2.5 text-left transition-colors ${isActive ? 'bg-primary/10' : 'hover:bg-muted'}`}
                  >
                    <item.icon className={`w-4 h-4 shrink-0 ${isActive ? 'text-primary' : 'text-muted-foreground'}`} />
                    <div className="flex-1 min-w-0">
                      <p className={`text-clinical-sm truncate ${isActive ? 'font-semibold text-primary' : 'text-foreground'}`}>{item.label}</p>
                      {item.hint && <p className="text-[11px] text-muted-foreground truncate">{item.hint}</p>}
                    </div>
                  </button>
                );
              })}
            </div>
          ))}
        </div>

        <div className="border-t border-border px-4 py-2 flex items-center gap-3 text-[10px] text-muted-foreground">
          <span><kbd className="border border-border rounded px-1 font-clinical">↑↓</kbd> navigare</span>
          <span><kbd className="border border-border rounded px-1 font-clinical">↵</kbd> selectează</span>
          <span className="ml-auto">{filtered.length} rezultate</span>
        </div>
      </div>
    </div>
  );
};

export default CommandPalette;
