import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Route, Routes, Navigate } from "react-router-dom";
import { Toaster as Sonner } from "@/components/ui/sonner";
import { Toaster } from "@/components/ui/toaster";
import { TooltipProvider } from "@/components/ui/tooltip";
import { AppProvider, useApp } from "@/contexts/AppContext";
import LoginPage from "./pages/LoginPage";
import DashboardRouter from "./pages/DashboardRouter";
import PatientsPage from "./pages/PatientsPage";
import PatientProfilePage from "./pages/PatientProfilePage";
import AppointmentsPage from "./pages/AppointmentsPage";
import ConsultationPage from "./pages/ConsultationPage";
import InvestigationsPage from "./pages/InvestigationsPage";
import PrescriptionsPage from "./pages/PrescriptionsPage";
import OpticalERPPage from "./pages/OpticalERPPage";
import InventoryPage from "./pages/InventoryPage";
import BillingPage from "./pages/BillingPage";
import ReportsPage from "./pages/ReportsPage";
import NotificationsPage from "./pages/NotificationsPage";
import PatientPortal from "./pages/PatientPortal";
import SettingsPage from "./pages/SettingsPage";
import AuditLogPage from "./pages/AuditLogPage";
import NotFound from "./pages/NotFound";

const queryClient = new QueryClient();

const AppRoutes = () => {
  const { isLoggedIn } = useApp();

  if (!isLoggedIn) {
    return <LoginPage />;
  }

  return (
    <Routes>
      <Route path="/" element={<Navigate to="/dashboard" replace />} />
      <Route path="/dashboard" element={<DashboardRouter />} />
      <Route path="/patients" element={<PatientsPage />} />
      <Route path="/patients/:id" element={<PatientProfilePage />} />
      <Route path="/appointments" element={<AppointmentsPage />} />
      <Route path="/consultation" element={<ConsultationPage />} />
      <Route path="/investigations" element={<InvestigationsPage />} />
      <Route path="/prescriptions" element={<PrescriptionsPage />} />
      <Route path="/optical" element={<OpticalERPPage />} />
      <Route path="/inventory" element={<InventoryPage />} />
      <Route path="/lab" element={<OpticalERPPage />} />
      <Route path="/billing" element={<BillingPage />} />
      <Route path="/reports" element={<ReportsPage />} />
      <Route path="/notifications" element={<NotificationsPage />} />
      <Route path="/portal" element={<PatientPortal />} />
      <Route path="/settings" element={<SettingsPage />} />
      <Route path="/audit" element={<AuditLogPage />} />
      <Route path="*" element={<NotFound />} />
    </Routes>
  );
};

const App = () => (
  <QueryClientProvider client={queryClient}>
    <TooltipProvider>
      <Toaster />
      <Sonner />
      <AppProvider>
        <BrowserRouter>
          <AppRoutes />
        </BrowserRouter>
      </AppProvider>
    </TooltipProvider>
  </QueryClientProvider>
);

export default App;
