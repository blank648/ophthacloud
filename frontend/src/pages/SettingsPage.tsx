import React, { useState } from 'react';
import AppLayout from '@/components/AppLayout';
import { Users, Building, Monitor, Bell, Shield, Plug, Check, X } from 'lucide-react';

const settingsSections = [
  { key: 'users', label: 'Utilizatori & Roluri', icon: Users },
  { key: 'clinic', label: 'Configurare Clinică', icon: Building },
  { key: 'equipment', label: 'Echipamente', icon: Monitor },
  { key: 'templates', label: 'Șabloane Notificări', icon: Bell },
  { key: 'gdpr', label: 'GDPR & Confidențialitate', icon: Shield },
  { key: 'integrations', label: 'Integrări', icon: Plug },
];

const demoUsers = [
  { name: 'Dr. Alexandru Popescu', email: 'a.popescu@visiomed.ro', role: 'Doctor', locations: ['București'], status: 'active' },
  { name: 'Dr. Ioana Mihailescu', email: 'i.mihailescu@visiomed.ro', role: 'Doctor', locations: ['București','Cluj'], status: 'active' },
  { name: 'Optometrist Radu', email: 'radu@visiomed.ro', role: 'Optometrist', locations: ['București'], status: 'active' },
  { name: 'Optician Gheorghe', email: 'gheorghe@visiomed.ro', role: 'Optician', locations: ['București'], status: 'active' },
  { name: 'Ana Recepție', email: 'ana@visiomed.ro', role: 'Recepție', locations: ['București'], status: 'active' },
  { name: 'Manager Clinică', email: 'manager@visiomed.ro', role: 'Manager', locations: ['București','Cluj'], status: 'active' },
];

const demoEquipment = [
  { name: 'OCT Topcon Triton', brand: 'Topcon', type: 'OCT', location: 'București', dicom: true, lastSync: '29.03.2026 08:00' },
  { name: 'Câmp Vizual Nidek', brand: 'Nidek', type: 'Perimetru', location: 'București', dicom: true, lastSync: '29.03.2026 07:45' },
  { name: 'Pentacam HR', brand: 'Oculus', type: 'Topograf', location: 'București', dicom: true, lastSync: '28.03.2026 16:30' },
  { name: 'IOLMaster 700', brand: 'Zeiss', type: 'Biometru', location: 'București', dicom: true, lastSync: '29.03.2026 09:00' },
  { name: 'Autorefractometru Huvitz', brand: 'Huvitz', type: 'Autorefractometru', location: 'București', dicom: false, lastSync: null },
];

const integrations = [
  { name: 'SMS Gateway', provider: 'SMSRO', status: 'connected', lastPing: '< 1 min' },
  { name: 'Email', provider: 'SendGrid', status: 'connected', lastPing: '< 1 min' },
  { name: 'Plăți', provider: 'Stripe', status: 'disconnected', lastPing: null },
  { name: 'Laborator EDI', provider: 'OptiLab SRL', status: 'connected', lastPing: '5 min' },
  { name: 'HL7 FHIR', provider: 'Endpoint intern', status: 'configured', lastPing: '2 min' },
];

const roleColors: Record<string, string> = {
  Doctor: 'bg-blue-100 text-blue-700',
  Optometrist: 'bg-purple-100 text-purple-700',
  Optician: 'bg-amber-100 text-amber-700',
  Recepție: 'bg-green-100 text-green-700',
  Manager: 'bg-teal-100 text-teal-700',
  Admin: 'bg-red-100 text-red-700',
};

const SettingsPage: React.FC = () => {
  const [activeSection, setActiveSection] = useState('users');

  return (
    <AppLayout breadcrumbs={[{ label: 'Setări & Admin' }]}>
      <h1 className="text-clinical-xl font-bold mb-6">Setări & Admin</h1>

      <div className="flex gap-6">
        {/* Sub-nav */}
        <div className="w-56 shrink-0">
          <div className="bg-card rounded-xl border border-border shadow-sm p-2 sticky top-20">
            {settingsSections.map(s => (
              <button key={s.key} onClick={() => setActiveSection(s.key)}
                className={`w-full flex items-center gap-2 px-3 py-2.5 rounded-lg text-clinical-sm transition-colors text-left ${
                  activeSection === s.key ? 'bg-primary/10 text-primary font-semibold' : 'hover:bg-muted text-foreground'
                }`}>
                <s.icon className="w-4 h-4" />{s.label}
              </button>
            ))}
          </div>
        </div>

        {/* Content */}
        <div className="flex-1 min-w-0">
          {activeSection === 'users' && (
            <div className="bg-card rounded-xl border border-border shadow-sm overflow-hidden">
              <div className="p-4 flex items-center justify-between border-b border-border">
                <h3 className="text-clinical-md font-semibold">Utilizatori</h3>
                <button className="px-3 py-1.5 rounded-lg bg-primary text-white text-clinical-xs font-semibold">+ Utilizator nou</button>
              </div>
              <table className="w-full text-clinical-sm">
                <thead><tr className="border-b border-border bg-muted/30">
                  <th className="text-left p-3 text-clinical-xs text-muted-foreground">Nume</th>
                  <th className="text-left p-3 text-clinical-xs text-muted-foreground">Email</th>
                  <th className="text-left p-3 text-clinical-xs text-muted-foreground">Rol</th>
                  <th className="text-left p-3 text-clinical-xs text-muted-foreground">Locații</th>
                  <th className="text-left p-3 text-clinical-xs text-muted-foreground">Status</th>
                </tr></thead>
                <tbody>
                  {demoUsers.map((u, i) => (
                    <tr key={i} className="border-b border-border hover:bg-muted/30 cursor-pointer">
                      <td className="p-3 font-semibold">{u.name}</td>
                      <td className="p-3 text-muted-foreground">{u.email}</td>
                      <td className="p-3"><span className={`px-2 py-0.5 rounded-full text-[10px] font-semibold ${roleColors[u.role] || ''}`}>{u.role}</span></td>
                      <td className="p-3 text-clinical-xs">{u.locations.join(', ')}</td>
                      <td className="p-3"><span className="px-2 py-0.5 rounded-full text-[10px] font-semibold bg-green-100 text-green-700">Activ</span></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {activeSection === 'clinic' && (
            <div className="bg-card rounded-xl border border-border shadow-sm p-6 space-y-4">
              <h3 className="text-clinical-md font-semibold">Configurare Clinică</h3>
              <div className="grid grid-cols-2 gap-4">
                {[['Denumire clinică','Clinica Oftalmologică Visiomed'],['CIF','RO12345678'],['Telefon','+40 21 123 4567'],['Email','contact@visiomed.ro']].map(([label,val]) => (
                  <div key={label}><label className="text-clinical-xs text-muted-foreground block mb-1">{label}</label>
                    <input defaultValue={val} className="w-full rounded-lg border border-border px-3 py-2 text-clinical-sm"/></div>
                ))}
                <div className="col-span-2"><label className="text-clinical-xs text-muted-foreground block mb-1">Adresă</label>
                  <input defaultValue="Str. Victoriei 42, Sector 1, București" className="w-full rounded-lg border border-border px-3 py-2 text-clinical-sm"/></div>
              </div>
            </div>
          )}

          {activeSection === 'equipment' && (
            <div className="bg-card rounded-xl border border-border shadow-sm overflow-hidden">
              <div className="p-4 border-b border-border"><h3 className="text-clinical-md font-semibold">Registru Echipamente</h3></div>
              <table className="w-full text-clinical-sm">
                <thead><tr className="border-b border-border bg-muted/30">
                  <th className="text-left p-3 text-clinical-xs text-muted-foreground">Echipament</th>
                  <th className="text-left p-3 text-clinical-xs text-muted-foreground">Marcă</th>
                  <th className="text-left p-3 text-clinical-xs text-muted-foreground">Tip</th>
                  <th className="text-left p-3 text-clinical-xs text-muted-foreground">Locație</th>
                  <th className="text-left p-3 text-clinical-xs text-muted-foreground">DICOM</th>
                  <th className="text-left p-3 text-clinical-xs text-muted-foreground">Ultima sincronizare</th>
                </tr></thead>
                <tbody>
                  {demoEquipment.map((eq, i) => (
                    <tr key={i} className="border-b border-border hover:bg-muted/30">
                      <td className="p-3 font-semibold">{eq.name}</td>
                      <td className="p-3">{eq.brand}</td>
                      <td className="p-3">{eq.type}</td>
                      <td className="p-3 text-clinical-xs">{eq.location}</td>
                      <td className="p-3">{eq.dicom ? <Check className="w-4 h-4 text-green-600"/> : <X className="w-4 h-4 text-muted-foreground"/>}</td>
                      <td className="p-3 font-clinical text-clinical-xs text-muted-foreground">{eq.lastSync || '—'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {activeSection === 'integrations' && (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {integrations.map((intg, i) => (
                <div key={i} className="bg-card rounded-xl border border-border shadow-sm p-5">
                  <div className="flex items-center justify-between mb-2">
                    <h4 className="text-clinical-sm font-semibold">{intg.name}</h4>
                    <span className={`w-2.5 h-2.5 rounded-full ${intg.status === 'connected' ? 'bg-green-500' : intg.status === 'configured' ? 'bg-blue-500' : 'bg-red-500'}`}/>
                  </div>
                  <p className="text-clinical-xs text-muted-foreground mb-2">Provider: {intg.provider}</p>
                  <div className="flex items-center justify-between">
                    <span className={`text-clinical-xs font-semibold ${intg.status === 'connected' ? 'text-green-600' : intg.status === 'configured' ? 'text-blue-600' : 'text-red-600'}`}>
                      {intg.status === 'connected' ? 'Conectat' : intg.status === 'configured' ? 'Configurat' : 'Deconectat'}
                    </span>
                    {intg.lastPing && <span className="text-clinical-xs text-muted-foreground">Ping: {intg.lastPing}</span>}
                    <button className="text-clinical-xs text-primary hover:underline">Test conexiune</button>
                  </div>
                </div>
              ))}
            </div>
          )}

          {activeSection === 'gdpr' && (
            <div className="bg-card rounded-xl border border-border shadow-sm p-6 space-y-4">
              <h3 className="text-clinical-md font-semibold">GDPR & Confidențialitate</h3>
              <div className="grid grid-cols-2 gap-4">
                <div className="p-4 rounded-lg bg-muted/30"><p className="text-clinical-xs text-muted-foreground mb-1">DPO</p><p className="text-clinical-sm font-semibold">Av. Maria Stanciu</p><p className="text-clinical-xs text-muted-foreground">dpo@visiomed.ro</p></div>
                <div className="p-4 rounded-lg bg-muted/30"><p className="text-clinical-xs text-muted-foreground mb-1">DPIA</p><p className="text-clinical-sm font-semibold">Completat</p><p className="text-clinical-xs text-muted-foreground">Ultima revizuire: 01.01.2026</p></div>
                <div className="p-4 rounded-lg bg-muted/30"><p className="text-clinical-xs text-muted-foreground mb-1">Retenție date medicale</p><p className="text-clinical-sm font-semibold">10 ani</p></div>
                <div className="p-4 rounded-lg bg-muted/30"><p className="text-clinical-xs text-muted-foreground mb-1">Retenție facturare</p><p className="text-clinical-sm font-semibold">5 ani</p></div>
              </div>
              <button className="px-4 py-2 rounded-lg border border-red-200 text-red-600 text-clinical-sm font-medium hover:bg-red-50">🚨 Raportează Incident Securitate</button>
            </div>
          )}

          {activeSection === 'templates' && (
            <div className="bg-card rounded-xl border border-border shadow-sm p-6">
              <h3 className="text-clinical-md font-semibold mb-4">Șabloane Notificări</h3>
              <p className="text-clinical-sm text-muted-foreground">Editarea template-urilor de notificări se face din modulul Notificări → Reguli Automate.</p>
              <div className="mt-4 grid grid-cols-2 gap-3 text-clinical-xs">
                <div className="p-3 rounded-lg bg-muted/30"><span className="text-muted-foreground">Ore liniștite:</span> <strong>08:00 — 20:00</strong></div>
                <div className="p-3 rounded-lg bg-muted/30"><span className="text-muted-foreground">Max SMS/zi per pacient:</span> <strong>2</strong></div>
              </div>
            </div>
          )}
        </div>
      </div>
    </AppLayout>
  );
};

export default SettingsPage;
