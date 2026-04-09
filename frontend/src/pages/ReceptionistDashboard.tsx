import React from 'react';
import AppLayout from '@/components/AppLayout';
import KPICard from '@/components/KPICard';
import { AppointmentStatusBadge, ClinicalFlagBadge } from '@/components/StatusBadge';
import { todayAppointments, doctors } from '@/data/demo-data';
import { CalendarDays, UserCheck, CreditCard, Plus } from 'lucide-react';

const ReceptionistDashboard: React.FC = () => {
  const hours = Array.from({ length: 10 }, (_, i) => `${8 + i}:00`);

  return (
    <AppLayout breadcrumbs={[{ label: 'Acasă' }, { label: 'Dashboard Recepție' }]}>
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        <KPICard icon={CalendarDays} title="Programări azi" value={todayAppointments.length.toString()} />
        <KPICard icon={UserCheck} title="Check-in realizate" value="3" iconColor="#10B981" />
        <KPICard icon={CreditCard} title="Plăți restante" value="2.450 RON" iconColor="#C0392B" />
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-3 gap-6">
        {/* Day Calendar */}
        <div className="xl:col-span-2 bg-card rounded-xl border border-border shadow-sm overflow-hidden">
          <div className="p-4 border-b border-border flex items-center justify-between">
            <h3 className="text-clinical-md font-semibold">Calendar zilnic — 29.03.2026</h3>
            <button className="flex items-center gap-1.5 px-3 py-2 rounded-lg bg-primary text-white text-[12px] font-semibold">
              <Plus className="w-3.5 h-3.5" /> Programare nouă
            </button>
          </div>
          <div className="overflow-auto max-h-[600px]">
            <table className="w-full">
              <thead className="sticky top-0 z-10">
                <tr className="bg-muted">
                  <th className="w-16 p-2 text-clinical-xs font-semibold text-muted-foreground text-right">ORA</th>
                  {doctors.slice(0, 3).map(d => (
                    <th key={d.id} className="p-2 text-clinical-xs font-semibold text-muted-foreground text-left">
                      <div className="flex items-center gap-2">
                        <div className="w-6 h-6 rounded-full bg-primary-100 text-primary-700 text-[9px] font-bold flex items-center justify-center">
                          {d.name.split(' ').slice(-1)[0][0]}{d.name.split(' ').slice(-2)[0]?.[0]}
                        </div>
                        <span className="truncate">{d.name}</span>
                      </div>
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {hours.map((hour) => (
                  <tr key={hour} className="border-t border-border hover:bg-primary-50/50">
                    <td className="p-2 text-clinical-xs text-muted-foreground text-right font-clinical">{hour}</td>
                    {doctors.slice(0, 3).map((doc, di) => {
                      const apt = todayAppointments.find(
                        a => a.time.startsWith(hour.split(':')[0] + ':') && di === 0
                      );
                      return (
                        <td key={doc.id} className="p-1">
                          {apt && (
                            <div
                              className="rounded-md p-2 border-l-[3px]"
                              style={{
                                borderLeftColor: '#13759C',
                                backgroundColor: '#13759C1A',
                              }}
                            >
                              <p className="text-clinical-xs font-semibold text-foreground">{apt.patientName}</p>
                              <p className="text-[10px] text-muted-foreground">{apt.duration} min</p>
                            </div>
                          )}
                        </td>
                      );
                    })}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        {/* Right panel */}
        <div className="space-y-4">
          {/* Check-in queue */}
          <div className="bg-card rounded-xl border border-border shadow-sm">
            <div className="p-4 border-b border-border">
              <h3 className="text-clinical-md font-semibold">Coadă check-in</h3>
            </div>
            <div className="divide-y divide-border">
              {todayAppointments.filter(a => ['confirmed', 'checked_in'].includes(a.status)).map(apt => (
                <div key={apt.id} className="p-3 flex items-center gap-3">
                  <div className="w-8 h-8 rounded-full bg-primary-100 text-primary-700 text-[11px] font-semibold flex items-center justify-center">
                    {apt.patientName.split(' ').map(n => n[0]).join('')}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-clinical-sm font-semibold truncate">{apt.patientName}</p>
                    <p className="text-clinical-xs text-muted-foreground">{apt.time}</p>
                  </div>
                  <AppointmentStatusBadge status={apt.status} />
                </div>
              ))}
            </div>
          </div>

          {/* Outstanding payments */}
          <div className="bg-card rounded-xl border border-border shadow-sm">
            <div className="p-4 border-b border-border">
              <h3 className="text-clinical-md font-semibold">Plăți restante</h3>
            </div>
            <div className="divide-y divide-border">
              {[
                { name: 'Elena Dumitrescu', amount: '1.200 RON', days: 12 },
                { name: 'Mihai Stanescu', amount: '850 RON', days: 5 },
                { name: 'Ana Georgescu', amount: '400 RON', days: 2 },
              ].map((p, i) => (
                <div key={i} className="p-3 flex items-center justify-between">
                  <div>
                    <p className="text-clinical-sm font-semibold">{p.name}</p>
                    <p className="text-clinical-xs text-muted-foreground">Depășit cu {p.days} zile</p>
                  </div>
                  <span className="text-clinical-sm font-bold text-clinical-danger">{p.amount}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </AppLayout>
  );
};

export default ReceptionistDashboard;
