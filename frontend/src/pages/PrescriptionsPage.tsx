import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import AppLayout from '@/components/AppLayout';
import PrintPreviewModal from '@/components/PrintPreviewModal';
import { prescriptionStatusStyles, type OpticalOrder } from '@/data/demo-data';
import { useData } from '@/contexts/DataContext';
import { Download, ArrowRight, Search, X, RefreshCw } from 'lucide-react';
import { usePrescriptions } from '@/hooks/usePrescriptions';
import { usePatients } from '@/hooks/usePatients';
import { useCreateOrder } from '@/hooks/useOptical';
import type { PrescriptionDto, PrescriptionLineDto } from '@/types/prescriptions';

const formatRefraction = (line?: PrescriptionLineDto) => {
  if (!line) return '—';
  const sph = line.sph || 0;
  const cyl = line.cyl || 0;
  const axis = line.axis || 0;
  const add = line.addPower;
  return `${sph > 0 ? '+' : ''}${sph.toFixed(2)} / ${cyl.toFixed(2)} / ${axis}°${add ? ` Add +${add.toFixed(2)}` : ''}`;
};

const translateLensType = (type?: string) => {
  if (!type) return '—';
  const mapping: Record<string, string> = {
    'SINGLE_VISION': 'Monofocal',
    'BIFOCAL': 'Bifocal',
    'PROGRESSIVE': 'Progresiv',
    'OFFICE': 'Ocupațional',
    'CONTACT': 'Lentilă de contact'
  };
  return mapping[type] || type;
};

const translateStatus = (status?: string) => {
  if (!status) return '—';
  const mapping: Record<string, string> = {
    'ACTIVE': 'Activă',
    'EXPIRED': 'Expirată',
    'CANCELLED': 'Anulată',
    'SUPERSEDED': 'Înlocuită'
  };
  return mapping[status.toUpperCase()] || status;
};

const PrescriptionsPage: React.FC = () => {
  const navigate = useNavigate();
  const { mutateAsync: createOrder } = useCreateOrder();
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('all');
  const [selectedRx, setSelectedRx] = useState<PrescriptionDto | null>(null);
  const [printRx, setPrintRx] = useState<PrescriptionDto | null>(null);
  const [patientId, setPatientId] = useState<string>('');

  const { data: patientsData } = usePatients({ page: 0, size: 5000 });
  const { data: rxData, isLoading } = usePrescriptions(patientId || undefined);

  const patients = patientsData?.data || [];
  const prescriptions = rxData?.data || [];

  React.useEffect(() => {
    if (!patientId && patients.length > 0) {
      setPatientId(patients[0].id);
    }
  }, [patients, patientId]);

  const filtered = prescriptions.filter(rx => {
    if (statusFilter !== 'all' && rx.status.toLowerCase() !== statusFilter) return false;
    const patient = patients.find(p => p.id === rx.patientId);
    const patientName = patient ? `${patient.lastName} ${patient.firstName}` : '';
    if (search && !patientName.toLowerCase().includes(search.toLowerCase()) && !rx.prescriptionNumber.toLowerCase().includes(search.toLowerCase())) return false;
    return true;
  });

  const handleGenerateOrder = async (rx: PrescriptionDto) => {
    try {
      const res = await createOrder({
        patientId: rx.patientId,
        prescriptionId: rx.id,
        orderType: 'GLASSES',
        depositPaid: 0,
        notes: `Creat din rețeta ${rx.prescriptionNumber}`
      });

      setSelectedRx(null);
      toast.success('Comandă optică creată', { description: `${res.orderNumber} · Navigare la ERP Optic...` });
      setTimeout(() => navigate('/optical'), 600);
    } catch (err) {
      toast.error('A apărut o eroare la generarea comenzii optice');
    }
  };

  return (
    <AppLayout breadcrumbs={[{ label: 'Rețete' }]}>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-clinical-xl font-bold">Rețete Oftalmologice</h1>
        <div className="flex gap-4 items-center">
          <div className="flex items-center gap-2">
            <label className="text-clinical-sm font-semibold">Selectează Pacient:</label>
            <select 
              value={patientId} 
              onChange={(e) => setPatientId(e.target.value)}
              className="rounded-lg border border-border px-3 py-1.5 text-clinical-sm bg-background min-w-[200px]"
            >
              <option value="">-- Alege pacient --</option>
              {patients.map(p => (
                <option key={p.id} value={p.id}>{p.lastName} {p.firstName}</option>
              ))}
            </select>
          </div>
          <button onClick={() => navigate('/prescriptions/new')} className="px-4 py-2 rounded-lg bg-primary text-primary-foreground text-clinical-sm font-semibold">+ Rețetă Nouă</button>
        </div>
      </div>

      <div className="flex gap-3 mb-4">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground"/>
          <input value={search} onChange={e=>setSearch(e.target.value)} placeholder="Caută după pacient sau ID rețetă..."
            className="w-full pl-10 pr-3 py-2 rounded-lg border border-border bg-card text-clinical-sm"/>
        </div>
        {['all','active','expired','cancelled'].map(s => (
          <button key={s} onClick={()=>setStatusFilter(s)}
            className={`px-3 py-2 rounded-lg text-clinical-xs font-semibold border transition-colors ${statusFilter===s?'bg-primary text-primary-foreground border-primary':'border-border hover:bg-muted'}`}>
            {s === 'all' ? 'Toate' : s === 'active' ? 'Active' : s === 'expired' ? 'Expirate' : 'Anulate'}
          </button>
        ))}
      </div>

      <div className="bg-card rounded-xl border border-border shadow-sm overflow-hidden">
        <table className="w-full text-clinical-sm">
          <thead>
            <tr className="border-b border-border bg-muted/30">
              <th className="text-left p-3 text-clinical-xs text-muted-foreground font-semibold">Număr</th>
              <th className="text-left p-3 text-clinical-xs text-muted-foreground font-semibold">Pacient</th>
              <th className="text-left p-3 text-clinical-xs text-muted-foreground font-semibold">Data</th>
              <th className="text-left p-3 text-clinical-xs text-muted-foreground font-semibold">OD</th>
              <th className="text-left p-3 text-clinical-xs text-muted-foreground font-semibold">OS</th>
              <th className="text-left p-3 text-clinical-xs text-muted-foreground font-semibold">Status</th>
              <th className="text-left p-3 text-clinical-xs text-muted-foreground font-semibold">Medic</th>
              <th className="text-right p-3 text-clinical-xs text-muted-foreground font-semibold">Acțiuni</th>
            </tr>
          </thead>
          <tbody>
            {!patientId && (
              <tr><td colSpan={8} className="p-8 text-center text-muted-foreground text-clinical-sm">Vă rugăm să selectați un pacient.</td></tr>
            )}
            {patientId && isLoading && (
              <tr><td colSpan={8} className="p-8 text-center text-muted-foreground text-clinical-sm">Se încarcă...</td></tr>
            )}
            {patientId && !isLoading && filtered.length === 0 && (
              <tr><td colSpan={8} className="p-8 text-center text-muted-foreground text-clinical-sm">Nicio rețetă găsită.</td></tr>
            )}
            {filtered.map(rx => {
              const st = rx.status.toLowerCase() === 'superseded'
                ? { bg: '#EFF6FF', text: '#1D4ED8' }
                : prescriptionStatusStyles[rx.status.toLowerCase()] || { bg: '#eee', text: '#333' };
              const od = rx.lines.find(l => l.eye === 'OD');
              const os = rx.lines.find(l => l.eye === 'OS');
              const patient = patients.find(p => p.id === rx.patientId);
              const patientName = patient ? `${patient.lastName} ${patient.firstName}` : '';
              return (
                <tr key={rx.id} onClick={()=>setSelectedRx(rx)} className="border-b border-border hover:bg-muted/30 cursor-pointer transition-colors">
                  <td className="p-3 font-clinical text-primary">{rx.prescriptionNumber || rx.id.substring(0,8)}</td>
                  <td className="p-3 font-semibold">{patientName}</td>
                  <td className="p-3 text-muted-foreground">{new Date(rx.issuedAt || rx.createdAt).toLocaleDateString('ro-RO')}</td>
                  <td className="p-3 font-clinical text-clinical-xs">{formatRefraction(od)}</td>
                  <td className="p-3 font-clinical text-clinical-xs">{formatRefraction(os)}</td>
                  <td className="p-3"><span className="px-2 py-0.5 rounded-full text-[10px] font-semibold" style={{background:st.bg,color:st.text}}>{translateStatus(rx.status)}</span></td>
                  <td className="p-3 text-muted-foreground">{rx.issuedByName}</td>
                  <td className="p-3 text-right"><button className="text-primary hover:underline text-clinical-xs">Deschide</button></td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      {/* Detail modal */}
      {selectedRx && (
        <div className="fixed inset-0 bg-black/40 z-50 flex items-center justify-center p-4" onClick={()=>setSelectedRx(null)}>
          <div className="bg-card rounded-2xl shadow-xl max-w-lg w-full max-h-[90vh] overflow-auto" onClick={e=>e.stopPropagation()}>
            <div className="p-6">
              <div className="flex items-center justify-between mb-4">
                <div>
                  <h2 className="text-clinical-lg font-bold">REȚETĂ OFTALMOLOGICĂ</h2>
                  <p className="text-clinical-xs text-muted-foreground font-clinical">{selectedRx.prescriptionNumber}</p>
                </div>
                <button onClick={()=>setSelectedRx(null)}><X className="w-5 h-5 text-muted-foreground"/></button>
              </div>
              <div className="text-clinical-xs text-muted-foreground mb-4">
                <p>Pacient: <strong className="text-foreground">{patients.find(p => p.id === selectedRx.patientId)?.lastName} {patients.find(p => p.id === selectedRx.patientId)?.firstName}</strong></p>
                <p>Emitent: {selectedRx.issuedByName} · Data: {new Date(selectedRx.issuedAt || selectedRx.createdAt).toLocaleDateString('ro-RO')} · Valabilă până: {selectedRx.validUntil}</p>
              </div>

              <div className="grid grid-cols-2 gap-4 mb-4">
                <div className="p-4 rounded-xl border-2 border-red-200 bg-red-50/30">
                  <p className="text-clinical-xs font-semibold text-red-600 clinical-label mb-2">OD (Ochi Drept)</p>
                  <div className="font-clinical text-clinical-sm space-y-1">
                    {(() => {
                      const l = selectedRx.lines.find(x => x.eye === 'OD');
                      if (!l) return <p>—</p>;
                      return (
                        <>
                          <p>Sph: {l.sph ? (l.sph > 0 ? '+' : '') + l.sph.toFixed(2) + 'D' : '—'}</p>
                          <p>Cyl: {l.cyl ? l.cyl.toFixed(2) + 'D' : '—'}</p>
                          <p>Ax: {l.axis ? l.axis + '°' : '—'}</p>
                          {l.addPower && <p>Add: +{l.addPower.toFixed(2)}D</p>}
                        </>
                      );
                    })()}
                  </div>
                </div>
                <div className="p-4 rounded-xl border-2 border-blue-200 bg-blue-50/30">
                  <p className="text-clinical-xs font-semibold text-blue-600 clinical-label mb-2">OS (Ochi Stâng)</p>
                  <div className="font-clinical text-clinical-sm space-y-1">
                    {(() => {
                      const l = selectedRx.lines.find(x => x.eye === 'OS');
                      if (!l) return <p>—</p>;
                      return (
                        <>
                          <p>Sph: {l.sph ? (l.sph > 0 ? '+' : '') + l.sph.toFixed(2) + 'D' : '—'}</p>
                          <p>Cyl: {l.cyl ? l.cyl.toFixed(2) + 'D' : '—'}</p>
                          <p>Ax: {l.axis ? l.axis + '°' : '—'}</p>
                          {l.addPower && <p>Add: +{l.addPower.toFixed(2)}D</p>}
                        </>
                      );
                    })()}
                  </div>
                </div>
              </div>

              <div className="bg-muted/30 rounded-lg p-3 mb-4 text-clinical-xs">
                <p><span className="text-muted-foreground">DP Distanță:</span> <span className="font-clinical">{selectedRx.pdBinocular || `${selectedRx.pdOd} / ${selectedRx.pdOs}`}mm</span></p>
                {selectedRx.lensType && <p><span className="text-muted-foreground">Tip lentilă:</span> {translateLensType(selectedRx.lensType)}</p>}
                {selectedRx.lensMaterial && <p><span className="text-muted-foreground">Material:</span> {selectedRx.lensMaterial}</p>}
                {selectedRx.lensCoating && <p><span className="text-muted-foreground">Tratament:</span> {selectedRx.lensCoating}</p>}
              </div>

              {selectedRx.clinicalNotes && <p className="text-clinical-xs text-muted-foreground italic mb-4">{selectedRx.clinicalNotes}</p>}

              <div className="flex gap-2">
                <button onClick={() => setPrintRx(selectedRx)} className="flex-1 py-2 rounded-lg border border-border text-clinical-sm font-medium flex items-center justify-center gap-1 hover:bg-muted">
                  <Download className="w-4 h-4"/> Descarcă PDF
                </button>
                <button onClick={() => handleGenerateOrder(selectedRx)} className="flex-1 py-2 rounded-lg text-clinical-sm font-semibold flex items-center justify-center gap-1 text-accent-foreground bg-accent">
                  Generează Comandă Optică <ArrowRight className="w-4 h-4"/>
                </button>
              </div>
              <div className="flex gap-2 mt-2">
                <button onClick={() => toast('Reînnoire rețetă inițiată')} className="flex-1 py-2 rounded-lg border border-border text-clinical-sm font-medium flex items-center justify-center gap-1 hover:bg-muted">
                  <RefreshCw className="w-4 h-4"/> Reînnoire
                </button>
                <button onClick={() => { toast.error('Rețetă anulată'); setSelectedRx(null); }} className="flex-1 py-2 rounded-lg border border-red-200 text-red-600 text-clinical-sm font-medium hover:bg-red-50">Anulează</button>
              </div>
            </div>
          </div>
        </div>
      )}

      {printRx && (
        <PrintPreviewModal
          open={!!printRx}
          onClose={() => setPrintRx(null)}
          title="Rețetă Oftalmologică"
          subtitle={`${printRx.prescriptionNumber} · Pacient: ${patients.find(p=>p.id===printRx.patientId)?.lastName} · Emitent: ${printRx.issuedByName}`}
          doctorName={printRx.issuedByName}
        >
          <div>
            <h2 className="text-lg font-bold mb-4">Prescripție Optică</h2>
            <table className="w-full text-sm border border-gray-300 mb-4">
              <thead className="bg-gray-100"><tr><th className="p-2 border">Ochi</th><th className="p-2 border">Sph</th><th className="p-2 border">Cyl</th><th className="p-2 border">Axis</th><th className="p-2 border">Add</th></tr></thead>
              <tbody>
                <tr><td className="p-2 border font-bold">OD</td>
                <td className="p-2 border font-mono">{printRx.lines.find(l=>l.eye==='OD')?.sph?.toFixed(2) || '—'}</td>
                <td className="p-2 border font-mono">{printRx.lines.find(l=>l.eye==='OD')?.cyl?.toFixed(2) || '—'}</td>
                <td className="p-2 border font-mono">{printRx.lines.find(l=>l.eye==='OD')?.axis ? printRx.lines.find(l=>l.eye==='OD')?.axis + '°' : '—'}</td>
                <td className="p-2 border font-mono">{printRx.lines.find(l=>l.eye==='OD')?.addPower ? `+${printRx.lines.find(l=>l.eye==='OD')?.addPower?.toFixed(2)}` : '—'}</td></tr>
                <tr><td className="p-2 border font-bold">OS</td>
                <td className="p-2 border font-mono">{printRx.lines.find(l=>l.eye==='OS')?.sph?.toFixed(2) || '—'}</td>
                <td className="p-2 border font-mono">{printRx.lines.find(l=>l.eye==='OS')?.cyl?.toFixed(2) || '—'}</td>
                <td className="p-2 border font-mono">{printRx.lines.find(l=>l.eye==='OS')?.axis ? printRx.lines.find(l=>l.eye==='OS')?.axis + '°' : '—'}</td>
                <td className="p-2 border font-mono">{printRx.lines.find(l=>l.eye==='OS')?.addPower ? `+${printRx.lines.find(l=>l.eye==='OS')?.addPower?.toFixed(2)}` : '—'}</td></tr>
              </tbody>
            </table>
            <p className="text-sm"><strong>DP Distanță:</strong> {printRx.pdBinocular || `${printRx.pdOd} / ${printRx.pdOs}`}mm</p>
            {printRx.lensType && <p className="text-sm"><strong>Lentile:</strong> {translateLensType(printRx.lensType)} · <strong>Material:</strong> {printRx.lensMaterial} · <strong>Tratament:</strong> {printRx.lensCoating}</p>}
            {printRx.clinicalNotes && <p className="text-sm italic mt-3">{printRx.clinicalNotes}</p>}
            <p className="text-xs text-gray-600 mt-6">Valabilă până la: {printRx.validUntil}</p>
          </div>
        </PrintPreviewModal>
      )}
    </AppLayout>
  );
};

export default PrescriptionsPage;
