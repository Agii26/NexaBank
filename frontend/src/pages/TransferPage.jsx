import { useEffect, useState } from "react";
import { getMyAccounts } from "../api/accounts";
import { transfer } from "../api/transactions";
import Spinner from "../components/Spinner";
import { ArrowLeftRight, CheckCircle } from "lucide-react";

export default function TransferPage() {
  const [accounts, setAccounts] = useState([]);
  const [form, setForm] = useState({ fromAccountId: "", toAccountNumber: "", amount: "", description: "" });
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState(null);

  useEffect(() => {
    getMyAccounts().then(({ data }) => {
      setAccounts(data);
      if (data.length > 0) setForm((f) => ({ ...f, fromAccountId: data[0].id }));
    }).finally(() => setLoading(false));
  }, []);

  const fromAccount = accounts.find((a) => a.id === Number(form.fromAccountId));

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setSubmitting(true);
    try {
      const { data } = await transfer({
        fromAccountId:   Number(form.fromAccountId),
        toAccountNumber: form.toAccountNumber.trim().toUpperCase(),
        amount:          parseFloat(form.amount),
        description:     form.description,
      });
      setSuccess(data);
      setForm((f) => ({ ...f, toAccountNumber: "", amount: "", description: "" }));
    } catch (err) {
      setError(err.response?.data?.message ?? "Transfer failed. Please try again.");
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return <div className="flex justify-center py-20"><Spinner size="lg" /></div>;

  return (
    <div className="max-w-lg mx-auto">
      <div className="mb-8">
        <h2 className="text-2xl font-bold text-slate-900">Transfer</h2>
        <p className="text-slate-500 text-sm mt-1">Send money to any NexaBank account</p>
      </div>

      {success && (
        <div className="mb-6 p-4 bg-green-50 border border-green-200 rounded-xl flex items-start gap-3">
          <CheckCircle className="h-5 w-5 text-green-600 flex-shrink-0 mt-0.5" />
          <div>
            <p className="font-medium text-green-800">Transfer successful!</p>
            <p className="text-sm text-green-600 mt-0.5">
              ₱{Number(success.amount).toLocaleString("en-PH", { minimumFractionDigits: 2 })} sent
            </p>
          </div>
        </div>
      )}

      <div className="bg-white rounded-2xl shadow-sm border border-slate-100 p-6">
        {error && (
          <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg text-red-700 text-sm">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-5">
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">From account</label>
            <select
              required
              value={form.fromAccountId}
              onChange={(e) => setForm({ ...form, fromAccountId: e.target.value })}
              className="w-full px-4 py-2.5 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
            >
              {accounts.filter((a) => a.status === "ACTIVE").map((a) => (
                <option key={a.id} value={a.id}>
                  {a.accountNumber} — ₱{Number(a.balance).toLocaleString("en-PH", { minimumFractionDigits: 2 })} ({a.accountType})
                </option>
              ))}
            </select>
            {fromAccount && (
              <p className="text-xs text-slate-400 mt-1">
                Available: ₱{Number(fromAccount.balance).toLocaleString("en-PH", { minimumFractionDigits: 2 })}
              </p>
            )}
          </div>

          <div className="flex items-center gap-3">
            <div className="flex-1 border-t border-slate-200" />
            <ArrowLeftRight className="h-4 w-4 text-slate-400" />
            <div className="flex-1 border-t border-slate-200" />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">To account number</label>
            <input
              type="text"
              required
              value={form.toAccountNumber}
              onChange={(e) => setForm({ ...form, toAccountNumber: e.target.value })}
              placeholder="PH-20260624-XXXXXX"
              className="w-full px-4 py-2.5 border border-slate-300 rounded-lg text-sm font-mono
                focus:outline-none focus:ring-2 focus:ring-brand-500 uppercase"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Amount (₱)</label>
            <input
              type="number"
              required
              min="0.01"
              step="0.01"
              value={form.amount}
              onChange={(e) => setForm({ ...form, amount: e.target.value })}
              placeholder="0.00"
              className="w-full px-4 py-2.5 border border-slate-300 rounded-lg text-sm
                focus:outline-none focus:ring-2 focus:ring-brand-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Description (optional)</label>
            <input
              type="text"
              value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
              placeholder="e.g. Rent, Utilities"
              className="w-full px-4 py-2.5 border border-slate-300 rounded-lg text-sm
                focus:outline-none focus:ring-2 focus:ring-brand-500"
            />
          </div>

          <button
            type="submit"
            disabled={submitting || accounts.length === 0}
            className="w-full py-2.5 bg-brand-600 hover:bg-brand-700 disabled:opacity-60
              text-white font-semibold rounded-lg transition-colors flex items-center justify-center gap-2"
          >
            {submitting && <Spinner size="sm" />}
            {submitting ? "Processing…" : "Send money"}
          </button>
        </form>
      </div>
    </div>
  );
}
