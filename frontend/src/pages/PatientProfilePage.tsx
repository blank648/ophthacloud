import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import AppLayout from '@/components/AppLayout';
import { getPatientById, clinicalFlagStyles, prescriptionStatusStyles } from '@/data/demo-data';
import { ClinicalFlagBadge } from '@/components/StatusBadge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { User, FileText, Shield, FolderOpen, ClipboardList, Check, X, AlertTriangle, Info, Lock, Download, Upload, Eye, ChevronDown, ChevronRight, Pill } from 'lucide-react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ReferenceLine, ResponsiveContainer, Legend } from 'recharts';

const PatientProfilePage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const patient = getPatientById(id || '');

  if (!patient) {
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
            <button className="px-3 py-2 rounded-lg border border-border text-clinical-sm font-medium hover:bg-muted transition-colors">Programare</button>
            <button className="px-3 py-2 rounded-lg bg-primary text-white text-clinical-sm font-semibold hover:opacity-90 transition-colors">Deschide EMR</button>
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

  return (
    <div>
      {/* Diabetes banner */}
      {mh.systemicComorbidities.diabetes && (
        <div className="mb-4 p-3 rounded-lg bg-amber-50 border border-amber-200 border-l-4 border-l-amber-500 text-clinical-sm text-amber-800 flex items-center gap-2">
          <AlertTriangle className="w-4 h-4 shrink-0" /> ⚠ Screening retinopatie diabetică anual recomandat — Diabet tip {mh.systemicComorbidities.diabetes.type}, HbA1c: {mh.systemicComorbidities.diabetes.hba1c}%
        </div>
      )}
      {/* Glaucoma family banner */}
      {mh.familyHistory.glaucoma && (
        <div className="mb-4 p-3 rounded-lg bg-blue-50 border border-blue-200 border-l-4 border-l-blue-500 text-clinical-sm text-blue-800 flex items-center gap-2">
          <Info className="w-4 h-4 shrink-0" /> ℹ Risc genetic crescut glaucom — {mh.familyHistory.glaucoma.relative} diagnosticat la {mh.familyHistory.glaucoma.ageAtDiagnosis} ani — screening IOP anual
        </div>
      )}

      <AccordionSection title="Antecedente oftalmologice personale" defaultOpen>
        <div className="grid grid-cols-2 gap-4 text-clinical-sm">
          <div><span className="text-muted-foreground">Prima vizită:</span> {mh.ophthalmicHistory.firstVisitAge} ani — {mh.ophthalmicHistory.firstVisitReason}</div>
          <div><span className="text-muted-foreground">Ochelari:</span> {mh.ophthalmicHistory.glassesUse ? 'Da' : 'Nu'} · Lentile contact: {mh.ophthalmicHistory.contactLensUse ? 'Da' : 'Nu'}</div>
          {mh.ophthalmicHistory.conditions.length > 0 && (
            <div className="col-span-2"><span className="text-muted-foreground">Condiții:</span> {mh.ophthalmicHistory.conditions.join(', ')}</div>
          )}
          {mh.ophthalmicHistory.surgeries.length > 0 && (
            <div className="col-span-2"><span className="text-muted-foreground">Intervenții:</span> {mh.ophthalmicHistory.surgeries.map((s: any) => `${s.type} (${s.year})`).join(', ')}</div>
          )}
          {mh.ophthalmicHistory.ocularAllergies && <div><span className="text-muted-foreground">Alergii oculare:</span> {mh.ophthalmicHistory.ocularAllergies}</div>}
        </div>
      </AccordionSection>

      <AccordionSection title="Comorbidități sistemice">
        <div className="grid grid-cols-2 gap-3 text-clinical-sm">
          <div className="flex items-center gap-2">
            {mh.systemicComorbidities.diabetes ? <Check className="w-4 h-4 text-amber-600" /> : <X className="w-4 h-4 text-muted-foreground" />}
            Diabet zaharat {mh.systemicComorbidities.diabetes ? `tip ${mh.systemicComorbidities.diabetes.type} (${mh.systemicComorbidities.diabetes.durationYears} ani)` : ''}
          </div>
          <div className="flex items-center gap-2">
            {mh.systemicComorbidities.hypertension ? <Check className="w-4 h-4 text-amber-600" /> : <X className="w-4 h-4 text-muted-foreground" />}
            Hipertensiune arterială {mh.systemicComorbidities.hypertensionMed ? `(${mh.systemicComorbidities.hypertensionMed})` : ''}
          </div>
          <div className="flex items-center gap-2">
            {mh.systemicComorbidities.hyperlipidemia ? <Check className="w-4 h-4 text-amber-600" /> : <X className="w-4 h-4 text-muted-foreground" />}
            Hiperlipidemie
          </div>
          <div className="flex items-center gap-2">
            {mh.systemicComorbidities.migraineWithAura ? <Check className="w-4 h-4 text-amber-600" /> : <X className="w-4 h-4 text-muted-foreground" />}
            Migrenă cu aură
          </div>
          <div className="flex items-center gap-2">
            {mh.systemicComorbidities.osteoporosis ? <Check className="w-4 h-4 text-amber-600" /> : <X className="w-4 h-4 text-muted-foreground" />}
            Osteoporoză
          </div>
        </div>
      </AccordionSection>

      <AccordionSection title="Medicație sistemică în curs">
        {mh.medications.length === 0 ? <p className="text-clinical-sm text-muted-foreground">Fără medicație sistemică.</p> : (
          <div className="space-y-2">
            {mh.medications.map((med: any, i: number) => (
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
            {mh.familyHistory.glaucoma ? <Check className="w-4 h-4 text-red-600" /> : <X className="w-4 h-4 text-muted-foreground" />}
            Glaucom {mh.familyHistory.glaucoma ? `— ${mh.familyHistory.glaucoma.relative} (la ${mh.familyHistory.glaucoma.ageAtDiagnosis} ani)` : ''}
          </div>
          <div className="flex items-center gap-2">{mh.familyHistory.amd ? <Check className="w-4 h-4 text-purple-600" /> : <X className="w-4 h-4 text-muted-foreground" />} AMD</div>
          <div className="flex items-center gap-2">{mh.familyHistory.diabeticRetinopathy ? <Check className="w-4 h-4 text-amber-600" /> : <X className="w-4 h-4 text-muted-foreground" />} Retinopatie diabetică</div>
          <div className="flex items-center gap-2">{mh.familyHistory.strabismus ? <Check className="w-4 h-4 text-blue-600" /> : <X className="w-4 h-4 text-muted-foreground" />} Strabism / Ambliopie</div>
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
            {[
              { date: '29.03.2026 08:32', user: 'Dr. Alexandru Popescu', role: 'Doctor', action: 'Vizualizare dosar', ip: '192.168.1.***' },
              { date: '15.01.2026 09:10', user: 'Dr. Alexandru Popescu', role: 'Doctor', action: 'Editare consultație', ip: '192.168.1.***' },
              { date: '10.10.2025 14:20', user: 'Ana Recepție', role: 'Recepție', action: 'Vizualizare programări', ip: '192.168.1.***' },
            ].map((entry, i) => (
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
