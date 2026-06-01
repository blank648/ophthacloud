import React from 'react';
import AppLayout from '@/components/AppLayout';
import KPICard from '@/components/KPICard';
import { useDashboardKpis, useRevenueStatistics, usePatientDemographics } from '@/hooks/useReports';
import { DollarSign, TrendingUp, Users, Star, UserPlus } from 'lucide-react';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend,
  PieChart, Pie, Cell,
} from 'recharts';

const ManagerDashboard: React.FC = () => {
  const { data: kpis } = useDashboardKpis();
  const today = new Date();
  const lastYear = new Date();
  lastYear.setFullYear(today.getFullYear() - 1);
  const { data: revStats } = useRevenueStatistics(lastYear.toISOString().split('T')[0], today.toISOString().split('T')[0], 'month');
  const { data: demographics } = usePatientDemographics();

  const revenueData = revStats?.series?.map(s => ({ month: s.period, medical: s.total, optical: 0 })) || [];
  const topDiagnoses = demographics?.topDiagnoses?.slice(0, 5).map(d => ({ name: d.description, count: d.count })) || [];
  
  return (
  <AppLayout breadcrumbs={[{ label: 'Acasă' }, { label: 'Dashboard Manager' }]}>
    {/* KPI row */}
    <div className="grid grid-cols-1 md:grid-cols-3 xl:grid-cols-5 gap-4 mb-6">
      <KPICard icon={DollarSign} title="Revenue săptămână" value={`${kpis?.weekRevenue?.amount || 0} ${kpis?.weekRevenue?.currency || 'RON'}`} trend={{ value: `${kpis?.weekRevenue?.trendPercent || 0}%`, positive: (kpis?.weekRevenue?.trendPercent || 0) >= 0 }} iconColor="#10B981" />
      <KPICard icon={TrendingUp} title="Comenzi în așteptare" value={kpis?.pendingOrders?.count?.toString() || "0"} iconColor="#8B5CF6" />
      <KPICard icon={Star} title="Stoc redus" value={kpis?.lowStockItems?.count?.toString() || "0"} />
      <KPICard icon={Users} title="Pacienți activi" value={kpis?.activePatients?.count?.toString() || "0"} iconColor="#F59E0B" />
      <KPICard icon={UserPlus} title="Pacienți noi" value={kpis?.activePatients?.newThisMonth?.toString() || "0"} subtitle="Această lună" iconColor="#06B6D4" />
    </div>

    {/* Charts row */}
    <div className="grid grid-cols-1 xl:grid-cols-3 gap-6 mb-6">
      {/* Revenue bar chart */}
      <div className="xl:col-span-2 bg-card rounded-xl border border-border shadow-sm p-5">
        <h3 className="text-clinical-md font-semibold mb-4">Revenue 12 luni (Medical vs. Optic)</h3>
        <ResponsiveContainer width="100%" height={300}>
          <BarChart data={revenueData}>
            <CartesianGrid strokeDasharray="3 3" stroke="hsl(var(--color-border-subtle))" vertical={false} />
            <XAxis dataKey="month" tick={{ fontSize: 11 }} stroke="hsl(var(--color-text-muted))" />
            <YAxis tick={{ fontSize: 11 }} stroke="hsl(var(--color-text-muted))" />
            <Tooltip contentStyle={{ borderRadius: 8, fontSize: 12 }} />
            <Legend wrapperStyle={{ fontSize: 12 }} />
            <Bar dataKey="medical" name="Medical" fill="#13759C" radius={[4, 4, 0, 0]} />
            <Bar dataKey="optical" name="Optic" fill="#F5A020" radius={[4, 4, 0, 0]} />
          </BarChart>
        </ResponsiveContainer>
      </div>

      {/* Service distribution donut */}
      <div className="bg-card rounded-xl border border-border shadow-sm p-5">
        <h3 className="text-clinical-md font-semibold mb-4">Distribuție pe sexe</h3>
        <ResponsiveContainer width="100%" height={250}>
          <PieChart>
            <Pie data={Object.entries(demographics?.genderDistribution || {}).map(([gender, count]) => ({ gender, count }))} cx="50%" cy="50%" innerRadius={60} outerRadius={95} paddingAngle={3} dataKey="count" nameKey="gender">
              {Object.entries(demographics?.genderDistribution || {}).map(([gender], i) => (
                <Cell key={i} fill={gender === 'F' ? '#F472B6' : '#60A5FA'} />
              ))}
            </Pie>
            <Tooltip contentStyle={{ borderRadius: 8, fontSize: 12 }} />
          </PieChart>
        </ResponsiveContainer>
        <div className="flex flex-wrap gap-3 mt-2 justify-center">
          {Object.entries(demographics?.genderDistribution || {}).map(([gender, count]) => (
            <div key={gender} className="flex items-center gap-1.5">
              <div className="w-2.5 h-2.5 rounded-full" style={{ backgroundColor: gender === 'F' ? '#F472B6' : '#60A5FA' }} />
              <span className="text-clinical-xs text-muted-foreground">{gender}: {count}</span>
            </div>
          ))}
        </div>
      </div>
    </div>

    {/* Bottom row */}
    <div className="grid grid-cols-1 xl:grid-cols-2 gap-6">
      {/* Top diagnoses */}
      <div className="bg-card rounded-xl border border-border shadow-sm p-5">
        <h3 className="text-clinical-md font-semibold mb-4">Top 5 diagnostice</h3>
        <div className="space-y-3">
          {topDiagnoses.map((d, i) => (
            <div key={i} className="flex items-center gap-3">
              <span className="text-clinical-xs text-muted-foreground w-4">{i + 1}.</span>
              <div className="flex-1">
                <div className="flex items-center justify-between mb-1">
                  <span className="text-clinical-sm font-medium font-clinical text-foreground">{d.name}</span>
                  <span className="text-clinical-sm font-bold text-foreground">{d.count}</span>
                </div>
                <div className="w-full h-2 rounded-full bg-border">
                  <div className="h-2 rounded-full bg-primary" style={{ width: `${(d.count / 142) * 100}%` }} />
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Revenue per doctor */}
      <div className="bg-card rounded-xl border border-border shadow-sm p-5">
        <h3 className="text-clinical-md font-semibold mb-4">Revenue per medic</h3>
        <p className="text-clinical-sm text-muted-foreground">Date indisponibile (Așteptând modulul avansat de HR).</p>
      </div>
    </div>
  </AppLayout>
  );
};

export default ManagerDashboard;
