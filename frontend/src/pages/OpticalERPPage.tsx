import React, { useState } from 'react';
import { toast } from 'sonner';
import AppLayout from '@/components/AppLayout';
import { usePatients } from '@/hooks/usePatients';
import { usePrescription } from '@/hooks/usePrescriptions';
import {
  useOrders,
  useUpdateOrderStage,
  useSubmitQc,
  useCreateInvoice
} from '@/hooks/useOptical';
import type { OrderStage, QcResultDto, OpticalOrderDto } from '@/types/optical';
import { X, Check, AlertTriangle, ChevronRight, FileText, Ban, RefreshCw } from 'lucide-react';

const COLUMNS: { key: OrderStage; label: string; color: string }[] = [
  { key: 'RECEIVED', label: 'Nouă', color: 'hsl(var(--color-info))' },
  { key: 'SENT_TO_LAB', label: 'Lab în Lucru', color: 'hsl(var(--color-warning))' },
  { key: 'QC_CHECK', label: 'Control Calitate', color: '#8B5CF6' },
  { key: 'READY_FOR_FITTING', label: 'Gata Montaj', color: 'hsl(var(--color-primary-500))' },
  { key: 'COMPLETED', label: 'Finalizat', color: 'hsl(var(--color-success))' },
];

const QC_KEYS: (keyof QcResultDto)[] = [
  'valuesChecked',
  'opticalCenterOd',
  'opticalCenterOs',
  'pupillaryDistance',
  'segmentHeight',
  'treatmentsApplied',
  'assemblyQuality',
  'finalCleaning',
];

const QC_LABELS: Record<keyof QcResultDto, string> = {
  valuesChecked: 'Verificare dioptrii lensometru',
  opticalCenterOd: 'Centru optic ochi drept',
  opticalCenterOs: 'Centru optic ochi stâng',
  pupillaryDistance: 'Distanță pupilară',
  segmentHeight: 'Înălțime montaj / segment',
  treatmentsApplied: 'Tratamente aplicate',
  assemblyQuality: 'Ajustare și aliniere ramă',
  finalCleaning: 'Curățare finală și finisaj',
};

const OpticalERPPage: React.FC = () => {
  const { data: orders = [], isLoading: ordersLoading } = useOrders();
  const { data: patientsPaged } = usePatients({ page: 0, size: 5000 });
  const patients = patientsPaged?.data || [];

  const updateStageMutation = useUpdateOrderStage();
  const submitQcMutation = useSubmitQc();
  const createInvoiceMutation = useCreateInvoice();

  const [selected, setSelected] = useState<OpticalOrderDto | null>(null);
  const [isAdvancing, setIsAdvancing] = useState(false);
  const [dragItem, setDragItem] = useState<string | null>(null);

  const [localQc, setLocalQc] = useState<QcResultDto>({
    valuesChecked: false,
    opticalCenterOd: false,
    opticalCenterOs: false,
    pupillaryDistance: false,
    segmentHeight: false,
    treatmentsApplied: false,
    assemblyQuality: false,
    finalCleaning: false,
  });

  // Sync selected order's QC checklist values to local form
  React.useEffect(() => {
    if (selected) {
      const currentOrder = orders.find(o => o.id === selected.id);
      if (currentOrder?.qcResult) {
        setLocalQc(currentOrder.qcResult);
      } else {
        setLocalQc({
          valuesChecked: false,
          opticalCenterOd: false,
          opticalCenterOs: false,
          pupillaryDistance: false,
          segmentHeight: false,
          treatmentsApplied: false,
          assemblyQuality: false,
          finalCleaning: false,
        });
      }
    }
  }, [selected, orders]);

  // Dynamically fetch prescription details inside the slide-over
  const { data: prescription } = usePrescription(selected?.prescriptionId || undefined);

  const getPatientName = (patientId: string) => {
    const p = patients.find(pat => pat.id === patientId);
    return p ? `${p.lastName} ${p.firstName}` : 'Pacient';
  };

  const handleDragStart = (orderId: string) => setDragItem(orderId);

  const handleDrop = (targetStage: OrderStage) => {
    if (!dragItem) return;
    const order = orders.find(o => o.id === dragItem);
    if (order && order.stage !== targetStage) {
      updateStageMutation.mutate({
        id: dragItem,
        data: { newStage: targetStage }
      }, {
        onSuccess: () => {
          const colLabel = COLUMNS.find(c => c.key === targetStage)?.label || targetStage;
          toast.success('Statut actualizat', { description: `Comanda ${order.orderNumber || order.id.substring(0,8)} → ${colLabel}` });
        },
        onError: (err: any) => {
          const message = err?.response?.data?.message || err?.message || 'Eroare la actualizarea statusului';
          toast.error(message);
        }
      });
    }
    setDragItem(null);
  };

  const ordersByStatus = (stage: OrderStage) => orders.filter(o => o.stage === stage);

  const toggleQC = (key: keyof QcResultDto) => {
    setLocalQc(prev => ({
      ...prev,
      [key]: !prev[key]
    }));
  };

  const handleSaveQc = () => {
    if (!selected) return;
    submitQcMutation.mutate({
      id: selected.id,
      data: localQc
    }, {
      onSuccess: () => {
        toast.success('Control Calitate salvat cu succes');
      },
      onError: (err: any) => {
        const msg = err?.response?.data?.message || err?.message || 'Eroare la salvarea controlului de calitate';
        toast.error(msg);
      }
    });
  };

  const handleAdvanceStatus = async () => {
    if (!selected || isAdvancing) return;
    const nextStageMap: Record<OrderStage, OrderStage | null> = {
      RECEIVED: 'SENT_TO_LAB',
      SENT_TO_LAB: 'QC_CHECK',
      QC_CHECK: 'READY_FOR_FITTING',
      READY_FOR_FITTING: 'COMPLETED',
      COMPLETED: null,
      CANCELLED: null,
    };
    const next = nextStageMap[selected.stage];
    if (!next) return;

    setIsAdvancing(true);
    try {
      if (selected.stage === 'QC_CHECK') {
        const allPassed = Object.values(localQc).every(v => v === true);
        if (!allPassed) {
          toast.error('Controlul de calitate trebuie finalizat și toate criteriile bifate înainte de a avansa!');
          setIsAdvancing(false);
          return;
        }

        // 1. Submit QC results first to the backend
        toast.loading('Se salvează verificările QC...', { id: 'qc-advance-erp' });
        await submitQcMutation.mutateAsync({
          id: selected.id,
          data: localQc
        });
      }

      // 2. Advance the stage
      if (selected.stage === 'QC_CHECK') {
        toast.loading('Se actualizează statusul...', { id: 'qc-advance-erp' });
      }
      const res = await updateStageMutation.mutateAsync({
        id: selected.id,
        data: { newStage: next }
      });

      setSelected(res);
      const colLabel = COLUMNS.find(c => c.key === next)?.label || next;
      
      if (selected.stage === 'QC_CHECK') {
        toast.success('Control Calitate Promovat!', { 
          id: 'qc-advance-erp',
          description: `Comanda ${res.orderNumber} este gata de montaj pe pacient.` 
        });
      } else {
        toast.success('Statut actualizat', { description: `Comanda ${res.orderNumber} → ${colLabel}` });
      }
    } catch (err: any) {
      const message = err?.response?.data?.message || err?.message || 'Eroare la actualizarea statusului';
      if (selected.stage === 'QC_CHECK') {
        toast.error(message, { id: 'qc-advance-erp' });
      } else {
        toast.error(message);
      }
    } finally {
      setIsAdvancing(false);
    }
  };

  const handleCancelOrder = () => {
    if (!selected) return;
    const reason = window.prompt("Introdu motivul anulării comenzii:");
    if (reason === null) return;
    if (!reason.trim()) {
      toast.error("Motivul anulării este obligatoriu!");
      return;
    }
    updateStageMutation.mutate({
      id: selected.id,
      data: { newStage: 'CANCELLED', cancellationReason: reason }
    }, {
      onSuccess: () => {
        setSelected(null);
        toast.success('Comandă anulată cu succes');
      },
      onError: (err: any) => {
        const message = err?.response?.data?.message || err?.message || 'Eroare la anularea comenzii';
        toast.error(message);
      }
    });
  };

  const handleEmitInvoice = () => {
    if (!selected) return;
    createInvoiceMutation.mutate({
      opticalOrderId: selected.id,
      patientId: selected.patientId
    }, {
      onSuccess: (res) => {
        toast.success('Factură generată cu succes!', {
          description: `Factura ${res.invoiceNumber} în valoare de ${res.total} RON`
        });
      },
      onError: (err: any) => {
        const msg = err?.response?.data?.message || err?.message || 'Eroare la emiterea facturii';
        toast.error(msg);
      }
    });
  };

  return (
    <AppLayout breadcrumbs={[{ label: 'ERP Optic' }]}>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-clinical-xl font-bold">ERP Optic — Comenzi active</h1>
        <div className="flex items-center gap-3 text-clinical-sm">
          <span className="text-muted-foreground">Total: <strong>{orders.length}</strong></span>
          <span className="text-red-600 font-semibold">
            {orders.filter(o => o.expectedReadyAt && new Date(o.expectedReadyAt) < new Date() && o.stage !== 'COMPLETED' && o.stage !== 'CANCELLED').length} întârziate
          </span>
        </div>
      </div>

      {ordersLoading ? (
        <div className="flex items-center justify-center py-20 text-muted-foreground text-clinical-sm">
          <RefreshCw className="w-5 h-5 animate-spin mr-2" /> Se încarcă comenzile...
        </div>
      ) : (
        /* Kanban Board */
        <div className="flex gap-4 overflow-x-auto pb-6 snap-x snap-mandatory md:snap-none">
          {COLUMNS.map(col => (
            <div key={col.key}
              className="flex-1 min-w-[280px] snap-start"
              onDragOver={e => e.preventDefault()}
              onDrop={() => handleDrop(col.key)}>
              <div className="flex items-center gap-2 mb-3 px-1">
                <div className="w-3 h-3 rounded-full" style={{ background: col.color }} />
                <h3 className="text-clinical-sm font-semibold">{col.label}</h3>
                <span className="text-clinical-xs text-muted-foreground bg-muted rounded-full px-2 py-0.5">
                  {ordersByStatus(col.key).length}
                </span>
              </div>
              <div className="space-y-3 min-h-[500px] bg-muted/20 rounded-xl p-2 transition-colors border border-dashed border-transparent hover:border-border">
                {ordersByStatus(col.key).map(order => {
                  const isOverdue = order.expectedReadyAt && new Date(order.expectedReadyAt) < new Date() && order.stage !== 'COMPLETED' && order.stage !== 'CANCELLED';
                  const isUrgent = order.notes?.toLowerCase().includes('urgent');

                  return (
                    <div key={order.id}
                      draggable
                      onDragStart={() => handleDragStart(order.id)}
                      onClick={() => setSelected(order)}
                      className={`bg-card rounded-xl border shadow-sm p-4 cursor-grab active:cursor-grabbing hover:shadow-md transition-all hover:scale-[1.02] ${
                        isOverdue ? 'border-red-300 bg-red-50/50 dark:bg-red-950/20' : 'border-border'
                      }`}>
                      <div className="flex items-center justify-between mb-2">
                        <span className="font-clinical text-[10px] text-primary bg-primary/5 px-2 py-0.5 rounded">
                          {order.orderNumber || order.id.substring(0, 8)}
                        </span>
                        {isOverdue && <AlertTriangle className="w-4 h-4 text-red-500 animate-pulse" />}
                        {isUrgent && <span className="px-1.5 py-0.5 rounded text-[8px] font-bold bg-red-100 dark:bg-red-900 text-red-700 dark:text-red-200">URGENT</span>}
                      </div>
                      <p className="text-clinical-sm font-semibold mb-1">{getPatientName(order.patientId)}</p>
                      <div className="flex flex-wrap gap-1 mb-2 max-h-[40px] overflow-hidden">
                        {order.items.map((item, idx) => (
                          <span key={idx} className="px-1.5 py-0.5 rounded text-[8px] bg-muted text-muted-foreground whitespace-nowrap">
                            {item.productName}
                          </span>
                        ))}
                      </div>
                      <div className="flex items-center justify-between text-clinical-xs text-muted-foreground mt-3">
                        <span>{order.sentToLabAt ? new Date(order.sentToLabAt).toLocaleDateString('ro-RO') : 'Nouă'}</span>
                        <span className="font-clinical font-semibold text-foreground">{order.totalAmount.toFixed(2)} RON</span>
                      </div>
                      {order.labName && <p className="text-[10px] text-muted-foreground mt-1.5 italic">Lab: {order.labName}</p>}
                    </div>
                  );
                })}
                {ordersByStatus(col.key).length === 0 && (
                  <p className="text-center text-muted-foreground text-clinical-xs py-12 border border-dashed border-border/40 rounded-xl">
                    Trage o comandă aici
                  </p>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Detail Slide-over Drawer */}
      {selected && (
        <div className="fixed inset-0 z-50 flex justify-end animate-in fade-in duration-200" onClick={() => setSelected(null)}>
          <div className="absolute inset-0 bg-black/40 backdrop-blur-sm" />
          <div className="relative w-full max-w-[580px] bg-card h-full shadow-2xl overflow-y-auto border-l border-border flex flex-col animate-in slide-in-from-right duration-300" onClick={e => e.stopPropagation()}>
            <div className="p-6 border-b border-border flex items-center justify-between">
              <div>
                <h2 className="text-clinical-lg font-bold">{selected.orderNumber}</h2>
                <p className="text-clinical-xs text-muted-foreground font-clinical">ID: {selected.id}</p>
              </div>
              <button onClick={() => setSelected(null)} className="p-2 rounded-lg hover:bg-muted transition-colors">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-6 space-y-6 flex-1">
              {/* Patient Details */}
              <div className="bg-muted/20 rounded-xl p-4 border border-border">
                <h4 className="text-clinical-xs font-semibold text-muted-foreground clinical-label mb-2">Informații Pacient</h4>
                <p className="text-clinical-sm font-semibold">{getPatientName(selected.patientId)}</p>
                <p className="text-clinical-xs text-muted-foreground mt-1">ID Pacient: {selected.patientId}</p>
              </div>

              {/* Prescription Information */}
              {prescription && (
                <div>
                  <h4 className="text-clinical-xs font-semibold text-muted-foreground clinical-label mb-3">
                    Rețetă Asociată: {prescription.prescriptionNumber}
                  </h4>
                  <div className="grid grid-cols-2 gap-3">
                    <div className="p-3 rounded-xl border border-red-200 dark:border-red-950 bg-red-50/20 dark:bg-red-950/10">
                      <p className="text-[10px] font-semibold text-red-600 dark:text-red-400 clinical-label mb-1">OD (Ochi Drept)</p>
                      {prescription.lines.find(l => l.eye === 'OD') ? (
                        (() => {
                          const l = prescription.lines.find(x => x.eye === 'OD')!;
                          return (
                            <div className="font-clinical text-clinical-xs space-y-0.5">
                              <p>Sph: {l.sph ? (l.sph > 0 ? '+' : '') + l.sph.toFixed(2) + 'D' : '—'}</p>
                              <p>Cyl: {l.cyl ? l.cyl.toFixed(2) + 'D' : '—'}</p>
                              <p>Ax: {l.axis ? l.axis + '°' : '—'}</p>
                              {l.addPower && <p>Add: +{l.addPower.toFixed(2)}D</p>}
                            </div>
                          );
                        })()
                      ) : (
                        <p className="text-clinical-xs text-muted-foreground">—</p>
                      )}
                    </div>
                    <div className="p-3 rounded-xl border border-blue-200 dark:border-blue-950 bg-blue-50/20 dark:bg-blue-950/10">
                      <p className="text-[10px] font-semibold text-blue-600 dark:text-blue-400 clinical-label mb-1">OS (Ochi Stâng)</p>
                      {prescription.lines.find(l => l.eye === 'OS') ? (
                        (() => {
                          const l = prescription.lines.find(x => x.eye === 'OS')!;
                          return (
                            <div className="font-clinical text-clinical-xs space-y-0.5">
                              <p>Sph: {l.sph ? (l.sph > 0 ? '+' : '') + l.sph.toFixed(2) + 'D' : '—'}</p>
                              <p>Cyl: {l.cyl ? l.cyl.toFixed(2) + 'D' : '—'}</p>
                              <p>Ax: {l.axis ? l.axis + '°' : '—'}</p>
                              {l.addPower && <p>Add: +{l.addPower.toFixed(2)}D</p>}
                            </div>
                          );
                        })()
                      ) : (
                        <p className="text-clinical-xs text-muted-foreground">—</p>
                      )}
                    </div>
                  </div>
                  <div className="bg-muted/30 rounded-lg p-2.5 mt-2 text-clinical-xs">
                    <p>
                      <span className="text-muted-foreground">DP Distanță:</span>{' '}
                      <span className="font-clinical">{prescription.pdBinocular || `${prescription.pdOd} / ${prescription.pdOs}`} mm</span>
                    </p>
                  </div>
                </div>
              )}

              {/* Order Items Specification */}
              <div className="space-y-2">
                <h4 className="text-clinical-xs font-semibold text-muted-foreground clinical-label">Produse comandate</h4>
                <div className="space-y-2">
                  {selected.items.map((item, idx) => (
                    <div key={idx} className="flex justify-between items-start bg-muted/30 rounded-xl p-3 border border-border">
                      <div>
                        <p className="text-clinical-sm font-semibold">{item.productName}</p>
                        <p className="text-[10px] text-muted-foreground font-clinical">Cod: {item.productCode} · Qty: {item.quantity}</p>
                        {item.notes && <p className="text-clinical-xs italic text-muted-foreground mt-1">Obs: {item.notes}</p>}
                      </div>
                      <span className="font-clinical text-clinical-sm text-foreground">
                        {(item.unitPrice * item.quantity).toFixed(2)} RON
                      </span>
                    </div>
                  ))}
                </div>
                <div className="flex justify-between items-center pt-2 border-t border-border">
                  <span className="text-clinical-sm text-muted-foreground">Total Comandă:</span>
                  <span className="font-clinical text-clinical-md font-bold text-primary">
                    {selected.totalAmount.toFixed(2)} RON
                  </span>
                </div>
              </div>

              {/* Quality Control Checklist Panel */}
              {(selected.stage === 'QC_CHECK' || selected.stage === 'READY_FOR_FITTING' || selected.stage === 'COMPLETED') && (
                <div className="space-y-3">
                  <div className="flex justify-between items-center">
                    <h4 className="text-clinical-xs font-semibold text-muted-foreground clinical-label">
                      Verificări Control Calitate (QC)
                    </h4>
                    {selected.stage === 'QC_CHECK' && (
                      <button onClick={handleSaveQc} className="text-clinical-xs font-bold text-primary hover:underline">
                        Salvează Verificări
                      </button>
                    )}
                  </div>
                  <div className="grid grid-cols-1 gap-2">
                    {QC_KEYS.map(key => {
                      const checked = localQc[key] || false;
                      const disabled = selected.stage !== 'QC_CHECK';
                      return (
                        <label key={key} className={`flex items-center gap-3 p-2.5 rounded-lg border transition-all ${
                          checked ? 'bg-green-50/20 dark:bg-green-950/10 border-green-200 dark:border-green-900 text-green-700 dark:text-green-300' : 'border-border'
                        } ${disabled ? 'opacity-80 cursor-not-allowed' : 'cursor-pointer hover:bg-muted/30'}`}>
                          <input type="checkbox" checked={checked} disabled={disabled} onChange={() => toggleQC(key)} className="rounded text-primary focus:ring-primary" />
                          <span className={`text-clinical-sm ${checked ? 'font-medium' : ''}`}>
                            {QC_LABELS[key]}
                          </span>
                          {checked && <Check className="w-4.5 h-4.5 text-green-600 dark:text-green-400 ml-auto" />}
                        </label>
                      );
                    })}
                  </div>
                </div>
              )}

              {/* Order Notes / Metadata */}
              {selected.notes && (
                <div className="bg-muted/10 rounded-lg p-3 text-clinical-xs border border-border/40">
                  <span className="font-semibold text-muted-foreground block mb-1">Observații comandă:</span>
                  <p className="italic text-muted-foreground">{selected.notes}</p>
                </div>
              )}
            </div>

            {/* Actions Panel Footer */}
            <div className="p-6 border-t border-border bg-card flex flex-col gap-2">
              {selected.stage === 'COMPLETED' ? (
                <button onClick={handleEmitInvoice} className="w-full py-3 rounded-lg bg-primary text-primary-foreground text-clinical-sm font-semibold flex items-center justify-center gap-1.5 hover:bg-primary/95 transition-all shadow hover:shadow-md">
                  <FileText className="w-4.5 h-4.5" /> Emite Factură
                </button>
              ) : selected.stage === 'CANCELLED' ? (
                <div className="text-center p-3 rounded-xl bg-red-50 dark:bg-red-950/20 border border-red-200 dark:border-red-900 text-red-600 dark:text-red-400 text-clinical-xs">
                  Comandă Anulată
                  {selected.cancellationReason && (
                    <span className="block font-medium mt-1">Motiv: {selected.cancellationReason}</span>
                  )}
                </div>
              ) : (
                <div className="flex gap-2">
                  <button onClick={handleCancelOrder} className="px-4 py-3 rounded-lg border border-red-200 hover:bg-red-50 dark:hover:bg-red-950/20 text-red-600 dark:text-red-400 text-clinical-sm font-medium flex items-center justify-center gap-1">
                    <Ban className="w-4.5 h-4.5" /> Anulează
                  </button>
                  <button 
                    onClick={handleAdvanceStatus} 
                    disabled={isAdvancing}
                    className="flex-1 py-3 rounded-lg bg-primary text-primary-foreground text-clinical-sm font-semibold flex items-center justify-center gap-1 hover:bg-primary/95 transition-all shadow hover:shadow-md disabled:opacity-50"
                  >
                    {isAdvancing ? (
                      <>
                        Se actualizează... <RefreshCw className="w-4.5 h-4.5 animate-spin" />
                      </>
                    ) : (
                      <>
                        Avansează Status <ChevronRight className="w-4.5 h-4.5" />
                      </>
                    )}
                  </button>
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </AppLayout>
  );
};

export default OpticalERPPage;
