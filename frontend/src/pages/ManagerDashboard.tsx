import React from 'react';
import AppLayout from '@/components/AppLayout';
import KPICard from '@/components/KPICard';
import { revenueData, serviceMixData, topDiagnoses, doctors } from '@/data/demo-data';
import { DollarSign, TrendingUp, Users, Star, UserPlus } from 'lucide-react';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend,
  PieChart, Pie, Cell,
} from 'recharts';

const ManagerDashboard: React.FC = () => (
  <AppLayout breadcrumbs={[{ label: 'Acasă' }, { label: 'Dashboard Manager' }]}>
    {/* KPI row */}
    <div className="grid grid-cols-1 md:grid-cols-3 xl:grid-cols-5 gap-4 mb-6">
      <KPICard icon={DollarSign} title="Revenue lunar" value="90.200 RON" trend={{ value: '+15% vs. luna trecută', positive: true }} iconColor="#10B981" />
      <KPICard icon={TrendingUp} title="Revenue YTD" value="536.400 RON" iconColor="#8B5CF6" />
      <KPICard icon={Star} title="Conversie Rx→Optic" value="79.5%" trend={{ value: '+3.2%', positive: true }} />
      <KPICard icon={Users} title="Scor NPS" value="72" iconColor="#F59E0B" />
      <KPICard icon={UserPlus} title="Pacienți noi" value="34" subtitle="Această lună" iconColor="#06B6D4" />
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
        <h3 className="text-clinical-md font-semibold mb-4">Distribuție servicii</h3>
        <ResponsiveContainer width="100%" height={250}>
          <PieChart>
            <Pie data={serviceMixData} cx="50%" cy="50%" innerRadius={60} outerRadius={95} paddingAngle={3} dataKey="value">
              {serviceMixData.map((entry, i) => (
                <Cell key={i} fill={entry.color} />
              ))}
            </Pie>
            <Tooltip contentStyle={{ borderRadius: 8, fontSize: 12 }} />
          </PieChart>
        </ResponsiveContainer>
        <div className="flex flex-wrap gap-3 mt-2 justify-center">
          {serviceMixData.map(s => (
            <div key={s.name} className="flex items-center gap-1.5">
              <div className="w-2.5 h-2.5 rounded-full" style={{ backgroundColor: s.color }} />
              <span className="text-clinical-xs text-muted-foreground">{s.name}: {s.value}</span>
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
        <table className="w-full">
          <thead>
            <tr className="border-b-2 border-border">
              <th className="text-left text-clinical-xs font-semibold text-muted-foreground uppercase tracking-wider py-3">Medic</th>
              <th className="text-right text-clinical-xs font-semibold text-muted-foreground uppercase tracking-wider py-3">Consult.</th>
              <th className="text-right text-clinical-xs font-semibold text-muted-foreground uppercase tracking-wider py-3">Revenue</th>
            </tr>
          </thead>
          <tbody>
            {doctors.map((doc) => (
              <tr key={doc.id} className="border-b border-border hover:bg-primary-50 transition-colors">
                <td className="py-3">
                  <div className="flex items-center gap-2">
                    <div className="w-8 h-8 rounded-full bg-primary-100 text-primary-700 text-[11px] font-semibold flex items-center justify-center">
                      {doc.name.split(' ').slice(-1)[0][0]}{doc.name.split(' ')[1]?.[0]}
                    </div>
                    <div>
                      <p className="text-clinical-sm font-semibold">{doc.name}</p>
                      <p className="text-clinical-xs text-muted-foreground">{doc.specialty}</p>
                    </div>
                  </div>
                </td>
                <td className="text-right text-clinical-sm font-clinical">{doc.consultations}</td>
                <td className="text-right text-clinical-sm font-bold font-clinical text-primary-700">{doc.revenue.toLocaleString('ro-RO')} RON</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  </AppLayout>
);

export default ManagerDashboard;
