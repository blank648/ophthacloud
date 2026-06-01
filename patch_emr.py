import re

with open('frontend/src/pages/ConsultationPage.tsx', 'r') as f:
    content = f.read()

# 1. Imports
content = content.replace(
    "import { useNavigate } from 'react-router-dom';",
    "import { useNavigate, useSearchParams } from 'react-router-dom';\nimport { useConsultation, useCreateConsultation, useSaveSection, useSignConsultation } from '@/hooks/useEmr';\nimport type { SectionCode } from '@/types/emr';"
)

# 2. State hooks
hook_injection = """
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const urlPatientId = searchParams.get('patientId');
  const [consultationId, setConsultationId] = useState<string | null>(null);

  const { data: consultationRes } = useConsultation(consultationId || undefined);
  const { mutateAsync: createConsultation, isPending: isCreating } = useCreateConsultation();
  const { mutateAsync: saveSection, isPending: isSaving } = useSaveSection(consultationId || '');
  const { mutateAsync: signConsultation, isPending: isSigning } = useSignConsultation(consultationId || '');

  const handleStart = async () => {
    try {
      const res = await createConsultation({ patientId: urlPatientId || patient.id, consultationDate: new Date().toISOString() });
      setConsultationId(res.data.id);
      toast.success('Consultație inițializată');
    } catch (e) {
      toast.error('Eroare la inițializare');
    }
  };

  const handleNext = async (idx: number, code: string, payload: any) => {
    if (!consultationId) return setActiveSection(idx + 1);
    try {
      await saveSection({ sectionCode: code as SectionCode, data: { isCompleted: true, sectionData: payload } });
      setActiveSection(idx + 1);
      toast.success(`Secțiunea ${code} salvată`);
    } catch (e) {
      toast.error('Eroare la salvare secțiune ' + code);
    }
  };
"""
content = content.replace("  const navigate = useNavigate();", hook_injection)

# 3. handleSign
sign_orig = """  const handleSign = () => {
    setSigned(true);
    setActiveSection(8);
    toast.success('Consultație semnată digital', { description: `Dr. Alexandru Popescu · ${new Date().toLocaleString('ro-RO')}` });
  };"""
sign_new = """  const handleSign = async () => {
    if (!consultationId) return;
    try {
      await signConsultation({ signatureConfirmation: true });
      setSigned(true);
      setActiveSection(8);
      toast.success('Consultație semnată digital', { description: `Dr. Alexandru Popescu · ${new Date().toLocaleString('ro-RO')}` });
    } catch (e) {
      toast.error('Eroare la semnare');
    }
  };"""
content = content.replace(sign_orig, sign_new)

# 4. Next buttons
content = content.replace("onClick={()=>setActiveSection(1)}", "onClick={()=>handleNext(0, 'A', { od: {vaSC: vaOD, bcva: bcvaOD, sph: sphOD, cyl: cylOD, axis: axOD, add: addOD}, os: {vaSC: vaOS, bcva: bcvaOS, sph: sphOS, cyl: cylOS, axis: axOS, add: addOS} })}")
content = content.replace("onClick={()=>setActiveSection(2)}", "onClick={()=>handleNext(1, 'B', { pupilTouched })}")
content = content.replace("onClick={()=>setActiveSection(3)}", "onClick={()=>handleNext(2, 'C', { anteriorTouched })}")
content = content.replace("onClick={()=>setActiveSection(4)}", "onClick={()=>handleNext(3, 'D', { od: { iop: iopOD, iopMethod }, os: { iop: iopOS, iopMethod } })}")
content = content.replace("onClick={()=>setActiveSection(5)}", "onClick={()=>handleNext(4, 'E', { posteriorTouched })}")
content = content.replace("onClick={()=>setActiveSection(6)}", "onClick={()=>handleNext(5, 'F', { diagnoses })}")
content = content.replace("onClick={()=>setActiveSection(7)}", "onClick={()=>handleNext(6, 'G', { medications, followUpDate })}")
content = content.replace("onClick={()=>setActiveSection(8)}", "onClick={()=>handleNext(7, 'H', { templateApplied })}")

# 5. Disable fieldset logic
# Replace <fieldset disabled={signed} className="space-y-6"> with condition
content = content.replace(
    '<fieldset disabled={signed} className="space-y-6">',
    '        {!consultationId ? (\n          <div className="flex flex-col items-center justify-center h-64 bg-card rounded-xl border border-border shadow-sm p-6">\n            <h2 className="text-clinical-lg font-bold mb-4">Consultație Nouă</h2>\n            <p className="text-muted-foreground mb-6 text-clinical-sm">Inițializați o nouă consultație EMR pentru {patient.name}</p>\n            <button onClick={handleStart} disabled={isCreating} className="px-6 py-3 rounded-xl bg-primary text-white text-clinical-base font-bold shadow-md hover:opacity-90 disabled:opacity-50 transition-all flex items-center gap-2">\n              Începe Consultația\n            </button>\n          </div>\n        ) : (\n          <fieldset disabled={signed} className="space-y-6">'
)

# And close the tag at the end of the sections
content = content.replace(
    "          </fieldset>",
    "          </fieldset>\n        )}"
)

with open('frontend/src/pages/ConsultationPage.tsx', 'w') as f:
    f.write(content)

