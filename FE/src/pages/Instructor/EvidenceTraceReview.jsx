import { useState, useEffect, useCallback } from 'react';
import { Link, useParams } from 'react-router-dom';
import { AppHeader, LoadingSkeleton, StatusBadge, Modal } from '../../components';
import { instructorText, commonText } from '../../locales';
import { useLanguage } from '../../context/LanguageContext';
import api from '../../api';

const OUTCOMES = ['RESOLVED', 'PARTIALLY_RESOLVED', 'UNRESOLVED', 'STALE'];
const JUDGMENTS = ['EFFECTIVE', 'PARTIAL', 'INEFFECTIVE'];

const OUTCOME_CLASSES = {
  RESOLVED: 'bg-emerald-100 text-emerald-700 border border-emerald-200',
  PARTIALLY_RESOLVED: 'bg-amber-100 text-amber-700 border border-amber-200',
  UNRESOLVED: 'bg-slate-100 text-slate-600 border border-slate-200',
  STALE: 'bg-rose-100 text-rose-700 border border-rose-200',
};

export default function EvidenceTraceReview() {
  const { id } = useParams();
  const { language } = useLanguage();
  const ct = commonText[language];
  const t = instructorText[language];
  const [loading, setLoading] = useState(true);
  const [project, setProject] = useState(null);
  const [traces, setTraces] = useState([]);
  const [outcomeFilter, setOutcomeFilter] = useState('');
  const [savingId, setSavingId] = useState(null);
  const [reviewing, setReviewing] = useState(null);
  const [error, setError] = useState('');

  const loadTraces = useCallback(async () => {
    try {
      const params = outcomeFilter ? { outcome: outcomeFilter } : {};
      const r = await api.get(`/api/projects/${id}/evidence-traces`, { params });
      setTraces(r.data || []);
      setError('');
    } catch {
      setError(t.loadEvidenceTracesFailed);
    }
  }, [id, outcomeFilter, t]);

  useEffect(() => {
    api.get(`/api/projects/${id}`)
      .then(r => setProject(r.data))
      .catch(() => setError(t.projectLoadFailed));
  }, [id, t]);

  useEffect(() => {
    setLoading(true);
    loadTraces().finally(() => setLoading(false));
  }, [loadTraces]);

  const handleReview = async (traceId, judgment, instructorFeedback) => {
    setSavingId(traceId);
    try {
      const r = await api.patch(`/api/projects/${id}/evidence-traces/${traceId}/review`, {
        judgment,
        instructorFeedback: instructorFeedback || null,
      });
      setTraces(prev => prev.map(item => String(item.id) === String(traceId) ? r.data : item));
      setReviewing(null);
    } catch {
      setError(t.saveTraceJudgmentFailed);
    } finally {
      setSavingId(null);
    }
  };

  const summary = {
    total: traces.length,
    RESOLVED: traces.filter(x => x.outcome === 'RESOLVED').length,
    STALE: traces.filter(x => x.outcome === 'STALE').length,
    withAction: traces.filter(x => x.studentAction).length,
  };

  return (
    <div className="min-h-screen overflow-x-hidden bg-[var(--page-bg)] text-[var(--text-primary)] font-sans">
      <AppHeader />
      <main className="mx-auto max-w-6xl p-4 sm:p-6 lg:p-8">
        <Link to={`/instructor/projects/${id}`} className="text-xs font-bold text-[var(--text-secondary)] transition-colors hover:text-[var(--brand-foreground)]">&larr; {ct.back}</Link>
        <div className="mt-2 mb-6 flex flex-wrap items-start justify-between gap-3">
          <div className="min-w-0 flex-1">
            <h1 className="text-2xl font-black text-[var(--brand-foreground)]">{t.evidenceTraceReview}</h1>
            <p className="mt-1 text-sm text-[var(--text-secondary)]">{project?.title || ''}</p>
            {error && <p className="mt-2 text-xs font-bold text-rose-600">{error}</p>}
          </div>
          <div className="flex shrink-0 flex-wrap items-center gap-2 text-[11px]">
            <span className="rounded-lg border border-[var(--border)] bg-[var(--surface)] px-2.5 py-1.5 font-bold text-[var(--text-secondary)]">{t.tracesTotal}: {summary.total}</span>
            <span className="rounded-lg border border-emerald-200 bg-emerald-50 px-2.5 py-1.5 font-bold text-emerald-700">{t.outcomeResolved}: {summary.RESOLVED}</span>
            <span className="rounded-lg border border-rose-200 bg-rose-50 px-2.5 py-1.5 font-bold text-rose-700">{t.outcomeStale}: {summary.STALE}</span>
            <span className="rounded-lg border border-[var(--border)] bg-[var(--surface)] px-2.5 py-1.5 font-bold text-[var(--text-secondary)]">{t.tracesWithAction}: {summary.withAction}</span>
          </div>
        </div>

        <div className="mb-4 rounded-xl border border-amber-200 bg-amber-50 p-3 text-[11px] leading-relaxed text-amber-900">
          <p className="font-black">{t.traceHowItWorksTitle}</p>
          <p className="mt-1">{t.traceHowItWorksBody}</p>
          <p className="mt-1 text-[10px] opacity-80">{t.traceSteps}</p>
        </div>

        <div className="mb-4 flex flex-wrap items-center gap-2">
          <span className="text-xs font-bold text-[var(--text-tertiary)] uppercase tracking-wider">{t.filterByOutcome}:</span>
          <button onClick={() => setOutcomeFilter('')}
            className={`rounded-lg px-2.5 py-1 text-xs font-bold transition ${!outcomeFilter ? 'bg-[var(--brand)] text-white' : 'border border-[var(--border)] bg-[var(--surface)] text-[var(--text-secondary)] hover:bg-[var(--surface-secondary)]'}`}>
            {t.allOutcomes}
          </button>
          {OUTCOMES.map(outcome => (
            <button key={outcome} onClick={() => setOutcomeFilter(outcome)}
              className={`rounded-lg px-2.5 py-1 text-xs font-bold transition ${outcomeFilter === outcome ? 'bg-[var(--brand)] text-white' : 'border border-[var(--border)] bg-[var(--surface)] text-[var(--text-secondary)] hover:bg-[var(--surface-secondary)]'}`}>
              {outcome.replaceAll('_', ' ')}
            </button>
          ))}
        </div>

        {loading ? (
          <LoadingSkeleton count={5} />
        ) : traces.length === 0 ? (
          <div className="rounded-2xl border border-dashed border-[var(--border)] bg-[var(--surface)] p-10 text-center text-xs text-[var(--text-tertiary)]">{t.noEvidenceTraces}</div>
        ) : (
          <div className="overflow-hidden rounded-2xl border border-[var(--border)] bg-[var(--surface)] shadow-sm">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="border-b border-[var(--border)] bg-[var(--surface-secondary)]">
                  <tr className="text-[10px] uppercase tracking-wider text-[var(--text-tertiary)]">
                    <th className="px-4 py-3 font-black">{t.section}</th>
                    <th className="px-4 py-3 font-black">{t.findingIndex}</th>
                    <th className="px-4 py-3 font-black">{t.excerpt}</th>
                    <th className="px-4 py-3 font-black">{t.studentAction}</th>
                    <th className="px-4 py-3 font-black">{t.evidenceQuote}</th>
                    <th className="px-4 py-3 font-black">{t.afterPassage}</th>
                    <th className="px-4 py-3 font-black">{t.outcome}</th>
                    <th className="px-4 py-3 font-black">{t.actions}</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[var(--border)]">
                  {traces.map(trace => (
                    <tr key={trace.id} className="align-top hover:bg-[var(--surface-secondary)]/40">
                      <td className="px-4 py-3 font-bold text-[var(--text-primary)]">{trace.sectionTitle || '—'}</td>
                      <td className="px-4 py-3 text-[var(--text-secondary)] font-mono">#{trace.findingIndex}</td>
                      <td className="px-4 py-3 max-w-[240px]">
                        <p className="line-clamp-3 italic leading-relaxed text-[var(--text-secondary)]">“{trace.excerpt || ''}”</p>
                        {trace.suggestedAction && <p className="mt-1 text-[9px] font-bold text-indigo-600">{t.suggested}: {trace.suggestedAction.replaceAll('_', ' ')}</p>}
                      </td>
                      <td className="px-4 py-3">
                        {trace.studentAction
                          ? <span className="rounded bg-indigo-50 px-1.5 py-0.5 text-[10px] font-bold text-indigo-700 border border-indigo-200">{trace.studentAction.replaceAll('_', ' ')}</span>
                          : <span className="text-[var(--text-tertiary)] italic">—</span>}
                      </td>
                      <td className="px-4 py-3 max-w-[200px]">
                        {trace.evidenceQuote
                          ? <p className="line-clamp-2 italic leading-relaxed text-[var(--text-secondary)]">“{trace.evidenceQuote}”</p>
                          : <span className="text-[var(--text-tertiary)] italic">—</span>}
                      </td>
                      <td className="px-4 py-3 max-w-[200px]">
                        {trace.afterPassage
                          ? <p className="line-clamp-2 leading-relaxed text-[var(--text-secondary)]">{trace.afterPassage}</p>
                          : <span className="text-[var(--text-tertiary)] italic">—</span>}
                      </td>
                      <td className="px-4 py-3">
                        <span className={`rounded px-1.5 py-0.5 text-[9px] font-bold ${OUTCOME_CLASSES[trace.outcome] || OUTCOME_CLASSES.UNRESOLVED}`}>
                          {(trace.outcome || '—').replaceAll('_', ' ')}
                        </span>
                        {trace.judgment && <p className="mt-1 text-[9px] font-bold text-[var(--text-secondary)]">{t.judgmentLabel}: {trace.judgment}</p>}
                      </td>
                      <td className="px-4 py-3">
                        <button onClick={() => setReviewing(trace)} disabled={savingId !== null}
                          className="rounded-lg bg-[var(--brand)] px-2.5 py-1.5 text-[10px] font-bold text-white hover:bg-[var(--brand-hover)] disabled:opacity-40">
                          {t.reviewTrace}
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </main>

      {reviewing && (
        <Modal open={Boolean(reviewing)} title={t.reviewTrace} onClose={() => setReviewing(null)}>
          <div className="space-y-4">
            <div>
              <p className="text-[11px] font-bold text-[var(--text-tertiary)] uppercase tracking-wider">{t.section}</p>
              <p className="mt-1 text-sm font-bold text-[var(--text-primary)]">{reviewing.sectionTitle || '—'}</p>
              <blockquote className="mt-2 rounded-lg border-l-2 border-amber-400 bg-[var(--surface-secondary)] p-3 text-[11px] italic leading-relaxed text-[var(--text-secondary)]">“{reviewing.excerpt || ''}”</blockquote>
              {reviewing.explanation && <p className="mt-2 text-[11px] leading-relaxed text-[var(--text-secondary)]">{t.studentExplanation}: {reviewing.explanation}</p>}
              {reviewing.evidenceQuote && <p className="mt-1 text-[10px] italic leading-relaxed text-[var(--text-secondary)]">“{reviewing.evidenceQuote}”</p>}
            </div>
            <ReviewForm saving={savingId === reviewing.id} onSave={async (judgment, feedback) => handleReview(reviewing.id, judgment, feedback)} onCancel={() => setReviewing(null)} />
          </div>
        </Modal>
      )}
    </div>
  );
}

function ReviewForm({ saving, onSave, onCancel }) {
  const { language } = useLanguage();
  const t = instructorText[language];
  const ct = commonText[language];
  const [judgment, setJudgment] = useState('');
  const [feedback, setFeedback] = useState('');
  return (
    <div className="space-y-3">
      <label className="block">
        <span className="mb-1 block text-[11px] font-bold text-[var(--text-tertiary)] uppercase tracking-wider">{t.judgmentLabel}</span>
        <select value={judgment} onChange={e => setJudgment(e.target.value)}
          className="w-full rounded-lg border border-[var(--border)] bg-[var(--surface)] px-2.5 py-2 text-xs text-[var(--text-primary)] outline-none">
          <option value="">{t.selectJudgment}</option>
          {JUDGMENTS.map(j => <option key={j} value={j}>{j}</option>)}
        </select>
      </label>
      <label className="block">
        <span className="mb-1 block text-[11px] font-bold text-[var(--text-tertiary)] uppercase tracking-wider">{t.instructorFeedback}</span>
        <textarea value={feedback} onChange={e => setFeedback(e.target.value)} rows={3}
          placeholder={t.instructorFeedbackPlaceholder}
          className="w-full rounded-lg border border-[var(--border)] bg-[var(--surface)] px-2.5 py-2 text-xs text-[var(--text-secondary)] outline-none resize-y" />
      </label>
      <div className="flex justify-end gap-2">
        <button onClick={onCancel} className="rounded-lg border border-[var(--border)] px-3 py-2 text-xs font-bold text-[var(--text-secondary)] hover:bg-[var(--surface-secondary)]">
          {ct.cancel}
        </button>
        <button onClick={() => judgment && onSave(judgment, feedback)} disabled={!judgment || saving}
          className="rounded-lg bg-[var(--brand)] px-3 py-2 text-xs font-bold text-white hover:bg-[var(--brand-hover)] disabled:opacity-40">
          {saving ? ct.saving : t.saveJudgment}
        </button>
      </div>
    </div>
  );
}
