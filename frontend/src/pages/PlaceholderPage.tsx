import React from 'react';
import AppLayout from '@/components/AppLayout';
import { Eye } from 'lucide-react';

const PlaceholderPage: React.FC<{ title: string; description?: string }> = ({ title, description }) => (
  <AppLayout breadcrumbs={[{ label: 'Acasă' }, { label: title }]}>
    <div className="flex flex-col items-center justify-center py-24">
      <div className="w-20 h-20 rounded-full bg-primary-100 flex items-center justify-center mb-6">
        <Eye className="w-10 h-10 text-primary-300" />
      </div>
      <h2 className="text-clinical-lg font-semibold text-foreground mb-2">{title}</h2>
      <p className="text-clinical-sm text-muted-foreground max-w-sm text-center">
        {description || 'Această secțiune este în curs de implementare și va fi disponibilă în curând.'}
      </p>
    </div>
  </AppLayout>
);

export default PlaceholderPage;
