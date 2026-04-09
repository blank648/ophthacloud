import React, { useState } from 'react';
import AppLayout from '@/components/AppLayout';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { stockItems, StockItem } from '@/data/demo-data';
import { AlertTriangle, Package, TrendingUp, TrendingDown } from 'lucide-react';

const InventoryPage: React.FC = () => {
  const [items, setItems] = useState(stockItems);
  const categories: { key: StockItem['category']; label: string }[] = [
    { key: 'rame', label: 'Rame' },
    { key: 'lentile', label: 'Lentile' },
    { key: 'lentile_contact', label: 'Lentile Contact' },
    { key: 'consumabile', label: 'Consumabile' },
  ];

  const updateStock = (id: string, delta: number) => {
    setItems(prev => prev.map(i => i.id === id ? { ...i, currentStock: Math.max(0, i.currentStock + delta) } : i));
  };

  const totalCost = items.reduce((s, i) => s + i.costPrice * i.currentStock, 0);
  const totalRetail = items.reduce((s, i) => s + i.retailPrice * i.currentStock, 0);
  const lowStockCount = items.filter(i => i.currentStock <= i.minStock).length;

  return (
    <AppLayout breadcrumbs={[{ label: 'Stocuri & Inventar' }]}>
      <h1 className="text-clinical-xl font-bold mb-6">Stocuri & Inventar</h1>

      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <div className="bg-card rounded-xl border border-border shadow-sm p-4">
          <p className="text-clinical-xs text-muted-foreground">Valoare cost</p>
          <p className="text-clinical-lg font-bold font-clinical">{totalCost.toLocaleString()} RON</p>
        </div>
        <div className="bg-card rounded-xl border border-border shadow-sm p-4">
          <p className="text-clinical-xs text-muted-foreground">Valoare retail</p>
          <p className="text-clinical-lg font-bold font-clinical">{totalRetail.toLocaleString()} RON</p>
        </div>
        <div className={`bg-card rounded-xl border shadow-sm p-4 ${lowStockCount > 0 ? 'border-amber-300 bg-amber-50/30' : 'border-border'}`}>
          <p className="text-clinical-xs text-muted-foreground">Sub stoc minim</p>
          <p className={`text-clinical-lg font-bold ${lowStockCount > 0 ? 'text-amber-600' : 'text-green-600'}`}>{lowStockCount}</p>
        </div>
        <div className="bg-card rounded-xl border border-border shadow-sm p-4">
          <p className="text-clinical-xs text-muted-foreground">Total articole</p>
          <p className="text-clinical-lg font-bold font-clinical">{items.length}</p>
        </div>
      </div>

      <Tabs defaultValue="rame">
        <TabsList className="bg-card border border-border rounded-xl p-1 mb-6">
          {categories.map(c => (
            <TabsTrigger key={c.key} value={c.key} className="text-clinical-sm">{c.label}</TabsTrigger>
          ))}
        </TabsList>

        {categories.map(cat => (
          <TabsContent key={cat.key} value={cat.key}>
            <div className="bg-card rounded-xl border border-border shadow-sm overflow-hidden">
              <table className="w-full text-clinical-sm">
                <thead>
                  <tr className="border-b border-border bg-muted/30">
                    <th className="text-left p-3 text-clinical-xs text-muted-foreground font-semibold">Marcă</th>
                    <th className="text-left p-3 text-clinical-xs text-muted-foreground font-semibold">Model</th>
                    {cat.key === 'rame' && <th className="text-left p-3 text-clinical-xs text-muted-foreground font-semibold">Mărime</th>}
                    {cat.key === 'rame' && <th className="text-left p-3 text-clinical-xs text-muted-foreground font-semibold">Culoare</th>}
                    <th className="text-right p-3 text-clinical-xs text-muted-foreground font-semibold">Cost</th>
                    <th className="text-right p-3 text-clinical-xs text-muted-foreground font-semibold">Vânzare</th>
                    <th className="text-center p-3 text-clinical-xs text-muted-foreground font-semibold">Stoc</th>
                    <th className="text-center p-3 text-clinical-xs text-muted-foreground font-semibold">Minim</th>
                    <th className="text-right p-3 text-clinical-xs text-muted-foreground font-semibold">Acțiuni</th>
                  </tr>
                </thead>
                <tbody>
                  {items.filter(i => i.category === cat.key).map(item => {
                    const isLow = item.currentStock <= item.minStock;
                    return (
                      <tr key={item.id} className={`border-b border-border transition-colors ${isLow ? 'bg-amber-50/50' : 'hover:bg-muted/30'}`}>
                        <td className="p-3 font-semibold">{item.brand}</td>
                        <td className="p-3">{item.model}</td>
                        {cat.key === 'rame' && <td className="p-3 font-clinical text-clinical-xs">{item.size || '—'}</td>}
                        {cat.key === 'rame' && <td className="p-3 text-clinical-xs">{item.color || '—'}</td>}
                        <td className="p-3 text-right font-clinical">{item.costPrice}</td>
                        <td className="p-3 text-right font-clinical">{item.retailPrice}</td>
                        <td className="p-3 text-center">
                          <div className="flex items-center justify-center gap-1">
                            <button onClick={() => updateStock(item.id, -1)} className="w-6 h-6 rounded bg-muted hover:bg-muted-foreground/10 text-clinical-xs font-bold">−</button>
                            <span className={`font-clinical font-semibold min-w-[2rem] text-center ${isLow ? 'text-amber-600' : ''}`}>{item.currentStock}</span>
                            <button onClick={() => updateStock(item.id, 1)} className="w-6 h-6 rounded bg-muted hover:bg-muted-foreground/10 text-clinical-xs font-bold">+</button>
                          </div>
                        </td>
                        <td className="p-3 text-center font-clinical text-muted-foreground">{item.minStock}</td>
                        <td className="p-3 text-right">
                          {isLow && <span className="inline-flex items-center gap-1 text-[10px] text-amber-600 font-semibold"><AlertTriangle className="w-3 h-3"/>Sub minim</span>}
                        </td>
                      </tr>
                    );
                  })}
                  {items.filter(i => i.category === cat.key).length === 0 && (
                    <tr><td colSpan={9} className="p-8 text-center text-muted-foreground"><Package className="w-8 h-8 mx-auto mb-2 text-muted-foreground/50"/>Nu există articole în această categorie.</td></tr>
                  )}
                </tbody>
              </table>
            </div>
          </TabsContent>
        ))}
      </Tabs>
    </AppLayout>
  );
};

export default InventoryPage;
