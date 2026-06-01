import React, { useState, useMemo } from 'react';
import { Check, AlertCircle } from 'lucide-react';

const OSDI_QUESTIONS = [
  'Ochi sensibili la lumină',
  'Ochi care se simt nisipoși',
  'Ochi dureroși sau iritați',
  'Vedere încețoșată',
  'Vedere slabă',
  'Citit',
  'Condus pe timp de noapte',
  'Lucrat la calculator / ATM',
  'Privit la TV',
  'Condiții vântoase',
  'Locuri cu umiditate scăzută',
  'Zone cu aer condiționat',
];
const OSDI_OPTIONS = ['Niciodată', 'Uneori', 'Jumătate din timp', 'Aproape mereu', 'Tot timpul'];

const VFQ_QUESTIONS = [
  'Cum apreciați vederea în general?',
  'Cât vă afectează vederea activitățile zilnice?',
  'Aveți durere sau disconfort ocular?',
  'Cât de bine vedeți pentru a citi un ziar?',
  'Cât de bine vedeți pentru a recunoaște persoane?',
  'Vă simțiți frustrat din cauza vederii?',
  'Aveți dificultăți la condus noaptea?',
  'Aveți nevoie de ajutor pentru sarcini detaliate?',
];
const VFQ_OPTIONS = ['Excelent', 'Bun', 'Mediu', 'Slab', 'Foarte slab'];

type Mode = 'menu' | 'osdi' | 'amsler' | 'vfq';

const PROQuestionnaires: React.FC = () => {
  const [mode, setMode] = useState<Mode>('menu');
  const [osdiAnswers, setOsdiAnswers] = useState<number[]>(Array(12).fill(-1));
  const [vfqAnswers, setVfqAnswers] = useState<number[]>(Array(8).fill(-1));
  const [amslerOD, setAmslerOD] = useState<'normal' | 'distorted' | null>(null);
  const [amslerOS, setAmslerOS] = useState<'normal' | 'distorted' | null>(null);
  const [submitted, setSubmitted] = useState<Mode | null>(null);

  const osdiScore = useMemo(() => {
    const answered = osdiAnswers.filter(a => a >= 0);
    if (answered.length === 0) return 0;
    const sum = answered.reduce((s, a) => s + a, 0);
    return Math.round((sum * 100) / (answered.length * 4));
  }, [osdiAnswers]);

  const osdiSeverity = osdiScore <= 12 ? { label: 'Normal', color: 'text-green-600 bg-green-50 border-green-200' } :
    osdiScore <= 22 ? { label: 'Sindrom ușor', color: 'text-amber-600 bg-amber-50 border-amber-200' } :
    osdiScore <= 32 ? { label: 'Sindrom moderat', color: 'text-orange-600 bg-orange-50 border-orange-200' } :
    { label: 'Sindrom sever', color: 'text-red-600 bg-red-50 border-red-200' };

  if (mode === 'menu') {
    return (
      <div className="space-y-3">
        <h3 className="text-clinical-sm font-semibold mb-3">Chestionare PRO (Patient-Reported Outcomes)</h3>
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          <button onClick={() => setMode('osdi')} className="p-4 rounded-xl border border-border bg-card hover:border-primary hover:shadow-md text-left transition-all">
            <span className="text-2xl mb-2 block">💧</span>
            <p className="text-clinical-sm font-semibold">OSDI</p>
            <p className="text-[11px] text-muted-foreground">Ochi uscat — 12 întrebări</p>
          </button>
          <button onClick={() => setMode('amsler')} className="p-4 rounded-xl border border-border bg-card hover:border-primary hover:shadow-md text-left transition-all">
            <span className="text-2xl mb-2 block">🔲</span>
            <p className="text-clinical-sm font-semibold">Grilă Amsler</p>
            <p className="text-[11px] text-muted-foreground">Auto-test maculă</p>
          </button>
          <button onClick={() => setMode('vfq')} className="p-4 rounded-xl border border-border bg-card hover:border-primary hover:shadow-md text-left transition-all">
            <span className="text-2xl mb-2 block">👁</span>
            <p className="text-clinical-sm font-semibold">VFQ-25</p>
            <p className="text-[11px] text-muted-foreground">Calitatea vederii</p>
          </button>
        </div>
      </div>
    );
  }

  // OSDI
  if (mode === 'osdi') {
    const allAnswered = osdiAnswers.every(a => a >= 0);
    return (
      <div className="bg-card rounded-xl border border-border p-5">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-clinical-md font-semibold">OSDI — Ocular Surface Disease Index</h3>
          <button onClick={() => { setMode('menu'); setSubmitted(null); }} className="text-clinical-xs text-muted-foreground hover:text-foreground">← Înapoi</button>
        </div>
        {submitted === 'osdi' ? (
          <div className="text-center py-6">
            <div className={`inline-block px-6 py-4 rounded-xl border-2 ${osdiSeverity.color}`}>
              <p className="text-clinical-xs font-semibold uppercase tracking-wider mb-1">Scor OSDI</p>
              <p className="text-4xl font-bold font-clinical">{osdiScore}/100</p>
              <p className="text-clinical-sm font-semibold mt-2">{osdiSeverity.label}</p>
            </div>
            <p className="text-clinical-xs text-muted-foreground mt-4">Rezultatul a fost transmis medicului dvs. pentru evaluare.</p>
            <button onClick={() => setSubmitted(null)} className="mt-4 px-4 py-2 rounded-lg border border-border text-clinical-xs font-medium hover:bg-muted">Refă testul</button>
          </div>
        ) : (
          <>
            <p className="text-clinical-xs text-muted-foreground mb-4">În ultima săptămână, cât de des ați experimentat:</p>
            <div className="space-y-3 max-h-[400px] overflow-y-auto">
              {OSDI_QUESTIONS.map((q, qi) => (
                <div key={qi} className="border-b border-border pb-3">
                  <p className="text-clinical-sm font-medium mb-2">{qi + 1}. {q}</p>
                  <div className="grid grid-cols-5 gap-1">
                    {OSDI_OPTIONS.map((opt, oi) => (
                      <button key={oi} onClick={() => setOsdiAnswers(prev => prev.map((a, i) => i === qi ? oi : a))}
                        className={`px-2 py-1.5 rounded text-[10px] font-medium border transition-colors ${osdiAnswers[qi] === oi ? 'bg-primary text-white border-primary' : 'border-border hover:bg-muted'}`}>
                        {opt}
                      </button>
                    ))}
                  </div>
                </div>
              ))}
            </div>
            <button disabled={!allAnswered} onClick={() => setSubmitted('osdi')}
              className="mt-4 w-full py-2.5 rounded-lg bg-primary text-white text-clinical-sm font-semibold disabled:opacity-50 disabled:cursor-not-allowed">
              {allAnswered ? 'Calculează scorul' : `Completați toate întrebările (${osdiAnswers.filter(a => a >= 0).length}/12)`}
            </button>
          </>
        )}
      </div>
    );
  }

  // Amsler
  if (mode === 'amsler') {
    return (
      <div className="bg-card rounded-xl border border-border p-5">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-clinical-md font-semibold">Grilă Amsler — Test Maculă</h3>
          <button onClick={() => setMode('menu')} className="text-clinical-xs text-muted-foreground hover:text-foreground">← Înapoi</button>
        </div>
        <div className="bg-amber-50 border border-amber-200 rounded-lg p-3 mb-4 text-clinical-xs text-amber-800">
          <strong>Instrucțiuni:</strong> Acoperiți un ochi, priviți punctul central de la 30 cm. Liniile par drepte sau distorsionate?
        </div>
        <div className="grid grid-cols-2 gap-6">
          {(['OD', 'OS'] as const).map(eye => {
            const setter = eye === 'OD' ? setAmslerOD : setAmslerOS;
            const value = eye === 'OD' ? amslerOD : amslerOS;
            return (
              <div key={eye} className="text-center">
                <p className={`text-clinical-xs font-semibold mb-2 ${eye === 'OD' ? 'text-red-600' : 'text-blue-600'} clinical-label`}>{eye === 'OD' ? 'Ochi Drept (acoperă OS)' : 'Ochi Stâng (acoperă OD)'}</p>
                <svg viewBox="0 0 200 200" className="w-full max-w-[180px] mx-auto bg-white border border-border">
                  {Array.from({ length: 21 }).map((_, i) => (
                    <line key={`h${i}`} x1="0" y1={i * 10} x2="200" y2={i * 10} stroke="black" strokeWidth="0.5" />
                  ))}
                  {Array.from({ length: 21 }).map((_, i) => (
                    <line key={`v${i}`} x1={i * 10} y1="0" x2={i * 10} y2="200" stroke="black" strokeWidth="0.5" />
                  ))}
                  <circle cx="100" cy="100" r="3" fill="red" />
                </svg>
                <div className="grid grid-cols-2 gap-2 mt-3">
                  <button onClick={() => setter('normal')} className={`py-2 rounded-lg text-clinical-xs font-medium border transition-colors ${value === 'normal' ? 'bg-green-100 text-green-700 border-green-300' : 'border-border hover:bg-muted'}`}>
                    ✓ Normal
                  </button>
                  <button onClick={() => setter('distorted')} className={`py-2 rounded-lg text-clinical-xs font-medium border transition-colors ${value === 'distorted' ? 'bg-red-100 text-red-700 border-red-300' : 'border-border hover:bg-muted'}`}>
                    ⚠ Distorsionat
                  </button>
                </div>
              </div>
            );
          })}
        </div>
        {(amslerOD === 'distorted' || amslerOS === 'distorted') && (
          <div className="mt-4 p-3 rounded-lg bg-red-50 border border-red-200 text-red-800 text-clinical-xs flex items-start gap-2">
            <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
            <div>Distorsiune detectată. <strong>Recomandăm consult oftalmologic urgent.</strong> Medicul a fost notificat.</div>
          </div>
        )}
      </div>
    );
  }

  // VFQ
  if (mode === 'vfq') {
    const allAnswered = vfqAnswers.every(a => a >= 0);
    const score = allAnswered ? Math.round((vfqAnswers.reduce((s, a) => s + (4 - a) * 25, 0)) / vfqAnswers.length) : 0;
    return (
      <div className="bg-card rounded-xl border border-border p-5">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-clinical-md font-semibold">VFQ-25 — Calitatea Vederii</h3>
          <button onClick={() => { setMode('menu'); setSubmitted(null); }} className="text-clinical-xs text-muted-foreground hover:text-foreground">← Înapoi</button>
        </div>
        {submitted === 'vfq' ? (
          <div className="text-center py-6">
            <div className="inline-block px-6 py-4 rounded-xl border-2 border-primary/30 bg-primary/5">
              <p className="text-clinical-xs font-semibold uppercase tracking-wider mb-1">Scor VFQ</p>
              <p className="text-4xl font-bold font-clinical text-primary">{score}/100</p>
              <p className="text-clinical-sm mt-2">{score >= 75 ? 'Calitate vedere bună' : score >= 50 ? 'Calitate moderată' : 'Calitate redusă'}</p>
            </div>
          </div>
        ) : (
          <>
            <div className="space-y-3 max-h-[400px] overflow-y-auto">
              {VFQ_QUESTIONS.map((q, qi) => (
                <div key={qi} className="border-b border-border pb-3">
                  <p className="text-clinical-sm font-medium mb-2">{qi + 1}. {q}</p>
                  <div className="grid grid-cols-5 gap-1">
                    {VFQ_OPTIONS.map((opt, oi) => (
                      <button key={oi} onClick={() => setVfqAnswers(prev => prev.map((a, i) => i === qi ? oi : a))}
                        className={`px-2 py-1.5 rounded text-[10px] font-medium border transition-colors ${vfqAnswers[qi] === oi ? 'bg-primary text-white border-primary' : 'border-border hover:bg-muted'}`}>
                        {opt}
                      </button>
                    ))}
                  </div>
                </div>
              ))}
            </div>
            <button disabled={!allAnswered} onClick={() => setSubmitted('vfq')}
              className="mt-4 w-full py-2.5 rounded-lg bg-primary text-white text-clinical-sm font-semibold disabled:opacity-50 disabled:cursor-not-allowed">
              {allAnswered ? 'Calculează scorul' : `Completați toate întrebările (${vfqAnswers.filter(a => a >= 0).length}/8)`}
            </button>
          </>
        )}
      </div>
    );
  }

  return null;
};

export default PROQuestionnaires;
