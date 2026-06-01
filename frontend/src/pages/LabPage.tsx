import React, { useState } from 'react';
import { toast } from 'sonner';
import AppLayout from '@/components/AppLayout';
import { usePatients } from '@/hooks/usePatients';
import { usePrescription } from '@/hooks/usePrescriptions';
import {
  useOrders,
  useUpdateOrderStage,
  useSubmitQc,
} from '@/hooks/useOptical';
import type { OrderStage, QcResultDto, OpticalOrderDto } from '@/types/optical';
import { X, Check, FlaskConical, ChevronRight, RefreshCw, Barcode, MapPin, AlertTriangle } from 'lucide-react';

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

const LabPage: React.FC = () => {
  const { data: orders = [], isLoading: ordersLoading } = useOrders();
  const { data: patientsPaged } = usePatients({ page: 0, size: 5000 });
  const patients = patientsPaged?.data || [];

  const updateStageMutation = useUpdateOrderStage();
  const submitQcMutation = useSubmitQc();

  const [selected, setSelected] = useState<OpticalOrderDto | null>(null);
  const [isAdvancing, setIsAdvancing] = useState(false);

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

  const ordersInLab = orders.filter(o => o.stage === 'SENT_TO_LAB' || o.stage === 'QC_CHECK');
  const countInLab = orders.filter(o => o.stage === 'SENT_TO_LAB').length;
  const countInQc = orders.filter(o => o.stage === 'QC_CHECK').length;

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
    
    setIsAdvancing(true);
    try {
      if (selected.stage === 'SENT_TO_LAB') {
        const res = await updateStageMutation.mutateAsync({
          id: selected.id,
          data: { newStage: 'QC_CHECK' }
        });
        setSelected(res);
        toast.success('Montaj finalizat', { description: `Comanda ${res.orderNumber} trimisă la Control Calitate.` });
      } else if (selected.stage === 'QC_CHECK') {
        const allPassed = Object.values(localQc).every(v => v === true);
        if (!allPassed) {
          toast.error('Controlul de calitate trebuie finalizat și toate criteriile bifate înainte de a avansa!');
          setIsAdvancing(false);
          return;
        }

        // 1. Submit QC results first to the backend
        toast.loading('Se salvează verificările QC...', { id: 'qc-advance' });
        await submitQcMutation.mutateAsync({
          id: selected.id,
          data: localQc
        });

        // 2. Advance the stage
        toast.loading('Se validează QC și se actualizează statusul...', { id: 'qc-advance' });
        const res = await updateStageMutation.mutateAsync({
          id: selected.id,
          data: { newStage: 'READY_FOR_FITTING' }
        });

        toast.success('Control Calitate Promovat!', { 
          id: 'qc-advance',
          description: `Comanda ${res.orderNumber} este gata de montaj pe pacient.` 
        });
        setSelected(null);
      }
    } catch (err: any) {
      const message = err?.response?.data?.message || err?.message || 'Eroare la actualizarea statusului';
      toast.error(message, { id: 'qc-advance' });
    } finally {
      setIsAdvancing(false);
    }
  };

  return (
    <AppLayout breadcrumbs={[{ label: 'Laborator' }]}>
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-2">
          <FlaskConical className="w-7 h-7 text-primary" />
          <h1 className="text-clinical-xl font-bold">Laborator Optic — Atelier</h1>
        </div>
        <div className="flex items-center gap-3 text-clinical-sm">
          {ordersLoading && <RefreshCw className="w-4 h-4 animate-spin text-muted-foreground" />}
          <span className="text-muted-foreground">În lucru la laborator: <strong>{countInLab}</strong></span>
          <span className="text-[#8B5CF6] font-semibold">În control calitate: <strong>{countInQc}</strong></span>
        </div>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        <div className="bg-card rounded-xl border border-border shadow-sm p-4">
          <p className="text-clinical-xs text-muted-foreground">În lucru la laborator (Grinding & Mounting)</p>
          <p className="text-clinical-lg font-bold font-clinical text-amber-500">{countInLab}</p>
        </div>
        <div className="bg-card rounded-xl border border-border shadow-sm p-4">
          <p className="text-clinical-xs text-muted-foreground">În Control Calitate (QC Verification)</p>
          <p className="text-clinical-lg font-bold font-clinical text-[#8B5CF6]">{countInQc}</p>
        </div>
        <div className="bg-card rounded-xl border border-border shadow-sm p-4">
          <p className="text-clinical-xs text-muted-foreground">Total Comenzi active atelier</p>
          <p className="text-clinical-lg font-bold font-clinical text-primary">{ordersInLab.length}</p>
        </div>
      </div>

      {/* Queue Table */}
      {ordersLoading ? (
        <div className="flex items-center justify-center py-20 text-muted-foreground text-clinical-sm">
          <RefreshCw className="w-5 h-5 animate-spin mr-2" /> Se încarcă coada de comenzi a laboratorului...
        </div>
      ) : (
        <div className="bg-card rounded-xl border border-border shadow-sm overflow-hidden">
          <table className="w-full text-clinical-sm">
            <thead>
              <tr className="border-b border-border bg-muted/30">
                <th className="text-left p-3 text-clinical-xs text-muted-foreground font-semibold">Comandă</th>
                <th className="text-left p-3 text-clinical-xs text-muted-foreground font-semibold">Pacient</th>
                <th className="text-left p-3 text-clinical-xs text-muted-foreground font-semibold">Data trimiterii</th>
                <th className="text-left p-3 text-clinical-xs text-muted-foreground font-semibold">Tip Lentile</th>
                <th className="text-left p-3 text-clinical-xs text-muted-foreground font-semibold">Laborator / Ref</th>
                <th className="text-center p-3 text-clinical-xs text-muted-foreground font-semibold">Status</th>
                <th className="text-right p-3 text-clinical-xs text-muted-foreground font-semibold">Acțiuni</th>
              </tr>
            </thead>
            <tbody>
              {ordersInLab.map(order => {
                const isUrgent = order.notes?.toLowerCase().includes('urgent');
                return (
                  <tr
                    key={order.id}
                    onClick={() => setSelected(order)}
                    className="border-b border-border hover:bg-muted/30 cursor-pointer transition-colors"
                  >
                    <td className="p-3">
                      <span className="font-clinical text-clinical-xs text-primary bg-primary/5 px-2 py-0.5 rounded">
                        {order.orderNumber}
                      </span>
                    </td>
                    <td className="p-3">
                      <div className="font-semibold flex items-center gap-1.5">
                        {getPatientName(order.patientId)}
                        {isUrgent && <span className="px-1.5 py-0.5 rounded text-[8px] font-bold bg-red-100 dark:bg-red-950 text-red-700 dark:text-red-300">URGENT</span>}
                      </div>
                    </td>
                    <td className="p-3 text-muted-foreground">
                      {order.sentToLabAt ? new Date(order.sentToLabAt).toLocaleDateString('ro-RO') : '—'}
                    </td>
                    <td className="p-3 truncate max-w-[180px]">
                      {order.items.map(item => item.productName).join(', ') || 'Lentile Rx'}
                    </td>
                    <td className="p-3 text-clinical-xs text-muted-foreground italic">
                      {order.labName || 'Atelier Intern'}{order.labReference ? ` · Ref: ${order.labReference}` : ''}
                    </td>
                    <td className="p-3 text-center">
                      <span className={`px-2.5 py-0.5 rounded-full text-[10px] font-semibold border ${
                        order.stage === 'SENT_TO_LAB'
                          ? 'bg-amber-50/50 dark:bg-amber-950/20 text-amber-600 dark:text-amber-400 border-amber-200 dark:border-amber-900'
                          : 'bg-purple-50/50 dark:bg-purple-950/20 text-[#8B5CF6] dark:text-[#a78bfa] border-purple-200 dark:border-purple-900'
                      }`}>
                        {order.stage === 'SENT_TO_LAB' ? 'În Lucru' : 'Control Calitate'}
                      </span>
                    </td>
                    <td className="p-3 text-right">
                      <button className="text-primary hover:underline text-clinical-xs font-semibold flex items-center gap-1 ml-auto">
                        Deschide Fișă <ChevronRight className="w-3.5 h-3.5" />
                      </button>
                    </td>
                  </tr>
                );
              })}
              {ordersInLab.length === 0 && (
                <tr>
                  <td colSpan={7} className="p-16 text-center text-muted-foreground">
                    <FlaskConical className="w-10 h-10 mx-auto mb-2 text-muted-foreground/30 animate-pulse" />
                    Nicio comandă în lucru sau în control de calitate momentan.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}

      {/* Technical Worksheet Slide-over Drawer */}
      {selected && (
        <div className="fixed inset-0 z-50 flex justify-end animate-in fade-in duration-200" onClick={() => setSelected(null)}>
          <div className="absolute inset-0 bg-black/40 backdrop-blur-sm" />
          <div className="relative w-full max-w-[580px] bg-card h-full shadow-2xl overflow-y-auto border-l border-border flex flex-col animate-in slide-in-from-right duration-300" onClick={e => e.stopPropagation()}>
            <div className="p-6 border-b border-border flex items-center justify-between bg-muted/20">
              <div>
                <span className="text-[10px] uppercase font-bold text-muted-foreground tracking-widest block mb-0.5">Fișă Tehnică de Atelier</span>
                <h2 className="text-clinical-lg font-bold">{selected.orderNumber}</h2>
              </div>
              <button onClick={() => setSelected(null)} className="p-2 rounded-lg hover:bg-muted transition-colors">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-6 space-y-6 flex-1">
              {/* Patient Details */}
              <div className="bg-muted/10 rounded-xl p-4 border border-border/40">
                <span className="text-[10px] uppercase font-bold text-muted-foreground tracking-wider block mb-1">Pacient</span>
                <p className="text-clinical-sm font-semibold">{getPatientName(selected.patientId)}</p>
                <p className="text-clinical-xs text-muted-foreground mt-0.5">ID: {selected.patientId}</p>
              </div>

              {/* Prescription Clinical Details */}
              {prescription && (
                <div className="space-y-3">
                  <h4 className="text-clinical-xs font-semibold text-muted-foreground clinical-label">
                    Valori Prescripție Oftalmologică (Rx)
                  </h4>
                  <div className="grid grid-cols-2 gap-3">
                    <div className="p-4 rounded-xl border border-red-200 dark:border-red-950 bg-red-50/20 dark:bg-red-950/10">
                      <p className="text-clinical-xs font-bold text-red-600 dark:text-red-400 clinical-label mb-2">OD (Ochi Drept)</p>
                      {prescription.lines.find(l => l.eye === 'OD') ? (
                        (() => {
                          const l = prescription.lines.find(x => x.eye === 'OD')!;
                          return (
                            <div className="font-clinical text-clinical-sm space-y-1">
                              <p><span className="text-muted-foreground text-clinical-xs">Sph:</span> <strong>{l.sph ? (l.sph > 0 ? '+' : '') + l.sph.toFixed(2) + 'D' : '—'}</strong></p>
                              <p><span className="text-muted-foreground text-clinical-xs">Cyl:</span> <strong>{l.cyl ? l.cyl.toFixed(2) + 'D' : '—'}</strong></p>
                              <p><span className="text-muted-foreground text-clinical-xs">Axis:</span> <strong>{l.axis ? l.axis + '°' : '—'}</strong></p>
                              {l.addPower && <p><span className="text-muted-foreground text-clinical-xs">Add:</span> <strong>+{l.addPower.toFixed(2)}D</strong></p>}
                            </div>
                          );
                        })()
                      ) : (
                        <p className="text-clinical-xs text-muted-foreground">—</p>
                      )}
                    </div>
                    <div className="p-4 rounded-xl border border-blue-200 dark:border-blue-950 bg-blue-50/20 dark:bg-blue-950/10">
                      <p className="text-clinical-xs font-bold text-blue-600 dark:text-blue-400 clinical-label mb-2">OS (Ochi Stâng)</p>
                      {prescription.lines.find(l => l.eye === 'OS') ? (
                        (() => {
                          const l = prescription.lines.find(x => x.eye === 'OS')!;
                          return (
                            <div className="font-clinical text-clinical-sm space-y-1">
                              <p><span className="text-muted-foreground text-clinical-xs">Sph:</span> <strong>{l.sph ? (l.sph > 0 ? '+' : '') + l.sph.toFixed(2) + 'D' : '—'}</strong></p>
                              <p><span className="text-muted-foreground text-clinical-xs">Cyl:</span> <strong>{l.cyl ? l.cyl.toFixed(2) + 'D' : '—'}</strong></p>
                              <p><span className="text-muted-foreground text-clinical-xs">Axis:</span> <strong>{l.axis ? l.axis + '°' : '—'}</strong></p>
                              {l.addPower && <p><span className="text-muted-foreground text-clinical-xs">Add:</span> <strong>+{l.addPower.toFixed(2)}D</strong></p>}
                            </div>
                          );
                        })()
                      ) : (
                        <p className="text-clinical-xs text-muted-foreground">—</p>
                      )}
                    </div>
                  </div>
                  <div className="bg-muted/30 rounded-lg p-3 text-clinical-xs border border-border flex justify-between">
                    <div>
                      <span className="text-muted-foreground">DP Distanță:</span>{' '}
                      <span className="font-clinical font-semibold">{prescription.pdBinocular || `${prescription.pdOd} / ${prescription.pdOs}`} mm</span>
                    </div>
                    {prescription.lensType && (
                      <div>
                        <span className="text-muted-foreground">Tip lentile:</span>{' '}
                        <span className="font-semibold">{prescription.lensType}</span>
                      </div>
                    )}
                  </div>
                </div>
              )}

              {/* Technical Worksheet specs */}
              <div className="space-y-2">
                <h4 className="text-clinical-xs font-semibold text-muted-foreground clinical-label">Materiale de montat</h4>
                <div className="space-y-2">
                  {selected.items.map((item, idx) => (
                    <div key={idx} className="bg-muted/30 rounded-xl p-3 border border-border flex justify-between items-center">
                      <div>
                        <p className="text-clinical-sm font-semibold">{item.productName}</p>
                        <p className="text-[10px] text-muted-foreground font-clinical">Cod SKU: {item.productCode} · Qty: {item.quantity}</p>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              {/* QC Verification Panel */}
              {(selected.stage === 'QC_CHECK') && (
                <div className="space-y-3 pt-2 border-t border-border">
                  <div className="flex justify-between items-center">
                    <h4 className="text-clinical-xs font-semibold text-muted-foreground clinical-label">
                      Verificări Control Calitate (QC)
                    </h4>
                    <button onClick={handleSaveQc} className="text-clinical-xs font-bold text-primary hover:underline">
                      Salvează Verificări
                    </button>
                  </div>
                  <div className="grid grid-cols-1 gap-2">
                    {QC_KEYS.map(key => {
                      const checked = localQc[key] || false;
                      return (
                        <label key={key} className={`flex items-center gap-3 p-2.5 rounded-lg border transition-all cursor-pointer hover:bg-muted/30 ${
                          checked ? 'bg-green-50/20 dark:bg-green-950/10 border-green-200 dark:border-green-900 text-green-700 dark:text-green-300' : 'border-border'
                        }`}>
                          <input type="checkbox" checked={checked} onChange={() => toggleQC(key)} className="rounded text-primary focus:ring-primary" />
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

              {/* Order Notes */}
              {selected.notes && (
                <div className="bg-muted/10 rounded-lg p-3 text-clinical-xs border border-border/40">
                  <span className="font-semibold text-muted-foreground block mb-1">Indicații suplimentare medic / optician:</span>
                  <p className="italic text-muted-foreground">{selected.notes}</p>
                </div>
              )}
            </div>

            {/* Actions Panel Footer */}
            <div className="p-6 border-t border-border bg-card">
              {selected.stage === 'SENT_TO_LAB' ? (
                <button
                  onClick={handleAdvanceStatus}
                  disabled={isAdvancing}
                  className="w-full py-3 rounded-lg bg-primary text-primary-foreground text-clinical-sm font-semibold flex items-center justify-center gap-1.5 hover:bg-primary/95 transition-all shadow hover:shadow-md disabled:opacity-50"
                >
                  {isAdvancing ? (
                    <>
                      Se actualizează... <RefreshCw className="w-4.5 h-4.5 animate-spin" />
                    </>
                  ) : (
                    <>
                      Finalizează Montajul & Trimite la QC <ChevronRight className="w-4.5 h-4.5" />
                    </>
                  )}
                </button>
              ) : (
                <button
                  onClick={handleAdvanceStatus}
                  disabled={isAdvancing}
                  className="w-full py-3 rounded-lg bg-primary text-primary-foreground text-clinical-sm font-semibold flex items-center justify-center gap-1.5 hover:bg-primary/95 transition-all shadow hover:shadow-md disabled:opacity-50"
                >
                  {isAdvancing ? (
                    <>
                      Se validează... <RefreshCw className="w-4.5 h-4.5 animate-spin" />
                    </>
                  ) : (
                    <>
                      Validează QC & Pregătește pentru Montaj <ChevronRight className="w-4.5 h-4.5" />
                    </>
                  )}
                </button>
              )}
            </div>
          </div>
        </div>
      )}
    </AppLayout>
  );
};

export default LabPage;
