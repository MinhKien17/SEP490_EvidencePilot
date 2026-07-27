const COLORS = {
  PENDING: 'bg-amber-50 text-amber-700 border-amber-200',
  REVIEWED: 'bg-emerald-50 text-emerald-700 border-emerald-200',
  RETURNED: 'bg-rose-50 text-rose-700 border-rose-200',
  REJECTED: 'bg-rose-50 text-rose-700 border-rose-200',
  ACTIVE: 'bg-blue-50 text-blue-700 border-blue-200',
  IN_REVIEW: 'bg-amber-50 text-amber-700 border-amber-200',
  COMPLETED: 'bg-emerald-50 text-emerald-700 border-emerald-200',
  ARCHIVED: 'bg-slate-100 text-slate-500 border-slate-200',
  DRAFT: 'bg-gray-100 text-gray-600 border-gray-200',
  APPROVED: 'bg-emerald-50 text-emerald-700 border-emerald-200',
  IN_PROGRESS: 'bg-blue-50 text-blue-700 border-blue-200',
  SUBMITTED_FOR_REVIEW: 'bg-amber-50 text-amber-700 border-amber-200',
  ASSIGNED: 'bg-indigo-50 text-indigo-700 border-indigo-200',
};

export default function StatusBadge({ status, className = '' }) {
  const color = COLORS[status] || 'bg-gray-100 text-gray-600 border-gray-200';
  return (
    <span className={`inline-block text-[9px] font-black uppercase px-2 py-0.5 rounded border ${color} ${className}`}>
      {status || 'UNKNOWN'}
    </span>
  );
}
