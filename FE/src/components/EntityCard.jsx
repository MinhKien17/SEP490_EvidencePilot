import StatusBadge from './StatusBadge';

export default function EntityCard({ title, subtitle, status, onClick, onEdit, onDelete, children }) {
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
      <div className="flex gap-3 mt-2 opacity-0 group-hover:opacity-100 transition-opacity">
        {onEdit && (
          <button
            onClick={(e) => { e.stopPropagation(); onEdit(); }}
            className="text-[10px] text-indigo-500 hover:text-indigo-700 font-semibold"
          >
            Edit
          </button>
        )}
        {onDelete && (
          <button
            onClick={(e) => { e.stopPropagation(); onDelete(); }}
            className="text-[10px] text-rose-500 hover:text-rose-700 font-semibold"
          >
            Delete
          </button>
        )}
      </div>
    </div>
  );
}
