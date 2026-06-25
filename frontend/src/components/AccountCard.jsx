import { CreditCard, TrendingUp } from "lucide-react";

const typeColors = {
  SAVINGS:  "from-brand-600 to-brand-800",
  CHECKING: "from-slate-600 to-slate-800",
};

const typeIcons = {
  SAVINGS:  TrendingUp,
  CHECKING: CreditCard,
};

export default function AccountCard({ account, onClick }) {
  const Icon = typeIcons[account.accountType] ?? CreditCard;
  const gradient = typeColors[account.accountType] ?? typeColors.SAVINGS;

  return (
    <button
      onClick={() => onClick?.(account)}
      className={`bg-gradient-to-br ${gradient} text-white rounded-2xl p-6 text-left w-full
        shadow-lg hover:shadow-xl transition-shadow duration-200 focus:outline-none
        focus:ring-2 focus:ring-brand-500 focus:ring-offset-2`}
    >
      <div className="flex justify-between items-start mb-8">
        <div>
          <p className="text-white/70 text-xs font-medium uppercase tracking-widest">
            {account.accountType}
          </p>
          <p className="text-white font-semibold mt-1">{account.ownerName}</p>
        </div>
        <Icon className="h-7 w-7 text-white/60" />
      </div>

      <p className="text-white/70 text-xs mb-1">Account number</p>
      <p className="font-mono text-sm tracking-wider mb-6">{account.accountNumber}</p>

      <div className="flex justify-between items-end">
        <div>
          <p className="text-white/70 text-xs mb-1">Balance</p>
          <p className="text-2xl font-bold balance-amount">
            ₱{Number(account.balance).toLocaleString("en-PH", { minimumFractionDigits: 2 })}
          </p>
        </div>
        <span
          className={`text-xs px-2 py-1 rounded-full font-medium
            ${account.status === "ACTIVE"
              ? "bg-green-400/20 text-green-100"
              : "bg-red-400/20 text-red-100"
            }`}
        >
          {account.status}
        </span>
      </div>
    </button>
  );
}
