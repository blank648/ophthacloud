import React, { useState } from 'react';
import AppLayout from '@/components/AppLayout';
import { opticalOrders, OpticalOrder, patients } from '@/data/demo-data';
import { Package, X, Check, AlertTriangle, Clock, ChevronRight } from 'lucide-react';

const COLUMNS: { key: OpticalOrder['status']; label: string; color: string }[] = [
  { key: 'new', label: 'Nouă', color: 'hsl(var(--color-info))' },
  { key: 'lab', label: 'Lab în Lucru', color: 'hsl(var(--color-warning))' },
  { key: 'qc', label: 'Control Calitate', color: '#8B5CF6' },
  { key: 'fitting', label: 'Gata Montaj', color: 'hsl(var(--color-primary-500))' },
  { key: 'done', label: 'Finalizat', color: 'hsl(var(--color-success))' },
];

const QC_CHECKS = [
  'Verificare dioptrii cu lensometru',
  'Centru optic verificat',
  'Grosime lentilă corectă',
  'Tratamente aplicate corect',
  'Ajustare ramă (pad-uri, brațe)',
  'Control final pe pacient',
];

const OpticalERPPage: React.FC = () => {
  const [orders, setOrders] = useState(opticalOrders);
  const [selected, setSelected] = useState<OpticalOrder | null>(null);
  const [qcChecks, setQcChecks] = useState<Record<string, boolean[]>>({});
  const [dragItem, setDragItem] = useState<string | null>(null);

  const handleDragStart = (orderId: string) => setDragItem(orderId);
  const handleDrop = (targetStatus: OpticalOrder['status']) => {
    if (!dragItem) return;
    setOrders(prev => prev.map(o => o.id === dragItem ? { ...o, status: targetStatus } : o));
    setDragItem(null);
  };

  const ordersByStatus = (status: OpticalOrder['status']) => orders.filter(o => o.status === status);

  const toggleQC = (orderId: string, idx: number) => {
    setQcChecks(prev => {
      const checks = [...(prev[orderId] || Array(6).fill(false))];
      checks[idx] = !checks[idx];
      return { ...prev, [orderId]: checks };
    });
  };

  return (
    <AppLayout breadcrumbs={[{ label: 'ERP Optic' }]}>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-clinical-xl font-bold">ERP Optic — Comenzi</h1>
        <div className="flex items-center gap-3 text-clinical-sm">
          <span className="text-muted-foreground">Total: <strong>{orders.length}</strong></span>
          <span className="text-red-600 font-semibold">{orders.filter(o => o.slaStatus === 'overdue').length} întârziate</span>
        </div>
      </div>

      {/* Kanban */}
      <div className="flex gap-4 overflow-x-auto pb-4">
        {COLUMNS.map(col => (
          <div key={col.key}
            className="flex-1 min-w-[220px]"
            onDragOver={e => e.preventDefault()}
            onDrop={() => handleDrop(col.key)}>
            <div className="flex items-center gap-2 mb-3 px-1">
              <div className="w-3 h-3 rounded-full" style={{ background: col.color }} />
              <h3 className="text-clinical-sm font-semibold">{col.label}</h3>
              <span className="text-clinical-xs text-muted-foreground bg-muted rounded-full px-2 py-0.5">{ordersByStatus(col.key).length}</span>
            </div>
            <div className="space-y-3 min-h-[200px] bg-muted/20 rounded-xl p-2">
              {ordersByStatus(col.key).map(order => (
                <div key={order.id}
                  draggable
                  onDragStart={() => handleDragStart(order.id)}
                  onClick={() => setSelected(order)}
                  className={`bg-card rounded-xl border shadow-sm p-4 cursor-grab active:cursor-grabbing hover:shadow-md transition-shadow ${
                    order.slaStatus === 'overdue' ? 'border-red-300 bg-red-50/50' : 'border-border'
                  }`}>
                  <div className="flex items-center justify-between mb-2">
                    <span className="font-clinical text-clinical-xs text-primary">{order.id}</span>
                    {order.slaStatus === 'overdue' && <AlertTriangle className="w-4 h-4 text-red-500" />}
                    {order.priority === 'urgent' && <span className="px-1.5 py-0.5 rounded text-[9px] font-bold bg-red-100 text-red-700">URGENT</span>}
                  </div>
                  <p className="text-clinical-sm font-semibold mb-1">{order.patientName}</p>
                  <div className="flex flex-wrap gap-1 mb-2">
                    <span className="px-1.5 py-0.5 rounded text-[9px] bg-muted text-muted-foreground">{order.frameType}</span>
                    <span className="px-1.5 py-0.5 rounded text-[9px] bg-primary/10 text-primary">{order.lensType}</span>
                  </div>
                  <div className="flex items-center justify-between text-clinical-xs text-muted-foreground">
                    <span>{order.createdDate}</span>
                    {order.totalPrice && <span className="font-clinical font-semibold">{order.totalPrice} RON</span>}
                  </div>
                  {order.labName && <p className="text-[10px] text-muted-foreground mt-1">Lab: {order.labName}</p>}
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>

      {/* Detail slide-in */}
      {selected && (
        <div className="fixed inset-0 z-50 flex justify-end" onClick={() => setSelected(null)}>
          <div className="absolute inset-0 bg-black/30" />
          <div className="relative w-[620px] bg-card h-full shadow-xl overflow-y-auto" onClick={e => e.stopPropagation()}>
            <div className="p-6">
              <div className="flex items-center justify-between mb-6">
                <div>
                  <h2 className="text-clinical-lg font-bold">{selected.id}</h2>
                  <p className="text-clinical-sm text-muted-foreground">{selected.patientName} · {selected.createdDate}</p>
                </div>
                <button onClick={() => setSelected(null)}><X className="w-5 h-5" /></button>
              </div>

              {/* Prescription summary */}
              {(() => {
                const patient = patients.find(p => p.id === selected.patientId);
                const rx = patient?.prescriptions?.find(p => p.id === selected.prescriptionId);
                if (!rx) return null;
                return (
                  <div className="mb-6">
                    <h4 className="text-clinical-xs font-semibold text-muted-foreground clinical-label mb-2">Rețetă: {rx.id}</h4>
                    <div className="grid grid-cols-2 gap-3">
                      <div className="p-3 rounded-lg border-2 border-red-200 bg-red-50/30">
                        <p className="text-[10px] font-semibold text-red-600 clinical-label mb-1">OD</p>
                        <p className="font-clinical text-clinical-xs">Sph: {rx.od.sph.toFixed(2)} / Cyl: {rx.od.cyl.toFixed(2)} / Ax: {rx.od.axis}°</p>
                      </div>
                      <div className="p-3 rounded-lg border-2 border-blue-200 bg-blue-50/30">
                        <p className="text-[10px] font-semibold text-blue-600 clinical-label mb-1">OS</p>
                        <p className="font-clinical text-clinical-xs">Sph: {rx.os.sph.toFixed(2)} / Cyl: {rx.os.cyl.toFixed(2)} / Ax: {rx.os.axis}°</p>
                      </div>
                    </div>
                  </div>
                );
              })()}

              {/* Specs */}
              <div className="mb-6 space-y-2">
                <h4 className="text-clinical-xs font-semibold text-muted-foreground clinical-label">Specificații</h4>
                <div className="grid grid-cols-2 gap-2 text-clinical-sm">
                  <div className="bg-muted/30 rounded-lg p-2"><span className="text-muted-foreground text-clinical-xs">Ramă:</span><p className="font-semibold">{selected.frameType}</p></div>
                  <div className="bg-muted/30 rounded-lg p-2"><span className="text-muted-foreground text-clinical-xs">Lentile:</span><p className="font-semibold">{selected.lensType}</p></div>
                  <div className="bg-muted/30 rounded-lg p-2"><span className="text-muted-foreground text-clinical-xs">Laborator:</span><p className="font-semibold">{selected.labName || '—'}</p></div>
                  <div className="bg-muted/30 rounded-lg p-2"><span className="text-muted-foreground text-clinical-xs">Total:</span><p className="font-clinical font-semibold">{selected.totalPrice} RON</p></div>
                </div>
              </div>

              {/* QC Checklist */}
              <div className="mb-6">
                <h4 className="text-clinical-xs font-semibold text-muted-foreground clinical-label mb-2">Control Calitate</h4>
                <div className="space-y-2">
                  {QC_CHECKS.map((check, i) => {
                    const checked = qcChecks[selected.id]?.[i] || false;
                    return (
                      <label key={i} className={`flex items-center gap-3 p-2.5 rounded-lg border cursor-pointer transition-colors ${checked ? 'bg-green-50 border-green-200' : 'border-border hover:bg-muted/30'}`}>
                        <input type="checkbox" checked={checked} onChange={() => toggleQC(selected.id, i)} className="rounded" />
                        <span className={`text-clinical-sm ${checked ? 'text-green-700 line-through' : ''}`}>{check}</span>
                        {checked && <Check className="w-4 h-4 text-green-600 ml-auto" />}
                      </label>
                    );
                  })}
                </div>
              </div>

              {/* Lab thread */}
              <div className="mb-6">
                <h4 className="text-clinical-xs font-semibold text-muted-foreground clinical-label mb-2">Comunicare Laborator</h4>
                <div className="space-y-2 text-clinical-xs">
                  <div className="bg-muted/30 rounded-lg p-3"><span className="text-muted-foreground">20.03.2026 09:15</span><p>Comandă transmisă către {selected.labName}. Referință: {selected.id}</p></div>
                  <div className="bg-muted/30 rounded-lg p-3"><span className="text-muted-foreground">21.03.2026 14:30</span><p>Confirmare primire comandă. Estimare livrare: 5 zile lucrătoare.</p></div>
                  <div className="bg-primary/5 rounded-lg p-3 border border-primary/20"><span className="text-muted-foreground">25.03.2026 10:00</span><p className="text-primary font-semibold">Producție finalizată. Expediere azi.</p></div>
                </div>
              </div>

              {/* Actions */}
              <div className="flex gap-2">
                {selected.status === 'done' ? (
                  <button className="flex-1 py-2.5 rounded-lg bg-primary text-white text-clinical-sm font-semibold">Emite Factură</button>
                ) : (
                  <button onClick={() => {
                    const nextStatus: Record<string, OpticalOrder['status']> = { new: 'lab', lab: 'qc', qc: 'fitting', fitting: 'done' };
                    const next = nextStatus[selected.status];
                    if (next) {
                      setOrders(prev => prev.map(o => o.id === selected.id ? { ...o, status: next } : o));
                      setSelected({ ...selected, status: next });
                    }
                  }} className="flex-1 py-2.5 rounded-lg bg-primary text-white text-clinical-sm font-semibold flex items-center justify-center gap-1">
                    Avansează Status <ChevronRight className="w-4 h-4" />
                  </button>
                )}
              </div>
            </div>
          </div>
        </div>
      )}
    </AppLayout>
  );
};

export default OpticalERPPage;
