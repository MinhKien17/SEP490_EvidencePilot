import { useState, useEffect, useMemo, useCallback } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { StatusBadge, LoadingSkeleton, AppHeader } from '../../components';
import DiffMatchPatch from 'diff-match-patch';
import api from '../../api.js';
import { renderLatexToHtml } from '../../components/latexHtml.js';

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
    <div className="font-mono text-xs whitespace-pre-wrap leading-relaxed max-h-[55vh] overflow-y-auto pr-1">
      {ops.map((op, i) => op[0] === 0 ? <span key={i}>{op[1]}</span>
        : op[0] === 1 ? <span key={i} className="bg-emerald-100 text-emerald-800 rounded px-0.5">{op[1]}</span>
          : <span key={i} className="bg-rose-100 text-rose-700 line-through rounded px-0.5">{op[1]}</span>)}
    </div>
  );
}

const ACTION_LABELS = {
  REVIEWED: { label: 'Approve', cls: 'bg-emerald-600 hover:bg-emerald-700' },
  RETURNED: { label: 'Return for revision', cls: 'bg-amber-500 hover:bg-amber-600' },
};

export default function ReviewSpace() {
  const { projectId } = useParams();
  const navigate = useNavigate();
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
        if (!cancelled) setErrorMessage('Failed to load the review space.');
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
      .catch(() => setErrorMessage('Failed to load feedback items.'));
  }, [activeRequestId]);

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
      setErrorMessage(err?.response?.data?.message || 'Failed to save feedback.');
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
    if (!window.confirm('Delete this feedback item?')) return;
    setErrorMessage('');
    try {
      await api.delete(`/api/instructor-feedback/${itemId}`);
      loadFeedback();
    } catch (err) {
      setErrorMessage(err?.response?.data?.message || 'Failed to delete feedback.');
    }
  };

  const handleTransitionStatus = async (requestId, targetStatus) => {
    setErrorMessage(''); setSuccessMessage('');
    setTransitioningRequestId(requestId);
    try {
      const res = await api.patch(`/api/feedback-requests/${requestId}/status?status=${targetStatus}`);
      setRequests(prev => prev.map(r => r.id === requestId ? { ...r, status: res.data.status } : r));
      setSuccessMessage(`Review ${targetStatus === 'REVIEWED' ? 'approved' : 'returned for revision'}.`);
      if (targetStatus === 'REVIEWED') {
        setTimeout(() => navigate('/instructor/requests'), 1000);
      }
    } catch (err) {
      setErrorMessage(err?.response?.data?.message || 'Failed to update status.');
    } finally { setTransitioningRequestId(null); }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-[#f8fafc]">
        <AppHeader />
        <div className="max-w-7xl mx-auto p-8"><LoadingSkeleton count={4} height="h-24" /></div>
      </div>
    );
  }

  const sectionFeedback = feedbackItems.filter(fb => !selectedSectionId || String(fb.sectionId) === String(selectedSectionId));

  return (
    <div className="min-h-screen bg-[#f8fafc] text-[#0f172a]">
      <AppHeader />
      <div className="max-w-7xl mx-auto p-8 space-y-6">
        <div className="flex items-start justify-between border-b border-gray-200 pb-6">
          <div>
            <Link to="/instructor/requests" className="text-xs font-bold text-gray-400 hover:text-[#1e3a8a] transition-colors">&larr; Back to requests</Link>
            <h1 className="text-3xl font-black text-[#1e3a8a] tracking-tight mt-2">{project?.title || 'Project'}</h1>
            <div className="flex items-center gap-2 mt-2 flex-wrap">
              <StatusBadge status={project?.status} />
              {requests.map(req => (
                <button key={req.id} onClick={() => setActiveRequestId(req.id)}
                  className={`text-[10px] font-bold px-2 py-0.5 rounded-full border transition ${req.id === activeRequest?.id ? 'bg-[#1e3a8a] text-white border-[#1e3a8a]' : 'bg-white text-gray-500 border-gray-200 hover:border-[#1e3a8a]'}`}>
                  {req.requestedAt ? new Date(req.requestedAt).toLocaleString() : String(req.id).slice(0, 8)} · <StatusBadge status={req.status} />
                </button>
              ))}
            </div>
          </div>
          <div className="flex gap-2 shrink-0">
            {activeRequest && (activeRequest.status === 'PENDING' || activeRequest.status === 'RETURNED') && (
              <>
                <button onClick={() => handleTransitionStatus(activeRequest.id, 'RETURNED')} disabled={transitioningRequestId === activeRequest.id}
                  className={`px-3 py-2 text-xs font-bold text-white rounded-xl transition ${ACTION_LABELS.RETURNED.cls} disabled:opacity-50`}>
                  {ACTION_LABELS.RETURNED.label}
                </button>
                <button onClick={() => { if (window.confirm('Finalize review and save all feedback?')) handleTransitionStatus(activeRequest.id, 'REVIEWED'); }} disabled={transitioningRequestId === activeRequest.id}
                  className={`px-3 py-2 text-xs font-bold text-white rounded-xl transition ${ACTION_LABELS.REVIEWED.cls} disabled:opacity-50`}>
                  {ACTION_LABELS.REVIEWED.label}
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
          <div className="lg:col-span-2 bg-white rounded-3xl border border-gray-200 shadow-sm p-6">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-sm font-bold text-[#1e3a8a]">Paper — read only</h2>
              <label className="flex items-center gap-2 text-[11px] font-bold text-gray-500 cursor-pointer select-none">
                <input type="checkbox" checked={diffEnabled} onChange={e => setDiffEnabled(e.target.checked)}
                  className="w-3.5 h-3.5 rounded border-gray-300 text-[#1e3a8a] focus:ring-[#1e3a8a]" />
                Show changes since last checkpoint
              </label>
            </div>

            {papers.length === 0 ? (
              <p className="text-xs text-gray-400 italic">No papers uploaded yet.</p>
            ) : (
              <>
                <div className="flex gap-1 flex-wrap mb-3">
                  {papers.map(p => (
                    <button key={p.id} onClick={() => { setSelectedPaperId(p.id); setSelectedSectionId(null); }}
                      className={`px-2.5 py-1.5 rounded-lg text-[10px] font-bold transition ${String(p.id) === String(selectedPaperId) ? 'bg-[#1e3a8a] text-white' : 'bg-gray-100 text-gray-500 hover:bg-gray-200'}`}>
                      {p.originalFilename || p.title}
                    </button>
                  ))}
                </div>
                {sections.length === 0 ? (
                  <p className="text-xs text-gray-400 italic">No sections in this paper.</p>
                ) : (
                  <>
                    <div className="flex gap-1 flex-wrap mb-4">
                      {sections.map(s => (
                        <button key={s.id} onClick={() => setSelectedSectionId(s.id)}
                          className={`px-2.5 py-1 rounded-lg text-[10px] font-bold transition ${String(s.id) === String(selectedSectionId) ? 'bg-indigo-600 text-white' : 'bg-gray-100 text-gray-500 hover:bg-gray-200'}`}>
                          {s.sectionTitle}
                          {s.version > 1 && <span className="ml-1 text-[9px]">v{s.version}</span>}
                        </button>
                      ))}
                    </div>
                    {!selectedSection ? (
                      <p className="text-xs text-gray-400 italic">Select a section to view its content.</p>
                    ) : diffEnabled ? (
                      diffOps === null ? (
                        <p className="text-xs text-gray-400 italic">No prior checkpoint baseline for this section — capture one by submitting for review.</p>
                      ) : (
                        <div>
                          {baseline && (
                            <p className="text-[10px] text-gray-400 mb-2">
                              Baseline: {baseline.trigger || 'checkpoint'} · {baseline.createdAt ? new Date(baseline.createdAt).toLocaleString() : ''}
                            </p>
                          )}
                          <DiffView ops={diffOps} />
                        </div>
                      )
                    ) : (
                      <div
                        className="max-h-[55vh] overflow-y-auto pr-1 whitespace-pre-wrap break-words preview-content"
                        dangerouslySetInnerHTML={{ __html: renderLatexToHtml(selectedSection.contentTex, mediaUrlMap) }}
                      />
                    )}
                  </>
                )}
              </>
            )}
          </div>

          {/* Right column: feedback + sources */}
          <div className="space-y-6">
            <div className="bg-white rounded-3xl border border-gray-200 shadow-sm p-6">
              <h2 className="text-sm font-bold text-[#1e3a8a] mb-4">Section Feedback</h2>
              {!selectedSectionId ? (
                <p className="text-xs text-gray-400 italic">Select a paper section to add or review feedback.</p>
              ) : (
                <>
                  <div className="space-y-3 mb-4">
                    {sectionFeedback.length === 0 ? (
                      <p className="text-xs text-gray-400 italic">No feedback for this section yet.</p>
                    ) : sectionFeedback.map(fb => (
                      <div key={fb.id} className="bg-gray-50 rounded-xl p-3 text-xs space-y-1">
                        <div className="flex items-center justify-between gap-2">
                          <span className="text-[9px] font-black text-indigo-600 bg-indigo-50 px-1.5 py-0.5 rounded">Section {fb.sectionTitle || ''}</span>
                          <div className="flex items-center gap-1.5">
                            {fb.stale && <span className="text-[9px] font-bold bg-amber-100 text-amber-700 px-1.5 py-0.5 rounded">section changed</span>}
                            {fb.answered && <span className="text-[9px] font-bold bg-emerald-100 text-emerald-700 px-1.5 py-0.5 rounded">answered</span>}
                            {!fb.answered && !requestLocked && (
                              <>
                                <button onClick={() => handleEditFeedback(fb)} className="text-gray-400 hover:text-indigo-600 text-xs" title="Edit">&#9998;</button>
                                <button onClick={() => handleDeleteFeedback(fb.id)} className="text-gray-400 hover:text-rose-600 text-xs" title="Delete">&#10005;</button>
                              </>
                            )}
                          </div>
                        </div>
                        {fb.lineReference && <p className="text-[10px] text-gray-400 font-mono">{fb.lineReference}</p>}
                        <p className="text-gray-700 leading-relaxed">{fb.content}</p>
                        {fb.answered && fb.answerContent && (
                          <p className="text-[10px] text-emerald-700 bg-emerald-50 rounded-lg p-2">Student: {fb.answerContent}</p>
                        )}
                      </div>
                    ))}
                  </div>
                  {requestLocked ? (
                    <p className="text-xs text-gray-400 italic">This review is closed — feedback is read-only.</p>
                  ) : (
                    <form onSubmit={handleSubmitFeedback} className="space-y-2 border-t border-gray-100 pt-3">
                      <input value={feedbackLineRef} onChange={e => setFeedbackLineRef(e.target.value)}
                        placeholder="Line reference (optional, max 100 chars)" maxLength={100}
                        className="w-full px-3 py-2 bg-gray-50 border border-gray-200 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-[#1e3a8a]" />
                      <textarea rows="3" value={feedbackDraft} onChange={e => setFeedbackDraft(e.target.value)}
                        placeholder="Feedback for this section..."
                        className="w-full px-3 py-2 bg-gray-50 border border-gray-200 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-[#1e3a8a]" />
                      <div className="flex gap-2">
                        {editingFeedbackId && (
                          <button type="button" onClick={handleCancelEdit}
                            className="flex-1 py-2 bg-gray-100 text-gray-600 rounded-xl hover:bg-gray-200 transition text-xs font-bold">Cancel</button>
                        )}
                        <button type="submit" disabled={savingFeedback || !feedbackDraft.trim()}
                          className="flex-1 py-2 bg-[#1e3a8a] text-white rounded-xl hover:bg-blue-800 transition shadow-sm disabled:opacity-50 text-xs font-bold">
                          {savingFeedback ? 'Saving...' : editingFeedbackId ? 'Update feedback' : 'Add feedback'}
                        </button>
                      </div>
                    </form>
                  )}
                </>
              )}
            </div>

            <div className="bg-white rounded-3xl border border-gray-200 shadow-sm p-6">
              <h2 className="text-sm font-bold text-[#1e3a8a] mb-4">Sources</h2>
              {sources.length === 0 ? (
                <p className="text-xs text-gray-400 italic">No sources in this project.</p>
              ) : (
                <div className="space-y-2">
                  {sources.map(src => (
                    <div key={src.id} className="flex items-center justify-between gap-2 bg-gray-50 rounded-lg px-3 py-2 text-xs">
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
      </div>
    </div>
  );
}
