import React from 'react';
import AppLayout from '@/components/AppLayout';
import { patients } from '@/data/demo-data';
import { ClinicalFlagBadge } from '@/components/StatusBadge';
import { Search, Plus, ChevronRight } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

const PatientsPage: React.FC = () => {
  const navigate = useNavigate();

  return (
    <AppLayout breadcrumbs={[{ label: 'Acasă' }, { label: 'Pacienți' }]}>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-clinical-lg font-bold text-foreground">Pacienți</h1>
        <button className="flex items-center gap-1.5 px-4 py-2.5 rounded-lg bg-primary text-white text-clinical-sm font-semibold hover:bg-primary-700 transition-colors">
          <Plus className="w-4 h-4" /> Pacient Nou
        </button>
      </div>

      {/* Search */}
      <div className="relative mb-4">
        <Search className="absolute left-3 top-2.5 w-4 h-4 text-muted-foreground" />
        <input
          type="text"
          placeholder="Caută pacient după nume, CNP sau ID..."
          className="w-full h-10 rounded-md border border-input bg-card pl-10 pr-4 text-clinical-base focus:border-primary focus:ring-2 focus:ring-primary/25 outline-none transition-colors"
        />
      </div>

      {/* Table */}
      <div className="bg-card rounded-xl border border-border shadow-sm overflow-hidden">
        <table className="w-full">
          <thead>
            <tr className="bg-muted border-b-2 border-border">
              <th className="text-left text-clinical-xs font-semibold text-muted-foreground uppercase tracking-wider py-3 px-4">Pacient</th>
              <th className="text-left text-clinical-xs font-semibold text-muted-foreground uppercase tracking-wider py-3 px-4">CNP</th>
              <th className="text-left text-clinical-xs font-semibold text-muted-foreground uppercase tracking-wider py-3 px-4">Vârstă</th>
              <th className="text-left text-clinical-xs font-semibold text-muted-foreground uppercase tracking-wider py-3 px-4">Diagnostic</th>
              <th className="text-left text-clinical-xs font-semibold text-muted-foreground uppercase tracking-wider py-3 px-4">Flags</th>
              <th className="text-left text-clinical-xs font-semibold text-muted-foreground uppercase tracking-wider py-3 px-4">Ultima vizită</th>
              <th className="w-10"></th>
            </tr>
          </thead>
          <tbody>
            {patients.map((p, i) => (
              <tr
                key={p.id}
                className={`border-b border-border hover:bg-primary-50 transition-colors cursor-pointer ${i % 2 ? 'bg-muted/30' : ''}`}
                onClick={() => navigate(`/patients/${p.id}`)}
              >
                <td className="py-3.5 px-4">
                  <div className="flex items-center gap-3">
                    <div className="w-9 h-9 rounded-full bg-primary-100 text-primary-700 text-[12px] font-semibold flex items-center justify-center">
                      {p.name.split(' ').map(n => n[0]).join('')}
                    </div>
                    <div>
                      <p className="text-clinical-sm font-semibold">{p.name}</p>
                      <p className="text-clinical-xs text-muted-foreground font-clinical">{p.id}</p>
                    </div>
                  </div>
                </td>
                <td className="py-3.5 px-4 text-clinical-sm font-clinical text-clinical-text">{p.cnp}</td>
                <td className="py-3.5 px-4 text-clinical-sm">{p.age} ani</td>
                <td className="py-3.5 px-4">
                  <p className="text-clinical-sm">{p.primaryDiagnosis}</p>
                  <p className="text-clinical-xs text-muted-foreground font-clinical">{p.icdCode}</p>
                </td>
                <td className="py-3.5 px-4">
                  <div className="flex gap-1">{p.clinicalFlags.map(f => <ClinicalFlagBadge key={f} flag={f} />)}</div>
                </td>
                <td className="py-3.5 px-4 text-clinical-sm text-muted-foreground">{p.lastVisit}</td>
                <td className="py-3.5 px-4"><ChevronRight className="w-4 h-4 text-muted-foreground" /></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </AppLayout>
  );
};

export default PatientsPage;
