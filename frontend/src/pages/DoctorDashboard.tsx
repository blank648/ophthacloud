import React, { useState } from 'react';
import AppLayout from '@/components/AppLayout';
import KPICard from '@/components/KPICard';
import { AppointmentStatusBadge, AppointmentTypeBadge, ClinicalFlagBadge, IOPValue } from '@/components/StatusBadge';
import { todayAppointments, iopTrendData } from '@/data/demo-data';
import type { AppointmentStatus } from '@/data/demo-data';
import { Stethoscope, Eye, FileText, DollarSign, AlertTriangle, MessageSquare, ChevronRight } from 'lucide-react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ReferenceLine, ResponsiveContainer, Legend } from 'recharts';

const DoctorDashboard: React.FC = () => {
  const [appointments, setAppointments] = useState(todayAppointments);

  const cycleStatus = (id: string) => {
    const order: AppointmentStatus[] = ['booked', 'confirmed', 'checked_in', 'in_progress', 'completed'];
    setAppointments(prev => prev.map(a => {
      if (a.id !== id) return a;
      const idx = order.indexOf(a.status);
      if (idx === -1 || idx >= order.length - 1) return a;
      return { ...a, status: order[idx + 1] };
    }));
  };

  return (
    <AppLayout breadcrumbs={[{ label: 'Acasă' }, { label: 'Dashboard Medic' }]}>
      {/* KPI Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-4 mb-6">
        <KPICard icon={Stethoscope} title="Consultații azi" value="12" subtitle="Rămase: 4" trend={{ value: '+8% vs. săpt. trecută', positive: true }} />
        <KPICard icon={Eye} title="Investigații pendinte" value="3" iconColor="#8B5CF6" />
        <KPICard icon={FileText} title="Rețete de semnat" value="2" iconColor="#F59E0B" />
        <KPICard icon={DollarSign} title="Revenue azi" value="1.840 RON" trend={{ value: '+12% vs. ieri', positive: true }} iconColor="#10B981" />
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-5 gap-6 mb-6">
        {/* Today's appointments */}
        <div className="xl:col-span-3 bg-card rounded-xl border border-border shadow-sm">
          <div className="p-5 border-b border-border flex items-center justify-between">
            <h3 className="text-clinical-md font-semibold text-foreground">Programări azi — 29 Martie 2026</h3>
            <span className="text-clinical-xs text-muted-foreground">{appointments.length} programări</span>
          </div>
          <div className="divide-y divide-border">
            {appointments.map((apt) => (
              <div key={apt.id} className="flex items-center gap-4 px-5 py-3 hover:bg-primary-50 transition-colors group">
                <div className="w-12 text-center">
                  <p className="text-clinical-sm font-semibold text-foreground">{apt.time}</p>
                  <p className="text-clinical-xs text-muted-foreground">{apt.duration} min</p>
                </div>
                <div className="w-9 h-9 rounded-full bg-primary-100 flex items-center justify-center text-primary-700 text-[12px] font-semibold shrink-0">
                  {apt.patientName.split(' ').map(n => n[0]).join('')}
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-clinical-sm font-semibold text-foreground truncate">{apt.patientName}</p>
                  <p className="text-clinical-xs text-muted-foreground">{apt.patientAge} ani</p>
                </div>
                <div className="flex items-center gap-2">
                  {apt.clinicalFlags.map(f => <ClinicalFlagBadge key={f} flag={f} />)}
                </div>
                <AppointmentTypeBadge type={apt.type} />
                <button onClick={() => cycleStatus(apt.id)}>
                  <AppointmentStatusBadge status={apt.status} />
                </button>
                <button className="opacity-0 group-hover:opacity-100 flex items-center gap-1 px-3 py-1.5 rounded-md bg-primary text-white text-[11px] font-semibold transition-all">
                  EMR <ChevronRight className="w-3 h-3" />
                </button>
              </div>
            ))}
          </div>
        </div>

        {/* Right panel: Alerts + Messages */}
        <div className="xl:col-span-2 space-y-4">
          <div className="bg-card rounded-xl border border-border shadow-sm">
            <div className="p-4 border-b border-border flex items-center gap-2">
              <AlertTriangle className="w-4 h-4 text-clinical-warning" />
              <h3 className="text-clinical-md font-semibold text-foreground">Alerte clinice</h3>
            </div>
            <div className="p-4 space-y-3">
              {[
                { name: 'Ion Marinescu', flag: 'GLAUCOM', msg: 'Control anual depășit cu 3 luni', type: 'danger' as const },
                { name: 'Maria Popa', flag: 'DIABET', msg: 'Fără control retinopatie de 14 luni', type: 'warning' as const },
                { name: 'Andrei Ionescu', flag: 'NORMAL', msg: 'Rețeta expiră în 12 zile', type: 'info' as const },
              ].map((alert, i) => (
                <div
                  key={i}
                  className="p-3 rounded-lg border-l-4"
                  style={{
                    borderLeftColor: alert.type === 'danger' ? '#C0392B' : alert.type === 'warning' ? '#C97B00' : '#2563EB',
                    backgroundColor: alert.type === 'danger' ? '#FEF2F2' : alert.type === 'warning' ? '#FEF3C7' : '#EFF6FF',
                  }}
                >
                  <div className="flex items-center gap-2 mb-1">
                    <span className="text-clinical-sm font-semibold text-foreground">{alert.name}</span>
                    <ClinicalFlagBadge flag={alert.flag} />
                  </div>
                  <p className="text-clinical-xs text-muted-foreground">⚠ {alert.msg}</p>
                </div>
              ))}
            </div>
          </div>

          <div className="bg-card rounded-xl border border-border shadow-sm">
            <div className="p-4 border-b border-border flex items-center gap-2">
              <MessageSquare className="w-4 h-4 text-primary" />
              <h3 className="text-clinical-md font-semibold text-foreground">Mesaje portal</h3>
              <span className="ml-auto w-5 h-5 rounded-full bg-clinical-danger text-white text-[10px] font-bold flex items-center justify-center">2</span>
            </div>
            <div className="divide-y divide-border">
              {[
                { name: 'Elena Dumitrescu', msg: 'Bună ziua, am o întrebare despre tratamentul...', time: '2h' },
                { name: 'Ana Georgescu', msg: 'Doresc să reprogramez controlul de...', time: '5h' },
                { name: 'Mihai Stanescu', msg: 'Mulțumesc pentru consultație, am...', time: '1d' },
              ].map((m, i) => (
                <div key={i} className="p-4 hover:bg-primary-50 transition-colors cursor-pointer">
                  <div className="flex items-center justify-between mb-1">
                    <span className="text-clinical-sm font-semibold text-foreground">{m.name}</span>
                    <span className="text-clinical-xs text-muted-foreground">{m.time}</span>
                  </div>
                  <p className="text-clinical-xs text-muted-foreground truncate">{m.msg}</p>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* IOP Trend Chart */}
      <div className="bg-card rounded-xl border border-border shadow-sm p-5">
        <h3 className="text-clinical-md font-semibold text-foreground mb-4">Tendință IOP — Top 5 pacienți glaucom (12 luni)</h3>
        <ResponsiveContainer width="100%" height={320}>
          <LineChart data={iopTrendData}>
            <CartesianGrid strokeDasharray="3 3" stroke="hsl(var(--color-border-subtle))" vertical={false} />
            <XAxis dataKey="month" tick={{ fontSize: 11, fontFamily: 'JetBrains Mono' }} stroke="hsl(var(--color-text-muted))" />
            <YAxis domain={[10, 30]} tick={{ fontSize: 11, fontFamily: 'JetBrains Mono' }} stroke="hsl(var(--color-text-muted))" unit=" mmHg" />
            <Tooltip
              contentStyle={{ borderRadius: 8, boxShadow: 'var(--shadow-md)', fontFamily: 'JetBrains Mono', fontSize: 12 }}
            />
            <Legend wrapperStyle={{ fontSize: 12 }} />
            <ReferenceLine y={21} stroke="#C97B00" strokeDasharray="5 5" label={{ value: 'Normal max.', position: 'right', fontSize: 11, fill: '#C97B00' }} />
            <Line type="monotone" dataKey="Ion Marinescu" stroke="#C0392B" strokeWidth={2} dot={{ r: 3 }} />
            <Line type="monotone" dataKey="Mihai Stanescu" stroke="#13759C" strokeWidth={2} dot={{ r: 3 }} />
            <Line type="monotone" dataKey="Elena Dumitrescu" stroke="#10B981" strokeWidth={2} dot={{ r: 3 }} />
            <Line type="monotone" dataKey="Maria Popa" stroke="#8B5CF6" strokeWidth={2} dot={{ r: 3 }} />
            <Line type="monotone" dataKey="Ana Georgescu" stroke="#F59E0B" strokeWidth={2} dot={{ r: 3 }} />
          </LineChart>
        </ResponsiveContainer>
      </div>
    </AppLayout>
  );
};

export default DoctorDashboard;
