import React from 'react';
import { useNavigate } from 'react-router-dom';
import AppLayout from '@/components/AppLayout';
import KPICard from '@/components/KPICard';
import { AppointmentStatusBadge } from '@/components/StatusBadge';
import { CalendarDays, UserCheck, CreditCard, Plus } from 'lucide-react';
import { useAppointments } from '@/hooks/useAppointments';
import { useStaff } from '@/hooks/useAdmin';
import { useInvoices } from '@/hooks/useOptical';

const ReceptionistDashboard: React.FC = () => {
  const navigate = useNavigate();

  // Get today's ISO date string (YYYY-MM-DD)
  const today = new Date().toISOString().split('T')[0];

  // Dynamic calendar columns hours (8:00 to 17:00)
  const hours = Array.from({ length: 10 }, (_, i) => `${8 + i}:00`);

  // 1. Fetch dynamic doctors list (staff with DOCTOR role)
  const { data: doctorsData, isLoading: loadingDoctors } = useStaff({ role: 'DOCTOR' });
  const doctors = doctorsData?.data || [];
  const activeDoctors = doctors.slice(0, 3); // Display up to 3 doctors for layout neatness

  // 2. Fetch today's appointments live
  const { data: appointments = [], isLoading: loadingAppointments } = useAppointments(today, today);

  // 3. Fetch all invoices live (to compute outstanding balances)
  const { data: invoicesData, isLoading: loadingInvoices } = useInvoices({ page: 0, size: 100 });
  const outstandingInvoices = invoicesData?.data?.filter(inv => inv.status !== 'PAID') || [];

  // Helper: Find appointment starting in the given hour for a specific doctor
  const getAppointmentForDoctorAndHour = (doctorId: string, hourNum: number) => {
    return appointments.find(apt => {
      if (apt.doctorId !== doctorId) return false;
      const aptDate = new Date(apt.startAt);
      return aptDate.getHours() === hourNum;
    });
  };

  // Helper: Get doctor initials
  const getDoctorInitials = (d: any) => {
    const first = d.firstName ? d.firstName[0] : '';
    const last = d.lastName ? d.lastName[0] : '';
    return `${first}${last}`.toUpperCase() || 'DR';
  };

  // Helper: Calculate days outstanding since issued
  const getDaysOutstanding = (issuedAtStr?: string) => {
    if (!issuedAtStr) return 0;
    const diff = Date.now() - new Date(issuedAtStr).getTime();
    return Math.max(0, Math.floor(diff / (1000 * 60 * 60 * 24)));
  };

  // Helper: Format ISO time to short HH:MM
  const formatTime = (isoString: string) => {
    return new Date(isoString).toLocaleTimeString('ro-RO', { hour: '2-digit', minute: '2-digit' });
  };

  // Compute stats
  const totalCheckedIn = appointments.filter(a => ['CHECKED_IN', 'IN_PROGRESS', 'COMPLETED'].includes(a.status)).length;
  const totalOutstandingSum = outstandingInvoices.reduce((sum, inv) => sum + inv.total, 0);

  // Formatting date and currency
  const formattedToday = new Date().toLocaleDateString('ro-RO', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric'
  });

  const formattedOutstanding = new Intl.NumberFormat('ro-RO', {
    style: 'currency',
    currency: 'RON',
    maximumFractionDigits: 0
  }).format(totalOutstandingSum);

  if (loadingDoctors || loadingAppointments || loadingInvoices) {
    return (
      <AppLayout breadcrumbs={[{ label: 'Acasă' }, { label: 'Dashboard Recepție' }]}>
        <div className="min-h-[400px] flex items-center justify-center bg-background">
          <div className="flex flex-col items-center gap-3">
            <div className="w-8 h-8 rounded-full border-4 border-primary/20 border-t-primary animate-spin" />
            <p className="text-clinical-sm text-muted-foreground">Se încarcă datele recepției…</p>
          </div>
        </div>
      </AppLayout>
    );
  }

  return (
    <AppLayout breadcrumbs={[{ label: 'Acasă' }, { label: 'Dashboard Recepție' }]}>
      {/* KPI Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        <KPICard icon={CalendarDays} title="Programări azi" value={appointments.length.toString()} />
        <KPICard icon={UserCheck} title="Check-in realizate" value={totalCheckedIn.toString()} iconColor="#10B981" />
        <KPICard icon={CreditCard} title="Plăți restante" value={formattedOutstanding} iconColor="#C0392B" />
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-3 gap-6">
        {/* Day Calendar */}
        <div className="xl:col-span-2 bg-card rounded-xl border border-border shadow-sm overflow-hidden flex flex-col">
          <div className="p-4 border-b border-border flex items-center justify-between shrink-0">
            <h3 className="text-clinical-md font-semibold">Calendar zilnic — {formattedToday}</h3>
            <button 
              onClick={() => navigate('/appointments')}
              className="flex items-center gap-1.5 px-3 py-2 rounded-lg bg-primary text-white text-[12px] font-semibold hover:bg-primary-600 active:bg-primary-700 transition-colors"
            >
              <Plus className="w-3.5 h-3.5" /> Programare nouă
            </button>
          </div>
          <div className="overflow-auto max-h-[600px] flex-1">
            <table className="w-full border-collapse">
              <thead className="sticky top-0 z-10 bg-muted">
                <tr className="border-b border-border">
                  <th className="w-16 p-3 text-clinical-xs font-semibold text-muted-foreground text-right">ORA</th>
                  {activeDoctors.length === 0 ? (
                    <th className="p-3 text-clinical-xs font-semibold text-muted-foreground text-left">MEDIC</th>
                  ) : (
                    activeDoctors.map(d => (
                      <th key={d.id} className="p-3 text-clinical-xs font-semibold text-muted-foreground text-left">
                        <div className="flex items-center gap-2">
                          <div className="w-6 h-6 rounded-full bg-primary-100 text-primary-700 text-[10px] font-bold flex items-center justify-center">
                            {getDoctorInitials(d)}
                          </div>
                          <span className="truncate">{`Dr. ${d.firstName} ${d.lastName}`}</span>
                        </div>
                      </th>
                    ))
                  )}
                </tr>
              </thead>
              <tbody>
                {activeDoctors.length === 0 ? (
                  <tr>
                    <td colSpan={2} className="p-8 text-center text-clinical-sm text-muted-foreground">
                      Nu există medici activi înregistrați în clinică.
                    </td>
                  </tr>
                ) : (
                  hours.map((hour) => {
                    const hourNum = parseInt(hour.split(':')[0], 10);
                    return (
                      <tr key={hour} className="border-t border-border hover:bg-muted/30">
                        <td className="p-3 text-clinical-xs text-muted-foreground text-right font-clinical font-semibold select-none">{hour}</td>
                        {activeDoctors.map((doc) => {
                          const apt = getAppointmentForDoctorAndHour(doc.id, hourNum);
                          return (
                            <td key={doc.id} className="p-1.5 h-[56px] min-w-[150px] border-l border-border/50">
                              {apt && (
                                <div
                                  onClick={() => navigate('/appointments')}
                                  className="rounded-md p-2 border-l-[3px] cursor-pointer hover:brightness-95 active:scale-[0.99] transition-all h-full flex flex-col justify-center shadow-sm select-none"
                                  style={{
                                    borderLeftColor: apt.appointmentTypeColor || '#13759C',
                                    backgroundColor: (apt.appointmentTypeColor || '#13759C') + '1A',
                                  }}
                                  title={`${apt.patientName} (${apt.appointmentTypeName})`}
                                >
                                  <p className="text-clinical-xs font-semibold text-foreground truncate">{apt.patientName}</p>
                                  <p className="text-[10px] text-muted-foreground font-medium truncate">{apt.durationMinutes} min • {apt.appointmentTypeName}</p>
                                </div>
                              )}
                            </td>
                          );
                        })}
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* Right panel */}
        <div className="space-y-4">
          {/* Check-in queue */}
          <div className="bg-card rounded-xl border border-border shadow-sm flex flex-col max-h-[300px]">
            <div className="p-4 border-b border-border flex items-center justify-between shrink-0">
              <h3 className="text-clinical-md font-semibold">Coadă check-in</h3>
              <span className="text-[11px] bg-primary/10 text-primary-800 px-2 py-0.5 rounded-full font-bold">
                {appointments.filter(a => ['CONFIRMED', 'CHECKED_IN'].includes(a.status)).length}
              </span>
            </div>
            <div className="divide-y divide-border overflow-y-auto">
              {appointments.filter(a => ['CONFIRMED', 'CHECKED_IN'].includes(a.status)).length === 0 ? (
                <p className="p-6 text-clinical-sm text-muted-foreground text-center">Nicio programare confirmată sau în check-in azi.</p>
              ) : (
                appointments.filter(a => ['CONFIRMED', 'CHECKED_IN'].includes(a.status)).map(apt => (
                  <div key={apt.id} className="p-3 flex items-center gap-3 hover:bg-muted/20 transition-colors">
                    <div className="w-8 h-8 rounded-full bg-primary-100 text-primary-700 text-[11px] font-semibold flex items-center justify-center shrink-0">
                      {apt.patientName.split(' ').map(n => n[0]).join('')}
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-clinical-sm font-semibold truncate text-foreground">{apt.patientName}</p>
                      <p className="text-clinical-xs text-muted-foreground">{formatTime(apt.startAt)}</p>
                    </div>
                    <AppointmentStatusBadge status={apt.status} />
                  </div>
                ))
              )}
            </div>
          </div>

          {/* Outstanding payments */}
          <div className="bg-card rounded-xl border border-border shadow-sm flex flex-col max-h-[350px]">
            <div className="p-4 border-b border-border flex items-center justify-between shrink-0">
              <h3 className="text-clinical-md font-semibold">Plăți restante</h3>
              <span className="text-[11px] bg-destructive/10 text-destructive px-2 py-0.5 rounded-full font-bold">
                {outstandingInvoices.length}
              </span>
            </div>
            <div className="divide-y divide-border overflow-y-auto">
              {outstandingInvoices.length === 0 ? (
                <p className="p-6 text-clinical-sm text-muted-foreground text-center">Nu există facturi restante active în baza de date.</p>
              ) : (
                outstandingInvoices.slice(0, 5).map(inv => (
                  <div key={inv.id} className="p-3 flex items-center justify-between hover:bg-muted/20 transition-colors">
                    <div>
                      <p className="text-clinical-sm font-semibold text-foreground truncate max-w-[160px]">{inv.patientName || 'Pacient Înregistrat'}</p>
                      <p className="text-clinical-xs text-muted-foreground">
                        {getDaysOutstanding(inv.issuedAt) === 0 
                          ? 'Emisă astăzi' 
                          : `Depășit cu ${getDaysOutstanding(inv.issuedAt)} zile`}
                      </p>
                    </div>
                    <span className="text-clinical-sm font-bold text-clinical-danger shrink-0">
                      {new Intl.NumberFormat('ro-RO', { style: 'currency', currency: 'RON', maximumFractionDigits: 0 }).format(inv.total)}
                    </span>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      </div>
    </AppLayout>
  );
};

export default ReceptionistDashboard;
