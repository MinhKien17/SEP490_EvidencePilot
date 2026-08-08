import { useState, useEffect, useMemo, useCallback } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { StatusBadge, LoadingSkeleton, AppHeader } from '../../components';
import DiffMatchPatch from 'diff-match-patch';
import api from '../../api.js';
import { renderLatexToHtml } from '../../components/latexHtml.js';
import { commonText, instructorText } from '../../locales';
import { useLanguage } from '../../context/LanguageContext';
import { legacyClaimsEnabled } from '../../featureFlags.js';

function wrapLatexLines(latex) {
  if (!latex) return '';
  const lines = latex.split(/\r?\n/);
  let inBlock = false;
  
  const mathTableEnvs = ['equation', 'equation*', 'align', 'align*', 'aligned', 'aligned*', 'tabular', 'table', 'matrix', 'pmatrix', 'bmatrix', 'array'];

  const wrappedLines = lines.map((line, idx) => {
    const lineNum = idx + 1;
    const trimmed = line.trim();
    if (!trimmed) return line;
    
    // Check for math/table block starts/ends
    const beginMatch = trimmed.match(/\\begin\{([^}]+)\}/);
    const endMatch = trimmed.match(/\\end\{([^}]+)\}/);
    
    const isBlockStart = (beginMatch && mathTableEnvs.includes(beginMatch[1])) || trimmed.includes('\\[') || trimmed.includes('$$');
    const isBlockEnd = (endMatch && mathTableEnvs.includes(endMatch[1])) || trimmed.includes('\\]') || trimmed.includes('$$');
    
    if (isBlockStart) {
      inBlock = true;
    }
    
    let result = line;
    
    if (!inBlock) {
      const isStructureCommand = trimmed.startsWith('\\section') || 
                                 trimmed.startsWith('\\subsection') || 
                                 trimmed.startsWith('\\title') || 
                                 trimmed.startsWith('\\author') || 
                                 trimmed.startsWith('\\documentclass') || 
                                 trimmed.startsWith('\\usepackage') || 
                                 trimmed.startsWith('\\maketitle');
      
      if (!isStructureCommand) {
        if (trimmed.startsWith('\\item')) {
          const itemIdx = line.indexOf('\\item');
          const pre = line.substring(0, itemIdx + 5);
          const post = line.substring(itemIdx + 5);
          result = `${pre}<span data-line="${lineNum}" class="hover:bg-indigo-50/50 transition-colors">${post}</span>`;
        } else {
          result = `<span data-line="${lineNum}" class="hover:bg-indigo-50/50 transition-colors">${line}</span>`;
        }
      }
    }
    
    if (isBlockEnd) {
      inBlock = false;
    }
    
    return result;
  });
  
  return wrappedLines.join('\n');
}


async function loadAllProjectSources(projectId) {
  const sources = [];
  let page = 0;
  let last = false;
  while (!last) {
    const response = await api.get(`/api/projects/${projectId}/sources`, {
      params: { page, size: 100, active: true },
    });
    sources.push(...(response.data?.content || []));
    last = response.data?.last ?? true;
    page += 1;
  }
  return sources;
}

function DiffView({ ops }) {
  return (
    <div className="font-mono text-xs whitespace-pre-wrap leading-relaxed max-h-[55vh] overflow-y-auto pr-1 hide-scrollbar">
      {ops.map((op, i) => op[0] === 0 ? <span key={i}>{op[1]}</span>
        : op[0] === 1 ? <span key={i} className="bg-emerald-100 text-emerald-800 rounded px-0.5">{op[1]}</span>
          : <span key={i} className="bg-rose-100 text-rose-700 line-through rounded px-0.5">{op[1]}</span>)}
    </div>
  );
}

const ACTION_LABELS = {
  REVIEWED: { key: 'approve', cls: 'bg-emerald-600 hover:bg-emerald-700' },
  RETURNED: { key: 'returnForRevision', cls: 'bg-amber-500 hover:bg-amber-600' },
};

export default function ReviewSpace() {
  const { projectId } = useParams();
  const navigate = useNavigate();
  const { language } = useLanguage();
  const t = instructorText[language];
  const ct = commonText[language];
  const [project, setProject] = useState(null);
  const [papers, setPapers] = useState([]);
  const [sections, setSections] = useState([]);
  const [selectedPaperId, setSelectedPaperId] = useState(null);
  const [selectedSectionId, setSelectedSectionId] = useState(null);
  const [requests, setRequests] = useState([]);
  const [activeRequestId, setActiveRequestId] = useState(null);
  const [feedbackItems, setFeedbackItems] = useState([]);
  const [sources, setSources] = useState([]);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [diffEnabled, setDiffEnabled] = useState(false);
  const [baseline, setBaseline] = useState(null);
  const [feedbackDraft, setFeedbackDraft] = useState('');
  const [feedbackLineRef, setFeedbackLineRef] = useState('');
  const [editingFeedbackId, setEditingFeedbackId] = useState(null);
  const [savingFeedback, setSavingFeedback] = useState(false);
  const [mediaUrlMap, setMediaUrlMap] = useState({});
  const [transitioningRequestId, setTransitioningRequestId] = useState(null);
  const [hoveredLine, setHoveredLine] = useState(null);
  const [tooltipPos, setTooltipPos] = useState({ x: 0, y: 0 });

  useEffect(() => {
    let cancelled = false;
    (async () => {
      setLoading(true);
      setErrorMessage('');
      try {
        const [proj, papersRes, reqs, srcs] = await Promise.all([
          api.get(`/api/projects/${projectId}`),
          api.get(`/api/projects/${projectId}/papers`),
          api.get('/api/feedback-requests'),
          loadAllProjectSources(projectId).catch(() => []),
        ]);
        if (cancelled) return;
        setProject(proj.data);
        setPapers(papersRes.data || []);
        setRequests((reqs.data || []).filter(r => String(r.projectId) === String(projectId)));
        setSources(srcs);
        if ((papersRes.data || []).length > 0) setSelectedPaperId(papersRes.data[0].id);
      } catch {
        if (!cancelled) setErrorMessage(t.loadReviewSpaceFailed);
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => { cancelled = true; };
  }, [projectId]);

  const activeRequest = requests.find(r => r.id === activeRequestId) || requests[0] || null;

  const requestLocked = !activeRequest || (activeRequest.status !== 'PENDING' && activeRequest.status !== 'RETURNED');

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const assets = await api.get(`/api/media/projects/${projectId}`);
        if (cancelled || !assets.data?.length) return;
        const r = await api.post('/api/media/urls', { ids: assets.data.map(a => a.id) });
        if (cancelled) return;
        const urls = r.data || {};
        const map = {};
        for (const asset of assets.data) {
          const url = urls[asset.id];
          if (url) map[asset.texFilename] = url;
        }
        setMediaUrlMap(map);
      } catch {
        if (!cancelled) setMediaUrlMap({});
      }
    })();
    return () => { cancelled = true; };
  }, [projectId]);

  useEffect(() => {
    if (requests.length > 0 && !requests.some(r => r.id === activeRequestId)) {
      setActiveRequestId((requests.find(r => r.status === 'PENDING') || requests[0]).id);
    }
  }, [requests, activeRequestId]);

  useEffect(() => {
    if (!activeRequestId) { setFeedbackItems([]); return; }
    let cancelled = false;
    api.get(`/api/feedback-requests/${activeRequestId}/feedback`)
      .then(r => { if (!cancelled) setFeedbackItems(r.data || []); })
      .catch(() => { if (!cancelled) setFeedbackItems([]); });
    return () => { cancelled = true; };
  }, [activeRequestId]);

  useEffect(() => {
    if (!selectedPaperId) { setSections([]); setSelectedSectionId(null); return; }
    let cancelled = false;
    api.get(`/api/papers/${selectedPaperId}/sections`)
      .then(r => { if (!cancelled) { setSections(r.data || []); setSelectedSectionId(prev => prev || (r.data || [])[0]?.id || null); } })
      .catch(() => { if (!cancelled) setSections([]); });
    return () => { cancelled = true; };
  }, [selectedPaperId]);

  useEffect(() => {
    if (!diffEnabled || !projectId || !selectedSectionId) { setBaseline(null); return; }
    let cancelled = false;
    api.get(`/api/projects/${projectId}/checkpoints/latest/sections/${selectedSectionId}`, {
      params: activeRequest?.requestedAt ? { before: activeRequest.requestedAt } : {},
    })
      .then(r => { if (!cancelled) setBaseline(r.data); })
      .catch(() => { if (!cancelled) setBaseline(null); });
    return () => { cancelled = true; };
  }, [diffEnabled, projectId, selectedSectionId, activeRequest?.requestedAt]);

  const selectedSection = sections.find(s => String(s.id) === String(selectedSectionId)) || null;

  const lineRefContent = useMemo(() => {
    const map = new Map();
    for (const fb of feedbackItems) {
      if (String(fb.sectionId) !== String(selectedSectionId || '')) continue;
      if (fb.lineReference) map.set(fb.lineReference, fb.content);
    }
    return map;
  }, [feedbackItems, selectedSectionId]);

  const sectionLineRefs = Array.from(lineRefContent.keys());

  const diffOps = useMemo(() => {
    if (!diffEnabled || !baseline || !selectedSection) return null;
    const dmp = new DiffMatchPatch();
    const ops = dmp.diff_main(baseline.contentTex || '', selectedSection.contentTex || '');
    dmp.diff_cleanupSemantic(ops);
    return ops;
  }, [diffEnabled, baseline, selectedSection]);

  const loadFeedback = useCallback(() => {
    if (!activeRequestId) return;
    api.get(`/api/feedback-requests/${activeRequestId}/feedback`)
      .then(r => setFeedbackItems(r.data || []))
      .catch(() => setErrorMessage(t.loadFeedbackFailed));
  }, [activeRequestId, t.loadFeedbackFailed]);

  const handleSubmitFeedback = async (e) => {
    e.preventDefault();
    if (!activeRequestId || !selectedSectionId || !feedbackDraft.trim()) return;
    setSavingFeedback(true); setErrorMessage('');
    try {
      const body = {
        sectionId: selectedSectionId,
        lineReference: feedbackLineRef.trim() || null,
        content: feedbackDraft.trim(),
      };
      if (editingFeedbackId) {
        await api.patch(`/api/instructor-feedback/${editingFeedbackId}`, body);
      } else {
        await api.post(`/api/feedback-requests/${activeRequestId}/feedback`, body);
      }
      setFeedbackDraft(''); setFeedbackLineRef(''); setEditingFeedbackId(null);
      loadFeedback();
    } catch (err) {
      setErrorMessage(err?.response?.data?.message || t.saveFeedbackFailed);
    } finally { setSavingFeedback(false); }
  };

  const handleEditFeedback = (item) => {
    setEditingFeedbackId(item.id);
    setFeedbackDraft(item.content || '');
    setFeedbackLineRef(item.lineReference || '');
  };

  const handleCancelEdit = () => {
    setEditingFeedbackId(null);
    setFeedbackDraft('');
    setFeedbackLineRef('');
  };

  const handleDeleteFeedback = async (itemId) => {
    if (!window.confirm(t.deleteFeedbackConfirm)) return;
    setErrorMessage('');
    try {
      await api.delete(`/api/instructor-feedback/${itemId}`);
      loadFeedback();
    } catch (err) {
      setErrorMessage(err?.response?.data?.message || t.deleteFeedbackFailed);
    }
  };

  const handleTransitionStatus = async (requestId, targetStatus) => {
    setErrorMessage(''); setSuccessMessage('');
    setTransitioningRequestId(requestId);
    try {
      const res = await api.patch(`/api/feedback-requests/${requestId}/status?status=${targetStatus}`);
      setRequests(prev => prev.map(r => r.id === requestId ? { ...r, status: res.data.status } : r));
      setSuccessMessage(targetStatus === 'REVIEWED' ? t.reviewApproved : t.reviewReturned);
      if (targetStatus === 'REVIEWED') {
        setTimeout(() => navigate('/instructor/requests'), 1000);
      }
    } catch (err) {
      setErrorMessage(err?.response?.data?.message || t.updateStatusFailed);
    } finally { setTransitioningRequestId(null); }
  };

  const handleMouseMove = (e) => {
    const target = e.target.closest('[data-line]');
    if (target) {
      setHoveredLine(target.getAttribute('data-line'));
      setTooltipPos({ x: e.clientX, y: e.clientY });
    } else {
      setHoveredLine(null);
    }
  };

  const handleMouseLeave = () => {
    setHoveredLine(null);
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-(--page-bg)">
        <AppHeader />
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8"><LoadingSkeleton count={4} height="h-24" /></div>
      </div>
    );
  }

  const sectionFeedback = feedbackItems.filter(fb => !selectedSectionId || String(fb.sectionId) === String(selectedSectionId));

  return (
    <div className="min-h-screen bg-(--page-bg) text-(--text-primary)">
      <AppHeader />
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
        <div className="flex flex-col lg:flex-row lg:items-start justify-between gap-4 border-b border-(--border) pb-6">
          <div>
            <Link to="/instructor/requests" className="text-xs font-bold text-(--text-tertiary) hover:text-(--brand-foreground) transition-colors">&larr; {t.backToRequests}</Link>
            <h1 className="text-3xl font-black text-(--brand-foreground) tracking-tight mt-2">{project?.title || t.project}</h1>
            <div className="flex items-center gap-2 mt-2 flex-wrap">
              <StatusBadge status={project?.status} />
              {requests.map(req => (
                <button key={req.id} onClick={() => setActiveRequestId(req.id)}
                  className={`text-xs font-bold px-2 py-1 rounded-full border transition-colors ${req.id === activeRequest?.id ? 'bg-(--brand) text-(--on-brand) border-(--brand)' : 'bg-(--surface) text-(--text-secondary) border-(--border) hover:border-(--brand)'}`}>
                  {req.requestedAt ? new Date(req.requestedAt).toLocaleString(language === 'vi' ? 'vi-VN' : 'en-US') : String(req.id).slice(0, 8)} · <StatusBadge status={req.status} />
                </button>
              ))}
            </div>
          </div>
          <div className="flex flex-wrap gap-2 shrink-0">
            {activeRequest && (activeRequest.status === 'PENDING' || activeRequest.status === 'RETURNED') && (
              <>
                <button onClick={() => handleTransitionStatus(activeRequest.id, 'RETURNED')} disabled={transitioningRequestId === activeRequest.id}
                  className={`px-3 py-2 text-xs font-bold text-white rounded-xl transition ${ACTION_LABELS.RETURNED.cls} disabled:opacity-50`}>
                  {t[ACTION_LABELS.RETURNED.key]}
                </button>
                <button onClick={() => { if (window.confirm(t.finalizeReviewConfirm)) handleTransitionStatus(activeRequest.id, 'REVIEWED'); }} disabled={transitioningRequestId === activeRequest.id}
                  className={`px-3 py-2 text-xs font-bold text-white rounded-xl transition ${ACTION_LABELS.REVIEWED.cls} disabled:opacity-50`}>
                  {t[ACTION_LABELS.REVIEWED.key]}
                </button>
              </>
            )}
          </div>
        </div>

        {errorMessage && (
          <div className="p-4 rounded-xl bg-rose-50 border border-rose-100 text-rose-700 text-xs font-bold">{errorMessage}</div>
        )}
        {successMessage && (
          <div className="p-4 rounded-xl bg-emerald-50 border border-emerald-100 text-emerald-700 text-xs font-bold">{successMessage}</div>
        )}

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Paper (read-only) + diff */}
          <div className="lg:col-span-2 bg-(--surface) rounded-2xl border border-(--border) shadow-sm p-4 sm:p-6">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-sm font-bold text-(--brand-foreground)">{t.paperReadOnly}</h2>
              <label className="flex items-center gap-2 text-xs font-bold text-(--text-secondary) cursor-pointer select-none">
                <input type="checkbox" checked={diffEnabled} onChange={e => setDiffEnabled(e.target.checked)}
                  className="w-3.5 h-3.5 rounded border-gray-300 text-[#1e3a8a] focus:ring-[#1e3a8a]" />
                {t.showChanges}
              </label>
            </div>

            {papers.length === 0 ? (
              <p className="text-xs text-(--text-tertiary) italic">{t.noPapers}</p>
            ) : (
              <>
                <div className="flex gap-1 flex-wrap mb-3">
                  {papers.map(p => (
                    <button key={p.id} onClick={() => { setSelectedPaperId(p.id); setSelectedSectionId(null); }}
                      className={`px-2.5 py-1.5 rounded-lg text-xs font-bold transition-colors ${String(p.id) === String(selectedPaperId) ? 'bg-(--brand) text-(--on-brand)' : 'bg-(--surface-secondary) text-(--text-secondary) hover:bg-(--surface-tertiary)'}`}>
                      {p.originalFilename || p.title}
                    </button>
                  ))}
                </div>
                {sections.length === 0 ? (
                  <p className="text-xs text-(--text-tertiary) italic">{t.noPaperSections}</p>
                ) : (
                  <>
                    <div className="flex gap-1 flex-wrap mb-4">
                      {sections.map(s => (
                        <button key={s.id} onClick={() => setSelectedSectionId(s.id)}
                          className={`px-2.5 py-1.5 rounded-lg text-xs font-bold transition-colors ${String(s.id) === String(selectedSectionId) ? 'bg-(--brand) text-(--on-brand)' : 'bg-(--surface-secondary) text-(--text-secondary) hover:bg-(--surface-tertiary)'}`}>
                          {s.sectionTitle}
                          {s.version > 1 && <span className="ml-1 text-[9px]">v{s.version}</span>}
                        </button>
                      ))}
                    </div>
                    {!selectedSection ? (
                      <p className="text-xs text-(--text-tertiary) italic">{t.selectSectionContent}</p>
                    ) : diffEnabled ? (
                      diffOps === null ? (
                        <p className="text-xs text-(--text-tertiary) italic">{t.noCheckpointBaseline}</p>
                      ) : (
                        <div>
                          {baseline && (
                            <p className="text-[10px] text-(--text-tertiary) mb-2">
                              {t.baseline}: {baseline.trigger || t.checkpoint} · {baseline.createdAt ? new Date(baseline.createdAt).toLocaleString(language === 'vi' ? 'vi-VN' : 'en-US') : ''}
                            </p>
                          )}
                          <DiffView ops={diffOps} />
                        </div>
                      )
                    ) : (
                      <div className="max-h-[55vh] overflow-y-auto pr-1 whitespace-pre-wrap break-words preview-content hide-scrollbar"
                        onMouseMove={handleMouseMove}
                        onMouseLeave={handleMouseLeave}>
                        {sectionLineRefs.length > 0 && (
                          <div className="mb-2 flex flex-wrap gap-1">
                            {sectionLineRefs.map(reference => (
                              <span key={reference} className="bg-indigo-50 text-indigo-600 font-mono text-[10px] font-bold px-1.5 py-0.5 rounded">
                                {reference}
                              </span>
                            ))}
                          </div>
                        )}
                        <div dangerouslySetInnerHTML={{ __html: renderLatexToHtml(wrapLatexLines(selectedSection.contentTex), mediaUrlMap) }} />
                      </div>
                    )}
                  </>
                )}
              </>
            )}
          </div>

          {/* Right column: claims + feedback + sources */}
          <div className="space-y-6">
            <div className="bg-(--surface) rounded-2xl border border-(--border) shadow-sm p-4 sm:p-6">
              <div className="flex items-center justify-between gap-3 mb-4">
                <h2 className="text-sm font-bold text-(--brand-foreground)">{t.sectionFeedback}</h2>
              </div>
              {!selectedSectionId ? (
                <p className="text-xs text-(--text-tertiary) italic">{t.selectSectionFeedback}</p>
              ) : (
                <>
                  <div className="space-y-3 mb-4">
                    {sectionFeedback.length === 0 ? (
                      <p className="text-xs text-(--text-tertiary) italic">{t.noSectionFeedback}</p>
                    ) : sectionFeedback.map(fb => (
                      <div key={fb.id} className="bg-(--surface-secondary) border border-(--border-light) rounded-xl p-3 text-xs space-y-1">
                        <div className="flex items-center justify-between gap-2">
                          <span className="text-[9px] font-black text-indigo-600 bg-indigo-50 dark:bg-indigo-900/30 px-1.5 py-0.5 rounded">{t.section} {fb.sectionTitle || ''}</span>
                          <div className="flex items-center gap-1.5">
                            {fb.stale && <span className="text-[9px] font-bold bg-amber-100 text-amber-700 px-1.5 py-0.5 rounded">{t.sectionChanged}</span>}
                            {fb.answered && <span className="text-[9px] font-bold bg-emerald-100 text-emerald-700 px-1.5 py-0.5 rounded">{t.answered}</span>}
                            {!fb.answered && !requestLocked && (
                              <>
                                <button onClick={() => handleEditFeedback(fb)} className="text-(--text-tertiary) hover:text-(--brand) p-1" title={ct.edit} aria-label={ct.edit}><svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 13H9v-2.828l6.586-6.586z" /></svg></button>
                                <button onClick={() => handleDeleteFeedback(fb.id)} className="text-(--text-tertiary) hover:text-rose-600 p-1" title={ct.delete} aria-label={ct.delete}><svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6M4 7h16" /></svg></button>
                              </>
                            )}
                          </div>
                        </div>
                        {fb.lineReference && <p className="text-[10px] text-gray-400 font-mono">{fb.lineReference}</p>}
                        <p className="text-(--text-primary) leading-relaxed">{fb.content}</p>
                        {fb.answered && fb.answerContent && (
                          <p className="text-[10px] text-emerald-700 bg-emerald-50 dark:bg-emerald-900/30 rounded-lg p-2">{t.studentAnswer.replace('{{answer}}', fb.answerContent)}</p>
                        )}
                      </div>
                    ))}
                  </div>
                  {requestLocked ? (
                    <p className="text-xs text-(--text-tertiary) italic">{t.reviewClosed}</p>
                  ) : (
                    <form onSubmit={handleSubmitFeedback} className="space-y-2 border-t border-(--border-light) pt-3">
                      <input value={feedbackLineRef} onChange={e => setFeedbackLineRef(e.target.value)}
                        placeholder={t.lineReferencePlaceholder} maxLength={100}
                        className="w-full px-3 py-2 bg-(--surface-secondary) border border-(--border) rounded-xl text-xs text-(--text-primary) focus:outline-none focus:ring-2 focus:ring-(--focus)" />
                      <textarea rows="3" value={feedbackDraft} onChange={e => setFeedbackDraft(e.target.value)}
                        placeholder={t.sectionFeedbackPlaceholder}
                        className="w-full px-3 py-2 bg-(--surface-secondary) border border-(--border) rounded-xl text-xs text-(--text-primary) focus:outline-none focus:ring-2 focus:ring-(--focus)" />
                      <div className="flex gap-2">
                        {editingFeedbackId && (
                          <button type="button" onClick={handleCancelEdit}
                            className="flex-1 py-2 bg-(--surface-secondary) text-(--text-secondary) rounded-xl hover:bg-(--surface-tertiary) transition-colors text-xs font-bold">{ct.cancel}</button>
                        )}
                        <button type="submit" disabled={savingFeedback || !feedbackDraft.trim()}
                          className="flex-1 py-2 bg-(--brand) text-(--on-brand) rounded-xl hover:bg-(--brand-hover) transition-colors shadow-sm disabled:opacity-50 text-xs font-bold">
                          {savingFeedback ? ct.saving : editingFeedbackId ? t.updateFeedback : t.addFeedback}
                        </button>
                      </div>
                    </form>
                  )}
                </>
              )}
            </div>

            <div className="bg-(--surface) rounded-2xl border border-(--border) shadow-sm p-4 sm:p-6">
              <h2 className="text-sm font-bold text-(--brand-foreground) mb-4">{t.sources}</h2>
              {sources.length === 0 ? (
                <p className="text-xs text-(--text-tertiary) italic">{t.noProjectSources}</p>
              ) : (
                <div className="space-y-2">
                  {sources.map(src => (
                    <div key={src.id} className="flex items-center justify-between gap-2 bg-(--surface-secondary) border border-(--border-light) rounded-lg px-3 py-2 text-xs">
                      <div className="min-w-0">
                        <p className="font-medium truncate">{src.title || src.originalFilename || src.id}</p>
                        <StatusBadge status={src.processingStatus || 'READY'} />
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>
      </main>

      {hoveredLine && (
        <div 
          className="fixed z-50 bg-[#1e3a8a] text-white text-[10px] font-bold px-2 py-1 rounded shadow-md pointer-events-none transition-all duration-75"
          style={{ left: tooltipPos.x + 15, top: tooltipPos.y - 10 }}
        >
          {t.lineNumber.replace('{{line}}', hoveredLine)}
        </div>
      )}
    </div>
  );
}
