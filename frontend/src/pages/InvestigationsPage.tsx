import React, { useState } from 'react';
import { toast } from 'sonner';
import AppLayout from '@/components/AppLayout';
import { 
  Eye, X, ZoomIn, Maximize, Clock, Plus, UploadCloud, 
  FileText, Check, AlertTriangle, FileDown, HelpCircle 
} from 'lucide-react';

import { 
  useInvestigations, 
  useCreateInvestigation, 
  useUpdateInvestigationResult, 
  useUploadInvestigationFile 
} from '@/hooks/useInvestigations';
import { usePatients } from '@/hooks/usePatients';
import type { 
  InvestigationDto, 
  InvestigationCategoryType, 
  InvestigationStatusType 
} from '@/types/investigations';

const statusColors: Record<string, { bg: string; text: string }> = {
  ordered: { bg: '#EFF6FF', text: '#1D4ED8' },
  in_progress: { bg: '#FEF9C3', text: '#854D0E' },
  completed: { bg: '#ECFDF5', text: '#065F46' },
  cancelled: { bg: '#FEF2F2', text: '#991B1B' },
};

const statusLabels: Record<string, string> = { 
  ordered: 'Comandată', 
  in_progress: 'În Lucru', 
  completed: 'Efectuată', 
  cancelled: 'Anulată' 
};

const categoryLabels: Record<InvestigationCategoryType, string> = {
  OCT: 'OCT Macular',
  VISUAL_FIELD: 'Câmp Vizual',
  TOPOGRAPHY: 'Topografie Corneană',
  FUNDUS_PHOTO: 'Fotografie Fundus',
  BIOMETRY: 'Biometrie & Calcul IOL',
  SPECULAR_MICROSCOPY: 'Microscopie Speculară',
  ELECTRORETINOGRAPHY: 'Electroretinografie',
  BLOOD_TEST: 'Analize Sânge',
  OTHER: 'Altă Investigație'
};

const InvestigationsPage: React.FC = () => {
  const [patientId, setPatientId] = useState<string>('');
  const [selected, setSelected] = useState<InvestigationDto | null>(null);
  
  // Modals state
  const [showOrderModal, setShowOrderModal] = useState(false);
  const [isEditingResults, setIsEditingResults] = useState(false);
  
  // Order Form State
  const [newOrder, setNewOrder] = useState({
    category: 'OCT' as InvestigationCategoryType,
    name: 'OCT Macular OD+OS',
    device: 'Heidelberg Spectralis',
    isUrgent: false,
    notes: ''
  });

  // Result Form State
  const [resultStatus, setResultStatus] = useState<InvestigationStatusType>('COMPLETED');
  const [interpretation, setInterpretation] = useState('');
  const [resultData, setResultData] = useState<any>({});

  // File Upload State
  const [uploadFile, setUploadFile] = useState<File | null>(null);
  const [uploadLaterality, setUploadLaterality] = useState('BOTH');
  const [uploadFileType, setUploadFileType] = useState('DICOM');

  // API Queries & Mutations
  const { data: patientsData } = usePatients({ page: 0, size: 5000 });
  const { data: invData, isLoading, refetch } = useInvestigations(patientId || undefined);
  
  const createMutation = useCreateInvestigation();
  const updateMutation = useUpdateInvestigationResult();
  const uploadMutation = useUploadInvestigationFile();

  const patients = patientsData?.data || [];
  const investigations = invData?.data || [];

  const handleOpenViewer = (inv: InvestigationDto) => {
    setSelected(inv);
    setResultStatus(inv.status);
    setInterpretation(inv.interpretation || '');
    setResultData(inv.resultData || {});
    setUploadFile(null);
    setIsEditingResults(false);
  };

  const handleCreateOrder = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!patientId) {
      toast.error('Selectați mai întâi un pacient.');
      return;
    }

    try {
      await createMutation.mutateAsync({
        patientId,
        category: newOrder.category,
        name: newOrder.name,
        device: newOrder.device,
        isUrgent: newOrder.isUrgent,
        notes: newOrder.notes
      });
      
      toast.success('Comandă creată cu succes');
      setShowOrderModal(false);
      setNewOrder({
        category: 'OCT',
        name: 'OCT Macular OD+OS',
        device: 'Heidelberg Spectralis',
        isUrgent: false,
        notes: ''
      });
      refetch();
    } catch (err: any) {
      toast.error('Eroare la crearea comenzii: ' + err.message);
    }
  };

  const handleUpdateResults = async () => {
    if (!selected) return;

    try {
      await updateMutation.mutateAsync({
        id: selected.id,
        data: {
          status: resultStatus,
          performedAt: new Date().toISOString(),
          resultData: resultData,
          interpretation: interpretation
        }
      });

      toast.success('Rezultate salvate cu succes');
      setIsEditingResults(false);
      
      // Update selected snapshot
      const updatedItem = {
        ...selected,
        status: resultStatus,
        resultData: resultData,
        interpretation: interpretation
      };
      setSelected(updatedItem);
      refetch();
    } catch (err: any) {
      toast.error('Eroare la salvarea rezultatelor: ' + err.message);
    }
  };

  const handleFileUpload = async () => {
    if (!selected || !uploadFile) {
      toast.error('Selectați un fișier mai întâi.');
      return;
    }

    try {
      await uploadMutation.mutateAsync({
        id: selected.id,
        file: uploadFile,
        fileType: uploadFileType,
        laterality: uploadLaterality
      });

      toast.success('Fișier atașat cu succes');
      setUploadFile(null);
      
      // Refresh current detail view
      const refreshedDetail = await refetch();
      const updatedSelected = refreshedDetail.data?.data.find(i => i.id === selected.id);
      if (updatedSelected) {
        setSelected(updatedSelected);
      }
    } catch (err: any) {
      toast.error('Eroare la încărcarea fișierului: ' + err.message);
    }
  };

  // Helper for generating initial result template inputs
  const loadCategoryTemplate = (cat: InvestigationCategoryType) => {
    if (cat === 'OCT') {
      setResultData({ rnflOD: 85, rnflOS: 83, macThickOD: 270, macThickOS: 268, macVolOD: 8.4, macVolOS: 8.3, flags: [] });
    } else if (cat === 'VISUAL_FIELD') {
      setResultData({ mdOD: -1.2, mdOS: -1.5, psdOD: 1.4, psdOS: 1.6, ghtOD: 'Normal', ghtOS: 'Normal', flOD: 0, fpOD: 0, fnOD: 0 });
    } else if (cat === 'TOPOGRAPHY') {
      setResultData({ k1OD: 43.1, k2OD: 43.8, axisOD: 90, k1OS: 42.9, k2OS: 43.5, axisOS: 85, pachyOD: 540, pachyOS: 538, kcIndexOD: 'Negativ', classOD: 'Normal' });
    } else if (cat === 'BIOMETRY') {
      setResultData({ axLenOD: 23.4, k1OD: 43.2, k2OD: 43.9, acdOD: 3.2, iolCalc: [
        { formula: 'Barrett Universal II', powerOD: 19.5, refrOD: -0.12 },
        { formula: 'SRK/T', powerOD: 19.0, refrOD: -0.45 },
        { formula: 'Hoffer Q', powerOD: 20.0, refrOD: 0.18 }
      ]});
    } else {
      setResultData({});
    }
  };

  return (
    <AppLayout breadcrumbs={[{ label: 'Investigații' }]}>
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-6">
        <div>
          <h1 className="text-clinical-xl font-bold">Investigații</h1>
          <p className="text-clinical-xs text-muted-foreground">Vizualizare, ordonare și operare imagistică clinică medicală.</p>
        </div>
        <div className="flex flex-wrap items-center gap-3">
          <div className="flex items-center gap-2">
            <label className="text-clinical-sm font-semibold whitespace-nowrap">Pacient:</label>
            <select 
              value={patientId} 
              onChange={(e) => {
                setPatientId(e.target.value);
                setSelected(null);
              }}
              className="rounded-lg border border-border px-3 py-1.5 text-clinical-sm bg-background min-w-[200px] max-w-xs clinical-input"
            >
              <option value="">-- Alege pacient --</option>
              {patients.map(p => (
                <option key={p.id} value={p.id}>{p.lastName} {p.firstName} ({p.mrn})</option>
              ))}
            </select>
          </div>
          
          {patientId && (
            <button 
              onClick={() => {
                loadCategoryTemplate('OCT');
                setShowOrderModal(true);
              }}
              className="px-3 py-1.5 rounded-lg bg-primary text-white text-clinical-sm font-semibold flex items-center gap-1 hover:bg-primary/95 transition-colors shadow-sm"
            >
              <Plus className="w-4 h-4" /> Comandă Nouă
            </button>
          )}
        </div>
      </div>

      <div className="bg-card rounded-xl border border-border shadow-sm overflow-hidden mb-6">
        <table className="w-full text-clinical-sm">
          <thead>
            <tr className="border-b border-border bg-muted/30">
              <th className="text-left p-3 text-clinical-xs text-muted-foreground font-semibold">Cod Comandă</th>
              <th className="text-left p-3 text-clinical-xs text-muted-foreground font-semibold">Tip Investigație</th>
              <th className="text-left p-3 text-clinical-xs text-muted-foreground font-semibold">Pacient</th>
              <th className="text-left p-3 text-clinical-xs text-muted-foreground font-semibold">Data Comandării</th>
              <th className="text-left p-3 text-clinical-xs text-muted-foreground font-semibold">Status</th>
              <th className="text-left p-3 text-clinical-xs text-muted-foreground font-semibold">Medic</th>
            </tr>
          </thead>
          <tbody>
            {!patientId && (
              <tr>
                <td colSpan={6} className="p-12 text-center text-muted-foreground text-clinical-sm">
                  <HelpCircle className="w-8 h-8 mx-auto mb-2 text-muted-foreground/45" />
                  Vă rugăm să selectați un pacient din dropdown pentru a lista fișele.
                </td>
              </tr>
            )}
            {patientId && isLoading && (
              <tr>
                <td colSpan={6} className="p-12 text-center text-muted-foreground text-clinical-sm">
                  <div className="skeleton-heading skeleton mx-auto mb-3" />
                  <div className="skeleton-text skeleton mx-auto max-w-[200px]" />
                </td>
              </tr>
            )}
            {patientId && !isLoading && investigations.length === 0 && (
              <tr>
                <td colSpan={6} className="p-12 text-center text-muted-foreground text-clinical-sm">
                  Nu există investigații înregistrate pentru acest pacient. 
                  Apasă pe "Comandă Nouă" pentru a rezerva un test clinic.
                </td>
              </tr>
            )}
            {investigations.map(inv => {
              const st = statusColors[inv.status.toLowerCase()] || { bg: '#eee', text: '#333' };
              const typeStr = categoryLabels[inv.category] || inv.category;
              return (
                <tr 
                  key={inv.id} 
                  onClick={() => handleOpenViewer(inv)} 
                  className="border-b border-border hover:bg-primary-50/40 cursor-pointer transition-colors table-row"
                >
                  <td className="p-3 font-clinical text-primary text-clinical-xs">{inv.id.substring(0, 8).toUpperCase()}</td>
                  <td className="p-3 font-semibold text-clinical-sm">
                    {typeStr} <span className="text-clinical-xs text-muted-foreground font-normal">({inv.name})</span>
                  </td>
                  <td className="p-3">
                    {patients.find(p => p.id === inv.patientId)?.lastName || ''} {patients.find(p => p.id === inv.patientId)?.firstName || ''}
                  </td>
                  <td className="p-3 text-muted-foreground text-clinical-xs">
                    {new Date(inv.orderedAt).toLocaleString('ro-RO')}
                  </td>
                  <td className="p-3">
                    <span 
                      className="px-2.5 py-0.5 rounded-full text-[10px] font-semibold tracking-wider uppercase" 
                      style={{ background: st.bg, color: st.text }}
                    >
                      {statusLabels[inv.status.toLowerCase()] || inv.status}
                    </span>
                  </td>
                  <td className="p-3 text-muted-foreground text-clinical-xs font-semibold">
                    {inv.orderedByName}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      {/* ── CREATE ORDER MODAL ────────────────────────────────────────────── */}
      {showOrderModal && (
        <div className="fixed inset-0 bg-black/40 z-50 flex items-center justify-center p-4" onClick={() => setShowOrderModal(false)}>
          <div className="bg-card rounded-2xl shadow-xl max-w-md w-full p-6 modal" onClick={e => e.stopPropagation()}>
            <div className="flex items-center justify-between pb-3 border-b mb-4">
              <h3 className="text-clinical-md font-semibold">Comandă Investigație Nouă</h3>
              <button onClick={() => setShowOrderModal(false)}><X className="w-5 h-5 text-muted-foreground hover:text-primary" /></button>
            </div>
            
            <form onSubmit={handleCreateOrder} className="space-y-4">
              <div>
                <label className="text-clinical-xs text-muted-foreground block mb-1">Categorie Paraclinică</label>
                <select 
                  value={newOrder.category}
                  onChange={(e) => {
                    const cat = e.target.value as InvestigationCategoryType;
                    const defaultName = categoryLabels[cat] + " OD+OS";
                    setNewOrder(prev => ({ ...prev, category: cat, name: defaultName }));
                    loadCategoryTemplate(cat);
                  }}
                  className="w-full rounded-lg border border-border px-3 py-2 text-clinical-sm bg-background clinical-input"
                >
                  {Object.entries(categoryLabels).map(([k, v]) => (
                    <option key={k} value={k}>{v}</option>
                  ))}
                </select>
              </div>

              <div>
                <label className="text-clinical-xs text-muted-foreground block mb-1">Denumire Investigație</label>
                <input 
                  type="text" 
                  value={newOrder.name}
                  onChange={(e) => setNewOrder(prev => ({ ...prev, name: e.target.value }))}
                  className="w-full rounded-lg border border-border px-3 py-2 text-clinical-sm bg-background clinical-input"
                  required
                />
              </div>

              <div>
                <label className="text-clinical-xs text-muted-foreground block mb-1">Echipament / Aparat</label>
                <input 
                  type="text" 
                  value={newOrder.device}
                  onChange={(e) => setNewOrder(prev => ({ ...prev, device: e.target.value }))}
                  className="w-full rounded-lg border border-border px-3 py-2 text-clinical-sm bg-background clinical-input"
                />
              </div>

              <div className="flex items-center gap-2 py-1">
                <input 
                  type="checkbox" 
                  id="isUrgent"
                  checked={newOrder.isUrgent}
                  onChange={(e) => setNewOrder(prev => ({ ...prev, isUrgent: e.target.checked }))}
                  className="rounded border-border text-primary"
                />
                <label htmlFor="isUrgent" className="text-clinical-xs font-semibold text-red-600 select-none cursor-pointer flex items-center gap-1">
                  <AlertTriangle className="w-3.5 h-3.5" /> Urgență Medicală!
                </label>
              </div>

              <div>
                <label className="text-clinical-xs text-muted-foreground block mb-1">Observații / Recomandare medic</label>
                <textarea 
                  value={newOrder.notes}
                  onChange={(e) => setNewOrder(prev => ({ ...prev, notes: e.target.value }))}
                  className="w-full rounded-lg border border-border px-3 py-2 text-clinical-sm bg-background h-20 clinical-input"
                  placeholder="Detalii suplimentare..."
                />
              </div>

              <div className="flex gap-2 pt-2 border-t mt-4">
                <button 
                  type="button" 
                  onClick={() => setShowOrderModal(false)}
                  className="flex-1 py-2 rounded-lg border border-border text-clinical-sm font-medium hover:bg-muted"
                >
                  Anulează
                </button>
                <button 
                  type="submit" 
                  className="flex-1 py-2 rounded-lg bg-primary text-white text-clinical-sm font-semibold hover:bg-primary/95 shadow-sm"
                >
                  Trimite Comandă
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ── DETAIL MODAL (DICOM VIEWER PANELS) ────────────────────────────────── */}
      {selected && (
        <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4" onClick={() => setSelected(null)}>
          <div 
            className="bg-card rounded-2xl shadow-xl max-w-6xl w-full max-h-[92vh] flex flex-col overflow-hidden modal-xl" 
            onClick={e => e.stopPropagation()}
          >
            {/* Modal Header */}
            <div className="flex items-center justify-between p-4 border-b border-border bg-muted/20">
              <div>
                <div className="flex items-center gap-2">
                  <h2 className="text-clinical-md font-bold">{categoryLabels[selected.category] || selected.category}</h2>
                  {selected.isUrgent && <span className="bg-red-100 text-red-700 text-[10px] font-bold px-2 py-0.5 rounded uppercase tracking-wider">Urgență</span>}
                </div>
                <p className="text-clinical-xs text-muted-foreground">
                  Pacient: <span className="font-semibold text-primary">{patients.find(p => p.id === selected.patientId)?.lastName} {patients.find(p => p.id === selected.patientId)?.firstName}</span> · 
                  Cod: <span className="font-clinical">{selected.id.substring(0, 12).toUpperCase()}</span> · 
                  Status: <span className="font-semibold uppercase">{statusLabels[selected.status.toLowerCase()]}</span>
                </p>
              </div>
              <button 
                onClick={() => setSelected(null)}
                className="w-8 h-8 rounded-full flex items-center justify-center hover:bg-muted transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Modal Body */}
            <div className="flex-1 grid grid-cols-1 lg:grid-cols-2 overflow-y-auto">
              
              {/* STÂNGA: DICOM Viewport (Dark Panel) */}
              <div className="bg-[#0F1923] p-5 flex flex-col border-r border-[#1a2d3e] min-h-[450px]">
                <div className="flex items-center justify-between mb-3 text-gray-400">
                  <div className="flex gap-2">
                    <button className="p-1 rounded bg-[#1a2a3a] hover:bg-gray-700 text-white"><ZoomIn className="w-4 h-4" /></button>
                    <button className="p-1 rounded bg-[#1a2a3a] hover:bg-gray-700 text-white"><Maximize className="w-4 h-4" /></button>
                  </div>
                  <span className="text-[10px] font-clinical">
                    IOL: {selected.device || 'Nespecificat'} · {new Date(selected.orderedAt).toLocaleDateString('ro-RO')}
                  </span>
                </div>

                <div className="flex-1 rounded-xl bg-[#000000] border border-gray-800 flex flex-col items-center justify-center p-4 relative">
                  <Eye className="w-12 h-12 text-gray-700 mb-2" />
                  <p className="text-gray-400 text-clinical-xs font-clinical">{selected.category} Viewer Active</p>
                  <p className="text-gray-600 text-[10px] font-mono">MinIO Storage: {selected.files?.length || 0} fișiere atașate</p>
                </div>

                {/* Scrubber Timeline */}
                <div className="mt-3 py-2 flex items-center gap-3 border-t border-[#1a2d3e]">
                  <Clock className="w-4 h-4 text-gray-500" />
                  <div className="flex-1 h-1.5 bg-gray-800 rounded-full overflow-hidden">
                    <div className={`h-full ${selected.status === 'COMPLETED' ? 'bg-green-600 w-full' : 'bg-amber-500 w-1/2'}`} />
                  </div>
                  <span className="text-gray-500 text-[10px] font-clinical uppercase font-semibold">
                    {statusLabels[selected.status.toLowerCase()]}
                  </span>
                </div>

                {/* Fișiere Atașate (DICOM/PDF list) */}
                <div className="mt-4 p-3 rounded-xl bg-[#152230] border border-[#22354a]">
                  <h4 className="text-clinical-xs font-bold text-gray-300 mb-2 flex items-center gap-1">
                    <FileText className="w-4 h-4 text-primary" /> Atașamente ({selected.files?.length || 0})
                  </h4>
                  
                  <div className="space-y-1.5 max-h-[140px] overflow-y-auto mb-3">
                    {selected.files?.map(file => (
                      <div key={file.id} className="flex items-center justify-between p-2 rounded bg-[#0b141d] border border-gray-800 text-[11px] font-clinical text-gray-400">
                        <div className="flex items-center gap-1.5 overflow-hidden">
                          <span className={`w-2 h-2 rounded-full ${file.laterality === 'OD' ? 'bg-red-500' : file.laterality === 'OS' ? 'bg-blue-500' : 'bg-purple-500'}`} />
                          <span className="truncate font-semibold text-gray-300 max-w-[150px]">{file.fileName}</span>
                          <span className="text-[10px] text-gray-600">({(file.fileSizeBytes / 1024).toFixed(0)} KB)</span>
                        </div>
                        <a 
                          href={file.downloadUrl} 
                          target="_blank" 
                          rel="noreferrer"
                          className="p-1 rounded bg-primary text-white hover:bg-primary/95 flex items-center gap-0.5 font-sans font-semibold text-[10px]"
                        >
                          <FileDown className="w-3.5 h-3.5" /> Descărcare
                        </a>
                      </div>
                    ))}
                    {(!selected.files || selected.files.length === 0) && (
                      <p className="text-[11px] text-gray-500 italic py-2">Niciun fișier clinic atașat.</p>
                    )}
                  </div>

                  {/* Upload new attachment */}
                  <div className="border-t border-gray-800 pt-3">
                    <p className="text-[10px] font-bold text-gray-400 mb-1.5 uppercase">Atașează fișier nou</p>
                    <div className="grid grid-cols-3 gap-2 mb-2">
                      <select 
                        value={uploadLaterality}
                        onChange={e => setUploadLaterality(e.target.value)}
                        className="rounded border border-gray-800 bg-[#0b141d] text-gray-300 text-[10px] p-1 font-semibold"
                      >
                        <option value="BOTH">Bilateral (BOTH)</option>
                        <option value="OD">Ochiul Drept (OD)</option>
                        <option value="OS">Ochiul Stâng (OS)</option>
                      </select>
                      <select 
                        value={uploadFileType}
                        onChange={e => setUploadFileType(e.target.value)}
                        className="rounded border border-gray-800 bg-[#0b141d] text-gray-300 text-[10px] p-1 font-semibold"
                      >
                        <option value="DICOM">DICOM</option>
                        <option value="PDF">PDF Clinical</option>
                        <option value="IMAGE">IMAGE (PNG/JPG)</option>
                      </select>
                      <label className="flex items-center justify-center rounded border border-dashed border-gray-600 bg-[#0b141d] text-gray-400 hover:text-white cursor-pointer text-[10px] font-semibold py-1">
                        <UploadCloud className="w-3.5 h-3.5 mr-1" /> Alege...
                        <input 
                          type="file" 
                          onChange={e => setUploadFile(e.target.files?.[0] || null)}
                          className="hidden" 
                        />
                      </label>
                    </div>

                    {uploadFile && (
                      <div className="flex items-center justify-between bg-primary/10 border border-primary/30 p-2 rounded mb-2">
                        <p className="text-[10px] font-semibold text-primary truncate max-w-[200px]">{uploadFile.name}</p>
                        <button 
                          onClick={handleFileUpload}
                          className="px-2 py-0.5 rounded bg-primary text-white text-[10px] font-semibold flex items-center gap-1 hover:bg-primary/95"
                        >
                          <Check className="w-3 h-3" /> Confirmă Încărcare
                        </button>
                      </div>
                    )}
                  </div>
                </div>
              </div>

              {/* DREAPTA: Rezultate Structurate & Formular Interpretare */}
              <div className="p-6 flex flex-col justify-between overflow-y-auto">
                <div className="space-y-5">
                  <div className="flex items-center justify-between border-b pb-2 mb-2">
                    <h3 className="text-clinical-sm font-bold uppercase text-primary">Interpretare & Indici</h3>
                    <button 
                      onClick={() => {
                        if (!selected.resultData || Object.keys(selected.resultData).length === 0) {
                          loadCategoryTemplate(selected.category);
                        }
                        setIsEditingResults(!isEditingResults);
                      }}
                      className="px-2.5 py-1 text-clinical-xs font-semibold rounded border border-primary text-primary hover:bg-primary-50 transition-colors"
                    >
                      {isEditingResults ? 'Renunță' : 'Editează Rezultate'}
                    </button>
                  </div>

                  {/* Observații inițiale (Doctor Notes) */}
                  {selected.notes && (
                    <div className="p-3 bg-muted/40 rounded-xl border text-clinical-xs italic text-muted-foreground">
                      <span className="font-bold not-italic text-primary block mb-0.5">Indicații Comandă:</span>
                      {selected.notes}
                    </div>
                  )}

                  {/* DATA ENTRY FORM (When isEditingResults = true) */}
                  {isEditingResults ? (
                    <div className="space-y-4 bg-muted/20 p-4 rounded-xl border">
                      <h4 className="text-clinical-xs font-bold text-primary">Formular Date Clinice</h4>
                      
                      <div className="grid grid-cols-2 gap-3">
                        <div>
                          <label className="text-[10px] text-muted-foreground uppercase font-bold block mb-1">Status Nou</label>
                          <select 
                            value={resultStatus}
                            onChange={e => setResultStatus(e.target.value as InvestigationStatusType)}
                            className="w-full rounded border px-2 py-1 text-clinical-xs bg-background clinical-input"
                          >
                            <option value="IN_PROGRESS">În Lucru</option>
                            <option value="COMPLETED">Finalizată (COMPLETED)</option>
                            <option value="CANCELLED">Anulată (CANCELLED)</option>
                          </select>
                        </div>
                      </div>

                      {/* Custom Category Input Forms */}
                      {selected.category === 'OCT' && (
                        <div className="space-y-3 pt-2 border-t">
                          <p className="text-[10px] font-bold text-red-600">OD (Ochiul Drept)</p>
                          <div className="grid grid-cols-3 gap-2">
                            <input type="number" placeholder="RNFL OD" value={resultData.rnflOD || ''} onChange={e => setResultData({...resultData, rnflOD: +e.target.value})} className="rounded border px-2 py-1 text-clinical-xs bg-background" />
                            <input type="number" placeholder="Macular Thick OD" value={resultData.macThickOD || ''} onChange={e => setResultData({...resultData, macThickOD: +e.target.value})} className="rounded border px-2 py-1 text-clinical-xs bg-background" />
                            <input type="number" placeholder="Macular Vol OD" value={resultData.macVolOD || ''} onChange={e => setResultData({...resultData, macVolOD: +e.target.value})} className="rounded border px-2 py-1 text-clinical-xs bg-background" />
                          </div>
                          
                          <p className="text-[10px] font-bold text-blue-600">OS (Ochiul Stâng)</p>
                          <div className="grid grid-cols-3 gap-2">
                            <input type="number" placeholder="RNFL OS" value={resultData.rnflOS || ''} onChange={e => setResultData({...resultData, rnflOS: +e.target.value})} className="rounded border px-2 py-1 text-clinical-xs bg-background" />
                            <input type="number" placeholder="Macular Thick OS" value={resultData.macThickOS || ''} onChange={e => setResultData({...resultData, macThickOS: +e.target.value})} className="rounded border px-2 py-1 text-clinical-xs bg-background" />
                            <input type="number" placeholder="Macular Vol OS" value={resultData.macVolOS || ''} onChange={e => setResultData({...resultData, macVolOS: +e.target.value})} className="rounded border px-2 py-1 text-clinical-xs bg-background" />
                          </div>
                        </div>
                      )}

                      {selected.category === 'VISUAL_FIELD' && (
                        <div className="space-y-3 pt-2 border-t">
                          <p className="text-[10px] font-bold text-red-600">OD (Ochiul Drept)</p>
                          <div className="grid grid-cols-3 gap-2">
                            <input type="number" step="0.1" placeholder="MD OD" value={resultData.mdOD || ''} onChange={e => setResultData({...resultData, mdOD: +e.target.value})} className="rounded border px-2 py-1 text-clinical-xs bg-background" />
                            <input type="number" step="0.1" placeholder="PSD OD" value={resultData.psdOD || ''} onChange={e => setResultData({...resultData, psdOD: +e.target.value})} className="rounded border px-2 py-1 text-clinical-xs bg-background" />
                            <input type="text" placeholder="GHT OD" value={resultData.ghtOD || ''} onChange={e => setResultData({...resultData, ghtOD: e.target.value})} className="rounded border px-2 py-1 text-clinical-xs bg-background" />
                          </div>
                          <p className="text-[10px] font-bold text-blue-600">OS (Ochiul Stâng)</p>
                          <div className="grid grid-cols-3 gap-2">
                            <input type="number" step="0.1" placeholder="MD OS" value={resultData.mdOS || ''} onChange={e => setResultData({...resultData, mdOS: +e.target.value})} className="rounded border px-2 py-1 text-clinical-xs bg-background" />
                            <input type="number" step="0.1" placeholder="PSD OS" value={resultData.psdOS || ''} onChange={e => setResultData({...resultData, psdOS: +e.target.value})} className="rounded border px-2 py-1 text-clinical-xs bg-background" />
                            <input type="text" placeholder="GHT OS" value={resultData.ghtOS || ''} onChange={e => setResultData({...resultData, ghtOS: e.target.value})} className="rounded border px-2 py-1 text-clinical-xs bg-background" />
                          </div>
                        </div>
                      )}

                      {selected.category === 'TOPOGRAPHY' && (
                        <div className="space-y-3 pt-2 border-t">
                          <p className="text-[10px] font-bold text-red-600">OD (Ochiul Drept)</p>
                          <div className="grid grid-cols-4 gap-2">
                            <input type="number" step="0.1" placeholder="K1 OD" value={resultData.k1OD || ''} onChange={e => setResultData({...resultData, k1OD: +e.target.value})} className="rounded border px-2 py-1 text-clinical-xs bg-background" />
                            <input type="number" step="0.1" placeholder="K2 OD" value={resultData.k2OD || ''} onChange={e => setResultData({...resultData, k2OD: +e.target.value})} className="rounded border px-2 py-1 text-clinical-xs bg-background" />
                            <input type="number" placeholder="Axis OD" value={resultData.axisOD || ''} onChange={e => setResultData({...resultData, axisOD: +e.target.value})} className="rounded border px-2 py-1 text-clinical-xs bg-background" />
                            <input type="number" placeholder="Pachy OD" value={resultData.pachyOD || ''} onChange={e => setResultData({...resultData, pachyOD: +e.target.value})} className="rounded border px-2 py-1 text-clinical-xs bg-background" />
                          </div>
                          <p className="text-[10px] font-bold text-blue-600">OS (Ochiul Stâng)</p>
                          <div className="grid grid-cols-4 gap-2">
                            <input type="number" step="0.1" placeholder="K1 OS" value={resultData.k1OS || ''} onChange={e => setResultData({...resultData, k1OS: +e.target.value})} className="rounded border px-2 py-1 text-clinical-xs bg-background" />
                            <input type="number" step="0.1" placeholder="K2 OS" value={resultData.k2OS || ''} onChange={e => setResultData({...resultData, k2OS: +e.target.value})} className="rounded border px-2 py-1 text-clinical-xs bg-background" />
                            <input type="number" placeholder="Axis OS" value={resultData.axisOS || ''} onChange={e => setResultData({...resultData, axisOS: +e.target.value})} className="rounded border px-2 py-1 text-clinical-xs bg-background" />
                            <input type="number" placeholder="Pachy OS" value={resultData.pachyOS || ''} onChange={e => setResultData({...resultData, pachyOS: +e.target.value})} className="rounded border px-2 py-1 text-clinical-xs bg-background" />
                          </div>
                        </div>
                      )}

                      <div>
                        <label className="text-[10px] text-muted-foreground uppercase font-bold block mb-1">Interpretare Clinică / Diagnostic</label>
                        <textarea 
                          value={interpretation}
                          onChange={e => setInterpretation(e.target.value)}
                          className="w-full rounded border px-2 py-1 text-clinical-xs bg-background h-16 clinical-input"
                          placeholder="Introduceți interpretarea rezultatelor..."
                        />
                      </div>

                      <button 
                        onClick={handleUpdateResults}
                        className="w-full py-2 rounded-lg bg-primary text-white text-clinical-sm font-semibold hover:bg-primary/95 flex items-center justify-center gap-1 shadow-sm"
                      >
                        <Check className="w-4 h-4" /> Salvează Date Diagnostic
                      </button>
                    </div>
                  ) : (
                    /* STATIC RENDERING (When isEditingResults = false) */
                    <div className="space-y-4">
                      {/* Interpretation display */}
                      <div>
                        <h4 className="text-clinical-xs font-bold text-muted-foreground uppercase mb-1">Interpretare Medic:</h4>
                        <p className="text-clinical-sm text-gray-800 bg-primary-50/20 p-3 rounded-lg border border-primary-100 font-medium">
                          {selected.interpretation || <span className="italic text-gray-400">Nicio interpretare completată încă. Apăsați pe "Editează Rezultate".</span>}
                        </p>
                      </div>

                      {/* Structurate Details depending on Category */}
                      {selected.resultData && Object.keys(selected.resultData).length > 0 ? (
                        <div className="space-y-4">
                          {selected.category === 'OCT' && (
                            <div className="space-y-3">
                              <h4 className="text-clinical-xs font-bold text-muted-foreground uppercase">Măsurători OCT</h4>
                              {selected.resultData.flags?.map((f: string, i: number) => (
                                <div key={i} className="p-2.5 rounded-lg bg-red-50 border border-red-200 text-red-800 text-clinical-xs flex items-center gap-1 font-semibold">
                                  <AlertTriangle className="w-4 h-4" /> {f}
                                </div>
                              ))}
                              
                              <div className="grid grid-cols-2 gap-4">
                                <div className="bg-red-50/20 border border-red-200 rounded-xl p-3">
                                  <span className="bg-red-500 text-white text-[9px] font-bold px-1.5 py-0.5 rounded tracking-wide mb-2 inline-block">OD</span>
                                  <p className="font-clinical text-clinical-sm">RNFL: <span className={`font-bold ${selected.resultData.rnflOD < 80 ? 'text-red-600' : 'text-green-600'}`}>{selected.resultData.rnflOD}µm</span></p>
                                  <p className="font-clinical text-clinical-xs text-muted-foreground">Maculă: {selected.resultData.macThickOD}µm · {selected.resultData.macVolOD}mm³</p>
                                </div>
                                <div className="bg-blue-50/20 border border-blue-200 rounded-xl p-3">
                                  <span className="bg-blue-500 text-white text-[9px] font-bold px-1.5 py-0.5 rounded tracking-wide mb-2 inline-block">OS</span>
                                  <p className="font-clinical text-clinical-sm">RNFL: <span className={`font-bold ${selected.resultData.rnflOS < 80 ? 'text-red-600' : 'text-green-600'}`}>{selected.resultData.rnflOS}µm</span></p>
                                  <p className="font-clinical text-clinical-xs text-muted-foreground">Maculă: {selected.resultData.macThickOS}µm · {selected.resultData.macVolOS}mm³</p>
                                </div>
                              </div>
                            </div>
                          )}

                          {selected.category === 'VISUAL_FIELD' && (
                            <div className="space-y-3">
                              <h4 className="text-clinical-xs font-bold text-muted-foreground uppercase">Indici Câmp Vizual</h4>
                              <div className="grid grid-cols-2 gap-4">
                                <div className="bg-red-50/20 border border-red-200 rounded-xl p-3">
                                  <span className="bg-red-500 text-white text-[9px] font-bold px-1.5 py-0.5 rounded tracking-wide mb-2 inline-block">OD</span>
                                  <p className="font-clinical text-clinical-sm font-bold">MD: {selected.resultData.mdOD} dB</p>
                                  <p className="font-clinical text-clinical-xs">PSD: {selected.resultData.psdOD} dB</p>
                                  <p className="text-clinical-xs mt-1">GHT: <span className="font-semibold text-red-600">{selected.resultData.ghtOD}</span></p>
                                </div>
                                <div className="bg-blue-50/20 border border-blue-200 rounded-xl p-3">
                                  <span className="bg-blue-500 text-white text-[9px] font-bold px-1.5 py-0.5 rounded tracking-wide mb-2 inline-block">OS</span>
                                  <p className="font-clinical text-clinical-sm font-bold">MD: {selected.resultData.mdOS} dB</p>
                                  <p className="font-clinical text-clinical-xs">PSD: {selected.resultData.psdOS} dB</p>
                                  <p className="text-clinical-xs mt-1">GHT: <span className="font-semibold text-green-600">{selected.resultData.ghtOS}</span></p>
                                </div>
                              </div>
                            </div>
                          )}

                          {selected.category === 'TOPOGRAPHY' && (
                            <div className="space-y-3">
                              <h4 className="text-clinical-xs font-bold text-muted-foreground uppercase">Keratometrie & Pahimetrie</h4>
                              <div className="grid grid-cols-2 gap-4">
                                <div className="bg-red-50/20 border border-red-200 rounded-xl p-3">
                                  <span className="bg-red-500 text-white text-[9px] font-bold px-1.5 py-0.5 rounded tracking-wide mb-2 inline-block">OD</span>
                                  <p className="font-clinical text-clinical-xs">K1: {selected.resultData.k1OD}D · K2: {selected.resultData.k2OD}D</p>
                                  <p className="font-clinical text-clinical-xs">Axe: {selected.resultData.axisOD}°</p>
                                  <p className="font-clinical text-clinical-sm">Pahimetrie: <span className={`font-bold ${selected.resultData.pachyOD < 480 ? 'text-red-600' : 'text-green-600'}`}>{selected.resultData.pachyOD}µm</span></p>
                                </div>
                                <div className="bg-blue-50/20 border border-blue-200 rounded-xl p-3">
                                  <span className="bg-blue-500 text-white text-[9px] font-bold px-1.5 py-0.5 rounded tracking-wide mb-2 inline-block">OS</span>
                                  <p className="font-clinical text-clinical-xs">K1: {selected.resultData.k1OS}D · K2: {selected.resultData.k2OS}D</p>
                                  <p className="font-clinical text-clinical-xs">Axe: {selected.resultData.axisOS}°</p>
                                  <p className="font-clinical text-clinical-sm">Pahimetrie: <span className={`font-bold ${selected.resultData.pachyOS < 480 ? 'text-red-600' : 'text-green-600'}`}>{selected.resultData.pachyOS}µm</span></p>
                                </div>
                              </div>
                            </div>
                          )}

                          {selected.category === 'BIOMETRY' && (
                            <div className="space-y-3">
                              <h4 className="text-clinical-xs font-bold text-muted-foreground uppercase">Biometrie & Calcul IOL</h4>
                              <div className="bg-muted/35 rounded-lg p-2.5 font-clinical text-clinical-xs text-primary">
                                <p>OD: AxLen {selected.resultData.axLenOD}mm · K1 {selected.resultData.k1OD}D · K2 {selected.resultData.k2OD}D · ACD {selected.resultData.acdOD}mm</p>
                              </div>
                              <table className="w-full text-clinical-xs">
                                <thead>
                                  <tr className="border-b"><th className="text-left p-1 text-muted-foreground">Formulă</th><th className="text-left p-1 text-muted-foreground">Putere Cristalin</th><th className="text-left p-1 text-muted-foreground">Ref. Post-Op</th></tr>
                                </thead>
                                <tbody>
                                  {selected.resultData.iolCalc?.map((c: any, i: number) => (
                                    <tr key={i} className={`border-b ${c.formula === 'Barrett Universal II' ? 'bg-[#FEF3E0] font-semibold text-[#C96A00]' : ''}`}>
                                      <td className="p-2">{c.formula}</td>
                                      <td className="p-2 font-clinical">{c.powerOD}D</td>
                                      <td className="p-2 font-clinical">{c.refrOD}D</td>
                                    </tr>
                                  ))}
                                </tbody>
                              </table>
                            </div>
                          )}
                        </div>
                      ) : (
                        <p className="text-clinical-xs text-muted-foreground italic">Niciun indiciu structurat disponibil.</p>
                      )}
                    </div>
                  )}
                </div>
              </div>
            </div>

            {/* Modal Footer */}
            <div className="p-4 border-t border-border bg-muted/15 flex justify-end gap-2">
              <button 
                onClick={() => setSelected(null)}
                className="px-4 py-2 rounded-lg border border-border text-clinical-sm font-medium hover:bg-muted"
              >
                Închide
              </button>
            </div>
          </div>
        </div>
      )}
    </AppLayout>
  );
};

export default InvestigationsPage;
