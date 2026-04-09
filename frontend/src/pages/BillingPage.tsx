import React, { useState } from 'react';
import AppLayout from '@/components/AppLayout';
import { serviceCatalog, patients } from '@/data/demo-data';
import { Plus, Trash2, CreditCard, Banknote, Building, FileText, Search } from 'lucide-react';

const BillingPage: React.FC = () => {
  const [selectedPatient, setSelectedPatient] = useState<string | null>('OC-004821');
  const [billItems, setBillItems] = useState<{ code: string; name: string; price: number }[]>([
    { code: 'MED002', name: 'Consultație follow-up oftalmolog', price: 100 },
    { code: 'INV001', name: 'OCT macular', price: 65 },
  ]);
  const [payMethod, setPayMethod] = useState<'cash' | 'card' | 'transfer'>('card');
  const [patientSearch, setPatientSearch] = useState('');
  const total = billItems.reduce((s, i) => s + i.price, 0);

  const outstandingBalances = [
    { patient: 'Ion Marinescu', amount: 250, days: 15 },
    { patient: 'Elena Dumitrescu', amount: 180, days: 8 },
    { patient: 'Andrei Popescu', amount: 65, days: 3 },
  ];

  return (
    <AppLayout breadcrumbs={[{ label: 'Facturare & POS' }]}>
      <h1 className="text-clinical-xl font-bold mb-6">Facturare & POS</h1>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Billing session */}
        <div className="lg:col-span-2 space-y-4">
          <div className="bg-card rounded-xl border border-border shadow-sm p-5">
            <h3 className="text-clinical-sm font-semibold mb-3">Sesiune facturare</h3>
            <div className="mb-4">
              <label className="text-clinical-xs text-muted-foreground block mb-1">Pacient</label>
              <div className="relative">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground"/>
                <input value={patientSearch} onChange={e=>setPatientSearch(e.target.value)} placeholder="Caută pacient..."
                  className="w-full pl-10 pr-3 py-2 rounded-lg border border-border bg-background text-clinical-sm"/>
              </div>
              {selectedPatient && <p className="text-clinical-sm font-semibold mt-2 text-primary">{patients.find(p=>p.id===selectedPatient)?.name} ({selectedPatient})</p>}
            </div>

            <div className="space-y-2 mb-4">
              {billItems.map((item, i) => (
                <div key={i} className="flex items-center gap-3 p-2 rounded-lg bg-muted/30">
                  <span className="font-clinical text-clinical-xs text-primary">{item.code}</span>
                  <span className="flex-1 text-clinical-sm">{item.name}</span>
                  <input type="number" value={item.price} onChange={e => setBillItems(prev => prev.map((b,ii) => ii===i?{...b,price:+e.target.value}:b))}
                    className="w-24 text-right font-clinical text-clinical-sm clinical-input rounded-md px-2 py-1"/>
                  <span className="text-clinical-xs text-muted-foreground">RON</span>
                  <button onClick={() => setBillItems(prev => prev.filter((_,ii) => ii!==i))}><Trash2 className="w-4 h-4 text-muted-foreground hover:text-red-500"/></button>
                </div>
              ))}
            </div>

            <div className="mb-4">
              <select onChange={e => {
                const svc = serviceCatalog.find(s => s.code === e.target.value);
                if (svc) setBillItems(prev => [...prev, { code: svc.code, name: svc.name, price: Math.round((svc.priceMin+svc.priceMax)/2) }]);
                e.target.value = '';
              }} className="w-full rounded-lg border border-border px-3 py-2 text-clinical-sm">
                <option value="">+ Adaugă serviciu din catalog...</option>
                {serviceCatalog.map(s => <option key={s.code} value={s.code}>{s.code} — {s.name} ({s.priceMin}-{s.priceMax} RON)</option>)}
              </select>
            </div>

            <div className="border-t border-border pt-4">
              <div className="flex items-center justify-between mb-3">
                <span className="text-clinical-md font-semibold">TOTAL</span>
                <span className="text-clinical-xl font-bold font-clinical">{total} RON</span>
              </div>
              <div className="flex gap-2 mb-4">
                {([['cash','Numerar',Banknote],['card','Card',CreditCard],['transfer','Transfer',Building]] as const).map(([key,label,Icon]) => (
                  <button key={key} onClick={() => setPayMethod(key)}
                    className={`flex-1 py-2 rounded-lg text-clinical-sm font-medium border flex items-center justify-center gap-1 transition-colors ${payMethod===key?'bg-primary text-white border-primary':'border-border hover:bg-muted'}`}>
                    <Icon className="w-4 h-4"/>{label}
                  </button>
                ))}
              </div>
              <div className="flex gap-2">
                <button className="flex-1 py-2.5 rounded-lg bg-primary text-white text-clinical-sm font-semibold flex items-center justify-center gap-1">
                  <FileText className="w-4 h-4"/> Emite Chitanță
                </button>
                <button className="flex-1 py-2.5 rounded-lg border border-border text-clinical-sm font-medium flex items-center justify-center gap-1 hover:bg-muted">
                  <FileText className="w-4 h-4"/> Emite Factură
                </button>
              </div>
            </div>
          </div>
        </div>

        {/* Outstanding */}
        <div className="space-y-4">
          <div className="bg-card rounded-xl border border-border shadow-sm p-5">
            <h3 className="text-clinical-sm font-semibold mb-3">Solduri restante</h3>
            <div className="space-y-3">
              {outstandingBalances.map((b,i) => (
                <div key={i} className="flex items-center justify-between p-3 rounded-lg bg-muted/30">
                  <div>
                    <p className="text-clinical-sm font-semibold">{b.patient}</p>
                    <p className="text-clinical-xs text-muted-foreground">{b.days} zile restanță</p>
                  </div>
                  <span className="font-clinical text-clinical-sm font-bold text-red-600">{b.amount} RON</span>
                </div>
              ))}
            </div>
            <div className="mt-3 pt-3 border-t border-border flex items-center justify-between">
              <span className="text-clinical-sm font-semibold">Total restant</span>
              <span className="font-clinical text-clinical-base font-bold text-red-600">{outstandingBalances.reduce((s,b)=>s+b.amount,0)} RON</span>
            </div>
          </div>

          <div className="bg-card rounded-xl border border-border shadow-sm p-5">
            <h3 className="text-clinical-sm font-semibold mb-3">Catalog servicii</h3>
            <div className="space-y-1 max-h-[300px] overflow-y-auto">
              {serviceCatalog.map(s => (
                <div key={s.code} className="flex items-center justify-between py-1.5 text-clinical-xs">
                  <div><span className="font-clinical text-primary">{s.code}</span> <span>{s.name}</span></div>
                  <span className="font-clinical text-muted-foreground">{s.priceMin}-{s.priceMax}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </AppLayout>
  );
};

export default BillingPage;
