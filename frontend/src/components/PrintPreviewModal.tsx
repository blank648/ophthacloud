import React from 'react';
import { X, Printer, Download } from 'lucide-react';
import OphthaLogo from '@/components/OphthaLogo';
import { useClinicSettings } from '@/hooks/useAdmin';

interface PrintPreviewModalProps {
  open: boolean;
  onClose: () => void;
  title: string;
  subtitle?: string;
  children: React.ReactNode;
  doctorName?: string;
}

const PrintPreviewModal: React.FC<PrintPreviewModalProps> = ({
  open,
  onClose,
  title,
  subtitle,
  children,
  doctorName = 'Medic Necunoscut',
}) => {
  const { data: clinicData } = useClinicSettings();
  if (!open) return null;

  const handlePrint = () => window.print();

  return (
    <div
      className="fixed inset-0 z-[100] bg-black/60 backdrop-blur-sm flex items-center justify-center p-4 overflow-y-auto print:bg-transparent print:p-0 print:static print:overflow-visible"
      onClick={onClose}
    >
      <div
        className="bg-white text-black rounded-2xl shadow-xl w-full max-w-[820px] my-8 print:shadow-none print:rounded-none print:max-w-none print:my-0"
        onClick={e => e.stopPropagation()}
      >
        {/* Toolbar */}
        <div className="flex items-center justify-between px-5 py-3 border-b border-gray-200 print:hidden">
          <div>
            <p className="text-clinical-sm font-semibold text-gray-900">Previzualizare tipărire</p>
            <p className="text-clinical-xs text-gray-500">{title}</p>
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={handlePrint}
              className="px-3 py-1.5 rounded-lg bg-primary text-white text-clinical-xs font-semibold flex items-center gap-1 hover:opacity-90"
            >
              <Printer className="w-3.5 h-3.5" /> Tipărește
            </button>
            <button
              onClick={handlePrint}
              className="px-3 py-1.5 rounded-lg border border-gray-300 text-clinical-xs font-medium flex items-center gap-1 hover:bg-gray-50"
            >
              <Download className="w-3.5 h-3.5" /> Descarcă PDF
            </button>
            <button
              onClick={onClose}
              className="w-8 h-8 rounded-md hover:bg-gray-100 flex items-center justify-center"
            >
              <X className="w-4 h-4 text-gray-700" />
            </button>
          </div>
        </div>

        {/* A4 sheet */}
        <div className="px-12 py-10 print:px-16 print:py-12 print-area">
          {/* Header */}
          <header className="flex items-start justify-between pb-5 border-b-2 border-gray-300 mb-6">
            <div className="flex items-center gap-3">
              <OphthaLogo size={32} showText={false} />
              <div>
                <h1 className="text-[18px] font-bold text-gray-900">{clinicData?.name || 'Clinica Oftalmologică Demo SRL'}</h1>
                <p className="text-[11px] text-gray-600">{clinicData?.address || 'Str. Victoriei 42, Sector 1, București'} · CIF: {clinicData?.cui || 'RO12345678'}</p>
                <p className="text-[11px] text-gray-600">{clinicData?.phone || '+40 21 123 4567'} · {clinicData?.email || 'contact@visiomed.ro'}</p>
              </div>
            </div>
            <div className="text-right">
              <p className="text-[10px] text-gray-500 uppercase tracking-widest font-semibold">Document</p>
              <p className="text-[12px] font-semibold text-gray-800">{new Date().toLocaleDateString('ro-RO')}</p>
            </div>
          </header>

          {/* Title */}
          <div className="mb-6">
            <h2 className="text-[22px] font-bold uppercase tracking-wide text-gray-900">{title}</h2>
            {subtitle && <p className="text-[12px] text-gray-600 mt-1">{subtitle}</p>}
          </div>

          {/* Body */}
          <div className="text-[13px] text-gray-800 leading-relaxed">{children}</div>

          {/* Footer / signature */}
          <div className="mt-12 flex items-end justify-between">
            <div className="text-[11px] text-gray-500">
              <p>Document generat electronic prin OphthaCloud</p>
              <p>ID: DOC-{Date.now().toString().slice(-8)}</p>
            </div>
            <div className="text-right border-t-2 border-gray-400 pt-2 min-w-[220px]">
              <p className="text-[12px] font-semibold text-gray-900">{doctorName}</p>
              <p className="text-[10px] text-gray-600">Medic primar oftalmolog · Lic. #12345</p>
              <p className="text-[10px] text-gray-500 mt-1 italic">Semnătură digitală certificată</p>
            </div>
          </div>
        </div>
      </div>

      <style>{`
        @media print {
          body * {
            visibility: hidden !important;
          }
          html, body, #root, div:not(.print-area):not(.print-area *), main, section, article {
            position: static !important;
            display: block !important;
            min-height: 0 !important;
            height: 0 !important;
            margin: 0 !important;
            padding: 0 !important;
            border: none !important;
            overflow: visible !important;
            box-shadow: none !important;
          }
          .print-area, .print-area * {
            visibility: visible !important;
          }
          .print-area {
            position: absolute !important;
            left: 0 !important;
            top: 0 !important;
            width: 100% !important;
            height: auto !important;
            margin: 0 !important;
            box-sizing: border-box !important;
          }
          @page {
            margin: 0 !important;
          }
        }
      `}</style>
    </div>
  );
};

export default PrintPreviewModal;
