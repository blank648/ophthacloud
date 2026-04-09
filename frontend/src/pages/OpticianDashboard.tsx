import React, { useState } from 'react';
import AppLayout from '@/components/AppLayout';
import KPICard from '@/components/KPICard';
import { opticalOrders } from '@/data/demo-data';
import type { OpticalOrder } from '@/data/demo-data';
import { Package, CheckCircle2, DollarSign, GripVertical } from 'lucide-react';

type KanbanCol = 'new' | 'lab' | 'qc' | 'fitting' | 'done';

const columns: { key: KanbanCol; label: string; color: string }[] = [
  { key: 'new', label: 'De procesat', color: '#2563EB' },
  { key: 'lab', label: 'La laborator', color: '#F59E0B' },
  { key: 'qc', label: 'Control calitate', color: '#8B5CF6' },
  { key: 'fitting', label: 'Gata de montaj', color: '#10B981' },
  { key: 'done', label: 'Finalizat', color: '#6B7280' },
];

const OpticianDashboard: React.FC = () => {
  const [orders, setOrders] = useState<OpticalOrder[]>(opticalOrders);
  const [dragItem, setDragItem] = useState<string | null>(null);

  const handleDrop = (targetCol: KanbanCol) => {
    if (!dragItem) return;
    setOrders(prev => prev.map(o => o.id === dragItem ? { ...o, status: targetCol } : o));
    setDragItem(null);
  };

  return (
    <AppLayout breadcrumbs={[{ label: 'Acasă' }, { label: 'Dashboard Optician' }]}>
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        <KPICard icon={Package} title="Comenzi active" value={orders.filter(o => o.status !== 'done').length.toString()} />
        <KPICard icon={CheckCircle2} title="Finalizate azi" value="4" iconColor="#10B981" />
        <KPICard icon={DollarSign} title="Revenue comenzi azi" value="3.200 RON" iconColor="#F59E0B" />
      </div>

      {/* Kanban Board */}
      <div className="flex gap-4 overflow-x-auto pb-4">
        {columns.map(col => {
          const colOrders = orders.filter(o => o.status === col.key);
          return (
            <div
              key={col.key}
              className="min-w-[260px] flex-1 bg-muted rounded-xl"
              onDragOver={e => e.preventDefault()}
              onDrop={() => handleDrop(col.key)}
            >
              <div className="p-3 border-b-2" style={{ borderColor: col.color }}>
                <div className="flex items-center justify-between">
                  <h4 className="text-clinical-sm font-semibold">{col.label}</h4>
                  <span className="w-6 h-6 rounded-full bg-card text-clinical-xs font-bold flex items-center justify-center text-foreground shadow-sm">
                    {colOrders.length}
                  </span>
                </div>
              </div>
              <div className="p-2 space-y-2 min-h-[200px]">
                {colOrders.map(order => (
                  <div
                    key={order.id}
                    draggable
                    onDragStart={() => setDragItem(order.id)}
                    className="bg-card rounded-lg border border-border shadow-sm p-3 cursor-grab hover:shadow-md transition-shadow active:cursor-grabbing"
                  >
                    <div className="flex items-start gap-2">
                      <GripVertical className="w-4 h-4 text-muted-foreground shrink-0 mt-0.5" />
                      <div className="flex-1 min-w-0">
                        <p className="text-clinical-sm font-semibold truncate">{order.patientName}</p>
                        <p className="text-clinical-xs text-muted-foreground font-clinical">{order.prescriptionId}</p>
                        <div className="mt-2 flex flex-wrap gap-1">
                          <span className="px-2 py-0.5 rounded text-[10px] bg-primary-50 text-primary-700 font-medium">{order.frameType}</span>
                          <span className="px-2 py-0.5 rounded text-[10px] bg-accent-100 text-accent-600 font-medium">{order.lensType}</span>
                        </div>
                        <div className="mt-2 flex items-center justify-between">
                          <span className="text-clinical-xs text-muted-foreground">{order.createdDate}</span>
                          {order.slaStatus === 'overdue' && (
                            <span className="px-1.5 py-0.5 rounded text-[10px] font-semibold bg-[#FEF2F2] text-[#991B1B]">Întârziat</span>
                          )}
                          {order.priority === 'urgent' && (
                            <span className="px-1.5 py-0.5 rounded text-[10px] font-semibold bg-[#FEF2F2] text-[#C0392B]">Urgent</span>
                          )}
                        </div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          );
        })}
      </div>
    </AppLayout>
  );
};

export default OpticianDashboard;
