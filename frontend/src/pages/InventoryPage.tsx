import React, { useState } from 'react';
import { toast } from 'sonner';
import AppLayout from '@/components/AppLayout';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@/lib/apiClient';
import { AlertTriangle, Package, Loader2, Plus, X, Barcode, MapPin } from 'lucide-react';

export interface StockItem {
  id: string;
  serviceItemId?: string;
  name: string;
  category: 'rame' | 'lentile' | 'lentile_contact' | 'consumabile';
  brand: string;
  sku?: string;
  barcode?: string;
  currentStock: number;
  minimumStock: number;
  unitCost: number;
  unitPrice: number;
  currency?: string;
  location?: string;
  isActive?: boolean;
}

const InventoryPage: React.FC = () => {
  const queryClient = useQueryClient();
  const [isModalOpen, setIsModalOpen] = useState(false);

  // Form local state
  const [formCategory, setFormCategory] = useState<StockItem['category']>('rame');
  const [formBrand, setFormBrand] = useState('');
  const [formName, setFormName] = useState('');
  const [formSku, setFormSku] = useState('');
  const [formBarcode, setFormBarcode] = useState('');
  const [formUnitCost, setFormUnitCost] = useState(0);
  const [formUnitPrice, setFormUnitPrice] = useState(0);
  const [formCurrentStock, setFormCurrentStock] = useState(0);
  const [formMinimumStock, setFormMinimumStock] = useState(5);
  const [formLocation, setFormLocation] = useState('');

  const { data: items = [], isLoading } = useQuery<StockItem[]>({
    queryKey: ['stockItems'],
    queryFn: async () => {
      const res = await apiClient.get('/api/v1/optical/stock');
      return res.data.data;
    }
  });

  const { mutate: updateStockLevel } = useMutation({
    mutationFn: async ({ id, newStock }: { id: string; newStock: number }) => {
      return apiClient.patch(`/api/v1/optical/stock/${id}/level`, { newStock });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['stockItems'] });
    },
    onError: () => {
      toast.error('Eroare la actualizarea stocului');
    }
  });

  const { mutate: createStockItem, isPending: isCreating } = useMutation({
    mutationFn: async (payload: Omit<StockItem, 'id'>) => {
      return apiClient.post('/api/v1/optical/stock', payload);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['stockItems'] });
      toast.success('Articol adăugat cu succes în inventar');
      setIsModalOpen(false);
      // Reset form
      setFormBrand('');
      setFormName('');
      setFormSku('');
      setFormBarcode('');
      setFormUnitCost(0);
      setFormUnitPrice(0);
      setFormCurrentStock(0);
      setFormMinimumStock(5);
      setFormLocation('');
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.message || err?.message || 'Eroare la adăugarea articolului';
      toast.error(msg);
    }
  });

  const categories: { key: StockItem['category']; label: string }[] = [
    { key: 'rame', label: 'Rame' },
    { key: 'lentile', label: 'Lentile' },
    { key: 'lentile_contact', label: 'Lentile Contact' },
    { key: 'consumabile', label: 'Consumabile' },
  ];

  const updateStock = (item: StockItem, delta: number) => {
    const newStock = Math.max(0, item.currentStock + delta);
    updateStockLevel({ id: item.id, newStock });
    
    const becameLow = newStock <= item.minimumStock && item.currentStock > item.minimumStock;
    if (becameLow) {
      toast.warning('Sub stoc minim', { description: `${item.brand} ${item.name} · ${newStock} buc` });
    } else {
      toast.success(`Stoc actualizat: ${item.brand} ${item.name}`, { description: `${item.currentStock} → ${newStock}` });
    }
  };

  const handleCreateSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!formBrand.trim() || !formName.trim()) {
      toast.error('Marca și Numele/Modelul sunt obligatorii!');
      return;
    }
    createStockItem({
      category: formCategory,
      brand: formBrand,
      name: formName,
      sku: formSku || undefined,
      barcode: formBarcode || undefined,
      unitCost: formUnitCost,
      unitPrice: formUnitPrice,
      currentStock: formCurrentStock,
      minimumStock: formMinimumStock,
      location: formLocation || undefined,
    });
  };

  const totalCost = items.reduce((s, i) => s + (i.unitCost || 0) * i.currentStock, 0);
  const totalRetail = items.reduce((s, i) => s + (i.unitPrice || 0) * i.currentStock, 0);
  const lowStockCount = items.filter(i => i.currentStock <= i.minimumStock).length;

  return (
    <AppLayout breadcrumbs={[{ label: 'Stocuri & Inventar' }]}>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-clinical-xl font-bold">Stocuri & Inventar</h1>
        <div className="flex items-center gap-3">
          {isLoading && <Loader2 className="w-5 h-5 animate-spin text-muted-foreground" />}
          <button
            onClick={() => setIsModalOpen(true)}
            className="px-4 py-2 rounded-lg bg-primary text-primary-foreground text-clinical-sm font-semibold flex items-center gap-1.5 hover:bg-primary/95 transition-all shadow hover:shadow-md"
          >
            <Plus className="w-4 h-4" /> Adaugă Articol
          </button>
        </div>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <div className="bg-card rounded-xl border border-border shadow-sm p-4">
          <p className="text-clinical-xs text-muted-foreground">Valoare cost</p>
          <p className="text-clinical-lg font-bold font-clinical text-foreground">{totalCost.toLocaleString('ro-RO')} RON</p>
        </div>
        <div className="bg-card rounded-xl border border-border shadow-sm p-4">
          <p className="text-clinical-xs text-muted-foreground">Valoare retail</p>
          <p className="text-clinical-lg font-bold font-clinical text-foreground">{totalRetail.toLocaleString('ro-RO')} RON</p>
        </div>
        <div className={`bg-card rounded-xl border shadow-sm p-4 ${lowStockCount > 0 ? 'border-amber-300 bg-amber-50/30 dark:bg-amber-950/10' : 'border-border'}`}>
          <p className="text-clinical-xs text-muted-foreground">Sub stoc minim</p>
          <p className={`text-clinical-lg font-bold ${lowStockCount > 0 ? 'text-amber-600' : 'text-green-600'}`}>{lowStockCount}</p>
        </div>
        <div className="bg-card rounded-xl border border-border shadow-sm p-4">
          <p className="text-clinical-xs text-muted-foreground">Total articole</p>
          <p className="text-clinical-lg font-bold font-clinical text-foreground">{items.length}</p>
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
                    <th className="text-left p-3 text-clinical-xs text-muted-foreground font-semibold">Model / Nume</th>
                    <th className="text-left p-3 text-clinical-xs text-muted-foreground font-semibold">SKU / Cod Bare</th>
                    <th className="text-left p-3 text-clinical-xs text-muted-foreground font-semibold">Locație</th>
                    <th className="text-right p-3 text-clinical-xs text-muted-foreground font-semibold">Cost</th>
                    <th className="text-right p-3 text-clinical-xs text-muted-foreground font-semibold">Vânzare</th>
                    <th className="text-center p-3 text-clinical-xs text-muted-foreground font-semibold">Stoc</th>
                    <th className="text-center p-3 text-clinical-xs text-muted-foreground font-semibold">Minim</th>
                    <th className="text-right p-3 text-clinical-xs text-muted-foreground font-semibold">Acțiuni</th>
                  </tr>
                </thead>
                <tbody>
                  {items.filter(i => i.category === cat.key).map(item => {
                    const isLow = item.currentStock <= item.minimumStock;
                    return (
                      <tr key={item.id} className={`border-b border-border transition-colors ${isLow ? 'bg-amber-50/40 dark:bg-amber-950/10' : 'hover:bg-muted/30'}`}>
                        <td className="p-3 font-semibold">{item.brand}</td>
                        <td className="p-3">{item.name}</td>
                        <td className="p-3 font-clinical text-clinical-xs text-muted-foreground">
                          {item.sku ? <span className="block">{item.sku}</span> : null}
                          {item.barcode ? <span className="text-[10px] flex items-center gap-1 mt-0.5"><Barcode className="w-3.5 h-3.5" />{item.barcode}</span> : null}
                          {!item.sku && !item.barcode ? '—' : null}
                        </td>
                        <td className="p-3 text-clinical-xs text-muted-foreground">
                          {item.location ? <span className="flex items-center gap-1"><MapPin className="w-3.5 h-3.5" />{item.location}</span> : '—'}
                        </td>
                        <td className="p-3 text-right font-clinical">{item.unitCost ? item.unitCost.toFixed(2) : '0.00'} RON</td>
                        <td className="p-3 text-right font-clinical">{item.unitPrice ? item.unitPrice.toFixed(2) : '0.00'} RON</td>
                        <td className="p-3 text-center">
                          <div className="flex items-center justify-center gap-1">
                            <button onClick={() => updateStock(item, -1)} className="w-6 h-6 rounded bg-muted hover:bg-muted-foreground/10 text-clinical-xs font-bold">−</button>
                            <span className={`font-clinical font-semibold min-w-[2rem] text-center ${isLow ? 'text-amber-600' : ''}`}>{item.currentStock}</span>
                            <button onClick={() => updateStock(item, 1)} className="w-6 h-6 rounded bg-muted hover:bg-muted-foreground/10 text-clinical-xs font-bold">+</button>
                          </div>
                        </td>
                        <td className="p-3 text-center font-clinical text-muted-foreground">{item.minimumStock}</td>
                        <td className="p-3 text-right">
                          {isLow && (
                            <span className="inline-flex items-center gap-1 text-[10px] text-amber-600 font-semibold bg-amber-100 dark:bg-amber-950/20 px-2 py-0.5 rounded-full">
                              <AlertTriangle className="w-3 h-3" /> Sub minim
                            </span>
                          )}
                        </td>
                      </tr>
                    );
                  })}
                  {items.filter(i => i.category === cat.key).length === 0 && !isLoading && (
                    <tr>
                      <td colSpan={9} className="p-8 text-center text-muted-foreground">
                        <Package className="w-8 h-8 mx-auto mb-2 text-muted-foreground/50" />
                        Nu există articole în această categorie.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </TabsContent>
        ))}
      </Tabs>

      {/* Add Stock Item Modal Dialog */}
      {isModalOpen && (
        <div className="fixed inset-0 bg-black/40 z-50 flex items-center justify-center p-4 backdrop-blur-sm animate-in fade-in duration-200" onClick={() => setIsModalOpen(false)}>
          <div className="bg-card rounded-2xl border border-border shadow-2xl max-w-lg w-full overflow-hidden flex flex-col animate-in scale-in duration-300" onClick={e => e.stopPropagation()}>
            <div className="p-6 border-b border-border flex items-center justify-between bg-muted/20">
              <h2 className="text-clinical-md font-bold flex items-center gap-2 text-primary">
                <Package className="w-5 h-5" /> Adăugare Articol Inventar
              </h2>
              <button onClick={() => setIsModalOpen(false)} className="p-1 rounded-lg hover:bg-muted transition-colors">
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleCreateSubmit} className="p-6 space-y-4 max-h-[75vh] overflow-y-auto">
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1">
                  <label className="text-clinical-xs font-semibold text-muted-foreground clinical-label">Categorie</label>
                  <select
                    value={formCategory}
                    onChange={e => setFormCategory(e.target.value as StockItem['category'])}
                    className="w-full rounded-lg border border-border px-3 py-2 text-clinical-sm bg-background"
                  >
                    <option value="rame">Rame</option>
                    <option value="lentile">Lentile</option>
                    <option value="lentile_contact">Lentile Contact</option>
                    <option value="consumabile">Consumabile</option>
                  </select>
                </div>
                <div className="space-y-1">
                  <label className="text-clinical-xs font-semibold text-muted-foreground clinical-label">Marcă (Brand) *</label>
                  <input
                    type="text"
                    required
                    value={formBrand}
                    onChange={e => setFormBrand(e.target.value)}
                    placeholder="ex: Ray-Ban, Zeiss"
                    className="w-full rounded-lg border border-border px-3 py-2 text-clinical-sm bg-background focus:ring-primary focus:border-primary"
                  />
                </div>
              </div>

              <div className="space-y-1">
                <label className="text-clinical-xs font-semibold text-muted-foreground clinical-label">Model / Nume Articol *</label>
                <input
                  type="text"
                  required
                  value={formName}
                  onChange={e => setFormName(e.target.value)}
                  placeholder="ex: Aviator Classic, Progresiv HighIndex 1.67"
                  className="w-full rounded-lg border border-border px-3 py-2 text-clinical-sm bg-background focus:ring-primary"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1">
                  <label className="text-clinical-xs font-semibold text-muted-foreground clinical-label">Cod SKU</label>
                  <input
                    type="text"
                    value={formSku}
                    onChange={e => setFormSku(e.target.value)}
                    placeholder="ex: RB-3025"
                    className="w-full rounded-lg border border-border px-3 py-2 text-clinical-sm bg-background focus:ring-primary"
                  />
                </div>
                <div className="space-y-1">
                  <label className="text-clinical-xs font-semibold text-muted-foreground clinical-label">Cod de bare</label>
                  <input
                    type="text"
                    value={formBarcode}
                    onChange={e => setFormBarcode(e.target.value)}
                    placeholder="ex: 8053672000284"
                    className="w-full rounded-lg border border-border px-3 py-2 text-clinical-sm bg-background focus:ring-primary"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1">
                  <label className="text-clinical-xs font-semibold text-muted-foreground clinical-label">Locație Depozitare</label>
                  <input
                    type="text"
                    value={formLocation}
                    onChange={e => setFormLocation(e.target.value)}
                    placeholder="ex: Sertar 2A, Raftul de sus"
                    className="w-full rounded-lg border border-border px-3 py-2 text-clinical-sm bg-background focus:ring-primary"
                  />
                </div>
                <div className="space-y-1">
                  <label className="text-clinical-xs font-semibold text-muted-foreground clinical-label">Stoc Minim Alertă</label>
                  <input
                    type="number"
                    min="0"
                    required
                    value={formMinimumStock}
                    onChange={e => setFormMinimumStock(parseInt(e.target.value) || 0)}
                    className="w-full rounded-lg border border-border px-3 py-2 text-clinical-sm bg-background focus:ring-primary"
                  />
                </div>
              </div>

              <div className="grid grid-cols-3 gap-3">
                <div className="space-y-1">
                  <label className="text-clinical-xs font-semibold text-muted-foreground clinical-label">Stoc Curent</label>
                  <input
                    type="number"
                    min="0"
                    required
                    value={formCurrentStock}
                    onChange={e => setFormCurrentStock(parseInt(e.target.value) || 0)}
                    className="w-full rounded-lg border border-border px-3 py-2 text-clinical-sm bg-background focus:ring-primary"
                  />
                </div>
                <div className="space-y-1">
                  <label className="text-clinical-xs font-semibold text-muted-foreground clinical-label">Preț Cost (RON)</label>
                  <input
                    type="number"
                    step="0.01"
                    min="0"
                    required
                    value={formUnitCost}
                    onChange={e => setFormUnitCost(parseFloat(e.target.value) || 0)}
                    className="w-full rounded-lg border border-border px-3 py-2 text-clinical-sm bg-background focus:ring-primary"
                  />
                </div>
                <div className="space-y-1">
                  <label className="text-clinical-xs font-semibold text-muted-foreground clinical-label">Preț Vânzare (RON)</label>
                  <input
                    type="number"
                    step="0.01"
                    min="0"
                    required
                    value={formUnitPrice}
                    onChange={e => setFormUnitPrice(parseFloat(e.target.value) || 0)}
                    className="w-full rounded-lg border border-border px-3 py-2 text-clinical-sm bg-background focus:ring-primary"
                  />
                </div>
              </div>

              <div className="pt-4 border-t border-border flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="px-4 py-2 rounded-lg border border-border text-clinical-sm font-semibold hover:bg-muted/50"
                >
                  Anulează
                </button>
                <button
                  type="submit"
                  disabled={isCreating}
                  className="px-5 py-2 rounded-lg bg-primary text-primary-foreground text-clinical-sm font-semibold hover:bg-primary/95 flex items-center gap-1.5"
                >
                  {isCreating ? <Loader2 className="w-4 h-4 animate-spin" /> : 'Salvează Articol'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </AppLayout>
  );
};

export default InventoryPage;
