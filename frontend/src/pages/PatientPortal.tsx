import React, { useState, useEffect } from 'react';
import { toast } from 'sonner';
import {
  CalendarDays,
  FolderOpen,
  FileText,
  Package,
  MessageSquare,
  Settings,
  LogOut,
  ChevronLeft,
  Download,
  Send,
  Star,
  CreditCard,
  Video,
  Activity,
  Loader2,
  AlertTriangle,
  Info,
  CheckCircle2,
  Lock,
  Eye,
  Check
} from 'lucide-react';
import OphthaLogo from '@/components/OphthaLogo';
import { useApp } from '@/contexts/AppContext';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ReferenceLine, ResponsiveContainer, Legend } from 'recharts';
import PROQuestionnaires from '@/components/PROQuestionnaires';
import {
  usePortalProfile,
  usePortalAppointments,
  usePortalPrescriptions,
  usePortalPrescriptionDetail,
  usePortalInvestigations,
  usePortalOpticalOrders,
  usePortalNotifications,
  useUpdatePortalConsents
} from '@/hooks/usePortal';

const portalNav = [
  { label: 'Programări', icon: CalendarDays, key: 'appointments' },
  { label: 'Dosarul meu', icon: FolderOpen, key: 'records' },
  { label: 'Rețete', icon: FileText, key: 'prescriptions' },
  { label: 'Comenzi', icon: Package, key: 'orders' },
  { label: 'Plăți', icon: CreditCard, key: 'payments' },
  { label: 'Telemedicină', icon: Video, key: 'telemedicine' },
  { label: 'PRO', icon: Activity, key: 'pro' },
  { label: 'Mesaje', icon: MessageSquare, key: 'messages' },
  { label: 'Setări', icon: Settings, key: 'settings' },
];

const bookingServices = [
  { icon: '👁', label: 'Consultație', desc: 'Examinare completă' },
  { icon: '🔬', label: 'Investigație', desc: 'OCT, câmp vizual' },
  { icon: '👓', label: 'Montaj ochelari', desc: 'Ajustare și livrare' },
  { icon: '💬', label: 'Telemedicină', desc: 'Consultație video' },
];

const formatDate = (isoString: string | undefined) => {
  if (!isoString) return '';
  try {
    const d = new Date(isoString);
    if (isNaN(d.getTime())) return isoString;
    return d.toLocaleDateString('ro-RO', { day: '2-digit', month: '2-digit', year: 'numeric' });
  } catch {
    return isoString;
  }
};

const formatTime = (isoString: string | undefined) => {
  if (!isoString) return '';
  try {
    const d = new Date(isoString);
    if (isNaN(d.getTime())) return isoString;
    return d.toLocaleTimeString('ro-RO', { hour: '2-digit', minute: '2-digit' });
  } catch {
    return isoString;
  }
};

const PatientPortal: React.FC = () => {
  const { logout, setRole } = useApp();
  const [activeTab, setActiveTab] = useState('appointments');
  const [bookingStep, setBookingStep] = useState(0);
  const [showBooking, setShowBooking] = useState(false);
  const [messageInput, setMessageInput] = useState('');
  const [simulatedMessages, setSimulatedMessages] = useState<Array<{ from: 'patient' | 'doctor'; text: string; time: string; doctor?: string }>>([
    { from: 'patient', text: 'Bună ziua, am observat o ușoară încețoșare la ochiul drept.', time: '27.05.2026 10:15' },
    { from: 'doctor', text: 'Bună ziua. Vom verifica la controlul de mâine. Vă rog să nu conduceți dacă observați deteriorare.', time: '27.05.2026 14:30', doctor: 'Dr. Popescu' },
  ]);

  // Consents local state (synchronized with live updates)
  const [consentsState, setConsentsState] = useState({
    dataProcessingConsent: true,
    communicationConsent: true,
    researchConsent: false
  });

  // Access log state (mock but includes live consent logs dynamically)
  const [accessLogs, setAccessLogs] = useState<Array<{ date: string; user: string; role: string; action: string }>>([
    { date: '30.05.2026 08:32', user: 'Dr. Popescu', role: 'Doctor', action: 'Vizualizare dosar' },
    { date: '29.05.2026 10:05', user: 'Pacient Autentificat', role: 'Pacient', action: 'Acces portal' },
  ]);

  // Fetch Portal Data
  const { data: profile, isLoading: isProfileLoading } = usePortalProfile();
  const { data: appointments, isLoading: isAptsLoading } = usePortalAppointments();
  const { data: prescriptions, isLoading: isRxLoading } = usePortalPrescriptions();
  const { data: investigations, isLoading: isInvestigationsLoading } = usePortalInvestigations();
  const { data: opticalOrders, isLoading: isOrdersLoading } = usePortalOpticalOrders();
  const { data: notifications, isLoading: isNotificationsLoading } = usePortalNotifications();
  const updateConsentsMutation = useUpdatePortalConsents();

  // Selected Prescription Detail state
  const [selectedRxId, setSelectedRxId] = useState<string | null>(null);

  // Synchronize initial selected prescription
  useEffect(() => {
    if (prescriptions && prescriptions.length > 0 && !selectedRxId) {
      setSelectedRxId(prescriptions[0].id);
    }
  }, [prescriptions, selectedRxId]);

  const { data: rxDetail, isLoading: isRxDetailLoading } = usePortalPrescriptionDetail(selectedRxId || undefined);

  // Find next upcoming appointment
  const upcomingApt = appointments && appointments.length > 0
    ? appointments.find(apt => new Date(apt.startAt) > new Date())
    : null;

  // Handle GDPR Consents Toggle
  const handleToggleConsent = (key: 'dataProcessingConsent' | 'communicationConsent' | 'researchConsent', isMandatory: boolean) => {
    if (isMandatory) {
      toast.warning('Consimțământul obligatoriu nu poate fi anulat direct din portal.');
      return;
    }

    const newState = {
      ...consentsState,
      [key]: !consentsState[key]
    };
    
    setConsentsState(newState);

    updateConsentsMutation.mutate(newState, {
      onSuccess: () => {
        toast.success('Consimțămintele GDPR au fost actualizate cu succes!');
        
        // Log dynamically to settings access log
        const timestamp = new Date().toLocaleString('ro-RO');
        setAccessLogs(prev => [
          { date: timestamp, user: profile ? `${profile.firstName} ${profile.lastName}` : 'Pacient', role: 'Pacient', action: `Actualizat acord ${key}` },
          ...prev
        ]);
      },
      onError: (err: any) => {
        toast.error('Eroare la actualizarea consimțămintelor: ' + err.message);
        // revert local state
        setConsentsState(consentsState);
      }
    });
  };

  // Simulated messaging
  const handleSendMessage = () => {
    if (!messageInput.trim()) return;
    const timeStr = new Date().toLocaleString('ro-RO').substring(0, 16);
    setSimulatedMessages(prev => [...prev, { from: 'patient', text: messageInput, time: timeStr }]);
    setMessageInput('');
    
    // Simulated doctor response
    setTimeout(() => {
      setSimulatedMessages(prev => [
        ...prev,
        {
          from: 'doctor',
          text: 'Vă mulțumim pentru mesaj. Echipa medicală a fost notificată și vă va răspunde în cel mai scurt timp posibil.',
          time: new Date().toLocaleString('ro-RO').substring(0, 16),
          doctor: 'Asistent Cabinet'
        }
      ]);
      toast.success('Mesaj nou de la clinică!');
    }, 2000);
  };

  // Helper: calculate validity percentage
  const getValidityPercentage = (validUntilStr: string) => {
    try {
      const validUntil = new Date(validUntilStr);
      const now = new Date();
      if (validUntil <= now) return 0;
      
      const diffTime = Math.abs(validUntil.getTime() - now.getTime());
      const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
      return Math.min(100, Math.max(5, Math.ceil((diffDays / 365) * 100)));
    } catch {
      return 50;
    }
  };

  const getRemainingMonthsLabel = (validUntilStr: string) => {
    try {
      const validUntil = new Date(validUntilStr);
      const now = new Date();
      if (validUntil <= now) return 'Expirată';
      
      const diffMonths = (validUntil.getFullYear() - now.getFullYear()) * 12 + (validUntil.getMonth() - now.getMonth());
      if (diffMonths <= 0) {
        const diffDays = Math.ceil((validUntil.getTime() - now.getTime()) / (1000 * 60 * 60 * 24));
        return `${diffDays} zile rămase`;
      }
      return `${diffMonths} luni rămase`;
    } catch {
      return 'Valabilă';
    }
  };

  // Parse IOP Trend values OD/OS from completed investigations
  const parseIopData = (invList: any[]) => {
    if (!invList || invList.length === 0) return [];
    
    const parsed = invList
      .map(inv => {
        const summary = inv.resultSummary || '';
        // Match expressions like: "IOP OD: 18 OS: 17", "IOP OD 24 / OS 20 mmHg", "IOP: OD 24, OS 20"
        const odMatch = summary.match(/IOP\s*OD\s*[:=\s]\s*(\d+)/i) || summary.match(/OD\s*[:=\s]\s*(\d+)\s*mmHg/i);
        const osMatch = summary.match(/IOP\s*OS\s*[:=\s]\s*(\d+)/i) || summary.match(/OS\s*[:=\s]\s*(\d+)\s*mmHg/i);
        
        let odVal = odMatch ? parseInt(odMatch[1]) : null;
        let osVal = osMatch ? parseInt(osMatch[1]) : null;
        
        if ((odVal === null || osVal === null) && /iop|tensiune|ocular/i.test(summary)) {
          const nums = summary.match(/\b(1\d|2\d|3\d)\b/g);
          if (nums && nums.length >= 2) {
            odVal = odVal ?? parseInt(nums[0]);
            osVal = osVal ?? parseInt(nums[1]);
          }
        }
        
        const dateObj = new Date(inv.completedAt || inv.orderedAt);
        const monthLabel = dateObj.toLocaleDateString('ro-RO', { month: 'short', year: '2-digit' });
        
        return {
          m: monthLabel,
          od: odVal,
          os: osVal,
          fullDate: formatDate(inv.completedAt || inv.orderedAt)
        };
      })
      .filter(item => item.od !== null || item.os !== null)
      .reverse(); // Chronological sequence
      
    return parsed;
  };

  // Parse Visual Acuity from completed investigations
  const parseVaData = (invList: any[]) => {
    if (!invList || invList.length === 0) return [];
    
    const parsed = invList
      .map(inv => {
        const summary = inv.resultSummary || '';
        // Match expressions like: "VA: 0.8", "BCVA: OD 0.8 / OS 0.9", "VA OD: 0.8"
        const vaMatch = summary.match(/(?:VA|acuitate|visual acuity|bcva|v\.a\.)\s*[:=\s]\s*(0\.\d+|1\.0|\d)/i) || summary.match(/OD\s*[:=\s]\s*(0\.\d+|1\.0)/i);
        const vaVal = vaMatch ? parseFloat(vaMatch[1]) : null;
        
        const dateObj = new Date(inv.completedAt || inv.orderedAt);
        const monthLabel = dateObj.toLocaleDateString('ro-RO', { month: 'short', year: '2-digit' });
        
        return {
          m: monthLabel,
          va: vaVal,
          fullDate: formatDate(inv.completedAt || inv.orderedAt)
        };
      })
      .filter(item => item.va !== null)
      .reverse(); // Chronological sequence
      
    return parsed;
  };

  // Completed trend data (parsed from database or showing high-fidelity default)
  const realIopData = parseIopData(investigations || []);
  const realVaData = parseVaData(investigations || []);

  const defaultIopData = [
    { m: 'Apr 25', od: 24, os: 20 },
    { m: 'Iul 25', od: 23, os: 20 },
    { m: 'Oct 25', od: 24, os: 21 },
    { m: 'Ian 26', od: 26, os: 22 }
  ];
  
  const defaultVaData = [
    { m: 'Apr 25', va: 0.67 },
    { m: 'Iul 25', va: 0.67 },
    { m: 'Oct 25', va: 0.5 },
    { m: 'Ian 26', va: 0.5 }
  ];

  const chartIopData = realIopData.length > 0 ? realIopData : defaultIopData;
  const chartVaData = realVaData.length > 0 ? realVaData : defaultVaData;

  // Active greeting name
  const patientGreeting = profile
    ? `Bună ziua, ${profile.gender === 'MALE' ? 'Dl.' : profile.gender === 'FEMALE' ? 'Dna.' : ''} ${profile.firstName} ${profile.lastName}!`
    : 'Bună ziua!';

  return (
    <div className="min-h-screen bg-background">
      <header className="bg-card border-b border-border shadow-sm">
        <div className="max-w-[860px] mx-auto flex items-center justify-between h-14 px-4">
          <OphthaLogo size={28} />
          <nav className="flex items-center gap-1">
            {portalNav.map(item => (
              <button
                key={item.key}
                onClick={() => {
                  setActiveTab(item.key);
                  setShowBooking(false);
                }}
                className={`px-3 py-2 rounded-md text-[13px] font-medium transition-colors ${
                  activeTab === item.key
                    ? 'text-primary bg-primary/10 font-semibold'
                    : 'text-muted-foreground hover:text-foreground hover:bg-muted'
                }`}
              >
                {item.label}
              </button>
            ))}
          </nav>
          <div className="flex items-center gap-2">
            <button onClick={() => setRole('doctor')} className="text-clinical-xs text-muted-foreground hover:text-primary">
              ← Staff
            </button>
            <button onClick={logout} className="w-8 h-8 flex items-center justify-center text-muted-foreground hover:text-red-500">
              <LogOut className="w-4 h-4" />
            </button>
          </div>
        </div>
      </header>

      <div className="max-w-[860px] mx-auto px-4 py-8">
        <div className="mb-8">
          {isProfileLoading ? (
            <div className="space-y-2">
              <div className="h-7 w-64 bg-muted animate-pulse rounded-md" />
              <div className="h-4 w-96 bg-muted animate-pulse rounded-md" />
            </div>
          ) : (
            <>
              <h1 className="text-clinical-xl font-bold flex items-center gap-2 text-foreground">
                {patientGreeting}
              </h1>
              <p className="text-clinical-base text-muted-foreground mt-1">
                {upcomingApt ? (
                  <>
                    Următoarea programare: <strong>{formatDate(upcomingApt.startAt)} la {formatTime(upcomingApt.startAt)}</strong>
                    {upcomingApt.chiefComplaint ? ` — ${upcomingApt.chiefComplaint}` : ''}
                  </>
                ) : (
                  'Nicio programare viitoare programată.'
                )}
              </p>
            </>
          )}
        </div>

        {/* PROGRAMĂRI */}
        {activeTab === 'appointments' && !showBooking && (
          <div className="space-y-4">
            <h2 className="text-clinical-lg font-semibold text-foreground">Programările mele</h2>
            
            {isAptsLoading ? (
              <div className="space-y-3">
                <div className="h-24 bg-card rounded-xl border border-border animate-pulse" />
                <div className="h-24 bg-card rounded-xl border border-border animate-pulse" />
              </div>
            ) : appointments && appointments.length > 0 ? (
              appointments.map((apt) => (
                <div key={apt.id} className="bg-card rounded-xl border border-border shadow-sm p-5 hover:shadow-md transition-shadow">
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-clinical-base font-semibold text-foreground">{apt.chiefComplaint || 'Consultație Medicală'}</p>
                      <p className="text-clinical-sm text-muted-foreground mt-1">
                        {apt.doctorName} · {formatDate(apt.startAt)} la {formatTime(apt.startAt)}
                      </p>
                      {apt.room && <p className="text-clinical-xs text-muted-foreground mt-0.5">Cabinet: {apt.room} · Durată: {apt.durationMinutes} min</p>}
                      {apt.patientNotes && <p className="text-clinical-xs text-muted-foreground italic mt-2">"{apt.patientNotes}"</p>}
                    </div>
                    <div className="flex items-center gap-2">
                      <span className={`px-3 py-1 rounded-full text-[12px] font-semibold ${
                        apt.status === 'CONFIRMED' || apt.status === 'CHECKED_IN' || apt.status === 'COMPLETED'
                          ? 'bg-green-100 text-green-700'
                          : apt.status === 'CANCELLED'
                          ? 'bg-red-100 text-red-700'
                          : 'bg-amber-100 text-amber-700'
                      }`}>
                        {apt.status === 'CONFIRMED' ? 'Confirmat' : apt.status === 'COMPLETED' ? 'Finalizată' : apt.status === 'CANCELLED' ? 'Anulată' : 'Programat'}
                      </span>
                    </div>
                  </div>
                </div>
              ))
            ) : (
              <div className="bg-card rounded-xl border border-border p-8 text-center text-muted-foreground">
                Nu aveți programări viitoare înregistrate.
              </div>
            )}
            
            <button
              onClick={() => {
                setShowBooking(true);
                setBookingStep(0);
              }}
              className="w-full py-3 rounded-lg border-2 border-dashed border-primary/30 text-clinical-sm font-semibold text-primary hover:bg-primary/5 transition-colors"
            >
              + Solicită programare nouă
            </button>
          </div>
        )}

        {/* BOOKING FLOW */}
        {activeTab === 'appointments' && showBooking && (
          <div className="bg-card rounded-xl border border-border shadow-sm p-6">
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-clinical-lg font-semibold text-foreground">Rezervă programare</h2>
              <button onClick={() => setShowBooking(false)} className="text-clinical-xs text-muted-foreground hover:text-foreground">
                ✕ Anulează
              </button>
            </div>
            {/* Progress */}
            <div className="flex items-center gap-2 mb-6">
              {['Serviciu', 'Medic', 'Data & Ora', 'Confirmare'].map((step, i) => (
                <React.Fragment key={step}>
                  <div className={`flex items-center gap-1 ${i <= bookingStep ? 'text-primary' : 'text-muted-foreground'}`}>
                    <div className={`w-6 h-6 rounded-full flex items-center justify-center text-[10px] font-bold ${
                      i <= bookingStep ? 'bg-primary text-white' : 'bg-muted'
                    }`}>
                      {i < bookingStep ? '✓' : i + 1}
                    </div>
                    <span className="text-clinical-xs font-medium">{step}</span>
                  </div>
                  {i < 3 && <div className={`flex-1 h-0.5 ${i < bookingStep ? 'bg-primary' : 'bg-muted'}`} />}
                </React.Fragment>
              ))}
            </div>
            {bookingStep === 0 && (
              <div className="grid grid-cols-2 gap-3">
                {bookingServices.map((s, i) => (
                  <button
                    key={i}
                    onClick={() => setBookingStep(1)}
                    className="p-4 rounded-xl border border-border hover:border-primary hover:shadow-md text-left transition-all bg-card"
                  >
                    <span className="text-2xl block mb-2">{s.icon}</span>
                    <span className="text-clinical-sm font-semibold block text-foreground">{s.label}</span>
                    <span className="text-clinical-xs text-muted-foreground">{s.desc}</span>
                  </button>
                ))}
              </div>
            )}
            {bookingStep === 1 && (
              <div className="space-y-3">
                {[
                  { name: 'Dr. Alexandru Popescu', spec: 'Oftalmolog', rating: 4.8 },
                  { name: 'Dr. Ioana Mihailescu', spec: 'Oftalmolog', rating: 4.9 }
                ].map((d, i) => (
                  <button
                    key={i}
                    onClick={() => setBookingStep(2)}
                    className="w-full p-4 rounded-xl border border-border hover:border-primary text-left flex items-center gap-4 transition-all bg-card"
                  >
                    <div className="w-12 h-12 rounded-full bg-primary/10 flex items-center justify-center text-primary font-bold">
                      {d.name.split(' ').map(w => w[0]).join('').slice(0, 2)}
                    </div>
                    <div>
                      <p className="text-clinical-sm font-semibold text-foreground">{d.name}</p>
                      <p className="text-clinical-xs text-muted-foreground">{d.spec}</p>
                    </div>
                    <div className="ml-auto flex items-center gap-1 text-amber-500">
                      <Star className="w-4 h-4 fill-amber-400" />
                      <span className="text-clinical-sm font-semibold">{d.rating}</span>
                    </div>
                  </button>
                ))}
              </div>
            )}
            {bookingStep === 2 && (
              <div className="text-center py-8">
                <p className="text-clinical-sm text-muted-foreground mb-4">Selectați data și ora disponibilă</p>
                <input type="date" className="rounded-lg border border-border px-4 py-2 text-clinical-sm mb-4 bg-card text-foreground" />
                <div className="flex flex-wrap gap-2 justify-center">
                  {['09:00', '09:30', '10:00', '10:30', '11:00', '14:00', '14:30', '15:00'].map(t => (
                    <button
                      key={t}
                      onClick={() => setBookingStep(3)}
                      className="px-4 py-2 rounded-lg border border-border text-clinical-sm hover:bg-primary hover:text-white transition-colors bg-card text-foreground"
                    >
                      {t}
                    </button>
                  ))}
                </div>
              </div>
            )}
            {bookingStep === 3 && (
              <div className="text-center py-6">
                <div className="w-16 h-16 mx-auto mb-4 rounded-full bg-green-100 flex items-center justify-center text-green-600 text-2xl">
                  ✓
                </div>
                <h3 className="text-clinical-md font-semibold mb-2 text-foreground">Programare înregistrată cu succes!</h3>
                <p className="text-clinical-sm text-muted-foreground mb-4">
                  Cererea dvs. a fost trimisă. Veți primi o notificare de îndată ce este aprobată de clinică.
                </p>
                <button
                  onClick={() => setShowBooking(false)}
                  className="px-6 py-2 rounded-lg bg-primary text-white text-clinical-sm font-semibold"
                >
                  Închide
                </button>
              </div>
            )}
            {bookingStep > 0 && bookingStep < 3 && (
              <button
                onClick={() => setBookingStep(bookingStep - 1)}
                className="mt-4 flex items-center gap-1 text-clinical-sm text-muted-foreground hover:text-foreground"
              >
                <ChevronLeft className="w-4 h-4" />Înapoi
              </button>
            )}
          </div>
        )}

        {/* DOSARUL MEU */}
        {activeTab === 'records' && (
          <div className="space-y-6">
            <h2 className="text-clinical-lg font-semibold text-foreground">Dosarul meu medical</h2>
            
            {/* Visual Indicators of Demo / Parsed state */}
            {(realIopData.length === 0 || realVaData.length === 0) && (
              <div className="p-3.5 bg-blue-50 border border-blue-200 text-blue-800 rounded-xl text-clinical-sm flex items-center gap-2.5">
                <Info className="w-4 h-4 shrink-0" />
                <span>Vizualizați date demo de tendințe medicale deoarece nu există date de investigații finalizate încă.</span>
              </div>
            )}

            <div className="grid grid-cols-2 gap-4">
              <div className="bg-card rounded-xl border border-border shadow-sm p-4">
                <h3 className="text-clinical-sm font-semibold mb-3 text-foreground">Evoluție Tensiune Oculară (IOP)</h3>
                <ResponsiveContainer width="100%" height={180}>
                  <LineChart data={chartIopData}>
                    <CartesianGrid strokeDasharray="3 3" stroke="hsl(var(--border))" />
                    <XAxis dataKey="m" tick={{ fontSize: 10 }} />
                    <YAxis tick={{ fontSize: 10 }} domain={[10, 30]} />
                    <Tooltip />
                    <ReferenceLine y={21} stroke="red" strokeDasharray="3 3" label={{ value: '21 mmHg', fontSize: 9, fill: 'red' }} />
                    <Line type="monotone" dataKey="od" stroke="#EF4444" name="OD (Ochi Drept)" strokeWidth={2.5} activeDot={{ r: 6 }} />
                    <Line type="monotone" dataKey="os" stroke="#3B82F6" name="OS (Ochi Stâng)" strokeWidth={2.5} activeDot={{ r: 6 }} />
                    <Legend wrapperStyle={{ fontSize: 10 }} />
                  </LineChart>
                </ResponsiveContainer>
              </div>
              <div className="bg-card rounded-xl border border-border shadow-sm p-4">
                <h3 className="text-clinical-sm font-semibold mb-3 text-foreground">Acuitate Vizuală (BCVA)</h3>
                <ResponsiveContainer width="100%" height={180}>
                  <LineChart data={chartVaData}>
                    <CartesianGrid strokeDasharray="3 3" stroke="hsl(var(--border))" />
                    <XAxis dataKey="m" tick={{ fontSize: 10 }} />
                    <YAxis tick={{ fontSize: 10 }} domain={[0, 1.2]} />
                    <Tooltip />
                    <Line type="monotone" dataKey="va" stroke="hsl(var(--primary))" name="VA (Snellen)" strokeWidth={2.5} activeDot={{ r: 6 }} />
                    <Legend wrapperStyle={{ fontSize: 10 }} />
                  </LineChart>
                </ResponsiveContainer>
              </div>
            </div>

            <div className="space-y-3">
              <h3 className="text-clinical-sm font-semibold text-foreground">Investigații finalizate</h3>
              {isInvestigationsLoading ? (
                <div className="space-y-2">
                  <div className="h-14 bg-card rounded-xl border border-border animate-pulse" />
                  <div className="h-14 bg-card rounded-xl border border-border animate-pulse" />
                </div>
              ) : investigations && investigations.length > 0 ? (
                investigations.map((inv) => (
                  <div key={inv.id} className="bg-card rounded-xl border border-border shadow-sm p-4 flex items-center justify-between">
                    <div>
                      <p className="text-clinical-sm font-semibold text-foreground">
                        {formatDate(inv.completedAt)} — {inv.category}
                      </p>
                      <p className="text-clinical-xs text-muted-foreground mt-0.5">
                        Ordonat de: {inv.orderedByName}
                      </p>
                      {inv.resultSummary && (
                        <p className="text-clinical-xs text-foreground bg-muted/50 p-2 rounded-md mt-2 italic border-l-2 border-primary">
                          Interpretare medic: "{inv.resultSummary}"
                        </p>
                      )}
                    </div>
                    <button
                      onClick={() => toast.success('Descărcare raport investigație PDF...')}
                      className="text-clinical-xs text-primary flex items-center gap-1 font-semibold hover:underline shrink-0"
                    >
                      <Download className="w-3.5 h-3.5" /> PDF
                    </button>
                  </div>
                ))
              ) : (
                <div className="bg-card rounded-xl border border-border p-6 text-center text-muted-foreground text-clinical-sm">
                  Fără investigații medicale finalizate înregistrate.
                </div>
              )}
            </div>
          </div>
        )}

        {/* REȚETE */}
        {activeTab === 'prescriptions' && (
          <div className="space-y-4">
            <h2 className="text-clinical-lg font-semibold text-foreground">Rețetele mele</h2>
            
            {isRxLoading ? (
              <div className="h-64 bg-card rounded-xl border border-border animate-pulse" />
            ) : prescriptions && prescriptions.length > 0 ? (
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                {/* Prescription List Selector */}
                <div className="space-y-2">
                  <p className="text-clinical-xs font-semibold text-muted-foreground uppercase">Alegeți rețetă</p>
                  {prescriptions.map((rx) => (
                    <button
                      key={rx.id}
                      onClick={() => setSelectedRxId(rx.id)}
                      className={`w-full p-4 rounded-xl border text-left transition-all ${
                        selectedRxId === rx.id
                          ? 'border-primary bg-primary/5 shadow-sm'
                          : 'border-border bg-card hover:bg-muted/50'
                      }`}
                    >
                      <p className="text-clinical-sm font-semibold text-foreground">ID: {rx.id.substring(0, 8).toUpperCase()}</p>
                      <p className="text-clinical-xs text-muted-foreground mt-1">Emisă: {formatDate(rx.issuedDate)}</p>
                      <span className={`inline-block mt-2 px-2 py-0.5 rounded-full text-[10px] font-semibold ${
                        rx.status === 'SIGNED' ? 'bg-green-100 text-green-700' : 'bg-muted text-muted-foreground'
                      }`}>
                        {rx.status === 'SIGNED' ? 'ACTIVĂ' : rx.status}
                      </span>
                    </button>
                  ))}
                </div>

                {/* Prescription Detail view */}
                <div className="md:col-span-2">
                  {isRxDetailLoading ? (
                    <div className="h-64 bg-card rounded-xl border border-border animate-pulse" />
                  ) : rxDetail ? (
                    <div className="bg-card rounded-xl border border-border shadow-sm p-6 space-y-4">
                      <div className="flex items-center justify-between border-b border-border pb-4">
                        <div>
                          <h3 className="text-clinical-md font-semibold text-foreground">
                            REȚETĂ MEDICALĂ
                          </h3>
                          <p className="text-clinical-xs text-muted-foreground">
                            Emisă la: {formatDate(rxDetail.issuedDate)} · Valabilă până la: {formatDate(rxDetail.validUntil)}
                          </p>
                          <p className="text-clinical-xs text-primary font-semibold mt-1">
                            Prescris de: {rxDetail.issuedByName}
                          </p>
                        </div>
                        <span className={`px-3 py-1 rounded-full text-[12px] font-semibold ${
                          rxDetail.status === 'SIGNED' ? 'bg-green-100 text-green-700' : 'bg-muted text-muted-foreground'
                        }`}>
                          {rxDetail.status === 'SIGNED' ? 'ACTIVĂ' : rxDetail.status}
                        </span>
                      </div>

                      {/* Optical Lines */}
                      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        {rxDetail.lines && rxDetail.lines.map((line, idx) => (
                          <div
                            key={idx}
                            className={`p-4 rounded-xl border-2 ${
                              line.eye === 'OD'
                                ? 'border-red-100 bg-red-50/20'
                                : 'border-blue-100 bg-blue-50/20'
                            }`}
                          >
                            <p className={`text-clinical-xs font-semibold uppercase tracking-wider mb-2 ${
                              line.eye === 'OD' ? 'text-red-600' : 'text-blue-600'
                            }`}>
                              {line.eye === 'OD' ? 'Ochi Drept (OD)' : 'Ochi Stâng (OS)'}
                            </p>
                            <div className="space-y-1 text-clinical-sm text-foreground">
                              <p>Sferă: <strong className="font-clinical">{line.sphere > 0 ? '+' : ''}{line.sphere.toFixed(2)} D</strong></p>
                              <p>Cilindru: <strong className="font-clinical">{line.cylinder.toFixed(2)} D</strong></p>
                              {line.axis !== undefined && <p>Ax: <strong className="font-clinical">{line.axis}°</strong></p>}
                              {line.add > 0 && <p>Adiție (Add): <strong className="font-clinical">+{line.add.toFixed(2)} D</strong></p>}
                            </div>
                          </div>
                        ))}
                      </div>

                      {/* Pupil Distance */}
                      {rxDetail.lines && rxDetail.lines[0]?.pd && (
                        <p className="text-clinical-xs text-muted-foreground mt-2">
                          Distanță pupilară (PD): <span className="font-clinical font-semibold text-foreground">{rxDetail.lines[0].pd} mm</span>
                        </p>
                      )}

                      {/* Validity Bar */}
                      <div className="pt-2">
                        <div className="flex items-center justify-between text-clinical-xs mb-1.5">
                          <span className="text-muted-foreground">Progres Valabilitate</span>
                          <span className="text-green-600 font-semibold">{getRemainingMonthsLabel(rxDetail.validUntil)}</span>
                        </div>
                        <div className="h-2 rounded-full bg-muted overflow-hidden">
                          <div
                            className="h-full rounded-full bg-green-500 transition-all duration-500"
                            style={{ width: `${getValidityPercentage(rxDetail.validUntil)}%` }}
                          />
                        </div>
                      </div>

                      {/* Actions */}
                      <div className="flex gap-2 pt-2">
                        <button
                          onClick={() => toast.info('Generare PDF rețetă...')}
                          className="flex-1 py-2 rounded-lg border border-border text-clinical-sm font-medium flex items-center justify-center gap-1.5 hover:bg-muted text-foreground transition-colors bg-card"
                        >
                          <Download className="w-4 h-4" /> Descarcă PDF
                        </button>
                        <button
                          onClick={() => {
                            setActiveTab('orders');
                            toast.info('Direcționare către departamentul optic.');
                          }}
                          className="flex-1 py-2 rounded-lg text-clinical-sm font-semibold text-white flex items-center justify-center gap-1.5 hover:opacity-90 transition-opacity bg-primary"
                        >
                          Comandă ochelari
                        </button>
                      </div>

                      {/* QR and Verification Token */}
                      {rxDetail.qrVerificationToken && (
                        <div className="pt-4 border-t border-border flex items-center justify-between">
                          <div className="text-[10px] text-muted-foreground font-clinical">
                            TOKEN SECURIZAT: {rxDetail.qrVerificationToken}
                          </div>
                          <div className="w-14 h-14 bg-foreground/5 border border-border rounded-md flex flex-col items-center justify-center text-[7px] text-muted-foreground font-clinical p-1 shrink-0">
                            <span className="font-bold border-b border-border pb-0.5 mb-1 w-full text-center">QR SECURITY</span>
                            <span>VERIFIED</span>
                          </div>
                        </div>
                      )}
                    </div>
                  ) : (
                    <div className="bg-card rounded-xl border border-border p-8 text-center text-muted-foreground text-clinical-sm">
                      Selectați o rețetă pentru a vizualiza detaliile.
                    </div>
                  )}
                </div>
              </div>
            ) : (
              <div className="bg-card rounded-xl border border-border p-8 text-center text-muted-foreground text-clinical-sm">
                Nu aveți nicio rețetă înregistrată.
              </div>
            )}
          </div>
        )}

        {/* COMENZI */}
        {activeTab === 'orders' && (
          <div className="space-y-4">
            <h2 className="text-clinical-lg font-semibold text-foreground">Comenzile mele</h2>
            
            {isOrdersLoading ? (
              <div className="h-48 bg-card rounded-xl border border-border animate-pulse animate-pulse" />
            ) : opticalOrders && opticalOrders.length > 0 ? (
              opticalOrders.map((order) => {
                // Determine step states
                const isSent = ['SENT_TO_LAB', 'QC_CHECK', 'READY_FOR_FITTING', 'COMPLETED'].includes(order.stage);
                const isReady = ['READY_FOR_FITTING', 'COMPLETED'].includes(order.stage);
                const isCompleted = order.stage === 'COMPLETED';

                return (
                  <div key={order.id} className="bg-card rounded-xl border border-border shadow-sm p-6 space-y-4">
                    <div className="flex items-center justify-between border-b border-border pb-3">
                      <div>
                        <p className="text-clinical-sm font-semibold text-foreground">Cod Comandă: {order.orderNumber}</p>
                        <p className="text-clinical-xs text-muted-foreground mt-0.5">Tip: {order.orderType === 'GLASSES' ? 'Ochelari dioptrii' : order.orderType}</p>
                      </div>
                      <span className={`px-3 py-1 rounded-full text-[12px] font-semibold ${
                        order.stage === 'COMPLETED'
                          ? 'bg-green-100 text-green-700'
                          : order.stage === 'CANCELLED'
                          ? 'bg-red-100 text-red-700'
                          : 'bg-amber-100 text-amber-700'
                      }`}>
                        {order.stage === 'SENT_TO_LAB'
                          ? 'La laborator'
                          : order.stage === 'QC_CHECK'
                          ? 'Control Calitate'
                          : order.stage === 'READY_FOR_FITTING'
                          ? 'Pregătit pentru montaj'
                          : order.stage === 'COMPLETED'
                          ? 'Finalizată'
                          : order.stage}
                      </span>
                    </div>

                    {/* Stage Timeline */}
                    <div className="flex items-center justify-between text-center pt-2 max-w-lg mx-auto">
                      {[
                        { label: 'Plasată', done: true },
                        { label: 'Laborator', done: isSent },
                        { label: 'Montaj', done: isReady },
                        { label: 'Livrată', done: isCompleted }
                      ].map((step, i, arr) => (
                        <React.Fragment key={step.label}>
                          <div className="relative z-10">
                            <div className={`w-8 h-8 rounded-full flex items-center justify-center text-[10px] font-bold mx-auto mb-1.5 transition-colors ${
                              step.done ? 'bg-primary text-white' : 'bg-muted text-muted-foreground'
                            }`}>
                              {step.done ? '✓' : i + 1}
                            </div>
                            <p className="text-[10px] font-semibold text-foreground">{step.label}</p>
                          </div>
                          {i < arr.length - 1 && (
                            <div className={`flex-1 h-0.5 mx-2 -mt-4 transition-colors ${
                              arr[i + 1].done ? 'bg-primary' : 'bg-muted'
                            }`} />
                          )}
                        </React.Fragment>
                      ))}
                    </div>

                    <div className="pt-2 text-clinical-xs text-muted-foreground flex justify-between items-center">
                      <span>Înregistrată la: {formatDate(order.createdAt)}</span>
                      <span>Estimare livrare: <strong>{formatDate(new Date(new Date(order.createdAt).getTime() + 7 * 24 * 60 * 60 * 1000).toISOString())}</strong></span>
                    </div>
                  </div>
                );
              })
            ) : (
              <div className="bg-card rounded-xl border border-border p-8 text-center text-muted-foreground text-clinical-sm">
                Nu aveți comenzi optice înregistrate.
              </div>
            )}
          </div>
        )}

        {/* MESAJE */}
        {activeTab === 'messages' && (
          <div className="space-y-4">
            <h2 className="text-clinical-lg font-semibold text-foreground">Mesaje & Notificări clinică</h2>
            
            <div className="bg-card rounded-xl border border-border shadow-sm p-6">
              <div className="space-y-4 mb-4 max-h-[400px] overflow-y-auto pr-2">
                {/* Real incoming notification logs */}
                {isNotificationsLoading ? (
                  <div className="space-y-3">
                    <div className="h-16 bg-muted animate-pulse rounded-xl" />
                    <div className="h-16 bg-muted animate-pulse rounded-xl" />
                  </div>
                ) : notifications && notifications.length > 0 ? (
                  notifications.map((notif) => (
                    <div key={notif.id} className="flex justify-start">
                      <div className="max-w-[85%] p-3.5 rounded-xl bg-muted rounded-bl-sm border border-border/50 shadow-xs">
                        <p className="text-[9px] font-semibold mb-1 text-primary uppercase tracking-wide">
                          {notif.channel} · Clinică
                        </p>
                        <p className="text-clinical-sm font-semibold text-foreground mb-0.5">{notif.subject}</p>
                        <p className="text-clinical-sm text-muted-foreground whitespace-pre-wrap">{notif.bodyPreview}</p>
                        <p className="text-[10px] mt-1.5 text-muted-foreground/80 font-clinical">
                          {formatDate(notif.sentAt)} la {formatTime(notif.sentAt)}
                        </p>
                      </div>
                    </div>
                  ))
                ) : null}

                {/* Simulated live chat */}
                {simulatedMessages.map((msg, i) => (
                  <div key={i} className={`flex ${msg.from === 'patient' ? 'justify-end' : 'justify-start'}`}>
                    <div className={`max-w-[75%] p-3.5 rounded-xl shadow-xs ${
                      msg.from === 'patient'
                        ? 'bg-primary text-white rounded-br-sm'
                        : 'bg-muted rounded-bl-sm border border-border/50'
                    }`}>
                      {msg.from === 'doctor' && (
                        <p className="text-[9px] font-semibold mb-1 text-primary uppercase tracking-wide">
                          {msg.doctor || 'Dr. Popescu'}
                        </p>
                      )}
                      <p className="text-clinical-sm leading-relaxed">{msg.text}</p>
                      <p className={`text-[9px] mt-1.5 font-clinical ${
                        msg.from === 'patient' ? 'text-white/80' : 'text-muted-foreground/80'
                      }`}>
                        {msg.time}
                      </p>
                    </div>
                  </div>
                ))}
              </div>
              
              <div className="flex gap-2 border-t border-border pt-4">
                <input
                  value={messageInput}
                  onChange={e => setMessageInput(e.target.value)}
                  onKeyDown={e => {
                    if (e.key === 'Enter') handleSendMessage();
                  }}
                  placeholder="Scrieți un mesaj către cabinetul medical..."
                  className="flex-1 rounded-lg border border-border px-3 py-2 text-clinical-sm bg-card text-foreground focus:outline-primary"
                />
                <button
                  onClick={handleSendMessage}
                  className="px-4 py-2 rounded-lg bg-primary text-white hover:opacity-90 transition-opacity flex items-center justify-center"
                >
                  <Send className="w-4 h-4" />
                </button>
              </div>
            </div>
          </div>
        )}

        {/* PLĂȚI */}
        {activeTab === 'payments' && (
          <div className="space-y-6">
            <h2 className="text-clinical-lg font-semibold text-foreground">Plăți & Facturi</h2>
            <div className="bg-card rounded-xl border border-border shadow-sm p-6 flex items-center justify-between">
              <div>
                <p className="text-clinical-xs text-muted-foreground uppercase tracking-wider">Sold restant</p>
                <p className="text-clinical-xl font-bold font-clinical text-amber-600 mt-1">
                  {opticalOrders && opticalOrders.length > 0 ? (opticalOrders.length * 450).toLocaleString('ro-RO') : '0'} RON
                </p>
                <p className="text-clinical-xs text-muted-foreground">
                  Calculat pe baza a {opticalOrders?.length || 0} comenzi optice înregistrate în sistem.
                </p>
              </div>
              <button
                onClick={() => toast.success('Redirecționare către procesatorul securizat de plăți...')}
                className="px-5 py-2.5 rounded-lg bg-primary text-primary-foreground text-clinical-sm font-semibold flex items-center gap-2 hover:opacity-90"
              >
                <CreditCard className="w-4 h-4" /> Plătește online
              </button>
            </div>
            <div className="bg-card rounded-xl border border-border shadow-sm overflow-hidden">
              <div className="p-4 border-b border-border">
                <h3 className="text-clinical-sm font-semibold text-foreground">Istoric documente de plată</h3>
              </div>
              <table className="w-full text-clinical-sm text-left">
                <thead>
                  <tr className="bg-muted border-b border-border">
                    <th className="p-3 text-clinical-xs text-muted-foreground font-semibold uppercase">Data</th>
                    <th className="p-3 text-clinical-xs text-muted-foreground font-semibold uppercase">Document</th>
                    <th className="p-3 text-clinical-xs text-muted-foreground font-semibold uppercase">Descriere</th>
                    <th className="p-3 text-clinical-xs text-muted-foreground font-semibold uppercase text-right">Sumă</th>
                    <th className="p-3 text-clinical-xs text-muted-foreground font-semibold uppercase">Status</th>
                    <th className="p-3"></th>
                  </tr>
                </thead>
                <tbody>
                  {[
                    { d: '15.05.2026', doc: 'INV-2026-0123', desc: 'Consultație + OCT', amt: 280, st: 'Plătit' },
                    { d: '10.04.2026', doc: 'INV-2026-0092', desc: 'Follow-up Glaucom', amt: 150, st: 'Plătit' },
                  ].map((r, i) => (
                    <tr key={i} className="border-b border-border hover:bg-muted/30">
                      <td className="p-3 font-clinical text-clinical-xs text-foreground">{r.d}</td>
                      <td className="p-3 font-clinical text-primary font-semibold">{r.doc}</td>
                      <td className="p-3 text-foreground">{r.desc}</td>
                      <td className="p-3 text-right font-clinical font-semibold text-foreground">{r.amt} RON</td>
                      <td className="p-3">
                        <span className="px-2 py-0.5 rounded-full text-[10px] font-semibold bg-green-100 text-green-700">
                          {r.st}
                        </span>
                      </td>
                      <td className="p-3 text-right">
                        <button
                          onClick={() => toast.info('Descărcare factură PDF...')}
                          className="text-primary hover:underline text-clinical-xs flex items-center gap-1 ml-auto font-semibold"
                        >
                          <Download className="w-3 h-3" /> PDF
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* TELEMEDICINĂ */}
        {activeTab === 'telemedicine' && (
          <div className="space-y-6">
            <h2 className="text-clinical-lg font-semibold text-foreground">Sesiuni Telemedicină</h2>
            <div className="bg-card rounded-xl border border-border shadow-sm p-6">
              <div className="flex items-center justify-between mb-4">
                <div>
                  <p className="text-clinical-base font-semibold text-foreground">Consultație video — Follow-up</p>
                  <p className="text-clinical-sm text-muted-foreground mt-1">
                    Dr. Alexandru Popescu · Mâine, 14:00 (durată 20 min)
                  </p>
                </div>
                <button
                  onClick={() => toast.info('Sesiunea va fi disponibilă cu 10 min înainte de programare.')}
                  className="px-5 py-2.5 rounded-lg bg-primary text-primary-foreground text-clinical-sm font-semibold flex items-center gap-2 opacity-60"
                  disabled
                >
                  <Video className="w-4 h-4" /> Join
                </button>
              </div>
              <div className="aspect-video bg-gradient-to-br from-slate-800 to-slate-900 rounded-xl flex items-center justify-center relative overflow-hidden shadow-inner">
                <div className="text-center text-white p-4">
                  <Video className="w-12 h-12 mx-auto mb-2 opacity-40 animate-pulse" />
                  <p className="text-clinical-sm opacity-80 font-semibold">Cameră video oprită</p>
                  <p className="text-clinical-xs opacity-60 mt-1">Sesiunea video va porni automat la ora programată.</p>
                </div>
                <div className="absolute bottom-4 right-4 w-32 h-24 bg-slate-700/80 rounded-lg border-2 border-slate-600 flex items-center justify-center text-white/60 text-clinical-xs backdrop-blur-xs">
                  Pacient
                </div>
                <div className="absolute bottom-4 left-1/2 -translate-x-1/2 flex gap-2">
                  <button className="w-10 h-10 rounded-full bg-white/10 text-white hover:bg-white/20 flex items-center justify-center backdrop-blur transition-colors">
                    🎤
                  </button>
                  <button className="w-10 h-10 rounded-full bg-white/10 text-white hover:bg-white/20 flex items-center justify-center backdrop-blur transition-colors">
                    📹
                  </button>
                  <button
                    onClick={() => toast.info('Sesiune deconectată')}
                    className="w-10 h-10 rounded-full bg-red-500 text-white hover:bg-red-600 flex items-center justify-center transition-colors"
                  >
                    📞
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* PRO */}
        {activeTab === 'pro' && (
          <div className="space-y-4">
            <h2 className="text-clinical-lg font-semibold text-foreground">Chestionare PRO (Patient-Reported Outcomes)</h2>
            <p className="text-clinical-sm text-muted-foreground">
              Răspunsurile dvs. ajută echipa clinică să monitorizeze precis progresul tratamentelor.
            </p>
            <PROQuestionnaires />
          </div>
        )}

        {/* SETĂRI & GDPR */}
        {activeTab === 'settings' && (
          <div className="space-y-6">
            <h2 className="text-clinical-lg font-semibold text-foreground">Setări cont & GDPR</h2>
            
            <div className="bg-card rounded-xl border border-border shadow-sm p-6">
              <h3 className="text-clinical-sm font-semibold mb-1 text-foreground">Consimțăminte GDPR active</h3>
              <p className="text-clinical-xs text-muted-foreground mb-4">
                Administrați modul în care clinica folosește datele dvs. conform Regulamentului General privind Protecția Datelor.
              </p>
              
              <div className="space-y-3">
                {[
                  { name: 'Prelucrare Date Medicale', key: 'dataProcessingConsent', description: 'Permite prelucrarea datelor cu caracter medical în scopul stabilirii diagnosticului și tratamentului.', mandatory: true, on: consentsState.dataProcessingConsent },
                  { name: 'Comunicări Cabinet (Email / SMS)', key: 'communicationConsent', description: 'Trimiterea de notificări privind programările, stadiul comenzilor de ochelari și mesaje administrative.', mandatory: false, on: consentsState.communicationConsent },
                  { name: 'Studii Clinice & Cercetare', key: 'researchConsent', description: 'Permite participarea la studii clinice și utilizarea datelor anonimizate în scopuri de cercetare științifică.', mandatory: false, on: consentsState.researchConsent }
                ].map((c) => (
                  <div key={c.key} className="flex items-start justify-between py-3 border-b border-border last:border-0">
                    <div className="mr-4">
                      <p className="text-clinical-sm font-semibold text-foreground">{c.name}</p>
                      <p className="text-clinical-xs text-muted-foreground mt-0.5 leading-relaxed">{c.description}</p>
                      {c.mandatory && (
                        <span className="inline-block text-[9px] text-red-600 font-bold tracking-wider mt-1 uppercase bg-red-50 px-1.5 py-0.5 rounded">
                          OBLIGATORIU CLINIC
                        </span>
                      )}
                    </div>
                    
                    <button
                      onClick={() => handleToggleConsent(c.key as any, c.mandatory)}
                      className={`w-11 h-6 rounded-full transition-colors relative shrink-0 ${
                        c.on ? 'bg-primary' : 'bg-muted'
                      } ${c.mandatory ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'}`}
                    >
                      <div className={`w-5 h-5 rounded-full bg-white shadow-sm absolute top-0.5 transition-transform ${
                        c.on ? 'translate-x-5.5' : 'translate-x-0.5'
                      }`} />
                    </button>
                  </div>
                ))}
              </div>
            </div>

            {/* Access Logs */}
            <div className="bg-card rounded-xl border border-border shadow-sm p-6 space-y-4">
              <div className="flex items-center justify-between border-b border-border pb-3">
                <div>
                  <h3 className="text-clinical-sm font-semibold text-foreground">Jurnal de Securitate și Audit</h3>
                  <p className="text-clinical-xs text-muted-foreground mt-0.5">
                    Istoricul accesărilor și modificărilor efectuate asupra dosarului dvs. medical.
                  </p>
                </div>
                <button
                  onClick={() => toast.info('Exportare jurnal de securitate în format securizat...')}
                  className="px-3 py-1.5 rounded-lg border border-border text-clinical-xs font-semibold hover:bg-muted text-foreground flex items-center gap-1 transition-colors"
                >
                  <Download className="w-3 h-3" /> Export PDF
                </button>
              </div>

              <div className="overflow-x-auto">
                <table className="w-full text-clinical-xs text-left">
                  <thead>
                    <tr className="border-b border-border bg-muted/40">
                      <th className="p-2 font-semibold text-muted-foreground">Dată/Oră</th>
                      <th className="p-2 font-semibold text-muted-foreground">Utilizator</th>
                      <th className="p-2 font-semibold text-muted-foreground">Rol</th>
                      <th className="p-2 font-semibold text-muted-foreground">Acțiune realizată</th>
                    </tr>
                  </thead>
                  <tbody>
                    {accessLogs.map((log, i) => (
                      <tr key={i} className="border-b border-border hover:bg-muted/20">
                        <td className="p-2 font-clinical text-muted-foreground">{log.date}</td>
                        <td className="p-2 text-foreground font-semibold">{log.user}</td>
                        <td className="p-2 text-muted-foreground">{log.role}</td>
                        <td className="p-2 text-foreground font-medium">{log.action}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              <div className="flex gap-2 pt-2">
                <button
                  onClick={() => toast.success('Cererea a fost înregistrată. Clinica vă va contacta în termen legal.')}
                  className="px-3 py-2 rounded-lg border border-red-200 text-red-600 text-clinical-xs font-semibold hover:bg-red-50/50 bg-card transition-colors"
                >
                  Cerere ștergere date (Dreptul de a fi uitat)
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default PatientPortal;
