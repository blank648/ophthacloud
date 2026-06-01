import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import AppLayout from '@/components/AppLayout';
import { clinicalFlagStyles, prescriptionStatusStyles } from '@/data/demo-data';
import { ClinicalFlagBadge } from '@/components/StatusBadge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { User, FileText, Shield, FolderOpen, ClipboardList, Check, X, AlertTriangle, Info, Lock, Download, Upload, Eye, ChevronDown, ChevronRight, Pill, Loader2, Edit, Save, Phone, MapPin } from 'lucide-react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ReferenceLine, ResponsiveContainer, Legend } from 'recharts';
import { usePatient, useInvitePatientToPortal, useUpdatePatient } from '@/hooks/usePatients';
import { useConsultations } from '@/hooks/useEmr';
import { usePrescriptions } from '@/hooks/usePrescriptions';
import { useForm } from 'react-hook-form';
import { useAuditLogs } from '@/hooks/useAdmin';
import { setServerErrors } from '@/lib/formUtils';
import type { UpdatePatientRequest } from '@/types/patients';
import { usePermissions } from '@/lib/permissions';
import { toast } from 'sonner';

const PatientProfilePage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  
  const { data: realPatient, isLoading, isError } = usePatient(id);
  const { data: consultationsPage } = useConsultations(id, { page: 0, size: 50 });
  const { data: prescriptionsPage } = usePrescriptions(id, { page: 0, size: 50 });
  const { data: auditLogsPage } = useAuditLogs({ entityType: 'PATIENT', entityId: id });
  const inviteMutation = useInvitePatientToPortal();
  
  const [isEditing, setIsEditing] = useState(false);
  const { mutateAsync: updatePatient, isPending: isUpdating } = useUpdatePatient();
  const { hasPermission } = usePermissions();
  const canEdit = hasPermission('patients', 'EDIT');

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
    reset
  } = useForm<UpdatePatientRequest>();

  React.useEffect(() => {
    if (realPatient) {
      reset({
        firstName: realPatient.firstName,
        lastName: realPatient.lastName,
        dateOfBirth: realPatient.dateOfBirth,
        gender: realPatient.gender,
        phone: realPatient.phone || '',
        email: realPatient.email || '',
        cnp: realPatient.cnp || '',
        address: realPatient.address || '',
        city: realPatient.city || '',
        county: realPatient.county || '',
        bloodType: realPatient.bloodType || '',
        insuranceProvider: realPatient.insuranceProvider || '',
        insuranceNumber: realPatient.insuranceNumber || '',
        occupation: realPatient.occupation || '',
        emergencyContactName: realPatient.emergencyContactName || '',
        emergencyContactPhone: realPatient.emergencyContactPhone || '',
        notes: realPatient.notes || '',
      });
    }
  }, [realPatient, reset]);

  const onEditSubmit = async (data: UpdatePatientRequest) => {
    try {
      const sanitizedData = Object.fromEntries(
        Object.entries(data).map(([key, value]) => [key, value === '' ? undefined : value])
      ) as UpdatePatientRequest;

      await updatePatient({ id: id || '', data: sanitizedData });
      toast.success('Datele pacientului au fost actualizate cu succes!');
      setIsEditing(false);
    } catch (error) {
      const handled = setServerErrors(error, setError);
      if (handled) {
        toast.error('Vă rugăm să corectați câmpurile evidențiate');
      } else {
        toast.error('A apărut o eroare la actualizarea pacientului');
      }
    }
  };

  if (isLoading) {
    return (
      <AppLayout breadcrumbs={[{ label: 'Pacienți', path: '/patients' }, { label: 'Se încarcă...' }]}>
        <div className="flex flex-col items-center justify-center py-20">
          <Loader2 className="w-10 h-10 animate-spin text-primary mb-4" />
          <p className="text-clinical-sm text-muted-foreground">Se încarcă datele pacientului...</p>
        </div>
      </AppLayout>
    );
  }

  if (isError || !realPatient) {
    return (
      <AppLayout breadcrumbs={[{ label: 'Pacienți', path: '/patients' }, { label: 'Necunoscut' }]}>
        <div className="flex flex-col items-center justify-center py-20">
          <User className="w-16 h-16 text-muted-foreground mb-4" />
          <h2 className="text-clinical-lg font-semibold">Pacient negăsit</h2>
          <p className="text-clinical-sm text-muted-foreground mb-4">ID: {id}</p>
          <button onClick={() => navigate('/patients')} className="px-4 py-2 rounded-lg bg-primary text-white text-clinical-sm font-semibold">Înapoi la lista</button>
        </div>
      </AppLayout>
    );
  }

  const patient = {
    ...realPatient,
    name: `${realPatient.firstName} ${realPatient.lastName}`,
    clinicalFlags: [],
    status: realPatient.isActive ? 'active' : 'inactive',
    dob: realPatient.dateOfBirth,
    preferredLanguage: 'RO',
    primaryDiagnosis: 'N/A',
    icdCode: '',
    lastVisit: 'N/A',
    gender: realPatient.gender === 'MALE' ? 'M' : 'F',
    cnp: realPatient.cnp || '',
    phone: realPatient.phone || '',
    address: realPatient.address || '',
    email: realPatient.email || '',
    age: realPatient.age || 0,
    consultations: (Array.isArray(consultationsPage?.data)
      ? consultationsPage.data
      : (consultationsPage?.data as any)?.content || []).map((c: any) => ({
      id: c.id,
      signed: c.status === 'SIGNED',
      date: c.consultationDate ? new Date(c.consultationDate).toLocaleDateString('ro-RO') : 'N/A',
      doctorName: c.doctorName,
      duration: 15,
      diagnosis: c.chiefComplaint || 'Consultație oftalmologică',
      icdCode: c.status === 'SIGNED' ? 'Finalizată' : 'Ciornă',
      summary: c.status === 'SIGNED'
        ? `Consultație finalizată și semnată digital pe data de ${c.signedAt ? new Date(c.signedAt).toLocaleDateString('ro-RO') : 'N/A'}.`
        : 'Consultație în curs de desfășurare.',
      followUpDate: null
    })),
    prescriptions: (Array.isArray(prescriptionsPage?.data)
      ? prescriptionsPage.data
      : (prescriptionsPage?.data as any)?.content || []).map((rx: any) => {
      const odLine = rx.lines?.find((l: any) => l.eye === 'OD');
      const osLine = rx.lines?.find((l: any) => l.eye === 'OS');
      return {
        id: rx.id,
        date: rx.createdAt ? new Date(rx.createdAt).toLocaleDateString('ro-RO') : 'N/A',
        status: rx.status ? rx.status.toLowerCase() : 'active',
        od: {
          sph: odLine?.sph || 0,
          cyl: odLine?.cyl || 0,
          axis: odLine?.axis || 0,
          add: odLine?.addPower || null,
        },
        os: {
          sph: osLine?.sph || 0,
          cyl: osLine?.cyl || 0,
          axis: osLine?.axis || 0,
          add: osLine?.addPower || null,
        }
      };
    }),
    emergencyContact: realPatient.emergencyContactName ? {
       name: realPatient.emergencyContactName,
       relationship: 'N/A',
       phone: realPatient.emergencyContactPhone
    } : null,
    medicalHistory: realPatient.medicalHistory || {
      systemicComorbidities: {},
      familyHistory: {},
      ophthalmicHistory: { conditions: [], surgeries: [] },
      medications: []
    },
    consents: [
      {
        id: '1',
        name: 'Acord GDPR (Prelucrarea Datelor)',
        description: 'Acord obligatoriu conform Regulamentului UE 2016/679 pentru prelucrarea datelor cu caracter personal și a istoricului medical în scop terapeutic.',
        mandatory: true,
        granted: true,
        dateSigned: realPatient.createdAt ? new Date(realPatient.createdAt).toLocaleDateString('ro-RO') : new Date().toLocaleDateString('ro-RO')
      },
      {
        id: '2',
        name: 'Acord Comunicări (SMS/Email)',
        description: 'Consimțământ pentru primirea notificărilor de reamintire a programărilor, alertelor de recall și a rețetelor în format electronic.',
        mandatory: false,
        granted: realPatient.hasPortalAccess || false,
        dateSigned: realPatient.portalInvitedAt ? new Date(realPatient.portalInvitedAt).toLocaleDateString('ro-RO') : null
      },
      {
        id: '3',
        name: 'Consimțământ Dilatație Pupilară',
        description: 'Acord pentru administrarea picăturilor midriatice în scopul examinării fundului de ochi și determinării refracției ciclopegice.',
        mandatory: false,
        granted: true,
        dateSigned: realPatient.createdAt ? new Date(realPatient.createdAt).toLocaleDateString('ro-RO') : new Date().toLocaleDateString('ro-RO')
      }
    ],
    accessLogs: (Array.isArray(auditLogsPage?.data)
      ? auditLogsPage.data
      : (auditLogsPage?.data as any)?.content || []).map((log: any) => {
      const formatAuditRole = (r: string) => {
        switch (r?.toUpperCase()) {
          case 'SUPER_ADMIN': return 'Administrator';
          case 'CLINIC_ADMIN': return 'Manager Clinică';
          case 'DOCTOR': return 'Medic Oftalmolog';
          case 'RECEPTIONIST': return 'Recepție';
          case 'OPTOMETRIST': return 'Optometrist';
          case 'NURSE': return 'Asistent';
          case 'OPTICAL_TECHNICIAN': return 'Tehnician Optică';
          case 'MANAGER': return 'Manager';
          case 'PATIENT': return 'Pacient';
          case 'SYSTEM': return 'Sistem';
          default: return r || 'Utilizator';
        }
      };
      const formatAction = (act: string) => {
        switch (act?.toUpperCase()) {
          case 'CREATE': return 'Creare dosar';
          case 'UPDATE': return 'Actualizare date';
          case 'VIEW': return 'Vizualizare dosar';
          case 'DELETE': return 'Ștergere date';
          default: return act;
        }
      };
      return {
        date: log.occurredAt ? new Date(log.occurredAt).toLocaleString('ro-RO') : 'N/A',
        user: log.actorName || log.actorId,
        role: formatAuditRole(log.actorRole),
        action: formatAction(log.action),
        ip: log.ipAddress || '—'
      };
    })
  };

  const iopChartData = patient.consultations?.map(c => {
    const iopMatch = c.summary.match(/IOP OD:\s*(\d+).*OS:\s*(\d+)/);
    return { date: c.date.substring(0, 10), iopOD: iopMatch ? parseInt(iopMatch[1]) : null, iopOS: iopMatch ? parseInt(iopMatch[2]) : null };
  }).filter(d => d.iopOD !== null).reverse() || [];

  return (
    <AppLayout breadcrumbs={[{ label: 'Pacienți', path: '/patients' }, { label: patient.name }]}>
      {/* Patient Header */}
      <div className="bg-card rounded-xl border border-border shadow-sm p-5 mb-6">
        <div className="flex items-center gap-4">
          <div className="w-16 h-16 rounded-full bg-primary-100 flex items-center justify-center text-primary-700 text-xl font-bold shrink-0">
            {patient.firstName[0]}{patient.lastName[0]}
          </div>
          <div className="flex-1">
            <div className="flex items-center gap-3 mb-1">
              <h1 className="text-clinical-lg font-bold text-foreground">{patient.name}</h1>
              <div className="flex gap-1">{patient.clinicalFlags.map(f => <ClinicalFlagBadge key={f} flag={f} />)}</div>
              <span className={`px-2 py-0.5 rounded-full text-[10px] font-semibold ${patient.status === 'active' ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'}`}>
                {patient.status === 'active' ? 'Activ' : 'Inactiv'}
              </span>
            </div>
            <div className="flex items-center gap-6 text-clinical-sm text-muted-foreground">
              <span className="font-clinical text-clinical-text">{patient.id}</span>
              <span>{patient.age} ani · {patient.gender === 'M' ? 'Masculin' : 'Feminin'}</span>
              <span>CNP: {'●'.repeat(7) + patient.cnp.slice(-6)}</span>
              <span>Ultima vizită: {patient.lastVisit}</span>
            </div>
          </div>
          <div className="flex gap-2">
            {realPatient?.portalInvitedAt ? (
              <span className="inline-flex items-center gap-1.5 px-3 py-2 rounded-lg bg-green-50 border border-green-200 text-clinical-sm font-semibold text-green-700 select-none">
                <Check className="w-4 h-4" /> Invitat în Portal
              </span>
            ) : (
              <button
                disabled={inviteMutation.isPending || !realPatient?.email}
                onClick={async () => {
                  try {
                    await inviteMutation.mutateAsync(patient.id);
                    toast.success('Invitație trimisă cu succes!', {
                      description: `Pacientul a fost invitat în portal la adresa ${patient.email}.`
                    });
                  } catch (err: any) {
                    toast.error('Eroare la trimiterea invitației', {
                      description: err.message || 'Verificați conexiunea cu serverul.'
                    });
                  }
                }}
                className="flex items-center gap-1.5 px-3 py-2 rounded-lg border border-primary/20 bg-primary/5 hover:bg-primary/10 text-primary text-clinical-sm font-semibold hover:opacity-90 active:scale-[0.99] transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                title={!realPatient?.email ? "Pacientul trebuie să aibă o adresă de email validă." : "Trimite invitație cont portal"}
              >
                {inviteMutation.isPending ? (
                  <Loader2 className="w-4 h-4 animate-spin" />
                ) : (
                  <Shield className="w-4 h-4" />
                )}
                Invită în Portal
              </button>
            )}

            {canEdit && (
              <button 
                onClick={() => setIsEditing(true)}
                className="flex items-center gap-1.5 px-3 py-2 rounded-lg border border-border text-clinical-sm font-semibold hover:bg-muted transition-colors"
              >
                <Edit className="w-4 h-4 text-primary" /> Editează Pacient
              </button>
            )}

            <button 
              onClick={() => navigate(`/appointments?patientId=${patient.id}&openBooking=true`)}
              className="px-3 py-2 rounded-lg border border-border text-clinical-sm font-medium hover:bg-muted transition-colors"
            >
              Programare
            </button>
            <button 
              onClick={() => navigate(`/consultation?patientId=${patient.id}`)}
              className="px-3 py-2 rounded-lg bg-primary text-white text-clinical-sm font-semibold hover:opacity-90 transition-colors"
            >
              Deschide EMR
            </button>
          </div>
        </div>
        {/* Age-based banners */}
        {patient.age < 18 && (
          <div className="mt-3 p-2.5 rounded-lg bg-amber-50 border border-amber-200 text-clinical-sm text-amber-800 flex items-center gap-2">
            <AlertTriangle className="w-4 h-4" /> Pacient pediatric — protocoale pediatrice aplicate
          </div>
        )}
        {patient.age >= 40 && patient.age <= 50 && (
          <div className="mt-3 p-2.5 rounded-lg bg-blue-50 border border-blue-200 text-clinical-sm text-blue-800 flex items-center gap-2">
            <Info className="w-4 h-4" /> Vârstă prezbiopie — monitorizare Add
          </div>
        )}
        {patient.age >= 65 && (
          <div className="mt-3 p-2.5 rounded-lg bg-purple-50 border border-purple-200 text-clinical-sm text-purple-800 flex items-center gap-2">
            <Info className="w-4 h-4" /> Pacient geriatric — frecvență vizite crescută
          </div>
        )}
      </div>

      {/* Tabs */}
      <Tabs defaultValue="personal" className="w-full">
        <TabsList className="w-full justify-start bg-card border border-border rounded-xl p-1 mb-6">
          <TabsTrigger value="personal" className="gap-1.5 text-clinical-sm"><User className="w-4 h-4" /> Date Personale</TabsTrigger>
          <TabsTrigger value="medical" className="gap-1.5 text-clinical-sm"><ClipboardList className="w-4 h-4" /> Istoric Medical</TabsTrigger>
          <TabsTrigger value="consent" className="gap-1.5 text-clinical-sm"><Shield className="w-4 h-4" /> Consimțăminte & GDPR</TabsTrigger>
          <TabsTrigger value="documents" className="gap-1.5 text-clinical-sm"><FolderOpen className="w-4 h-4" /> Documente</TabsTrigger>
          <TabsTrigger value="history" className="gap-1.5 text-clinical-sm"><FileText className="w-4 h-4" /> Istoric Consultații</TabsTrigger>
        </TabsList>

        {/* TAB 1 — Date Personale */}
        <TabsContent value="personal">
          <div className="grid grid-cols-1 xl:grid-cols-3 gap-6">
            <div className="xl:col-span-2 bg-card rounded-xl border border-border shadow-sm p-6">
              <h3 className="text-clinical-md font-semibold mb-4">Informații personale</h3>
              <div className="grid grid-cols-2 gap-4">
                {[
                  ['ID Pacient', patient.id, true],
                  ['CNP', patient.cnp, false, true],
                  ['Prenume', patient.firstName],
                  ['Nume', patient.lastName],
                  ['Data nașterii', patient.dob],
                  ['Gen', patient.gender === 'M' ? 'Masculin' : 'Feminin'],
                  ['Telefon', patient.phone],
                  ['Email', patient.email],
                  ['Adresă', patient.address],
                  ['Limba preferată', patient.preferredLanguage === 'RO' ? 'Română' : 'English'],
                ].map(([label, value, mono, cnp]) => (
                  <div key={label as string} className={label === 'Adresă' ? 'col-span-2' : ''}>
                    <label className="block text-clinical-xs text-muted-foreground font-semibold uppercase tracking-wider mb-1">{label as string}</label>
                    <div className={`h-10 flex items-center px-3 rounded-md border border-border bg-muted/30 text-clinical-sm ${mono ? 'font-clinical' : ''}`}>
                      {value as string}
                      {cnp && <Check className="w-4 h-4 text-green-600 ml-2" />}
                    </div>
                  </div>
                ))}
              </div>
              {patient.emergencyContact && (
                <div className="mt-6 pt-4 border-t border-border">
                  <h4 className="text-clinical-sm font-semibold mb-3">Contact de urgență</h4>
                  <div className="grid grid-cols-3 gap-4">
                    <div><label className="block text-clinical-xs text-muted-foreground mb-1">Nume</label><p className="text-clinical-sm">{patient.emergencyContact.name}</p></div>
                    <div><label className="block text-clinical-xs text-muted-foreground mb-1">Relație</label><p className="text-clinical-sm">{patient.emergencyContact.relationship}</p></div>
                    <div><label className="block text-clinical-xs text-muted-foreground mb-1">Telefon</label><p className="text-clinical-sm">{patient.emergencyContact.phone}</p></div>
                  </div>
                </div>
              )}
            </div>
            {/* Quick stats card */}
            <div className="bg-card rounded-xl border border-border shadow-sm p-6">
              <h3 className="text-clinical-md font-semibold mb-4">Rezumat clinic</h3>
              <div className="space-y-4">
                <div><label className="text-clinical-xs text-muted-foreground uppercase tracking-wider">ID Pacient</label><p className="font-clinical text-clinical-base text-foreground">{patient.id}</p></div>
                <div><label className="text-clinical-xs text-muted-foreground uppercase tracking-wider">Nr. consultații</label><p className="text-clinical-xl font-bold text-foreground">{patient.consultations?.length || 0}</p></div>
                <div><label className="text-clinical-xs text-muted-foreground uppercase tracking-wider">Ultima vizită</label><p className="text-clinical-base">{patient.lastVisit}</p></div>
                <div>
                  <label className="text-clinical-xs text-muted-foreground uppercase tracking-wider">Diagnostic primar</label>
                  <div className="mt-1">{patient.clinicalFlags.map(f => <ClinicalFlagBadge key={f} flag={f} />)}</div>
                  <p className="text-clinical-sm mt-1">{patient.primaryDiagnosis}</p>
                  <p className="text-clinical-xs font-clinical text-muted-foreground">{patient.icdCode}</p>
                </div>
                <div><label className="text-clinical-xs text-muted-foreground uppercase tracking-wider">Rețete active</label><p className="text-clinical-base font-semibold">{patient.prescriptions?.filter(p => p.status === 'active').length || 0}</p></div>
              </div>
            </div>
          </div>
        </TabsContent>

        {/* TAB 2 — Istoric Medical */}
        <TabsContent value="medical">
          <MedicalHistoryTab patient={patient} />
        </TabsContent>

        {/* TAB 3 — Consimțăminte & GDPR */}
        <TabsContent value="consent">
          <ConsentTab patient={patient} />
        </TabsContent>

        {/* TAB 4 — Documente */}
        <TabsContent value="documents">
          <DocumentsTab patient={patient} />
        </TabsContent>

        {/* TAB 5 — Istoric Consultații */}
        <TabsContent value="history">
          <ConsultationHistoryTab patient={patient} iopChartData={iopChartData} />
        </TabsContent>
      </Tabs>

      {isEditing && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-background/80 backdrop-blur-sm">
          <div className="bg-card border border-border rounded-xl shadow-lg w-full max-w-4xl max-h-[90vh] overflow-y-auto flex flex-col animate-in fade-in zoom-in-95 duration-200">
            {/* Modal Header */}
            <div className="flex items-center justify-between p-5 border-b border-border sticky top-0 bg-card z-10">
              <div>
                <h2 className="text-clinical-lg font-bold text-foreground flex items-center gap-2">
                  <Edit className="w-5 h-5 text-primary" /> Editează Date Pacient
                </h2>
                <p className="text-clinical-xs text-muted-foreground">
                  Cod dosar: <span className="font-clinical">{patient.mrn}</span>
                </p>
              </div>
              <button 
                onClick={() => setIsEditing(false)}
                className="p-1.5 rounded-lg hover:bg-muted text-muted-foreground transition-colors"
                title="Închide"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Modal Body */}
            <form onSubmit={handleSubmit(onEditSubmit)} className="p-6 space-y-6 flex-1">
              {/* Date personale */}
              <section className="bg-muted/10 rounded-xl border border-border p-5">
                <h3 className="text-clinical-xs font-semibold uppercase tracking-wider text-muted-foreground mb-4 flex items-center gap-2">
                  <User className="w-3.5 h-3.5 text-primary" /> Date personale
                </h3>
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                  <div>
                    <label className="block text-clinical-xs font-medium text-muted-foreground mb-1.5">Prenume *</label>
                    <input className="w-full h-10 rounded-md border border-input bg-background px-3 text-clinical-sm focus:border-primary focus:ring-2 focus:ring-primary/25 outline-none transition-colors disabled:opacity-50" {...register('firstName', { required: 'Prenumele este obligatoriu' })} disabled={isUpdating} />
                    {errors.firstName && <p className="text-clinical-xs text-destructive mt-1">{errors.firstName.message}</p>}
                  </div>
                  <div>
                    <label className="block text-clinical-xs font-medium text-muted-foreground mb-1.5">Nume *</label>
                    <input className="w-full h-10 rounded-md border border-input bg-background px-3 text-clinical-sm focus:border-primary focus:ring-2 focus:ring-primary/25 outline-none transition-colors disabled:opacity-50" {...register('lastName', { required: 'Numele este obligatoriu' })} disabled={isUpdating} />
                    {errors.lastName && <p className="text-clinical-xs text-destructive mt-1">{errors.lastName.message}</p>}
                  </div>
                  <div>
                    <label className="block text-clinical-xs font-medium text-muted-foreground mb-1.5">CNP</label>
                    <input
                      maxLength={13}
                      className="w-full h-10 rounded-md border border-input bg-background px-3 text-clinical-sm focus:border-primary focus:ring-2 focus:ring-primary/25 outline-none transition-colors disabled:opacity-50 font-clinical"
                      {...register('cnp')}
                      disabled={isUpdating}
                    />
                    {errors.cnp && <p className="text-clinical-xs text-destructive mt-1">{errors.cnp.message}</p>}
                  </div>
                  <div>
                    <label className="block text-clinical-xs font-medium text-muted-foreground mb-1.5">Data nașterii *</label>
                    <input type="date" className="w-full h-10 rounded-md border border-input bg-background px-3 text-clinical-sm focus:border-primary focus:ring-2 focus:ring-primary/25 outline-none transition-colors disabled:opacity-50" {...register('dateOfBirth', {
                      required: 'Data nașterii este obligatorie',
                      validate: val => new Date(val!) <= new Date() || 'Data nașterii nu poate fi în viitor'
                    })} disabled={isUpdating} />
                    {errors.dateOfBirth && <p className="text-clinical-xs text-destructive mt-1">{errors.dateOfBirth.message}</p>}
                  </div>
                  <div>
                    <label className="block text-clinical-xs font-medium text-muted-foreground mb-1.5">Sex</label>
                    <select className="w-full h-10 rounded-md border border-input bg-background px-3 text-clinical-sm focus:border-primary focus:ring-2 focus:ring-primary/25 outline-none transition-colors disabled:opacity-50" {...register('gender')} disabled={isUpdating}>
                      <option value="MALE">Masculin</option>
                      <option value="FEMALE">Feminin</option>
                      <option value="UNKNOWN">Necunoscut</option>
                    </select>
                    {errors.gender && <p className="text-clinical-xs text-destructive mt-1">{errors.gender.message}</p>}
                  </div>
                  <div>
                    <label className="block text-clinical-xs font-medium text-muted-foreground mb-1.5">Grupă sanguină</label>
                    <input className="w-full h-10 rounded-md border border-input bg-background px-3 text-clinical-sm focus:border-primary focus:ring-2 focus:ring-primary/25 outline-none transition-colors disabled:opacity-50" {...register('bloodType')} placeholder="A+, B-, etc." disabled={isUpdating} />
                    {errors.bloodType && <p className="text-clinical-xs text-destructive mt-1">{errors.bloodType.message}</p>}
                  </div>
                </div>
              </section>

              {/* Contact */}
              <section className="bg-muted/10 rounded-xl border border-border p-5">
                <h3 className="text-clinical-xs font-semibold uppercase tracking-wider text-muted-foreground mb-4 flex items-center gap-2">
                  <Phone className="w-3.5 h-3.5 text-primary" /> Contact & Adresă
                </h3>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-clinical-xs font-medium text-muted-foreground mb-1.5">Telefon</label>
                    <input className="w-full h-10 rounded-md border border-input bg-background px-3 text-clinical-sm focus:border-primary focus:ring-2 focus:ring-primary/25 outline-none transition-colors disabled:opacity-50" {...register('phone', {
                      pattern: { value: /^[+0-9() -]{10,15}$/, message: 'Număr de telefon invalid' }
                    })} placeholder="07XX XXX XXX" disabled={isUpdating} />
                    {errors.phone && <p className="text-clinical-xs text-destructive mt-1">{errors.phone.message}</p>}
                  </div>
                  <div>
                    <label className="block text-clinical-xs font-medium text-muted-foreground mb-1.5">Email</label>
                    <input type="email" className="w-full h-10 rounded-md border border-input bg-background px-3 text-clinical-sm focus:border-primary focus:ring-2 focus:ring-primary/25 outline-none transition-colors disabled:opacity-50" {...register('email')} disabled={isUpdating} />
                    {errors.email && <p className="text-clinical-xs text-destructive mt-1">{errors.email.message}</p>}
                  </div>
                  <div className="md:col-span-2">
                    <label className="block text-clinical-xs font-medium text-muted-foreground mb-1.5">
                      <MapPin className="inline w-3 h-3 mr-1" /> Stradă și număr
                    </label>
                    <input className="w-full h-10 rounded-md border border-input bg-background px-3 text-clinical-sm focus:border-primary focus:ring-2 focus:ring-primary/25 outline-none transition-colors disabled:opacity-50" {...register('address')} disabled={isUpdating} />
                    {errors.address && <p className="text-clinical-xs text-destructive mt-1">{errors.address.message}</p>}
                  </div>
                  <div>
                    <label className="block text-clinical-xs font-medium text-muted-foreground mb-1.5">Oraș</label>
                    <input className="w-full h-10 rounded-md border border-input bg-background px-3 text-clinical-sm focus:border-primary focus:ring-2 focus:ring-primary/25 outline-none transition-colors disabled:opacity-50" {...register('city')} disabled={isUpdating} />
                    {errors.city && <p className="text-clinical-xs text-destructive mt-1">{errors.city.message}</p>}
                  </div>
                  <div>
                    <label className="block text-clinical-xs font-medium text-muted-foreground mb-1.5">Județ</label>
                    <input className="w-full h-10 rounded-md border border-input bg-background px-3 text-clinical-sm focus:border-primary focus:ring-2 focus:ring-primary/25 outline-none transition-colors disabled:opacity-50" {...register('county')} disabled={isUpdating} />
                    {errors.county && <p className="text-clinical-xs text-destructive mt-1">{errors.county.message}</p>}
                  </div>
                </div>
              </section>

              {/* Administrative & Urgență */}
              <section className="bg-muted/10 rounded-xl border border-border p-5">
                <h3 className="text-clinical-xs font-semibold uppercase tracking-wider text-muted-foreground mb-4 flex items-center gap-2">
                  <ClipboardList className="w-3.5 h-3.5 text-primary" /> Administrativ & Urgență
                </h3>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                  <div>
                    <label className="block text-clinical-xs font-medium text-muted-foreground mb-1.5">Casă Asigurări</label>
                    <input className="w-full h-10 rounded-md border border-input bg-background px-3 text-clinical-sm focus:border-primary focus:ring-2 focus:ring-primary/25 outline-none transition-colors disabled:opacity-50" {...register('insuranceProvider')} placeholder="CASMB, etc." disabled={isUpdating} />
                    {errors.insuranceProvider && <p className="text-clinical-xs text-destructive mt-1">{errors.insuranceProvider.message}</p>}
                  </div>
                  <div>
                    <label className="block text-clinical-xs font-medium text-muted-foreground mb-1.5">Număr Asigurare</label>
                    <input className="w-full h-10 rounded-md border border-input bg-background px-3 text-clinical-sm focus:border-primary focus:ring-2 focus:ring-primary/25 outline-none transition-colors disabled:opacity-50" {...register('insuranceNumber')} disabled={isUpdating} />
                    {errors.insuranceNumber && <p className="text-clinical-xs text-destructive mt-1">{errors.insuranceNumber.message}</p>}
                  </div>
                  <div>
                    <label className="block text-clinical-xs font-medium text-muted-foreground mb-1.5">Ocupație</label>
                    <input className="w-full h-10 rounded-md border border-input bg-background px-3 text-clinical-sm focus:border-primary focus:ring-2 focus:ring-primary/25 outline-none transition-colors disabled:opacity-50" {...register('occupation')} disabled={isUpdating} />
                    {errors.occupation && <p className="text-clinical-xs text-destructive mt-1">{errors.occupation.message}</p>}
                  </div>
                  
                  <div className="col-span-1 md:col-span-3 mt-2 border-t border-border pt-4">
                    <label className="block text-clinical-sm font-semibold text-foreground mb-3">Contact de urgență</label>
                  </div>
                  <div>
                    <label className="block text-clinical-xs font-medium text-muted-foreground mb-1.5">Nume contact</label>
                    <input className="w-full h-10 rounded-md border border-input bg-background px-3 text-clinical-sm focus:border-primary focus:ring-2 focus:ring-primary/25 outline-none transition-colors disabled:opacity-50" {...register('emergencyContactName')} disabled={isUpdating} />
                    {errors.emergencyContactName && <p className="text-clinical-xs text-destructive mt-1">{errors.emergencyContactName.message}</p>}
                  </div>
                  <div>
                    <label className="block text-clinical-xs font-medium text-muted-foreground mb-1.5">Telefon contact</label>
                    <input className="w-full h-10 rounded-md border border-input bg-background px-3 text-clinical-sm focus:border-primary focus:ring-2 focus:ring-primary/25 outline-none transition-colors disabled:opacity-50" {...register('emergencyContactPhone')} disabled={isUpdating} />
                    {errors.emergencyContactPhone && <p className="text-clinical-xs text-destructive mt-1">{errors.emergencyContactPhone.message}</p>}
                  </div>
                  <div>
                    <label className="block text-clinical-xs font-medium text-muted-foreground mb-1.5">Note / Diverse</label>
                    <input className="w-full h-10 rounded-md border border-input bg-background px-3 text-clinical-sm focus:border-primary focus:ring-2 focus:ring-primary/25 outline-none transition-colors disabled:opacity-50" {...register('notes')} disabled={isUpdating} />
                    {errors.notes && <p className="text-clinical-xs text-destructive mt-1">{errors.notes.message}</p>}
                  </div>
                </div>
              </section>

              {/* Modal Footer */}
              <div className="flex items-center justify-end gap-3 pt-4 border-t border-border sticky bottom-0 bg-card z-10">
                <button
                  type="button"
                  onClick={() => setIsEditing(false)}
                  className="px-4 py-2 rounded-lg border border-border text-clinical-sm font-semibold hover:bg-muted transition-colors disabled:opacity-50"
                  disabled={isUpdating}
                >
                  Anulează
                </button>
                <button
                  type="submit"
                  disabled={isUpdating}
                  className="flex items-center gap-1.5 px-4 py-2 rounded-lg bg-primary text-white text-clinical-sm font-semibold hover:bg-primary-700 transition-colors disabled:opacity-50"
                >
                  {isUpdating ? <Loader2 className="w-4 h-4 animate-spin" /> : <Save className="w-4 h-4" />} 
                  Salvează Modificări
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </AppLayout>
  );
};

// ===== Tab Components =====

const AccordionSection: React.FC<{ title: string; children: React.ReactNode; defaultOpen?: boolean }> = ({ title, children, defaultOpen = false }) => {
  const [open, setOpen] = useState(defaultOpen);
  return (
    <div className="bg-card rounded-xl border border-border shadow-sm overflow-hidden mb-4">
      <button onClick={() => setOpen(!open)} className="w-full flex items-center justify-between p-4 hover:bg-muted/30 transition-colors">
        <span className="text-clinical-md font-semibold text-foreground">{title}</span>
        {open ? <ChevronDown className="w-5 h-5 text-muted-foreground" /> : <ChevronRight className="w-5 h-5 text-muted-foreground" />}
      </button>
      {open && <div className="px-4 pb-4 border-t border-border pt-4">{children}</div>}
    </div>
  );
};

const MedicalHistoryTab: React.FC<{ patient: any }> = ({ patient }) => {
  const mh = patient.medicalHistory;
  if (!mh) return <p className="text-muted-foreground p-4">Nu există date de istoric medical.</p>;

  const sys = mh.systemicComorbidities || {};
  const fam = mh.familyHistory || {};
  const oph = mh.ophthalmicHistory || { conditions: [], surgeries: [] };
  const meds = mh.medications || [];

  return (
    <div>
      {/* Diabetes banner */}
      {sys.diabetes && (
        <div className="mb-4 p-3 rounded-lg bg-amber-50 border border-amber-200 border-l-4 border-l-amber-500 text-clinical-sm text-amber-800 flex items-center gap-2">
          <AlertTriangle className="w-4 h-4 shrink-0" /> ⚠ Screening retinopatie diabetică anual recomandat — Diabet tip {sys.diabetes.type}, HbA1c: {sys.diabetes.hba1c}%
        </div>
      )}
      {/* Glaucoma family banner */}
      {fam.glaucoma && (
        <div className="mb-4 p-3 rounded-lg bg-blue-50 border border-blue-200 border-l-4 border-l-blue-500 text-clinical-sm text-blue-800 flex items-center gap-2">
          <Info className="w-4 h-4 shrink-0" /> ℹ Risc genetic crescut glaucom — {fam.glaucoma.relative} diagnosticat la {fam.glaucoma.ageAtDiagnosis} ani — screening IOP anual
        </div>
      )}

      <AccordionSection title="Antecedente oftalmologice personale" defaultOpen>
        <div className="grid grid-cols-2 gap-4 text-clinical-sm">
          <div><span className="text-muted-foreground">Prima vizită:</span> {oph.firstVisitAge || '-'} ani — {oph.firstVisitReason || '-'}</div>
          <div><span className="text-muted-foreground">Ochelari:</span> {oph.glassesUse ? 'Da' : 'Nu'} · Lentile contact: {oph.contactLensUse ? 'Da' : 'Nu'}</div>
          {oph.conditions?.length > 0 && (
            <div className="col-span-2"><span className="text-muted-foreground">Condiții:</span> {oph.conditions.join(', ')}</div>
          )}
          {oph.surgeries?.length > 0 && (
            <div className="col-span-2"><span className="text-muted-foreground">Intervenții:</span> {oph.surgeries.map((s: any) => `${s.type} (${s.year})`).join(', ')}</div>
          )}
          {oph.ocularAllergies && <div><span className="text-muted-foreground">Alergii oculare:</span> {oph.ocularAllergies}</div>}
        </div>
      </AccordionSection>

      <AccordionSection title="Comorbidități sistemice">
        <div className="grid grid-cols-2 gap-3 text-clinical-sm">
          <div className="flex items-center gap-2">
            {sys.diabetes ? <Check className="w-4 h-4 text-amber-600" /> : <X className="w-4 h-4 text-muted-foreground" />}
            Diabet zaharat {sys.diabetes ? `tip ${sys.diabetes.type} (${sys.diabetes.durationYears} ani)` : ''}
          </div>
          <div className="flex items-center gap-2">
            {sys.hypertension ? <Check className="w-4 h-4 text-amber-600" /> : <X className="w-4 h-4 text-muted-foreground" />}
            Hipertensiune arterială {sys.hypertensionMed ? `(${sys.hypertensionMed})` : ''}
          </div>
          <div className="flex items-center gap-2">
            {sys.hyperlipidemia ? <Check className="w-4 h-4 text-amber-600" /> : <X className="w-4 h-4 text-muted-foreground" />}
            Hiperlipidemie
          </div>
          <div className="flex items-center gap-2">
            {sys.migraineWithAura ? <Check className="w-4 h-4 text-amber-600" /> : <X className="w-4 h-4 text-muted-foreground" />}
            Migrenă cu aură
          </div>
          <div className="flex items-center gap-2">
            {sys.osteoporosis ? <Check className="w-4 h-4 text-amber-600" /> : <X className="w-4 h-4 text-muted-foreground" />}
            Osteoporoză
          </div>
        </div>
      </AccordionSection>

      <AccordionSection title="Medicație sistemică în curs">
        {meds.length === 0 ? <p className="text-clinical-sm text-muted-foreground">Fără medicație sistemică.</p> : (
          <div className="space-y-2">
            {meds.map((med: any, i: number) => (
              <div key={i} className="flex items-center gap-3 p-2 rounded-md bg-muted/30">
                <Pill className="w-4 h-4 text-primary" />
                <span className="text-clinical-sm font-semibold">{med.name}</span>
                <span className="text-clinical-sm text-muted-foreground">{med.dose} · {med.frequency}</span>
                {med.riskFlag && <span className="ml-auto px-2 py-0.5 rounded-full text-[10px] font-semibold bg-red-100 text-red-700 border border-red-200">{med.riskFlag}</span>}
              </div>
            ))}
          </div>
        )}
      </AccordionSection>

      <AccordionSection title="Istoric familial oftalmologic">
        <div className="grid grid-cols-2 gap-3 text-clinical-sm">
          <div className="flex items-center gap-2">
            {fam.glaucoma ? <Check className="w-4 h-4 text-red-600" /> : <X className="w-4 h-4 text-muted-foreground" />}
            Glaucom {fam.glaucoma ? `— ${fam.glaucoma.relative} (la ${fam.glaucoma.ageAtDiagnosis} ani)` : ''}
          </div>
          <div className="flex items-center gap-2">{fam.amd ? <Check className="w-4 h-4 text-purple-600" /> : <X className="w-4 h-4 text-muted-foreground" />} AMD</div>
          <div className="flex items-center gap-2">{fam.diabeticRetinopathy ? <Check className="w-4 h-4 text-amber-600" /> : <X className="w-4 h-4 text-muted-foreground" />} Retinopatie diabetică</div>
          <div className="flex items-center gap-2">{fam.strabismus ? <Check className="w-4 h-4 text-blue-600" /> : <X className="w-4 h-4 text-muted-foreground" />} Strabism / Ambliopie</div>
        </div>
      </AccordionSection>
    </div>
  );
};

const ConsentTab: React.FC<{ patient: any }> = ({ patient }) => {
  const consents = patient.consents || [];
  return (
    <div>
      {/* Consent renewal alert */}
      {consents.some((c: any) => c.expiresAt) && (
        <div className="mb-4 p-3 rounded-lg bg-amber-50 border border-amber-200 text-clinical-sm text-amber-800 flex items-center gap-2">
          <AlertTriangle className="w-4 h-4" /> ⚠ Unele consimțăminte necesită reînnoire — verificați datele de expirare
        </div>
      )}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-8">
        {consents.map((c: any) => (
          <div key={c.id} className="bg-card rounded-xl border border-border shadow-sm p-5">
            <div className="flex items-center justify-between mb-2">
              <h4 className="text-clinical-sm font-semibold text-foreground">{c.name}</h4>
              {c.mandatory && <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-red-100 text-red-700">OBLIGATORIU</span>}
            </div>
            <p className="text-clinical-xs text-muted-foreground mb-3">{c.description}</p>
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <div className={`w-10 h-6 rounded-full relative cursor-pointer transition-colors ${c.granted ? 'bg-primary' : 'bg-border'} ${c.mandatory ? 'opacity-80' : ''}`}>
                  <div className={`absolute top-0.5 w-5 h-5 rounded-full bg-white shadow transition-transform ${c.granted ? 'translate-x-[18px]' : 'translate-x-0.5'}`} />
                </div>
                <span className="text-clinical-xs font-semibold">{c.granted ? 'Acordat' : 'Neacordat'}</span>
                {c.mandatory && c.granted && <Lock className="w-3 h-3 text-muted-foreground" />}
              </div>
              {c.dateSigned && <span className="text-clinical-xs text-muted-foreground">Semnat: {c.dateSigned}</span>}
            </div>
            {c.expiresAt && <p className="text-clinical-xs text-amber-600 mt-2">Expiră: {c.expiresAt}</p>}
          </div>
        ))}
      </div>

      {/* Access log */}
      <div className="bg-card rounded-xl border border-border shadow-sm overflow-hidden">
        <div className="p-4 border-b border-border flex items-center justify-between">
          <h3 className="text-clinical-md font-semibold">Jurnal acces dosar medical</h3>
          <button className="flex items-center gap-1.5 px-3 py-1.5 rounded-md border border-border text-clinical-xs font-semibold hover:bg-muted transition-colors">
            <Download className="w-3 h-3" /> Export PDF
          </button>
        </div>
        <table className="w-full">
          <thead><tr className="bg-muted border-b-2 border-border">
            <th className="text-left text-clinical-xs font-semibold text-muted-foreground uppercase tracking-wider py-2.5 px-4">Data/Ora</th>
            <th className="text-left text-clinical-xs font-semibold text-muted-foreground uppercase tracking-wider py-2.5 px-4">Utilizator</th>
            <th className="text-left text-clinical-xs font-semibold text-muted-foreground uppercase tracking-wider py-2.5 px-4">Rol</th>
            <th className="text-left text-clinical-xs font-semibold text-muted-foreground uppercase tracking-wider py-2.5 px-4">Acțiune</th>
            <th className="text-left text-clinical-xs font-semibold text-muted-foreground uppercase tracking-wider py-2.5 px-4">IP</th>
          </tr></thead>
          <tbody>
            {(patient.accessLogs || []).map((entry: any, i: number) => (
              <tr key={i} className={`border-b border-border hover:bg-primary-50 transition-colors ${i % 2 ? 'bg-muted/30' : ''}`}>
                <td className="py-2.5 px-4 text-clinical-xs font-clinical">{entry.date}</td>
                <td className="py-2.5 px-4 text-clinical-sm">{entry.user}</td>
                <td className="py-2.5 px-4 text-clinical-xs">{entry.role}</td>
                <td className="py-2.5 px-4 text-clinical-sm">{entry.action}</td>
                <td className="py-2.5 px-4 text-clinical-xs font-clinical text-muted-foreground">{entry.ip}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

const DocumentsTab: React.FC<{ patient: any }> = ({ patient }) => {
  const docs = patient.documents || [];
  const typeIcons: Record<string, string> = { scrisoare: '📄', reteta: '💊', referire: '📋', certificat: '📜', investigatie: '🔬', dicom: '🏥', consimtamant: '🔒' };
  const typeLabels: Record<string, string> = { scrisoare: 'Scrisoare', reteta: 'Rețetă', referire: 'Referire', certificat: 'Certificat', investigatie: 'Investigație', dicom: 'DICOM', consimtamant: 'Consimțământ' };

  return (
    <div>
      {/* Upload zone */}
      <div className="mb-6 border-2 border-dashed border-border rounded-xl p-8 text-center hover:border-primary/50 transition-colors cursor-pointer">
        <Upload className="w-10 h-10 mx-auto text-muted-foreground mb-2" />
        <p className="text-clinical-sm font-semibold text-foreground">Încărcați documente</p>
        <p className="text-clinical-xs text-muted-foreground">PDF, JPG, PNG, DICOM — max 10MB</p>
      </div>

      {/* Document grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
        {docs.map((doc: any) => (
          <div key={doc.id} className={`bg-card rounded-xl border border-border shadow-sm p-4 hover:shadow-md transition-shadow ${doc.type === 'dicom' ? 'bg-gray-900 text-white border-gray-700' : ''}`}>
            <div className="flex items-start gap-3">
              <span className="text-2xl">{typeIcons[doc.type] || '📄'}</span>
              <div className="flex-1 min-w-0">
                <p className={`text-clinical-sm font-semibold truncate ${doc.type === 'dicom' ? 'text-white' : 'text-foreground'}`}>{doc.filename}</p>
                <div className="flex items-center gap-2 mt-1">
                  <span className="px-1.5 py-0.5 rounded text-[10px] font-semibold bg-primary-100 text-primary-700">{typeLabels[doc.type]}</span>
                  <span className={`text-clinical-xs ${doc.type === 'dicom' ? 'text-gray-400' : 'text-muted-foreground'}`}>{doc.date} · {doc.size}</span>
                </div>
              </div>
            </div>
            <div className="flex gap-2 mt-3">
              <button className={`flex items-center gap-1 px-2.5 py-1 rounded-md text-[11px] font-semibold transition-colors ${doc.type === 'dicom' ? 'bg-gray-700 text-white hover:bg-gray-600' : 'bg-muted hover:bg-muted/80 text-foreground'}`}>
                <Eye className="w-3 h-3" /> Vizualizare
              </button>
              <button className={`flex items-center gap-1 px-2.5 py-1 rounded-md text-[11px] font-semibold transition-colors ${doc.type === 'dicom' ? 'bg-gray-700 text-white hover:bg-gray-600' : 'bg-muted hover:bg-muted/80 text-foreground'}`}>
                <Download className="w-3 h-3" /> Descarcă
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

const ConsultationHistoryTab: React.FC<{ patient: any; iopChartData: any[] }> = ({ patient, iopChartData }) => {
  const consultations = patient.consultations || [];
  const prescriptions = patient.prescriptions || [];

  return (
    <div className="grid grid-cols-1 xl:grid-cols-5 gap-6">
      {/* Timeline */}
      <div className="xl:col-span-3">
        <h3 className="text-clinical-md font-semibold mb-4">Consultații</h3>
        <div className="space-y-4 relative">
          <div className="absolute left-4 top-0 bottom-0 w-px bg-border" />
          {consultations.map((c: any) => (
            <div key={c.id} className="relative pl-10">
              <div className="absolute left-2 top-2 w-5 h-5 rounded-full bg-primary flex items-center justify-center">
                {c.signed && <Check className="w-3 h-3 text-white" />}
              </div>
              <div className="bg-card rounded-xl border border-border shadow-sm p-4">
                <div className="flex items-center gap-3 mb-2">
                  <span className="px-2.5 py-0.5 rounded-full text-[11px] font-semibold bg-primary-100 text-primary-700">{c.date}</span>
                  <span className="text-clinical-sm text-muted-foreground">{c.doctorName}</span>
                  <ClinicalFlagBadge flag={patient.clinicalFlags[0]} />
                  <span className="text-clinical-xs text-muted-foreground">{c.duration} min</span>
                </div>
                <p className="text-clinical-sm font-semibold text-foreground mb-1">{c.diagnosis}</p>
                <p className="text-clinical-xs font-clinical text-muted-foreground mb-2">{c.icdCode}</p>
                <p className="text-clinical-sm text-muted-foreground">{c.summary}</p>
                {c.followUpDate && <p className="text-clinical-xs text-primary mt-2">Follow-up: {c.followUpDate}</p>}
              </div>
            </div>
          ))}
        </div>

        {/* Prescription history table */}
        {prescriptions.length > 0 && (
          <div className="mt-8">
            <h3 className="text-clinical-md font-semibold mb-4">Istoric rețete</h3>
            <div className="bg-card rounded-xl border border-border shadow-sm overflow-hidden">
              <table className="w-full">
                <thead><tr className="bg-muted border-b-2 border-border">
                  {['Data', 'Sph OD', 'Cyl OD', 'Ax OD', 'Sph OS', 'Cyl OS', 'Ax OS', 'Add', 'Status'].map(h => (
                    <th key={h} className="text-left text-clinical-xs font-semibold text-muted-foreground uppercase tracking-wider py-2.5 px-3">{h}</th>
                  ))}
                </tr></thead>
                <tbody>
                  {prescriptions.map((rx: any, i: number) => {
                    const st = prescriptionStatusStyles[rx.status] || { bg: '#F3F4F6', text: '#6B7280' };
                    return (
                      <tr key={rx.id} className={`border-b border-border hover:bg-primary-50 cursor-pointer ${i % 2 ? 'bg-muted/30' : ''}`}>
                        <td className="py-2.5 px-3 text-clinical-sm">{rx.date}</td>
                        <td className="py-2.5 px-3 font-clinical text-clinical-sm" style={{ color: 'hsl(var(--color-text-clinical))' }}>{rx.od.sph > 0 ? '+' : ''}{rx.od.sph.toFixed(2)}</td>
                        <td className="py-2.5 px-3 font-clinical text-clinical-sm" style={{ color: 'hsl(var(--color-text-clinical))' }}>{rx.od.cyl.toFixed(2)}</td>
                        <td className="py-2.5 px-3 font-clinical text-clinical-sm" style={{ color: 'hsl(var(--color-text-clinical))' }}>{rx.od.axis}°</td>
                        <td className="py-2.5 px-3 font-clinical text-clinical-sm" style={{ color: 'hsl(var(--color-text-clinical))' }}>{rx.os.sph > 0 ? '+' : ''}{rx.os.sph.toFixed(2)}</td>
                        <td className="py-2.5 px-3 font-clinical text-clinical-sm" style={{ color: 'hsl(var(--color-text-clinical))' }}>{rx.os.cyl.toFixed(2)}</td>
                        <td className="py-2.5 px-3 font-clinical text-clinical-sm" style={{ color: 'hsl(var(--color-text-clinical))' }}>{rx.os.axis}°</td>
                        <td className="py-2.5 px-3 font-clinical text-clinical-sm" style={{ color: 'hsl(var(--color-text-clinical))' }}>{rx.od.add ? `+${rx.od.add.toFixed(2)}` : '—'}</td>
                        <td className="py-2.5 px-3">
                          <span className="px-2 py-0.5 rounded-full text-[10px] font-semibold" style={{ backgroundColor: st.bg, color: st.text }}>
                            {rx.status === 'active' ? 'Activă' : rx.status === 'expired' ? 'Expirată' : rx.status === 'cancelled' ? 'Anulată' : 'Revizuită'}
                          </span>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </div>

      {/* Charts panel */}
      <div className="xl:col-span-2 space-y-6">
        {iopChartData.length > 0 && (
          <div className="bg-card rounded-xl border border-border shadow-sm p-5">
            <h4 className="text-clinical-sm font-semibold mb-3">Tendință IOP (mmHg)</h4>
            <ResponsiveContainer width="100%" height={200}>
              <LineChart data={iopChartData}>
                <CartesianGrid strokeDasharray="3 3" stroke="hsl(var(--color-border-subtle))" vertical={false} />
                <XAxis dataKey="date" tick={{ fontSize: 10, fontFamily: 'JetBrains Mono' }} stroke="hsl(var(--color-text-muted))" />
                <YAxis domain={[10, 30]} tick={{ fontSize: 10, fontFamily: 'JetBrains Mono' }} stroke="hsl(var(--color-text-muted))" />
                <Tooltip contentStyle={{ borderRadius: 8, fontSize: 11, fontFamily: 'JetBrains Mono' }} />
                <ReferenceLine y={21} stroke="#C97B00" strokeDasharray="5 5" label={{ value: '21', position: 'right', fontSize: 10, fill: '#C97B00' }} />
                <Line type="monotone" dataKey="iopOD" name="IOP OD" stroke="#C0392B" strokeWidth={2} dot={{ r: 4 }} />
                <Line type="monotone" dataKey="iopOS" name="IOP OS" stroke="#2563EB" strokeWidth={2} dot={{ r: 4 }} />
                <Legend wrapperStyle={{ fontSize: 11 }} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        )}
      </div>
    </div>
  );
};

export default PatientProfilePage;
