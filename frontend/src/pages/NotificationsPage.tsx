import React from 'react';
import { toast } from 'sonner';
import AppLayout from '@/components/AppLayout';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Bell, MessageSquare, Mail, Clock } from 'lucide-react';
import { 
  useNotificationRules, 
  useToggleNotificationRule, 
  useRecallProtocols, 
  useNotificationLogs 
} from '@/hooks/useNotifications';

const NotificationsPage: React.FC = () => {
  const { data: rulesData, isLoading: isLoadingRules } = useNotificationRules();
  const { data: protocolsData, isLoading: isLoadingProtocols } = useRecallProtocols();
  const { data: logsData, isLoading: isLoadingLogs } = useNotificationLogs();
  
  const toggleRuleMutation = useToggleNotificationRule();

  const rules = (rulesData as any)?.data?.content || [];
  const recallRules = (protocolsData as any)?.data?.content || [];
  const sendLog = (logsData as any)?.data?.content || [];

  const getTriggerLabel = (trigger: string) => {
    switch (trigger) {
      case 'APPOINTMENT_CONFIRMED': return 'La programare confirmată';
      case 'APPOINTMENT_CANCELLED': return 'La programare anulată';
      case 'APPOINTMENT_NO_SHOW': return 'La neprezentare programare';
      case 'LOW_STOCK': return 'La atingere stoc critic';
      case 'ORDER_READY': return 'Când comanda optică e gata';
      case 'PRESCRIPTION_SIGNED': return 'La semnare rețetă medicală';
      case 'CONSULTATION_SIGNED': return 'La finalizare fișă consultație';
      default: return trigger;
    }
  };

  return (
    <AppLayout breadcrumbs={[{ label: 'Notificări & Recall' }]}>
      <h1 className="text-clinical-xl font-bold mb-6">Notificări & Recall Inteligent</h1>

      <Tabs defaultValue="rules">
        <TabsList className="bg-card border border-border rounded-xl p-1 mb-6">
          <TabsTrigger value="rules" className="text-clinical-sm">Reguli Automate</TabsTrigger>
          <TabsTrigger value="recall" className="text-clinical-sm">Recall Clinic</TabsTrigger>
          <TabsTrigger value="log" className="text-clinical-sm">Jurnal Trimiteri</TabsTrigger>
        </TabsList>

        <TabsContent value="rules">
          {isLoadingRules ? (
            <div className="text-center py-8 text-clinical-sm text-muted-foreground">Se încarcă regulile...</div>
          ) : rules.length === 0 ? (
            <div className="text-center py-8 text-clinical-sm text-muted-foreground">Nu există reguli înregistrate.</div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {rules.map(rule => {
                const triggerType = rule.configData?.trigger_type || '';
                const channels = rule.configData?.channels || [];
                const template = rule.configData?.template_email_body || '';
                
                return (
                  <div key={rule.id} className="bg-card rounded-xl border border-border shadow-sm p-5 flex flex-col justify-between min-h-[140px] transition-shadow hover:shadow-md">
                    <div>
                      <div className="flex items-center justify-between mb-2">
                        <h4 className="text-clinical-sm font-semibold">{rule.name}</h4>
                        <button 
                          onClick={() => {
                            toggleRuleMutation.mutate(rule.id, {
                              onSuccess: () => {
                                toast(rule.isActive ? 'Regulă dezactivată' : 'Regulă activată', { 
                                  description: rule.name 
                                });
                              }
                            });
                          }}
                          disabled={toggleRuleMutation.isPending}
                          className={`relative w-10 h-5 rounded-full transition-colors cursor-pointer flex-shrink-0 ${
                            rule.isActive ? 'bg-primary' : 'bg-muted'
                          }`}
                        >
                          <div className={`absolute top-0.5 w-4 h-4 rounded-full bg-white shadow transition-transform ${
                            rule.isActive ? 'translate-x-5' : 'translate-x-0.5'
                          }`} />
                        </button>
                      </div>
                      <div className="flex items-center gap-2 mb-3">
                        {channels.includes('SMS') && (
                          <span className="flex items-center gap-1 text-[10px] px-1.5 py-0.5 rounded bg-green-100 text-green-700 font-semibold">
                            <MessageSquare className="w-3 h-3"/>SMS
                          </span>
                        )}
                        {channels.includes('EMAIL') && (
                          <span className="flex items-center gap-1 text-[10px] px-1.5 py-0.5 rounded bg-blue-100 text-blue-700 font-semibold">
                            <Mail className="w-3 h-3"/>Email
                          </span>
                        )}
                        <span className="flex items-center gap-1 text-[10px] px-1.5 py-0.5 rounded bg-muted text-muted-foreground">
                          <Clock className="w-3 h-3"/>{getTriggerLabel(triggerType)}
                        </span>
                      </div>
                    </div>
                    <p className="text-clinical-xs text-muted-foreground italic line-clamp-3">"{template}"</p>
                  </div>
                );
              })}
            </div>
          )}
        </TabsContent>

        <TabsContent value="recall">
          {isLoadingProtocols ? (
            <div className="text-center py-8 text-clinical-sm text-muted-foreground">Se încarcă protocoalele...</div>
          ) : recallRules.length === 0 ? (
            <div className="text-center py-8 text-clinical-sm text-muted-foreground">Nu există protocoale înregistrate.</div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {recallRules.map((rule, i) => (
                <div key={rule.id || i} className="bg-card rounded-xl border border-border shadow-sm p-5 transition-shadow hover:shadow-md">
                  <h4 className="text-clinical-sm font-semibold mb-1">{rule.name}</h4>
                  <div className="flex items-center gap-2 mb-3">
                    <span className="text-clinical-xs px-2 py-0.5 rounded-full bg-primary/10 text-primary font-semibold">
                      La {rule.recallIntervalMonths} luni
                    </span>
                    <span className="text-clinical-xs text-muted-foreground">
                      ICD-10 Trigger: <strong className="font-semibold text-foreground">{rule.icd10Code || 'General'}</strong>
                    </span>
                  </div>
                  <p className="text-clinical-xs text-muted-foreground italic">"{rule.description || 'Protocol de rechemare clinică preventivă.'}"</p>
                </div>
              ))}
            </div>
          )}
        </TabsContent>

        <TabsContent value="log">
          {isLoadingLogs ? (
            <div className="text-center py-8 text-clinical-sm text-muted-foreground">Se încarcă jurnalul...</div>
          ) : sendLog.length === 0 ? (
            <div className="text-center py-8 text-clinical-sm text-muted-foreground">Nicio notificare înregistrată în jurnal.</div>
          ) : (
            <div className="bg-card rounded-xl border border-border shadow-sm overflow-hidden">
              <div className="overflow-x-auto">
                <table className="w-full text-clinical-sm">
                  <thead>
                    <tr className="border-b border-border bg-muted/30">
                      <th className="text-left p-3 text-clinical-xs text-muted-foreground font-semibold">Data/Ora</th>
                      <th className="text-left p-3 text-clinical-xs text-muted-foreground font-semibold">Pacient</th>
                      <th className="text-left p-3 text-clinical-xs text-muted-foreground font-semibold">Canal</th>
                      <th className="text-left p-3 text-clinical-xs text-muted-foreground font-semibold">Subiect / Tip</th>
                      <th className="text-left p-3 text-clinical-xs text-muted-foreground font-semibold">Status</th>
                      <th className="text-left p-3 text-clinical-xs text-muted-foreground font-semibold">Conținut</th>
                    </tr>
                  </thead>
                  <tbody>
                    {sendLog.map((entry, i) => {
                      const formattedDate = entry.sentAt ? new Date(entry.sentAt).toLocaleString('ro-RO', {
                        day: '2-digit', month: '2-digit', year: 'numeric',
                        hour: '2-digit', minute: '2-digit'
                      }) : 'În așteptare';
                      
                      return (
                        <tr key={entry.id || i} className="border-b border-border hover:bg-muted/30 transition-colors">
                          <td className="p-3 font-clinical text-clinical-xs text-muted-foreground whitespace-nowrap">{formattedDate}</td>
                          <td className="p-3 font-semibold">{entry.patientName}</td>
                          <td className="p-3 whitespace-nowrap">
                            {entry.channel === 'SMS' ? (
                              <span className="text-green-700 text-clinical-xs font-medium">📱 SMS</span>
                            ) : (
                              <span className="text-blue-700 text-clinical-xs font-medium">📧 Email</span>
                            )}
                          </td>
                          <td className="p-3 font-medium max-w-[150px] truncate" title={entry.subject}>{entry.subject || 'Notificare automată'}</td>
                          <td className="p-3 whitespace-nowrap">
                            <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                              entry.status === 'SENT' || entry.status === 'DELIVERED' 
                                ? 'bg-green-100 text-green-700' 
                                : entry.status === 'PENDING' 
                                  ? 'bg-amber-100 text-amber-700' 
                                  : 'bg-red-100 text-red-700'
                            }`}>
                              {entry.status === 'SENT' ? 'Trimis' : entry.status === 'DELIVERED' ? 'Livrat' : entry.status === 'PENDING' ? 'În așteptare' : 'Eșuat'}
                            </span>
                          </td>
                          <td className="p-3 text-muted-foreground text-clinical-xs max-w-[200px] truncate" title={entry.bodyPreview}>
                            {entry.bodyPreview}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </TabsContent>
      </Tabs>
    </AppLayout>
  );
};

export default NotificationsPage;
