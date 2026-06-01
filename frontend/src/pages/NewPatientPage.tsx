import React from 'react';
import AppLayout from '@/components/AppLayout';
import { useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import { ArrowLeft, Save, User, Phone, MapPin, Loader2 } from 'lucide-react';
import { useForm } from 'react-hook-form';
import { useCreatePatient } from '@/hooks/usePatients';
import { setServerErrors } from '@/lib/formUtils';
import type { CreatePatientRequest } from '@/types/patients';

const sectionTitle = 'text-clinical-xs font-semibold uppercase tracking-wider text-muted-foreground mb-3 flex items-center gap-2';
const labelCls = 'block text-clinical-xs font-medium text-muted-foreground mb-1.5';
const inputCls = 'w-full h-10 rounded-md border border-input bg-background px-3 text-clinical-sm focus:border-primary focus:ring-2 focus:ring-primary/25 outline-none transition-colors disabled:opacity-50';

const NewPatientPage: React.FC = () => {
  const navigate = useNavigate();
  const { mutateAsync: createPatient, isPending } = useCreatePatient();
  
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<CreatePatientRequest>({
    defaultValues: {
      gender: 'UNKNOWN',
    }
  });

  const onSubmit = async (data: CreatePatientRequest) => {
    try {
      // Sanitize empty strings to undefined to avoid backend validation errors on optional fields
      const sanitizedData = Object.fromEntries(
        Object.entries(data).map(([key, value]) => [key, value === '' ? undefined : value])
      ) as CreatePatientRequest;

      const newPatient = await createPatient(sanitizedData);
      toast.success(`Pacient creat: ${newPatient.firstName} ${newPatient.lastName} (${newPatient.mrn})`);
      navigate(`/patients/${newPatient.id}`);
    } catch (error) {
      const handled = setServerErrors(error, setError);
      if (handled) {
        toast.error('Vă rugăm să corectați câmpurile evidențiate');
      } else {
        toast.error('A apărut o eroare la salvarea pacientului');
      }
    }
  };

  return (
    <AppLayout
      breadcrumbs={[{ label: 'Acasă' }, { label: 'Pacienți', path: '/patients' }, { label: 'Pacient Nou' }]}
    >
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-3">
          <button
            onClick={() => navigate('/patients')}
            className="p-2 rounded-md hover:bg-muted text-muted-foreground transition-colors"
            aria-label="Înapoi"
          >
            <ArrowLeft className="w-4 h-4" />
          </button>
          <div>
            <h1 className="text-clinical-lg font-bold text-foreground">Pacient Nou</h1>
            <p className="text-clinical-xs text-muted-foreground">Completați datele pentru înregistrarea pacientului</p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => navigate('/patients')}
            className="px-4 py-2.5 rounded-lg border border-border text-clinical-sm font-semibold hover:bg-muted transition-colors disabled:opacity-50"
            disabled={isPending}
          >
            Anulează
          </button>
          <button
            type="submit"
            form="new-patient-form"
            disabled={isPending}
            className="flex items-center gap-1.5 px-4 py-2.5 rounded-lg bg-primary text-white text-clinical-sm font-semibold hover:bg-primary-700 transition-colors disabled:opacity-50"
          >
            {isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Save className="w-4 h-4" />} 
            Salvează Pacient
          </button>
        </div>
      </div>

      <form id="new-patient-form" onSubmit={handleSubmit(onSubmit)} className="space-y-6 pb-20">
        {/* Date personale */}
        <section className="bg-card rounded-xl border border-border shadow-sm p-6">
          <h2 className={sectionTitle}>
            <User className="w-3.5 h-3.5" /> Date personale
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            <div>
              <label className={labelCls}>Prenume *</label>
              <input className={inputCls} {...register('firstName', { required: 'Prenumele este obligatoriu' })} disabled={isPending} />
              {errors.firstName && <p className="text-clinical-xs text-destructive mt-1">{errors.firstName.message}</p>}
            </div>
            <div>
              <label className={labelCls}>Nume *</label>
              <input className={inputCls} {...register('lastName', { required: 'Numele este obligatoriu' })} disabled={isPending} />
              {errors.lastName && <p className="text-clinical-xs text-destructive mt-1">{errors.lastName.message}</p>}
            </div>
            <div>
              <label className={labelCls}>CNP</label>
              <input
                maxLength={13}
                className={`${inputCls} font-clinical`}
                {...register('cnp')}
                disabled={isPending}
              />
              {errors.cnp && <p className="text-clinical-xs text-destructive mt-1">{errors.cnp.message}</p>}
            </div>
            <div>
              <label className={labelCls}>Data nașterii *</label>
              <input type="date" className={inputCls} {...register('dateOfBirth', {
                required: 'Data nașterii este obligatorie',
                validate: val => new Date(val) <= new Date() || 'Data nașterii nu poate fi în viitor'
              })} disabled={isPending} />
              {errors.dateOfBirth && <p className="text-clinical-xs text-destructive mt-1">{errors.dateOfBirth.message}</p>}
            </div>
            <div>
              <label className={labelCls}>Sex</label>
              <select className={inputCls} {...register('gender')} disabled={isPending}>
                <option value="MALE">Masculin</option>
                <option value="FEMALE">Feminin</option>
                <option value="UNKNOWN">Necunoscut</option>
              </select>
              {errors.gender && <p className="text-clinical-xs text-destructive mt-1">{errors.gender.message}</p>}
            </div>
            <div>
              <label className={labelCls}>Grupă sanguină</label>
              <input className={inputCls} {...register('bloodType')} placeholder="A+, B-, etc." disabled={isPending} />
              {errors.bloodType && <p className="text-clinical-xs text-destructive mt-1">{errors.bloodType.message}</p>}
            </div>
          </div>
        </section>

        {/* Contact */}
        <section className="bg-card rounded-xl border border-border shadow-sm p-6">
          <h2 className={sectionTitle}>
            <Phone className="w-3.5 h-3.5" /> Contact & Adresă
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className={labelCls}>Telefon</label>
              <input className={inputCls} {...register('phone', {
                pattern: { value: /^[+0-9() -]{10,15}$/, message: 'Număr de telefon invalid' }
              })} placeholder="07XX XXX XXX" disabled={isPending} />
              {errors.phone && <p className="text-clinical-xs text-destructive mt-1">{errors.phone.message}</p>}
            </div>
            <div>
              <label className={labelCls}>Email</label>
              <input type="email" className={inputCls} {...register('email')} disabled={isPending} />
              {errors.email && <p className="text-clinical-xs text-destructive mt-1">{errors.email.message}</p>}
            </div>
            <div className="md:col-span-2">
              <label className={labelCls}>
                <MapPin className="inline w-3 h-3 mr-1" /> Stradă și număr
              </label>
              <input className={inputCls} {...register('address')} disabled={isPending} />
              {errors.address && <p className="text-clinical-xs text-destructive mt-1">{errors.address.message}</p>}
            </div>
            <div>
              <label className={labelCls}>Oraș</label>
              <input className={inputCls} {...register('city')} disabled={isPending} />
              {errors.city && <p className="text-clinical-xs text-destructive mt-1">{errors.city.message}</p>}
            </div>
            <div>
              <label className={labelCls}>Județ</label>
              <input className={inputCls} {...register('county')} disabled={isPending} />
              {errors.county && <p className="text-clinical-xs text-destructive mt-1">{errors.county.message}</p>}
            </div>
          </div>
        </section>

        {/* Administrative */}
        <section className="bg-card rounded-xl border border-border shadow-sm p-6">
          <h2 className={sectionTitle}>Administrativ & Urgență</h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label className={labelCls}>Casă Asigurări</label>
              <input className={inputCls} {...register('insuranceProvider')} placeholder="CASMB, OPSNAJ, etc." disabled={isPending} />
              {errors.insuranceProvider && <p className="text-clinical-xs text-destructive mt-1">{errors.insuranceProvider.message}</p>}
            </div>
            <div>
              <label className={labelCls}>Număr Asigurare</label>
              <input className={inputCls} {...register('insuranceNumber')} disabled={isPending} />
              {errors.insuranceNumber && <p className="text-clinical-xs text-destructive mt-1">{errors.insuranceNumber.message}</p>}
            </div>
            <div>
              <label className={labelCls}>Ocupație</label>
              <input className={inputCls} {...register('occupation')} disabled={isPending} />
              {errors.occupation && <p className="text-clinical-xs text-destructive mt-1">{errors.occupation.message}</p>}
            </div>
            
            {/* Contact de urgență */}
            <div className="col-span-1 md:col-span-3 mt-4 mb-2">
              <h3 className="text-clinical-sm font-semibold text-foreground border-b pb-2">Contact de urgență</h3>
            </div>
            <div>
              <label className={labelCls}>Nume contact</label>
              <input className={inputCls} {...register('emergencyContactName')} disabled={isPending} />
              {errors.emergencyContactName && <p className="text-clinical-xs text-destructive mt-1">{errors.emergencyContactName.message}</p>}
            </div>
            <div>
              <label className={labelCls}>Telefon contact</label>
              <input className={inputCls} {...register('emergencyContactPhone')} disabled={isPending} />
              {errors.emergencyContactPhone && <p className="text-clinical-xs text-destructive mt-1">{errors.emergencyContactPhone.message}</p>}
            </div>
            <div>
              <label className={labelCls}>Note / Diverse</label>
              <input className={inputCls} {...register('notes')} disabled={isPending} />
              {errors.notes && <p className="text-clinical-xs text-destructive mt-1">{errors.notes.message}</p>}
            </div>
          </div>
        </section>

      </form>
    </AppLayout>
  );
};

export default NewPatientPage;
