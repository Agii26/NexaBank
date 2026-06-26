import { Routes, Route, Navigate } from "react-router-dom";
import { useAuthStore } from "./store/authStore";
import Layout         from "./components/Layout";
import ProtectedRoute from "./components/ProtectedRoute";
import LoginPage      from "./pages/LoginPage";
import RegisterPage   from "./pages/RegisterPage";
import DashboardPage  from "./pages/DashboardPage";
import AccountsPage   from "./pages/AccountsPage";
import TransferPage   from "./pages/TransferPage";
import HistoryPage    from "./pages/HistoryPage";
import AdminPage      from "./pages/AdminPage";

function AdminRoute({ children }) {
  const user = useAuthStore((s) => s.user);
  if (!user) return <Navigate to="/login" replace />;
  if (user.role !== "ADMIN") return <Navigate to="/dashboard" replace />;
  return children;
}

export default function App() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);

  return (
    <Routes>
      <Route path="/login"
        element={isAuthenticated ? <Navigate to="/dashboard" replace /> : <LoginPage />} />
      <Route path="/register"
        element={isAuthenticated ? <Navigate to="/dashboard" replace /> : <RegisterPage />} />

      <Route element={<ProtectedRoute><Layout /></ProtectedRoute>}>
        <Route index                element={<Navigate to="/dashboard" replace />} />
        <Route path="/dashboard"    element={<DashboardPage />} />
        <Route path="/accounts"     element={<AccountsPage />} />
        <Route path="/transfer"     element={<TransferPage />} />
        <Route path="/history"      element={<HistoryPage />} />
        <Route path="/admin"        element={<AdminRoute><AdminPage /></AdminRoute>} />
      </Route>

      <Route path="*"
        element={<Navigate to={isAuthenticated ? "/dashboard" : "/login"} replace />} />
    </Routes>
  );
}
