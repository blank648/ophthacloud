import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import AppLayout from '@/components/AppLayout';
import { ArrowLeft, Save } from 'lucide-react';
import { useCreateStaff } from '@/hooks/useAdmin';
import type { StaffRole } from '@/types/admin';

const roles: { label: string, value: StaffRole }[] = [
  { label: 'Doctor', value: 'DOCTOR' },
  { label: 'Optometrist', value: 'OPTOMETRIST' },
  { label: 'Optician', value: 'OPTICAL_TECHNICIAN' },
  { label: 'Recepție', value: 'RECEPTIONIST' },
  { label: 'Manager', value: 'MANAGER' },
  { label: 'Admin', value: 'CLINIC_ADMIN' }
];

const NewUserPage: React.FC = () => {
  const navigate = useNavigate();
  const createStaff = useCreateStaff();
  
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [role, setRole] = useState<StaffRole>('DOCTOR');
  const [specialization, setSpecialization] = useState('');
  const [licenseNumber, setLicenseNumber] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});

  const handleSave = () => {
    const e: Record<string, string> = {};
    if (!firstName.trim()) e.firstName = 'Prenume obligatoriu';
    if (!lastName.trim()) e.lastName = 'Nume obligatoriu';
    if (!email.trim()) e.email = 'Email obligatoriu';
    else if (!/^\S+@\S+\.\S+$/.test(email)) e.email = 'Email invalid';
    if (Object.keys(e).length) { setErrors(e); return; }

    createStaff.mutate({
      firstName,
      lastName,
      email,
      phone: phone || undefined,
      role,
      specialization: specialization || undefined,
      licenseNumber: licenseNumber || undefined,
      sendInviteEmail: false
    }, {
      onSuccess: () => {
        toast.success('Utilizator creat cu succes', {
          description: `${firstName} ${lastName}`,
        });
        navigate('/settings');
      },
      onError: (err: any) => {
        toast.error('Eroare la crearea utilizatorului', {
          description: err?.response?.data?.message || err.message
        });
      }
    });
  };

  return (
    <AppLayout breadcrumbs={[{ label: 'Setări & Admin', path: '/settings' }, { label: 'Utilizator Nou' }]}>
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-3">
          <button onClick={() => navigate('/settings')} className="p-2 rounded-lg hover:bg-muted">
            <ArrowLeft className="w-4 h-4" />
          </button>
          <h1 className="text-clinical-xl font-bold">Utilizator Nou</h1>
        </div>
        <button onClick={handleSave} disabled={createStaff.isPending}
          className="px-4 py-2 rounded-lg bg-primary text-primary-foreground text-clinical-sm font-semibold flex items-center gap-2 disabled:opacity-50">
          <Save className="w-4 h-4" /> {createStaff.isPending ? 'Se creează...' : 'Creează Utilizator'}
        </button>
      </div>

      <div className="max-w-3xl space-y-4">
        <div className="bg-card rounded-xl border border-border shadow-sm p-5">
          <h3 className="text-clinical-sm font-semibold mb-3">Date Personale</h3>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="text-clinical-xs text-muted-foreground block mb-1">Prenume *</label>
              <input value={firstName} onChange={e => setFirstName(e.target.value)}
                className="w-full rounded-lg border border-border px-3 py-2 text-clinical-sm bg-background" />
              {errors.firstName && <p className="text-clinical-xs text-red-600 mt-1">{errors.firstName}</p>}
            </div>
            <div>
              <label className="text-clinical-xs text-muted-foreground block mb-1">Nume *</label>
              <input value={lastName} onChange={e => setLastName(e.target.value)}
                className="w-full rounded-lg border border-border px-3 py-2 text-clinical-sm bg-background" />
              {errors.lastName && <p className="text-clinical-xs text-red-600 mt-1">{errors.lastName}</p>}
            </div>
            <div>
              <label className="text-clinical-xs text-muted-foreground block mb-1">Email *</label>
              <input type="email" value={email} onChange={e => setEmail(e.target.value)}
                placeholder="nume@clinica.ro"
                className="w-full rounded-lg border border-border px-3 py-2 text-clinical-sm bg-background" />
              {errors.email && <p className="text-clinical-xs text-red-600 mt-1">{errors.email}</p>}
            </div>
            <div>
              <label className="text-clinical-xs text-muted-foreground block mb-1">Telefon</label>
              <input value={phone} onChange={e => setPhone(e.target.value)} placeholder="+40 ..."
                className="w-full rounded-lg border border-border px-3 py-2 text-clinical-sm bg-background" />
            </div>
          </div>
        </div>

        <div className="bg-card rounded-xl border border-border shadow-sm p-5">
          <h3 className="text-clinical-sm font-semibold mb-3">Rol & Detalii Profesionale</h3>
          <div className="grid grid-cols-2 gap-4 mb-4">
            <div>
              <label className="text-clinical-xs text-muted-foreground block mb-1">Rol *</label>
              <select value={role} onChange={e => setRole(e.target.value as StaffRole)}
                className="w-full rounded-lg border border-border px-3 py-2 text-clinical-sm bg-background">
                {roles.map(r => <option key={r.value} value={r.value}>{r.label}</option>)}
              </select>
            </div>
          </div>
          {(role === 'DOCTOR' || role === 'OPTOMETRIST') && (
             <div className="grid grid-cols-2 gap-4 mt-4">
               <div>
                 <label className="text-clinical-xs text-muted-foreground block mb-1">Specializare</label>
                 <input value={specialization} onChange={e => setSpecialization(e.target.value)} placeholder="ex. Medic Primar Oftalmolog"
                   className="w-full rounded-lg border border-border px-3 py-2 text-clinical-sm bg-background" />
               </div>
               <div>
                 <label className="text-clinical-xs text-muted-foreground block mb-1">Cod Parafă / Licență</label>
                 <input value={licenseNumber} onChange={e => setLicenseNumber(e.target.value)} placeholder="123456"
                   className="w-full rounded-lg border border-border px-3 py-2 text-clinical-sm bg-background" />
               </div>
             </div>
          )}
        </div>
      </div>
    </AppLayout>
  );
};

export default NewUserPage;
