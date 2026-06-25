import { ArrowDownLeft, ArrowUpRight, ArrowLeftRight } from "lucide-react";

const config = {
  DEPOSIT:      { icon: ArrowDownLeft,  color: "text-green-600", bg: "bg-green-50",  sign: "+" },
  WITHDRAWAL:   { icon: ArrowUpRight,   color: "text-red-600",   bg: "bg-red-50",    sign: "-" },
  TRANSFER_OUT: { icon: ArrowUpRight,   color: "text-red-600",   bg: "bg-red-50",    sign: "-" },
  TRANSFER_IN:  { icon: ArrowDownLeft,  color: "text-green-600", bg: "bg-green-50",  sign: "+" },
};

export default function TransactionRow({ tx }) {
  const { icon: Icon, color, bg, sign } = config[tx.type] ?? config.DEPOSIT;
  const label = tx.type.replace(/_/g, " ");
  const date  = new Date(tx.createdAt).toLocaleDateString("en-PH", {
    month: "short", day: "numeric", year: "numeric", hour: "2-digit", minute: "2-digit",
  });

  return (
    <div className="flex items-center gap-4 py-3 border-b border-slate-100 last:border-0">
      <div className={`${bg} ${color} p-2 rounded-full flex-shrink-0`}>
        <Icon className="h-4 w-4" />
      </div>
      <div className="flex-1 min-w-0">
        <p className="font-medium text-slate-800 text-sm capitalize">{label.toLowerCase()}</p>
        <p className="text-xs text-slate-400 truncate">
          {tx.description || (tx.counterpartAccountNumber
            ? `${tx.type.includes("OUT") ? "To" : "From"}: ${tx.counterpartAccountNumber}`
            : "—")}
        </p>
      </div>
      <div className="text-right flex-shrink-0">
        <p className={`font-semibold text-sm balance-amount ${color}`}>
          {sign}₱{Number(tx.amount).toLocaleString("en-PH", { minimumFractionDigits: 2 })}
        </p>
        <p className="text-xs text-slate-400">{date}</p>
      </div>
    </div>
  );
}
