import React from 'react';
import { useAuthStore } from '@/stores/authStore';
import PageHeader from '@/components/PageHeader';
import { UserCircle, Mail, Briefcase, Shield, Key } from 'lucide-react';

const UserProfilePage: React.FC = () => {
  const userInfo = useAuthStore((s) => s.userInfo);

  const getInitials = () => {
    if (!userInfo?.name) return 'U';
    const parts = userInfo.name.split(' ');
    if (parts.length >= 2) return `${parts[0][0]}${parts[parts.length - 1][0]}`.toUpperCase();
    return parts[0].substring(0, 2).toUpperCase();
  };

  const formatRole = (role: string) => {
    switch (role) {
      case 'DOCTOR': return 'Medic Oftalmolog';
      case 'CLINIC_ADMIN': return 'Manager Clinică';
      case 'RECEPTIONIST': return 'Recepție';
      case 'OPTICIAN': return 'Optician';
      default: return role;
    }
  };

  if (!userInfo) return null;

  return (
    <div className="flex flex-col h-screen overflow-hidden bg-background">
      <PageHeader breadcrumbs={[{ label: 'Acasă', path: '/dashboard' }, { label: 'Profilul Meu' }]} />
      <div className="flex-1 overflow-auto p-4 md:p-8">
        <div className="max-w-3xl mx-auto space-y-6">
          <div className="bg-card border border-border shadow-sm rounded-xl p-6 md:p-8 flex flex-col md:flex-row items-center gap-6">
            <div className="w-24 h-24 rounded-full bg-primary flex items-center justify-center text-white text-3xl font-bold shadow-sm">
              {getInitials()}
            </div>
            <div className="text-center md:text-left">
              <h1 className="text-3xl font-bold text-foreground">{userInfo.name}</h1>
              <p className="text-lg text-muted-foreground">{formatRole(userInfo.role)}</p>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="bg-card border border-border shadow-sm rounded-xl p-6">
              <h2 className="text-xl font-semibold mb-6 flex items-center gap-2">
                <UserCircle className="w-5 h-5 text-primary" />
                Date Personale
              </h2>
              <div className="space-y-4">
                <div>
                  <label className="text-sm font-medium text-muted-foreground flex items-center gap-2">
                    <Mail className="w-4 h-4" /> Email
                  </label>
                  <p className="text-foreground font-medium mt-1">{userInfo.email}</p>
                </div>
                <div>
                  <label className="text-sm font-medium text-muted-foreground flex items-center gap-2">
                    <Briefcase className="w-4 h-4" /> Funcție
                  </label>
                  <p className="text-foreground font-medium mt-1">{formatRole(userInfo.role)}</p>
                </div>
              </div>
            </div>

            <div className="bg-card border border-border shadow-sm rounded-xl p-6">
              <h2 className="text-xl font-semibold mb-6 flex items-center gap-2">
                <Shield className="w-5 h-5 text-primary" />
                Securitate & Acces
              </h2>
              <div className="space-y-4">
                <div>
                  <label className="text-sm font-medium text-muted-foreground flex items-center gap-2">
                    <Key className="w-4 h-4" /> Tenant ID
                  </label>
                  <p className="text-foreground font-medium mt-1 font-mono text-sm">{userInfo.tenantId}</p>
                </div>
                <div>
                  <label className="text-sm font-medium text-muted-foreground flex items-center gap-2">
                    <UserCircle className="w-4 h-4" /> Staff ID
                  </label>
                  <p className="text-foreground font-medium mt-1 font-mono text-sm">{userInfo.staffId}</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default UserProfilePage;
