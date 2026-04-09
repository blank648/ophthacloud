import React, { useState } from 'react';
import AppLayout from '@/components/AppLayout';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { revenueData, serviceMixData, topDiagnoses, doctors } from '@/data/demo-data';
import { AreaChart, Area, BarChart, Bar, LineChart, Line, PieChart, Pie, Cell, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend, ReferenceLine } from 'recharts';
import { Download, TrendingUp, TrendingDown, Target, AlertTriangle } from 'lucide-react';

const KPI: React.FC<{ label: string; value: string; target?: string; status?: 'good' | 'warn' | 'bad' }> = ({ label, value, target, status = 'good' }) => (
  <div className="bg-card rounded-xl border border-border shadow-sm p-4">
    <p className="text-clinical-xs text-muted-foreground font-semibold mb-1">{label}</p>
    <p className={`text-clinical-xl font-bold font-clinical ${status === 'good' ? 'text-green-600' : status === 'warn' ? 'text-amber-600' : 'text-red-600'}`}>{value}</p>
    {target && <p className="text-clinical-xs text-muted-foreground mt-1">Țintă: {target}</p>}
  </div>
);

const conversionFunnel = [
  { stage: 'Consultații', count: 187, pct: 100 },
  { stage: 'Rețete emise', count: 142, pct: 76 },
  { stage: 'Comenzi create', count: 98, pct: 52 },
  { stage: 'Finalizate', count: 84, pct: 45 },
];

const vaOutcomes = [
  { level: '6/6', pct: 42 },{ level: '6/9', pct: 28 },{ level: '6/12', pct: 15 },{ level: '6/18', pct: 8 },{ level: '6/24', pct: 4 },{ level: '<6/24', pct: 3 },
];

const consultTimeByDoctor = [
  { name: 'Dr. Popescu', avg: 32 },{ name: 'Dr. Mihailescu', avg: 28 },{ name: 'Optom. Radu', avg: 22 },
];

const stockOverview = { totalCost: 45600, totalRetail: 98400, lowStock: 2, slowMovers: 3, pendingOrders: 4 };

const ReportsPage: React.FC = () => {
  const [dateRange, setDateRange] = useState('Luna curentă');

  return (
    <AppLayout breadcrumbs={[{ label: 'Rapoarte & KPI' }]}>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-clinical-xl font-bold">Rapoarte & KPI</h1>
        <div className="flex items-center gap-2">
          {['Azi','Săptămâna','Luna curentă','Trimestrul','An'].map(r => (
            <button key={r} onClick={() => setDateRange(r)}
              className={`px-3 py-1.5 rounded-lg text-clinical-xs font-medium border transition-colors ${dateRange === r ? 'bg-primary text-white border-primary' : 'border-border hover:bg-muted'}`}>{r}</button>
          ))}
          <button className="px-3 py-1.5 rounded-lg border border-border text-clinical-xs font-medium flex items-center gap-1 hover:bg-muted"><Download className="w-3 h-3"/>Export</button>
        </div>
      </div>

      <Tabs defaultValue="clinical">
        <TabsList className="bg-card border border-border rounded-xl p-1 mb-6">
          <TabsTrigger value="clinical" className="text-clinical-sm">Clinici</TabsTrigger>
          <TabsTrigger value="commercial" className="text-clinical-sm">Comerciali</TabsTrigger>
          <TabsTrigger value="operational" className="text-clinical-sm">Operaționali</TabsTrigger>
          <TabsTrigger value="stock" className="text-clinical-sm">Stocuri</TabsTrigger>
        </TabsList>

        <TabsContent value="clinical">
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
            <KPI label="Completare investigații" value="62%" target="60%" status="good"/>
            <KPI label="BCVA ≥6/9 post-refracție" value="78%" target="80%" status="warn"/>
            <KPI label="Timp mediu consultație" value="28 min" target="25-35 min" status="good"/>
            <KPI label="Rată no-show" value="8.4%" target="<10%" status="good"/>
          </div>
          <div className="grid grid-cols-2 gap-6">
            <div className="bg-card rounded-xl border border-border shadow-sm p-4">
              <h3 className="text-clinical-sm font-semibold mb-3">VA post-refracție</h3>
              <ResponsiveContainer width="100%" height={220}>
                <BarChart data={vaOutcomes}><CartesianGrid strokeDasharray="3 3" stroke="hsl(var(--border))"/>
                  <XAxis dataKey="level" tick={{ fontSize: 11, fontFamily: 'JetBrains Mono' }}/>
                  <YAxis tick={{ fontSize: 10 }} unit="%"/>
                  <Tooltip/>
                  <Bar dataKey="pct" fill="hsl(var(--color-primary-500))" radius={[4,4,0,0]}/>
                </BarChart>
              </ResponsiveContainer>
            </div>
            <div className="bg-card rounded-xl border border-border shadow-sm p-4">
              <h3 className="text-clinical-sm font-semibold mb-3">Timp consultație per medic</h3>
              <ResponsiveContainer width="100%" height={220}>
                <BarChart data={consultTimeByDoctor} layout="vertical"><CartesianGrid strokeDasharray="3 3" stroke="hsl(var(--border))"/>
                  <XAxis type="number" unit=" min" tick={{ fontSize: 10 }}/>
                  <YAxis dataKey="name" type="category" width={120} tick={{ fontSize: 11 }}/>
                  <Tooltip/><Bar dataKey="avg" fill="hsl(var(--color-primary-400))" radius={[0,4,4,0]}/>
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>
        </TabsContent>

        <TabsContent value="commercial">
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
            <KPI label="Conversie rețetă→comandă" value="68%" target="70%" status="warn"/>
            <KPI label="Ticket mediu" value="285 RON"/>
            <KPI label="Revenue lunar" value="28.450 RON" status="good"/>
            <KPI label="Pacienți recurenți" value="52%" target="50%" status="good"/>
          </div>
          <div className="grid grid-cols-2 gap-6 mb-6">
            <div className="bg-card rounded-xl border border-border shadow-sm p-4">
              <h3 className="text-clinical-sm font-semibold mb-3">Trend Revenue (12 luni)</h3>
              <ResponsiveContainer width="100%" height={250}>
                <AreaChart data={revenueData}><CartesianGrid strokeDasharray="3 3" stroke="hsl(var(--border))"/>
                  <XAxis dataKey="month" tick={{ fontSize: 11 }}/>
                  <YAxis tick={{ fontSize: 10 }} tickFormatter={v => `${(v/1000).toFixed(0)}k`}/>
                  <Tooltip formatter={(v: number) => `${v.toLocaleString()} RON`}/>
                  <Legend/>
                  <Area type="monotone" dataKey="medical" stackId="1" fill="hsl(var(--color-primary-400))" stroke="hsl(var(--color-primary-600))" fillOpacity={0.4} name="Medical"/>
                  <Area type="monotone" dataKey="optical" stackId="1" fill="hsl(var(--color-accent-400))" stroke="hsl(var(--color-accent-600))" fillOpacity={0.4} name="Optic"/>
                </AreaChart>
              </ResponsiveContainer>
            </div>
            <div className="bg-card rounded-xl border border-border shadow-sm p-4">
              <h3 className="text-clinical-sm font-semibold mb-3">Funnel conversie</h3>
              <div className="space-y-3">
                {conversionFunnel.map((step, i) => (
                  <div key={step.stage}>
                    <div className="flex items-center justify-between text-clinical-xs mb-1">
                      <span className="font-medium">{step.stage}</span>
                      <span className="font-clinical font-semibold">{step.count} ({step.pct}%)</span>
                    </div>
                    <div className="h-6 rounded-md bg-muted overflow-hidden">
                      <div className="h-full rounded-md transition-all" style={{ width: `${step.pct}%`, background: i === 0 ? 'hsl(var(--color-primary-500))' : i === 1 ? 'hsl(var(--color-primary-400))' : i === 2 ? 'hsl(var(--color-accent-500))' : 'hsl(var(--color-success))' }}/>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
          <div className="bg-card rounded-xl border border-border shadow-sm p-4">
            <h3 className="text-clinical-sm font-semibold mb-3">Revenue per medic</h3>
            <table className="w-full text-clinical-sm">
              <thead><tr className="border-b border-border bg-muted/30"><th className="text-left p-2 text-clinical-xs text-muted-foreground">Medic</th><th className="text-right p-2 text-clinical-xs text-muted-foreground">Consultații</th><th className="text-right p-2 text-clinical-xs text-muted-foreground">Total RON</th></tr></thead>
              <tbody>{doctors.map(d => (
                <tr key={d.id} className="border-b border-border"><td className="p-2 font-semibold">{d.name}</td><td className="p-2 text-right font-clinical">{d.consultations}</td><td className="p-2 text-right font-clinical font-semibold">{d.revenue.toLocaleString()}</td></tr>
              ))}</tbody>
            </table>
          </div>
        </TabsContent>

        <TabsContent value="operational">
          <div className="grid grid-cols-2 gap-6 mb-6">
            <div className="bg-card rounded-xl border border-border shadow-sm p-4">
              <h3 className="text-clinical-sm font-semibold mb-3">Distribuție servicii</h3>
              <ResponsiveContainer width="100%" height={250}>
                <PieChart><Pie data={serviceMixData} cx="50%" cy="50%" innerRadius={60} outerRadius={100} paddingAngle={2} dataKey="value" label={({name,pct})=>`${name}`}>
                  {serviceMixData.map((entry,i) => <Cell key={i} fill={entry.color}/>)}
                </Pie><Tooltip/><Legend/></PieChart>
              </ResponsiveContainer>
            </div>
            <div className="bg-card rounded-xl border border-border shadow-sm p-4">
              <h3 className="text-clinical-sm font-semibold mb-3">Top 5 diagnostice</h3>
              <div className="space-y-3">
                {topDiagnoses.map(d => (
                  <div key={d.name}>
                    <div className="flex items-center justify-between text-clinical-xs mb-1">
                      <span className="font-medium">{d.name}</span>
                      <span className="font-clinical">{d.count} ({d.pct}%)</span>
                    </div>
                    <div className="h-4 rounded bg-muted overflow-hidden">
                      <div className="h-full rounded bg-primary/60" style={{ width: `${d.pct * 3}%` }}/>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </TabsContent>

        <TabsContent value="stock">
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
            <KPI label="Valoare stoc (cost)" value={`${stockOverview.totalCost.toLocaleString()} RON`}/>
            <KPI label="Valoare stoc (retail)" value={`${stockOverview.totalRetail.toLocaleString()} RON`}/>
            <KPI label="Sub stoc minim" value={String(stockOverview.lowStock)} status="warn"/>
            <KPI label="Fără mișcare >60 zile" value={String(stockOverview.slowMovers)} status="bad"/>
          </div>
          <div className="bg-card rounded-xl border border-border shadow-sm p-4">
            <h3 className="text-clinical-sm font-semibold mb-3">Sugestii reaprovizionare</h3>
            <table className="w-full text-clinical-sm">
              <thead><tr className="border-b border-border bg-muted/30">
                <th className="text-left p-2 text-clinical-xs text-muted-foreground">Articol</th>
                <th className="text-right p-2 text-clinical-xs text-muted-foreground">Stoc</th>
                <th className="text-right p-2 text-clinical-xs text-muted-foreground">Minim</th>
                <th className="text-left p-2 text-clinical-xs text-muted-foreground">Status</th>
              </tr></thead>
              <tbody>
                <tr className="border-b bg-amber-50/50"><td className="p-2">Silhouette SPX 1578</td><td className="p-2 text-right font-clinical">2</td><td className="p-2 text-right font-clinical">3</td><td className="p-2"><span className="text-amber-600 font-semibold text-clinical-xs">⚠ Sub minim</span></td></tr>
                <tr className="border-b bg-amber-50/50"><td className="p-2">Zeiss SmartLife Individual</td><td className="p-2 text-right font-clinical">4</td><td className="p-2 text-right font-clinical">5</td><td className="p-2"><span className="text-amber-600 font-semibold text-clinical-xs">⚠ Sub minim</span></td></tr>
                <tr className="border-b"><td className="p-2">Ray-Ban RB5154</td><td className="p-2 text-right font-clinical">8</td><td className="p-2 text-right font-clinical">3</td><td className="p-2"><span className="text-green-600 font-semibold text-clinical-xs">✓ OK</span></td></tr>
              </tbody>
            </table>
          </div>
        </TabsContent>
      </Tabs>
    </AppLayout>
  );
};

export default ReportsPage;
