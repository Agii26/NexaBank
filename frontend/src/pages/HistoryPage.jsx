import { useEffect, useState } from "react";
import { getMyAccounts } from "../api/accounts";
import { getHistory } from "../api/transactions";
import { downloadStatement } from "../api/statements";
import TransactionRow from "../components/TransactionRow";
import Spinner from "../components/Spinner";
import EmptyState from "../components/EmptyState";
import { Clock, ChevronLeft, ChevronRight, Download } from "lucide-react";

export default function HistoryPage() {
  const [accounts, setAccounts]     = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [history, setHistory]       = useState({ content: [], totalPages: 0, page: 0, totalElements: 0 });
  const [loading, setLoading]       = useState(true);
  const [txLoading, setTxLoading]   = useState(false);

  // Statement date range
  const today = new Date().toISOString().split("T")[0];
  const monthAgo = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().split("T")[0];
  const [dateFrom, setDateFrom]   = useState(monthAgo);
  const [dateTo, setDateTo]       = useState(today);
  const [downloading, setDownloading] = useState(false);
  const [dlError, setDlError]     = useState("");

  useEffect(() => {
    getMyAccounts().then(({ data }) => {
      setAccounts(data);
      if (data.length > 0) setSelectedId(data[0].id);
    }).finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    if (!selectedId) return;
    loadHistory(selectedId, 0);
  }, [selectedId]);

  const loadHistory = async (accountId, page) => {
    setTxLoading(true);
    try {
      const { data } = await getHistory(accountId, page, 10);
      setHistory({ ...data, page });
    } finally {
      setTxLoading(false);
    }
  };

  const handleDownload = async () => {
    if (!selectedId) return;
    setDownloading(true);
    setDlError("");
    try {
      await downloadStatement(selectedId, dateFrom, dateTo);
    } catch (err) {
      setDlError(err.response?.data?.message ?? "Download failed. Try a different date range.");
    } finally {
      setDownloading(false);
    }
  };

  if (loading) return <div className="flex justify-center py-20"><Spinner size="lg" /></div>;

  return (
    <div className="max-w-3xl mx-auto">
      <div className="mb-8">
        <h2 className="text-2xl font-bold text-slate-900">Transaction History</h2>
        <p className="text-slate-500 text-sm mt-1">View and download your account statements</p>
      </div>

      {/* Account selector */}
      <div className="flex gap-2 mb-6 flex-wrap">
        {accounts.map((acc) => (
          <button key={acc.id} onClick={() => setSelectedId(acc.id)}
            className={`px-4 py-2 rounded-full text-sm font-medium transition-colors
              ${selectedId === acc.id
                ? "bg-brand-600 text-white"
                : "bg-white border border-slate-200 text-slate-600 hover:border-brand-300"}`}>
            {acc.accountNumber} ({acc.accountType})
          </button>
        ))}
      </div>

      {/* PDF Statement download */}
      <div className="bg-white rounded-2xl border border-slate-100 shadow-sm p-4 mb-6">
        <p className="text-sm font-medium text-slate-700 mb-3">Download PDF Statement</p>
        <div className="flex flex-wrap items-end gap-3">
          <div>
            <label className="block text-xs text-slate-500 mb-1">From</label>
            <input type="date" value={dateFrom} onChange={(e) => setDateFrom(e.target.value)}
              max={dateTo}
              className="px-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-brand-500" />
          </div>
          <div>
            <label className="block text-xs text-slate-500 mb-1">To</label>
            <input type="date" value={dateTo} onChange={(e) => setDateTo(e.target.value)}
              min={dateFrom} max={today}
              className="px-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-brand-500" />
          </div>
          <button onClick={handleDownload} disabled={downloading || !selectedId}
            className="flex items-center gap-2 px-4 py-2 bg-brand-600 hover:bg-brand-700
              text-white text-sm font-medium rounded-lg disabled:opacity-60 transition-colors">
            {downloading
              ? <><Spinner size="sm" /> Generating…</>
              : <><Download className="h-4 w-4" /> Download PDF</>}
          </button>
        </div>
        {dlError && <p className="text-red-600 text-xs mt-2">{dlError}</p>}
      </div>

      {/* Transaction list */}
      <div className="bg-white rounded-2xl shadow-sm border border-slate-100 px-6 py-2 min-h-64">
        {txLoading ? (
          <div className="flex justify-center py-12"><Spinner /></div>
        ) : history.content.length === 0 ? (
          <EmptyState icon={Clock} title="No transactions yet"
            description="Deposit or transfer funds to see your history" />
        ) : (
          history.content.map((tx) => <TransactionRow key={tx.id} tx={tx} />)
        )}
      </div>

      {/* Pagination */}
      {history.totalPages > 1 && (
        <div className="flex items-center justify-between mt-4 text-sm text-slate-500">
          <span>{history.totalElements} transactions total</span>
          <div className="flex items-center gap-2">
            <button disabled={history.page === 0}
              onClick={() => loadHistory(selectedId, history.page - 1)}
              className="p-1.5 rounded-lg border border-slate-200 hover:bg-slate-50 disabled:opacity-40">
              <ChevronLeft className="h-4 w-4" />
            </button>
            <span>Page {history.page + 1} of {history.totalPages}</span>
            <button disabled={history.page >= history.totalPages - 1}
              onClick={() => loadHistory(selectedId, history.page + 1)}
              className="p-1.5 rounded-lg border border-slate-200 hover:bg-slate-50 disabled:opacity-40">
              <ChevronRight className="h-4 w-4" />
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
