import React, { createContext, useContext, useState, useCallback } from 'react';
import {
  patients as seedPatients,
  opticalOrders as seedOrders,
  todayAppointments as seedAppointments,
  notificationRules as seedRules,
  type Patient,
  type Prescription,
  type OpticalOrder,
  type Appointment,
  type AppointmentStatus,
  type NotificationRule,
} from '@/data/demo-data';

export interface ExtendedPrescription extends Prescription {
  patientId: string;
  patientName: string;
}

interface DataContextType {
  patients: Patient[];
  prescriptions: ExtendedPrescription[];
  orders: OpticalOrder[];
  appointments: Appointment[];
  rules: NotificationRule[];

  addPatient: (patient: Patient) => void;
  addPrescription: (rx: ExtendedPrescription) => void;
  addOrder: (order: OpticalOrder) => void;
  updateOrderStatus: (id: string, status: OpticalOrder['status']) => void;
  updateAppointmentStatus: (id: string, status: AppointmentStatus) => void;
  toggleRule: (id: string) => void;
}

const DataContext = createContext<DataContextType | null>(null);

export const useData = () => {
  const ctx = useContext(DataContext);
  if (!ctx) throw new Error('useData must be inside DataProvider');
  return ctx;
};

const seedPrescriptions: ExtendedPrescription[] = seedPatients.flatMap(p =>
  (p.prescriptions || []).map(rx => ({ ...rx, patientId: p.id, patientName: p.name }))
);

export const DataProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [patients, setPatients] = useState<Patient[]>(seedPatients);
  const [prescriptions, setPrescriptions] = useState<ExtendedPrescription[]>(seedPrescriptions);
  const [orders, setOrders] = useState<OpticalOrder[]>(seedOrders);
  const [appointments, setAppointments] = useState<Appointment[]>(seedAppointments);
  const [rules, setRules] = useState<NotificationRule[]>(seedRules);

  const addPatient = useCallback((patient: Patient) => {
    setPatients(prev => [patient, ...prev]);
  }, []);

  const addPrescription = useCallback((rx: ExtendedPrescription) => {
    setPrescriptions(prev => [rx, ...prev]);
  }, []);

  const addOrder = useCallback((order: OpticalOrder) => {
    setOrders(prev => [order, ...prev]);
  }, []);

  const updateOrderStatus = useCallback((id: string, status: OpticalOrder['status']) => {
    setOrders(prev => prev.map(o => o.id === id ? { ...o, status } : o));
  }, []);

  const updateAppointmentStatus = useCallback((id: string, status: AppointmentStatus) => {
    setAppointments(prev => prev.map(a => a.id === id ? { ...a, status } : a));
  }, []);

  const toggleRule = useCallback((id: string) => {
    setRules(prev => prev.map(r => r.id === id ? { ...r, enabled: !r.enabled } : r));
  }, []);

  return (
    <DataContext.Provider
      value={{
        patients,
        prescriptions,
        orders,
        appointments,
        rules,
        addPatient,
        addPrescription,
        addOrder,
        updateOrderStatus,
        updateAppointmentStatus,
        toggleRule,
      }}
    >
      {children}
    </DataContext.Provider>
  );
};
