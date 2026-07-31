const point = (index, count, radius, center = 150) => {
  const angle = -Math.PI / 2 + (index * Math.PI * 2) / count;
  return [center + Math.cos(angle) * radius, center + Math.sin(angle) * radius];
};

const polygon = (count, radius) => Array.from(
  { length: count },
  (_, index) => point(index, count, radius).join(','),
).join(' ');

export default function SourceCategoryRadar({ radar, compact = false }) {
  const axes = radar?.axes || [];
  if (axes.length < 3) {
    return (
      <div className="rounded-xl border border-slate-200 bg-white p-4 text-xs text-slate-500">
        Source-category radar needs at least three active categories.
      </div>
    );
  }

  const values = axes.map((axis, index) =>
    point(index, axes.length, Math.max(0, Math.min(100, Number(axis.percentage) || 0))));

  return (
    <section aria-label="Source category radar" className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm dark:border-slate-700 dark:bg-slate-900">
      <div className="mb-2 flex items-center justify-between gap-3">
        <div>
          <h3 className="text-xs font-bold uppercase tracking-wider text-slate-900 dark:text-slate-100">Source categories</h3>
          <p className="mt-0.5 text-[10px] text-slate-500">
            {radar?.totalSources || 0} distinct Source{radar?.totalSources === 1 ? '' : 's'} used by active Claims
          </p>
        </div>
        <span className="rounded-full bg-indigo-50 px-2 py-1 text-[10px] font-bold text-indigo-700">1 series</span>
      </div>

      <svg viewBox="0 0 300 300" className={`mx-auto w-full ${compact ? 'max-w-[270px]' : 'max-w-[360px]'}`} role="img">
        <title>Distribution of Sources used by active Claims across project categories</title>
        {[20, 40, 60, 80, 100].map(radius => (
          <polygon key={radius} points={polygon(axes.length, radius)} fill="none" stroke="#cbd5e1" strokeWidth="0.7" />
        ))}
        {axes.map((axis, index) => {
          const [x, y] = point(index, axes.length, 100);
          const [labelX, labelY] = point(index, axes.length, 122);
          return (
            <g key={axis.categoryId || axis.code}>
              <line x1="150" y1="150" x2={x} y2={y} stroke="#cbd5e1" strokeWidth="0.7" />
              <text x={labelX} y={labelY} textAnchor="middle" dominantBaseline="middle" fill="currentColor" className="text-[8px] font-bold text-slate-600 dark:text-slate-300">
                {axis.code}
              </text>
            </g>
          );
        })}
        <polygon points={values.map(value => value.join(',')).join(' ')} fill="#6366f1" fillOpacity="0.25" stroke="#4f46e5" strokeWidth="2" />
        {values.map(([x, y], index) => (
          <circle key={axes[index].code} cx={x} cy={y} r="3" fill="#4f46e5">
            <title>{axes[index].label}: {axes[index].sourceCount} ({axes[index].percentage}%)</title>
          </circle>
        ))}
      </svg>

      {!compact && (
        <div className="mt-2 grid grid-cols-2 gap-1.5 sm:grid-cols-3">
          {axes.map(axis => (
            <div key={axis.categoryId || axis.code} className="flex items-center justify-between rounded-md bg-slate-50 px-2 py-1.5 text-[10px] dark:bg-slate-800">
              <span className="truncate font-semibold text-slate-700 dark:text-slate-200" title={axis.label}>{axis.code}</span>
              <span className="ml-2 tabular-nums text-slate-500">{axis.sourceCount} · {axis.percentage}%</span>
            </div>
          ))}
        </div>
      )}
    </section>
  );
}
