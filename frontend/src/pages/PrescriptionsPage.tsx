import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AppLayout from '@/components/AppLayout';
import { patients, prescriptionStatusStyles } from '@/data/demo-data';
import { FileText, Download, ArrowRight, Search, X, RefreshCw } from 'lucide-react';

const allPrescriptions = patients.flatMap(p => (p.prescriptions || []).map(rx => ({ ...rx, patientName: p.name, patientId: p.id })));

const formatRefraction = (eye: { sph: number; cyl: number; axis: number; add?: number }) =>
  `${eye.sph > 0 ? '+' : ''}${eye.sph.toFixed(2)} / ${eye.cyl.toFixed(2)} / ${eye.axis}°${eye.add ? ` Add +${eye.add.toFixed(2)}` : ''}`;

const PrescriptionsPage: React.FC = () => {
  const navigate = useNavigate();
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('all');
  const [selectedRx, setSelectedRx] = useState<typeof allPrescriptions[0] | null>(null);

  const filtered = allPrescriptions.filter(rx => {
    if (statusFilter !== 'all' && rx.status !== statusFilter) return false;
    if (search && !rx.patientName.toLowerCase().includes(search.toLowerCase()) && !rx.id.toLowerCase().includes(search.toLowerCase())) return false;
    return true;
  });

  return (
    <AppLayout breadcrumbs={[{ label: 'Rețete' }]}>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-clinical-xl font-bold">Rețete Oftalmologice</h1>
        <button className="px-4 py-2 rounded-lg bg-primary text-white text-clinical-sm font-semibold">+ Rețetă Nouă</button>
      </div>

      <div className="flex gap-3 mb-4">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground"/>
          <input value={search} onChange={e=>setSearch(e.target.value)} placeholder="Caută după pacient sau ID rețetă..."
            className="w-full pl-10 pr-3 py-2 rounded-lg border border-border bg-card text-clinical-sm"/>
        </div>
        {['all','active','expired','cancelled'].map(s => (
          <button key={s} onClick={()=>setStatusFilter(s)}
            className={`px-3 py-2 rounded-lg text-clinical-xs font-semibold border transition-colors ${statusFilter===s?'bg-primary text-white border-primary':'border-border hover:bg-muted'}`}>
            {s === 'all' ? 'Toate' : s === 'active' ? 'Active' : s === 'expired' ? 'Expirate' : 'Anulate'}
          </button>
        ))}
      </div>

      <div className="bg-card rounded-xl border border-border shadow-sm overflow-hidden">
        <table className="w-full text-clinical-sm">
          <thead>
            <tr className="border-b border-border bg-muted/30">
              <th className="text-left p-3 text-clinical-xs text-muted-foreground font-semibold">ID</th>
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
            {filtered.map(rx => {
              const st = prescriptionStatusStyles[rx.status];
              return (
                <tr key={rx.id} onClick={()=>setSelectedRx(rx)} className="border-b border-border hover:bg-muted/30 cursor-pointer transition-colors">
                  <td className="p-3 font-clinical text-primary">{rx.id}</td>
                  <td className="p-3 font-semibold">{rx.patientName}</td>
                  <td className="p-3 text-muted-foreground">{rx.date}</td>
                  <td className="p-3 font-clinical text-clinical-xs">{formatRefraction(rx.od)}</td>
                  <td className="p-3 font-clinical text-clinical-xs">{formatRefraction(rx.os)}</td>
                  <td className="p-3"><span className="px-2 py-0.5 rounded-full text-[10px] font-semibold" style={{background:st.bg,color:st.text}}>{rx.status==='active'?'Activă':rx.status==='expired'?'Expirată':'Anulată'}</span></td>
                  <td className="p-3 text-muted-foreground">{rx.doctorName}</td>
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
                  <p className="text-clinical-xs text-muted-foreground font-clinical">{selectedRx.id}</p>
                </div>
                <button onClick={()=>setSelectedRx(null)}><X className="w-5 h-5 text-muted-foreground"/></button>
              </div>
              <div className="text-clinical-xs text-muted-foreground mb-4">
                <p>Pacient: <strong className="text-foreground">{selectedRx.patientName}</strong> ({selectedRx.patientId})</p>
                <p>Emitent: {selectedRx.doctorName} · Data: {selectedRx.date} · Valabilă până: {selectedRx.validUntil}</p>
              </div>

              <div className="grid grid-cols-2 gap-4 mb-4">
                <div className="p-4 rounded-xl border-2 border-red-200 bg-red-50/30">
                  <p className="text-clinical-xs font-semibold text-red-600 clinical-label mb-2">OD (Ochi Drept)</p>
                  <div className="font-clinical text-clinical-sm space-y-1">
                    <p>Sph: {selectedRx.od.sph > 0 ? '+' : ''}{selectedRx.od.sph.toFixed(2)}D</p>
                    <p>Cyl: {selectedRx.od.cyl.toFixed(2)}D</p>
                    <p>Ax: {selectedRx.od.axis}°</p>
                    {selectedRx.od.add && <p>Add: +{selectedRx.od.add.toFixed(2)}D</p>}
                  </div>
                </div>
                <div className="p-4 rounded-xl border-2 border-blue-200 bg-blue-50/30">
                  <p className="text-clinical-xs font-semibold text-blue-600 clinical-label mb-2">OS (Ochi Stâng)</p>
                  <div className="font-clinical text-clinical-sm space-y-1">
                    <p>Sph: {selectedRx.os.sph > 0 ? '+' : ''}{selectedRx.os.sph.toFixed(2)}D</p>
                    <p>Cyl: {selectedRx.os.cyl.toFixed(2)}D</p>
                    <p>Ax: {selectedRx.os.axis}°</p>
                    {selectedRx.os.add && <p>Add: +{selectedRx.os.add.toFixed(2)}D</p>}
                  </div>
                </div>
              </div>

              <div className="bg-muted/30 rounded-lg p-3 mb-4 text-clinical-xs">
                <p><span className="text-muted-foreground">DP Distanță:</span> <span className="font-clinical">{selectedRx.pdDistance}mm</span></p>
                {selectedRx.pdNear && <p><span className="text-muted-foreground">DP Aproape:</span> <span className="font-clinical">{selectedRx.pdNear}mm</span></p>}
                {selectedRx.lensType && <p><span className="text-muted-foreground">Tip lentilă:</span> {selectedRx.lensType}</p>}
                {selectedRx.material && <p><span className="text-muted-foreground">Material:</span> {selectedRx.material}</p>}
              </div>

              {selectedRx.treatments && (
                <div className="flex flex-wrap gap-1 mb-4">
                  {selectedRx.treatments.map(t => <span key={t} className="px-2 py-0.5 rounded-full bg-primary/10 text-primary text-[10px] font-semibold">{t}</span>)}
                </div>
              )}

              {selectedRx.notes && <p className="text-clinical-xs text-muted-foreground italic mb-4">{selectedRx.notes}</p>}

              {/* Status timeline */}
              <div className="flex items-center gap-2 mb-6">
                {['Emisă','Comandă','Producție','Finalizată'].map((step,i) => (
                  <React.Fragment key={step}>
                    <div className={`flex items-center gap-1 ${i<=1?'text-primary':'text-muted-foreground'}`}>
                      <div className={`w-5 h-5 rounded-full flex items-center justify-center text-[9px] font-bold ${i<=1?'bg-primary text-white':'bg-muted text-muted-foreground'}`}>{i<=1?'✓':i+1}</div>
                      <span className="text-clinical-xs font-medium">{step}</span>
                    </div>
                    {i < 3 && <div className={`flex-1 h-0.5 ${i<1?'bg-primary':'bg-muted'}`}/>}
                  </React.Fragment>
                ))}
              </div>

              <div className="flex gap-2">
                <button className="flex-1 py-2 rounded-lg border border-border text-clinical-sm font-medium flex items-center justify-center gap-1 hover:bg-muted">
                  <Download className="w-4 h-4"/> Descarcă PDF
                </button>
                <button onClick={()=>{setSelectedRx(null);navigate('/optical');}} className="flex-1 py-2 rounded-lg text-clinical-sm font-semibold flex items-center justify-center gap-1 text-white"
                  style={{background:'hsl(var(--color-accent-500))'}}>
                  Generează Comandă Optică <ArrowRight className="w-4 h-4"/>
                </button>
              </div>
              <div className="flex gap-2 mt-2">
                <button className="flex-1 py-2 rounded-lg border border-border text-clinical-sm font-medium flex items-center justify-center gap-1 hover:bg-muted">
                  <RefreshCw className="w-4 h-4"/> Reînnoire
                </button>
                <button className="flex-1 py-2 rounded-lg border border-red-200 text-red-600 text-clinical-sm font-medium hover:bg-red-50">Anulează</button>
              </div>

              {/* QR placeholder */}
              <div className="mt-4 flex justify-end">
                <div className="w-14 h-14 bg-foreground/5 border border-border rounded-md flex items-center justify-center text-[8px] text-muted-foreground font-clinical">QR</div>
              </div>
            </div>
          </div>
        </div>
      )}
    </AppLayout>
  );
};

export default PrescriptionsPage;
