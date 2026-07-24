import StatusBadge from './StatusBadge';

export default function EntityCard({ title, subtitle, status, onClick, onDelete, children }) {
  return (
    <div
      onClick={onClick}
      className="bg-white border border-slate-200 rounded-xl p-3.5 hover:shadow-md hover:border-indigo-300 transition-all cursor-pointer group"
    >
      <div className="flex justify-between items-start gap-2">
        <div className="min-w-0 flex-1">
          <p className="text-sm font-bold text-slate-800 truncate group-hover:text-indigo-700 transition-colors">
            {title}
          </p>
          {subtitle && <p className="text-[11px] text-slate-500 mt-0.5 truncate">{subtitle}</p>}
        </div>
        {status && <StatusBadge status={status} className="shrink-0" />}
      </div>
      {children && <div className="mt-2 text-xs text-slate-600">{children}</div>}
      {onDelete && (
        <button
          onClick={(e) => { e.stopPropagation(); onDelete(); }}
          className="mt-2 text-[10px] text-rose-500 hover:text-rose-700 font-semibold opacity-0 group-hover:opacity-100 transition-opacity"
        >
          Delete
        </button>
      )}
    </div>
  );
}
