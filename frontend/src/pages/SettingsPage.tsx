import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import AppLayout from '@/components/AppLayout';
import PermissionMatrix from '@/components/PermissionMatrix';
import { Users, Building, Monitor, Bell, Shield, Plug, Check, X, Lock, Save, Plus, Trash2 } from 'lucide-react';
import { useStaff, useEquipment, useClinicSettings, useUpdateClinicSettings, useCreateEquipment, useDeleteEquipment, useDeleteStaff } from '@/hooks/useAdmin';

const settingsSections = [
  { key: 'users', label: 'Utilizatori & Roluri', icon: Users },
  { key: 'permissions', label: 'Matrice Permisiuni', icon: Lock },
  { key: 'clinic', label: 'Configurare Clinică', icon: Building },
  { key: 'equipment', label: 'Echipamente', icon: Monitor },
  { key: 'templates', label: 'Șabloane Notificări', icon: Bell },
  { key: 'gdpr', label: 'GDPR & Confidențialitate', icon: Shield },
  { key: 'integrations', label: 'Integrări', icon: Plug },
];

const integrations = [
  { name: 'SMS Gateway', provider: 'SMSRO', status: 'connected', lastPing: '< 1 min' },
  { name: 'Email', provider: 'SendGrid', status: 'connected', lastPing: '< 1 min' },
  { name: 'Plăți', provider: 'Stripe', status: 'disconnected', lastPing: null },
  { name: 'Laborator EDI', provider: 'OptiLab SRL', status: 'connected', lastPing: '5 min' },
  { name: 'HL7 FHIR', provider: 'Endpoint intern', status: 'configured', lastPing: '2 min' },
];

const roleColors: Record<string, string> = {
  DOCTOR: 'bg-blue-100 text-blue-700',
  OPTOMETRIST: 'bg-purple-100 text-purple-700',
  OPTICAL_TECHNICIAN: 'bg-amber-100 text-amber-700',
  RECEPTIONIST: 'bg-green-100 text-green-700',
  MANAGER: 'bg-teal-100 text-teal-700',
  CLINIC_ADMIN: 'bg-red-100 text-red-700',
};

const SettingsPage: React.FC = () => {
  const navigate = useNavigate();
  const [activeSection, setActiveSection] = useState('users');

  const { data: staffData, isLoading: isLoadingStaff } = useStaff();
  const { data: eqData, isLoading: isLoadingEq } = useEquipment();
  const { data: clinicData, isLoading: isLoadingClinic } = useClinicSettings();
  
  const updateSettings = useUpdateClinicSettings();
  const createEquipment = useCreateEquipment();
  const deleteEquipment = useDeleteEquipment();
  const deleteStaff = useDeleteStaff();

  const handleDeleteStaff = (id: string, name: string) => {
    if (confirm(`Ești sigur că vrei să ștergi/dezactivezi utilizatorul ${name}?`)) {
      deleteStaff.mutate(id, {
        onSuccess: () => toast.success('Utilizator șters cu succes')
      });
    }
  };

  const handleUpdateSettings = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const formData = new FormData(e.currentTarget);
    updateSettings.mutate({
      quietHoursStart: formData.get('quietHoursStart') as string,
      quietHoursEnd: formData.get('quietHoursEnd') as string,
      maxSmsPerPatient: Number(formData.get('maxSmsPerPatient')),
    }, {
      onSuccess: () => toast.success('Setări salvate cu succes')
    });
  };

  const handleSaveClinicDetails = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const formData = new FormData(e.currentTarget);
    updateSettings.mutate({
      name: formData.get('name') as string,
      cui: formData.get('cui') as string,
      phone: formData.get('phone') as string,
      email: formData.get('email') as string,
      address: formData.get('address') as string,
    }, {
      onSuccess: () => toast.success('Datele clinicii au fost salvate cu succes!')
    });
  };

  const handleAddEquipment = () => {
    const name = prompt('Nume echipament:');
    if (!name) return;
    const type = prompt('Tip (ex: OCT, Perimetru):');
    if (!type) return;
    const brand = prompt('Marcă (opțional, ex: Zeiss):');
    const location = prompt('Locație (opțional, ex: Cabinet 1):');
    const dicomEnabled = confirm('Are suport DICOM integrat? (OK pentru Da, Cancel pentru Nu)');
    
    createEquipment.mutate({
      name,
      type,
      brand: brand || undefined,
      location: location || undefined,
      dicomEnabled
    }, {
      onSuccess: () => toast.success('Echipament adăugat')
    });
  };

  return (
    <AppLayout breadcrumbs={[{ label: 'Setări & Admin' }]}>
      <h1 className="text-clinical-xl font-bold mb-6">Setări & Admin</h1>

      <div className="flex gap-6">
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

        <div className="flex-1 min-w-0">
          {activeSection === 'users' && (
            <div className="bg-card rounded-xl border border-border shadow-sm overflow-hidden">
              <div className="p-4 flex items-center justify-between border-b border-border">
                <h3 className="text-clinical-md font-semibold">Utilizatori</h3>
                <button onClick={() => navigate('/settings/users/new')} className="px-3 py-1.5 rounded-lg bg-primary text-white text-clinical-xs font-semibold">+ Utilizator nou</button>
              </div>
              <table className="w-full text-clinical-sm">
                <thead><tr className="border-b border-border bg-muted/30">
                  <th className="text-left p-3 text-clinical-xs text-muted-foreground">Nume</th>
                  <th className="text-left p-3 text-clinical-xs text-muted-foreground">Email</th>
                  <th className="text-left p-3 text-clinical-xs text-muted-foreground">Rol</th>
                  <th className="text-left p-3 text-clinical-xs text-muted-foreground">Status</th>
                  <th className="text-left p-3 text-clinical-xs text-muted-foreground"></th>
                </tr></thead>
                <tbody>
                  {isLoadingStaff ? (
                    <tr><td colSpan={5} className="p-4 text-center text-muted-foreground">Se încarcă...</td></tr>
                  ) : staffData?.data?.filter(u => u.isActive).map((u, i) => (
                    <tr key={i} className="border-b border-border hover:bg-muted/30 cursor-pointer">
                      <td className="p-3 font-semibold">{u.firstName} {u.lastName}</td>
                      <td className="p-3 text-muted-foreground">{u.email}</td>
                      <td className="p-3"><span className={`px-2 py-0.5 rounded-full text-[10px] font-semibold ${roleColors[u.role] || 'bg-gray-100 text-gray-700'}`}>{u.role}</span></td>
                      <td className="p-3">
                        <span className="px-2 py-0.5 rounded-full text-[10px] font-semibold bg-green-100 text-green-700">Activ</span>
                      </td>
                      <td className="p-3 text-right">
                        <button onClick={(e) => { e.stopPropagation(); handleDeleteStaff(u.id, u.firstName + ' ' + u.lastName); }} className="text-red-500 hover:bg-red-50 p-1.5 rounded-md transition-colors" title="Dezactivează">
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {activeSection === 'permissions' && (
            <PermissionMatrix />
          )}

          {activeSection === 'clinic' && (
            <form onSubmit={handleSaveClinicDetails} className="bg-card rounded-xl border border-border shadow-sm p-6 space-y-4">
              <div className="flex justify-between items-center mb-2">
                <h3 className="text-clinical-md font-semibold">Configurare Clinică (Date Companie)</h3>
                <button type="submit" disabled={updateSettings.isPending} className="bg-primary text-white px-4 py-2 rounded-lg text-clinical-sm font-medium hover:bg-primary/90 transition-colors disabled:opacity-50">
                  {updateSettings.isPending ? 'Se salvează...' : 'Salvează Modificările'}
                </button>
              </div>
              <p className="text-clinical-xs text-muted-foreground mb-4">Actualizați datele publice ale clinicii care vor apărea pe facturi și rețete.</p>
              
              {isLoadingClinic ? (
                <p className="text-clinical-sm text-muted-foreground">Se încarcă...</p>
              ) : (
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="text-clinical-xs text-muted-foreground block mb-1">Denumire clinică</label>
                    <input name="name" defaultValue={clinicData?.name || ''} className="w-full rounded-lg border border-border px-3 py-2 text-clinical-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"/>
                  </div>
                  <div>
                    <label className="text-clinical-xs text-muted-foreground block mb-1">CIF</label>
                    <input name="cui" defaultValue={clinicData?.cui || ''} className="w-full rounded-lg border border-border px-3 py-2 text-clinical-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"/>
                  </div>
                  <div>
                    <label className="text-clinical-xs text-muted-foreground block mb-1">Telefon</label>
                    <input name="phone" defaultValue={clinicData?.phone || ''} className="w-full rounded-lg border border-border px-3 py-2 text-clinical-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"/>
                  </div>
                  <div>
                    <label className="text-clinical-xs text-muted-foreground block mb-1">Email</label>
                    <input name="email" defaultValue={clinicData?.email || ''} className="w-full rounded-lg border border-border px-3 py-2 text-clinical-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"/>
                  </div>
                  <div className="col-span-2">
                    <label className="text-clinical-xs text-muted-foreground block mb-1">Adresă</label>
                    <input name="address" defaultValue={clinicData?.address || ''} className="w-full rounded-lg border border-border px-3 py-2 text-clinical-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"/>
                  </div>
                </div>
              )}
            </form>
          )}

          {activeSection === 'equipment' && (
            <div className="bg-card rounded-xl border border-border shadow-sm overflow-hidden">
              <div className="p-4 border-b border-border flex justify-between items-center">
                <h3 className="text-clinical-md font-semibold">Registru Echipamente</h3>
                <button onClick={handleAddEquipment} className="flex items-center gap-1 text-primary text-clinical-xs font-medium hover:underline">
                  <Plus className="w-4 h-4" /> Adaugă Echipament
                </button>
              </div>
              <table className="w-full text-clinical-sm">
                <thead><tr className="border-b border-border bg-muted/30">
                  <th className="text-left p-3 text-clinical-xs text-muted-foreground">Echipament</th>
                  <th className="text-left p-3 text-clinical-xs text-muted-foreground">Marcă</th>
                  <th className="text-left p-3 text-clinical-xs text-muted-foreground">Tip</th>
                  <th className="text-left p-3 text-clinical-xs text-muted-foreground">Locație</th>
                  <th className="text-left p-3 text-clinical-xs text-muted-foreground">DICOM</th>
                  <th className="text-left p-3 text-clinical-xs text-muted-foreground">Ultima sincronizare</th>
                  <th className="text-left p-3 text-clinical-xs text-muted-foreground"></th>
                </tr></thead>
                <tbody>
                  {isLoadingEq ? (
                    <tr><td colSpan={7} className="p-4 text-center text-muted-foreground">Se încarcă...</td></tr>
                  ) : eqData?.length === 0 ? (
                     <tr><td colSpan={7} className="p-4 text-center text-muted-foreground">Niciun echipament înregistrat</td></tr>
                  ) : eqData?.map((eq, i) => (
                    <tr key={i} className="border-b border-border hover:bg-muted/30">
                      <td className="p-3 font-semibold">{eq.name}</td>
                      <td className="p-3">{eq.brand || '-'}</td>
                      <td className="p-3">{eq.type}</td>
                      <td className="p-3 text-clinical-xs">{eq.location || '-'}</td>
                      <td className="p-3">{eq.dicomEnabled ? <Check className="w-4 h-4 text-green-600"/> : <X className="w-4 h-4 text-muted-foreground"/>}</td>
                      <td className="p-3 font-clinical text-clinical-xs text-muted-foreground">{eq.lastSyncAt ? new Date(eq.lastSyncAt).toLocaleString() : '—'}</td>
                      <td className="p-3 text-right">
                        <button onClick={() => {
                          if (confirm(`Ești sigur că vrei să ștergi echipamentul ${eq.name}?`)) {
                            deleteEquipment.mutate(eq.id, {
                              onSuccess: () => toast.success('Echipament șters cu succes')
                            });
                          }
                        }} className="text-red-500 hover:bg-red-50 p-1.5 rounded-md transition-colors" title="Șterge echipament">
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </td>
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
                    <button onClick={() => toast.success(`Test conexiune: ${intg.name}`, { description: 'Răspuns OK în 142ms' })} className="text-clinical-xs text-primary hover:underline">Test conexiune</button>
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
              <a href="mailto:dpo@visiomed.ro?subject=Incident%20Securitate" className="inline-block px-4 py-2 rounded-lg border border-red-200 text-red-600 text-clinical-sm font-medium hover:bg-red-50">🚨 Raportează Incident Securitate</a>
            </div>
          )}

          {activeSection === 'templates' && (
            <form onSubmit={handleUpdateSettings} className="bg-card rounded-xl border border-border shadow-sm p-6">
              <h3 className="text-clinical-md font-semibold mb-4">Șabloane Notificări</h3>
              <p className="text-clinical-sm text-muted-foreground">Editarea template-urilor de notificări se face din modulul Notificări → Reguli Automate. Mai jos puteți seta limitele globale.</p>
              
              {isLoadingClinic ? (
                 <p className="mt-4 text-clinical-sm text-muted-foreground">Se încarcă...</p>
              ) : (
                <div className="mt-4 grid grid-cols-2 gap-6 text-clinical-sm">
                  <div>
                    <label className="block text-clinical-xs text-muted-foreground mb-1">Început ore liniștite</label>
                    <input name="quietHoursStart" type="time" defaultValue={clinicData?.quietHoursStart?.substring(0,5) || '20:00'} className="w-full rounded-lg border border-border px-3 py-2" />
                  </div>
                  <div>
                    <label className="block text-clinical-xs text-muted-foreground mb-1">Sfârșit ore liniștite</label>
                    <input name="quietHoursEnd" type="time" defaultValue={clinicData?.quietHoursEnd?.substring(0,5) || '08:00'} className="w-full rounded-lg border border-border px-3 py-2" />
                  </div>
                  <div className="col-span-2">
                    <label className="block text-clinical-xs text-muted-foreground mb-1">Max SMS/zi per pacient</label>
                    <input name="maxSmsPerPatient" type="number" min="0" defaultValue={clinicData?.maxSmsPerPatient || 2} className="w-full md:w-1/2 rounded-lg border border-border px-3 py-2" />
                  </div>
                </div>
              )}

              <div className="mt-6 flex justify-end">
                <button type="submit" disabled={updateSettings.isPending} className="flex items-center gap-2 px-4 py-2 bg-primary text-white rounded-lg text-clinical-sm font-medium hover:bg-primary/90 disabled:opacity-50">
                  <Save className="w-4 h-4" />
                  {updateSettings.isPending ? 'Se salvează...' : 'Salvează Setările'}
                </button>
              </div>
            </form>
          )}
        </div>
      </div>
    </AppLayout>
  );
};

export default SettingsPage;
