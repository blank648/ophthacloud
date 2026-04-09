import React from 'react';
import { useApp } from '@/contexts/AppContext';
import DoctorDashboard from './DoctorDashboard';
import ReceptionistDashboard from './ReceptionistDashboard';
import ManagerDashboard from './ManagerDashboard';
import OpticianDashboard from './OpticianDashboard';
import PatientPortal from './PatientPortal';

const DashboardRouter: React.FC = () => {
  const { role } = useApp();

  switch (role) {
    case 'doctor': return <DoctorDashboard />;
    case 'receptionist': return <ReceptionistDashboard />;
    case 'manager': return <ManagerDashboard />;
    case 'optician': return <OpticianDashboard />;
    case 'patient': return <PatientPortal />;
    default: return <DoctorDashboard />;
  }
};

export default DashboardRouter;
