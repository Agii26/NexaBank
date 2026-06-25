import { Navigate, Outlet } from "react-router-dom";
import { useAuthStore } from "../store/authStore";

// Works both as a wrapper (<ProtectedRoute><Layout /></ProtectedRoute>)
// and as a standalone route guard that renders <Outlet />
export default function ProtectedRoute({ children }) {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  return children ?? <Outlet />;
}
