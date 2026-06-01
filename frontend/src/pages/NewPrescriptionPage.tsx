import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import AppLayout from '@/components/AppLayout';
import { usePatients } from '@/hooks/usePatients';
import { useCreatePrescription } from '@/hooks/usePrescriptions';
import type { CreatePrescriptionRequest } from '@/types/prescriptions';

import { ArrowLeft, Save, Loader2 } from 'lucide-react';

const lensTypes = ['Monofocal', 'Bifocal', 'Progresiv', 'Ocupațional'];
const materials = ['CR-39', 'Policarbonat', 'Trivex', 'Index 1.60', 'Index 1.67', 'Index 1.74'];
const treatmentsList = ['Antireflex', 'Anti-blue', 'Fotocromatic', 'Hidrofob', 'Anti-zgârieturi'];

const NewPrescriptionPage: React.FC = () => {
  const navigate = useNavigate();
  
  // Real API hooks
  const { data: patientsRes, isLoading: isLoadingPatients } = usePatients({ page: 0, size: 5000 });
  const { mutateAsync: createPrescription, isPending } = useCreatePrescription();
  const patients = patientsRes?.data || [];

  const [patientId, setPatientId] = useState('');
  
  useEffect(() => {
    if (!patientId && patients.length > 0) {
      setPatientId(patients[0].id);
    }
  }, [patients, patientId]);

  const today = new Date().toISOString().slice(0, 10);
  const oneYear = new Date(Date.now() + 365 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10);
  const [date, setDate] = useState(today);
  const [validUntil, setValidUntil] = useState(oneYear);
  const [od, setOd] = useState({ sph: 0, cyl: 0, axis: 0, add: 0 });
  const [os, setOs] = useState({ sph: 0, cyl: 0, axis: 0, add: 0 });
  const [pdDistance, setPdDistance] = useState(63);
  const [pdNear, setPdNear] = useState(60);
  const [lensType, setLensType] = useState('Monofocal');
  const [material, setMaterial] = useState('CR-39');
  const [treatments, setTreatments] = useState<string[]>(['Antireflex']);
  const [notes, setNotes] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});

  const toggleTreatment = (t: string) =>
    setTreatments(prev => prev.includes(t) ? prev.filter(x => x !== t) : [...prev, t]);

  const handleSave = async () => {
    const e: Record<string, string> = {};
    if (!patientId) e.patient = 'Pacient obligatoriu';
    if (!date) e.date = 'Data obligatorie';
    if (!validUntil) e.validUntil = 'Valabilitate obligatorie';

    // BUG #5 Fix: Check physically impossible lens parameters bounds
    if (od.sph < -30 || od.sph > 30 || os.sph < -30 || os.sph > 30) {
      toast.error('Eroare: Valoare SPH nepermisă (trebuie să fie între -30D și +30D).');
      return;
    }
    if (od.cyl < -10 || od.cyl > 10 || os.cyl < -10 || os.cyl > 10) {
      toast.error('Eroare: Valoare CYL nepermisă (trebuie să fie între -10D și +10D).');
      return;
    }

    if (Object.keys(e).length) { setErrors(e); return; }

    try {
      let mappedLensType: any = 'SINGLE_VISION';
      if (lensType === 'Monofocal') mappedLensType = 'SINGLE_VISION';
      else if (lensType === 'Bifocal') mappedLensType = 'BIFOCAL';
      else if (lensType === 'Progresiv') mappedLensType = 'PROGRESSIVE';
      else if (lensType === 'Ocupațional') mappedLensType = 'OFFICE';

      const rxPayload: CreatePrescriptionRequest = {
        patientId,
        prescriptionType: 'DISTANCE', // Default for now
        validFrom: date,
        validUntil: validUntil,
        pdBinocular: pdDistance,
        lensType: mappedLensType,
        lines: [
          { eye: 'OD', sph: od.sph, cyl: od.cyl, axis: od.axis, addPower: od.add || undefined },
          { eye: 'OS', sph: os.sph, cyl: os.cyl, axis: os.axis, addPower: os.add || undefined }
        ]
      };

      await createPrescription(rxPayload);
      toast.success('Rețetă creată cu succes');
      navigate('/prescriptions');
    } catch (err) {
      toast.error('A apărut o eroare la salvarea rețetei');
    }
  };

  const eyeInput = (
    label: string,
    eye: typeof od,
    setEye: typeof setOd,
    color: string,
  ) => (
    <div className={`p-4 rounded-xl border-2 ${color}`}>
      <p className="text-clinical-xs font-semibold clinical-label mb-3">{label}</p>
      <div className="grid grid-cols-4 gap-2">
        {(['sph', 'cyl', 'axis', 'add'] as const).map(k => {
          let min, max;
          if (k === 'sph') { min = -30; max = 30; }
          else if (k === 'cyl') { min = -10; max = 10; }
          else if (k === 'axis') { min = 0; max = 180; }
          else { min = 0; max = 10; } // add
          
          return (
            <div key={k}>
              <label className="text-[10px] text-muted-foreground uppercase block mb-1">{k}</label>
              <input
                type="number"
                min={min}
                max={max}
                step={k === 'axis' ? 1 : 0.25}
                value={eye[k]}
                onChange={e => setEye({ ...eye, [k]: parseFloat(e.target.value) || 0 })}
                className="w-full font-clinical text-clinical-sm rounded-md border border-border px-2 py-1.5 bg-background"
                disabled={isPending}
              />
            </div>
          );
        })}
      </div>
    </div>
  );

  return (
    <AppLayout breadcrumbs={[{ label: 'Rețete', path: '/prescriptions' }, { label: 'Rețetă Nouă' }]}>
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-3">
          <button onClick={() => navigate('/prescriptions')} className="p-2 rounded-lg hover:bg-muted" disabled={isPending}>
            <ArrowLeft className="w-4 h-4" />
          </button>
          <h1 className="text-clinical-xl font-bold">Rețetă Nouă</h1>
        </div>
        <button
          onClick={handleSave}
          disabled={isPending || isLoadingPatients}
          className="px-4 py-2 rounded-lg bg-primary text-primary-foreground text-clinical-sm font-semibold flex items-center gap-2 disabled:opacity-50"
        >
          {isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Save className="w-4 h-4" />}
          Salvează Rețeta
        </button>
      </div>

      <div className="max-w-4xl space-y-4 pb-20">
        <div className="bg-card rounded-xl border border-border shadow-sm p-5">
          <h3 className="text-clinical-sm font-semibold mb-3">Pacient & Date Emitere</h3>
          <div className="grid grid-cols-3 gap-4">
            <div className="col-span-3">
              <label className="text-clinical-xs text-muted-foreground block mb-1">Pacient *</label>
              <select 
                value={patientId} 
                onChange={e => setPatientId(e.target.value)}
                disabled={isLoadingPatients || isPending}
                className="w-full rounded-lg border border-border px-3 py-2 text-clinical-sm bg-background"
              >
                {isLoadingPatients && <option value="">Se încarcă pacienții...</option>}
                {!isLoadingPatients && patients.map(p => <option key={p.id} value={p.id}>{p.firstName} {p.lastName} ({p.mrn})</option>)}
              </select>
              {errors.patient && <p className="text-clinical-xs text-red-600 mt-1">{errors.patient}</p>}
            </div>
            <div>
              <label className="text-clinical-xs text-muted-foreground block mb-1">Data emiterii *</label>
              <input type="date" value={date} onChange={e => setDate(e.target.value)} disabled={isPending}
                className="w-full rounded-lg border border-border px-3 py-2 text-clinical-sm bg-background" />
            </div>
            <div>
              <label className="text-clinical-xs text-muted-foreground block mb-1">Valabilă până *</label>
              <input type="date" value={validUntil} onChange={e => setValidUntil(e.target.value)} disabled={isPending}
                className="w-full rounded-lg border border-border px-3 py-2 text-clinical-sm bg-background" />
            </div>
          </div>
        </div>

        <div className="bg-card rounded-xl border border-border shadow-sm p-5">
          <h3 className="text-clinical-sm font-semibold mb-3">Refracție</h3>
          <div className="grid grid-cols-2 gap-4">
            {eyeInput('OD (Ochi Drept)', od, setOd, 'border-red-200 bg-red-50/30')}
            {eyeInput('OS (Ochi Stâng)', os, setOs, 'border-blue-200 bg-blue-50/30')}
          </div>
        </div>

        <div className="bg-card rounded-xl border border-border shadow-sm p-5">
          <h3 className="text-clinical-sm font-semibold mb-3">Distanță Pupilară & Lentile</h3>
          <div className="grid grid-cols-4 gap-4">
            <div>
              <label className="text-clinical-xs text-muted-foreground block mb-1">DP Distanță (mm)</label>
              <input type="number" value={pdDistance} onChange={e => setPdDistance(+e.target.value)} disabled={isPending}
                className="w-full font-clinical rounded-lg border border-border px-3 py-2 text-clinical-sm bg-background" />
            </div>
            <div>
              <label className="text-clinical-xs text-muted-foreground block mb-1">DP Aproape (mm)</label>
              <input type="number" value={pdNear} onChange={e => setPdNear(+e.target.value)} disabled={isPending}
                className="w-full font-clinical rounded-lg border border-border px-3 py-2 text-clinical-sm bg-background" />
            </div>
            <div>
              <label className="text-clinical-xs text-muted-foreground block mb-1">Tip lentilă</label>
              <select value={lensType} onChange={e => setLensType(e.target.value)} disabled={isPending}
                className="w-full rounded-lg border border-border px-3 py-2 text-clinical-sm bg-background">
                {lensTypes.map(t => <option key={t} value={t}>{t}</option>)}
              </select>
            </div>
            <div>
              <label className="text-clinical-xs text-muted-foreground block mb-1">Material</label>
              <select value={material} onChange={e => setMaterial(e.target.value)} disabled={isPending}
                className="w-full rounded-lg border border-border px-3 py-2 text-clinical-sm bg-background">
                {materials.map(t => <option key={t} value={t}>{t}</option>)}
              </select>
            </div>
          </div>
          <div className="mt-4">
            <label className="text-clinical-xs text-muted-foreground block mb-2">Tratamente</label>
            <div className="flex flex-wrap gap-2">
              {treatmentsList.map(t => (
                <button key={t} type="button" onClick={() => toggleTreatment(t)} disabled={isPending}
                  className={`px-3 py-1.5 rounded-full text-clinical-xs font-semibold border transition-colors ${
                    treatments.includes(t)
                      ? 'bg-primary text-primary-foreground border-primary'
                      : 'border-border hover:bg-muted'
                  } disabled:opacity-50`}>
                  {t}
                </button>
              ))}
            </div>
          </div>
        </div>

        <div className="bg-card rounded-xl border border-border shadow-sm p-5">
          <h3 className="text-clinical-sm font-semibold mb-3">Observații</h3>
          <textarea value={notes} onChange={e => setNotes(e.target.value)} rows={3} disabled={isPending}
            placeholder="Observații pentru optician sau pacient..."
            className="w-full rounded-lg border border-border px-3 py-2 text-clinical-sm bg-background" />
        </div>
      </div>
    </AppLayout>
  );
};

export default NewPrescriptionPage;
