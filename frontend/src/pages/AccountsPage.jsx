import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getMyAccounts, createAccount } from "../api/accounts";
import { deposit, withdraw } from "../api/transactions";
import AccountCard from "../components/AccountCard";
import Spinner from "../components/Spinner";
import EmptyState from "../components/EmptyState";
import { CreditCard, X } from "lucide-react";

function Modal({ title, onClose, children }) {
  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md p-6">
        <div className="flex items-center justify-between mb-5">
          <h3 className="font-semibold text-slate-900">{title}</h3>
          <button onClick={onClose} className="text-slate-400 hover:text-slate-600">
            <X className="h-5 w-5" />
          </button>
        </div>
        {children}
      </div>
    </div>
  );
}

export default function AccountsPage() {
  const [accounts, setAccounts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [modal, setModal] = useState(null); // "create" | "deposit" | "withdraw"
  const [selected, setSelected] = useState(null);
  const [amount, setAmount] = useState("");
  const [description, setDescription] = useState("");
  const [accountType, setAccountType] = useState("SAVINGS");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const load = async () => {
    try {
      const { data } = await getMyAccounts();
      setAccounts(data);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const openModal = (type, account = null) => {
    setModal(type);
    setSelected(account);
    setAmount("");
    setDescription("");
    setError("");
    setSuccess("");
  };

  const closeModal = () => { setModal(null); setSelected(null); };

  const handleCreate = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    setError("");
    try {
      await createAccount({ accountType });
      await load();
      setSuccess("Account opened successfully!");
      setTimeout(closeModal, 1500);
    } catch (err) {
      setError(err.response?.data?.message ?? "Failed to create account");
    } finally {
      setSubmitting(false);
    }
  };

  const handleTransaction = async (e, type) => {
    e.preventDefault();
    setSubmitting(true);
    setError("");
    try {
      const payload = { accountId: selected.id, amount: parseFloat(amount), description };
      if (type === "deposit") await deposit(payload);
      else await withdraw(payload);
      await load();
      setSuccess(`${type === "deposit" ? "Deposit" : "Withdrawal"} successful!`);
      setTimeout(closeModal, 1500);
    } catch (err) {
      setError(err.response?.data?.message ?? "Transaction failed");
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return <div className="flex justify-center py-20"><Spinner size="lg" /></div>;

  return (
    <div className="max-w-4xl mx-auto">
      <div className="flex items-center justify-between mb-8">
        <div>
          <h2 className="text-2xl font-bold text-slate-900">Accounts</h2>
          <p className="text-slate-500 text-sm mt-1">Manage your bank accounts</p>
        </div>
        <button
          onClick={() => openModal("create")}
          className="px-4 py-2 bg-brand-600 hover:bg-brand-700 text-white text-sm font-medium rounded-lg transition-colors"
        >
          + Open account
        </button>
      </div>

      {accounts.length === 0 ? (
        <EmptyState icon={CreditCard} title="No accounts yet" description="Open a savings or checking account to get started" />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {accounts.map((acc) => (
            <div key={acc.id}>
              <AccountCard account={acc} />
              <div className="flex gap-2 mt-2">
                <button onClick={() => openModal("deposit", acc)}
                  className="flex-1 py-2 text-sm bg-green-50 text-green-700 hover:bg-green-100 rounded-lg font-medium transition-colors">
                  Deposit
                </button>
                <button onClick={() => openModal("withdraw", acc)}
                  className="flex-1 py-2 text-sm bg-red-50 text-red-700 hover:bg-red-100 rounded-lg font-medium transition-colors">
                  Withdraw
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Create account modal */}
      {modal === "create" && (
        <Modal title="Open new account" onClose={closeModal}>
          {success ? <p className="text-green-600 text-center py-4 font-medium">{success}</p> : (
            <form onSubmit={handleCreate} className="space-y-4">
              {error && <p className="text-red-600 text-sm bg-red-50 p-3 rounded-lg">{error}</p>}
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Account type</label>
                <select value={accountType} onChange={(e) => setAccountType(e.target.value)}
                  className="w-full px-4 py-2.5 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-brand-500">
                  <option value="SAVINGS">Savings</option>
                  <option value="CHECKING">Checking</option>
                </select>
              </div>
              <button type="submit" disabled={submitting}
                className="w-full py-2.5 bg-brand-600 hover:bg-brand-700 text-white font-semibold rounded-lg disabled:opacity-60 flex items-center justify-center gap-2">
                {submitting && <Spinner size="sm" />} Open account
              </button>
            </form>
          )}
        </Modal>
      )}

      {/* Deposit/Withdraw modal */}
      {(modal === "deposit" || modal === "withdraw") && (
        <Modal title={modal === "deposit" ? "Deposit funds" : "Withdraw funds"} onClose={closeModal}>
          {success ? <p className="text-green-600 text-center py-4 font-medium">{success}</p> : (
            <form onSubmit={(e) => handleTransaction(e, modal)} className="space-y-4">
              {error && <p className="text-red-600 text-sm bg-red-50 p-3 rounded-lg">{error}</p>}
              <p className="text-sm text-slate-500">
                Account: <span className="font-mono font-medium text-slate-700">{selected?.accountNumber}</span>
              </p>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Amount (₱)</label>
                <input type="number" min="0.01" step="0.01" required value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                  className="w-full px-4 py-2.5 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
                  placeholder="0.00" />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Description (optional)</label>
                <input type="text" value={description} onChange={(e) => setDescription(e.target.value)}
                  className="w-full px-4 py-2.5 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
                  placeholder="e.g. Salary, ATM" />
              </div>
              <button type="submit" disabled={submitting}
                className={`w-full py-2.5 font-semibold text-white rounded-lg disabled:opacity-60 flex items-center justify-center gap-2 transition-colors
                  ${modal === "deposit" ? "bg-green-600 hover:bg-green-700" : "bg-red-600 hover:bg-red-700"}`}>
                {submitting && <Spinner size="sm" />}
                {modal === "deposit" ? "Deposit" : "Withdraw"}
              </button>
            </form>
          )}
        </Modal>
      )}
    </div>
  );
}
