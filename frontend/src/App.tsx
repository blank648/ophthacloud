import { QueryClientProvider } from "@tanstack/react-query";
import { useAuthStore } from "@/stores/authStore";
import { BrowserRouter, Route, Routes, Navigate } from "react-router-dom";
import { Toaster as Sonner } from "@/components/ui/sonner";
import { Toaster } from "@/components/ui/toaster";
import { TooltipProvider } from "@/components/ui/tooltip";
import { AppProvider, useApp } from "@/contexts/AppContext";
import { DataProvider } from "@/contexts/DataContext";
import { queryClient } from "@/lib/queryClient";
import { ApiErrorBoundary } from "@/components/ApiErrorBoundary";
import { ApiErrorToaster } from "@/components/ApiErrorToaster";
import { AuthGuard } from "@/components/AuthGuard";
import LoginPage from "./pages/LoginPage";
import DashboardRouter from "./pages/DashboardRouter";
import PatientsPage from "./pages/PatientsPage";
import NewPatientPage from "./pages/NewPatientPage";
import PatientProfilePage from "./pages/PatientProfilePage";
import AppointmentsPage from "./pages/AppointmentsPage";
import ConsultationPage from "./pages/ConsultationPage";
import InvestigationsPage from "./pages/InvestigationsPage";
import PrescriptionsPage from "./pages/PrescriptionsPage";
import NewPrescriptionPage from "./pages/NewPrescriptionPage";
import NewUserPage from "./pages/NewUserPage";
import OpticalERPPage from "./pages/OpticalERPPage";
import LabPage from "./pages/LabPage";
import InventoryPage from "./pages/InventoryPage";
import BillingPage from "./pages/BillingPage";
import ReportsPage from "./pages/ReportsPage";
import NotificationsPage from "./pages/NotificationsPage";
import PatientPortal from "./pages/PatientPortal";
import SettingsPage from "./pages/SettingsPage";
import AuditLogPage from "./pages/AuditLogPage";
import UnauthorizedPage from "./pages/UnauthorizedPage";
import NotFound from "./pages/NotFound";
import UserProfilePage from "./pages/UserProfilePage";

const AppRoutes = () => {
  const { isAuthenticated, isLoading } = useAuthStore();

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background">
        <div className="flex flex-col items-center gap-3">
          <div className="w-10 h-10 rounded-full border-4 border-primary/20 border-t-primary animate-spin" />
          <p className="text-clinical-sm text-muted-foreground">Se inițializează sesiunea…</p>
        </div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return (
      <Routes>
        <Route path="/unauthorized" element={<UnauthorizedPage />} />
        <Route path="*" element={<LoginPage />} />
      </Routes>
    );
  }

  return (
    <AuthGuard>
      <Routes>
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="/dashboard" element={<DashboardRouter />} />
        <Route path="/patients" element={<PatientsPage />} />
        <Route path="/patients/new" element={<NewPatientPage />} />
        <Route path="/patients/:id" element={<PatientProfilePage />} />
        <Route path="/appointments" element={<AppointmentsPage />} />
        <Route path="/consultation" element={<ConsultationPage />} />
        <Route path="/investigations" element={<InvestigationsPage />} />
        <Route path="/prescriptions" element={<PrescriptionsPage />} />
        <Route path="/prescriptions/new" element={<NewPrescriptionPage />} />
        <Route path="/settings/users/new" element={<NewUserPage />} />
        <Route path="/optical" element={<OpticalERPPage />} />
        <Route path="/inventory" element={<InventoryPage />} />
        <Route path="/lab" element={<LabPage />} />
        <Route path="/billing" element={<BillingPage />} />
        <Route path="/reports" element={<ReportsPage />} />
        <Route path="/notifications" element={<NotificationsPage />} />
        <Route path="/portal" element={<PatientPortal />} />
        <Route path="/settings" element={<SettingsPage />} />
        <Route path="/profile" element={<UserProfilePage />} />
        <Route path="/audit" element={<AuditLogPage />} />
        <Route path="/unauthorized" element={<UnauthorizedPage />} />
        <Route path="*" element={<NotFound />} />
      </Routes>
    </AuthGuard>
  );
};

const App = () => (
  <QueryClientProvider client={queryClient}>
    <ApiErrorBoundary>
      <TooltipProvider>
        <Toaster />
        <Sonner position="top-right" richColors closeButton />
        <ApiErrorToaster />
        <AppProvider>
          <DataProvider>
            <BrowserRouter>
              <AppRoutes />
            </BrowserRouter>
          </DataProvider>
        </AppProvider>
      </TooltipProvider>
    </ApiErrorBoundary>
  </QueryClientProvider>
);

export default App;
