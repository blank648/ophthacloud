import React, { useState } from 'react';
import AppLayout from '@/components/AppLayout';
import { useAuditLogs } from '@/hooks/useAdmin';
import { Search, Download, Shield, ChevronLeft, ChevronRight } from 'lucide-react';

const actionColors: Record<string, string> = {
  VIEW: 'bg-blue-100 text-blue-700 border-blue-200',
  CREATE: 'bg-green-100 text-green-700 border-green-200',
  UPDATE: 'bg-amber-100 text-amber-700 border-amber-200',
  DELETE: 'bg-red-100 text-red-700 border-red-200',
  SIGN: 'bg-purple-100 text-purple-700 border-purple-200',
  EXPORT: 'bg-teal-100 text-teal-700 border-teal-200',
  LOGIN: 'bg-indigo-100 text-indigo-700 border-indigo-200',
  STATUS_CHANGE: 'bg-zinc-100 text-zinc-700 border-zinc-200',
  PERMISSION_CHANGE: 'bg-pink-100 text-pink-700 border-pink-200',
  STAGE_CHANGE: 'bg-sky-100 text-sky-700 border-sky-200',
};

const actionLabels: Record<string, string> = {
  VIEW: 'Vizualizare',
  CREATE: 'Creare',
  UPDATE: 'Editare',
  DELETE: 'Ștergere',
  SIGN: 'Semnătură digitală',
  EXPORT: 'Export',
  LOGIN: 'Autentificare',
  STATUS_CHANGE: 'Modificare status',
  PERMISSION_CHANGE: 'Modificare permisiuni',
  STAGE_CHANGE: 'Schimbare etapă',
};

const fieldLabels: Record<string, string> = {
  email: 'Email',
  stage: 'Status comandă',
  status: 'Status',
  firstName: 'Prenume',
  lastName: 'Nume',
  phone: 'Telefon',
  role: 'Rol',
  isActive: 'Activ',
  specialization: 'Specializare',
  licenseNumber: 'Număr licență',
  lastLoginAt: 'Ultima autentificare',
  workingHours: 'Program de lucru',
  bookingSlotMinutes: 'Durată slot programare',
  bookingAdvanceDays: 'Zile avans rezervări',
  icd10Code: 'Cod ICD-10',
  recallIntervalMonths: 'Interval rechemare (luni)',
  itemName: 'Nume articol',
  currentStock: 'Stoc curent',
  minStock: 'Stoc minim',
  mrn: 'Număr fișă medicală (MRN)',
  diagnosis: 'Diagnostic',
  type: 'Tip',
  date: 'Dată',
  time: 'Oră',
  duration: 'Durată',
  notes: 'Note',
  signed: 'Semnat',
  validUntil: 'Validitate rețetă',
  pdDistance: 'Distanță pupilară',
  pdNear: 'Distanță aproape',
  lensType: 'Tip lentile',
  material: 'Material',
  totalPrice: 'Preț total',
  costPrice: 'Preț cost',
  retailPrice: 'Preț vânzare',
};

const translateValue = (val: any): string => {
  if (val === null || val === undefined) return 'lipsă';
  if (val === true) return 'Da';
  if (val === false) return 'Nu';
  if (typeof val === 'object') return JSON.stringify(val);
  return String(val);
};

const formatChangedFields = (changedFields: any): string => {
  if (!changedFields || typeof changedFields !== 'object') return '-';
  
  const entries = Object.entries(changedFields);
  if (entries.length === 0) return '-';
  
  return entries.map(([key, diff]: [string, any]) => {
    const label = fieldLabels[key] || key;
    if (diff && typeof diff === 'object' && ('old' in diff || 'new' in diff)) {
      const oldVal = translateValue(diff.old);
      const newVal = translateValue(diff.new);
      if (diff.old !== undefined && diff.old !== null && diff.new !== undefined && diff.new !== null) {
        return `${label}: de la "${oldVal}" la "${newVal}"`;
      } else if (diff.new !== undefined && diff.new !== null) {
        return `${label} setat la "${newVal}"`;
      } else {
        return `${label} a fost șters (era "${oldVal}")`;
      }
    }
    return `${label}: ${translateValue(diff)}`;
  }).join('; ');
};

const AuditLogPage: React.FC = () => {
  const [search, setSearch] = useState('');
  const [actionFilter, setActionFilter] = useState('all');
  const [pageIndex, setPageIndex] = useState(0);
  const pageSize = 50;

  const { data: auditData, isLoading } = useAuditLogs({
    page: pageIndex,
    size: pageSize,
    action: actionFilter === 'all' ? undefined : actionFilter,
  });

  const content = auditData?.data || [];
  const totalPages = auditData?.pagination?.totalPages || 1;

  const filtered = content.filter(a => {
    const actor = a.actorName || a.actorId || '';
    const ent = a.entityType || '';
    if (search && !actor.toLowerCase().includes(search.toLowerCase()) && !ent.toLowerCase().includes(search.toLowerCase())) return false;
    return true;
  });

  const handleExportCSV = () => {
    const headers = ['Data/Ora', 'Utilizator', 'Rol', 'Acțiune', 'Modul/Entitate', 'IP', 'Detalii'];
    const rows = content.map(entry => {
      const formattedDate = new Date(entry.occurredAt).toLocaleString('ro-RO', {
        day: '2-digit', month: '2-digit', year: 'numeric',
        hour: '2-digit', minute: '2-digit', second: '2-digit'
      });
      const actionLabel = actionLabels[entry.action] || entry.action;
      const details = formatChangedFields(entry.changedFields);
      return [
        formattedDate,
        entry.actorName || entry.actorId,
        entry.actorRole || '',
        actionLabel,
        entry.entityType,
        entry.ipAddress || '',
        details
      ];
    });

    const csvContent = "\uFEFF" + [headers.join(','), ...rows.map(e => e.map(val => `"${String(val).replace(/"/g, '""')}"`).join(','))].join('\n');
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.setAttribute('href', url);
    link.setAttribute('download', `jurnal_audit_${new Date().toISOString().slice(0,10)}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const handleExportPDF = () => {
    const style = document.createElement('style');
    style.innerHTML = `
      @media print {
        body * {
          visibility: hidden;
        }
        #print-audit-area, #print-audit-area * {
          visibility: visible;
        }
        #print-audit-area {
          position: absolute;
          left: 0;
          top: 0;
          width: 100%;
        }
        .no-print {
          display: none !important;
        }
      }
    `;
    document.head.appendChild(style);
    window.print();
    document.head.removeChild(style);
  };

  return (
    <AppLayout breadcrumbs={[{ label: 'Jurnal Audit' }]}>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-clinical-xl font-bold flex items-center gap-2"><Shield className="w-6 h-6 text-primary"/>Jurnal Audit</h1>
        <div className="flex gap-2">
          <button onClick={handleExportCSV} className="px-3 py-1.5 rounded-lg border border-border text-clinical-xs font-medium flex items-center gap-1 hover:bg-muted"><Download className="w-3 h-3"/>Export CSV</button>
          <button onClick={handleExportPDF} className="px-3 py-1.5 rounded-lg border border-border text-clinical-xs font-medium flex items-center gap-1 hover:bg-muted"><Download className="w-3 h-3"/>Export PDF</button>
        </div>
      </div>

      <div className="flex gap-3 mb-4">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground"/>
          <input value={search} onChange={e=>setSearch(e.target.value)} placeholder="Caută după utilizator sau entitate..."
            className="w-full pl-10 pr-3 py-2 rounded-lg border border-border bg-card text-clinical-sm"/>
        </div>
        <select value={actionFilter} onChange={e => { setActionFilter(e.target.value); setPageIndex(0); }}
          className="rounded-lg border border-border px-3 py-2 text-clinical-sm bg-card">
          <option value="all">Toate acțiunile</option>
          {Object.entries(actionLabels).map(([key, value]) => (
            <option key={key} value={key}>{value}</option>
          ))}
        </select>
      </div>

      <div id="print-audit-area" className="bg-card rounded-xl border border-border shadow-sm overflow-hidden">
        {isLoading ? (
          <div className="text-center py-8 text-clinical-sm text-muted-foreground">Se încarcă jurnalul de audit...</div>
        ) : filtered.length === 0 ? (
          <div className="text-center py-8 text-clinical-sm text-muted-foreground">Nu există înregistrări în jurnalul de audit.</div>
        ) : (
          <>
            <table className="w-full text-clinical-sm">
              <thead>
                <tr className="border-b border-border bg-muted/30">
                  <th className="text-left p-3 text-clinical-xs text-muted-foreground font-semibold">Data/Ora</th>
                  <th className="text-left p-3 text-clinical-xs text-muted-foreground font-semibold">Utilizator</th>
                  <th className="text-left p-3 text-clinical-xs text-muted-foreground font-semibold">Rol</th>
                  <th className="text-left p-3 text-clinical-xs text-muted-foreground font-semibold">Acțiune</th>
                  <th className="text-left p-3 text-clinical-xs text-muted-foreground font-semibold">Modul/Entitate</th>
                  <th className="text-left p-3 text-clinical-xs text-muted-foreground font-semibold">IP</th>
                  <th className="text-left p-3 text-clinical-xs text-muted-foreground font-semibold">Detalii</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map(entry => {
                  const formattedDate = new Date(entry.occurredAt).toLocaleString('ro-RO', {
                    day: '2-digit', month: '2-digit', year: 'numeric',
                    hour: '2-digit', minute: '2-digit'
                  });
                  return (
                    <tr key={entry.id} className="border-b border-border hover:bg-muted/30">
                      <td className="p-3 font-clinical text-clinical-xs text-muted-foreground whitespace-nowrap">{formattedDate}</td>
                      <td className="p-3 font-semibold">{entry.actorName || entry.actorId}</td>
                      <td className="p-3 text-clinical-xs">{entry.actorRole || 'System'}</td>
                      <td className="p-3">
                        <span className={`px-2 py-0.5 rounded-full text-[10px] font-semibold border ${actionColors[entry.action] || 'bg-muted text-foreground border-border'}`}>
                          {actionLabels[entry.action] || entry.action}
                        </span>
                      </td>
                      <td className="p-3 text-clinical-xs">{entry.entityType}</td>
                      <td className="p-3 font-clinical text-clinical-xs text-muted-foreground">{entry.ipAddress || '-'}</td>
                      <td className="p-3 text-clinical-xs text-muted-foreground max-w-[300px] truncate" title={entry.changedFields ? formatChangedFields(entry.changedFields) : undefined}>
                        {formatChangedFields(entry.changedFields)}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>

            {totalPages > 1 && (
              <div className="flex items-center justify-between p-4 border-t border-border no-print">
                <span className="text-clinical-xs text-muted-foreground">Pagina {pageIndex + 1} din {totalPages}</span>
                <div className="flex gap-2">
                  <button onClick={() => setPageIndex(p => Math.max(0, p - 1))} disabled={pageIndex === 0}
                    className="p-1.5 rounded border border-border disabled:opacity-50 hover:bg-muted"><ChevronLeft className="w-4 h-4"/></button>
                  <button onClick={() => setPageIndex(p => Math.min(totalPages - 1, p + 1))} disabled={pageIndex === totalPages - 1}
                    className="p-1.5 rounded border border-border disabled:opacity-50 hover:bg-muted"><ChevronRight className="w-4 h-4"/></button>
                </div>
              </div>
            )}
          </>
        )}
      </div>
      <p className="text-clinical-xs text-muted-foreground mt-3 no-print">Retenție minimă: 5 ani · Imuabil — nu poate fi modificat sau șters</p>
    </AppLayout>
  );
};

export default AuditLogPage;
