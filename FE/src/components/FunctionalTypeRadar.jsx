const point = (index, count, radius, center = 150) => {
  const angle = -Math.PI / 2 + (index * Math.PI * 2) / count;
  return [center + Math.cos(angle) * radius, center + Math.sin(angle) * radius];
};

const polygon = (count, radius) => Array.from(
  { length: count },
  (_, index) => point(index, count, radius).join(','),
).join(' ');

const AXES = [
  { code: 'EMP', key: 'EMPIRICAL', label: 'EMPIRICAL', hint: 'Data, experiments, results' },
  { code: 'THEO', key: 'THEORETICAL', label: 'THEORETICAL', hint: 'Literature, background, concepts' },
  { code: 'METH', key: 'METHODOLOGICAL', label: 'METHODOLOGICAL', hint: 'Tools, frameworks, implementation' },
  { code: 'ANAL', key: 'ANALYTICAL', label: 'ANALYTICAL', hint: 'Evaluations, comparisons, interpretations' },
  { code: 'APPL', key: 'APPLIED', label: 'APPLIED', hint: 'Real-world applications, impact' },
];

export default function FunctionalTypeRadar({ stats, compact = false }) {
  if (!stats) {
    return (
      <div className="rounded-xl border border-slate-200 bg-white p-4 text-xs text-slate-500">
        Claim profile unavailable.
      </div>
    );
  }

  const counts = AXES.map(axis => Number(stats.byFunctionalType?.[axis.key]) || 0);
  const max = Math.max(1, ...counts);
  const values = counts.map((count, index) =>
    point(index, AXES.length, (count / max) * 100));

  return (
    <section aria-label="Claim functional type radar" className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm dark:border-slate-700 dark:bg-slate-900">
      <div className="mb-2 flex items-center justify-between gap-3">
        <div>
          <h3 className="text-xs font-bold uppercase tracking-wider text-slate-900 dark:text-slate-100">Claim Development Path</h3>
          <p className="mt-0.5 text-[10px] text-slate-500">
            {stats.totalClaims || 0} Claim{stats.totalClaims === 1 ? '' : 's'} across 5 functional types
          </p>
        </div>
        <span className="rounded-full bg-indigo-50 px-2 py-1 text-[10px] font-bold text-indigo-700">1 series</span>
      </div>

      <svg viewBox="0 0 300 300" className={`mx-auto w-full ${compact ? 'max-w-[270px]' : 'max-w-[360px]'}`} role="img">
        <title>Distribution of Claims by functional type</title>
        {[20, 40, 60, 80, 100].map(radius => (
          <polygon key={radius} points={polygon(AXES.length, radius)} fill="none" stroke="#cbd5e1" strokeWidth="0.7" />
        ))}
        {AXES.map((axis, index) => {
          const [x, y] = point(index, AXES.length, 100);
          const [labelX, labelY] = point(index, AXES.length, 122);
          return (
            <g key={axis.key}>
              <line x1="150" y1="150" x2={x} y2={y} stroke="#cbd5e1" strokeWidth="0.7" />
              <text x={labelX} y={labelY} textAnchor="middle" dominantBaseline="middle" fill="currentColor" className="text-[8px] font-bold text-slate-600 dark:text-slate-300">
                {axis.code}
              </text>
            </g>
          );
        })}
        {stats.totalClaims > 0 && (
          <>
            <polygon points={values.map(value => value.join(',')).join(' ')} fill="#6366f1" fillOpacity="0.25" stroke="#4f46e5" strokeWidth="2" />
            {values.map(([x, y], index) => (
              <circle key={`v-${AXES[index].key}`} cx={x} cy={y} r="3" fill="#4f46e5">
                <title>{AXES[index].label}: {counts[index]} Claim{counts[index] === 1 ? '' : 's'}</title>
              </circle>
            ))}
          </>
        )}
      </svg>

      {stats.totalClaims === 0 && (
        <p className="mt-1 text-center text-[10px] italic text-slate-500">
          No claims yet — add a claim to build your profile.
        </p>
      )}

      {!compact && (
        <div className="mt-2 grid grid-cols-1 gap-1.5 sm:grid-cols-2">
          {AXES.map((axis, index) => (
            <div key={axis.key} className="flex items-center rounded-md bg-slate-50 px-2 py-1.5 text-[10px] dark:bg-slate-800">
              <span className="font-bold text-slate-700 dark:text-slate-200">{axis.code}</span>
              <span className="mx-1 text-slate-400">:</span>
              <span className="truncate font-semibold text-slate-600 dark:text-slate-300" title={axis.hint}>
                {axis.label} ({counts[index]})
              </span>
            </div>
          ))}
        </div>
      )}
    </section>
  );
}
