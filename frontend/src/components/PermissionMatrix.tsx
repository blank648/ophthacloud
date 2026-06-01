import React, { useState, useEffect } from 'react';
import { Check, X, Save } from 'lucide-react';
import { usePermissions, useUpdatePermissions } from '@/hooks/useAdmin';
import type { StaffRole, PermissionMatrixDto } from '@/types/admin';
import { toast } from 'sonner';

const ROLES: { label: string, value: StaffRole }[] = [
  { label: 'Doctor', value: 'DOCTOR' },
  { label: 'Optometrist', value: 'OPTOMETRIST' },
  { label: 'Optician', value: 'OPTICAL_TECHNICIAN' },
  { label: 'Recepție', value: 'RECEPTIONIST' },
  { label: 'Manager', value: 'MANAGER' },
  { label: 'Admin', value: 'CLINIC_ADMIN' }
];

const MODULES: { code: string, label: string }[] = [
  { code: 'dashboard', label: 'Dashboard' },
  { code: 'patients', label: 'Pacienți' },
  { code: 'appointments', label: 'Programări' },
  { code: 'emr', label: 'Consultație EMR' },
  { code: 'investigations', label: 'Investigații' },
  { code: 'prescriptions', label: 'Rețete' },
  { code: 'optical', label: 'ERP Optic' },
  { code: 'notifications', label: 'Notificări' },
  { code: 'portal', label: 'Portal Pacienți' },
  { code: 'reports', label: 'Rapoarte' },
  { code: 'admin', label: 'Admin & Setări' },
];

const PERMISSIONS = ['VIEW', 'CREATE', 'EDIT', 'DELETE', 'SIGN', 'EXPORT'] as const;
type Perm = typeof PERMISSIONS[number];

const PermissionMatrix: React.FC = () => {
  const [activeRole, setActiveRole] = useState<StaffRole>('DOCTOR');
  const { data: serverPermissions, isLoading } = usePermissions(activeRole);
  const updatePermissions = useUpdatePermissions();
  
  const [matrix, setMatrix] = useState<Record<string, Record<Perm, boolean>>>({});

  // Sync state with server response
  useEffect(() => {
    if (serverPermissions) {
      const newMatrix: Record<string, Record<Perm, boolean>> = {};
      
      // Initialize all to false first
      MODULES.forEach(m => {
        newMatrix[m.code] = { VIEW: false, CREATE: false, EDIT: false, DELETE: false, SIGN: false, EXPORT: false };
      });

      // Populate from server
      serverPermissions.forEach(sp => {
        if (newMatrix[sp.moduleCode]) {
          newMatrix[sp.moduleCode] = {
            VIEW: sp.canView,
            CREATE: sp.canCreate,
            EDIT: sp.canEdit,
            DELETE: sp.canDelete,
            SIGN: sp.canSign,
            EXPORT: sp.canExport,
          };
        }
      });
      setMatrix(newMatrix);
    }
  }, [serverPermissions]);

  const toggle = (modCode: string, perm: Perm) => {
    setMatrix(prev => ({
      ...prev,
      [modCode]: {
        ...prev[modCode],
        [perm]: !prev[modCode][perm],
      },
    }));
  };

  const handleSave = () => {
    const permissionsToSave = Object.keys(matrix).map(moduleCode => ({
      moduleCode,
      canView: matrix[moduleCode].VIEW || false,
      canCreate: matrix[moduleCode].CREATE || false,
      canEdit: matrix[moduleCode].EDIT || false,
      canDelete: matrix[moduleCode].DELETE || false,
      canSign: matrix[moduleCode].SIGN || false,
      canExport: matrix[moduleCode].EXPORT || false,
    }));

    updatePermissions.mutate({
      role: activeRole,
      permissions: permissionsToSave
    }, {
      onSuccess: () => {
        toast.success(`Permisiuni salvate pentru rolul ${activeRole}`);
      }
    });
  };

  return (
    <div className="bg-card rounded-xl border border-border shadow-sm p-6">
      <h3 className="text-clinical-md font-semibold mb-1">Matrice Permisiuni (RBAC)</h3>
      <p className="text-clinical-xs text-muted-foreground mb-4">Configurați permisiunile per rol × modul. Modificările se aplică imediat tuturor utilizatorilor.</p>

      {/* Role tabs */}
      <div className="flex gap-1 mb-4 border-b border-border overflow-x-auto">
        {ROLES.map(r => (
          <button key={r.value} onClick={() => setActiveRole(r.value)}
            className={`px-3 py-2 text-clinical-sm font-medium whitespace-nowrap border-b-2 transition-colors ${activeRole === r.value ? 'border-primary text-primary' : 'border-transparent text-muted-foreground hover:text-foreground'}`}>
            {r.label}
          </button>
        ))}
      </div>

      {isLoading ? (
         <div className="p-8 text-center text-muted-foreground">Se încarcă permisiunile...</div>
      ) : (
        <>
          <div className="overflow-x-auto">
            <table className="w-full text-clinical-sm">
              <thead>
                <tr className="border-b border-border bg-muted/30">
                  <th className="text-left p-2 text-clinical-xs text-muted-foreground font-semibold">Modul</th>
                  {PERMISSIONS.map(p => (
                    <th key={p} className="text-center p-2 text-clinical-xs text-muted-foreground font-semibold w-20">{p}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {MODULES.map(mod => (
                  <tr key={mod.code} className="border-b border-border hover:bg-muted/20">
                    <td className="p-2 font-medium">{mod.label}</td>
                    {PERMISSIONS.map(perm => {
                      const checked = matrix[mod.code]?.[perm] || false;
                      return (
                        <td key={perm} className="p-2 text-center">
                          <button onClick={() => toggle(mod.code, perm)}
                            className={`w-7 h-7 rounded-md flex items-center justify-center mx-auto transition-colors ${
                              checked ? 'bg-green-100 text-green-700 hover:bg-green-200' : 'bg-muted text-muted-foreground/40 hover:bg-muted-foreground/10'
                            }`}>
                            {checked ? <Check className="w-4 h-4" /> : <X className="w-4 h-4" />}
                          </button>
                        </td>
                      );
                    })}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="mt-4 flex items-center justify-end gap-2">
            <button disabled={updatePermissions.isPending} onClick={handleSave} 
               className="px-4 py-2 rounded-lg bg-primary text-white text-clinical-sm font-semibold flex items-center gap-2 disabled:opacity-50">
              <Save className="w-4 h-4" /> 
              {updatePermissions.isPending ? 'Se salvează...' : 'Salvează modificările'}
            </button>
          </div>
        </>
      )}
    </div>
  );
};

export default PermissionMatrix;
