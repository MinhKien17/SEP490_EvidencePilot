import { useTranslation } from 'react-i18next';

const OUTCOME_CLASSES = {
  RESOLVED: 'bg-emerald-100 text-emerald-700 border border-emerald-200',
  PARTIALLY_RESOLVED: 'bg-amber-100 text-amber-700 border border-amber-200',
  UNRESOLVED: 'bg-slate-100 text-slate-600 border border-slate-200',
  STALE: 'bg-rose-100 text-rose-700 border border-rose-200',
};

export default function TraceEvidenceList({
  sectionTraces,
  updatingTraceIds,
  traceError,
  onRunReview,
  showHistory,
}) {
  const { t } = useTranslation();

  return (
    <div className="space-y-4">
      {traceError && (
        <div className="rounded-xl border border-rose-200 bg-rose-50 p-3 text-[11px] text-rose-800">
          {traceError}
          <button type="button" onClick={onRunReview} className="mt-1 font-bold underline">{t('runReviewAgain')}</button>
        </div>
      )}

      {showHistory && sectionTraces.length > 0 && (
        <div className="space-y-2 rounded-xl border border-(--border) bg-(--surface) p-3">
          <p className="text-[10px] font-bold uppercase tracking-wider text-(--text-tertiary)">{t('savedTraceDecisions')}</p>
          {sectionTraces.slice(0, 10).map(trace => {
            const outcome = trace.outcome;
            return (
              <div key={trace.id} className="rounded-lg border border-(--border-light) bg-(--surface-secondary) p-2.5 text-[10px]">
                <div className="flex items-start justify-between gap-2">
                  <span className="font-bold text-(--text-primary)">#{trace.findingIndex + 1} {trace.studentAction?.replaceAll('_', ' ') || t('notAddressed')}</span>
                  <span className="flex items-center gap-1">
                    {outcome && <span className={`rounded px-1.5 py-0.5 text-[9px] font-bold ${OUTCOME_CLASSES[outcome] || OUTCOME_CLASSES.UNRESOLVED}`}>{outcome.replaceAll('_', ' ')}</span>}
                    {trace.judgment && <span className="rounded bg-emerald-50 px-1.5 py-0.5 font-bold text-emerald-700">{trace.judgment}</span>}
                  </span>
                </div>
                <p className="mt-1 line-clamp-2 italic text-(--text-secondary)">“{trace.excerpt || ''}”</p>
                {trace.explanation && <p className="mt-1 text-(--text-secondary)">{trace.explanation}</p>}
                {trace.aiRecheckJudgment && (
                  <p className="mt-1 rounded bg-indigo-50 p-1.5 text-indigo-700">
                    {t('aiAdvisory')}: <strong>{trace.aiRecheckJudgment}</strong>{trace.aiRecheckReason ? ` — ${trace.aiRecheckReason}` : ''}
                  </p>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}