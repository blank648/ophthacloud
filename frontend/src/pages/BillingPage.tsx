import React, { useState } from 'react';
import { toast } from 'sonner';
import AppLayout from '@/components/AppLayout';
import PrintPreviewModal from '@/components/PrintPreviewModal';
import { Plus, Trash2, CreditCard, Banknote, Building, FileText, Search, Loader2 } from 'lucide-react';
import { useClinicSettings } from '@/hooks/useAdmin';
import Big from 'big.js';
import { usePatients, usePatient } from '@/hooks/usePatients';
import { useServices, useInvoices, useCreateInvoice, usePayInvoice } from '@/hooks/useOptical';
import type { PatientSummaryDto } from '@/types/patients';
import type { InvoiceDto } from '@/types/optical';

const BillingPage: React.FC = () => {
  const { data: clinicData } = useClinicSettings();
  const [selectedPatient, setSelectedPatient] = useState<PatientSummaryDto | null>(null);
  const [billItems, setBillItems] = useState<{ id: string; code: string; name: string; price: number }[]>([]);
  const [payMethod, setPayMethod] = useState<'cash' | 'card' | 'transfer'>('card');
  const [patientSearch, setPatientSearch] = useState('');
  const [showDropdown, setShowDropdown] = useState(false);
  const [printDoc, setPrintDoc] = useState<null | { kind: 'receipt' | 'invoice'; invoice: InvoiceDto }>(null);
  const [invoiceCompany, setInvoiceCompany] = useState({ name: '', cif: '', address: '' });
  const [showInvoiceForm, setShowInvoiceForm] = useState(false);
  const [payingInvoice, setPayingInvoice] = useState<InvoiceDto | null>(null);
  const [payMethodForOutstanding, setPayMethodForOutstanding] = useState<'CASH' | 'CARD' | 'TRANSFER'>('CARD');

  // Queries
  const { data: patientsData, isLoading: loadingPatients } = usePatients({ q: patientSearch, page: 0, size: 10 });
  const { data: fullPatient } = usePatient(selectedPatient?.id);
  const { data: servicesData, isLoading: loadingServices } = useServices();
  const { data: invoicesData, isLoading: loadingInvoices, refetch: refetchInvoices } = useInvoices({ page: 0, size: 100 });

  // Mutations
  const createInvoiceMutation = useCreateInvoice();
  const payInvoiceMutation = usePayInvoice();

  const total = billItems.reduce((s, i) => s + i.price, 0);

  // Filter out unpaid/draft invoices for tenant outstanding balances
  const outstandingInvoices = invoicesData?.data?.filter(inv => inv.status !== 'PAID') || [];
  const outstandingBalances = outstandingInvoices.map(inv => {
    const days = inv.issuedAt ? Math.floor((new Date().getTime() - new Date(inv.issuedAt).getTime()) / (1000 * 60 * 60 * 24)) : 0;
    return {
      id: inv.id,
      patient: inv.patientName || 'Pacient Necunoscut',
      amount: inv.total,
      days: Math.max(0, days),
      invoice: inv
    };
  });

  const isEmitting = createInvoiceMutation.isPending || payInvoiceMutation.isPending;

  const handleEmitReceipt = async () => {
    if (!selectedPatient || billItems.length === 0) {
      toast.error('Selectați un pacient și adăugați servicii.');
      return;
    }

    try {
      // 1. Create invoice in DRAFT status with POS items
      const newInvoice = await createInvoiceMutation.mutateAsync({
        patientId: selectedPatient.id,
        items: billItems.map(item => ({
          serviceItemId: item.id,
          description: item.name,
          quantity: 1,
          unitPrice: item.price,
          vatRate: 19.00
        }))
      });

      // 2. Pay invoice immediately via PATCH payment
      const paidInvoice = await payInvoiceMutation.mutateAsync({
        id: newInvoice.id,
        paymentMethod: payMethod.toUpperCase()
      });

      // 3. Render print view & reset session
      setPrintDoc({ kind: 'receipt', invoice: paidInvoice });
      toast.success('Chitanță emisă cu succes!', { description: paidInvoice.invoiceNumber });
      
      // Cleanup states
      setBillItems([]);
      setSelectedPatient(null);
      setPatientSearch('');
      refetchInvoices();
    } catch (error: any) {
      toast.error(error?.message || 'Eroare la emiterea chitanței.');
    }
  };

  const handleEmitInvoice = async () => {
    if (!selectedPatient || billItems.length === 0) {
      toast.error('Selectați un pacient și adăugați servicii.');
      return;
    }

    try {
      const companyNotes = invoiceCompany.name 
        ? `Factură PJ: ${invoiceCompany.name}, CIF: ${invoiceCompany.cif}, Adresă: ${invoiceCompany.address}`
        : 'Factură PF';

      // 1. Create invoice in DRAFT status with PJ notes
      const newInvoice = await createInvoiceMutation.mutateAsync({
        patientId: selectedPatient.id,
        items: billItems.map(item => ({
          serviceItemId: item.id,
          description: item.name,
          quantity: 1,
          unitPrice: item.price,
          vatRate: 19.00
        }))
      });

      // 2. Pay invoice immediately via PATCH payment
      const paidInvoice = await payInvoiceMutation.mutateAsync({
        id: newInvoice.id,
        paymentMethod: payMethod.toUpperCase()
      });

      // 3. Render print view & reset session
      setPrintDoc({ kind: 'invoice', invoice: paidInvoice });
      toast.success('Factură emisă cu succes!', { description: paidInvoice.invoiceNumber });
      
      // Cleanup states
      setBillItems([]);
      setSelectedPatient(null);
      setPatientSearch('');
      setShowInvoiceForm(false);
      setInvoiceCompany({ name: '', cif: '', address: '' });
      refetchInvoices();
    } catch (error: any) {
      toast.error(error?.message || 'Eroare la emiterea facturii.');
    }
  };

  return (
    <AppLayout breadcrumbs={[{ label: 'Facturare & POS' }]}>
      <h1 className="text-clinical-xl font-bold mb-6">Facturare & POS</h1>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Billing session */}
        <div className="lg:col-span-2 space-y-4">
          <div className="bg-card rounded-xl border border-border shadow-sm p-5">
            <h3 className="text-clinical-sm font-semibold mb-3">Sesiune facturare</h3>
            
            {/* Patient Search */}
            <div className="mb-4 relative">
              <label className="text-clinical-xs text-muted-foreground block mb-1">Pacient</label>
              <div className="relative">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground"/>
                <input 
                  value={patientSearch} 
                  onChange={e => {
                    setPatientSearch(e.target.value);
                    setShowDropdown(true);
                  }}
                  onFocus={() => setShowDropdown(true)}
                  placeholder="Caută pacient..."
                  className="w-full pl-10 pr-3 py-2 rounded-lg border border-border bg-background text-clinical-sm focus:outline-none focus:ring-1 focus:ring-primary"
                />
              </div>
              
              {showDropdown && patientSearch.trim().length > 0 && (
                <div className="absolute left-0 right-0 mt-1 bg-card border border-border rounded-lg shadow-lg max-h-48 overflow-y-auto z-10">
                  {loadingPatients ? (
                    <div className="p-3 text-clinical-xs text-muted-foreground">Se încarcă...</div>
                  ) : patientsData && patientsData.data.length > 0 ? (
                    patientsData.data.map(p => (
                      <div 
                        key={p.id} 
                        onClick={() => {
                          setSelectedPatient(p);
                          setPatientSearch('');
                          setShowDropdown(false);
                        }}
                        className="p-2.5 hover:bg-muted cursor-pointer transition-colors border-b border-border last:border-b-0 text-clinical-sm"
                      >
                        <div className="font-semibold text-foreground">{p.firstName} {p.lastName}</div>
                        <div className="text-clinical-xs text-muted-foreground">{p.mrn} · {p.phone || 'Fără telefon'}</div>
                      </div>
                    ))
                  ) : (
                    <div className="p-3 text-clinical-xs text-muted-foreground">Niciun pacient găsit</div>
                  )}
                </div>
              )}

              {selectedPatient && (
                <div className="mt-2 p-3 rounded-lg bg-primary/5 border border-primary/20 flex items-center justify-between">
                  <div>
                    <span className="text-clinical-sm font-semibold text-primary">{selectedPatient.firstName} {selectedPatient.lastName}</span>
                    <span className="text-clinical-xs text-muted-foreground block">MRN: {selectedPatient.mrn}</span>
                  </div>
                  <button 
                    onClick={() => {
                      setSelectedPatient(null);
                      setPatientSearch('');
                    }} 
                    className="text-clinical-xs text-muted-foreground hover:text-red-500 underline"
                  >
                    Schimbă
                  </button>
                </div>
              )}
            </div>

            {/* Billing Lines */}
            <div className="space-y-2 mb-4">
              {billItems.map((item, i) => (
                <div key={i} className="flex items-center gap-3 p-2 rounded-lg bg-muted/30">
                  <span className="font-clinical text-clinical-xs text-primary">{item.code}</span>
                  <span className="flex-1 text-clinical-sm">{item.name}</span>
                  <input 
                    type="number" 
                    value={item.price} 
                    onChange={e => setBillItems(prev => prev.map((b, ii) => ii === i ? { ...b, price: +e.target.value } : b))}
                    className="w-24 text-right font-clinical text-clinical-sm clinical-input rounded-md px-2 py-1 bg-background border border-border"
                  />
                  <span className="text-clinical-xs text-muted-foreground">RON</span>
                  <button onClick={() => setBillItems(prev => prev.filter((_, ii) => ii !== i))}>
                    <Trash2 className="w-4 h-4 text-muted-foreground hover:text-red-500"/>
                  </button>
                </div>
              ))}
            </div>

            {/* Service selector */}
            <div className="mb-4">
              <select 
                onChange={e => {
                  const svc = servicesData?.find(s => s.id === e.target.value);
                  if (svc) {
                    setBillItems(prev => [...prev, { id: svc.id, code: svc.sku || 'SRV', name: svc.name, price: svc.unitPrice }]);
                  }
                  e.target.value = '';
                }} 
                className="w-full rounded-lg border border-border px-3 py-2 text-clinical-sm bg-background"
                disabled={loadingServices}
              >
                <option value="">{loadingServices ? 'Se încarcă catalogul...' : '+ Adaugă serviciu din catalog...'}</option>
                {servicesData?.map(s => (
                  <option key={s.id} value={s.id}>
                    {s.sku || 'SRV'} — {s.name} ({s.unitPrice} RON)
                  </option>
                ))}
              </select>
            </div>

            {/* Totals & Payments */}
            <div className="border-t border-border pt-4">
              <div className="flex items-center justify-between mb-3">
                <span className="text-clinical-md font-semibold">TOTAL</span>
                <span className="text-clinical-xl font-bold font-clinical">{total.toFixed(2)} RON</span>
              </div>
              <div className="flex gap-2 mb-4">
                {([['cash', 'Numerar', Banknote], ['card', 'Card', CreditCard], ['transfer', 'Transfer', Building]] as const).map(([key, label, Icon]) => (
                  <button 
                    key={key} 
                    onClick={() => setPayMethod(key)}
                    className={`flex-1 py-2 rounded-lg text-clinical-sm font-medium border flex items-center justify-center gap-1 transition-colors ${payMethod === key ? 'bg-primary text-white border-primary' : 'border-border hover:bg-muted'}`}
                  >
                    <Icon className="w-4 h-4"/>{label}
                  </button>
                ))}
              </div>
              
              <div className="flex gap-2">
                <button
                  disabled={isEmitting}
                  onClick={handleEmitReceipt}
                  className="flex-1 py-2.5 rounded-lg bg-primary text-white text-clinical-sm font-semibold flex items-center justify-center gap-1 hover:bg-primary/95 disabled:opacity-50"
                >
                  {isEmitting ? <Loader2 className="w-4 h-4 animate-spin" /> : <FileText className="w-4 h-4"/>}
                  Emite Chitanță
                </button>
                <button
                  disabled={isEmitting}
                  onClick={() => {
                    if (!selectedPatient || billItems.length === 0) {
                      toast.error('Selectați un pacient și adăugați servicii.');
                      return;
                    }
                    setShowInvoiceForm(true);
                  }}
                  className="flex-1 py-2.5 rounded-lg border border-border text-clinical-sm font-medium flex items-center justify-center gap-1 hover:bg-muted disabled:opacity-50"
                >
                  <FileText className="w-4 h-4"/> Emite Factură
                </button>
              </div>
            </div>
          </div>

          {/* Panel Istoric Documente Emise */}
          <div className="bg-card rounded-xl border border-border shadow-sm p-5">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-clinical-sm font-semibold">Istoric Documente Emise (Facturi & Chitanțe)</h3>
              <span className="text-clinical-xs text-muted-foreground bg-muted rounded-full px-2 py-0.5 font-clinical">
                {invoicesData?.data?.length || 0} documente
              </span>
            </div>

            {loadingInvoices ? (
              <div className="text-clinical-xs text-muted-foreground py-10 flex justify-center items-center">
                <Loader2 className="w-5 h-5 animate-spin mr-2 text-primary" /> Se încarcă istoricul...
              </div>
            ) : invoicesData?.data && invoicesData.data.length > 0 ? (
              <div className="overflow-y-auto max-h-[400px] pr-1">
                <table className="w-full text-clinical-sm border-collapse">
                  <thead>
                    <tr className="border-b border-border text-muted-foreground text-clinical-xs text-left">
                      <th className="pb-2 font-semibold">Număr Doc</th>
                      <th className="pb-2 font-semibold">Pacient</th>
                      <th className="pb-2 font-semibold">Dată Emitere</th>
                      <th className="pb-2 font-semibold text-center">Status</th>
                      <th className="pb-2 font-semibold text-right">Total</th>
                      <th className="pb-2 font-semibold text-center">Plată</th>
                      <th className="pb-2 font-semibold text-right">Acțiuni</th>
                    </tr>
                  </thead>
                  <tbody>
                    {invoicesData.data.map(inv => (
                      <tr key={inv.id} className="border-b border-border/50 hover:bg-muted/30 last:border-0 transition-colors">
                        <td className="py-2.5 font-clinical text-clinical-xs text-primary font-semibold">
                          {inv.invoiceNumber}
                        </td>
                        <td className="py-2.5 font-medium">{inv.patientName || 'Pacient Necunoscut'}</td>
                        <td className="py-2.5 text-muted-foreground text-clinical-xs">
                          {inv.issuedAt ? new Date(inv.issuedAt).toLocaleString('ro-RO') : '—'}
                        </td>
                        <td className="py-2.5 text-center">
                          <div className="flex flex-col items-center gap-1">
                            <span className={`px-2.5 py-0.5 rounded-full text-[10px] font-semibold border ${
                              inv.status === 'PAID'
                                ? 'bg-green-50/50 dark:bg-green-950/20 text-green-600 dark:text-green-400 border-green-200 dark:border-green-900'
                                : 'bg-amber-50/50 dark:bg-amber-950/20 text-amber-600 dark:text-amber-400 border-amber-200 dark:border-amber-900'
                            }`}>
                              {inv.status === 'PAID' ? 'Plătită' : 'Restantă'}
                            </span>
                            {inv.status !== 'PAID' && (
                              <button
                                onClick={() => setPayingInvoice(inv)}
                                className="text-[10px] font-bold text-primary hover:underline flex items-center gap-0.5"
                              >
                                Încasează
                              </button>
                            )}
                          </div>
                        </td>
                        <td className="py-2.5 text-right font-clinical font-semibold">{inv.total.toFixed(2)} RON</td>
                        <td className="py-2.5 text-center text-clinical-xs text-muted-foreground capitalize">
                          {inv.paymentMethod ? (inv.paymentMethod.toLowerCase() === 'cash' ? 'Numerar' : inv.paymentMethod.toLowerCase() === 'card' ? 'Card' : inv.paymentMethod) : '—'}
                        </td>
                        <td className="py-2.5 text-right">
                          <button
                            onClick={() => setPrintDoc({ kind: inv.invoiceNumber.startsWith('FACT') ? 'invoice' : 'receipt', invoice: inv })}
                            className="text-primary hover:underline text-clinical-xs font-semibold flex items-center gap-1 ml-auto"
                          >
                            <FileText className="w-3.5 h-3.5" /> Vizualizează
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <div className="text-clinical-xs text-muted-foreground text-center py-10 border border-dashed border-border/40 rounded-xl">
                Nu s-a găsit niciun document emis în istoric.
              </div>
            )}
          </div>
        </div>

        {/* Outstanding sidebar */}
        <div className="space-y-4">
          <div className="bg-card rounded-xl border border-border shadow-sm p-5">
            <h3 className="text-clinical-sm font-semibold mb-3">Solduri restante</h3>
            <div className="space-y-3 max-h-[250px] overflow-y-auto pr-1">
              {loadingInvoices ? (
                <div className="text-clinical-xs text-muted-foreground p-3">Se încarcă restanțele...</div>
              ) : outstandingBalances.length > 0 ? (
                outstandingBalances.map((b, i) => (
                  <div key={i} className="flex items-center justify-between p-3 rounded-lg bg-muted/30 animate-in fade-in">
                    <div>
                      <p className="text-clinical-sm font-semibold">{b.patient}</p>
                      <p className="text-clinical-xs text-muted-foreground">{b.days} zile restanță</p>
                      <button 
                        onClick={() => setPayingInvoice(b.invoice)}
                        className="text-[10px] font-bold text-primary hover:underline mt-1.5 flex items-center gap-0.5"
                      >
                        <CreditCard className="w-3 h-3" /> Încasează Plată
                      </button>
                    </div>
                    <span className="font-clinical text-clinical-sm font-bold text-red-600">{b.amount.toFixed(2)} RON</span>
                  </div>
                ))
              ) : (
                <div className="text-clinical-xs text-muted-foreground text-center py-4">Fără solduri restante</div>
              )}
            </div>
            <div className="mt-3 pt-3 border-t border-border flex items-center justify-between">
              <span className="text-clinical-sm font-semibold">Total restant</span>
              <span className="font-clinical text-clinical-base font-bold text-red-600">
                {outstandingBalances.reduce((s, b) => s + b.amount, 0).toFixed(2)} RON
              </span>
            </div>
          </div>

          <div className="bg-card rounded-xl border border-border shadow-sm p-5">
            <h3 className="text-clinical-sm font-semibold mb-3">Catalog servicii</h3>
            <div className="space-y-1 max-h-[300px] overflow-y-auto pr-1">
              {loadingServices ? (
                <div className="text-clinical-xs text-muted-foreground p-2">Se încarcă catalogul...</div>
              ) : servicesData && servicesData.length > 0 ? (
                servicesData.map(s => (
                  <div key={s.id} className="flex items-center justify-between py-1.5 text-clinical-xs border-b border-border/50 last:border-0">
                    <div>
                      <span className="font-clinical text-primary mr-1.5">{s.sku || 'SRV'}</span> 
                      <span>{s.name}</span>
                    </div>
                    <span className="font-clinical text-muted-foreground shrink-0 ml-2">{s.unitPrice.toFixed(2)} RON</span>
                  </div>
                ))
              ) : (
                <div className="text-clinical-xs text-muted-foreground py-2">Catalog gol</div>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Invoice details modal */}
      {showInvoiceForm && (
        <div className="fixed inset-0 bg-black/40 z-50 flex items-center justify-center p-4" onClick={() => setShowInvoiceForm(false)}>
          <div className="bg-card rounded-2xl shadow-xl max-w-md w-full p-6" onClick={e => e.stopPropagation()}>
            <h3 className="text-clinical-md font-semibold mb-1">Date facturare</h3>
            <p className="text-clinical-xs text-muted-foreground mb-4">Persoană juridică (opțional — lăsați gol pentru persoană fizică).</p>
            <div className="space-y-3">
              <div>
                <label className="text-clinical-xs text-muted-foreground block mb-1">Denumire firmă</label>
                <input 
                  value={invoiceCompany.name} 
                  onChange={e => setInvoiceCompany(c => ({ ...c, name: e.target.value }))}
                  className="w-full rounded-lg border border-border px-3 py-2 text-clinical-sm bg-background focus:outline-none focus:ring-1 focus:ring-primary" 
                />
              </div>
              <div>
                <label className="text-clinical-xs text-muted-foreground block mb-1">CIF</label>
                <input 
                  value={invoiceCompany.cif} 
                  onChange={e => setInvoiceCompany(c => ({ ...c, cif: e.target.value }))}
                  className="w-full rounded-lg border border-border px-3 py-2 text-clinical-sm bg-background focus:outline-none focus:ring-1 focus:ring-primary" 
                />
              </div>
              <div>
                <label className="text-clinical-xs text-muted-foreground block mb-1">Adresă</label>
                <input 
                  value={invoiceCompany.address} 
                  onChange={e => setInvoiceCompany(c => ({ ...c, address: e.target.value }))}
                  className="w-full rounded-lg border border-border px-3 py-2 text-clinical-sm bg-background focus:outline-none focus:ring-1 focus:ring-primary" 
                />
              </div>
            </div>
            <div className="flex gap-2 mt-5">
              <button onClick={() => setShowInvoiceForm(false)} className="flex-1 py-2 rounded-lg border border-border text-clinical-sm font-medium hover:bg-muted">Anulează</button>
              <button 
                onClick={handleEmitInvoice} 
                className="flex-1 py-2 rounded-lg bg-primary text-primary-foreground text-clinical-sm font-semibold flex items-center justify-center hover:bg-primary/95"
              >
                {isEmitting ? <Loader2 className="w-4 h-4 animate-spin" /> : 'Emite'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Receipt / Invoice preview */}
      {printDoc && (() => {
        const isInvoice = printDoc.kind === 'invoice';
        const doc = printDoc.invoice;
        
        let net = doc.subtotal;
        let vat = doc.vatTotal;
        if (isInvoice) {
          const totalBig = new Big(doc.total);
          const vatDivider = new Big(1.19);
          const netBig = totalBig.div(vatDivider).round(2, Big.roundHalfUp);
          const vatBig = totalBig.minus(netBig).round(2, Big.roundHalfUp);
          net = netBig.toNumber();
          vat = vatBig.toNumber();
        }
        
        return (
          <PrintPreviewModal
            open={!!printDoc}
            onClose={() => setPrintDoc(null)}
            title={isInvoice ? 'Factură Fiscală' : 'Chitanță'}
            subtitle={`${doc.invoiceNumber} · ${doc.issuedAt ? new Date(doc.issuedAt).toLocaleString('ro-RO') : new Date().toLocaleString('ro-RO')}`}
          >
            <div>
              <div className="flex items-start justify-between mb-6">
                <div>
                  <h2 className="text-xl font-bold">{isInvoice ? 'FACTURĂ FISCALĂ' : 'CHITANȚĂ'}</h2>
                  <p className="text-sm font-mono">Nr. {doc.invoiceNumber}</p>
                  <p className="text-xs text-gray-600">Data: {doc.issuedAt ? new Date(doc.issuedAt).toLocaleString('ro-RO') : new Date().toLocaleString('ro-RO')}</p>
                </div>
                 <div className="text-right text-xs">
                  <p className="font-bold">{clinicData?.name || 'Clinica Oftalmologică Demo SRL'}</p>
                  <p>CIF: {clinicData?.cui || 'RO12345678'}</p>
                  <p>{clinicData?.address || 'Str. Victoriei 42, Sector 1, București'}</p>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4 mb-5 text-sm">
                <div className="border border-gray-300 rounded p-3">
                  <p className="text-xs text-gray-500 uppercase mb-1">Către</p>
                  {isInvoice && invoiceCompany.name ? (
                    <>
                      <p className="font-bold">{invoiceCompany.name}</p>
                      {invoiceCompany.cif && <p className="text-xs">CIF: {invoiceCompany.cif}</p>}
                      {invoiceCompany.address && <p className="text-xs">{invoiceCompany.address}</p>}
                      {doc.patientName && <p className="text-xs mt-1 text-gray-600">Pacient: {doc.patientName}</p>}
                    </>
                  ) : doc.patientName ? (
                    <>
                      <p className="font-bold">{doc.patientName}</p>
                      {fullPatient && `${fullPatient.firstName} ${fullPatient.lastName}` === doc.patientName && (
                        <>
                          <p className="text-xs">CNP: {fullPatient.cnp || '—'}</p>
                          <p className="text-xs">{fullPatient.address || '—'}</p>
                        </>
                      )}
                    </>
                  ) : fullPatient ? (
                    <>
                      <p className="font-bold">{fullPatient.firstName} {fullPatient.lastName}</p>
                      <p className="text-xs">CNP: {fullPatient.cnp || '—'}</p>
                      <p className="text-xs">{fullPatient.address || '—'}</p>
                    </>
                  ) : selectedPatient ? (
                    <>
                      <p className="font-bold">{selectedPatient.firstName} {selectedPatient.lastName}</p>
                      <p className="text-xs">MRN: {selectedPatient.mrn}</p>
                    </>
                  ) : <p className="text-xs text-gray-500">—</p>}
                </div>
                <div className="border border-gray-300 rounded p-3">
                  <p className="text-xs text-gray-500 uppercase mb-1">Metodă plată</p>
                  <p className="font-bold capitalize">
                    {doc.paymentMethod 
                      ? (doc.paymentMethod.toLowerCase() === 'cash' ? 'Numerar' : doc.paymentMethod.toLowerCase() === 'card' ? 'Card bancar' : 'Transfer bancar')
                      : (payMethod === 'cash' ? 'Numerar' : payMethod === 'card' ? 'Card bancar' : 'Transfer bancar')}
                  </p>
                </div>
              </div>

              <table className="w-full text-sm border border-gray-300 mb-4">
                <thead className="bg-gray-100">
                  <tr>
                    <th className="p-2 border text-left">Cod</th>
                    <th className="p-2 border text-left">Serviciu</th>
                    <th className="p-2 border text-right">Preț (RON)</th>
                  </tr>
                </thead>
                <tbody>
                  {doc.lines && doc.lines.length > 0 ? (
                    doc.lines.map((it, i) => (
                      <tr key={i}>
                        <td className="p-2 border font-mono text-xs">SRV</td>
                        <td className="p-2 border">{it.description}</td>
                        <td className="p-2 border text-right font-mono">{(it.unitPrice * (1 + it.vatRate / 100)).toFixed(2)}</td>
                      </tr>
                    ))
                  ) : (
                    billItems.map((it, i) => (
                      <tr key={i}>
                        <td className="p-2 border font-mono text-xs">{it.code}</td>
                        <td className="p-2 border">{it.name}</td>
                        <td className="p-2 border text-right font-mono">{it.price.toFixed(2)}</td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>

              <div className="flex justify-end">
                <div className="w-72 text-sm">
                  {isInvoice && (
                    <>
                      <div className="flex justify-between py-1"><span>Bază impozabilă:</span><span className="font-mono">{net.toFixed(2)} RON</span></div>
                      <div className="flex justify-between py-1"><span>TVA 19%:</span><span className="font-mono">{vat.toFixed(2)} RON</span></div>
                    </>
                  )}
                  <div className="flex justify-between py-2 border-t-2 border-black mt-1 font-bold text-base">
                    <span>TOTAL:</span><span className="font-mono">{doc.total.toFixed(2)} RON</span>
                  </div>
                </div>
              </div>

              <div className="mt-8 grid grid-cols-2 gap-8 text-xs">
                <div>
                  <p className="text-gray-500">Emitent</p>
                  <div className="mt-8 border-t border-gray-400 pt-1">Semnătură & ștampilă</div>
                </div>
                <div>
                  <p className="text-gray-500">Primitor</p>
                  <div className="mt-8 border-t border-gray-400 pt-1">Semnătură</div>
                </div>
              </div>
            </div>
          </PrintPreviewModal>
        );
      })()}
      {/* Payment Registration Modal */}
      {payingInvoice && (
        <div className="fixed inset-0 bg-black/40 z-[150] flex items-center justify-center p-4" onClick={() => setPayingInvoice(null)}>
          <div className="bg-card rounded-2xl shadow-xl max-w-md w-full p-6 animate-in zoom-in duration-200" onClick={e => e.stopPropagation()}>
            <h3 className="text-clinical-md font-semibold mb-1 text-foreground">Înregistrare Plată Restanță</h3>
            <p className="text-clinical-xs text-muted-foreground mb-4">
              Înregistrați încasarea sumei restante pentru documentul selectat.
            </p>
            
            <div className="bg-muted/30 p-4 rounded-xl border border-border mb-4 space-y-2">
              <div className="flex justify-between text-clinical-sm">
                <span className="text-muted-foreground">Număr Document:</span>
                <span className="font-semibold text-primary">{payingInvoice.invoiceNumber}</span>
              </div>
              <div className="flex justify-between text-clinical-sm">
                <span className="text-muted-foreground">Pacient:</span>
                <span className="font-semibold">{payingInvoice.patientName || 'Pacient Necunoscut'}</span>
              </div>
              <div className="flex justify-between text-clinical-sm pt-2 border-t border-border/60">
                <span className="text-muted-foreground font-medium">Sumă de încasat:</span>
                <span className="font-clinical font-bold text-clinical-md text-red-600">
                  {payingInvoice.total.toFixed(2)} RON
                </span>
              </div>
            </div>

            <div className="mb-5">
              <label className="text-clinical-xs text-muted-foreground block mb-2">Metodă plată încasată</label>
              <div className="flex gap-2">
                {([['CASH', 'Numerar', Banknote], ['CARD', 'Card', CreditCard], ['TRANSFER', 'Transfer', Building]] as const).map(([key, label, Icon]) => (
                  <button 
                    key={key} 
                    onClick={() => setPayMethodForOutstanding(key)}
                    className={`flex-1 py-2 rounded-lg text-clinical-sm font-medium border flex items-center justify-center gap-1 transition-colors ${payMethodForOutstanding === key ? 'bg-primary text-white border-primary' : 'border-border hover:bg-muted bg-background'}`}
                  >
                    <Icon className="w-4 h-4"/>{label}
                  </button>
                ))}
              </div>
            </div>

            <div className="flex gap-2">
              <button 
                onClick={() => setPayingInvoice(null)} 
                className="flex-1 py-2 rounded-lg border border-border text-clinical-sm font-medium hover:bg-muted bg-background"
                disabled={payInvoiceMutation.isPending}
              >
                Anulează
              </button>
              <button 
                onClick={async () => {
                  try {
                    await payInvoiceMutation.mutateAsync({
                      id: payingInvoice.id,
                      paymentMethod: payMethodForOutstanding
                    });
                    toast.success('Plată înregistrată cu succes!', {
                      description: `Documentul ${payingInvoice.invoiceNumber} a fost marcat ca Achitat.`
                    });
                    setPayingInvoice(null);
                    refetchInvoices();
                  } catch (error: any) {
                    toast.error(error?.message || 'Eroare la înregistrarea plății.');
                  }
                }}
                disabled={payInvoiceMutation.isPending}
                className="flex-1 py-2 rounded-lg bg-primary text-primary-foreground text-clinical-sm font-semibold flex items-center justify-center hover:bg-primary/95 disabled:opacity-50"
              >
                {payInvoiceMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : 'Înregistrează Plată'}
              </button>
            </div>
          </div>
        </div>
      )}
    </AppLayout>
  );
};

export default BillingPage;
