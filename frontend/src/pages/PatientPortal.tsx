import React, { useState } from 'react';
import { CalendarDays, FolderOpen, FileText, Package, MessageSquare, Settings, LogOut, ChevronRight, ChevronLeft, Download, Send, Star } from 'lucide-react';
import OphthaLogo from '@/components/OphthaLogo';
import { useApp } from '@/contexts/AppContext';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ReferenceLine, ResponsiveContainer } from 'recharts';

const portalNav = [
  { label: 'Programări', icon: CalendarDays, key: 'appointments' },
  { label: 'Dosarul meu', icon: FolderOpen, key: 'records' },
  { label: 'Rețete', icon: FileText, key: 'prescriptions' },
  { label: 'Comenzi', icon: Package, key: 'orders' },
  { label: 'Mesaje', icon: MessageSquare, key: 'messages' },
  { label: 'Setări', icon: Settings, key: 'settings' },
];

const vaData = [{m:'Apr 25',va:0.67},{m:'Iul 25',va:0.67},{m:'Oct 25',va:0.5},{m:'Ian 26',va:0.5}];
const iopData = [{m:'Apr 25',od:24,os:20},{m:'Iul 25',od:23,os:20},{m:'Oct 25',od:24,os:21},{m:'Ian 26',od:26,os:22}];

const bookingServices = [
  {icon:'👁',label:'Consultație',desc:'Examinare completă'},
  {icon:'🔬',label:'Investigație',desc:'OCT, câmp vizual'},
  {icon:'👓',label:'Montaj ochelari',desc:'Ajustare și livrare'},
  {icon:'💬',label:'Telemedicină',desc:'Consultație video'},
];

const PatientPortal: React.FC = () => {
  const { logout, setRole } = useApp();
  const [activeTab, setActiveTab] = useState('appointments');
  const [bookingStep, setBookingStep] = useState(0);
  const [showBooking, setShowBooking] = useState(false);
  const [messageInput, setMessageInput] = useState('');
  const messages = [
    { from: 'patient', text: 'Bună ziua, am observat o ușoară încețoșare la ochiul drept.', time: '27.03.2026 10:15' },
    { from: 'doctor', text: 'Bună ziua. Vom verifica la controlul de mâine. Vă rog să nu conduceți dacă observați deteriorare.', time: '27.03.2026 14:30', doctor: 'Dr. Popescu' },
  ];

  return (
    <div className="min-h-screen bg-background">
      <header className="bg-card border-b border-border shadow-sm">
        <div className="max-w-[860px] mx-auto flex items-center justify-between h-14 px-4">
          <OphthaLogo size={28} />
          <nav className="flex items-center gap-1">
            {portalNav.map(item => (
              <button key={item.key} onClick={() => {setActiveTab(item.key); setShowBooking(false);}}
                className={`px-3 py-2 rounded-md text-[13px] font-medium transition-colors ${activeTab === item.key ? 'text-primary bg-primary/10 font-semibold' : 'text-muted-foreground hover:text-foreground hover:bg-muted'}`}>
                {item.label}
              </button>
            ))}
          </nav>
          <div className="flex items-center gap-2">
            <button onClick={() => setRole('doctor')} className="text-clinical-xs text-muted-foreground hover:text-primary">← Staff</button>
            <button onClick={logout} className="w-8 h-8 flex items-center justify-center text-muted-foreground hover:text-red-500"><LogOut className="w-4 h-4" /></button>
          </div>
        </div>
      </header>

      <div className="max-w-[860px] mx-auto px-4 py-8">
        <div className="mb-8">
          <h1 className="text-clinical-xl font-bold">Bună ziua, Dl. Marinescu!</h1>
          <p className="text-clinical-base text-muted-foreground mt-1">Următoarea programare: <strong>Mâine, 09:00</strong> — Follow-up Glaucom</p>
        </div>

        {/* PROGRAMĂRI */}
        {activeTab === 'appointments' && !showBooking && (
          <div className="space-y-4">
            <h2 className="text-clinical-lg font-semibold">Programările mele</h2>
            {[{date:'30.03.2026',time:'09:00',type:'Follow-up Glaucom',doctor:'Dr. Popescu',status:'Confirmat'},
              {date:'15.04.2026',time:'09:00',type:'OCT Disc Optic',doctor:'Dr. Mihailescu',status:'Programat'}].map((apt,i) => (
              <div key={i} className="bg-card rounded-xl border border-border shadow-sm p-5 hover:shadow-md transition-shadow">
                <div className="flex items-center justify-between">
                  <div><p className="text-clinical-base font-semibold">{apt.type}</p><p className="text-clinical-sm text-muted-foreground mt-1">{apt.doctor} · {apt.date} la {apt.time}</p></div>
                  <div className="flex items-center gap-2">
                    <span className="px-3 py-1 rounded-full text-[12px] font-semibold bg-green-100 text-green-700">{apt.status}</span>
                    <button className="text-clinical-xs text-primary hover:underline">Modifică</button>
                  </div>
                </div>
              </div>
            ))}
            <button onClick={() => {setShowBooking(true); setBookingStep(0);}} className="w-full py-3 rounded-lg border-2 border-dashed border-primary/30 text-clinical-sm font-semibold text-primary hover:bg-primary/5">
              + Solicită programare nouă
            </button>
          </div>
        )}

        {/* BOOKING FLOW */}
        {activeTab === 'appointments' && showBooking && (
          <div className="bg-card rounded-xl border border-border shadow-sm p-6">
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-clinical-lg font-semibold">Rezervă programare</h2>
              <button onClick={() => setShowBooking(false)} className="text-clinical-xs text-muted-foreground hover:text-foreground">✕ Anulează</button>
            </div>
            {/* Progress */}
            <div className="flex items-center gap-2 mb-6">
              {['Serviciu','Medic','Data & Ora','Confirmare'].map((step,i) => (
                <React.Fragment key={step}>
                  <div className={`flex items-center gap-1 ${i <= bookingStep ? 'text-primary' : 'text-muted-foreground'}`}>
                    <div className={`w-6 h-6 rounded-full flex items-center justify-center text-[10px] font-bold ${i <= bookingStep ? 'bg-primary text-white' : 'bg-muted'}`}>{i < bookingStep ? '✓' : i+1}</div>
                    <span className="text-clinical-xs font-medium">{step}</span>
                  </div>
                  {i < 3 && <div className={`flex-1 h-0.5 ${i < bookingStep ? 'bg-primary' : 'bg-muted'}`}/>}
                </React.Fragment>
              ))}
            </div>
            {bookingStep === 0 && (
              <div className="grid grid-cols-2 gap-3">
                {bookingServices.map((s,i) => (
                  <button key={i} onClick={() => setBookingStep(1)} className="p-4 rounded-xl border border-border hover:border-primary hover:shadow-md text-left transition-all">
                    <span className="text-2xl block mb-2">{s.icon}</span>
                    <span className="text-clinical-sm font-semibold block">{s.label}</span>
                    <span className="text-clinical-xs text-muted-foreground">{s.desc}</span>
                  </button>
                ))}
              </div>
            )}
            {bookingStep === 1 && (
              <div className="space-y-3">
                {[{name:'Dr. Alexandru Popescu',spec:'Oftalmolog',rating:4.8},{name:'Dr. Ioana Mihailescu',spec:'Oftalmolog',rating:4.9}].map((d,i) => (
                  <button key={i} onClick={() => setBookingStep(2)} className="w-full p-4 rounded-xl border border-border hover:border-primary text-left flex items-center gap-4 transition-all">
                    <div className="w-12 h-12 rounded-full bg-primary/10 flex items-center justify-center text-primary font-bold">{d.name.split(' ').map(w=>w[0]).join('').slice(0,2)}</div>
                    <div><p className="text-clinical-sm font-semibold">{d.name}</p><p className="text-clinical-xs text-muted-foreground">{d.spec}</p></div>
                    <div className="ml-auto flex items-center gap-1 text-amber-500"><Star className="w-4 h-4 fill-amber-400"/><span className="text-clinical-sm font-semibold">{d.rating}</span></div>
                  </button>
                ))}
              </div>
            )}
            {bookingStep === 2 && (
              <div className="text-center py-8">
                <p className="text-clinical-sm text-muted-foreground mb-4">Selectați data și ora disponibilă</p>
                <input type="date" className="rounded-lg border border-border px-4 py-2 text-clinical-sm mb-4"/>
                <div className="flex flex-wrap gap-2 justify-center">
                  {['09:00','09:30','10:00','10:30','11:00','14:00','14:30','15:00'].map(t => (
                    <button key={t} onClick={() => setBookingStep(3)} className="px-4 py-2 rounded-lg border border-border text-clinical-sm hover:bg-primary hover:text-white transition-colors">{t}</button>
                  ))}
                </div>
              </div>
            )}
            {bookingStep === 3 && (
              <div className="text-center py-6">
                <div className="w-16 h-16 mx-auto mb-4 rounded-full bg-green-100 flex items-center justify-center text-green-600 text-2xl">✓</div>
                <h3 className="text-clinical-md font-semibold mb-2">Programare confirmată!</h3>
                <p className="text-clinical-sm text-muted-foreground mb-4">Follow-up Glaucom · Dr. Popescu · 05.04.2026 la 10:00</p>
                <button onClick={() => setShowBooking(false)} className="px-6 py-2 rounded-lg bg-primary text-white text-clinical-sm font-semibold">Închide</button>
              </div>
            )}
            {bookingStep > 0 && bookingStep < 3 && (
              <button onClick={() => setBookingStep(bookingStep-1)} className="mt-4 flex items-center gap-1 text-clinical-sm text-muted-foreground hover:text-foreground">
                <ChevronLeft className="w-4 h-4"/>Înapoi
              </button>
            )}
          </div>
        )}

        {/* DOSARUL MEU */}
        {activeTab === 'records' && (
          <div className="space-y-6">
            <h2 className="text-clinical-lg font-semibold">Dosarul meu medical</h2>
            <div className="grid grid-cols-2 gap-4">
              <div className="bg-card rounded-xl border border-border shadow-sm p-4">
                <h3 className="text-clinical-sm font-semibold mb-3">IOP Trend</h3>
                <ResponsiveContainer width="100%" height={180}>
                  <LineChart data={iopData}><CartesianGrid strokeDasharray="3 3" stroke="hsl(var(--border))"/>
                    <XAxis dataKey="m" tick={{fontSize:10}}/><YAxis tick={{fontSize:10}} domain={[10,30]}/>
                    <Tooltip/><ReferenceLine y={21} stroke="hsl(var(--color-danger))" strokeDasharray="3 3" label="21"/>
                    <Line type="monotone" dataKey="od" stroke="#EF4444" name="OD" strokeWidth={2}/>
                    <Line type="monotone" dataKey="os" stroke="#3B82F6" name="OS" strokeWidth={2}/>
                  </LineChart>
                </ResponsiveContainer>
              </div>
              <div className="bg-card rounded-xl border border-border shadow-sm p-4">
                <h3 className="text-clinical-sm font-semibold mb-3">Acuitate Vizuală</h3>
                <ResponsiveContainer width="100%" height={180}>
                  <LineChart data={vaData}><CartesianGrid strokeDasharray="3 3" stroke="hsl(var(--border))"/>
                    <XAxis dataKey="m" tick={{fontSize:10}}/><YAxis tick={{fontSize:10}} domain={[0,1]}/>
                    <Tooltip/><Line type="monotone" dataKey="va" stroke="hsl(var(--color-primary-500))" name="VA" strokeWidth={2}/>
                  </LineChart>
                </ResponsiveContainer>
              </div>
            </div>
            <div className="space-y-3">
              {[{date:'15.01.2026',doctor:'Dr. Popescu',diag:'Glaucom cu unghi deschis'},{date:'10.10.2025',doctor:'Dr. Popescu',diag:'Glaucom cu unghi deschis'}].map((c,i) => (
                <div key={i} className="bg-card rounded-xl border border-border shadow-sm p-4 flex items-center justify-between">
                  <div><p className="text-clinical-sm font-semibold">{c.date} — {c.doctor}</p><p className="text-clinical-xs text-muted-foreground">{c.diag}</p></div>
                  <button className="text-clinical-xs text-primary flex items-center gap-1"><Download className="w-3 h-3"/>PDF</button>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* REȚETE */}
        {activeTab === 'prescriptions' && (
          <div className="space-y-4">
            <h2 className="text-clinical-lg font-semibold">Rețetele mele</h2>
            <div className="bg-card rounded-xl border border-border shadow-sm p-6">
              <div className="flex items-center justify-between mb-4">
                <div><h3 className="text-clinical-md font-semibold">REC-20260115-012</h3><p className="text-clinical-xs text-muted-foreground">Emisă: 15.01.2026 · Valabilă: 15.01.2027</p></div>
                <span className="px-3 py-1 rounded-full text-[12px] font-semibold bg-green-100 text-green-700">ACTIVĂ</span>
              </div>
              <div className="grid grid-cols-2 gap-4 mb-4">
                <div className="p-4 rounded-xl border-2 border-red-200 bg-red-50/30">
                  <p className="text-clinical-xs font-semibold text-red-600 clinical-label mb-2">Ochi Drept</p>
                  <p className="font-clinical text-clinical-sm">Sferă: −1.50D · Cilindru: −0.75D · Ax: 90°</p>
                  <p className="font-clinical text-clinical-xs text-muted-foreground mt-1">Add: +2.00D</p>
                </div>
                <div className="p-4 rounded-xl border-2 border-blue-200 bg-blue-50/30">
                  <p className="text-clinical-xs font-semibold text-blue-600 clinical-label mb-2">Ochi Stâng</p>
                  <p className="font-clinical text-clinical-sm">Sferă: −1.25D · Cilindru: −0.50D · Ax: 85°</p>
                  <p className="font-clinical text-clinical-xs text-muted-foreground mt-1">Add: +2.00D</p>
                </div>
              </div>
              <p className="text-clinical-xs text-muted-foreground mb-3">Distanță pupilară: <span className="font-clinical">63.5mm</span></p>
              {/* Validity bar */}
              <div className="mb-4">
                <div className="flex items-center justify-between text-clinical-xs mb-1">
                  <span>Valabilitate</span><span className="text-green-600 font-semibold">10 luni rămase</span>
                </div>
                <div className="h-2 rounded-full bg-muted"><div className="h-full rounded-full bg-green-500" style={{width:'83%'}}/></div>
              </div>
              <div className="flex gap-2">
                <button className="flex-1 py-2 rounded-lg border border-border text-clinical-sm font-medium flex items-center justify-center gap-1 hover:bg-muted"><Download className="w-4 h-4"/>Descarcă PDF</button>
                <button className="flex-1 py-2 rounded-lg text-clinical-sm font-semibold text-white flex items-center justify-center gap-1" style={{background:'hsl(var(--color-accent-500))'}}>Comandă ochelari</button>
              </div>
              <div className="mt-4 flex justify-end">
                <div className="w-14 h-14 bg-foreground/5 border border-border rounded-md flex items-center justify-center text-[8px] text-muted-foreground font-clinical">QR</div>
              </div>
            </div>
          </div>
        )}

        {/* COMENZI */}
        {activeTab === 'orders' && (
          <div className="space-y-4">
            <h2 className="text-clinical-lg font-semibold">Comenzile mele</h2>
            <div className="bg-card rounded-xl border border-border shadow-sm p-6">
              <div className="flex items-center justify-between mb-4">
                <div><p className="text-clinical-sm font-semibold">ORD-2026-0041</p><p className="text-clinical-xs text-muted-foreground">Progresive Varilux Comfort + Silhouette SPX</p></div>
                <span className="px-3 py-1 rounded-full text-[12px] font-semibold bg-amber-100 text-amber-700">La laborator</span>
              </div>
              <div className="flex items-center gap-2 mb-4">
                {[{label:'Comandă plasată',done:true,date:'20.03'},{label:'La laborator',done:true,date:'21.03'},{label:'Gata de montaj',done:false},{label:'Livrat',done:false}].map((step,i) => (
                  <React.Fragment key={step.label}>
                    <div className="text-center">
                      <div className={`w-8 h-8 rounded-full flex items-center justify-center text-[10px] font-bold mx-auto mb-1 ${step.done?'bg-primary text-white':'bg-muted text-muted-foreground'}`}>{step.done?'✓':i+1}</div>
                      <p className="text-[10px] font-medium">{step.label}</p>
                      {step.date && <p className="text-[9px] text-muted-foreground">{step.date}</p>}
                    </div>
                    {i < 3 && <div className={`flex-1 h-0.5 mt-[-12px] ${step.done && i < 1 ? 'bg-primary' : 'bg-muted'}`}/>}
                  </React.Fragment>
                ))}
              </div>
              <p className="text-clinical-xs text-muted-foreground">Estimare livrare: <strong>28.03.2026</strong></p>
            </div>
          </div>
        )}

        {/* MESAJE */}
        {activeTab === 'messages' && (
          <div className="space-y-4">
            <h2 className="text-clinical-lg font-semibold">Mesaje</h2>
            <div className="bg-card rounded-xl border border-border shadow-sm p-6">
              <div className="space-y-3 mb-4 max-h-[400px] overflow-y-auto">
                {messages.map((msg,i) => (
                  <div key={i} className={`flex ${msg.from === 'patient' ? 'justify-end' : 'justify-start'}`}>
                    <div className={`max-w-[70%] p-3 rounded-xl ${msg.from === 'patient' ? 'bg-primary text-white rounded-br-sm' : 'bg-muted rounded-bl-sm'}`}>
                      {msg.from === 'doctor' && <p className="text-[10px] font-semibold mb-1">{(msg as any).doctor}</p>}
                      <p className="text-clinical-sm">{msg.text}</p>
                      <p className={`text-[10px] mt-1 ${msg.from === 'patient' ? 'text-white/70' : 'text-muted-foreground'}`}>{msg.time}</p>
                    </div>
                  </div>
                ))}
              </div>
              <div className="flex gap-2">
                <input value={messageInput} onChange={e => setMessageInput(e.target.value)} placeholder="Scrieți un mesaj..." className="flex-1 rounded-lg border border-border px-3 py-2 text-clinical-sm"/>
                <button className="px-4 py-2 rounded-lg bg-primary text-white"><Send className="w-4 h-4"/></button>
              </div>
            </div>
          </div>
        )}

        {/* SETĂRI */}
        {activeTab === 'settings' && (
          <div className="space-y-6">
            <h2 className="text-clinical-lg font-semibold">Setări & GDPR</h2>
            <div className="bg-card rounded-xl border border-border shadow-sm p-6">
              <h3 className="text-clinical-sm font-semibold mb-3">Consimțăminte</h3>
              {[{name:'Îngrijiri medicale',mandatory:true,on:true},{name:'GDPR',mandatory:true,on:true},{name:'Marketing SMS/Email',mandatory:false,on:true},{name:'Studii clinice',mandatory:false,on:false},{name:'Telemedicină',mandatory:false,on:true}].map((c,i) => (
                <div key={i} className="flex items-center justify-between py-2 border-b border-border last:border-0">
                  <div><p className="text-clinical-sm">{c.name}</p>{c.mandatory && <span className="text-[10px] text-red-600 font-semibold">OBLIGATORIU</span>}</div>
                  <div className={`w-10 h-5 rounded-full transition-colors ${c.on ? 'bg-primary' : 'bg-muted'} ${c.mandatory ? 'opacity-60 cursor-not-allowed' : 'cursor-pointer'}`}>
                    <div className={`w-4 h-4 rounded-full bg-white shadow mt-0.5 transition-transform ${c.on ? 'translate-x-5' : 'translate-x-0.5'}`}/>
                  </div>
                </div>
              ))}
            </div>
            <div className="bg-card rounded-xl border border-border shadow-sm p-6">
              <h3 className="text-clinical-sm font-semibold mb-3">Jurnal acces</h3>
              <table className="w-full text-clinical-xs">
                <thead><tr className="border-b"><th className="text-left p-2">Data</th><th className="text-left p-2">Utilizator</th><th className="text-left p-2">Rol</th><th className="text-left p-2">Acțiune</th></tr></thead>
                <tbody>
                  <tr className="border-b"><td className="p-2">29.03.2026 08:32</td><td className="p-2">Dr. Popescu</td><td className="p-2">Doctor</td><td className="p-2">Vizualizare dosar</td></tr>
                  <tr className="border-b"><td className="p-2">28.03.2026 10:05</td><td className="p-2">Ion Marinescu</td><td className="p-2">Pacient</td><td className="p-2">Acces portal</td></tr>
                </tbody>
              </table>
              <div className="mt-4 flex gap-2">
                <button className="px-3 py-2 rounded-lg border border-border text-clinical-xs font-medium hover:bg-muted flex items-center gap-1"><Download className="w-3 h-3"/>Descarcă jurnal PDF</button>
                <button className="px-3 py-2 rounded-lg border border-border text-clinical-xs font-medium hover:bg-muted flex items-center gap-1"><Download className="w-3 h-3"/>Descarcă dosar complet</button>
                <button className="px-3 py-2 rounded-lg border border-red-200 text-red-600 text-clinical-xs font-medium hover:bg-red-50">Cerere ștergere date</button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default PatientPortal;
