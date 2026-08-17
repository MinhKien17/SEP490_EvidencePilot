import { useTranslation } from 'react-i18next';

const STUDENT_ACTIONS = [
  'ADD_CITATION',
  'PARAPHRASE',
  'QUALIFY',
  'SYNTHESIZE',
  'QUOTE',
  'REMOVE',
  'DISMISS_WITH_REASON',
];

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
  decideDraft,
  setDecideDraft,
  onDecideTrace,
  onRunReview,
  aiSourceMatches,
  showHistory,
}) {
  const { t } = useTranslation();
  const traceUpdating = (id) => Boolean(updatingTraceIds?.includes(id));
  const draftTrace = decideDraft?.findingIndex != null
    ? sectionTraces.find(item => item.findingIndex === decideDraft.findingIndex)
    : null;
  const draftCandidates = decideDraft?.findingIndex != null
    ? (aiSourceMatches?.[decideDraft.findingIndex] || [])
    : [];

  return (
    <div className="space-y-4">
      {traceError && (
        <div className="rounded-xl border border-rose-200 bg-rose-50 p-3 text-[11px] text-rose-800">
          {traceError}
          <button type="button" onClick={onRunReview} className="mt-1 font-bold underline">{t('runReviewAgain')}</button>
        </div>
      )}

      {decideDraft && draftTrace && (
        <div className="rounded-xl border border-(--border) bg-(--surface) p-4 shadow-sm">
          <p className="mb-2 text-[10px] font-bold uppercase tracking-wider text-(--text-tertiary)">
            #{decideDraft.findingIndex + 1} {t('recordTraceDecision')}
          </p>
          <div className="space-y-2">
            <label className="block">
              <span className="mb-1 block text-[9px] font-bold uppercase tracking-wider text-(--text-tertiary)">{t('studentAction')}</span>
              <select
                value={decideDraft.action}
                onChange={e => setDecideDraft(prev => ({ ...prev, action: e.target.value }))}
                className="w-full rounded-lg border border-(--border) bg-(--surface) px-2 py-1.5 text-[11px] text-(--text-primary) outline-none">
                <option value="">{t('selectAction')}</option>
                {STUDENT_ACTIONS.map(action => (
                  <option key={action} value={action}>{action.replaceAll('_', ' ')}</option>
                ))}
              </select>
            </label>
            {['ADD_CITATION', 'QUOTE', 'PARAPHRASE', 'SYNTHESIZE', 'QUALIFY'].includes(decideDraft.action) && draftCandidates.length > 0 && (
              <label className="block">
                <span className="mb-1 block text-[9px] font-bold uppercase tracking-wider text-(--text-tertiary)">{t('bindSource')}</span>
                <select
                  value={decideDraft.sourceId}
                  onChange={e => {
                    const selected = draftCandidates.find(c => (c.sourceId || c.documentId) === e.target.value);
                    setDecideDraft(prev => ({
                      ...prev,
                      sourceId: e.target.value,
                      chunkId: selected?.documentChunkId || '',
                      evidenceQuote: selected?.excerpt || prev.evidenceQuote,
                    }));
                  }}
                  className="w-full rounded-lg border border-(--border) bg-(--surface) px-2 py-1.5 text-[11px] text-(--text-primary) outline-none">
                  <option value="">{t('selectSource')}</option>
                  {draftCandidates.map(candidate => (
                    <option key={candidate.documentChunkId} value={candidate.sourceId || candidate.documentId}>
                      {candidate.title || candidate.sourceFilename}
                    </option>
                  ))}
                </select>
              </label>
            )}
            {decideDraft.sourceId && (
              <label className="block">
                <span className="mb-1 block text-[9px] font-bold uppercase tracking-wider text-(--text-tertiary)">{t('evidenceQuote')}</span>
                <textarea
                  value={decideDraft.evidenceQuote}
                  onChange={e => setDecideDraft(prev => ({ ...prev, evidenceQuote: e.target.value }))}
                  rows={2}
                  className="w-full rounded-lg border border-(--border) bg-(--surface) px-2 py-1.5 text-[10px] italic text-(--text-secondary) outline-none resize-y" />
              </label>
            )}
            <label className="block">
              <span className="mb-1 block text-[9px] font-bold uppercase tracking-wider text-(--text-tertiary)">{t('explanation')} *</span>
              <textarea
                value={decideDraft.explanation}
                onChange={e => setDecideDraft(prev => ({ ...prev, explanation: e.target.value }))}
                maxLength={2000}
                required
                rows={2}
                className="w-full rounded-lg border border-(--border) bg-(--surface) px-2 py-1.5 text-[10px] text-(--text-secondary) outline-none resize-y"
                placeholder={t('explanationPlaceholder')} />
              <span className="mt-0.5 block text-right text-[9px] text-(--text-tertiary)">{decideDraft.explanation.length}/2000</span>
            </label>
            <div className="flex gap-2">
              <button type="button" disabled={!decideDraft.action || !decideDraft.explanation.trim() || traceUpdating(draftTrace.id)}
                onClick={async () => {
                  try {
                    await onDecideTrace(draftTrace, {
                      studentAction: decideDraft.action,
                      sourceId: decideDraft.sourceId || null,
                      chunkId: decideDraft.chunkId || null,
                      evidenceQuote: decideDraft.evidenceQuote || null,
                      relation: decideDraft.relation || null,
                      explanation: decideDraft.explanation.trim(),
                    });
                    setDecideDraft(null);
                  } catch { /* toast shown by parent */ }
                }}
                className="flex-1 rounded-lg bg-(--brand) px-2 py-1.5 text-[10px] font-bold text-(--on-brand) hover:bg-(--brand-hover) disabled:opacity-40">
                {traceUpdating(draftTrace.id) ? t('saving') : t('saveDecision')}
              </button>
              <button type="button" onClick={() => setDecideDraft(null)}
                className="rounded-lg border border-(--border) px-2 py-1.5 text-[10px] font-bold text-(--text-secondary) hover:bg-(--surface-tertiary)">
                {t('cancel')}
              </button>
            </div>
          </div>
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