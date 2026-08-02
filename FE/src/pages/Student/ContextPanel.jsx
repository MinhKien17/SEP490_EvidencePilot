import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import api from '../../api.js';
import FunctionalTypeRadar from '../../components/FunctionalTypeRadar.jsx';

const CLAIM_STATUS_CLASSES = {
  PRESENT: 'border-emerald-200 bg-emerald-50 text-emerald-700',
  MISSING: 'border-amber-200 bg-amber-50 text-amber-700',
  ORPHANED: 'border-rose-200 bg-rose-50 text-rose-700',
};

const FUNCTIONAL_TYPES = [
  { value: 'EMPIRICAL', label: 'EMPIRICAL — Data, experiments, results' },
  { value: 'THEORETICAL', label: 'THEORETICAL — Literature, background, concepts' },
  { value: 'METHODOLOGICAL', label: 'METHODOLOGICAL — Tools, frameworks, implementation' },
  { value: 'ANALYTICAL', label: 'ANALYTICAL — Evaluations, comparisons, interpretations' },
  { value: 'APPLIED', label: 'APPLIED — Real-world applications, user feedback, impact' },
];

const BREAKDOWN_LABELS = [
  ['semantic_alignment', 'Semantic alignment'],
  ['contextual_sufficiency', 'Contextual sufficiency'],
  ['logical_restraint', 'Logical restraint'],
];

function parseScoreBreakdown(s) {
  if (!s) return null;
  try { return JSON.parse(s); } catch { return null; }
}

function FunctionalTypeDropdown({ value, onChange, className }) {
  const [open, setOpen] = useState(false);
  const ref = useRef(null);
  useEffect(() => {
    if (!open) return;
    const close = (e) => { if (ref.current && !ref.current.contains(e.target)) setOpen(false); };
    document.addEventListener('mousedown', close);
    return () => document.removeEventListener('mousedown', close);
  }, [open]);
  const selected = FUNCTIONAL_TYPES.find(t => t.value === value) || FUNCTIONAL_TYPES[0];
  return (
    <div ref={ref} className={`relative ${className || ''}`}>
      <button type="button" onClick={() => setOpen(o => !o)}
        className="w-full text-[10px] border border-(--border) rounded-lg px-2 py-1.5 bg-(--surface) outline-none focus:ring-1 focus:ring-indigo-500 text-(--text-primary) flex items-center justify-between gap-1">
        <span className="truncate">{selected.value}</span>
        <svg className={`w-3 h-3 shrink-0 transition-transform ${open ? 'rotate-180' : ''}`} fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 9l-7 7-7-7" /></svg>
      </button>
      {open && (
        <ul className="absolute z-20 left-0 right-0 mt-1 bg-(--surface) border border-(--border) rounded-lg shadow-lg max-h-48 overflow-y-auto [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
          {FUNCTIONAL_TYPES.map(t => (
            <li key={t.value}>
              <button type="button" onClick={() => { onChange(t.value); setOpen(false); }}
                className={`w-full text-left text-[10px] px-2 py-1.5 hover:bg-(--surface-secondary) ${t.value === selected.value ? 'font-bold text-indigo-600' : 'text-(--text-primary)'}`}>
                {t.label}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

function EvidenceEvaluationCard({ match, status, breakdownOpenId, setBreakdownOpenId, children }) {
  const breakdown = parseScoreBreakdown(match.scoreBreakdown);
  const open = breakdownOpenId === match.id;
  const statusClass = status === 'ACTIVE'
    ? 'bg-emerald-100 text-emerald-700'
    : status === 'REJECTED'
      ? 'bg-rose-100 text-rose-700'
      : status === 'INACTIVE'
        ? 'bg-slate-100 text-slate-600'
        : 'bg-amber-100 text-amber-700';
  return (
    <div className="bg-(--surface-secondary) border border-(--border) rounded p-2 text-[11px]">
      <div className="flex justify-between items-center gap-2 mb-1">
        <span className="truncate font-bold text-(--text-primary)">{match.sourceFilename}</span>
        <span className={`text-[9px] font-bold px-1.5 py-0.5 rounded ${statusClass}`}>{status}</span>
      </div>
      <div className="flex gap-2 text-[9px] font-bold mb-1">
        <span className="text-indigo-600">{match.relation || 'UNKNOWN'}</span>
        {match.strengthScore != null && <span className="text-(--text-secondary)">Evidence Strength: {match.strengthScore}/100 · {match.strengthBand}</span>}
      </div>
      <p className="text-[10px] text-(--text-secondary) line-clamp-3 italic leading-relaxed">"{match.excerpt}"</p>
      {match.explanation && <p className="text-[10px] text-indigo-600 mt-1 leading-relaxed">{match.explanation}</p>}
      {breakdown && (
        <div className="mt-1.5">
          <button onClick={() => setBreakdownOpenId(open ? null : match.id)} className="text-[9px] font-bold text-(--text-secondary) hover:text-indigo-600 flex items-center gap-1">
            <svg className={`w-2.5 h-2.5 transition-transform ${open ? 'rotate-90' : ''}`} fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 5l7 7-7 7" /></svg>
            Evidence Strength breakdown
          </button>
          {open && (
            <div className="mt-1.5 space-y-1">
              {BREAKDOWN_LABELS.map(([key, label]) => {
                const item = breakdown[key];
                if (!item || item.max == null) return null;
                const pct = item.max > 0 ? Math.round((item.earned / item.max) * 100) : 0;
                return (
                  <div key={key} className="flex items-center gap-2">
                    <span className="w-28 text-[9px] text-(--text-secondary) shrink-0">{label}</span>
                    <div className="flex-1 h-1 bg-(--border) rounded-full overflow-hidden">
                      <div className="h-full bg-indigo-500 rounded-full" style={{ width: `${pct}%` }} />
                    </div>
                    <span className="text-[9px] font-bold text-(--text-primary) shrink-0 w-12 text-right">{item.earned}/{item.max}</span>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}
      {children}
    </div>
  );
}

export default function ContextPanel({
  isOpen, width, onResizeStart,
  activeTab, setActiveTab,
  showToast,
  // Source tab
  sources, isUploading, setIsUploading, project, setViewerFile, fetchSources,
  // Claims tab
  newClaimContent, onNewClaimContentChange, newClaimFunctionalType, setNewClaimFunctionalType,
  claimEvaluation, evaluatingClaim, claimEvaluationError, handleEvaluateClaim, canAddEvaluatedClaim,
  handleCreateClaim, canCreateClaim, creatingClaim,
  claims, selectedClaim, claimMatches, claimMappings, loadingMatches,
  claimCandidates, loadingCandidates, candidateError, evaluatingChunkId, updatingSuggestionId,
  handleSearchClaimMatches, handleEvaluateMatch, handleSuggestionStatus, canEditClaim,
  editingClaim, setEditingClaim, editClaimContent, setEditClaimContent, editClaimFunctionalType, setEditClaimFunctionalType, handleDeleteClaim, handleUpdateClaim,
  onSelectClaim,
  // Feedback tab
  feedbacks, setShowSubmitReviewModal, userProjectRole,
  // Coverage tab
  graphData, graphScope, onGraphScopeToggle, claimStats,
  isLocked,
}) {
  const [showSourceModal, setShowSourceModal] = useState(false);
  const [sourceMode, setSourceMode] = useState('doi');
  const [doiInput, setDoiInput] = useState('');
  const [doiPreview, setDoiPreview] = useState(null);
  const [sourceBusy, setSourceBusy] = useState(false);
  const fileInputRef = useRef(null);
  const { t } = useTranslation();
  const [breakdownOpenId, setBreakdownOpenId] = useState(null);
  const [expandedFeedbackId, setExpandedFeedbackId] = useState(null);
  const [feedbackDetail, setFeedbackDetail] = useState({});

  const toggleFeedbackDetail = async (fb) => {
    const id = fb.id || fb.requestId;
    if (!id) return;
    if (expandedFeedbackId === id) { setExpandedFeedbackId(null); return; }
    setExpandedFeedbackId(id);
    if (!feedbackDetail[id]) {
      try {
        const r = await api.get(`/api/feedback-requests/${id}/feedback`);
        setFeedbackDetail(prev => ({ ...prev, [id]: r.data || [] }));
      } catch { setFeedbackDetail(prev => ({ ...prev, [id]: [] })); }
    }
  };

  if (!isOpen) return null;

  const activeClass = (tab) =>
    `flex-1 py-3 text-[10px] font-bold uppercase tracking-wider flex flex-col justify-center items-center gap-1 transition-all relative ${activeTab === tab ? 'text-indigo-600' : 'text-(--text-secondary) hover:text-(--text-primary) hover:bg-(--surface-secondary)'}`;

  return (
    <>
      <div onMouseDown={onResizeStart} className="w-1 hover:w-1.5 bg-(--border) hover:bg-(--text-tertiary) cursor-col-resize self-stretch transition-all shrink-0 z-10 relative group flex items-center justify-center border-l border-(--border)/80">
        <div className="h-6 w-0.5 bg-(--text-tertiary) group-hover:bg-(--text-secondary) rounded"></div>
      </div>
      <aside data-tour="context-panel" style={{ width }} className="bg-(--surface) border-l border-(--border) flex flex-col shrink-0 shadow-[-4px_0_15px_-3px_rgba(0,0,0,0.05)] z-10 overflow-hidden">
        <div className="flex border-b border-(--border) bg-(--surface) relative shrink-0">
          <button data-tour="context-info-tab" onClick={() => setActiveTab('Source')} className={activeClass('Source')}>
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
            {t('sources')}
            {activeTab === 'Source' && <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-indigo-600 shadow-[0_-2px_8px_rgba(79,70,229,0.5)]"></div>}
          </button>
          <button data-tour="context-claims-tab" onClick={() => setActiveTab('Claims')} className={activeClass('Claims')}>
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01" /></svg>
            {t('claims')}
            {activeTab === 'Claims' && <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-indigo-600 shadow-[0_-2px_8px_rgba(79,70,229,0.5)]"></div>}
          </button>
          <button data-tour="context-feedback-tab" onClick={() => setActiveTab('Feedback')} className={activeClass('Feedback')}>
            <div className="relative">
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z" /></svg>
              {feedbacks.length > 0 && <span className="absolute -top-1.5 -right-2 bg-rose-500 text-white flex items-center justify-center text-[9px] w-4 h-4 rounded-full font-bold animate-pulse">{feedbacks.length}</span>}
            </div>
            {t('feedback')}
            {activeTab === 'Feedback' && <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-indigo-600 shadow-[0_-2px_8px_rgba(79,70,229,0.5)]"></div>}
          </button>
          <button data-tour="context-graph-tab" onClick={() => setActiveTab('Graph')} className={activeClass('Graph')}>
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M7 16V4m0 0L3 8m4-4l4 4m6 0v12m0 0l4-4m-4 4l-4-4" /></svg>
            Coverage
            {activeTab === 'Graph' && <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-indigo-600 shadow-[0_-2px_8px_rgba(79,70,229,0.5)]"></div>}
          </button>
        </div>

        <div className="flex-1 overflow-y-auto bg-(--surface-secondary)/50 p-4">
          {activeTab === 'Source' && (
            <div className="p-5 flex flex-col gap-6 animate-in fade-in duration-300">
              <button onClick={() => setShowSourceModal(true)} disabled={isLocked} className="w-full flex items-center justify-center gap-2 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-40 text-white font-bold text-sm py-3 px-4 rounded-xl shadow-md hover:shadow-lg transition-all">
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4v16m8-8H4" /></svg>
                Insert Source
              </button>

              {showSourceModal && (
                <div className="bg-(--surface) border border-(--border) rounded-xl p-4 shadow-lg space-y-3 animate-in fade-in slide-in-from-top-2 duration-150">
                  <div className="flex justify-between items-center">
                    <span className="text-xs font-bold text-(--text-primary)">Add Source</span>
                    <button onClick={() => { setShowSourceModal(false); setDoiPreview(null); }} className="text-(--text-tertiary) hover:text-(--text-primary) cursor-pointer"><svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" /></svg></button>
                  </div>

                  <div className="flex gap-2">
                    {['doi', 'file', 'both'].map(m => (
                      <button key={m} onClick={() => { setSourceMode(m); setDoiPreview(null); }}
                        className={`flex-1 text-[10px] font-bold px-2 py-1.5 rounded-lg border transition-all cursor-pointer ${sourceMode === m ? 'bg-indigo-600 text-white border-indigo-600' : 'bg-(--surface-secondary) text-(--text-secondary) border-(--border) hover:border-indigo-300'}`}>
                        {m === 'doi' ? 'From DOI' : m === 'file' ? 'From File' : 'DOI + File'}
                      </button>
                    ))}
                  </div>

                  <div className="space-y-2">
                    {sourceMode !== 'file' && (
                      <div>
                        <label className="text-[10px] font-bold text-(--text-secondary) block mb-1">DOI</label>
                        <div className="flex gap-2">
                          <input value={doiInput} onChange={e => setDoiInput(e.target.value)} placeholder="10.1000/xyz123" className="flex-1 text-xs border border-(--border) rounded-lg px-2 py-1.5 bg-(--surface) outline-none focus:ring-1 focus:ring-indigo-500 text-(--text-primary)" />
                          {sourceMode === 'both' && (
                            <button onClick={async () => {
                              if (!doiInput.trim() || sourceBusy) return;
                              setSourceBusy(true); setDoiPreview(null);
                              try { const r = await api.post('/api/documents/lookup', { doi: doiInput.trim() }); setDoiPreview(r.data); } catch { showToast('DOI lookup failed.'); }
                              finally { setSourceBusy(false); }
                            }} disabled={sourceBusy} className="text-[10px] font-bold text-white bg-emerald-600 hover:bg-emerald-700 disabled:opacity-50 px-2 py-1 rounded-lg cursor-pointer">
                              {sourceBusy ? '...' : 'Lookup'}
                            </button>
                          )}
                        </div>
                        {doiPreview && (
                          <div className="mt-2 bg-(--surface-secondary) border border-(--border) rounded-lg p-2 text-[10px] text-(--text-primary) space-y-0.5">
                            <p className="font-bold">{doiPreview.title}</p>
                            <p className="text-(--text-secondary)">{doiPreview.authors?.join(', ')} ({doiPreview.publicationYear})</p>
                            {doiPreview.hasPdf && <p className="text-emerald-600 text-[9px]">Open Access PDF available</p>}
                          </div>
                        )}
                      </div>
                    )}
                    {sourceMode !== 'doi' && (
                      <div>
                        <label className="text-[10px] font-bold text-(--text-secondary) block mb-1">File (PDF/DOCX)</label>
                        <input ref={fileInputRef} type="file" accept=".pdf,.docx" className="block text-xs text-(--text-primary) file:mr-2 file:py-1 file:px-2 file:rounded-lg file:border-0 file:text-xs file:font-bold file:bg-indigo-50 dark:file:bg-indigo-900/30 file:text-indigo-700 hover:file:bg-indigo-100 dark:hover:file:bg-indigo-900/50 cursor-pointer file:cursor-pointer" />
                      </div>
                    )}
                  </div>

                  <button onClick={async () => {
                    if (sourceBusy || !project) return;
                    setSourceBusy(true);
                    try {
                      if (sourceMode === 'doi') {
                        await api.post('/api/documents/ingest/doi', {
                          doi: doiInput.trim(),
                          projectId: project.id,
                        });
                        showToast('Source queued from DOI.');
                      } else {
                        const file = fileInputRef.current?.files?.[0];
                        if (!file) { showToast('Please select a file.'); setSourceBusy(false); return; }
                        const fd = new FormData(); fd.append('file', file); fd.append('projectId', project.id);
                        await api.post('/api/sources', fd);
                        showToast('Source uploaded.');
                      }
                      setShowSourceModal(false); setDoiPreview(null); setDoiInput('');
                      if (fetchSources) fetchSources();
                    } catch { showToast('Failed to add source.'); }
                    finally { setSourceBusy(false); }
                  }} disabled={sourceBusy || (sourceMode !== 'file' && !doiInput.trim()) || (sourceMode !== 'doi' && !fileInputRef.current?.files?.[0])} className="w-full text-xs font-bold text-white bg-indigo-600 hover:bg-indigo-700 disabled:opacity-40 py-2 rounded-lg transition-all cursor-pointer">
                    {sourceBusy ? 'Working...' : 'Insert Source'}
                  </button>
                </div>
              )}

              <div>
                <h3 className="text-[11px] font-bold text-(--text-tertiary) tracking-widest mb-3 uppercase flex items-center gap-2"><div className="h-px bg-(--border) flex-1"></div> {t('availableSource')} <div className="h-px bg-(--border) flex-1"></div></h3>
                <div className="flex flex-col gap-3">
                  {sources.length === 0 ? <div className="text-sm text-(--text-secondary) italic text-center p-4">No uploaded sources yet.</div> : (
                    sources.map(src => (
                      <div key={src.id} onClick={() => src.fileUrl ? setViewerFile({ fileUrl: src.fileUrl, fileName: src.originalFilename }) : showToast('File URL not available')} className="bg-(--surface) border border-(--border) rounded-xl p-3.5 hover:shadow-md hover:border-indigo-300 dark:hover:border-indigo-700 transition-all cursor-pointer transform hover:-translate-y-0.5">
                        <p className="text-sm font-bold text-(--text-primary) flex items-center gap-2"><svg className="w-4 h-4 text-red-500" fill="currentColor" viewBox="0 0 20 20"><path fillRule="evenodd" d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4z" clipRule="evenodd" /></svg>{src.originalFilename}</p>
                        <p className="text-xs text-(--text-secondary) mt-1.5 line-clamp-2 leading-relaxed">Source file uploaded to this project.</p>
                      </div>
                    ))
                  )}
                </div>
              </div>
            </div>
          )}

          {activeTab === 'Claims' && (
            <div className="space-y-3">
              {canCreateClaim && (
                <div className="bg-(--surface) border border-(--border) rounded-xl p-3.5 shadow-sm">
                  <h4 className="text-[11px] font-bold text-(--text-secondary) mb-2 uppercase tracking-wider">Add Claim</h4>
                  <div className="flex flex-col gap-2">
                    <input value={newClaimContent} onChange={(e) => onNewClaimContentChange(e.target.value)} placeholder="Claim content..." className="text-xs border border-(--border) rounded-lg px-2 py-1.5 bg-(--surface) outline-none focus:ring-1 focus:ring-indigo-500 min-w-[120px] text-(--text-primary)" />
                    <div className="flex gap-2">
                      <button onClick={handleCreateClaim} disabled={!newClaimContent.trim() || creatingClaim} className="flex-1 text-xs font-bold text-white bg-emerald-600 hover:bg-emerald-700 disabled:bg-(--border) disabled:dark:bg-(--border) px-3 py-1.5 rounded-lg transition-colors">
                        {creatingClaim ? 'Adding...' : 'Add Claim'}
                      </button>
                      <button onClick={handleEvaluateClaim} disabled={!newClaimContent.trim() || evaluatingClaim} className="flex-1 text-xs font-bold text-white bg-indigo-600 hover:bg-indigo-700 disabled:bg-(--border) disabled:dark:bg-(--border) px-3 py-1.5 rounded-lg transition-colors">
                        {evaluatingClaim ? 'Evaluating...' : claimEvaluationError ? 'Retry AI Evaluate' : 'AI Evaluate'}
                      </button>
                    </div>
                    {claimEvaluationError && <p className="rounded-lg border border-rose-200 bg-rose-50 p-2 text-[10px] font-medium text-rose-700">{claimEvaluationError}</p>}
                    {claimEvaluation && (
                      <div className="space-y-2 rounded-lg border border-indigo-200 bg-indigo-50/60 p-2.5">
                        <div className="flex items-center justify-between gap-2">
                          <span className="text-[10px] font-bold uppercase tracking-wider text-indigo-700">Claim Quality · {claimEvaluation.totalScore}/10</span>
                          <span className={`rounded px-1.5 py-0.5 text-[9px] font-black ${claimEvaluation.decision === 'READY' ? 'bg-emerald-100 text-emerald-700' : 'bg-amber-100 text-amber-700'}`}>{claimEvaluation.decision}</span>
                        </div>
                        <div className="space-y-1">
                          {(claimEvaluation.criteria || []).map(criterion => (
                            <div key={criterion.code} className="rounded border border-indigo-100 bg-white/80 p-1.5">
                              <div className="flex justify-between text-[9px] font-bold text-(--text-primary)">
                                <span>{criterion.code.replaceAll('_', ' ')}</span>
                                <span>{criterion.score}/2</span>
                              </div>
                              <p className="mt-0.5 text-[9px] leading-relaxed text-(--text-secondary)">{criterion.reason}</p>
                            </div>
                          ))}
                        </div>
                        {claimEvaluation.suggestedRevision && claimEvaluation.suggestedRevision.trim() !== newClaimContent.trim() && (
                          <div className="rounded border border-amber-200 bg-amber-50 p-2 text-[10px] text-amber-800">
                            <p>{claimEvaluation.suggestedRevision}</p>
                            <button onClick={() => onNewClaimContentChange(claimEvaluation.suggestedRevision)} className="mt-1 font-bold text-amber-900 underline">Use revision and evaluate again</button>
                          </div>
                        )}
                        <div>
                          <label className="mb-1 block text-[9px] font-bold uppercase tracking-wider text-(--text-secondary)">Functional type</label>
                          <FunctionalTypeDropdown value={newClaimFunctionalType} onChange={setNewClaimFunctionalType} className="w-full" />
                        </div>
                        <p className="text-[9px] text-(--text-tertiary)">AI advice only. You may add either READY or REVISE after reviewing the result.</p>
                        <button onClick={handleCreateClaim} disabled={!canAddEvaluatedClaim || creatingClaim} className="w-full text-xs font-bold text-white bg-emerald-600 hover:bg-emerald-700 disabled:bg-(--border) px-3 py-1.5 rounded-lg transition-colors">{creatingClaim ? 'Adding...' : 'Add Claim'}</button>
                      </div>
                    )}
                  </div>
                </div>
              )}
              {claims.length === 0 ? <div className="text-xs text-(--text-tertiary) italic text-center py-8">No claims yet.</div> : (
                claims.map((claim, claimIndex) => {
                  const isSelected = selectedClaim?.id === claim.id;
                  const activeSuggestionIds = new Set(
                    (isSelected ? claimMappings : [])
                      .filter(mapping => mapping.status === 'ACTIVE')
                      .map(mapping => String(mapping.suggestionId)),
                  );
                  const activeEvidence = isSelected
                    ? claimMatches.filter(match => activeSuggestionIds.has(String(match.id)))
                    : [];
                  const aiSuggestions = isSelected
                    ? claimMatches.filter(match =>
                      !activeSuggestionIds.has(String(match.id))
                      && (match.status === 'PENDING' || match.status === 'REJECTED'))
                    : [];
                  return (
                    <div key={claim.id} onClick={() => { if (onSelectClaim) onSelectClaim(claim); }} className={`bg-(--surface) border rounded-xl p-3.5 shadow-sm hover:shadow-md transition-all relative overflow-hidden group cursor-pointer ${isSelected ? 'border-indigo-400 ring-1 ring-indigo-400/20' : 'border-(--border)'}`}>
                      <div className="absolute left-0 top-0 bottom-0 w-1.5 bg-indigo-500"></div>
                      <div className="flex justify-between items-center mb-1.5 pl-1">
                        <span className="text-[9px] font-black text-indigo-700 bg-indigo-50 dark:bg-indigo-900/30 px-1.5 py-0.5 rounded border border-indigo-100 dark:border-indigo-800 uppercase tracking-wide">#{claimIndex + 1}{claim.claimVersion > 1 ? ` v${claim.claimVersion}` : ''}</span>
                        <div className="flex items-center gap-1.5">
                          {claim.contentStatus && (
                            <span className={`text-[9px] font-bold px-1.5 py-0.5 rounded border ${CLAIM_STATUS_CLASSES[claim.contentStatus] || 'border-slate-200 bg-slate-50 text-slate-600'}`}>
                              {claim.contentStatus}
                            </span>
                          )}
                          {isSelected && activeEvidence.length > 0 && (
                            <span className="text-[9px] font-bold text-indigo-600 bg-indigo-50 dark:bg-indigo-900/30 px-1.5 py-0.5 rounded border border-indigo-200 dark:border-indigo-800">
                              {activeEvidence.length} evidence
                            </span>
                          )}
                          {claim.aiConfidenceScore !== null ? (
                            <span className={`text-[10px] font-bold px-1.5 py-0.5 rounded ${claim.aiConfidenceScore >= 0.7 ? 'bg-emerald-50 dark:bg-emerald-900/30 text-emerald-700 border border-emerald-100 dark:border-emerald-800' : claim.aiConfidenceScore >= 0.4 ? 'bg-amber-50 dark:bg-amber-900/30 text-amber-700 border-amber-100 dark:border-amber-800' : 'bg-rose-50 dark:bg-rose-900/30 text-rose-700 border border-rose-100 dark:border-rose-800'}`}>
                              {(claim.aiConfidenceScore * 100).toFixed(0)}%
                            </span>
                          ) : <span className="text-[10px] text-(--text-tertiary) italic">—</span>}
                        </div>
                      </div>
                      <p className="text-xs font-semibold text-(--text-primary) pl-1 leading-relaxed">{claim.content}</p>
                      <div className="flex gap-2 mt-3 pt-2.5 border-t border-(--border-light) pl-1">
                        {canEditClaim(claim) && <>
                          <button onClick={(e) => { e.stopPropagation(); handleSearchClaimMatches(claim); }} disabled={isLocked || loadingCandidates} className="text-[10px] font-bold text-indigo-600 hover:text-indigo-800 disabled:opacity-40 flex items-center gap-1">
                            <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 10V3L4 14h7v7l9-11h-7z" /></svg>
                            Find matches
                          </button>
                          <button onClick={(e) => { e.stopPropagation(); setEditingClaim(claim); setEditClaimContent(claim.content); setEditClaimFunctionalType(claim.functionalType || 'EMPIRICAL'); }} className="text-[10px] text-(--text-secondary) hover:text-(--text-primary) flex items-center gap-0.5 ml-auto">Edit</button>
                          <button onClick={(e) => { e.stopPropagation(); handleDeleteClaim(claim.id); }} className="text-[10px] text-rose-500 hover:text-rose-700 flex items-center gap-0.5">Delete</button>
                        </>}
                      </div>
                      {editingClaim && editingClaim.id === claim.id && (
                        <div className="mt-3 pt-3 border-t border-dashed border-(--border)">
                          <input value={editClaimContent} onChange={(e) => setEditClaimContent(e.target.value)} className="w-full text-xs border border-(--border) rounded-lg px-2 py-1.5 bg-(--surface) outline-none focus:ring-1 focus:ring-indigo-500 mb-2 text-(--text-primary)" />
                          <FunctionalTypeDropdown value={editClaimFunctionalType} onChange={setEditClaimFunctionalType} className="w-full mb-2" />
                          <div className="flex gap-2 justify-end">
                            <button onClick={() => setEditingClaim(null)} className="text-[10px] text-(--text-secondary) hover:text-(--text-primary) font-bold">Cancel</button>
                            <button onClick={handleUpdateClaim} className="text-[10px] font-bold text-white bg-indigo-600 hover:bg-indigo-700 px-2 py-1 rounded-lg">Save</button>
                          </div>
                        </div>
                      )}
                      {isSelected && (
                        <div className="mt-3 pt-3 border-t border-dashed border-(--border) animate-in fade-in slide-in-from-top-1 duration-200 space-y-4">
                          <div>
                            <h4 className="text-[10px] font-bold text-(--text-tertiary) uppercase tracking-widest mb-2">Qdrant matches</h4>
                            {loadingCandidates ? <div className="text-center py-2 text-[10px] text-(--text-tertiary) italic">Searching sources...</div> : candidateError ? (
                              <div className="text-center py-2 space-y-2">
                                <div className="text-[10px] text-rose-500 italic">{candidateError}</div>
                                <button onClick={(e) => { e.stopPropagation(); handleSearchClaimMatches(selectedClaim); }} className="text-[9px] font-bold text-indigo-600 hover:text-indigo-800">Retry</button>
                              </div>
                            ) : claimCandidates.length === 0 ? (
                              <div className="text-center py-2 text-[10px] text-(--text-tertiary) italic">Click Find matches to search active sources.</div>
                            ) : (
                              <div className="space-y-2 max-h-64 overflow-y-auto pr-1">
                                {claimCandidates.map(candidate => {
                                  const evaluated = claimMatches.some(match => match.documentChunkId === candidate.documentChunkId);
                                  return (
                                    <div key={candidate.documentChunkId} className="bg-(--surface-secondary) border border-(--border) rounded p-2 text-[11px]">
                                      <div className="flex justify-between items-center mb-1 text-[9px] font-medium text-(--text-secondary)">
                                        <span className="truncate max-w-[150px] font-bold text-(--text-primary)">{candidate.sourceFilename}</span>
                                        <span className="text-indigo-600 font-bold bg-indigo-50 dark:bg-indigo-900/30 px-1 rounded">{(candidate.similarityScore * 100).toFixed(0)}% match</span>
                                      </div>
                                      <p className="text-[10px] text-(--text-secondary) line-clamp-4 italic leading-relaxed">"{candidate.excerpt}"</p>
                                      <div className="flex justify-between items-center mt-2">
                                        <span className="text-[9px] text-(--text-tertiary)">Chunk {candidate.chunkIndex}</span>
                                        {canEditClaim(claim) && (
                                          <button
                                            onClick={(event) => { event.stopPropagation(); handleEvaluateMatch(claim.id, candidate.documentChunkId); }}
                                            disabled={isLocked || evaluated || evaluatingChunkId === candidate.documentChunkId}
                                            className="text-[9px] font-bold text-white bg-indigo-600 hover:bg-indigo-700 disabled:bg-(--border) px-2 py-1 rounded transition-colors">
                                            {evaluated ? 'Evaluated' : evaluatingChunkId === candidate.documentChunkId ? 'Evaluating...' : 'Select & evaluate'}
                                          </button>
                                        )}
                                      </div>
                                    </div>
                                  );
                                })}
                              </div>
                            )}
                          </div>

                          <div>
                            <h4 className="text-[10px] font-bold text-(--text-tertiary) uppercase tracking-widest mb-2">Evidence</h4>
                            {loadingMatches ? <div className="text-center py-2 text-[10px] text-(--text-tertiary) italic">Loading evidence...</div> : activeEvidence.length === 0 ? (
                              <div className="text-center py-2 text-[10px] text-(--text-tertiary) italic">No active evidence mapping yet.</div>
                            ) : (
                              <div className="space-y-2">
                                {activeEvidence.map(match => (
                                  <EvidenceEvaluationCard key={match.id} match={match} status="ACTIVE" breakdownOpenId={breakdownOpenId} setBreakdownOpenId={setBreakdownOpenId} />
                                ))}
                              </div>
                            )}
                          </div>

                          <div>
                            <h4 className="text-[10px] font-bold text-(--text-tertiary) uppercase tracking-widest mb-2">AI suggestions</h4>
                            {!loadingMatches && aiSuggestions.length === 0 ? (
                              <div className="text-center py-2 text-[10px] text-(--text-tertiary) italic">No pending or rejected suggestions.</div>
                            ) : (
                              <div className="space-y-2">
                                {aiSuggestions.map(match => (
                                  <EvidenceEvaluationCard
                                    key={match.id}
                                    match={match}
                                    status={match.status}
                                    breakdownOpenId={breakdownOpenId}
                                    setBreakdownOpenId={setBreakdownOpenId}>
                                    {match.status === 'PENDING' && canEditClaim(claim) && (
                                      <div className="flex justify-end gap-2 mt-2">
                                        <button onClick={(event) => { event.stopPropagation(); handleSuggestionStatus(match.id, 'REJECTED'); }} disabled={isLocked || updatingSuggestionId === match.id} className="text-[9px] font-bold text-rose-600 hover:text-rose-700 disabled:opacity-40">Reject</button>
                                        <button onClick={(event) => { event.stopPropagation(); handleSuggestionStatus(match.id, 'ACCEPTED'); }} disabled={isLocked || updatingSuggestionId === match.id} className="text-[9px] font-bold text-white bg-emerald-600 hover:bg-emerald-700 disabled:opacity-40 px-2 py-1 rounded">Accept</button>
                                      </div>
                                    )}
                                  </EvidenceEvaluationCard>
                                ))}
                              </div>
                            )}
                          </div>
                        </div>
                      )}
                    </div>
                  );
                })
              )}
            </div>
          )}

          {activeTab === 'Feedback' && (
            <div className="flex flex-col gap-4 animate-in fade-in duration-200">
              <div className="flex justify-between items-center mb-1 bg-(--surface) border border-(--border) rounded-xl p-3.5 shadow-sm">
                <div>
                  <p className="text-[10px] text-(--text-tertiary) uppercase tracking-wider font-bold">Project Status</p>
                  <p className="text-sm font-bold text-(--text-primary) mt-0.5">{project?.status || 'Unknown'}</p>
                </div>
                {userProjectRole === 'LEADER' && (project?.status === 'ASSIGNED' || project?.status === 'IN_PROGRESS' || project?.status === 'RETURNED') && <button onClick={() => setShowSubmitReviewModal(true)} className="bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-bold px-3 py-1.5 rounded-lg shadow-sm transition-all">{t('submitReview')}</button>}
              </div>
              <h3 className="text-[11px] font-bold text-(--text-tertiary) tracking-widest uppercase flex items-center gap-2 mt-2"><div className="h-px bg-(--border) flex-1"></div> Review History <div className="h-px bg-(--border) flex-1"></div></h3>
              <div className="space-y-4">
                {feedbacks.length === 0 ? <div className="text-xs text-(--text-tertiary) italic text-center py-8">No reviews yet.</div> : (
                  feedbacks.map((fb, idx) => (
                    <div key={fb.id || idx} className="bg-(--surface) border border-(--border) rounded-xl shadow-sm overflow-hidden">
                      <div className="bg-(--surface-secondary) border-b border-(--border-light) p-3 flex justify-between items-start cursor-pointer" onClick={() => toggleFeedbackDetail(fb)}>
                        <div className="flex items-center gap-2">
                          <div className="w-7 h-7 rounded-full bg-indigo-100 dark:bg-indigo-900/30 text-indigo-700 flex items-center justify-center font-bold text-xs border border-indigo-200 dark:border-indigo-800">I</div>
                          <div>
                            <p className="text-xs font-bold text-(--text-primary)">Instructor{fb.instructorName ? `: ${fb.instructorName}` : ''}</p>
                            <p className="text-[9px] text-(--text-tertiary) font-medium">{fb.requestedAt ? new Date(fb.requestedAt).toLocaleString() : ''}</p>
                          </div>
                        </div>
                        <div className="flex items-center gap-2">
                          <span className={`text-[9px] px-2 py-0.5 rounded font-black border uppercase ${fb.status === 'PENDING' ? 'bg-amber-50 dark:bg-amber-900/30 text-amber-700 border-amber-200 dark:border-amber-800' : fb.status === 'RETURNED' ? 'bg-rose-50 dark:bg-rose-900/30 text-rose-700 border-rose-200 dark:border-rose-800' : fb.status === 'REVIEWED' ? 'bg-emerald-50 dark:bg-emerald-900/30 text-emerald-700 border-emerald-200 dark:border-emerald-800' : 'bg-rose-50 dark:bg-rose-900/30 text-rose-700'}`}>{fb.status}</span>
                          <svg className={`w-3 h-3 text-(--text-tertiary) transition-transform ${expandedFeedbackId === (fb.id || fb.requestId) ? 'rotate-180' : ''}`} fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 9l-7 7-7-7" /></svg>
                        </div>
                      </div>
                      <div className="p-3 text-xs leading-relaxed text-(--text-primary)">
                        {fb.status === 'PENDING' && <p className="text-amber-600 font-medium italic">Project submitted. Waiting for instructor review.</p>}
                        {fb.status === 'RETURNED' && <p className="text-rose-600 font-medium">Instructor returned the project. Please review and resubmit.</p>}
                        {fb.status === 'REVIEWED' && <p className="text-emerald-600 font-medium">Instructor approved the project.</p>}
                        {fb.status === 'REJECTED' && <p className="text-red-600 font-medium">Review request was rejected.</p>}
                        {expandedFeedbackId === (fb.id || fb.requestId) && (
                          <div className="mt-3 space-y-2">
                            {(feedbackDetail[fb.id || fb.requestId] || []).length === 0 ? (
                              <p className="text-[10px] text-(--text-tertiary) italic">No per-section feedback items.</p>
                            ) : (
                              feedbackDetail[fb.id || fb.requestId].map(item => (
                                <div key={item.id} className="rounded-lg border border-(--border) bg-(--surface-secondary) p-2.5 space-y-1">
                                  <div className="flex items-center gap-2">
                                    <span className="text-[9px] font-black text-indigo-600 bg-indigo-50 dark:bg-indigo-900/30 px-1.5 py-0.5 rounded">Section {item.sectionTitle || ''}</span>
                                    {item.stale && <span className="text-[9px] font-bold bg-amber-100 text-amber-700 px-1.5 py-0.5 rounded">section changed</span>}
                                    {item.answered && <span className="text-[9px] font-bold bg-emerald-100 text-emerald-700 px-1.5 py-0.5 rounded">answered</span>}
                                  </div>
                                  {item.lineReference && <p className="text-[9px] text-(--text-tertiary) font-mono">{item.lineReference}</p>}
                                  <p className="text-[10px] text-(--text-primary) leading-relaxed">{item.content}</p>
                                  {item.answered && item.answerContent && (
                                    <p className="text-[9px] text-emerald-700 bg-emerald-50 dark:bg-emerald-900/30 rounded p-1.5">My answer: {item.answerContent}</p>
                                  )}
                                </div>
                              ))
                            )}
                          </div>
                        )}
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>
          )}

          {activeTab === 'Graph' && (
            <div className="flex flex-col gap-4 animate-in fade-in duration-200">
              <FunctionalTypeRadar stats={claimStats} compact />
              {graphData && graphData.sectionSummaries && graphData.sectionSummaries.length > 0 && (
                <div className="bg-(--surface) border border-(--border) rounded-xl p-4 shadow-sm">
                  <h4 className="font-bold text-xs text-(--text-primary) mb-3">Section Coverage</h4>
                  <div className="space-y-2 max-h-80 overflow-y-auto pr-1">
                    {graphData.sectionSummaries.map(summary => {
                      const coverage = summary.claimCount > 0 ? Math.round((summary.presentCount / summary.claimCount) * 100) : 0;
                      const status = summary.orphanedCount > 0 || summary.unsupportedCount > 0 ? 'bg-rose-50 text-rose-700 border-rose-200'
                        : summary.missingCount > 0 ? 'bg-amber-50 text-amber-700 border-amber-200' : 'bg-emerald-50 text-emerald-700 border-emerald-200';
                      return (
                        <div key={summary.sectionId} className="p-2 bg-(--surface-secondary) border border-(--border-light) rounded-lg">
                          <div className="flex justify-between items-center mb-1">
                            <span className="text-[11px] font-bold text-(--text-primary) truncate">{summary.sectionTitle || summary.sectionId}</span>
                            <span className={`text-[9px] font-bold px-1.5 py-0.5 rounded border shrink-0 ${status}`}>{coverage}%</span>
                          </div>
                          <div className="h-1.5 bg-(--border)/50 rounded-full overflow-hidden">
                            <div className={`h-full rounded-full ${summary.orphanedCount > 0 || summary.unsupportedCount > 0 ? 'bg-rose-400' : summary.missingCount > 0 ? 'bg-amber-400' : 'bg-emerald-400'}`} style={{ width: `${coverage}%` }} />
                          </div>
                          <p className="mt-1 text-[9px] text-(--text-tertiary)">
                            {summary.presentCount} supported &middot; {summary.missingCount} missing &middot; {summary.orphanedCount} orphaned &middot; {summary.unsupportedCount} unsupported{summary.assignedUserName ? ` &middot; ${summary.assignedUserName}` : ''}
                          </p>
                        </div>
                      );
                    })}
                  </div>
                </div>
              )}
              <label className="flex items-center gap-2 mb-3 px-1 text-[11px] text-(--text-tertiary) cursor-pointer select-none">
                <input type="checkbox" checked={graphScope === 'all'} onChange={onGraphScopeToggle}
                  className="w-3.5 h-3.5 rounded border-(--border) text-indigo-600 focus:ring-indigo-500" />
                <span>Show teammate claims</span>
              </label>
              {(!graphData?.sectionSummaries || graphData.sectionSummaries.length === 0) && (
                <div className="text-xs text-(--text-tertiary) text-center py-8">
                  No section coverage data yet.
                </div>
              )}

            </div>
          )}
        </div>
      </aside>
    </>
  );
}
