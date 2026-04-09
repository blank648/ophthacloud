import React, { useState } from 'react';
import AppLayout from '@/components/AppLayout';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { notificationRules } from '@/data/demo-data';
import { Bell, MessageSquare, Mail, Clock, Search } from 'lucide-react';

const recallRules = [
  { diagnosis: 'Glaucom (H40.x)', interval: 'Anual', channel: 'SMS + Email', template: 'Control anual glaucom recomandat.' },
  { diagnosis: 'Retinopatie diabetică (H35.0x)', interval: '6-12 luni', channel: 'SMS + Email', template: 'Control retinopatie diabetică recomandat.' },
  { diagnosis: 'AMD (H35.3x)', interval: '6 luni', channel: 'SMS + Email', template: 'Control AMD recomandat la 6 luni.' },
  { diagnosis: 'Prezbiopie + Progresive (H52.4)', interval: '2 ani', channel: 'SMS', template: 'Progresivele dvs. au 2 ani. Control adaptare.' },
  { diagnosis: 'Post-injecție IVT', interval: '4-6 săptămâni', channel: 'SMS', template: 'Control post-injecție recomandat.' },
  { diagnosis: 'Post-LASIK', interval: 'Protocol: 24h/1w/1m/3m', channel: 'SMS', template: 'Control post-LASIK programat.' },
];

const sendLog = [
  { date: '29.03.2026 08:30', patient: 'Ion Marinescu', channel: 'SMS', type: 'Reamintire −24h', status: 'Livrat', preview: 'Programare mâine ora 09:00...' },
  { date: '29.03.2026 08:00', patient: 'Elena Dumitrescu', channel: 'SMS', type: 'Reamintire −2h', status: 'Livrat', preview: 'Programare în 2 ore! Clinica...' },
  { date: '28.03.2026 16:00', patient: 'Maria Constantin', channel: 'Email', type: 'Studiu satisfacție', status: 'Trimis', preview: 'Evaluați experiența dvs...' },
  { date: '28.03.2026 14:00', patient: 'Andrei Popescu', channel: 'SMS', type: 'Ochelari gata', status: 'Livrat', preview: 'Ochelarii dvs. sunt gata!...' },
  { date: '27.03.2026 09:00', patient: 'Ion Marinescu', channel: 'SMS', type: 'Confirmare', status: 'Livrat', preview: 'Ați fost programat la Dr. Popescu...' },
  { date: '26.03.2026 10:00', patient: 'Mihai Georgescu', channel: 'SMS', type: 'Confirmare', status: 'Eșuat', preview: 'Ați fost programat...' },
];

const NotificationsPage: React.FC = () => {
  const [rules, setRules] = useState(notificationRules);

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
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {rules.map(rule => (
              <div key={rule.id} className="bg-card rounded-xl border border-border shadow-sm p-5">
                <div className="flex items-center justify-between mb-2">
                  <h4 className="text-clinical-sm font-semibold">{rule.type}</h4>
                  <button onClick={() => setRules(prev => prev.map(r => r.id === rule.id ? { ...r, enabled: !r.enabled } : r))}
                    className={`relative w-10 h-5 rounded-full transition-colors ${rule.enabled ? 'bg-primary' : 'bg-muted'}`}>
                    <div className={`absolute top-0.5 w-4 h-4 rounded-full bg-white shadow transition-transform ${rule.enabled ? 'translate-x-5' : 'translate-x-0.5'}`} />
                  </button>
                </div>
                <div className="flex items-center gap-2 mb-2">
                  {rule.channel.includes('SMS') && <span className="flex items-center gap-1 text-[10px] px-1.5 py-0.5 rounded bg-green-100 text-green-700"><MessageSquare className="w-3 h-3"/>SMS</span>}
                  {rule.channel.includes('Email') && <span className="flex items-center gap-1 text-[10px] px-1.5 py-0.5 rounded bg-blue-100 text-blue-700"><Mail className="w-3 h-3"/>Email</span>}
                  <span className="flex items-center gap-1 text-[10px] px-1.5 py-0.5 rounded bg-muted text-muted-foreground"><Clock className="w-3 h-3"/>{rule.timing}</span>
                </div>
                <p className="text-clinical-xs text-muted-foreground italic">{rule.template}</p>
              </div>
            ))}
          </div>
        </TabsContent>

        <TabsContent value="recall">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {recallRules.map((rule, i) => (
              <div key={i} className="bg-card rounded-xl border border-border shadow-sm p-5">
                <h4 className="text-clinical-sm font-semibold mb-1">{rule.diagnosis}</h4>
                <div className="flex items-center gap-2 mb-2">
                  <span className="text-clinical-xs px-2 py-0.5 rounded-full bg-primary/10 text-primary font-semibold">{rule.interval}</span>
                  <span className="text-clinical-xs text-muted-foreground">{rule.channel}</span>
                </div>
                <p className="text-clinical-xs text-muted-foreground italic">{rule.template}</p>
              </div>
            ))}
          </div>
        </TabsContent>

        <TabsContent value="log">
          <div className="bg-card rounded-xl border border-border shadow-sm overflow-hidden">
            <table className="w-full text-clinical-sm">
              <thead><tr className="border-b border-border bg-muted/30">
                <th className="text-left p-3 text-clinical-xs text-muted-foreground font-semibold">Data/Ora</th>
                <th className="text-left p-3 text-clinical-xs text-muted-foreground font-semibold">Pacient</th>
                <th className="text-left p-3 text-clinical-xs text-muted-foreground font-semibold">Canal</th>
                <th className="text-left p-3 text-clinical-xs text-muted-foreground font-semibold">Tip</th>
                <th className="text-left p-3 text-clinical-xs text-muted-foreground font-semibold">Status</th>
                <th className="text-left p-3 text-clinical-xs text-muted-foreground font-semibold">Conținut</th>
              </tr></thead>
              <tbody>
                {sendLog.map((entry, i) => (
                  <tr key={i} className="border-b border-border hover:bg-muted/30">
                    <td className="p-3 font-clinical text-clinical-xs text-muted-foreground">{entry.date}</td>
                    <td className="p-3 font-semibold">{entry.patient}</td>
                    <td className="p-3">{entry.channel === 'SMS' ? <span className="text-green-700 text-clinical-xs">📱 SMS</span> : <span className="text-blue-700 text-clinical-xs">📧 Email</span>}</td>
                    <td className="p-3">{entry.type}</td>
                    <td className="p-3"><span className={`px-2 py-0.5 rounded-full text-[10px] font-semibold ${entry.status==='Livrat'?'bg-green-100 text-green-700':entry.status==='Trimis'?'bg-blue-100 text-blue-700':'bg-red-100 text-red-700'}`}>{entry.status}</span></td>
                    <td className="p-3 text-muted-foreground text-clinical-xs max-w-[200px] truncate">{entry.preview}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </TabsContent>
      </Tabs>
    </AppLayout>
  );
};

export default NotificationsPage;
