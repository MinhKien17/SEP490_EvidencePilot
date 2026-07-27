import { useState, useMemo } from 'react';

export default function DataTable({ columns, data, pageSize = 10 }) {
  const [sort, setSort] = useState({ key: null, dir: 'asc' });
  const [page, setPage] = useState(0);

  const sorted = useMemo(() => {
    if (!sort.key) return data;
    return [...data].sort((a, b) => {
      const av = a[sort.key], bv = b[sort.key];
      if (av == null) return 1; if (bv == null) return -1;
      const cmp = typeof av === 'string' ? av.localeCompare(bv) : av - bv;
      return sort.dir === 'asc' ? cmp : -cmp;
    });
  }, [data, sort]);

  const totalPages = Math.max(1, Math.ceil(sorted.length / pageSize));
  const pageData = sorted.slice(page * pageSize, (page + 1) * pageSize);

  const toggleSort = (key) => {
    setSort(prev => prev.key === key && prev.dir === 'asc' ? { key, dir: 'desc' } : { key, dir: 'asc' });
    setPage(0);
  };

  return (
    <div>
      <div className="overflow-x-auto rounded-xl border border-slate-200">
        <table className="w-full text-sm">
          <thead>
            <tr className="bg-slate-50 text-left text-[11px] font-bold text-slate-500 uppercase tracking-wider">
              {columns.map(col => (
                <th key={col.key} className={`px-3 py-2.5 ${col.sortable !== false ? 'cursor-pointer hover:text-slate-700 select-none' : ''}`}
                  onClick={() => col.sortable !== false && toggleSort(col.key)}
                >
                  {col.label}
                  {sort.key === col.key && <span className="ml-1">{sort.dir === 'asc' ? '▲' : '▼'}</span>}
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {pageData.map((row, i) => (
              <tr key={row.id || i} className="hover:bg-slate-50 transition-colors">
                {columns.map(col => (
                  <td key={col.key} className="px-3 py-2.5 text-slate-700">{col.render ? col.render(row) : row[col.key]}</td>
                ))}
              </tr>
            ))}
            {pageData.length === 0 && (
              <tr><td colSpan={columns.length} className="px-3 py-8 text-center text-slate-400 text-xs">No data</td></tr>
            )}
          </tbody>
        </table>
      </div>
      {totalPages > 1 && (
        <div className="flex items-center justify-between mt-3 text-xs text-slate-500">
          <span>{sorted.length} items</span>
          <div className="flex gap-1">
            <button disabled={page === 0} onClick={() => setPage(p => p - 1)} className="px-2 py-1 rounded border disabled:opacity-30 hover:bg-slate-50">&lsaquo;</button>
            {Array.from({ length: totalPages }).map((_, i) => (
              <button key={i} onClick={() => setPage(i)}
                className={`px-2 py-1 rounded border ${i === page ? 'bg-indigo-50 border-indigo-300 text-indigo-700' : 'hover:bg-slate-50'}`}
              >{i + 1}</button>
            ))}
            <button disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)} className="px-2 py-1 rounded border disabled:opacity-30 hover:bg-slate-50">&rsaquo;</button>
          </div>
        </div>
      )}
    </div>
  );
}
