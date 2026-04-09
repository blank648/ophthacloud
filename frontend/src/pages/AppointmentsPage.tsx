import React, { useState } from 'react';
import AppLayout from '@/components/AppLayout';
import { todayAppointments, resources, appointmentTypeLabels, appointmentTypeColors, statusLabels, statusStyles } from '@/data/demo-data';
import type { Appointment, AppointmentStatus } from '@/data/demo-data';
import { ClinicalFlagBadge, AppointmentStatusBadge } from '@/components/StatusBadge';
import { CalendarDays, ChevronLeft, ChevronRight, Plus, X, Clock, User, MapPin, FileText, MessageSquare } from 'lucide-react';

type ViewMode = 'day' | 'week' | 'month';

const timeSlots = Array.from({ length: 20 }, (_, i) => {
  const hour = Math.floor(i / 2) + 8;
  const min = i % 2 === 0 ? '00' : '30';
  return `${hour.toString().padStart(2, '0')}:${min}`;
});

const doctorResources = resources.filter(r => r.type === 'doctor' || r.type === 'optometrist');

const AppointmentsPage: React.FC = () => {
  const [view, setView] = useState<ViewMode>('day');
  const [appointments, setAppointments] = useState<Appointment[]>(todayAppointments);
  const [selectedApt, setSelectedApt] = useState<Appointment | null>(null);
  const [showBooking, setShowBooking] = useState(false);

  const cycleStatus = (id: string) => {
    const order: AppointmentStatus[] = ['booked', 'confirmed', 'checked_in', 'in_progress', 'completed'];
    setAppointments(prev => prev.map(a => {
      if (a.id !== id) return a;
      const idx = order.indexOf(a.status);
      if (idx === -1 || idx >= order.length - 1) return a;
      return { ...a, status: order[idx + 1] };
    }));
  };

  const getAppointmentsForSlot = (resourceId: string, time: string) => {
    return appointments.filter(a => {
      const aptTime = a.time;
      const aptMinutes = parseInt(aptTime.split(':')[0]) * 60 + parseInt(aptTime.split(':')[1]);
      const slotMinutes = parseInt(time.split(':')[0]) * 60 + parseInt(time.split(':')[1]);
      return a.doctorId === resourceId && aptMinutes >= slotMinutes && aptMinutes < slotMinutes + 30;
    });
  };

  const currentTimeMinutes = 10 * 60 + 15; // Demo: 10:15
  const currentTimeSlotIndex = Math.floor((currentTimeMinutes - 8 * 60) / 30);

  return (
    <AppLayout breadcrumbs={[{ label: 'Acasă' }, { label: 'Programări' }]}>
      {/* Header */}
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
            <button className="p-1.5 rounded-md hover:bg-muted"><ChevronLeft className="w-4 h-4" /></button>
            <span className="text-clinical-sm font-semibold px-3">29 Martie 2026</span>
            <button className="p-1.5 rounded-md hover:bg-muted"><ChevronRight className="w-4 h-4" /></button>
          </div>
          <button onClick={() => setShowBooking(true)} className="flex items-center gap-1.5 px-4 py-2.5 rounded-lg bg-primary text-white text-clinical-sm font-semibold hover:opacity-90 transition-colors">
            <Plus className="w-4 h-4" /> Programare nouă
          </button>
        </div>
      </div>

      {view === 'day' && (
        <div className="bg-card rounded-xl border border-border shadow-sm overflow-hidden">
          {/* Resource headers */}
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

          {/* Time grid */}
          <div className="relative">
            {timeSlots.map((time, slotIdx) => (
              <div key={time} className="flex border-b border-border relative" style={{ minHeight: 56 }}>
                {/* Current time indicator */}
                {slotIdx === currentTimeSlotIndex && (
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
                        const color = appointmentTypeColors[apt.type];
                        return (
                          <button key={apt.id} onClick={() => setSelectedApt(apt)}
                            className="w-full text-left p-1.5 rounded-md border-l-[3px] mb-0.5 transition-shadow hover:shadow-md"
                            style={{ borderLeftColor: color, backgroundColor: color + '18' }}>
                            <p className="text-clinical-xs font-semibold text-foreground truncate">{apt.patientName}</p>
                            <p className="text-[10px] text-muted-foreground truncate">{appointmentTypeLabels[apt.type]} · {apt.duration}min</p>
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

      {view === 'week' && <WeekView appointments={appointments} onSelect={setSelectedApt} />}
      {view === 'month' && <MonthView appointments={appointments} />}

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
              {/* Patient info */}
              <div className="flex items-center gap-3">
                <div className="w-12 h-12 rounded-full bg-primary-100 flex items-center justify-center text-primary-700 font-semibold">
                  {selectedApt.patientName.split(' ').map(n => n[0]).join('')}
                </div>
                <div>
                  <p className="text-clinical-base font-semibold text-foreground">{selectedApt.patientName}</p>
                  <p className="text-clinical-sm text-muted-foreground">{selectedApt.patientAge} ani · {selectedApt.patientId}</p>
                </div>
              </div>
              <div className="flex gap-1">{selectedApt.clinicalFlags.map(f => <ClinicalFlagBadge key={f} flag={f} />)}</div>

              {/* Details */}
              <div className="space-y-3">
                <div className="flex items-center gap-2 text-clinical-sm">
                  <Clock className="w-4 h-4 text-muted-foreground" />
                  <span>{selectedApt.time} · {selectedApt.duration} minute</span>
                </div>
                <div className="flex items-center gap-2 text-clinical-sm">
                  <User className="w-4 h-4 text-muted-foreground" />
                  <span>{selectedApt.doctorName}</span>
                </div>
                <div className="flex items-center gap-2 text-clinical-sm">
                  <CalendarDays className="w-4 h-4 text-muted-foreground" />
                  <span style={{ color: appointmentTypeColors[selectedApt.type] }}>{appointmentTypeLabels[selectedApt.type]}</span>
                </div>
              </div>

              {/* Status flow */}
              <div>
                <label className="text-clinical-xs text-muted-foreground font-semibold uppercase tracking-wider mb-2 block">Flux status</label>
                <div className="flex flex-wrap gap-2">
                  {(['booked', 'confirmed', 'checked_in', 'in_progress', 'completed'] as AppointmentStatus[]).map(s => {
                    const st = statusStyles[s];
                    const isActive = s === selectedApt.status;
                    const order = ['booked', 'confirmed', 'checked_in', 'in_progress', 'completed'];
                    const isPast = order.indexOf(s) < order.indexOf(selectedApt.status);
                    return (
                      <button key={s} onClick={() => {
                        setAppointments(prev => prev.map(a => a.id === selectedApt.id ? { ...a, status: s } : a));
                        setSelectedApt({ ...selectedApt, status: s });
                      }}
                        className={`px-2.5 py-1 rounded-full text-[11px] font-semibold border transition-all ${isActive ? 'ring-2 ring-primary/30 scale-105' : isPast ? 'opacity-50' : ''}`}
                        style={{ backgroundColor: st.bg, color: st.text, borderColor: isActive ? st.text : 'transparent' }}>
                        {statusLabels[s]}
                      </button>
                    );
                  })}
                </div>
                <div className="flex gap-2 mt-3">
                  <button onClick={() => {
                    setAppointments(prev => prev.map(a => a.id === selectedApt.id ? { ...a, status: 'no_show' } : a));
                    setSelectedApt({ ...selectedApt, status: 'no_show' });
                  }}
                    className="px-3 py-1.5 rounded-md text-[11px] font-semibold bg-red-50 text-red-700 border border-red-200 hover:bg-red-100">
                    Neprezentare
                  </button>
                  <button onClick={() => {
                    setAppointments(prev => prev.map(a => a.id === selectedApt.id ? { ...a, status: 'cancelled' } : a));
                    setSelectedApt({ ...selectedApt, status: 'cancelled' });
                  }}
                    className="px-3 py-1.5 rounded-md text-[11px] font-semibold bg-gray-50 text-gray-700 border border-gray-200 hover:bg-gray-100">
                    Anulare
                  </button>
                </div>
              </div>

              {/* Notification log */}
              <div>
                <label className="text-clinical-xs text-muted-foreground font-semibold uppercase tracking-wider mb-2 block">Notificări trimise</label>
                <div className="space-y-2">
                  {[
                    { time: '27.03 09:00', channel: 'SMS', type: 'Confirmare', status: 'Livrat' },
                    { time: '28.03 09:00', channel: 'SMS', type: 'Reamintire −24h', status: 'Livrat' },
                    { time: '29.03 07:00', channel: 'SMS', type: 'Reamintire −2h', status: 'Trimis' },
                  ].map((n, i) => (
                    <div key={i} className="flex items-center gap-2 text-clinical-xs">
                      <MessageSquare className="w-3 h-3 text-muted-foreground" />
                      <span className="font-clinical text-muted-foreground">{n.time}</span>
                      <span className="px-1.5 py-0.5 rounded text-[9px] font-semibold bg-blue-100 text-blue-700">{n.channel}</span>
                      <span className="flex-1">{n.type}</span>
                      <span className="px-1.5 py-0.5 rounded text-[9px] font-semibold bg-green-100 text-green-700">{n.status}</span>
                    </div>
                  ))}
                </div>
              </div>

              {/* Actions */}
              <div className="flex gap-2 pt-4 border-t border-border">
                <button className="flex-1 px-4 py-2.5 rounded-lg bg-primary text-white text-clinical-sm font-semibold">Deschide EMR</button>
                <button className="px-4 py-2.5 rounded-lg border border-border text-clinical-sm font-medium hover:bg-muted">Reprogramează</button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Booking Modal */}
      {showBooking && <BookingModal onClose={() => setShowBooking(false)} />}
    </AppLayout>
  );
};

// Week View
const WeekView: React.FC<{ appointments: Appointment[]; onSelect: (a: Appointment) => void }> = ({ appointments, onSelect }) => {
  const days = ['Lun 24', 'Mar 25', 'Mie 26', 'Joi 27', 'Vin 28', 'Sâm 29', 'Dum 30'];
  return (
    <div className="bg-card rounded-xl border border-border shadow-sm overflow-hidden">
      <div className="grid grid-cols-7 border-b-2 border-border">
        {days.map((d, i) => (
          <div key={i} className={`p-3 text-center border-l border-border first:border-l-0 ${i === 5 ? 'bg-primary-50' : 'bg-muted'}`}>
            <p className={`text-clinical-xs font-semibold ${i === 5 ? 'text-primary' : 'text-foreground'}`}>{d}</p>
          </div>
        ))}
      </div>
      <div className="grid grid-cols-7 min-h-[400px]">
        {days.map((_, dayIdx) => (
          <div key={dayIdx} className="border-l border-border first:border-l-0 p-1 space-y-1">
            {dayIdx === 5 && appointments.map(apt => {
              const color = appointmentTypeColors[apt.type];
              return (
                <button key={apt.id} onClick={() => onSelect(apt)}
                  className="w-full text-left p-1.5 rounded border-l-[3px] hover:shadow-sm transition-shadow"
                  style={{ borderLeftColor: color, backgroundColor: color + '12' }}>
                  <p className="text-[10px] font-semibold truncate">{apt.time} {apt.patientName}</p>
                </button>
              );
            })}
          </div>
        ))}
      </div>
    </div>
  );
};

// Month View
const MonthView: React.FC<{ appointments: Appointment[] }> = ({ appointments }) => {
  const daysInMonth = 31;
  const startDay = 6; // March 2026 starts on Sunday (shifted for Mon-first grid)
  const cells = Array.from({ length: 42 }, (_, i) => {
    const day = i - startDay + 1;
    return day >= 1 && day <= daysInMonth ? day : null;
  });

  return (
    <div className="bg-card rounded-xl border border-border shadow-sm overflow-hidden">
      <div className="grid grid-cols-7 border-b border-border">
        {['Lun', 'Mar', 'Mie', 'Joi', 'Vin', 'Sâm', 'Dum'].map(d => (
          <div key={d} className="p-2 text-center bg-muted text-clinical-xs font-semibold text-muted-foreground">{d}</div>
        ))}
      </div>
      <div className="grid grid-cols-7">
        {cells.map((day, i) => (
          <div key={i} className={`min-h-[100px] border-b border-r border-border p-1.5 ${day === 29 ? 'bg-primary-50' : ''} ${!day ? 'bg-muted/20' : ''}`}>
            {day && (
              <>
                <span className={`text-clinical-xs font-semibold ${day === 29 ? 'w-6 h-6 rounded-full bg-primary text-white inline-flex items-center justify-center' : 'text-foreground'}`}>{day}</span>
                {day === 29 && (
                  <div className="mt-1 space-y-0.5">
                    {appointments.slice(0, 3).map(apt => (
                      <div key={apt.id} className="flex items-center gap-1">
                        <div className="w-1.5 h-1.5 rounded-full" style={{ backgroundColor: appointmentTypeColors[apt.type] }} />
                        <span className="text-[9px] truncate">{apt.patientName.split(' ')[1]}</span>
                      </div>
                    ))}
                    {appointments.length > 3 && <span className="text-[9px] text-primary font-semibold">+{appointments.length - 3} mai mult</span>}
                  </div>
                )}
              </>
            )}
          </div>
        ))}
      </div>
    </div>
  );
};

// Booking Modal
const BookingModal: React.FC<{ onClose: () => void }> = ({ onClose }) => {
  const [step, setStep] = useState(1);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <div className="absolute inset-0 bg-black/50 backdrop-blur-sm" onClick={onClose} />
      <div className="relative bg-card rounded-xl shadow-lg w-[640px] max-h-[85vh] overflow-y-auto">
        <div className="p-5 border-b border-border flex items-center justify-between">
          <h3 className="text-clinical-lg font-semibold">Programare nouă</h3>
          <button onClick={onClose} className="p-1 rounded-md hover:bg-muted"><X className="w-5 h-5" /></button>
        </div>

        {/* Progress */}
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
              <h4 className="text-clinical-md font-semibold mb-4">Căutare pacient</h4>
              <input type="text" placeholder="Caută după nume, CNP sau ID..." className="w-full h-10 rounded-md border border-input px-3 text-clinical-base focus:border-primary focus:ring-2 focus:ring-primary/25 outline-none" />
              <p className="text-clinical-xs text-muted-foreground mt-2">sau <button className="text-primary font-semibold">Pacient nou</button></p>
            </div>
          )}
          {step === 2 && (
            <div>
              <h4 className="text-clinical-md font-semibold mb-4">Tip serviciu</h4>
              <div className="grid grid-cols-2 gap-3">
                {Object.entries(appointmentTypeLabels).map(([key, label]) => (
                  <button key={key} className="p-4 rounded-xl border border-border hover:border-primary hover:bg-primary-50 text-left transition-colors">
                    <div className="w-3 h-3 rounded-full mb-2" style={{ backgroundColor: appointmentTypeColors[key as keyof typeof appointmentTypeColors] }} />
                    <p className="text-clinical-sm font-semibold">{label}</p>
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
                  <button key={r.id} className="w-full flex items-center gap-3 p-4 rounded-xl border border-border hover:border-primary hover:bg-primary-50 transition-colors">
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
              <h4 className="text-clinical-md font-semibold mb-4">Data și ora</h4>
              <p className="text-clinical-sm text-muted-foreground mb-4">Selectați un slot disponibil:</p>
              <div className="grid grid-cols-4 gap-2">
                {['09:00', '09:30', '10:00', '10:30', '11:00', '11:30', '14:00', '14:30', '15:00', '15:30', '16:00', '16:30'].map(t => (
                  <button key={t} className="p-2 rounded-lg border border-border text-clinical-sm font-semibold text-center hover:border-primary hover:bg-primary-50 transition-colors">{t}</button>
                ))}
              </div>
            </div>
          )}
          {step === 5 && (
            <div>
              <h4 className="text-clinical-md font-semibold mb-4">Confirmare</h4>
              <div className="bg-muted/30 rounded-lg p-4 space-y-2 text-clinical-sm">
                <p><span className="text-muted-foreground">Pacient:</span> Ion Marinescu</p>
                <p><span className="text-muted-foreground">Tip:</span> Consultație inițială</p>
                <p><span className="text-muted-foreground">Medic:</span> Dr. Alexandru Popescu</p>
                <p><span className="text-muted-foreground">Data/Ora:</span> 29.03.2026 · 09:00</p>
              </div>
              <label className="flex items-center gap-2 mt-4 text-clinical-sm cursor-pointer">
                <input type="checkbox" className="w-4 h-4 rounded border-border text-primary" defaultChecked /> Trimite SMS de confirmare
              </label>
            </div>
          )}
        </div>

        <div className="p-5 border-t border-border flex justify-between">
          <button onClick={() => step > 1 ? setStep(step - 1) : onClose} className="px-4 py-2 rounded-lg text-clinical-sm font-medium text-muted-foreground hover:bg-muted">
            {step === 1 ? 'Anulare' : 'Înapoi'}
          </button>
          <button onClick={() => step < 5 ? setStep(step + 1) : onClose}
            className="px-5 py-2.5 rounded-lg bg-primary text-white text-clinical-sm font-semibold hover:opacity-90">
            {step === 5 ? 'Confirmă programarea' : 'Continuă'}
          </button>
        </div>
      </div>
    </div>
  );
};

export default AppointmentsPage;
