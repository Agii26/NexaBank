import { useEffect, useState } from "react";
import { getUsers, getAccounts, freezeAccount, unfreezeAccount } from "../api/admin";
import Spinner from "../components/Spinner";
import { Shield, Users, CreditCard, ChevronLeft, ChevronRight } from "lucide-react";

const RoleBadge = ({ role }) => {
  const styles = {
    ADMIN:    "bg-purple-100 text-purple-700",
    CUSTOMER: "bg-blue-100 text-blue-700",
    TELLER:   "bg-amber-100 text-amber-700",
  };
  return (
    <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${styles[role] ?? "bg-slate-100 text-slate-600"}`}>
      {role}
    </span>
  );
};

const StatusBadge = ({ status }) => {
  const styles = {
    ACTIVE: "bg-green-100 text-green-700",
    FROZEN: "bg-red-100 text-red-700",
    CLOSED: "bg-slate-100 text-slate-500",
  };
  return (
    <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${styles[status] ?? ""}`}>
      {status}
    </span>
  );
};

export default function AdminPage() {
  const [tab, setTab]         = useState("users");
  const [users, setUsers]     = useState({ content: [], totalPages: 0, page: 0 });
  const [accounts, setAccounts] = useState({ content: [], totalPages: 0, page: 0 });
  const [loading, setLoading] = useState(true);
  const [actionId, setActionId] = useState(null);

  useEffect(() => { loadTab(tab, 0); }, [tab]);

  const loadTab = async (t, page) => {
    setLoading(true);
    try {
      if (t === "users") {
        const { data } = await getUsers(page);
        setUsers({ ...data, page });
      } else {
        const { data } = await getAccounts(page);
        setAccounts({ ...data, page });
      }
    } finally {
      setLoading(false);
    }
  };

  const handleFreeze = async (id, frozen) => {
    setActionId(id);
    try {
      frozen ? await unfreezeAccount(id) : await freezeAccount(id);
      await loadTab("accounts", accounts.page);
    } finally {
      setActionId(null);
    }
  };

  const current = tab === "users" ? users : accounts;

  return (
    <div className="max-w-6xl mx-auto">
      {/* Header */}
      <div className="flex items-center gap-3 mb-8">
        <div className="h-10 w-10 rounded-xl bg-purple-100 flex items-center justify-center">
          <Shield className="h-5 w-5 text-purple-700" />
        </div>
        <div>
          <h2 className="text-2xl font-bold text-slate-900">Admin Panel</h2>
          <p className="text-slate-500 text-sm">Manage users and accounts</p>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex gap-2 mb-6">
        {[
          { key: "users",    icon: Users,      label: "Users"    },
          { key: "accounts", icon: CreditCard, label: "Accounts" },
        ].map(({ key, icon: Icon, label }) => (
          <button key={key} onClick={() => setTab(key)}
            className={`flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-colors
              ${tab === key ? "bg-brand-600 text-white" : "bg-white border border-slate-200 text-slate-600 hover:border-brand-300"}`}>
            <Icon className="h-4 w-4" /> {label}
          </button>
        ))}
      </div>

      {/* Table */}
      <div className="bg-white rounded-2xl shadow-sm border border-slate-100 overflow-hidden">
        {loading ? (
          <div className="flex justify-center py-16"><Spinner size="lg" /></div>
        ) : tab === "users" ? (
          <table className="w-full text-sm">
            <thead className="bg-slate-50 border-b border-slate-100">
              <tr>
                {["ID", "Name", "Email", "Role", "Active", "Accounts"].map((h) => (
                  <th key={h} className="px-4 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wide">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-50">
              {users.content.map((u) => (
                <tr key={u.id} className="hover:bg-slate-50 transition-colors">
                  <td className="px-4 py-3 text-slate-400">#{u.id}</td>
                  <td className="px-4 py-3 font-medium text-slate-800">{u.fullName}</td>
                  <td className="px-4 py-3 text-slate-500">{u.email}</td>
                  <td className="px-4 py-3"><RoleBadge role={u.role} /></td>
                  <td className="px-4 py-3">
                    <span className={`text-xs font-medium ${u.active ? "text-green-600" : "text-red-500"}`}>
                      {u.active ? "Active" : "Inactive"}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-slate-600">{u.accountCount}</td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : (
          <table className="w-full text-sm">
            <thead className="bg-slate-50 border-b border-slate-100">
              <tr>
                {["Account #", "Owner", "Type", "Balance", "Status", "Actions"].map((h) => (
                  <th key={h} className="px-4 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wide">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-50">
              {accounts.content.map((a) => (
                <tr key={a.id} className="hover:bg-slate-50 transition-colors">
                  <td className="px-4 py-3 font-mono text-xs text-slate-700">{a.accountNumber}</td>
                  <td className="px-4 py-3">
                    <p className="font-medium text-slate-800">{a.ownerName}</p>
                    <p className="text-xs text-slate-400">{a.ownerEmail}</p>
                  </td>
                  <td className="px-4 py-3 text-slate-500">{a.accountType}</td>
                  <td className="px-4 py-3 font-semibold tabular-nums">
                    ₱{Number(a.balance).toLocaleString("en-PH", { minimumFractionDigits: 2 })}
                  </td>
                  <td className="px-4 py-3"><StatusBadge status={a.status} /></td>
                  <td className="px-4 py-3">
                    {a.status !== "CLOSED" && (
                      <button
                        onClick={() => handleFreeze(a.id, a.status === "FROZEN")}
                        disabled={actionId === a.id}
                        className={`px-3 py-1 rounded-lg text-xs font-medium transition-colors disabled:opacity-50
                          ${a.status === "FROZEN"
                            ? "bg-green-50 text-green-700 hover:bg-green-100"
                            : "bg-red-50 text-red-700 hover:bg-red-100"}`}
                      >
                        {actionId === a.id ? "…" : a.status === "FROZEN" ? "Unfreeze" : "Freeze"}
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* Pagination */}
      {current.totalPages > 1 && (
        <div className="flex items-center justify-end gap-2 mt-4">
          <button disabled={current.page === 0}
            onClick={() => loadTab(tab, current.page - 1)}
            className="p-1.5 rounded-lg border border-slate-200 hover:bg-slate-50 disabled:opacity-40">
            <ChevronLeft className="h-4 w-4" />
          </button>
          <span className="text-sm text-slate-500">Page {current.page + 1} of {current.totalPages}</span>
          <button disabled={current.page >= current.totalPages - 1}
            onClick={() => loadTab(tab, current.page + 1)}
            className="p-1.5 rounded-lg border border-slate-200 hover:bg-slate-50 disabled:opacity-40">
            <ChevronRight className="h-4 w-4" />
          </button>
        </div>
      )}
    </div>
  );
}
