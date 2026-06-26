import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { LayoutDashboard, CreditCard, ArrowLeftRight, Clock, LogOut, Shield } from "lucide-react";
import { useAuthStore } from "../store/authStore";

export default function Layout() {
  const { user, logout } = useAuthStore();
  const navigate = useNavigate();
  const isAdmin  = user?.role === "ADMIN";

  const nav = [
    { to: "/dashboard", icon: LayoutDashboard, label: "Dashboard" },
    { to: "/accounts",  icon: CreditCard,      label: "Accounts"  },
    { to: "/transfer",  icon: ArrowLeftRight,  label: "Transfer"  },
    { to: "/history",   icon: Clock,           label: "History"   },
    ...(isAdmin ? [{ to: "/admin", icon: Shield, label: "Admin Panel" }] : []),
  ];

  return (
    <div className="min-h-screen flex bg-slate-50">
      <aside className="w-64 bg-white border-r border-slate-200 flex flex-col fixed h-full z-10">
        <div className="px-6 py-5 border-b border-slate-100">
          <h1 className="text-xl font-bold text-brand-700">NexaBank</h1>
          <p className="text-xs text-slate-400 mt-0.5">Online Banking</p>
        </div>

        <nav className="flex-1 px-3 py-4 space-y-1">
          {nav.map(({ to, icon: Icon, label }) => (
            <NavLink key={to} to={to}
              className={({ isActive }) =>
                `flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors
                 ${isActive
                   ? to === "/admin"
                     ? "bg-purple-50 text-purple-700"
                     : "bg-brand-50 text-brand-700"
                   : "text-slate-600 hover:bg-slate-50 hover:text-slate-900"}`
              }
            >
              <Icon className="h-4 w-4" />
              {label}
            </NavLink>
          ))}
        </nav>

        <div className="p-4 border-t border-slate-100">
          <div className="flex items-center gap-3 mb-3">
            <div className={`h-8 w-8 rounded-full flex items-center justify-center text-sm font-bold flex-shrink-0
              ${isAdmin ? "bg-purple-100 text-purple-700" : "bg-brand-100 text-brand-700"}`}>
              {user?.fullName?.[0]?.toUpperCase() ?? "U"}
            </div>
            <div className="min-w-0">
              <p className="text-sm font-medium text-slate-800 truncate">{user?.fullName}</p>
              <p className="text-xs text-slate-400 capitalize">{user?.role?.toLowerCase()}</p>
            </div>
          </div>
          <button onClick={() => { logout(); navigate("/login"); }}
            className="w-full flex items-center gap-2 px-3 py-2 text-sm text-slate-500
              hover:bg-red-50 hover:text-red-600 rounded-lg transition-colors">
            <LogOut className="h-4 w-4" /> Log out
          </button>
        </div>
      </aside>

      <main className="flex-1 ml-64 p-8">
        <Outlet />
      </main>
    </div>
  );
}
