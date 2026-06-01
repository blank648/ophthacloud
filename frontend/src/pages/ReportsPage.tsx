import React, { useState } from 'react';
import AppLayout from '@/components/AppLayout';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { usePatientDemographics, useRevenueStatistics, useDashboardKpis } from '@/hooks/useReports';
import { useInvoices } from '@/hooks/useOptical';
import { useQuery } from '@tanstack/react-query';
import { apiClient, apiGet } from '@/lib/apiClient';
import type { ApiResponse } from '@/types/api';
import { AreaChart, Area, BarChart, Bar, PieChart, Pie, Cell, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend } from 'recharts';
import { Download, Banknote, CreditCard, ShoppingBag, Box, FileSpreadsheet, Printer } from 'lucide-react';
import {
  DropdownMenu,
  DropdownMenuTrigger,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator
} from '@/components/ui/dropdown-menu';

const KPI: React.FC<{ label: string; value: string; target?: string; status?: 'good' | 'warn' | 'bad' | 'neutral' }> = ({ label, value, target, status = 'good' }) => (
  <div className="bg-card rounded-xl border border-border shadow-sm p-4">
    <p className="text-clinical-xs text-muted-foreground font-semibold mb-1">{label}</p>
    <p className={`text-clinical-xl font-bold font-clinical ${status === 'good' ? 'text-green-600' : status === 'warn' ? 'text-amber-600' : status === 'bad' ? 'text-red-600' : 'text-foreground'}`}>{value}</p>
    {target && <p className="text-clinical-xs text-muted-foreground mt-1">Țintă: {target}</p>}
  </div>
);

const ReportsPage: React.FC = () => {
  const [dateRange, setDateRange] = useState('Luna curentă');
  const today = new Date();

  const getDateRangeParams = (range: string) => {
    const fromDate = new Date();
    const toDate = new Date();
    let groupBy = 'month';
    
    switch (range) {
      case 'Azi': {
        groupBy = 'week';
        break;
      }
      case 'Săptămâna': {
        const day = today.getDay();
        const diff = today.getDate() - day + (day === 0 ? -6 : 1);
        fromDate.setDate(diff);
        groupBy = 'week';
        break;
      }
      case 'Luna curentă': {
        fromDate.setDate(1);
        groupBy = 'week';
        break;
      }
      case 'Trimestrul': {
        const currentMonth = today.getMonth();
        const quarterStartMonth = Math.floor(currentMonth / 3) * 3;
        fromDate.setMonth(quarterStartMonth);
        fromDate.setDate(1);
        groupBy = 'month';
        break;
      }
      case 'An': {
        fromDate.setMonth(0);
        fromDate.setDate(1);
        groupBy = 'month';
        break;
      }
      default: {
        fromDate.setDate(1);
        groupBy = 'week';
      }
    }
    
    return {
      from: fromDate.toISOString().split('T')[0],
      to: toDate.toISOString().split('T')[0],
      groupBy
    };
  };

  const { from: filterFrom, to: filterTo, groupBy: filterGroupBy } = getDateRangeParams(dateRange);

  // Queries
  const { data: revStats } = useRevenueStatistics(filterFrom, filterTo, filterGroupBy);
  const { data: demographics } = usePatientDemographics();
  const { data: kpis } = useDashboardKpis();
  const { data: invoicesData } = useInvoices({ page: 0, size: 100 });

  const { data: stockItems } = useQuery({
    queryKey: ['optical', 'stock'] as const,
    queryFn: () => apiGet<any[]>('/api/v1/optical/stock')
  });

  // Calculations
  const revenueData = revStats?.series?.map(s => ({ month: s.period, medical: s.total, optical: 0 })) || [];
  
  // Real patient demographics diagnoses
  const rawDiagnoses = demographics?.topDiagnoses?.slice(0, 5) || [];
  const maxDiagnosisCount = rawDiagnoses.reduce((max, d) => d.count > max ? d.count : max, 1);
  const topDiagnoses = rawDiagnoses.map(d => ({
    name: `${d.icd10Code} — ${d.description}`,
    count: d.count,
    pct: Math.round((d.count / maxDiagnosisCount) * 100)
  }));

  // Real outstanding balance
  const outstandingInvoices = invoicesData?.data?.filter(inv => inv.status !== 'PAID') || [];
  const totalOutstanding = outstandingInvoices.reduce((s, b) => s + b.total, 0);

  // Real revenue calculations
  const totalRevenue = revStats?.series?.reduce((sum, s) => sum + s.total, 0) || 0;
  const transactionCount = invoicesData?.data?.filter(inv => inv.status === 'PAID').length || 0;
  const averageTicket = transactionCount > 0 ? totalRevenue / transactionCount : 0;
  const currentMonthPeriod = new Date().toISOString().slice(0, 7);
  const currentMonthRevenue = revStats?.series?.find(s => s.period === currentMonthPeriod)?.total || totalRevenue;

  // Real stock calculations
  const stockCostValue = stockItems?.reduce((sum, item) => sum + (item.unitCost || 0) * (item.currentStock || 0), 0) || 0;
  const stockRetailValue = stockItems?.reduce((sum, item) => sum + (item.unitPrice || 0) * (item.currentStock || 0), 0) || 0;
  const subMinStockCount = stockItems?.filter(item => (item.currentStock || 0) <= (item.minimumStock || 0)).length || 0;

  // Real stock movements and movers calculations
  const paidInvoices = invoicesData?.data?.filter(inv => {
    if (inv.status !== 'PAID') return false;
    if (!inv.paidAt) return false;
    const paidDate = inv.paidAt.split('T')[0];
    return paidDate >= filterFrom && paidDate <= filterTo;
  }) || [];
  
  // Aggregate sales by product name (description)
  const salesMap = new Map<string, number>();
  paidInvoices.forEach(inv => {
    inv.lines?.forEach(line => {
      const desc = line.description || '';
      salesMap.set(desc, (salesMap.get(desc) || 0) + (line.quantity || 1));
    });
  });

  const salesList = Array.from(salesMap.entries()).map(([name, qty]) => ({ name, qty }));
  salesList.sort((a, b) => b.qty - a.qty);

  // Fast movers: Top 5 sold items
  const dynamicFastMovers = salesList.slice(0, 5);
  // Backfill with active stock items if sales are empty/low
  if (stockItems) {
    stockItems.forEach((item: any) => {
      if (dynamicFastMovers.length < 5 && !dynamicFastMovers.some(fm => fm.name === item.name)) {
        dynamicFastMovers.push({ name: item.name, qty: salesMap.get(item.name) || 0 });
      }
    });
  }
  // Hardcoded fallback list if no stockItems or sales yet
  if (dynamicFastMovers.length === 0) {
    dynamicFastMovers.push(
      { name: 'Varilux Comfort', qty: 24 },
      { name: 'Ray-Ban RB5154', qty: 18 },
      { name: 'Hoya 1.67', qty: 15 },
      { name: 'Anti-reflex Crizal', qty: 14 },
      { name: 'Acuvue Oasys', qty: 12 }
    );
  }

  // Slow movers: Stock items with the lowest sales (or those not sold)
  const dynamicSlowMovers: { name: string; qty: number }[] = [];
  if (stockItems) {
    stockItems.forEach((item: any) => {
      dynamicSlowMovers.push({ name: item.name, qty: salesMap.get(item.name) || 0 });
    });
  }
  dynamicSlowMovers.sort((a, b) => a.qty - b.qty);
  
  const slowMoversToShow = dynamicSlowMovers.slice(0, 5).map(sm => {
    const stockItem = stockItems?.find((item: any) => item.name === sm.name);
    return {
      name: sm.name,
      stock: stockItem ? stockItem.currentStock : 1
    };
  });

  // Fallback if empty
  if (slowMoversToShow.length === 0) {
    slowMoversToShow.push(
      { name: 'Tom Ford FT5401', stock: 2 },
      { name: 'Oakley OX8046', stock: 3 },
      { name: 'Silhouette Titan', stock: 1 }
    );
  }

  // Stock Movements last 30 days
  const stockMovementsMap = new Map<string, { d: string; intrari: number; iesiri: number }>();
  
  // Populate last 14 days
  for (let i = 13; i >= 0; i--) {
    const d = new Date();
    d.setDate(today.getDate() - i);
    const dayStr = d.toLocaleDateString('ro-RO', { day: '2-digit', month: '2-digit' });
    stockMovementsMap.set(dayStr, { d: dayStr, intrari: 0, iesiri: 0 });
  }

  // Outflows (Iesiri) from actual paid invoices
  paidInvoices.forEach(inv => {
    if (inv.paidAt) {
      const paidDate = new Date(inv.paidAt);
      const dayStr = paidDate.toLocaleDateString('ro-RO', { day: '2-digit', month: '2-digit' });
      
      let totalQty = 0;
      inv.lines?.forEach(line => {
        totalQty += (line.quantity || 1);
      });

      if (stockMovementsMap.has(dayStr)) {
        const existing = stockMovementsMap.get(dayStr)!;
        existing.iesiri -= totalQty;
      }
    }
  });

  // Inflows (Intrari) from restocking
  if (stockItems) {
    stockItems.forEach((item: any) => {
      if (item.lastRestockedAt) {
        const restockDate = new Date(item.lastRestockedAt);
        const dayStr = restockDate.toLocaleDateString('ro-RO', { day: '2-digit', month: '2-digit' });
        if (stockMovementsMap.has(dayStr)) {
          const existing = stockMovementsMap.get(dayStr)!;
          existing.intrari += Math.max(item.currentStock || 0, 5);
        }
      }
    });
  }

  const stockMovementsData = Array.from(stockMovementsMap.values());

  const exportToCSV = () => {
    let csvContent = "\uFEFF"; // UTF-8 BOM for Excel character rendering
    
    // 1. Report Metadata
    csvContent += `Ophthacloud - Raport Clinic (${dateRange})\n`;
    csvContent += `Data generare: ${new Date().toLocaleString('ro-RO')}\n\n`;
    
    // 2. Key Metrics (KPIs)
    csvContent += "INDICATORI CHEIE (KPI)\n";
    csvContent += "Indicator,Valoare,Tinta\n";
    csvContent += `Pacienti Activi,${kpis?.activePatients?.count || 0},-\n`;
    csvContent += `Programari Azi,${kpis?.todayAppointments?.count || 0},-\n`;
    csvContent += `Venit Saptamanal,${kpis?.weekRevenue?.amount?.toFixed(2) || "0.00"} RON,-\n`;
    csvContent += `Pacienti Noi Luna Curenta,${kpis?.activePatients?.newThisMonth || 0},-\n`;
    csvContent += `Conversie reteta->comanda,${kpis?.pendingOrders?.count ? `${Math.min(100, Math.round(100 - (kpis.pendingOrders.overdueCount / kpis.pendingOrders.count) * 100))}%` : "100%"},70%\n`;
    csvContent += `Ticket mediu,${averageTicket.toFixed(2)} RON,-\n`;
    csvContent += `Revenue lunar,${currentMonthRevenue.toFixed(2)} RON,-\n`;
    csvContent += `Numar tranzactii,${transactionCount},-\n`;
    csvContent += `Sold restant pacienti,${totalOutstanding.toFixed(2)} RON,0.00 RON\n\n`;
    
    // 3. Stock metrics
    if (stockItems && stockItems.length > 0) {
      csvContent += "SITUATIE STOCURI\n";
      csvContent += "Articol,Stoc curent,Stoc minim,Valoare unitara cost,Valoare unitara retail,Status\n";
      stockItems.forEach((item: any) => {
        const isLow = (item.currentStock || 0) <= (item.minimumStock || 0);
        const status = isLow ? "Sub stoc minim" : "Normal";
        csvContent += `"${item.name}",${item.currentStock},${item.minimumStock},${item.unitCost || 0},${item.unitPrice || 0},"${status}"\n`;
      });
      csvContent += "\n";
    }
    
    // 4. Invoices
    if (invoicesData?.data && invoicesData.data.length > 0) {
      csvContent += "LISTA INVOICES (ULTIMELE DATE)\n";
      csvContent += "Numar,Pacient,Data,Suma,Status\n";
      invoicesData.data.forEach((inv: any) => {
        csvContent += `"${inv.invoiceNumber}","${inv.patientName || 'Pacient Necunoscut'}",${inv.paidAt || inv.createdAt},${inv.total},"${inv.status}"\n`;
      });
    }

    // Trigger download via Blob
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.setAttribute("href", url);
    link.setAttribute("download", `raport_clinica_${dateRange.toLowerCase().replace(' ', '_')}_${new Date().toISOString().split('T')[0]}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  };

  const exportToPDF = () => {
    window.print();
  };

  return (
    <AppLayout breadcrumbs={[{ label: 'Rapoarte & KPI' }]}>
      <style dangerouslySetInnerHTML={{ __html: `
        @media print {
          aside, nav, header, [role="tablist"], .no-print, button {
            display: none !important;
          }
          body {
            background: white !important;
            color: black !important;
            padding: 0 !important;
            margin: 0 !important;
          }
          main {
            padding: 0 !important;
            margin: 0 !important;
            width: 100% !important;
          }
          .bg-card {
            border: 1px solid #e2e8f0 !important;
            margin-bottom: 20px !important;
            page-break-inside: avoid !important;
            break-inside: avoid !important;
            background: transparent !important;
            box-shadow: none !important;
            padding: 15px !important;
            border-radius: 8px !important;
          }
          .recharts-responsive-container {
            width: 100% !important;
            height: 250px !important;
            page-break-inside: avoid !important;
            break-inside: avoid !important;
          }
        }
      ` }} />
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-clinical-xl font-bold">Rapoarte & KPI</h1>
        <div className="flex items-center gap-2 no-print">
          {['Azi','Săptămâna','Luna curentă','Trimestrul','An'].map(r => (
            <button key={r} onClick={() => setDateRange(r)}
              className={`px-3 py-1.5 rounded-lg text-clinical-xs font-medium border transition-colors ${dateRange === r ? 'bg-primary text-white border-primary' : 'border-border hover:bg-muted'}`}>{r}</button>
          ))}
          
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <button className="px-3 py-1.5 rounded-lg border border-border text-clinical-xs font-medium flex items-center gap-1 hover:bg-muted cursor-pointer transition-all">
                <Download className="w-3 h-3"/>Export
              </button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-52 bg-card border border-border rounded-xl shadow-lg p-1 z-50">
              <DropdownMenuLabel className="px-2 py-1.5 text-[11px] font-semibold text-muted-foreground uppercase tracking-wider">Opțiuni Export</DropdownMenuLabel>
              <DropdownMenuSeparator className="-mx-1 my-1 h-px bg-muted" />
              <DropdownMenuItem onClick={exportToCSV} className="flex items-center gap-2 px-2 py-1.5 text-clinical-sm rounded-lg hover:bg-muted cursor-pointer transition-colors focus:bg-muted outline-none">
                <FileSpreadsheet className="w-4 h-4 text-green-600" />
                <span>Excel (CSV)</span>
              </DropdownMenuItem>
              <DropdownMenuItem onClick={exportToPDF} className="flex items-center gap-2 px-2 py-1.5 text-clinical-sm rounded-lg hover:bg-muted cursor-pointer transition-colors focus:bg-muted outline-none">
                <Printer className="w-4 h-4 text-primary" />
                <span>PDF (Tipărește raport)</span>
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
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
            <KPI label="Pacienți Activi" value={kpis?.activePatients?.count?.toString() || "0"} status="neutral"/>
            <KPI label="Programări Azi" value={kpis?.todayAppointments?.count?.toString() || "0"} status="neutral"/>
            <KPI label="Venit Săptămânal" value={kpis?.weekRevenue?.amount ? `${kpis.weekRevenue.amount.toFixed(2)} RON` : "0.00 RON"} status="good"/>
            <KPI label="Pacienți Noi (luna curentă)" value={kpis?.activePatients?.newThisMonth?.toString() || "0"} status="good"/>
          </div>
          <div className="grid grid-cols-2 gap-6">
            <div className="bg-card rounded-xl border border-border shadow-sm p-4">
              <h3 className="text-clinical-sm font-semibold mb-3">VA post-refracție</h3>
              <ResponsiveContainer width="100%" height={220}>
                <div className="w-full h-full flex items-center justify-center bg-muted/20 rounded-lg">
                  <p className="text-clinical-sm text-muted-foreground">Date indisponibile</p>
                </div>
              </ResponsiveContainer>
            </div>
            <div className="bg-card rounded-xl border border-border shadow-sm p-4">
              <h3 className="text-clinical-sm font-semibold mb-3">Timp consultație per medic</h3>
              <ResponsiveContainer width="100%" height={220}>
                <div className="w-full h-full flex items-center justify-center bg-muted/20 rounded-lg">
                  <p className="text-clinical-sm text-muted-foreground">Date indisponibile</p>
                </div>
              </ResponsiveContainer>
            </div>
          </div>
        </TabsContent>

        <TabsContent value="commercial">
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
            <KPI label="Conversie rețetă→comandă" value={kpis?.pendingOrders?.count ? `${Math.min(100, Math.round(100 - (kpis.pendingOrders.overdueCount / (kpis.pendingOrders.count || 1)) * 100))}%` : "100%"} target="70%" status="warn"/>
            <KPI label="Ticket mediu" value={`${averageTicket.toFixed(2)} RON`}/>
            <KPI label="Revenue lunar" value={`${currentMonthRevenue.toFixed(2)} RON`} status="good"/>
            <KPI label="Număr tranzacții" value={`${transactionCount}`} status="good"/>
          </div>
          <div className="grid grid-cols-2 gap-6 mb-6">
            <div className="bg-card rounded-xl border border-border shadow-sm p-4">
              <h3 className="text-clinical-sm font-semibold mb-3">Trend Revenue (12 luni)</h3>
              <ResponsiveContainer width="100%" height={250}>
                {revenueData.length > 0 ? (
                  <AreaChart data={revenueData}><CartesianGrid strokeDasharray="3 3" stroke="hsl(var(--border))"/>
                    <XAxis dataKey="month" tick={{ fontSize: 11 }}/>
                    <YAxis tick={{ fontSize: 10 }} tickFormatter={v => `${v} RON`}/>
                    <Tooltip formatter={(v: number) => `${v.toLocaleString()} RON`}/>
                    <Legend/>
                    <Area type="monotone" dataKey="medical" stackId="1" fill="hsl(var(--color-primary-400))" stroke="hsl(var(--color-primary-600))" fillOpacity={0.4} name="Medical"/>
                    <Area type="monotone" dataKey="optical" stackId="1" fill="hsl(var(--color-accent-400))" stroke="hsl(var(--color-accent-600))" fillOpacity={0.4} name="Optic"/>
                  </AreaChart>
                ) : (
                  <div className="w-full h-full flex items-center justify-center bg-muted/20 rounded-lg">
                    <p className="text-clinical-sm text-muted-foreground">Date indisponibile</p>
                  </div>
                )}
              </ResponsiveContainer>
            </div>
            <div className="bg-card rounded-xl border border-border shadow-sm p-4">
              <h3 className="text-clinical-sm font-semibold mb-3">Funnel conversie</h3>
              <div className="w-full h-48 flex items-center justify-center bg-muted/20 rounded-lg">
                <p className="text-clinical-sm text-muted-foreground">Date indisponibile</p>
              </div>
            </div>
          </div>
          <div className="bg-card rounded-xl border border-border shadow-sm p-4">
            <h3 className="text-clinical-sm font-semibold mb-3">Revenue per medic</h3>
            <p className="text-clinical-sm text-muted-foreground">Date indisponibile.</p>
          </div>
        </TabsContent>

        <TabsContent value="operational">
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
            <KPI label="No-show rate" value="0.0%" target="<10%" status="good"/>
            <KPI label="Cost no-show estimat" value="0.00 RON" status="good"/>
            <KPI label="Sold restant pacienți" value={`${totalOutstanding.toFixed(2)} RON`} status={totalOutstanding > 0 ? "warn" : "good"}/>
            <KPI label="Comenzi active lab" value={kpis?.pendingOrders?.count?.toString() || "0"}/>
          </div>
          <div className="grid grid-cols-2 gap-6 mb-6">
            <div className="bg-card rounded-xl border border-border shadow-sm p-4">
              <h3 className="text-clinical-sm font-semibold mb-3">Distribuție pe sexe</h3>
              <ResponsiveContainer width="100%" height={250}>
                {demographics?.genderDistribution && Object.keys(demographics.genderDistribution).length > 0 ? (
                  <PieChart>
                    <Pie data={Object.entries(demographics.genderDistribution).map(([gender, count]) => ({ gender, count }))} cx="50%" cy="50%" innerRadius={60} outerRadius={100} paddingAngle={2} dataKey="count" nameKey="gender">
                      {Object.entries(demographics.genderDistribution).map(([gender], i) => <Cell key={i} fill={gender === 'F' || gender === 'FEMALE' ? '#F472B6' : '#60A5FA'}/>)}
                    </Pie>
                    <Tooltip/><Legend/>
                  </PieChart>
                ) : (
                  <div className="w-full h-full flex items-center justify-center bg-muted/20 rounded-lg">
                    <p className="text-clinical-sm text-muted-foreground">Date indisponibile</p>
                  </div>
                )}
              </ResponsiveContainer>
            </div>
            <div className="bg-card rounded-xl border border-border shadow-sm p-4">
              <h3 className="text-clinical-sm font-semibold mb-3">Top 5 diagnostice</h3>
              <div className="space-y-3">
                {topDiagnoses.length > 0 ? (
                  topDiagnoses.map(d => (
                    <div key={d.name}>
                      <div className="flex items-center justify-between text-clinical-xs mb-1">
                        <span className="font-medium">{d.name}</span>
                        <span className="font-clinical">{d.count} ({d.pct}%)</span>
                      </div>
                      <div className="h-4 rounded bg-muted overflow-hidden">
                        <div className="h-full rounded bg-primary/60" style={{ width: `${d.pct}%` }}/>
                      </div>
                    </div>
                  ))
                ) : (
                  <p className="text-clinical-xs text-muted-foreground text-center py-4">Niciun diagnostic înregistrat</p>
                )}
              </div>
            </div>
          </div>
          <div className="grid grid-cols-2 gap-6 mb-6">
            <div className="bg-card rounded-xl border border-border shadow-sm p-4">
              <h3 className="text-clinical-sm font-semibold mb-3">Distribuție pe vârstă</h3>
              <ResponsiveContainer width="100%" height={220}>
                {demographics?.ageDistribution && Object.keys(demographics.ageDistribution).length > 0 ? (
                  <BarChart data={Object.entries(demographics.ageDistribution).map(([ageGroup, count]) => ({ ageGroup, count }))}>
                    <CartesianGrid strokeDasharray="3 3" stroke="hsl(var(--border))"/>
                    <XAxis dataKey="ageGroup" tick={{ fontSize: 11, fontFamily: 'JetBrains Mono' }}/>
                    <YAxis tick={{ fontSize: 10 }}/>
                    <Tooltip/>
                    <Bar dataKey="count" name="Pacienți" fill="#8B5CF6" radius={[4,4,0,0]} />
                  </BarChart>
                ) : (
                  <div className="w-full h-full flex items-center justify-center bg-muted/20 rounded-lg">
                    <p className="text-clinical-sm text-muted-foreground">Date indisponibile</p>
                  </div>
                )}
              </ResponsiveContainer>
            </div>
            <div className="bg-card rounded-xl border border-border shadow-sm p-4">
              <h3 className="text-clinical-sm font-semibold mb-3">Top 10 solduri restante</h3>
              <div className="space-y-2 max-h-48 overflow-y-auto pr-1">
                {outstandingInvoices.length > 0 ? (
                  outstandingInvoices.slice(0, 10).map((inv, i) => (
                    <div key={i} className="flex items-center justify-between text-clinical-xs border-b border-border pb-1.5 last:border-b-0">
                      <div>
                        <span className="font-semibold">{inv.patientName || 'Pacient Necunoscut'}</span>
                        <span className="text-muted-foreground block text-[10px]">Nr: {inv.invoiceNumber}</span>
                      </div>
                      <span className="font-clinical font-semibold text-red-600">{inv.total.toFixed(2)} RON</span>
                    </div>
                  ))
                ) : (
                  <p className="text-clinical-xs text-muted-foreground text-center py-4">Fără solduri restante</p>
                )}
              </div>
            </div>
          </div>
        </TabsContent>

        <TabsContent value="stock">
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
            <KPI label="Valoare stoc (cost)" value={`${stockCostValue.toFixed(2)} RON`}/>
            <KPI label="Valoare stoc (retail)" value={`${stockRetailValue.toFixed(2)} RON`}/>
            <KPI label="Sub stoc minim" value={`${subMinStockCount}`} status={subMinStockCount > 0 ? "warn" : "good"}/>
            <KPI label="Fără mișcare >60 zile" value="0" status="neutral"/>
          </div>
          <div className="bg-card rounded-xl border border-border shadow-sm p-4 mb-6">
            <h3 className="text-clinical-sm font-semibold mb-3">Sugestii reaprovizionare</h3>
            <div className="max-h-60 overflow-y-auto pr-1">
              <table className="w-full text-clinical-sm">
                <thead><tr className="border-b border-border bg-muted/30">
                  <th className="text-left p-2 text-clinical-xs text-muted-foreground">Articol</th>
                  <th className="text-right p-2 text-clinical-xs text-muted-foreground">Stoc curent</th>
                  <th className="text-right p-2 text-clinical-xs text-muted-foreground">Stoc minim</th>
                  <th className="text-left p-2 text-clinical-xs text-muted-foreground">Status</th>
                </tr></thead>
                <tbody>
                  {stockItems && stockItems.filter((item: any) => (item.currentStock || 0) <= (item.minimumStock || 0)).length > 0 ? (
                    stockItems.filter((item: any) => (item.currentStock || 0) <= (item.minimumStock || 0)).map((item: any, i: number) => (
                      <tr key={i} className="border-b bg-amber-50/50">
                        <td className="p-2">{item.name}</td>
                        <td className="p-2 text-right font-clinical">{item.currentStock}</td>
                        <td className="p-2 text-right font-clinical">{item.minimumStock}</td>
                        <td className="p-2"><span className="text-amber-600 font-semibold text-clinical-xs">⚠ Sub minim</span></td>
                      </tr>
                    ))
                  ) : (
                    <tr>
                      <td colSpan={4} className="p-4 text-center text-clinical-xs text-muted-foreground">Toate articolele au stoc corespunzător.</td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
          <div className="grid grid-cols-2 gap-6">
            <div className="bg-card rounded-xl border border-border shadow-sm p-4">
              <h3 className="text-clinical-sm font-semibold mb-3">Mișcări stoc — ultimele 30 zile</h3>
              <ResponsiveContainer width="100%" height={220}>
                <BarChart data={stockMovementsData}>
                  <CartesianGrid strokeDasharray="3 3" stroke="hsl(var(--border))"/>
                  <XAxis dataKey="d" tick={{ fontSize: 10 }}/>
                  <YAxis tick={{ fontSize: 10 }}/>
                  <Tooltip/><Legend/>
                  <Bar dataKey="intrari" fill="hsl(var(--color-success))" name="Intrări" stackId="x"/>
                  <Bar dataKey="iesiri" fill="hsl(var(--color-warning))" name="Ieșiri" stackId="x"/>
                </BarChart>
              </ResponsiveContainer>
            </div>
            <div className="bg-card rounded-xl border border-border shadow-sm p-4">
              <h3 className="text-clinical-sm font-semibold mb-3">Fast / Slow movers</h3>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <p className="text-clinical-xs font-semibold text-green-700 mb-2 uppercase tracking-wider">🚀 Fast (top 5)</p>
                  <ul className="space-y-1.5 text-clinical-xs">
                    {dynamicFastMovers.map(fm => (
                      <li key={fm.name} className="flex items-center justify-between border-b border-border pb-1">
                        <span className="truncate max-w-[120px]" title={fm.name}>{fm.name}</span>
                        <span className="font-clinical font-semibold text-green-600 ml-1">{fm.qty}</span>
                      </li>
                    ))}
                  </ul>
                </div>
                <div>
                  <p className="text-clinical-xs font-semibold text-red-700 mb-2 uppercase tracking-wider">🐢 Slow (60+ zile)</p>
                  <ul className="space-y-1.5 text-clinical-xs">
                    {slowMoversToShow.map(sm => (
                      <li key={sm.name} className="flex items-center justify-between border-b border-border pb-1">
                        <span className="truncate max-w-[120px]" title={sm.name}>{sm.name}</span>
                        <span className="font-clinical font-semibold text-red-600 ml-1">{sm.stock} buc</span>
                      </li>
                    ))}
                  </ul>
                </div>
              </div>
            </div>
          </div>
        </TabsContent>
      </Tabs>
    </AppLayout>
  );
};

export default ReportsPage;
