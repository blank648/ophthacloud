import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AppLayout from '@/components/AppLayout';
import KPICard from '@/components/KPICard';
import { AppointmentStatusBadge, AppointmentTypeBadge, ClinicalFlagBadge, IOPValue } from '@/components/StatusBadge';
import { useAppointments } from '@/hooks/useAppointments';
import { useDashboardKpis } from '@/hooks/useReports';
import type { AppointmentStatus } from '@/types/appointments';
import { Stethoscope, Eye, FileText, DollarSign, AlertTriangle, MessageSquare, ChevronRight } from 'lucide-react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ReferenceLine, ResponsiveContainer, Legend } from 'recharts';

const DoctorDashboard: React.FC = () => {
  const navigate = useNavigate();
  const { data: kpis } = useDashboardKpis();
  const today = new Date().toISOString().split('T')[0];
  const { data: appointments = [] } = useAppointments(today, today);

  return (
    <AppLayout breadcrumbs={[{ label: 'Acasă' }, { label: 'Dashboard Medic' }]}>
      {/* KPI Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-4 mb-6">
        <KPICard icon={Stethoscope} title="Consultații azi" value={kpis?.todayAppointments.count.toString() || "0"} subtitle={`Efectuate: ${kpis?.todayAppointments.completed || 0}`} />
        <KPICard icon={Eye} title="Investigații pendinte" value="0" iconColor="#8B5CF6" />
        <KPICard icon={FileText} title="Rețete de semnat" value="0" iconColor="#F59E0B" />
        <KPICard icon={DollarSign} title="Revenue săptămână" value={`${kpis?.weekRevenue.amount || 0} ${kpis?.weekRevenue.currency || 'RON'}`} trend={{ value: `${kpis?.weekRevenue.trendPercent || 0}%`, positive: (kpis?.weekRevenue.trendPercent || 0) >= 0 }} iconColor="#10B981" />
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-5 gap-6 mb-6">
        {/* Today's appointments */}
        <div className="xl:col-span-3 bg-card rounded-xl border border-border shadow-sm">
          <div className="p-5 border-b border-border flex items-center justify-between">
            <h3 className="text-clinical-md font-semibold text-foreground">Programări azi</h3>
            <span className="text-clinical-xs text-muted-foreground">{appointments.length} programări</span>
          </div>
          <div className="divide-y divide-border">
            {appointments.length === 0 ? (
              <p className="p-5 text-clinical-sm text-muted-foreground">Nu aveți nicio programare astăzi.</p>
            ) : appointments.map((apt) => (
              <div key={apt.id} className="flex items-center gap-4 px-5 py-3 hover:bg-primary-50 transition-colors group">
                <div className="w-12 text-center">
                  <p className="text-clinical-sm font-semibold text-foreground">{new Date(apt.startTime).toLocaleTimeString('ro-RO', { hour: '2-digit', minute: '2-digit' })}</p>
                  <p className="text-clinical-xs text-muted-foreground">{apt.durationMinutes} min</p>
                </div>
                <div className="w-9 h-9 rounded-full bg-primary-100 flex items-center justify-center text-primary-700 text-[12px] font-semibold shrink-0">
                  {apt.patientName.split(' ').map(n => n[0]).join('')}
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-clinical-sm font-semibold text-foreground truncate">{apt.patientName}</p>
                  <p className="text-clinical-xs text-muted-foreground">{apt.patientId ? 'Inregistrat' : 'Nou'}</p>
                </div>
                <div className="flex items-center gap-2">
                  {/* <ClinicalFlagBadge flag="TODO" /> */}
                </div>
                <AppointmentTypeBadge name={apt.appointmentTypeName} color={apt.appointmentTypeColor} />
                <button>
                  <AppointmentStatusBadge status={apt.status} />
                </button>
                <button 
                  onClick={() => navigate(`/consultation?patientId=${apt.patientId}`)}
                  className="opacity-0 group-hover:opacity-100 flex items-center gap-1 px-3 py-1.5 rounded-md bg-primary text-white text-[11px] font-semibold transition-all"
                >
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
              {/* Alerte din backend se vor mapa aici */}
              <p className="text-clinical-sm text-muted-foreground">Fără alerte curente.</p>
            </div>
          </div>

          <div className="bg-card rounded-xl border border-border shadow-sm">
            <div className="p-4 border-b border-border flex items-center gap-2">
              <MessageSquare className="w-4 h-4 text-primary" />
              <h3 className="text-clinical-md font-semibold text-foreground">Mesaje portal</h3>
              <span className="ml-auto w-5 h-5 rounded-full bg-clinical-danger text-white text-[10px] font-bold flex items-center justify-center">0</span>
            </div>
            <div className="divide-y divide-border">
              <p className="p-4 text-clinical-sm text-muted-foreground">Nu există mesaje noi.</p>
            </div>
          </div>
        </div>
      </div>

      {/* IOP Trend Chart */}
      <div className="bg-card rounded-xl border border-border shadow-sm p-5">
        <h3 className="text-clinical-md font-semibold text-foreground mb-4">Tendință IOP (Date indisponibile temporar)</h3>
        <ResponsiveContainer width="100%" height={320}>
          <div className="w-full h-full flex items-center justify-center bg-muted/20 rounded-lg">
            <p className="text-clinical-sm text-muted-foreground">Graficul IOP necesită selectarea unui pacient.</p>
          </div>
        </ResponsiveContainer>
      </div>
    </AppLayout>
  );
};

export default DoctorDashboard;
