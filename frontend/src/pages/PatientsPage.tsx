import React, { useState, useEffect } from 'react';
import AppLayout from '@/components/AppLayout';
import { usePatients } from '@/hooks/usePatients';
import { Search, Plus, ChevronRight, Loader2, ChevronLeft } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

const PatientsPage: React.FC = () => {
  const navigate = useNavigate();
  const [searchInput, setSearchInput] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [page, setPage] = useState(0);

  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedSearch(searchInput);
      setPage(0); // reset to page 0 on new search
    }, 400);
    return () => clearTimeout(handler);
  }, [searchInput]);

  const { data: pagedResponse, isLoading, isError } = usePatients({
    q: debouncedSearch || undefined,
    page,
    size: 20,
    sort: 'lastName',
    direction: 'asc'
  });

  const patients = pagedResponse?.data || [];
  const pagination = pagedResponse?.pagination;

  return (
    <AppLayout breadcrumbs={[{ label: 'Acasă' }, { label: 'Pacienți' }]}>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-clinical-lg font-bold text-foreground">Pacienți</h1>
        <button
          onClick={() => navigate('/patients/new')}
          className="flex items-center gap-1.5 px-4 py-2.5 rounded-lg bg-primary text-white text-clinical-sm font-semibold hover:bg-primary-700 transition-colors"
        >
          <Plus className="w-4 h-4" /> Pacient Nou
        </button>
      </div>

      <div className="relative mb-4">
        <Search className="absolute left-3 top-2.5 w-4 h-4 text-muted-foreground" />
        <input
          type="text"
          value={searchInput}
          onChange={(e) => setSearchInput(e.target.value)}
          placeholder="Caută pacient după nume, CNP sau MRN..."
          className="w-full h-10 rounded-md border border-input bg-card pl-10 pr-4 text-clinical-base focus:border-primary focus:ring-2 focus:ring-primary/25 outline-none transition-colors"
        />
      </div>

      <div className="bg-card rounded-xl border border-border shadow-sm overflow-hidden flex flex-col min-h-[400px]">
        {isLoading ? (
          <div className="flex flex-1 items-center justify-center p-8">
            <Loader2 className="w-8 h-8 animate-spin text-primary" />
          </div>
        ) : isError ? (
          <div className="flex flex-1 items-center justify-center p-8 text-destructive">
            A apărut o eroare la încărcarea pacienților.
          </div>
        ) : (
          <>
            <table className="w-full">
              <thead>
                <tr className="bg-muted border-b-2 border-border">
                  <th className="text-left text-clinical-xs font-semibold text-muted-foreground uppercase tracking-wider py-3 px-4">Pacient</th>
                  <th className="text-left text-clinical-xs font-semibold text-muted-foreground uppercase tracking-wider py-3 px-4">MRN</th>
                  <th className="text-left text-clinical-xs font-semibold text-muted-foreground uppercase tracking-wider py-3 px-4">Vârstă</th>
                  <th className="text-left text-clinical-xs font-semibold text-muted-foreground uppercase tracking-wider py-3 px-4">Diagnostic</th>
                  <th className="text-left text-clinical-xs font-semibold text-muted-foreground uppercase tracking-wider py-3 px-4">Ultima vizită</th>
                  <th className="w-10"></th>
                </tr>
              </thead>
              <tbody>
                {patients.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="text-center py-8 text-muted-foreground text-clinical-sm">
                      Nu s-au găsit pacienți.
                    </td>
                  </tr>
                ) : (
                  patients.map((p, i) => (
                    <tr
                      key={p.id}
                      className={`border-b border-border hover:bg-primary-50 transition-colors cursor-pointer ${i % 2 ? 'bg-muted/30' : ''}`}
                      onClick={() => navigate(`/patients/${p.id}`)}
                    >
                      <td className="py-3.5 px-4">
                        <div className="flex items-center gap-3">
                          <div className="w-9 h-9 rounded-full bg-primary-100 text-primary-700 text-[12px] font-semibold flex items-center justify-center">
                            {(p.firstName?.[0] || '') + (p.lastName?.[0] || '')}
                          </div>
                          <div>
                            <p className="text-clinical-sm font-semibold">{p.firstName} {p.lastName}</p>
                            <p className="text-clinical-xs text-muted-foreground font-clinical">{p.phone || p.email}</p>
                          </div>
                        </div>
                      </td>
                      <td className="py-3.5 px-4 text-clinical-sm font-clinical text-clinical-text">{p.mrn}</td>
                      <td className="py-3.5 px-4 text-clinical-sm">{p.age != null ? `${p.age} ani` : '—'}</td>
                      <td className="py-3.5 px-4">
                        {p.activeDiagnoses && p.activeDiagnoses.length > 0 ? (
                          <>
                            <p className="text-clinical-sm">{p.activeDiagnoses[0].icd10Name}</p>
                            <p className="text-clinical-xs text-muted-foreground font-clinical">{p.activeDiagnoses[0].icd10Code} ({p.activeDiagnoses[0].laterality})</p>
                          </>
                        ) : (
                          <p className="text-clinical-sm text-muted-foreground">—</p>
                        )}
                      </td>
                      <td className="py-3.5 px-4 text-clinical-sm text-muted-foreground">{p.lastConsultationDate || '—'}</td>
                      <td className="py-3.5 px-4"><ChevronRight className="w-4 h-4 text-muted-foreground" /></td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
            
            {/* Pagination controls */}
            {pagination && pagination.totalPages > 1 && (
              <div className="flex items-center justify-between px-4 py-3 bg-muted/30 border-t border-border mt-auto">
                <p className="text-clinical-xs text-muted-foreground">
                  Afișare {(pagination.page * pagination.size) + 1} - {Math.min((pagination.page + 1) * pagination.size, pagination.totalElements)} din {pagination.totalElements}
                </p>
                <div className="flex gap-2">
                  <button
                    disabled={!pagination.hasPrevious}
                    onClick={() => setPage(p => p - 1)}
                    className="p-1.5 rounded border border-border disabled:opacity-50 hover:bg-muted"
                  >
                    <ChevronLeft className="w-4 h-4" />
                  </button>
                  <button
                    disabled={!pagination.hasNext}
                    onClick={() => setPage(p => p + 1)}
                    className="p-1.5 rounded border border-border disabled:opacity-50 hover:bg-muted"
                  >
                    <ChevronRight className="w-4 h-4" />
                  </button>
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </AppLayout>
  );
};

export default PatientsPage;
