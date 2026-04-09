import React, { useState, useMemo, useCallback } from 'react';
import AppLayout from '@/components/AppLayout';
import { patients } from '@/data/demo-data';
import { Eye, Check, AlertTriangle, Info, Lock, FileText, Star, Plus, Trash2, ChevronRight } from 'lucide-react';

const VA_OPTIONS = ['6/6','6/9','6/12','6/18','6/24','6/36','6/60','1/60','PL','NPL'];
const IOP_METHODS = ['GAT Goldmann','Aer (Non-contact)','Rebound iCare'];
const ICD10_CATALOG = [
  {code:'H52.0',name:'Miopie'},{code:'H52.1',name:'Hipermetropie'},{code:'H52.2',name:'Astigmatism'},
  {code:'H52.3',name:'Anizometropie'},{code:'H52.4',name:'Prezbiopie'},{code:'H10.0',name:'Conjunctivită mucopurulentă'},
  {code:'H16.0',name:'Keratită ulcerativă'},{code:'H17.0',name:'Leucom aderent'},{code:'H18.6',name:'Keratoconus'},
  {code:'H25.0',name:'Cataractă senilă imatură'},{code:'H25.1',name:'Cataractă senilă nucleară'},
  {code:'H25.2',name:'Cataractă senilă subcapsulară post.'},{code:'H40.1',name:'Glaucom unghi deschis'},
  {code:'H40.2',name:'Glaucom unghi închis'},{code:'H40.0',name:'Glaucom suspect'},
  {code:'H34.1',name:'Ocluzie arteră centrală retiniană'},{code:'H35.0',name:'Retinopatie diabetică NPDR'},
  {code:'H35.02',name:'Retinopatie diabetică PDR'},{code:'H35.31',name:'AMD uscat'},
  {code:'H35.32',name:'AMD umed'},{code:'H43.1',name:'Hemoragie vitreană'},
  {code:'H49.0',name:'Paralizie n.III'},{code:'H50.0',name:'Esotropie'},{code:'H53.01',name:'Ambliopie'},
];

const TEMPLATES = [
  {id:1,name:'Screening Glaucom',icon:'🔍',codes:['H40.0'],prefill:{iopFocus:true}},
  {id:2,name:'Retinopatie Diabetică',icon:'🩸',codes:['H35.0'],prefill:{}},
  {id:3,name:'Evaluare Pediatrică',icon:'👶',codes:['H53.01','H50.0'],prefill:{}},
  {id:4,name:'Pre-op Cataractă',icon:'🔬',codes:['H25.1'],prefill:{}},
  {id:5,name:'AMD Follow-up',icon:'👁',codes:['H35.31'],prefill:{}},
  {id:6,name:'Post-LASIK',icon:'✨',codes:['H18.6'],prefill:{}},
  {id:7,name:'Post-injecție IVT',icon:'💉',codes:['H35.32'],prefill:{}},
  {id:8,name:'Consultație Progresive',icon:'👓',codes:['H52.4'],prefill:{}},
];

const sectionLabels = ['A — Acuitate & Refracție','B — Pupile & Motilitate','C — Segment Anterior','D — Tensiune Oculară','E — Segment Posterior','F — Diagnostice ICD-10','G — Plan Tratament','H — Template-uri','I — Semnătură'];

const ConsultationPage: React.FC = () => {
  const patient = patients[0]; // Ion Marinescu pre-loaded
  const [activeSection, setActiveSection] = useState(0);
  const [signed, setSigned] = useState(false);
  const [templateApplied, setTemplateApplied] = useState<string|null>(null);

  // Section A state
  const [vaOD, setVaOD] = useState('6/12');
  const [vaOS, setVaOS] = useState('6/9');
  const [bcvaOD, setBcvaOD] = useState('6/9');
  const [bcvaOS, setBcvaOS] = useState('6/6');
  const [sphOD, setSphOD] = useState(-1.50);
  const [cylOD, setCylOD] = useState(-0.75);
  const [axOD, setAxOD] = useState(90);
  const [addOD, setAddOD] = useState(2.00);
  const [sphOS, setSphOS] = useState(-1.25);
  const [cylOS, setCylOS] = useState(-0.50);
  const [axOS, setAxOS] = useState(85);
  const [addOS, setAddOS] = useState(2.00);
  const [pdDist, setPdDist] = useState(63.5);
  const [pdNear, setPdNear] = useState(60);
  const [cycloplegic, setCycloplegic] = useState(false);

  // Section D state
  const [iopOD, setIopOD] = useState(26);
  const [iopOS, setIopOS] = useState(22);
  const [iopMethod, setIopMethod] = useState('GAT Goldmann');
  const [iopTime, setIopTime] = useState('09:15');

  // Section F state
  const [diagnoses, setDiagnoses] = useState<{code:string;name:string;primary:boolean}[]>([
    {code:'H40.1',name:'Glaucom unghi deschis',primary:true},
    {code:'H35.0',name:'Retinopatie diabetică NPDR',primary:false},
  ]);
  const [icdSearch, setIcdSearch] = useState('');

  // Section G state
  const [medications, setMedications] = useState([
    {name:'Timolol 0.5%',dose:'1 pic',eye:'OU',freq:'x2/zi',duration:'90'},
    {name:'Latanoprost 0.005%',dose:'1 pic',eye:'OU',freq:'seara',duration:'90'},
  ]);

  // Computed
  const seqOD = useMemo(() => (sphOD + cylOD/2).toFixed(2), [sphOD, cylOD]);
  const seqOS = useMemo(() => (sphOS + cylOS/2).toFixed(2), [sphOS, cylOS]);
  const iopColorOD = iopOD <= 21 ? 'iop-normal' : iopOD <= 25 ? 'iop-warning' : 'iop-danger';
  const iopColorOS = iopOS <= 21 ? 'iop-normal' : iopOS <= 25 ? 'iop-warning' : 'iop-danger';

  const completedSections = useMemo(() => {
    const c = new Set<number>();
    c.add(0); // A always has data
    c.add(3); // D
    c.add(5); // F
    c.add(6); // G
    return c;
  }, []);

  const filteredICD = useMemo(() => {
    if (!icdSearch) return [];
    const q = icdSearch.toLowerCase();
    return ICD10_CATALOG.filter(c => c.code.toLowerCase().includes(q) || c.name.toLowerCase().includes(q)).slice(0,6);
  }, [icdSearch]);

  const applyTemplate = useCallback((t: typeof TEMPLATES[0]) => {
    setTemplateApplied(t.name);
    const newDiags = t.codes.map((code,i) => {
      const found = ICD10_CATALOG.find(c => c.code === code);
      return { code, name: found?.name || code, primary: i === 0 };
    });
    setDiagnoses(prev => {
      const existing = prev.map(d => ({...d, primary: false}));
      return [...newDiags, ...existing.filter(e => !newDiags.some(n => n.code === e.code))];
    });
    setActiveSection(5);
  }, []);

  const handleSign = () => { setSigned(true); setActiveSection(8); };

  // Diagnosis recommender
  const recommender = useMemo(() => {
    if (diagnoses.some(d => d.code.startsWith('H40'))) return { msg: 'ℹ Glaucom detectat — Recomandăm: Câmp Vizual + OCT Disc Optic', color: 'bg-blue-50 border-blue-200 text-blue-800' };
    if (diagnoses.some(d => d.code.startsWith('H35.0'))) return { msg: 'ℹ Retinopatie diabetică — Recomandăm: OCT Macular + Angiografie', color: 'bg-amber-50 border-amber-200 text-amber-800' };
    if (diagnoses.some(d => d.code.startsWith('H35.3'))) return { msg: 'ℹ AMD — Recomandăm: OCT Macular + Autofluorescență + Grid Amsler', color: 'bg-purple-50 border-purple-200 text-purple-800' };
    return null;
  }, [diagnoses]);

  return (
    <AppLayout breadcrumbs={[{ label: 'Consultație EMR' }, { label: patient.name }]}>
      {signed && (
        <div className="mb-4 p-3 rounded-lg bg-green-50 border border-green-200 text-green-800 text-clinical-sm font-semibold flex items-center gap-2">
          <Check className="w-5 h-5" /> ✓ Consultație semnată digital · Dr. Alexandru Popescu · 29.03.2026 · 14:32
        </div>
      )}
      {templateApplied && (
        <div className="mb-4 p-3 rounded-lg bg-amber-50 border border-amber-200 text-amber-800 text-clinical-sm flex items-center gap-2">
          <Info className="w-4 h-4" /> Template aplicat: [{templateApplied}] — câmpuri pre-completate. Verificați și ajustați.
        </div>
      )}

      <div className="flex gap-6">
        {/* Left panel — sticky */}
        <div className="w-[38%] shrink-0">
          <div className="sticky top-20 space-y-4">
            {/* Patient card */}
            <div className="bg-card rounded-xl border border-border shadow-sm p-4">
              <div className="flex items-center gap-3 mb-3">
                <div className="w-12 h-12 rounded-full bg-primary/10 flex items-center justify-center text-primary font-bold">{patient.firstName[0]}{patient.lastName[0]}</div>
                <div>
                  <h3 className="text-clinical-base font-semibold">{patient.name}</h3>
                  <p className="text-clinical-xs text-muted-foreground font-clinical">{patient.id} · {patient.age} ani</p>
                </div>
              </div>
              <div className="flex flex-wrap gap-1 mb-2">
                {patient.clinicalFlags.map(f => (
                  <span key={f} className="px-2 py-0.5 rounded-full text-[10px] font-semibold" style={{background:'hsl(var(--color-danger)/0.1)',color:'hsl(var(--color-danger))'}}>{f}</span>
                ))}
              </div>
              <div className="text-clinical-xs text-muted-foreground">
                <p>Dg: {patient.primaryDiagnosis} ({patient.icdCode})</p>
                <p>Ultima vizită: {patient.lastVisit}</p>
              </div>
            </div>

            {/* Section stepper */}
            <div className="bg-card rounded-xl border border-border shadow-sm p-4">
              <div className="flex items-center justify-between mb-3">
                <h4 className="text-clinical-sm font-semibold">Secțiuni</h4>
                <span className="text-clinical-xs text-muted-foreground font-clinical">{completedSections.size}/9</span>
              </div>
              <div className="w-full h-1.5 rounded-full bg-muted mb-3">
                <div className="h-full rounded-full bg-primary transition-all" style={{width:`${(completedSections.size/9)*100}%`}}/>
              </div>
              <div className="space-y-1">
                {sectionLabels.map((label, i) => (
                  <button key={i} onClick={() => setActiveSection(i)}
                    className={`w-full flex items-center gap-2 px-3 py-2 rounded-lg text-clinical-xs transition-colors text-left ${
                      activeSection === i ? 'bg-primary/10 text-primary font-semibold' : 'hover:bg-muted text-foreground'
                    }`}>
                    <div className={`w-5 h-5 rounded-full flex items-center justify-center text-[10px] font-bold shrink-0 ${
                      completedSections.has(i) ? 'bg-green-100 text-green-700' : 'bg-muted text-muted-foreground'
                    }`}>{completedSections.has(i) ? '✓' : String.fromCharCode(65+i)}</div>
                    {label}
                  </button>
                ))}
              </div>
            </div>
          </div>
        </div>

        {/* Right panel — scrollable */}
        <div className="flex-1 min-w-0">
          <fieldset disabled={signed} className="space-y-6">

          {/* SECTION A */}
          {activeSection === 0 && (
            <div className="bg-card rounded-xl border border-border shadow-sm p-6">
              <h2 className="text-clinical-md font-semibold mb-4 flex items-center gap-2"><Eye className="w-5 h-5 text-primary"/>A — Acuitate Vizuală & Refracție</h2>
              {(bcvaOD === '6/18' || bcvaOD === '6/24' || bcvaOD === '6/36' || bcvaOD === '6/60' || bcvaOD === '1/60') && (
                <div className="mb-4 p-2 rounded-lg bg-orange-50 border border-orange-200 text-orange-800 text-clinical-xs flex items-center gap-2">
                  <AlertTriangle className="w-4 h-4"/> ⚠ Recomandăm test pinhole — diferențiere optică vs. patologie
                </div>
              )}
              <div className="grid grid-cols-[auto_1fr_1fr] gap-3 items-center text-clinical-sm">
                <div/><div className="text-center font-semibold text-red-600 text-clinical-xs clinical-label">OD (Ochi Drept)</div><div className="text-center font-semibold text-blue-600 text-clinical-xs clinical-label">OS (Ochi Stâng)</div>
                
                <label className="text-clinical-xs text-muted-foreground font-semibold">VOsC</label>
                <select value={vaOD} onChange={e=>setVaOD(e.target.value)} className="clinical-input rounded-md px-2 py-1.5 text-clinical-sm">{VA_OPTIONS.map(v=><option key={v}>{v}</option>)}</select>
                <select value={vaOS} onChange={e=>setVaOS(e.target.value)} className="clinical-input rounded-md px-2 py-1.5 text-clinical-sm">{VA_OPTIONS.map(v=><option key={v}>{v}</option>)}</select>

                <label className="text-clinical-xs text-muted-foreground font-semibold">BCVA</label>
                <select value={bcvaOD} onChange={e=>setBcvaOD(e.target.value)} className="clinical-input rounded-md px-2 py-1.5 text-clinical-sm">{VA_OPTIONS.map(v=><option key={v}>{v}</option>)}</select>
                <select value={bcvaOS} onChange={e=>setBcvaOS(e.target.value)} className="clinical-input rounded-md px-2 py-1.5 text-clinical-sm">{VA_OPTIONS.map(v=><option key={v}>{v}</option>)}</select>

                <label className="text-clinical-xs text-muted-foreground font-semibold">Sph (D)</label>
                <input type="number" step={0.25} value={sphOD} onChange={e=>setSphOD(+e.target.value)} className="clinical-input rounded-md px-2 py-1.5 text-clinical-sm text-right"/>
                <input type="number" step={0.25} value={sphOS} onChange={e=>setSphOS(+e.target.value)} className="clinical-input rounded-md px-2 py-1.5 text-clinical-sm text-right"/>

                <label className="text-clinical-xs text-muted-foreground font-semibold">Cyl (D)</label>
                <input type="number" step={0.25} value={cylOD} onChange={e=>setCylOD(+e.target.value)} className="clinical-input rounded-md px-2 py-1.5 text-clinical-sm text-right"/>
                <input type="number" step={0.25} value={cylOS} onChange={e=>setCylOS(+e.target.value)} className="clinical-input rounded-md px-2 py-1.5 text-clinical-sm text-right"/>

                <label className="text-clinical-xs text-muted-foreground font-semibold">Ax (°)</label>
                <input type="number" min={0} max={180} value={axOD} onChange={e=>setAxOD(+e.target.value)} className="clinical-input rounded-md px-2 py-1.5 text-clinical-sm text-right"/>
                <input type="number" min={0} max={180} value={axOS} onChange={e=>setAxOS(+e.target.value)} className="clinical-input rounded-md px-2 py-1.5 text-clinical-sm text-right"/>

                <label className="text-clinical-xs text-muted-foreground font-semibold">Add (D)</label>
                <input type="number" step={0.25} value={addOD} onChange={e=>setAddOD(+e.target.value)} className="clinical-input rounded-md px-2 py-1.5 text-clinical-sm text-right"/>
                <input type="number" step={0.25} value={addOS} onChange={e=>setAddOS(+e.target.value)} className="clinical-input rounded-md px-2 py-1.5 text-clinical-sm text-right"/>

                <label className="text-clinical-xs text-muted-foreground font-semibold">SEQ</label>
                <div className="font-clinical text-clinical-sm text-center py-1.5 rounded-md bg-primary/5 text-primary font-semibold">{seqOD}D</div>
                <div className="font-clinical text-clinical-sm text-center py-1.5 rounded-md bg-primary/5 text-primary font-semibold">{seqOS}D</div>

                <label className="text-clinical-xs text-muted-foreground font-semibold">DP dist</label>
                <input type="number" step={0.5} value={pdDist} onChange={e=>setPdDist(+e.target.value)} className="clinical-input rounded-md px-2 py-1.5 text-clinical-sm text-right col-span-2" placeholder="mm"/>
                
                <label className="text-clinical-xs text-muted-foreground font-semibold">DP aproape</label>
                <input type="number" step={0.5} value={pdNear} onChange={e=>setPdNear(+e.target.value)} className="clinical-input rounded-md px-2 py-1.5 text-clinical-sm text-right col-span-2" placeholder="mm"/>
              </div>
              <label className="flex items-center gap-2 mt-4 text-clinical-sm">
                <input type="checkbox" checked={cycloplegic} onChange={e=>setCycloplegic(e.target.checked)} className="rounded"/>
                Refracție cicloplegică (pediatrie)
              </label>
              <div className="flex justify-end mt-4">
                <button onClick={()=>setActiveSection(1)} className="px-4 py-2 rounded-lg bg-primary text-white text-clinical-sm font-semibold flex items-center gap-1">Următorul <ChevronRight className="w-4 h-4"/></button>
              </div>
            </div>
          )}

          {/* SECTION B */}
          {activeSection === 1 && (
            <div className="bg-card rounded-xl border border-border shadow-sm p-6">
              <h2 className="text-clinical-md font-semibold mb-4">B — Reacții Pupilare & Motilitate Oculară</h2>
              <div className="grid grid-cols-[auto_1fr_1fr] gap-3 text-clinical-sm">
                <div/><div className="text-center font-semibold text-red-600 text-clinical-xs clinical-label">OD</div><div className="text-center font-semibold text-blue-600 text-clinical-xs clinical-label">OS</div>
                {['Reflex pupilar direct','APD','Reflex consensual','Reacție acomodare'].map(label => (
                  <React.Fragment key={label}>
                    <label className="text-clinical-xs text-muted-foreground font-semibold">{label}</label>
                    <select className="clinical-input rounded-md px-2 py-1.5 text-clinical-sm">
                      <option>Normal</option><option>Lent</option><option>Fix</option>{label==='APD'&&<option>Prezent</option>}
                    </select>
                    <select className="clinical-input rounded-md px-2 py-1.5 text-clinical-sm">
                      <option>Normal</option><option>Lent</option><option>Fix</option>{label==='APD'&&<option>Prezent</option>}
                    </select>
                  </React.Fragment>
                ))}
              </div>
              <h4 className="text-clinical-sm font-semibold mt-6 mb-3">Motilitate oculară</h4>
              <div className="grid grid-cols-2 gap-3 text-clinical-sm">
                <div><label className="text-clinical-xs text-muted-foreground block mb-1">Test cover</label>
                  <select className="clinical-input rounded-md px-2 py-1.5 w-full"><option>Ortoforă</option><option>Esoforă</option><option>Exoforă</option><option>Hipertropie</option></select>
                </div>
                <div><label className="text-clinical-xs text-muted-foreground block mb-1">Binocularitate</label>
                  <select className="clinical-input rounded-md px-2 py-1.5 w-full"><option>Normal</option><option>Alterată</option></select>
                </div>
                <div><label className="text-clinical-xs text-muted-foreground block mb-1">Nistagmus</label>
                  <select className="clinical-input rounded-md px-2 py-1.5 w-full"><option>Absent</option><option>Pendular</option><option>Jerk</option><option>Rotator</option></select>
                </div>
                <div><label className="text-clinical-xs text-muted-foreground block mb-1">Versiuni & vergențe</label>
                  <input className="clinical-input rounded-md px-2 py-1.5 w-full" defaultValue="Normal"/></div>
              </div>
              <div className="flex justify-between mt-4">
                <button onClick={()=>setActiveSection(0)} className="px-4 py-2 rounded-lg border border-border text-clinical-sm font-medium">← Anterior</button>
                <button onClick={()=>setActiveSection(2)} className="px-4 py-2 rounded-lg bg-primary text-white text-clinical-sm font-semibold flex items-center gap-1">Următorul <ChevronRight className="w-4 h-4"/></button>
              </div>
            </div>
          )}

          {/* SECTION C */}
          {activeSection === 2 && (
            <div className="bg-card rounded-xl border border-border shadow-sm p-6">
              <h2 className="text-clinical-md font-semibold mb-4">C — Segment Anterior</h2>
              {['Pleoape','Conjunctivă','Cornee','Iris','Cameră anterioară','Cristalin'].map(struct => (
                <div key={struct} className="mb-4">
                  <label className="text-clinical-xs text-muted-foreground font-semibold block mb-1">{struct}</label>
                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <span className="text-[10px] font-semibold text-red-600 clinical-label block mb-0.5">OD</span>
                      <input className="clinical-input rounded-md px-2 py-1.5 w-full text-clinical-sm" defaultValue={struct==='Cristalin'?'Clar':'Normal'}/>
                    </div>
                    <div>
                      <span className="text-[10px] font-semibold text-blue-600 clinical-label block mb-0.5">OS</span>
                      <input className="clinical-input rounded-md px-2 py-1.5 w-full text-clinical-sm" defaultValue={struct==='Cristalin'?'Clar':'Normal'}/>
                    </div>
                  </div>
                </div>
              ))}
              <div className="flex justify-between mt-4">
                <button onClick={()=>setActiveSection(1)} className="px-4 py-2 rounded-lg border border-border text-clinical-sm font-medium">← Anterior</button>
                <button onClick={()=>setActiveSection(3)} className="px-4 py-2 rounded-lg bg-primary text-white text-clinical-sm font-semibold flex items-center gap-1">Următorul <ChevronRight className="w-4 h-4"/></button>
              </div>
            </div>
          )}

          {/* SECTION D — IOP */}
          {activeSection === 3 && (
            <div className="bg-card rounded-xl border border-border shadow-sm p-6">
              <h2 className="text-clinical-md font-semibold mb-4">D — Tensiune Oculară (IOP)</h2>
              {iopOD > 25 && (
                <div className="mb-4 p-3 rounded-lg bg-red-50 border border-red-300 text-red-800 text-clinical-sm font-semibold flex items-center gap-2">
                  <AlertTriangle className="w-5 h-5"/> 🚨 IOP ridicat OD — evaluare glaucom urgentă
                </div>
              )}
              {iopOD > 21 && iopOD <= 25 && (
                <div className="mb-4 p-3 rounded-lg bg-amber-50 border border-amber-200 text-amber-800 text-clinical-sm flex items-center gap-2">
                  <AlertTriangle className="w-4 h-4"/> ⚠ Hipertensiune oculară OD
                </div>
              )}
              {iopOS > 21 && iopOS <= 25 && (
                <div className="mb-4 p-3 rounded-lg bg-amber-50 border border-amber-200 text-amber-800 text-clinical-sm flex items-center gap-2">
                  <AlertTriangle className="w-4 h-4"/> ⚠ Hipertensiune oculară OS
                </div>
              )}
              <div className="grid grid-cols-2 gap-8 mb-6">
                <div className="text-center">
                  <span className="text-clinical-xs font-semibold text-red-600 clinical-label block mb-2">OD (Ochi Drept)</span>
                  <div className="relative">
                    <input type="number" value={iopOD} onChange={e=>setIopOD(+e.target.value)}
                      className={`clinical-input rounded-xl w-full text-center text-3xl font-clinical py-4 ${iopColorOD}`}/>
                    <span className="absolute right-4 top-1/2 -translate-y-1/2 text-clinical-sm text-muted-foreground">mmHg</span>
                  </div>
                  <p className={`text-clinical-xs mt-1 ${iopColorOD}`}>
                    {iopOD <= 21 ? 'Normal (10-21 mmHg)' : iopOD <= 25 ? '⚠ Hipertensiune oculară' : '🚨 IOP ridicat'}
                  </p>
                </div>
                <div className="text-center">
                  <span className="text-clinical-xs font-semibold text-blue-600 clinical-label block mb-2">OS (Ochi Stâng)</span>
                  <div className="relative">
                    <input type="number" value={iopOS} onChange={e=>setIopOS(+e.target.value)}
                      className={`clinical-input rounded-xl w-full text-center text-3xl font-clinical py-4 ${iopColorOS}`}/>
                    <span className="absolute right-4 top-1/2 -translate-y-1/2 text-clinical-sm text-muted-foreground">mmHg</span>
                  </div>
                  <p className={`text-clinical-xs mt-1 ${iopColorOS}`}>
                    {iopOS <= 21 ? 'Normal (10-21 mmHg)' : iopOS <= 25 ? '⚠ Hipertensiune oculară' : '🚨 IOP ridicat'}
                  </p>
                </div>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="text-clinical-xs text-muted-foreground font-semibold block mb-1">Metodă</label>
                  <div className="flex gap-2 flex-wrap">
                    {IOP_METHODS.map(m => (
                      <button key={m} onClick={()=>setIopMethod(m)}
                        className={`px-3 py-1.5 rounded-lg text-clinical-xs font-medium border transition-colors ${iopMethod===m?'bg-primary text-white border-primary':'border-border hover:bg-muted'}`}>
                        {m}{m==='GAT Goldmann'&&' ★'}
                      </button>
                    ))}
                  </div>
                </div>
                <div>
                  <label className="text-clinical-xs text-muted-foreground font-semibold block mb-1">Ora măsurătorii</label>
                  <input type="time" value={iopTime} onChange={e=>setIopTime(e.target.value)} className="clinical-input rounded-md px-2 py-1.5 text-clinical-sm"/>
                </div>
              </div>
              <div className="flex justify-between mt-6">
                <button onClick={()=>setActiveSection(2)} className="px-4 py-2 rounded-lg border border-border text-clinical-sm font-medium">← Anterior</button>
                <button onClick={()=>setActiveSection(4)} className="px-4 py-2 rounded-lg bg-primary text-white text-clinical-sm font-semibold flex items-center gap-1">Următorul <ChevronRight className="w-4 h-4"/></button>
              </div>
            </div>
          )}

          {/* SECTION E */}
          {activeSection === 4 && (
            <div className="bg-card rounded-xl border border-border shadow-sm p-6">
              <h2 className="text-clinical-md font-semibold mb-4">E — Segment Posterior (Fundoscopie)</h2>
              {['Disc optic','Artere / Vene','Maculă','Retină periferie','Vitros'].map(struct => (
                <div key={struct} className="mb-4">
                  <label className="text-clinical-xs text-muted-foreground font-semibold block mb-1">{struct}</label>
                  <div className="grid grid-cols-2 gap-3">
                    <div><span className="text-[10px] font-semibold text-red-600 clinical-label block mb-0.5">OD</span>
                      <textarea className="clinical-input rounded-md px-2 py-1.5 w-full text-clinical-sm" rows={2}
                        defaultValue={struct==='Disc optic'?'Roz, CDR 0.7, excavație superioară':struct==='Maculă'?'Reflex foveolar prezent':'Normal'}/>
                    </div>
                    <div><span className="text-[10px] font-semibold text-blue-600 clinical-label block mb-0.5">OS</span>
                      <textarea className="clinical-input rounded-md px-2 py-1.5 w-full text-clinical-sm" rows={2}
                        defaultValue={struct==='Disc optic'?'Roz, CDR 0.5, margini nete':struct==='Maculă'?'Reflex foveolar prezent':'Normal'}/>
                    </div>
                  </div>
                </div>
              ))}
              <div className="flex justify-between mt-4">
                <button onClick={()=>setActiveSection(3)} className="px-4 py-2 rounded-lg border border-border text-clinical-sm font-medium">← Anterior</button>
                <button onClick={()=>setActiveSection(5)} className="px-4 py-2 rounded-lg bg-primary text-white text-clinical-sm font-semibold flex items-center gap-1">Următorul <ChevronRight className="w-4 h-4"/></button>
              </div>
            </div>
          )}

          {/* SECTION F — Diagnostice */}
          {activeSection === 5 && (
            <div className="bg-card rounded-xl border border-border shadow-sm p-6">
              <h2 className="text-clinical-md font-semibold mb-4">F — Diagnostice & Codificare ICD-10</h2>
              {recommender && (
                <div className={`mb-4 p-3 rounded-lg border text-clinical-sm flex items-center gap-2 ${recommender.color}`}>
                  <Info className="w-4 h-4 shrink-0"/> {recommender.msg}
                  <button className="ml-auto px-3 py-1 rounded-md bg-white/60 text-clinical-xs font-semibold border hover:bg-white">Adaugă investigații</button>
                </div>
              )}
              <div className="relative mb-4">
                <input value={icdSearch} onChange={e=>setIcdSearch(e.target.value)} placeholder="Caută cod ICD-10 sau diagnostic..."
                  className="w-full clinical-input rounded-md px-3 py-2 text-clinical-sm"/>
                {filteredICD.length > 0 && (
                  <div className="absolute z-10 top-full left-0 right-0 mt-1 bg-card border border-border rounded-lg shadow-lg overflow-hidden">
                    {filteredICD.map(c => (
                      <button key={c.code} onClick={() => {
                        if (!diagnoses.some(d=>d.code===c.code)) setDiagnoses(p=>[...p,{code:c.code,name:c.name,primary:false}]);
                        setIcdSearch('');
                      }} className="w-full text-left px-3 py-2 hover:bg-muted text-clinical-sm flex items-center gap-2">
                        <span className="font-clinical text-primary">{c.code}</span> {c.name}
                      </button>
                    ))}
                  </div>
                )}
              </div>
              <div className="flex flex-wrap gap-2 mb-4">
                {diagnoses.map((d,i) => (
                  <div key={d.code} className={`flex items-center gap-1.5 px-3 py-1.5 rounded-full text-clinical-xs font-semibold border ${d.primary?'bg-primary/10 border-primary text-primary':'bg-muted border-border text-foreground'}`}>
                    <button onClick={()=>setDiagnoses(prev=>prev.map((dd,ii)=>({...dd,primary:ii===i})))} title="Set primary">
                      <Star className={`w-3.5 h-3.5 ${d.primary?'fill-primary text-primary':'text-muted-foreground'}`}/>
                    </button>
                    <span className="font-clinical">{d.code}</span> {d.name}
                    <button onClick={()=>setDiagnoses(prev=>prev.filter((_,ii)=>ii!==i))}><Trash2 className="w-3 h-3 text-muted-foreground hover:text-red-500"/></button>
                  </div>
                ))}
              </div>
              <div className="flex justify-between mt-4">
                <button onClick={()=>setActiveSection(4)} className="px-4 py-2 rounded-lg border border-border text-clinical-sm font-medium">← Anterior</button>
                <button onClick={()=>setActiveSection(6)} className="px-4 py-2 rounded-lg bg-primary text-white text-clinical-sm font-semibold flex items-center gap-1">Următorul <ChevronRight className="w-4 h-4"/></button>
              </div>
            </div>
          )}

          {/* SECTION G — Plan Tratament */}
          {activeSection === 6 && (
            <div className="bg-card rounded-xl border border-border shadow-sm p-6">
              <h2 className="text-clinical-md font-semibold mb-4">G — Plan Tratament</h2>
              <h4 className="text-clinical-sm font-semibold mb-2">Medicație oftalmică</h4>
              <div className="space-y-2 mb-4">
                {medications.map((med,i) => (
                  <div key={i} className="grid grid-cols-[1fr_auto_auto_auto_auto_auto] gap-2 items-center">
                    <input className="clinical-input rounded-md px-2 py-1.5 text-clinical-sm" value={med.name} onChange={e=>setMedications(p=>p.map((m,ii)=>ii===i?{...m,name:e.target.value}:m))}/>
                    <input className="clinical-input rounded-md px-2 py-1.5 text-clinical-sm w-16" value={med.dose} onChange={e=>setMedications(p=>p.map((m,ii)=>ii===i?{...m,dose:e.target.value}:m))}/>
                    <select className="clinical-input rounded-md px-1 py-1.5 text-clinical-xs w-14" value={med.eye} onChange={e=>setMedications(p=>p.map((m,ii)=>ii===i?{...m,eye:e.target.value}:m))}>
                      <option>OD</option><option>OS</option><option>OU</option>
                    </select>
                    <input className="clinical-input rounded-md px-2 py-1.5 text-clinical-sm w-20" value={med.freq} onChange={e=>setMedications(p=>p.map((m,ii)=>ii===i?{...m,freq:e.target.value}:m))}/>
                    <input className="clinical-input rounded-md px-2 py-1.5 text-clinical-sm w-16" value={med.duration} onChange={e=>setMedications(p=>p.map((m,ii)=>ii===i?{...m,duration:e.target.value}:m))} placeholder="zile"/>
                    <button onClick={()=>setMedications(p=>p.filter((_,ii)=>ii!==i))}><Trash2 className="w-4 h-4 text-muted-foreground hover:text-red-500"/></button>
                  </div>
                ))}
                <button onClick={()=>setMedications(p=>[...p,{name:'',dose:'',eye:'OU',freq:'',duration:''}])}
                  className="flex items-center gap-1 text-clinical-xs text-primary font-semibold hover:underline"><Plus className="w-3 h-3"/>Adaugă medicament</button>
              </div>
              <div className="grid grid-cols-2 gap-4 mb-4">
                <button className="px-4 py-2.5 rounded-lg border-2 border-dashed border-primary/30 text-primary text-clinical-sm font-semibold hover:bg-primary/5">📋 Generează Rețetă</button>
                <div><label className="text-clinical-xs text-muted-foreground block mb-1">Data follow-up</label>
                  <input type="date" defaultValue="2026-06-29" className="clinical-input rounded-md px-2 py-1.5 text-clinical-sm w-full"/></div>
              </div>
              <h4 className="text-clinical-sm font-semibold mb-2">Instrucțiuni pacient</h4>
              <textarea className="clinical-input rounded-md px-3 py-2 w-full text-clinical-sm" rows={3} defaultValue="Continuare tratament topic cu Timolol + Latanoprost. Control IOP la 3 luni. Evitare efort fizic intens."/>
              <div className="flex justify-between mt-4">
                <button onClick={()=>setActiveSection(5)} className="px-4 py-2 rounded-lg border border-border text-clinical-sm font-medium">← Anterior</button>
                <button onClick={()=>setActiveSection(7)} className="px-4 py-2 rounded-lg bg-primary text-white text-clinical-sm font-semibold flex items-center gap-1">Următorul <ChevronRight className="w-4 h-4"/></button>
              </div>
            </div>
          )}

          {/* SECTION H — Templates */}
          {activeSection === 7 && (
            <div className="bg-card rounded-xl border border-border shadow-sm p-6">
              <h2 className="text-clinical-md font-semibold mb-4">H — Template-uri Clinice</h2>
              <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
                {TEMPLATES.map(t => (
                  <button key={t.id} onClick={()=>applyTemplate(t)}
                    className={`p-4 rounded-xl border text-left transition-all hover:shadow-md hover:border-primary/50 ${
                      templateApplied===t.name ? 'bg-amber-50 border-amber-300 ring-2 ring-amber-200' : 'border-border bg-card hover:bg-muted/30'
                    }`}>
                    <span className="text-2xl block mb-2">{t.icon}</span>
                    <span className="text-clinical-xs font-semibold block">{t.name}</span>
                  </button>
                ))}
              </div>
              <div className="flex justify-between mt-6">
                <button onClick={()=>setActiveSection(6)} className="px-4 py-2 rounded-lg border border-border text-clinical-sm font-medium">← Anterior</button>
                <button onClick={()=>setActiveSection(8)} className="px-4 py-2 rounded-lg bg-primary text-white text-clinical-sm font-semibold flex items-center gap-1">Următorul <ChevronRight className="w-4 h-4"/></button>
              </div>
            </div>
          )}

          {/* SECTION I — Semnătură */}
          {activeSection === 8 && (
            <div className="bg-card rounded-xl border border-border shadow-sm p-6">
              <h2 className="text-clinical-md font-semibold mb-4 flex items-center gap-2"><Lock className="w-5 h-5"/>I — Semnătură Digitală & Audit</h2>
              <div className="bg-muted/30 rounded-xl p-5 mb-6 space-y-3">
                <div className="flex items-center justify-between">
                  <span className="text-clinical-sm text-muted-foreground">Completare secțiuni</span>
                  <span className="font-clinical text-clinical-sm font-semibold">{completedSections.size}/9</span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-clinical-sm text-muted-foreground">Diagnostic primar</span>
                  <span className="text-clinical-sm font-semibold">{diagnoses.find(d=>d.primary)?.code} — {diagnoses.find(d=>d.primary)?.name}</span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-clinical-sm text-muted-foreground">Tratament</span>
                  <span className="text-clinical-sm">{medications.length} medicamente · Follow-up: 29.06.2026</span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-clinical-sm text-muted-foreground">Medic</span>
                  <span className="text-clinical-sm font-semibold">Dr. Alexandru Popescu · Lic. #12345 · Oftalmolog</span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-clinical-sm text-muted-foreground">Data/Ora</span>
                  <span className="font-clinical text-clinical-sm">29.03.2026 · 14:32</span>
                </div>
              </div>
              {!signed ? (
                <button onClick={handleSign} className="w-full py-3 rounded-xl bg-primary text-white text-clinical-base font-bold flex items-center justify-center gap-2 hover:opacity-90 transition-opacity">
                  <Lock className="w-5 h-5"/> Semnează Digital & Finalizează Consultația
                </button>
              ) : (
                <div className="space-y-3">
                  <div className="p-3 rounded-lg bg-green-50 border border-green-200 text-green-800 text-clinical-sm font-semibold flex items-center gap-2">
                    <Check className="w-5 h-5"/> Consultație finalizată și semnată digital
                  </div>
                  <div className="flex gap-2">
                    <button className="flex-1 py-2 rounded-lg border border-border text-clinical-sm font-medium flex items-center justify-center gap-1 hover:bg-muted"><FileText className="w-4 h-4"/>Scrisoare Medicală PDF</button>
                    <button className="flex-1 py-2 rounded-lg border border-border text-clinical-sm font-medium flex items-center justify-center gap-1 hover:bg-muted"><FileText className="w-4 h-4"/>Referire Specialist PDF</button>
                    <button className="flex-1 py-2 rounded-lg border border-border text-clinical-sm font-medium flex items-center justify-center gap-1 hover:bg-muted"><FileText className="w-4 h-4"/>Cerere Investigație PDF</button>
                  </div>
                </div>
              )}
            </div>
          )}

          </fieldset>
        </div>
      </div>
    </AppLayout>
  );
};

export default ConsultationPage;
