import React, { useState } from 'react';
import AppLayout from '@/components/AppLayout';
import { auditLog } from '@/data/demo-data';
import { Search, Download, Shield } from 'lucide-react';

const actionColors: Record<string, string> = {
  Vizualizare: 'bg-blue-100 text-blue-700',
  Creare: 'bg-green-100 text-green-700',
  Editare: 'bg-amber-100 text-amber-700',
  'Semnătură digitală': 'bg-purple-100 text-purple-700',
  Export: 'bg-teal-100 text-teal-700',
};

const AuditLogPage: React.FC = () => {
  const [search, setSearch] = useState('');
  const [actionFilter, setActionFilter] = useState('all');
  const actions = [...new Set(auditLog.map(a => a.action))];

  const filtered = auditLog.filter(a => {
    if (actionFilter !== 'all' && a.action !== actionFilter) return false;
    if (search && !a.user.toLowerCase().includes(search.toLowerCase()) && !a.entity.toLowerCase().includes(search.toLowerCase())) return false;
    return true;
  });

  return (
    <AppLayout breadcrumbs={[{ label: 'Jurnal Audit' }]}>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-clinical-xl font-bold flex items-center gap-2"><Shield className="w-6 h-6 text-primary"/>Jurnal Audit</h1>
        <div className="flex gap-2">
          <button className="px-3 py-1.5 rounded-lg border border-border text-clinical-xs font-medium flex items-center gap-1 hover:bg-muted"><Download className="w-3 h-3"/>Export CSV</button>
          <button className="px-3 py-1.5 rounded-lg border border-border text-clinical-xs font-medium flex items-center gap-1 hover:bg-muted"><Download className="w-3 h-3"/>Export PDF</button>
        </div>
      </div>

      <div className="flex gap-3 mb-4">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground"/>
          <input value={search} onChange={e=>setSearch(e.target.value)} placeholder="Caută după utilizator sau entitate..."
            className="w-full pl-10 pr-3 py-2 rounded-lg border border-border bg-card text-clinical-sm"/>
        </div>
        <select value={actionFilter} onChange={e => setActionFilter(e.target.value)}
          className="rounded-lg border border-border px-3 py-2 text-clinical-sm bg-card">
          <option value="all">Toate acțiunile</option>
          {actions.map(a => <option key={a} value={a}>{a}</option>)}
        </select>
      </div>

      <div className="bg-card rounded-xl border border-border shadow-sm overflow-hidden">
        <table className="w-full text-clinical-sm">
          <thead>
            <tr className="border-b border-border bg-muted/30">
              <th className="text-left p-3 text-clinical-xs text-muted-foreground font-semibold">Data/Ora</th>
              <th className="text-left p-3 text-clinical-xs text-muted-foreground font-semibold">Utilizator</th>
              <th className="text-left p-3 text-clinical-xs text-muted-foreground font-semibold">Rol</th>
              <th className="text-left p-3 text-clinical-xs text-muted-foreground font-semibold">Acțiune</th>
              <th className="text-left p-3 text-clinical-xs text-muted-foreground font-semibold">Modul</th>
              <th className="text-left p-3 text-clinical-xs text-muted-foreground font-semibold">Entitate</th>
              <th className="text-left p-3 text-clinical-xs text-muted-foreground font-semibold">IP</th>
              <th className="text-left p-3 text-clinical-xs text-muted-foreground font-semibold">Detalii</th>
            </tr>
          </thead>
          <tbody>
            {filtered.map(entry => (
              <tr key={entry.id} className="border-b border-border hover:bg-muted/30">
                <td className="p-3 font-clinical text-clinical-xs text-muted-foreground whitespace-nowrap">{entry.timestamp}</td>
                <td className="p-3 font-semibold">{entry.user}</td>
                <td className="p-3 text-clinical-xs">{entry.role}</td>
                <td className="p-3"><span className={`px-2 py-0.5 rounded-full text-[10px] font-semibold ${actionColors[entry.action] || 'bg-muted text-foreground'}`}>{entry.action}</span></td>
                <td className="p-3 text-clinical-xs">{entry.module}</td>
                <td className="p-3 text-clinical-xs max-w-[200px] truncate">{entry.entity}</td>
                <td className="p-3 font-clinical text-clinical-xs text-muted-foreground">{entry.ip}</td>
                <td className="p-3 text-clinical-xs text-muted-foreground max-w-[200px] truncate">{entry.details}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <p className="text-clinical-xs text-muted-foreground mt-3">Retenție minimă: 5 ani · Imuabil — nu poate fi modificat sau șters</p>
    </AppLayout>
  );
};

export default AuditLogPage;
