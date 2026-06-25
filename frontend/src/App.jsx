import { Routes, Route, Navigate } from "react-router-dom";
import { useAuthStore } from "./store/authStore";
import Layout          from "./components/Layout";
import ProtectedRoute  from "./components/ProtectedRoute";
import LoginPage       from "./pages/LoginPage";
import RegisterPage    from "./pages/RegisterPage";
import DashboardPage   from "./pages/DashboardPage";
import AccountsPage    from "./pages/AccountsPage";
import TransferPage    from "./pages/TransferPage";
import HistoryPage     from "./pages/HistoryPage";

export default function App() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);

  return (
    <Routes>
      {/* Public routes */}
      <Route path="/login"
        element={isAuthenticated ? <Navigate to="/dashboard" replace /> : <LoginPage />}
      />
      <Route path="/register"
        element={isAuthenticated ? <Navigate to="/dashboard" replace /> : <RegisterPage />}
      />

      {/* Protected routes — Layout renders <Outlet /> for child pages */}
      <Route element={<ProtectedRoute><Layout /></ProtectedRoute>}>
        <Route index                  element={<Navigate to="/dashboard" replace />} />
        <Route path="/dashboard"      element={<DashboardPage />} />
        <Route path="/accounts"       element={<AccountsPage />} />
        <Route path="/transfer"       element={<TransferPage />} />
        <Route path="/history"        element={<HistoryPage />} />
      </Route>

      {/* Fallback */}
      <Route path="*"
        element={<Navigate to={isAuthenticated ? "/dashboard" : "/login"} replace />}
      />
    </Routes>
  );
}
