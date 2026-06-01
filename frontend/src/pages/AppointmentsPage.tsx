import React, { useState, useMemo, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { toast } from 'sonner';
import AppLayout from '@/components/AppLayout';
import { statusLabels, statusStyles } from '@/data/demo-data';
import { ClinicalFlagBadge } from '@/components/StatusBadge';
import { CalendarDays, ChevronLeft, ChevronRight, Plus, X, Clock, User, MessageSquare, Loader2 } from 'lucide-react';
import { useAppointments, useUpdateAppointmentStatus, useCreateAppointment, useAppointmentTypes } from '@/hooks/useAppointments';
import { usePatients, usePatient } from '@/hooks/usePatients';
import { useStaff } from '@/hooks/useAdmin';
import type { AppointmentSummaryDto, AppointmentStatus } from '@/types/appointments';

type ViewMode = 'day' | 'week' | 'month';

const timeSlots = Array.from({ length: 20 }, (_, i) => {
  const hour = Math.floor(i / 2) + 8;
  const min = i % 2 === 0 ? '00' : '30';
  return `${hour.toString().padStart(2, '0')}:${min}`;
});

function formatTime(iso: string) {
  if (!iso) return '';
  const d = new Date(iso);
  const hh = String(d.getHours()).padStart(2, '0');
  const mm = String(d.getMinutes()).padStart(2, '0');
  return `${hh}:${mm}`;
}

const AppointmentsPage: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  
  const initialPatientId = searchParams.get('patientId') || undefined;
  const initialOpenBooking = searchParams.get('openBooking') === 'true';

  const [view, setView] = useState<ViewMode>('day');
  const [selectedDate, setSelectedDate] = useState<Date>(new Date());
  const [selectedApt, setSelectedApt] = useState<AppointmentSummaryDto | null>(null);
  const [showBooking, setShowBooking] = useState(initialOpenBooking);

  useEffect(() => {
    if (initialOpenBooking) {
      setShowBooking(true);
      // Remove openBooking from URL so it doesn't re-open on refresh
      searchParams.delete('openBooking');
      setSearchParams(searchParams, { replace: true });
    }
  }, [initialOpenBooking, searchParams, setSearchParams]);

  const { data: staffRes } = useStaff({ role: 'DOCTOR' });
  const doctorResources = (staffRes?.data || [])
    .filter(d => d.isActive)
    .map(d => ({
      id: d.id,
      name: `${d.firstName} ${d.lastName}`,
      specialty: d.specialization || 'Medic Oftalmolog'
    }));

  const range = useMemo(() => {
    const d = new Date(selectedDate);
    const fmt = (dt: Date) => {
      const y = dt.getFullYear();
      const m = String(dt.getMonth() + 1).padStart(2, '0');
      const dd = String(dt.getDate()).padStart(2, '0');
      return `${y}-${m}-${dd}`;
    };
    if (view === 'day') {
      const iso = fmt(d);
      return { from: iso, to: iso };
    }
    if (view === 'week') {
      const day = d.getDay() || 7;
      const start = new Date(d);
      start.setDate(d.getDate() - day + 1);
      const end = new Date(start);
      end.setDate(start.getDate() + 6);
      return { from: fmt(start), to: fmt(end) };
    }
    const start = new Date(d.getFullYear(), d.getMonth(), 1);
    const end = new Date(d.getFullYear(), d.getMonth() + 1, 0);
    return { from: fmt(start), to: fmt(end) };
  }, [selectedDate, view]);

  const { data: appointments = [], isLoading } = useAppointments(range.from, range.to);

  const { mutateAsync: updateStatus } = useUpdateAppointmentStatus();

  const handleUpdateStatus = async (id: string, newStatus: AppointmentStatus, apt: AppointmentSummaryDto) => {
    try {
      await updateStatus({ id, status: newStatus });
      setSelectedApt({ ...apt, status: newStatus });
      toast.success(`Status actualizat la ${statusLabels[newStatus]}`);
    } catch (e) {
      toast.error('Eroare la actualizarea statusului');
    }
  };

  const getAppointmentsForSlot = (resourceId: string, time: string) => {
    return appointments.filter(a => {
      const aptTime = formatTime(a.startAt);
      if (!aptTime) return false;
      const aptMinutes = parseInt(aptTime.split(':')[0]) * 60 + parseInt(aptTime.split(':')[1]);
      const slotMinutes = parseInt(time.split(':')[0]) * 60 + parseInt(time.split(':')[1]);
      return a.doctorId === resourceId && aptMinutes >= slotMinutes && aptMinutes < slotMinutes + 30;
    });
  };

  const shiftDate = (days: number) => {
    const newD = new Date(selectedDate);
    newD.setDate(newD.getDate() + days);
    setSelectedDate(newD);
  };

  const dateLabel = selectedDate.toLocaleDateString('ro-RO', { day: 'numeric', month: 'long', year: 'numeric' });

  // For the red line in day view
  const now = new Date();
  const currentTimeMinutes = now.getHours() * 60 + now.getMinutes();
  const currentTimeSlotIndex = Math.floor((currentTimeMinutes - 8 * 60) / 30);
  const isToday = new Date().toDateString() === selectedDate.toDateString();

  return (
    <AppLayout breadcrumbs={[{ label: 'Acasă' }, { label: 'Programări' }]}>
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-4">
          <h1 className="text-clinical-lg font-bold text-foreground">Programări</h1>
          <div className="flex items-center gap-1 bg-muted rounded-lg p-1">
            {(['day', 'week', 'month'] as ViewMode[]).map(v => (
              <button key={v} onClick={() => setView(v)}
                className={`px-3 py-1.5 rounded-md text-clinical-xs font-semibold transition-colors ${view === v ? 'bg-primary text-white' : 'text-muted-foreground hover:text-foreground'}`}>
                {v === 'day' ? 'Zi' : v === 'week' ? 'Săptămână' : 'Lună'}
              </button>
            ))}
          </div>
        </div>
        <div className="flex items-center gap-3">
          <div className="flex items-center gap-1">
            <button onClick={() => shiftDate(view === 'day' ? -1 : view === 'week' ? -7 : -30)} className="p-1.5 rounded-md hover:bg-muted"><ChevronLeft className="w-4 h-4" /></button>
            <span className="text-clinical-sm font-semibold px-3 w-40 text-center">{dateLabel}</span>
            <button onClick={() => shiftDate(view === 'day' ? 1 : view === 'week' ? 7 : 30)} className="p-1.5 rounded-md hover:bg-muted"><ChevronRight className="w-4 h-4" /></button>
          </div>
          <button onClick={() => setShowBooking(true)} className="flex items-center gap-1.5 px-4 py-2.5 rounded-lg bg-primary text-white text-clinical-sm font-semibold hover:opacity-90 transition-colors">
            <Plus className="w-4 h-4" /> Programare nouă
          </button>
        </div>
      </div>

      {isLoading && (
        <div className="flex justify-center p-8">
          <Loader2 className="w-8 h-8 animate-spin text-primary" />
        </div>
      )}

      {!isLoading && view === 'day' && (
        <div className="bg-card rounded-xl border border-border shadow-sm overflow-hidden">
          <div className="flex border-b-2 border-border">
            <div className="w-16 shrink-0 bg-muted" />
            {doctorResources.map(r => (
              <div key={r.id} className="flex-1 p-3 bg-muted border-l border-border text-center">
                <div className="w-8 h-8 mx-auto rounded-full bg-primary-100 text-primary-700 text-[11px] font-semibold flex items-center justify-center mb-1">
                  {r.name.split(' ').slice(-1)[0][0]}{r.name.split(' ').slice(-1)[0][1] || ''}
                </div>
                <p className="text-clinical-xs font-semibold text-foreground truncate">{r.name}</p>
                <span className="text-[10px] text-muted-foreground">{r.specialty}</span>
              </div>
            ))}
          </div>

          <div className="relative">
            {timeSlots.map((time, slotIdx) => (
              <div key={time} className="flex border-b border-border relative" style={{ minHeight: 56 }}>
                {isToday && slotIdx === currentTimeSlotIndex && (
                  <div className="absolute left-0 right-0 z-10" style={{ top: `${((currentTimeMinutes - 8 * 60) % 30) / 30 * 100}%` }}>
                    <div className="flex items-center">
                      <div className="w-2.5 h-2.5 rounded-full bg-destructive" />
                      <div className="flex-1 h-[2px] bg-destructive" />
                    </div>
                  </div>
                )}
                <div className="w-16 shrink-0 pr-2 py-1 text-right">
                  <span className="text-clinical-xs text-muted-foreground">{time}</span>
                </div>
                {doctorResources.map(r => {
                  const slotApts = getAppointmentsForSlot(r.id, time);
                  return (
                    <div key={r.id} className="flex-1 border-l border-border p-0.5 relative min-h-[56px] hover:bg-primary-50/30 transition-colors">
                      {slotApts.map(apt => {
                        const color = apt.appointmentTypeColor || '#cbd5e1';
                        return (
                          <button key={apt.id} onClick={() => setSelectedApt(apt)}
                            className="w-full text-left p-1.5 rounded-md border-l-[3px] mb-0.5 transition-shadow hover:shadow-md"
                            style={{ borderLeftColor: color, backgroundColor: color + '18' }}>
                            <p className="text-clinical-xs font-semibold text-foreground truncate">{apt.patientName}</p>
                            <p className="text-[10px] text-muted-foreground truncate">{apt.appointmentTypeName} · {apt.durationMinutes}min</p>
                          </button>
                        );
                      })}
                    </div>
                  );
                })}
              </div>
            ))}
          </div>
        </div>
      )}

      {!isLoading && view === 'week' && <WeekView appointments={appointments} onSelect={setSelectedApt} selectedDate={selectedDate} />}
      {!isLoading && view === 'month' && <MonthView appointments={appointments} selectedDate={selectedDate} />}

      {/* Appointment Detail Panel */}
      {selectedApt && (
        <div className="fixed inset-0 z-50 flex justify-end">
          <div className="absolute inset-0 bg-black/30 backdrop-blur-sm" onClick={() => setSelectedApt(null)} />
          <div className="relative w-[480px] bg-card h-full shadow-lg overflow-y-auto">
            <div className="p-5 border-b border-border flex items-center justify-between">
              <h3 className="text-clinical-md font-semibold">Detalii programare</h3>
              <button onClick={() => setSelectedApt(null)} className="p-1 rounded-md hover:bg-muted"><X className="w-5 h-5" /></button>
            </div>
            <div className="p-5 space-y-5">
              <div className="flex items-center gap-3">
                <div className="w-12 h-12 rounded-full bg-primary-100 flex items-center justify-center text-primary-700 font-semibold">
                  {selectedApt.patientName.split(' ').map(n => n[0]).join('')}
                </div>
                <div>
                  <p className="text-clinical-base font-semibold text-foreground">{selectedApt.patientName}</p>
                  <p className="text-clinical-sm text-muted-foreground">{selectedApt.patientMrn}</p>
                </div>
              </div>
              <div className="flex gap-1">{selectedApt.activeDiagnosisFlags?.map(f => <ClinicalFlagBadge key={f} flag={f} />)}</div>

              <div className="space-y-3">
                <div className="flex items-center gap-2 text-clinical-sm">
                  <Clock className="w-4 h-4 text-muted-foreground" />
                  <span>{new Date(selectedApt.startAt).toLocaleString('ro-RO')} · {selectedApt.durationMinutes} minute</span>
                </div>
                <div className="flex items-center gap-2 text-clinical-sm">
                  <User className="w-4 h-4 text-muted-foreground" />
                  <span>{selectedApt.doctorName || 'Medic Nespecificat'}</span>
                </div>
                <div className="flex items-center gap-2 text-clinical-sm">
                  <CalendarDays className="w-4 h-4 text-muted-foreground" />
                  <span style={{ color: selectedApt.appointmentTypeColor || '#000' }}>{selectedApt.appointmentTypeName}</span>
                </div>
                {selectedApt.chiefComplaint && (
                  <div className="flex items-start gap-2 text-clinical-sm mt-2 p-2 bg-muted/50 rounded">
                    <MessageSquare className="w-4 h-4 text-muted-foreground mt-0.5" />
                    <span><strong>Motiv:</strong> {selectedApt.chiefComplaint}</span>
                  </div>
                )}
              </div>

              <div>
                <label className="text-clinical-xs text-muted-foreground font-semibold uppercase tracking-wider mb-2 block">Flux status</label>
                <div className="flex flex-wrap gap-2">
                  {(['BOOKED', 'CONFIRMED', 'CHECKED_IN', 'IN_PROGRESS', 'COMPLETED'] as AppointmentStatus[]).map(s => {
                    const st = statusStyles[s.toLowerCase() as keyof typeof statusStyles];
                    if (!st) return null;
                    const isActive = s === selectedApt.status;
                    const order = ['BOOKED', 'CONFIRMED', 'CHECKED_IN', 'IN_PROGRESS', 'COMPLETED'];
                    const isPast = order.indexOf(s) < order.indexOf(selectedApt.status);
                    return (
                      <button key={s} onClick={() => handleUpdateStatus(selectedApt.id, s, selectedApt)}
                        className={`px-2.5 py-1 rounded-full text-[11px] font-semibold border transition-all ${isActive ? 'ring-2 ring-primary/30 scale-105' : isPast ? 'opacity-50' : ''}`}
                        style={{ backgroundColor: st.bg, color: st.text, borderColor: isActive ? st.text : 'transparent' }}>
                        {statusLabels[s.toLowerCase() as keyof typeof statusLabels] || s}
                      </button>
                    );
                  })}
                </div>
                <div className="flex gap-2 mt-3">
                  <button onClick={() => handleUpdateStatus(selectedApt.id, 'NO_SHOW', selectedApt)}
                    className="px-3 py-1.5 rounded-md text-[11px] font-semibold bg-red-50 text-red-700 border border-red-200 hover:bg-red-100">
                    Neprezentare
                  </button>
                  <button onClick={() => handleUpdateStatus(selectedApt.id, 'CANCELLED', selectedApt)}
                    className="px-3 py-1.5 rounded-md text-[11px] font-semibold bg-gray-50 text-gray-700 border border-gray-200 hover:bg-gray-100">
                    Anulare
                  </button>
                </div>
              </div>

              <div className="flex gap-2 pt-4 border-t border-border">
                <button 
                  onClick={() => navigate(`/consultation?patientId=${selectedApt.patientId}`)}
                  className="flex-1 px-4 py-2.5 rounded-lg bg-primary text-white text-clinical-sm font-semibold"
                >
                  Deschide EMR
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {showBooking && <BookingModal onClose={() => setShowBooking(false)} dateStr={range.from} doctorResources={doctorResources} initialPatientId={initialPatientId} />}
    </AppLayout>
  );
};

// Simplified Week View
const WeekView: React.FC<{ appointments: AppointmentSummaryDto[]; onSelect: (a: AppointmentSummaryDto) => void, selectedDate: Date }> = ({ appointments, onSelect, selectedDate }) => {
  const startOfWeek = new Date(selectedDate);
  const day = startOfWeek.getDay() || 7;
  startOfWeek.setDate(startOfWeek.getDate() - day + 1);

  const days = Array.from({ length: 7 }, (_, i) => {
    const d = new Date(startOfWeek);
    d.setDate(d.getDate() + i);
    return {
      label: d.toLocaleDateString('ro-RO', { weekday: 'short', day: 'numeric' }),
      dateStr: d.toISOString().split('T')[0]
    };
  });

  return (
    <div className="bg-card rounded-xl border border-border shadow-sm overflow-hidden">
      <div className="grid grid-cols-7 border-b-2 border-border">
        {days.map((d, i) => (
          <div key={i} className={`p-3 text-center border-l border-border first:border-l-0 bg-muted`}>
            <p className="text-clinical-xs font-semibold text-foreground">{d.label}</p>
          </div>
        ))}
      </div>
      <div className="grid grid-cols-7 min-h-[400px]">
        {days.map((d, dayIdx) => (
          <div key={dayIdx} className="border-l border-border first:border-l-0 p-1 space-y-1">
            {appointments.filter(a => a.startAt.startsWith(d.dateStr)).map(apt => {
              const color = apt.appointmentTypeColor || '#cbd5e1';
              return (
                <button key={apt.id} onClick={() => onSelect(apt)}
                  className="w-full text-left p-1.5 rounded border-l-[3px] hover:shadow-sm transition-shadow"
                  style={{ borderLeftColor: color, backgroundColor: color + '12' }}>
                  <p className="text-[10px] font-semibold truncate">{formatTime(apt.startAt)} {apt.patientName}</p>
                </button>
              );
            })}
          </div>
        ))}
      </div>
    </div>
  );
};

// Simplified Month View
const MonthView: React.FC<{ appointments: AppointmentSummaryDto[], selectedDate: Date }> = ({ appointments, selectedDate }) => {
  const year = selectedDate.getFullYear();
  const month = selectedDate.getMonth();
  const firstDay = new Date(year, month, 1).getDay() || 7;
  const daysInMonth = new Date(year, month + 1, 0).getDate();
  
  const cells = Array.from({ length: 42 }, (_, i) => {
    const day = i - firstDay + 2;
    if (day >= 1 && day <= daysInMonth) {
      const d = new Date(year, month, day);
      return { day, dateStr: d.toISOString().split('T')[0] };
    }
    return null;
  });

  return (
    <div className="bg-card rounded-xl border border-border shadow-sm overflow-hidden">
      <div className="grid grid-cols-7 border-b border-border">
        {['Lun', 'Mar', 'Mie', 'Joi', 'Vin', 'Sâm', 'Dum'].map(d => (
          <div key={d} className="p-2 text-center bg-muted text-clinical-xs font-semibold text-muted-foreground">{d}</div>
        ))}
      </div>
      <div className="grid grid-cols-7">
        {cells.map((cell, i) => (
          <div key={i} className={`min-h-[100px] border-b border-r border-border p-1.5 ${!cell ? 'bg-muted/20' : ''}`}>
            {cell && (
              <>
                <span className="text-clinical-xs font-semibold text-foreground">{cell.day}</span>
                <div className="mt-1 space-y-0.5">
                  {appointments.filter(a => a.startAt.startsWith(cell.dateStr)).slice(0, 3).map(apt => (
                    <div key={apt.id} className="flex items-center gap-1">
                      <div className="w-1.5 h-1.5 rounded-full" style={{ backgroundColor: apt.appointmentTypeColor || '#cbd5e1' }} />
                      <span className="text-[9px] truncate">{apt.patientName}</span>
                    </div>
                  ))}
                </div>
              </>
            )}
          </div>
        ))}
      </div>
    </div>
  );
};

// Live Booking Modal
const BookingModal: React.FC<{ onClose: () => void, dateStr: string, doctorResources: any[], initialPatientId?: string }> = ({ onClose, dateStr, doctorResources, initialPatientId }) => {
  const [step, setStep] = useState(initialPatientId ? 2 : 1);
  const [search, setSearch] = useState('');
  
  const { data: initialPatientRes } = usePatient(initialPatientId);
  const { data: patientsRes } = usePatients({ q: search || undefined, page: 0, size: 5 });
  const { data: aptTypes } = useAppointmentTypes();
  const { mutateAsync: createApt, isPending } = useCreateAppointment();

  const [patientId, setPatientId] = useState(initialPatientId || '');
  const [patientName, setPatientName] = useState('');
  const [aptTypeId, setAptTypeId] = useState('');
  const [aptTypeName, setAptTypeName] = useState('');
  const [doctorId, setDoctorId] = useState('');
  const [doctorName, setDoctorName] = useState('');
  const [time, setTime] = useState('');

  const patients = patientsRes?.data || [];
  const types = aptTypes || [];

  useEffect(() => {
    if (initialPatientRes) {
      setPatientName(initialPatientRes.name);
    }
  }, [initialPatientRes]);

  const handleCreate = async () => {
    if (!patientId || !aptTypeId || !doctorId || !time) return;
    try {
      const [hours, minutes] = time.split(':').map(Number);
      const [year, month, day] = dateStr.split('-').map(Number);
      const localDate = new Date(year, month - 1, day, hours, minutes);

      await createApt({
        patientId,
        doctorId,
        doctorName,
        appointmentTypeId: aptTypeId,
        startAt: localDate.toISOString(),
        durationMinutes: types.find(t => t.id === aptTypeId)?.durationMinutes || 30,
        channel: 'IN_PERSON',
        chiefComplaint: 'Consultație programată',
      });
      toast.success('Programare creată cu succes');
      onClose();
    } catch (e: any) {
      if (e?.code === 'DOUBLE_BOOKING') {
        toast.error('Eroare: Conflict de programare - doctorul este deja ocupat în acel interval.');
      } else {
        toast.error('Eroare la crearea programării');
      }
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <div className="absolute inset-0 bg-black/50 backdrop-blur-sm" onClick={onClose} />
      <div className="relative bg-card rounded-xl shadow-lg w-[640px] max-h-[85vh] overflow-y-auto">
        <div className="p-5 border-b border-border flex items-center justify-between">
          <h3 className="text-clinical-lg font-semibold">Programare nouă</h3>
          <button onClick={onClose} className="p-1 rounded-md hover:bg-muted"><X className="w-5 h-5" /></button>
        </div>

        <div className="px-5 pt-4">
          <div className="flex items-center gap-2 mb-6">
            {[1, 2, 3, 4, 5].map(s => (
              <React.Fragment key={s}>
                <div className={`w-8 h-8 rounded-full flex items-center justify-center text-[12px] font-semibold ${s <= step ? 'bg-primary text-white' : 'bg-muted text-muted-foreground'}`}>{s}</div>
                {s < 5 && <div className={`flex-1 h-1 rounded ${s < step ? 'bg-primary' : 'bg-border'}`} />}
              </React.Fragment>
            ))}
          </div>
        </div>

        <div className="p-5 min-h-[300px]">
          {step === 1 && (
            <div>
              <h4 className="text-clinical-md font-semibold mb-4">Selectare pacient</h4>
              <input type="text" value={search} onChange={e => setSearch(e.target.value)} placeholder="Caută pacient..." className="w-full h-10 rounded-md border border-input px-3 mb-4 focus:border-primary focus:ring-2 focus:ring-primary/25 outline-none" />
              <div className="space-y-2">
                {patients.map(p => (
                  <button key={p.id} onClick={() => { setPatientId(p.id); setPatientName(`${p.firstName} ${p.lastName}`); setStep(2); }}
                    className={`w-full text-left p-3 rounded border ${patientId === p.id ? 'border-primary bg-primary-50' : 'border-border hover:bg-muted'}`}>
                    <span className="font-semibold">{p.firstName} {p.lastName}</span> <span className="text-muted-foreground text-sm ml-2">{p.mrn}</span>
                  </button>
                ))}
              </div>
            </div>
          )}
          {step === 2 && (
            <div>
              <h4 className="text-clinical-md font-semibold mb-4">Tip serviciu</h4>
              <div className="grid grid-cols-2 gap-3">
                {types.map(t => (
                  <button key={t.id} onClick={() => { setAptTypeId(t.id); setAptTypeName(t.name); setStep(3); }}
                    className={`p-4 rounded-xl border text-left transition-colors ${aptTypeId === t.id ? 'border-primary bg-primary-50' : 'border-border hover:border-primary'}`}>
                    <div className="w-3 h-3 rounded-full mb-2" style={{ backgroundColor: t.color || '#000' }} />
                    <p className="text-clinical-sm font-semibold">{t.name}</p>
                  </button>
                ))}
              </div>
            </div>
          )}
          {step === 3 && (
            <div>
              <h4 className="text-clinical-md font-semibold mb-4">Selectare medic</h4>
              <div className="space-y-3">
                {doctorResources.map(r => (
                  <button key={r.id} onClick={() => { setDoctorId(r.id); setDoctorName(r.name); setStep(4); }}
                    className={`w-full flex items-center gap-3 p-4 rounded-xl border transition-colors ${doctorId === r.id ? 'border-primary bg-primary-50' : 'border-border hover:border-primary'}`}>
                    <div className="w-10 h-10 rounded-full bg-primary-100 text-primary-700 text-sm font-semibold flex items-center justify-center">
                      {r.name.split(' ').slice(-1)[0][0]}
                    </div>
                    <div className="text-left"><p className="text-clinical-sm font-semibold">{r.name}</p><p className="text-clinical-xs text-muted-foreground">{r.specialty}</p></div>
                  </button>
                ))}
              </div>
            </div>
          )}
          {step === 4 && (
            <div>
              <h4 className="text-clinical-md font-semibold mb-4">Data și ora ({dateStr})</h4>
              <div className="grid grid-cols-4 gap-2">
                {['09:00', '09:30', '10:00', '10:30', '11:00', '11:30', '14:00', '14:30', '15:00', '15:30', '16:00', '16:30'].map(t => (
                  <button key={t} onClick={() => { setTime(t); setStep(5); }}
                    className={`p-2 rounded-lg border text-clinical-sm font-semibold text-center transition-colors ${time === t ? 'border-primary bg-primary-50 text-primary' : 'border-border hover:border-primary'}`}>
                    {t}
                  </button>
                ))}
              </div>
            </div>
          )}
          {step === 5 && (
            <div>
              <h4 className="text-clinical-md font-semibold mb-4">Confirmare</h4>
              <div className="bg-muted/30 rounded-lg p-4 space-y-2 text-clinical-sm border border-border">
                <p><span className="text-muted-foreground w-20 inline-block">Pacient:</span> <span className="font-semibold">{patientName}</span></p>
                <p><span className="text-muted-foreground w-20 inline-block">Tip:</span> <span className="font-semibold">{aptTypeName}</span></p>
                <p><span className="text-muted-foreground w-20 inline-block">Medic:</span> <span className="font-semibold">{doctorName}</span></p>
                <p><span className="text-muted-foreground w-20 inline-block">Data/Ora:</span> <span className="font-semibold">{dateStr} · {time}</span></p>
              </div>
            </div>
          )}
        </div>

        <div className="p-5 border-t border-border flex justify-between">
          <button onClick={() => step > 1 ? setStep(step - 1) : onClose()} className="px-4 py-2 rounded-lg text-clinical-sm font-medium text-muted-foreground hover:bg-muted" disabled={isPending}>
            {step === 1 ? 'Anulare' : 'Înapoi'}
          </button>
          <button 
            onClick={step < 5 ? () => setStep(step + 1) : handleCreate}
            disabled={isPending || (step === 1 && !patientId) || (step === 2 && !aptTypeId) || (step === 3 && !doctorId) || (step === 4 && !time)}
            className="px-5 py-2.5 rounded-lg bg-primary text-white text-clinical-sm font-semibold hover:opacity-90 disabled:opacity-50 flex items-center gap-2">
            {isPending && <Loader2 className="w-4 h-4 animate-spin" />}
            {step === 5 ? 'Confirmă programarea' : 'Continuă'}
          </button>
        </div>
      </div>
    </div>
  );
};

export default AppointmentsPage;
