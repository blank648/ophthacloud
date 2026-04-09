import React, { useState } from 'react';
import AppLayout from '@/components/AppLayout';
import { Eye, X, ZoomIn, Maximize, Clock } from 'lucide-react';

const investigations = [
  { id: 'INV-001', type: 'OCT Macular', subtype: 'Macular', patient: 'Ion Marinescu', patientId: 'OC-004821', date: '15.01.2026', status: 'reviewed', doctor: 'Dr. Popescu',
    data: { rnflOD: 72, rnflOS: 88, macThickOD: 342, macThickOS: 298, macVolOD: 8.2, macVolOS: 7.8, flags: ['RNFL < 80µm OD — posibil defect'] } },
  { id: 'INV-002', type: 'Câmp Vizual', subtype: '24-2 SITA Standard', patient: 'Ion Marinescu', patientId: 'OC-004821', date: '15.01.2026', status: 'reviewed', doctor: 'Dr. Popescu',
    data: { mdOD: -4.2, mdOS: -1.8, psdOD: 3.8, psdOS: 1.5, ghtOD: 'Outside Normal Limits', ghtOS: 'Borderline', flOD: 8, fpOD: 5, fnOD: 3, gpaOD: 'Possible Progression', gpaOS: 'Stable' } },
  { id: 'INV-003', type: 'Topografie Corneană', subtype: 'Pentacam', patient: 'Andrei Popescu', patientId: 'OC-005410', date: '05.03.2026', status: 'completed', doctor: 'Dr. Popescu',
    data: { k1OD: 46.5, k2OD: 49.2, kmeanOD: 47.85, axisOD: 15, k1OS: 44.0, k2OS: 45.5, kmeanOS: 44.75, axisOS: 170, pachyOD: 465, pachyOS: 502, kcIndexOD: 1.52, classOD: 'Keratoconus suspect', classOS: 'Normal' } },
  { id: 'INV-004', type: 'Fundus Photo', subtype: 'Macular + Disc', patient: 'Elena Dumitrescu', patientId: 'OC-002765', date: '08.01.2026', status: 'reviewed', doctor: 'Dr. Mihailescu',
    data: { findingsOD: 'Drusen moi macular, fără fluid', findingsOS: 'Drusen moi macular, fără fluid', type: 'Color + FAF' } },
  { id: 'INV-005', type: 'Biometrie Oculară', subtype: 'IOLMaster', patient: 'Elena Dumitrescu', patientId: 'OC-002765', date: '15.03.2026', status: 'completed', doctor: 'Dr. Mihailescu',
    data: { axLenOD: 23.12, axLenOS: 23.45, k1OD: 43.75, k2OD: 44.25, k1OS: 43.50, k2OS: 44.00, acdOD: 3.12, acdOS: 3.18,
      iolCalc: [
        { formula: 'SRK-II', powerOD: 20.5, refrOD: '-0.25' },
        { formula: 'Haigis', powerOD: 20.0, refrOD: '-0.12' },
        { formula: 'Barrett Universal II', powerOD: 19.5, refrOD: '+0.05' },
      ] } },
];

const statusColors: Record<string, { bg: string; text: string }> = {
  ordered: { bg: '#EFF6FF', text: '#1D4ED8' },
  scheduled: { bg: '#FEF9C3', text: '#854D0E' },
  completed: { bg: '#ECFDF5', text: '#065F46' },
  reviewed: { bg: '#F0FDF4', text: '#15803D' },
};

const statusLabels: Record<string, string> = { ordered: 'Comandată', scheduled: 'Programată', completed: 'Efectuată', reviewed: 'Revizuită' };

const InvestigationsPage: React.FC = () => {
  const [selected, setSelected] = useState<typeof investigations[0] | null>(null);

  return (
    <AppLayout breadcrumbs={[{ label: 'Investigații' }]}>
      <h1 className="text-clinical-xl font-bold mb-6">Investigații</h1>

      <div className="bg-card rounded-xl border border-border shadow-sm overflow-hidden mb-6">
        <table className="w-full text-clinical-sm">
          <thead>
            <tr className="border-b border-border bg-muted/30">
              <th className="text-left p-3 text-clinical-xs text-muted-foreground font-semibold">ID</th>
              <th className="text-left p-3 text-clinical-xs text-muted-foreground font-semibold">Tip</th>
              <th className="text-left p-3 text-clinical-xs text-muted-foreground font-semibold">Pacient</th>
              <th className="text-left p-3 text-clinical-xs text-muted-foreground font-semibold">Data</th>
              <th className="text-left p-3 text-clinical-xs text-muted-foreground font-semibold">Status</th>
              <th className="text-left p-3 text-clinical-xs text-muted-foreground font-semibold">Medic</th>
            </tr>
          </thead>
          <tbody>
            {investigations.map(inv => {
              const st = statusColors[inv.status];
              return (
                <tr key={inv.id} onClick={() => setSelected(inv)} className="border-b border-border hover:bg-muted/30 cursor-pointer transition-colors">
                  <td className="p-3 font-clinical text-primary">{inv.id}</td>
                  <td className="p-3 font-semibold">{inv.type} <span className="text-clinical-xs text-muted-foreground">({inv.subtype})</span></td>
                  <td className="p-3">{inv.patient}</td>
                  <td className="p-3 text-muted-foreground">{inv.date}</td>
                  <td className="p-3"><span className="px-2 py-0.5 rounded-full text-[10px] font-semibold" style={{ background: st.bg, color: st.text }}>{statusLabels[inv.status]}</span></td>
                  <td className="p-3 text-muted-foreground">{inv.doctor}</td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      {/* Detail modal with DICOM-style viewer */}
      {selected && (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4" onClick={() => setSelected(null)}>
          <div className="bg-card rounded-2xl shadow-xl max-w-4xl w-full max-h-[90vh] overflow-auto" onClick={e => e.stopPropagation()}>
            <div className="flex items-center justify-between p-4 border-b border-border">
              <div>
                <h2 className="text-clinical-lg font-bold">{selected.type}</h2>
                <p className="text-clinical-xs text-muted-foreground">{selected.patient} · {selected.date} · {selected.subtype}</p>
              </div>
              <button onClick={() => setSelected(null)}><X className="w-5 h-5" /></button>
            </div>

            <div className="grid grid-cols-2 gap-0">
              {/* DICOM-style dark viewer */}
              <div className="bg-[#0F1923] p-6 min-h-[400px] flex flex-col">
                <div className="flex items-center gap-2 mb-3">
                  <ZoomIn className="w-4 h-4 text-gray-400" /><Maximize className="w-4 h-4 text-gray-400" />
                  <span className="text-gray-500 text-[10px] font-clinical ml-auto">{selected.id} · {selected.date}</span>
                </div>
                <div className="flex-1 rounded-lg bg-[#1a2a3a] flex items-center justify-center border border-gray-700">
                  <div className="text-center">
                    <Eye className="w-12 h-12 text-gray-600 mx-auto mb-2" />
                    <p className="text-gray-500 text-clinical-xs font-clinical">{selected.type}</p>
                    <p className="text-gray-600 text-[10px]">DICOM Viewer — Date demo</p>
                  </div>
                </div>
                {/* Timeline scrubber */}
                <div className="mt-3 flex items-center gap-2">
                  <Clock className="w-3 h-3 text-gray-500" />
                  <div className="flex-1 h-1 bg-gray-700 rounded-full"><div className="h-full w-2/3 bg-primary rounded-full" /></div>
                  <span className="text-gray-500 text-[9px] font-clinical">Baseline → Curent</span>
                </div>
              </div>

              {/* Data panel */}
              <div className="p-6">
                {selected.type === 'OCT Macular' && (
                  <div className="space-y-4">
                    <h4 className="text-clinical-sm font-semibold">Rezultate OCT</h4>
                    {(selected.data as any).flags?.map((f: string, i: number) => (
                      <div key={i} className="p-2 rounded-lg bg-amber-50 border border-amber-200 text-amber-800 text-clinical-xs">⚠ {f}</div>
                    ))}
                    <div className="grid grid-cols-2 gap-3">
                      <div className="bg-red-50/30 border border-red-200 rounded-lg p-3">
                        <p className="text-[10px] font-semibold text-red-600 clinical-label mb-1">OD</p>
                        <p className="font-clinical text-clinical-sm">RNFL: <span className={`font-bold ${(selected.data as any).rnflOD < 80 ? 'text-red-600' : 'text-green-600'}`}>{(selected.data as any).rnflOD}µm</span></p>
                        <p className="font-clinical text-clinical-xs">Macular: {(selected.data as any).macThickOD}µm · Vol: {(selected.data as any).macVolOD}mm³</p>
                      </div>
                      <div className="bg-blue-50/30 border border-blue-200 rounded-lg p-3">
                        <p className="text-[10px] font-semibold text-blue-600 clinical-label mb-1">OS</p>
                        <p className="font-clinical text-clinical-sm">RNFL: <span className="font-bold text-green-600">{(selected.data as any).rnflOS}µm</span></p>
                        <p className="font-clinical text-clinical-xs">Macular: {(selected.data as any).macThickOS}µm · Vol: {(selected.data as any).macVolOS}mm³</p>
                      </div>
                    </div>
                  </div>
                )}
                {selected.type === 'Câmp Vizual' && (
                  <div className="space-y-4">
                    <h4 className="text-clinical-sm font-semibold">Câmp Vizual — Indici</h4>
                    <div className="grid grid-cols-2 gap-3">
                      <div className="bg-red-50/30 border border-red-200 rounded-lg p-3">
                        <p className="text-[10px] font-semibold text-red-600 clinical-label mb-1">OD</p>
                        <p className="font-clinical text-clinical-sm">MD: <span className="font-bold">{(selected.data as any).mdOD} dB</span></p>
                        <p className="font-clinical text-clinical-xs">PSD: {(selected.data as any).psdOD} dB</p>
                        <p className="text-clinical-xs mt-1">GHT: <span className="font-semibold text-red-600">{(selected.data as any).ghtOD}</span></p>
                        <p className="text-clinical-xs">GPA: <span className="font-semibold text-amber-600">{(selected.data as any).gpaOD}</span></p>
                      </div>
                      <div className="bg-blue-50/30 border border-blue-200 rounded-lg p-3">
                        <p className="text-[10px] font-semibold text-blue-600 clinical-label mb-1">OS</p>
                        <p className="font-clinical text-clinical-sm">MD: <span className="font-bold">{(selected.data as any).mdOS} dB</span></p>
                        <p className="font-clinical text-clinical-xs">PSD: {(selected.data as any).psdOS} dB</p>
                        <p className="text-clinical-xs mt-1">GHT: <span className="font-semibold text-amber-600">{(selected.data as any).ghtOS}</span></p>
                        <p className="text-clinical-xs">GPA: <span className="font-semibold text-green-600">{(selected.data as any).gpaOS}</span></p>
                      </div>
                    </div>
                    <p className="text-clinical-xs text-muted-foreground">FL: {(selected.data as any).flOD}% · FP: {(selected.data as any).fpOD}% · FN: {(selected.data as any).fnOD}% — Reliable</p>
                  </div>
                )}
                {selected.type === 'Topografie Corneană' && (
                  <div className="space-y-4">
                    <h4 className="text-clinical-sm font-semibold">Topografie Corneană</h4>
                    <div className="grid grid-cols-2 gap-3">
                      <div className="bg-red-50/30 border border-red-200 rounded-lg p-3">
                        <p className="text-[10px] font-semibold text-red-600 clinical-label mb-1">OD</p>
                        <p className="font-clinical text-clinical-xs">K1: {(selected.data as any).k1OD}D · K2: {(selected.data as any).k2OD}D</p>
                        <p className="font-clinical text-clinical-xs">Kmean: {(selected.data as any).kmeanOD}D · Ax: {(selected.data as any).axisOD}°</p>
                        <p className="font-clinical text-clinical-xs">Pachy min: <span className={`font-bold ${(selected.data as any).pachyOD < 480 ? 'text-red-600' : 'text-green-600'}`}>{(selected.data as any).pachyOD}µm</span></p>
                        <p className="text-clinical-xs mt-1">KC Index: <span className="font-semibold text-amber-600">{(selected.data as any).kcIndexOD}</span></p>
                        <p className="text-[10px] font-semibold text-amber-600 mt-1">{(selected.data as any).classOD}</p>
                      </div>
                      <div className="bg-blue-50/30 border border-blue-200 rounded-lg p-3">
                        <p className="text-[10px] font-semibold text-blue-600 clinical-label mb-1">OS</p>
                        <p className="font-clinical text-clinical-xs">K1: {(selected.data as any).k1OS}D · K2: {(selected.data as any).k2OS}D</p>
                        <p className="font-clinical text-clinical-xs">Kmean: {(selected.data as any).kmeanOS}D · Ax: {(selected.data as any).axisOS}°</p>
                        <p className="font-clinical text-clinical-xs">Pachy min: <span className="font-bold text-green-600">{(selected.data as any).pachyOS}µm</span></p>
                        <p className="text-[10px] font-semibold text-green-600 mt-1">{(selected.data as any).classOS}</p>
                      </div>
                    </div>
                  </div>
                )}
                {selected.type === 'Fundus Photo' && (
                  <div className="space-y-4">
                    <h4 className="text-clinical-sm font-semibold">Fotografie Fundus</h4>
                    <p className="text-clinical-xs text-muted-foreground">Tip: {(selected.data as any).type}</p>
                    <div className="grid grid-cols-2 gap-3">
                      <div className="bg-red-50/30 border border-red-200 rounded-lg p-3"><p className="text-[10px] font-semibold text-red-600 clinical-label mb-1">OD</p><p className="text-clinical-xs">{(selected.data as any).findingsOD}</p></div>
                      <div className="bg-blue-50/30 border border-blue-200 rounded-lg p-3"><p className="text-[10px] font-semibold text-blue-600 clinical-label mb-1">OS</p><p className="text-clinical-xs">{(selected.data as any).findingsOS}</p></div>
                    </div>
                  </div>
                )}
                {selected.type === 'Biometrie Oculară' && (
                  <div className="space-y-4">
                    <h4 className="text-clinical-sm font-semibold">Biometrie & Calcul IOL</h4>
                    <div className="bg-muted/30 rounded-lg p-3 font-clinical text-clinical-xs">
                      <p>OD: Ax {(selected.data as any).axLenOD}mm · K1 {(selected.data as any).k1OD}D · K2 {(selected.data as any).k2OD}D · ACD {(selected.data as any).acdOD}mm</p>
                    </div>
                    <table className="w-full text-clinical-xs">
                      <thead><tr className="border-b"><th className="text-left p-2">Formulă</th><th className="text-left p-2">IOL Power</th><th className="text-left p-2">Refracție postop</th></tr></thead>
                      <tbody>
                        {(selected.data as any).iolCalc.map((c: any, i: number) => (
                          <tr key={i} className={`border-b ${c.formula === 'Barrett Universal II' ? 'bg-primary/5 font-semibold' : ''}`}>
                            <td className="p-2">{c.formula}{c.formula === 'Barrett Universal II' && ' ★'}</td>
                            <td className="p-2 font-clinical">{c.powerOD}D</td>
                            <td className="p-2 font-clinical">{c.refrOD}D</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      )}
    </AppLayout>
  );
};

export default InvestigationsPage;
