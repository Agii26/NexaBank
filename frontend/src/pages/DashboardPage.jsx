import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getMyAccounts } from "../api/accounts";
import { getHistory } from "../api/transactions";
import { useAuthStore } from "../store/authStore";
import AccountCard from "../components/AccountCard";
import TransactionRow from "../components/TransactionRow";
import Spinner from "../components/Spinner";
import EmptyState from "../components/EmptyState";
import { CreditCard, Plus } from "lucide-react";

export default function DashboardPage() {
  const { user } = useAuthStore();
  const navigate = useNavigate();
  const [accounts, setAccounts] = useState([]);
  const [recentTx, setRecentTx] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const load = async () => {
      try {
        const { data: accs } = await getMyAccounts();
        setAccounts(accs);
        if (accs.length > 0) {
          const { data: hist } = await getHistory(accs[0].id, 0, 5);
          setRecentTx(hist.content ?? []);
        }
      } catch (e) {
        console.error(e);
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  const totalBalance = accounts.reduce((sum, a) => sum + Number(a.balance), 0);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Spinner size="lg" />
      </div>
    );
  }

  return (
    <div className="max-w-5xl mx-auto">
      {/* Header */}
      <div className="mb-8">
        <h2 className="text-2xl font-bold text-slate-900">
          Good day, {user?.fullName?.split(" ")[0]} 👋
        </h2>
        <p className="text-slate-500 text-sm mt-1">Here's your financial overview</p>
      </div>

      {/* Total balance summary */}
      <div className="bg-gradient-to-r from-brand-600 to-brand-800 text-white rounded-2xl p-6 mb-8 shadow-lg">
        <p className="text-white/70 text-sm mb-1">Total balance across all accounts</p>
        <p className="text-4xl font-bold balance-amount">
          ₱{totalBalance.toLocaleString("en-PH", { minimumFractionDigits: 2 })}
        </p>
        <p className="text-white/60 text-sm mt-2">{accounts.length} account{accounts.length !== 1 ? "s" : ""}</p>
      </div>

      {/* Accounts */}
      <div className="flex items-center justify-between mb-4">
        <h3 className="font-semibold text-slate-900">My Accounts</h3>
        <button
          onClick={() => navigate("/accounts")}
          className="flex items-center gap-1.5 text-sm text-brand-600 hover:text-brand-700 font-medium"
        >
          <Plus className="h-4 w-4" /> Open account
        </button>
      </div>

      {accounts.length === 0 ? (
        <EmptyState
          icon={CreditCard}
          title="No accounts yet"
          description="Open your first savings or checking account to get started"
          action={
            <button
              onClick={() => navigate("/accounts")}
              className="px-4 py-2 bg-brand-600 text-white rounded-lg text-sm font-medium hover:bg-brand-700"
            >
              Open account
            </button>
          }
        />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-8">
          {accounts.map((acc) => (
            <AccountCard key={acc.id} account={acc} onClick={() => navigate("/history")} />
          ))}
        </div>
      )}

      {/* Recent transactions */}
      {recentTx.length > 0 && (
        <>
          <div className="flex items-center justify-between mb-4">
            <h3 className="font-semibold text-slate-900">Recent Transactions</h3>
            <button
              onClick={() => navigate("/history")}
              className="text-sm text-brand-600 hover:text-brand-700 font-medium"
            >
              View all
            </button>
          </div>
          <div className="bg-white rounded-2xl shadow-sm border border-slate-100 px-6 py-2">
            {recentTx.map((tx) => <TransactionRow key={tx.id} tx={tx} />)}
          </div>
        </>
      )}
    </div>
  );
}
