import React from 'react';
import { useApp } from '@/contexts/AppContext';
import AppSidebar from '@/components/AppSidebar';
import PageHeader from '@/components/PageHeader';

interface AppLayoutProps {
  children: React.ReactNode;
  breadcrumbs?: { label: string; path?: string }[];
}

const AppLayout: React.FC<AppLayoutProps> = ({ children, breadcrumbs }) => {
  const { sidebarCollapsed } = useApp();

  return (
    <div className="min-h-screen bg-background">
      <AppSidebar />
      <div
        className="transition-all duration-200"
        style={{ marginLeft: sidebarCollapsed ? 64 : 240 }}
      >
        <PageHeader breadcrumbs={breadcrumbs} />
        <main className="p-6">
          {children}
        </main>
      </div>
    </div>
  );
};

export default AppLayout;
