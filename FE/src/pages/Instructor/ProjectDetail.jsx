import { useState, useEffect, useCallback, useMemo } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { DragDropContext, Droppable, Draggable } from '@hello-pangea/dnd';
import { AppHeader, LoadingSkeleton, StatusBadge, Modal, TourLauncher, EvidenceGraph, Spinner, FunctionalTypeRadar } from '../../components';
import { Marker, MarkerIcon, MarkerContent } from '../../components/Marker';
import { instructorText, commonText } from '../../locales';
import { useLanguage } from '../../context/LanguageContext';
import api from '../../api';

const STANDARDS = ['IEEE', 'ACM', 'SPRINGER_LNCS', 'APA', 'MLA', 'CUSTOM'];

export default function ProjectDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { language } = useLanguage();
  const ct = commonText[language];
  const t = instructorText[language];
  const [activeTab, setActiveTab] = useState('setup');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [project, setProject] = useState(null);
  const [members, setMembers] = useState([]);
  const [papers, setPapers] = useState([]);
  const [sections, setSections] = useState([]);
  const [selectedPaper, setSelectedPaper] = useState(null);
  const [feedbackRequests, setFeedbackRequests] = useState([]);
  const [traceability, setTraceability] = useState(null);
  const [graphData, setGraphData] = useState(null);
  const [claimStats, setClaimStats] = useState(null);
  const [progressReport, setProgressReport] = useState(null);
  const [checkpointDiff, setCheckpointDiff] = useState(null);
  const [reportSectionId, setReportSectionId] = useState(null);
  const [reportPane, setReportPane] = useState('matrix');
  const [reviewPane, setReviewPane] = useState('distribution');
  const [users, setUsers] = useState([]);
  const [showAddMember, setShowAddMember] = useState(false);
  const [newMemberId, setNewMemberId] = useState('');
  const [newMemberRole, setNewMemberRole] = useState('MEMBER');

  // Setup tab state
  const [doiInput, setDoiInput] = useState('');
  const [standard, setStandard] = useState('');
  const [sources, setSources] = useState([]);
  const [showSourceDetail, setShowSourceDetail] = useState(false);
  const [sourceDetail, setSourceDetail] = useState(null);
  const [showAddSource, setShowAddSource] = useState(false);
  const [showShareCollection, setShowShareCollection] = useState(false);
  const [collections, setCollections] = useState([]);
  const [linkedCollections, setLinkedCollections] = useState([]);
  const [selectedCollectionId, setSelectedCollectionId] = useState('');
  const [showSetUpPaper, setShowSetUpPaper] = useState(false);
  const [setupMode, setSetupMode] = useState('standard');
  const [editingPaperId, setEditingPaperId] = useState(null);
  const [editingPaperTitle, setEditingPaperTitle] = useState('');
  const [editingSectionId, setEditingSectionId] = useState(null);
  const [editingSectionTitle, setEditingSectionTitle] = useState('');
  const [sectionStructureSaving, setSectionStructureSaving] = useState(false);
  const [orderDirty, setOrderDirty] = useState(false);
  const [uploadState, setUploadState] = useState(null);
  const [showExportModal, setShowExportModal] = useState(false);
  const [addSourceDocType, setAddSourceDocType] = useState('SOURCE');
  const [addSourceLoading, setAddSourceLoading] = useState(false);
  const [shareLoadingId, setShareLoadingId] = useState(null);
  const [pendingAssign, setPendingAssign] = useState(null); // { sectionId, userId, userName }

  const loadProject = useCallback(async () => {
    try {
      setLoading(true);
      const [projRes, memRes] = await Promise.all([
        api.get(`/api/projects/${id}`),
        api.get(`/api/projects/${id}/members`).catch(() => ({ data: [] })),
      ]);
      setProject(projRes.data);
      setStandard(projRes.data.targetStandard || '');
      setMembers(memRes.data || []);
    } catch { navigate('/instructor/projects'); }
    finally { setLoading(false); }
  }, [id, navigate]);

  const loadPapers = useCallback(async () => {
    try {
      const res = await api.get(`/api/projects/${id}/papers`);
      setPapers(res.data || []);
    } catch { }
  }, [id]);

  const loadSections = useCallback(async (paperId) => {
    try {
      const res = await api.get(`/api/papers/${paperId}/sections`);
      setSections(res.data || []);
      setOrderDirty(false);
    } catch { setSections([]); setOrderDirty(false); }
  }, []);

  const loadFeedbackAndTraceability = useCallback(async () => {
    try {
      const [fbRes, traceRes, graphRes, statsRes] = await Promise.all([
        api.get('/api/feedback-requests'),
        api.get(`/api/projects/${id}/traceability`).catch(() => null),
        api.get(`/api/projects/${id}/graph?scope=all`).catch(() => null),
        api.get(`/api/projects/${id}/graph/claim-stats`).catch(() => null),
      ]);
      const projectFbs = (fbRes.data || []).filter(fb => fb.projectId === id);
      setFeedbackRequests(projectFbs);
      setTraceability(traceRes?.data || null);
      setGraphData(graphRes?.data || null);
      setClaimStats(statsRes?.data || null);
    } catch { }
  }, [id]);

  const loadProgressReport = useCallback(async () => {
    try {
      const [progRes, diffRes] = await Promise.all([
        api.get(`/api/projects/${id}/progress-report`).catch(() => null),
        api.get(`/api/projects/${id}/checkpoints/diff`).catch(() => null),
      ]);
      setProgressReport(progRes?.data || null);
      setCheckpointDiff(diffRes?.data || null);
    } catch { }
  }, [id]);

  const loadUsers = useCallback(async () => {
    try {
      const res = await api.get('/api/users?role=STUDENT');
      setUsers(res.data || []);
    } catch { }
  }, []);

  const loadSources = useCallback(async () => {
    try {
      const res = await api.get(`/api/sources/projects/${id}`);
      setSources(res.data || []);
    } catch { }
  }, [id]);

  const loadCollections = useCallback(async () => {
    try {
      // ponytail: backend caps collection pages at 100; add modal pagination when an instructor exceeds that.
      const [collectionRes, linkedRes] = await Promise.all([
        api.get('/api/collections', { params: { size: 100 } }),
        api.get(`/api/projects/${id}/collections`),
      ]);
      setCollections(collectionRes.data?.content || collectionRes.data || []);
      setLinkedCollections(linkedRes.data || []);
    } catch { }
  }, [id]);

  useEffect(() => { loadProject(); }, [loadProject]);
  useEffect(() => { if (project) { loadPapers(); loadSources(); loadUsers(); } }, [project, loadPapers, loadSources, loadUsers]);

  const sectionMatrix = useMemo(() => {
    const rows = progressReport?.matrix || [];
    return reportSectionId ? rows.filter(r => String(r.sectionId) === String(reportSectionId)) : rows;
  }, [progressReport, reportSectionId]);

  const sectionDiff = useMemo(() => {
    if (!checkpointDiff) return null;
    const bySection = arr => reportSectionId
      ? arr.filter(c => String(c.sectionId) === String(reportSectionId)) : arr;
    return {
      ...checkpointDiff,
      claimsAdded: bySection(checkpointDiff.claimsAdded || []),
      claimsRemoved: bySection(checkpointDiff.claimsRemoved || []),
      claimsChanged: bySection(checkpointDiff.claimsChanged || []),
      sectionWordDeltas: (checkpointDiff.sectionWordDeltas || [])
        .filter(d => !reportSectionId || String(d.sectionId) === String(reportSectionId)),
    };
  }, [checkpointDiff, reportSectionId]);

  useEffect(() => {
    if (activeTab === 'review') loadFeedbackAndTraceability();
    if (activeTab === 'progress') loadProgressReport();
  }, [activeTab, loadFeedbackAndTraceability, loadProgressReport]);

  const handleUpdateStandard = async () => {
    if (!standard || !project) return;
    setSaving(true);
    try {
      await api.post(`/api/projects/${id}/papers/reset-standard?standard=${standard}`);
      await api.put(`/api/projects/${id}`, { ...project, targetStandard: standard });
      await loadProject();
      const papersRes = await api.get(`/api/projects/${id}/papers`);
      const freshPapers = papersRes.data || [];
      setPapers(freshPapers);
      const canonicalPaper = freshPapers.find(p => p.id === selectedPaper?.id) || freshPapers[0] || null;
      setSelectedPaper(canonicalPaper);
      if (canonicalPaper) {
        await loadSections(canonicalPaper.id);
      }
      setShowSetUpPaper(false);
    } catch { alert(t.updateStandardFailed); }
    finally { setSaving(false); }
  };

  const handleImportDoiUnified = async (asSource) => {
    if (!doiInput.trim()) return;
    setAddSourceLoading(true);
    try {
      const payload = {
        doi: doiInput.trim(),
        projectId: id,
      };
      if (asSource) payload.docType = 'SOURCE';
      await api.post('/api/documents/ingest/doi', payload);
      setDoiInput('');
      if (asSource) {
        await loadSources();
      } else {
        const papersRes = await api.get(`/api/projects/${id}/papers`);
        setPapers(papersRes.data || []);
      }
      setShowAddSource(false);
    } catch { alert(t.doiImportFailed); }
    finally { setAddSourceLoading(false); }
  };

  const handleUploadSource = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const formData = new FormData();
    formData.append('file', file);
    formData.append('projectId', id);
    try {
      await api.post('/api/sources', formData);
      await loadSources();
    } catch { alert(t.uploadFailed); }
  };

  const handleUploadPaper = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const formData = new FormData();
    formData.append('file', file);
    formData.append('projectId', id);
    setUploadState('uploading');
    try {
      const { data: doc } = await api.post('/api/papers', formData);
      setSelectedPaper(doc);
      setUploadState('processing');
      loadPapers();
      loadProject();
      if (doc?.id) loadSections(doc.id);
    } catch (err) {
      const msg = err?.response?.data?.message || err?.response?.data || t.uploadFailed;
      if (err?.response?.status === 409) {
        alert(msg);
      } else {
        alert(t.uploadFailed);
      }
      setUploadState(null);
    }
  };

  const handleToggleCollection = async () => {
    if (!selectedCollectionId) return;
    const linked = linkedCollections.some(c => String(c.id) === String(selectedCollectionId));
    setShareLoadingId(selectedCollectionId);
    try {
      if (linked) {
        await api.delete(`/api/projects/${id}/collections/${selectedCollectionId}`);
      } else {
        await api.put(`/api/projects/${id}/collections/${selectedCollectionId}`);
      }
      await Promise.all([loadCollections(), loadSources()]);
    } catch { alert(t.operationFailed); }
    finally { setShareLoadingId(null); }
  };

  const handleStartRename = (paper) => {
    setEditingPaperId(paper.id);
    setEditingPaperTitle(paper.title || paper.originalFilename || '');
  };

  const handleSaveRename = async (paperId) => {
    if (!editingPaperTitle.trim()) return;
    try {
      const newTitle = editingPaperTitle.trim();
      const newFilename = newTitle.endsWith('.tex') ? newTitle : newTitle + '.tex';
      await api.put(`/api/papers/${paperId}`, null, { params: { title: newTitle, originalFilename: newFilename } });
      setEditingPaperId(null);
      await loadPapers();
    } catch { alert(t.renameFailed); }
  };

  const handleDragEnd = (result) => {
    if (!result.destination || result.destination.index === result.source.index || !selectedPaper) return;
    const reordered = Array.from(sections);
    const [moved] = reordered.splice(result.source.index, 1);
    reordered.splice(result.destination.index, 0, moved);
    setSections(reordered);
    setOrderDirty(true);
  };

  const handleSaveSectionOrder = async () => {
    if (!selectedPaper || !orderDirty) return;
    setSectionStructureSaving(true);
    try {
      await Promise.all(sections.map((section, index) =>
        api.put(`/api/papers/${selectedPaper.id}/sections/${section.id}`, null, { params: { order: index } })
      ));
      await loadSections(selectedPaper.id);
    } catch (err) {
      alert(err?.response?.data?.message || t.reorderSectionsFailed);
      await loadSections(selectedPaper.id);
    } finally {
      setSectionStructureSaving(false);
    }
  };

  const handleAddSection = async () => {
    if (!selectedPaper) return;
    setSectionStructureSaving(true);
    try {
      await api.post(`/api/papers/${selectedPaper.id}/sections/create`, null, {
        params: { title: t.newSectionTitle },
      });
      await loadSections(selectedPaper.id);
    } catch (err) {
      alert(err?.response?.data?.message || t.addSectionFailed);
    } finally {
      setSectionStructureSaving(false);
    }
  };

  const handleStartSectionRename = (section) => {
    setEditingSectionId(section.id);
    setEditingSectionTitle(section.sectionTitle);
  };

  const handleSaveSectionRename = async (sectionId) => {
    if (!editingSectionTitle.trim() || !selectedPaper) return;
    setSectionStructureSaving(true);
    try {
      await api.put(`/api/papers/${selectedPaper.id}/sections/${sectionId}`, null, {
        params: { title: editingSectionTitle.trim() },
      });
      setEditingSectionId(null);
      await loadSections(selectedPaper.id);
    } catch (err) {
      alert(err?.response?.data?.message || t.renameSectionFailed);
    } finally {
      setSectionStructureSaving(false);
    }
  };

  const handleDeleteSection = async (sectionId) => {
    if (!selectedPaper || !confirm(t.deleteSectionConfirm)) return;
    setSectionStructureSaving(true);
    try {
      await api.delete(`/api/papers/${selectedPaper.id}/sections/${sectionId}`);
      await loadSections(selectedPaper.id);
    } catch (err) {
      alert(err?.response?.data?.message || t.deleteSectionFailed);
    } finally {
      setSectionStructureSaving(false);
    }
  };

  const handleAssignSection = async (sectionId, userId) => {
    const section = sections.find(s => s.id === sectionId);
    if (!userId) return handleConfirmAssign(null, sectionId);
    if (!section?.assignedUserId) {
      const member = projectMembers.find(m => m.userId === userId);
      setPendingAssign({ sectionId, userId, userName: displayName(member) });
      return;
    }
    handleConfirmAssign(userId, sectionId);
  };

  const handleConfirmAssign = async (userId, sectionId) => {
    setPendingAssign(null);
    try {
      await api.put(`/api/papers/${selectedPaper.id}/sections/${sectionId}/assign`, null, { params: { assignedUserId: userId || undefined } });
      await loadSections(selectedPaper.id);
    } catch { alert(t.assignmentFailed); }
  };

  const handleAddMember = async () => {
    if (!newMemberId) return;
    try {
      await api.post(`/api/projects/${id}/members`, null, { params: { userId: newMemberId, role: newMemberRole } });
      setShowAddMember(false);
      setNewMemberId('');
      setNewMemberRole('MEMBER');
      loadProject();
    } catch { alert(t.addMemberFailed); }
  };

  const handleRemoveMember = async (userId) => {
    try {
      await api.delete(`/api/projects/${id}/members/${userId}`);
      loadProject();
    } catch { alert(t.removeMemberFailed); }
  };

  const handlePatch = async (action) => {
    try {
      await api.patch(`/api/projects/${id}/${action}`);
      loadProject();
    } catch { alert(t.projectActionFailed.replace('{{action}}', t[action] || action)); }
  };

  const TOUR_STEPS = [
    { element: '#project-header', popover: { title: t.tourProjectTitle, description: t.tourProjectDesc, side: 'bottom', align: 'start' } },
    { element: '#tab-setup', popover: { title: t.projectSetup, description: t.tourSetupDesc, side: 'bottom', align: 'center' } },
    { element: '#tab-sections', popover: { title: t.projectSections, description: t.tourSectionsDesc, side: 'bottom', align: 'center' } },
    { element: '#tab-review', popover: { title: t.projectReview, description: t.tourProjectReviewDesc, side: 'bottom', align: 'center' } },
    { element: '#tab-settings', popover: { title: t.projectSettings, description: t.tourProjectSettingsDesc, side: 'bottom', align: 'center' } },
    { element: '#source-documents', popover: { title: t.sourceDocuments, description: t.tourSourceDocumentsDesc, side: 'top', align: 'start' } },
    { element: '#set-up-paper', popover: { title: t.setUpPaper, description: t.tourSetUpPaperDesc, side: 'top', align: 'start' } },
    { element: '#project-members', popover: { title: t.members, description: t.tourMembersDesc, side: 'top', align: 'start' } },
    { element: '#status-controls', popover: { title: ct.status, description: t.tourStatusControlsDesc, side: 'top', align: 'start' } },
  ];

  useEffect(() => {
    if (papers.length > 0 && !selectedPaper) {
      setSelectedPaper(papers[0]);
    }
  }, [papers]);

  useEffect(() => {
    if (selectedPaper) loadSections(selectedPaper.id);
  }, [selectedPaper]);

  useEffect(() => {
    if (!selectedPaper) return;
    const status = selectedPaper.processingStatus;
    if (status === 'READY' || status === 'FAILED' || !status) return;
    const interval = setInterval(async () => {
      try {
        const res = await api.get(`/api/papers/${selectedPaper.id}`);
        if (res.data.processingStatus === 'READY') {
          clearInterval(interval);
          setUploadState(null);
          loadSections(selectedPaper.id);
          loadPapers();
        }
      } catch { clearInterval(interval); }
    }, 3000);
    return () => clearInterval(interval);
  }, [selectedPaper?.id, selectedPaper?.processingStatus]);

  if (loading) return <div className="min-h-screen bg-[var(--page-bg)]"><AppHeader /><div className="mx-auto max-w-6xl p-4 sm:p-6 lg:p-8"><LoadingSkeleton count={6} /></div></div>;
  if (!project) return null;

  const projectMembers = members;
  const displayName = m => [m.firstName, m.lastName].filter(Boolean).join(' ') || m.email || m.userId?.slice(0, 8);
  const hasAssignedSections = sections.some(s => s.assignedUserId);
  const projectReadOnly = ['SUBMITTED_FOR_REVIEW', 'APPROVED', 'ARCHIVED'].includes(project.status);
  const sectionStructureLocked = hasAssignedSections || projectReadOnly;

  return (
    <div className="min-h-screen overflow-x-hidden bg-[var(--page-bg)] text-[var(--text-primary)] font-sans">
      <AppHeader />
      <main className="mx-auto max-w-6xl p-4 sm:p-6 lg:p-8">
        <div id="project-header" className="mb-6">
          <Link to="/instructor/projects" className="text-xs font-bold text-[var(--text-secondary)] transition-colors hover:text-[var(--brand-foreground)]">&larr; {ct.back}</Link>
          <div className="mt-2 flex flex-wrap items-start justify-between gap-3">
            <div className="min-w-0 flex-1">
              <h1 className="break-words text-2xl font-black text-[var(--brand-foreground)]">{project.title}</h1>
              {project.description && <p className="mt-1 text-sm text-[var(--text-secondary)]">{project.description}</p>}
              <p className="mt-1 flex flex-wrap items-center gap-1 text-xs text-[var(--text-tertiary)]">ID: {project.id} <span aria-hidden="true">&middot;</span> <StatusBadge status={project.status} /></p>
            </div>
            <div className="flex shrink-0 items-center gap-2">
              <button onClick={() => setShowExportModal(true)} className="rounded-lg bg-[var(--brand)] px-3 py-2 text-xs font-bold text-white transition hover:bg-[var(--brand-hover)]">{t.export}</button>
              <TourLauncher steps={TOUR_STEPS} tourKey="instructor-project-detail"
                className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full border border-[var(--border)] bg-[var(--surface)] text-sm font-bold text-[var(--text-secondary)] shadow-sm transition-all hover:border-indigo-300 hover:bg-[var(--brand-soft)] hover:text-[var(--brand-foreground)]" />
            </div>
          </div>
        </div>

        {/* Tabs */}
        <div className="mb-6 flex max-w-full gap-1 overflow-x-auto border-b border-[var(--border)]">
          {[
            { key: 'setup', label: t.projectSetup },
            { key: 'sections', label: t.projectSections },
            { key: 'review', label: t.projectReview },
            { key: 'progress', label: t.projectProgressReport },
            { key: 'settings', label: t.projectSettings },
          ].map(tab => (
            <button
              key={tab.key}
              id={`tab-${tab.key}`}
              onClick={() => setActiveTab(tab.key)}
              className={`-mb-px shrink-0 rounded-t-lg px-4 py-2 text-xs font-bold transition ${activeTab === tab.key ? 'border border-b-[var(--surface)] border-[var(--border)] bg-[var(--surface)] text-[var(--brand-foreground)]' : 'text-[var(--text-secondary)] hover:text-[var(--text-primary)]'
                }`}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {/* Tab: Setup */}
        {activeTab === 'setup' && (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div id="source-documents" className="rounded-2xl border border-[var(--border)] bg-[var(--surface)] p-4 shadow-sm sm:p-6">
              <div className="mb-4 flex flex-wrap items-center justify-between gap-2">
                <h2 className="text-sm font-bold text-[var(--brand-foreground)]">{t.sourceDocuments}</h2>
                <button onClick={() => setShowAddSource(true)} className="rounded-lg bg-[var(--brand)] px-3 py-2 text-xs font-bold text-white hover:bg-[var(--brand-hover)]">+ {t.addSource}</button>
              </div>
              {sources.length === 0 ? (
                <p className="text-xs italic text-[var(--text-tertiary)]">{t.noSourceDocuments}</p>
              ) : (
                <div className="space-y-1">
                  {sources.map(s => (
                    <button key={s.id} onClick={() => { setSourceDetail(s); setShowSourceDetail(true); }} className="flex w-full items-center justify-between gap-2 rounded-lg bg-[var(--surface-secondary)] px-3 py-2 text-left text-xs transition hover:bg-[var(--surface-tertiary)]">
                      <span className="min-w-0 truncate font-medium">{s.title || s.originalFilename || s.id}</span>
                      <span className="flex items-center gap-2">
                        <StatusBadge status={s.processingStatus || 'READY'} />
                      </span>
                    </button>
                  ))}
                </div>
              )}
            </div>
            <div id="set-up-paper" className="rounded-2xl border border-[var(--border)] bg-[var(--surface)] p-4 shadow-sm sm:p-6">
              <h2 className="mb-4 text-sm font-bold text-[var(--brand-foreground)]">{t.setUpPaper}</h2>
              {standard && (
                <div className="mb-3 flex items-center justify-between gap-2 rounded-lg bg-[var(--brand-soft)] px-3 py-2 text-xs">
                  <span className="font-medium text-[var(--brand-foreground)]">{t.standardLabel.replace('{{standard}}', standard)}</span>
                  <button onClick={() => { setSetupMode('standard'); setShowSetUpPaper(true); }} className="text-xs font-bold text-[var(--brand-foreground)] hover:underline">{t.change}</button>
                </div>
              )}
              {papers.length > 0 && (
                <div className="mb-3 space-y-1">
                  <p className="text-[10px] font-bold uppercase tracking-wider text-[var(--text-tertiary)]">{t.uploadedPapers}</p>
                  {papers.map(p => (
                    <div key={p.id} className="flex items-center justify-between gap-2 rounded-lg bg-[var(--surface-secondary)] px-3 py-2 text-xs">
                      <span className="min-w-0 truncate font-medium">{p.originalFilename || p.title}</span>
                      <StatusBadge status={p.processingStatus || 'READY'} />
                    </div>
                  ))}
                </div>
              )}
              {!standard && papers.length === 0 && (
                <p className="mb-3 text-xs italic text-[var(--text-tertiary)]">{t.noPaperConfigured}</p>
              )}
              {sectionStructureLocked ? (
                <div className="flex w-full items-center justify-center gap-2 rounded-lg bg-[var(--surface-tertiary)] px-4 py-2 text-center text-xs font-bold text-[var(--text-secondary)]">
                  <svg aria-hidden="true" viewBox="0 0 24 24" className="h-4 w-4 fill-none stroke-current" strokeWidth="2"><rect x="5" y="10" width="14" height="10" rx="2" /><path d="M8 10V7a4 4 0 0 1 8 0v3" /></svg>
                  {projectReadOnly ? t.setupLockedReadOnly : t.setupLockedAssigned}
                </div>
              ) : (
                <button onClick={() => { setSetupMode(standard ? 'standard' : 'paper'); setShowSetUpPaper(true); }} className="w-full rounded-lg bg-[var(--brand)] px-4 py-2 text-xs font-bold text-white hover:bg-[var(--brand-hover)]">
                  {standard || papers.length > 0 ? t.updateSetup : t.setUpPaper}
                </button>
              )}
            </div>
          </div>
        )}

        {/* Tab: Sections */}
        {activeTab === 'sections' && (
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <div className="rounded-2xl border border-[var(--border)] bg-[var(--surface)] p-4 shadow-sm sm:p-6 lg:col-span-1">
              <div className="flex justify-between items-center mb-4">
                <h2 className="text-sm font-bold text-[var(--brand-foreground)]">{t.papers}</h2>
              </div>
              {papers.length === 0 ? (
                <p className="text-xs italic text-[var(--text-tertiary)]">{t.uploadPaperFirst}</p>
              ) : (
                <div className="space-y-1">
                  {papers.map(p => (
                    <div key={p.id} className="flex items-center gap-1">
                      {editingPaperId === p.id ? (
                        <div className="flex flex-1 items-center gap-1 rounded-lg border border-indigo-200 bg-[var(--brand-soft)] px-3 py-2">
                          <input autoFocus value={editingPaperTitle} onChange={e => setEditingPaperTitle(e.target.value)} onKeyDown={e => { if (e.key === 'Enter') handleSaveRename(p.id); if (e.key === 'Escape') setEditingPaperId(null); }} className="min-w-0 flex-1 border-b border-indigo-300 bg-transparent text-xs outline-none" onClick={e => e.stopPropagation()} />
                          <button onClick={() => handleSaveRename(p.id)} className="rounded p-1 text-emerald-600 hover:bg-emerald-50 hover:text-emerald-800" title={ct.save} aria-label={ct.save}><svg aria-hidden="true" viewBox="0 0 24 24" className="h-4 w-4 fill-none stroke-current" strokeWidth="2"><path d="m5 12 4 4L19 6" /></svg></button>
                          <button onClick={() => setEditingPaperId(null)} className="rounded p-1 text-[var(--text-tertiary)] hover:bg-[var(--surface-tertiary)] hover:text-[var(--text-primary)]" title={ct.cancel} aria-label={ct.cancel}><svg aria-hidden="true" viewBox="0 0 24 24" className="h-4 w-4 fill-none stroke-current" strokeWidth="2"><path d="M6 6l12 12M18 6 6 18" /></svg></button>
                        </div>
                      ) : (
                        <button
                          onClick={() => { setSelectedPaper(p); loadSections(p.id); }}
                          className={`min-w-0 flex-1 rounded-lg px-3 py-2 text-left text-xs transition ${selectedPaper?.id === p.id ? 'border border-indigo-200 bg-[var(--brand-soft)] text-[var(--brand-foreground)]' : 'hover:bg-[var(--surface-secondary)]'}`}
                        >
                          <span className="font-medium">{p.originalFilename || p.title}</span>
                        </button>
                      )}
                      {editingPaperId !== p.id && (
                        <button onClick={e => { e.stopPropagation(); handleStartRename(p); }} className="rounded p-1 text-[var(--text-tertiary)] hover:bg-[var(--brand-soft)] hover:text-[var(--brand-foreground)]" title={t.rename} aria-label={t.rename}><svg aria-hidden="true" viewBox="0 0 24 24" className="h-4 w-4 fill-none stroke-current" strokeWidth="2"><path d="m4 16-1 5 5-1L19 9l-4-4L4 16Z" /><path d="m13 7 4 4" /></svg></button>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>
            <div className="rounded-2xl border border-[var(--border)] bg-[var(--surface)] p-4 shadow-sm sm:p-6 lg:col-span-2">
              <div className="mb-4 flex flex-wrap items-start justify-between gap-3">
                <div>
                  <h2 className="text-sm font-bold text-[var(--brand-foreground)]">{t.projectSections}</h2>
                  {selectedPaper && sectionStructureLocked && (
                    <p className="text-[10px] text-amber-700 mt-1">
                      {projectReadOnly ? t.projectReadOnly : t.sectionStructureLocked}
                    </p>
                  )}
                </div>
                <div className="flex gap-2">
                  {selectedPaper && (
                    <button
                      onClick={handleAddSection}
                      disabled={sectionStructureLocked || sectionStructureSaving
                        || selectedPaper.processingStatus === 'QUEUED'
                        || selectedPaper.processingStatus === 'PROCESSING'}
                      className="rounded-lg bg-[var(--brand)] px-3 py-2 text-xs font-bold text-white hover:bg-[var(--brand-hover)] disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      + {t.addSection}
                    </button>
                  )}
                  {selectedPaper && orderDirty && (
                    <button
                      onClick={handleSaveSectionOrder}
                      disabled={sectionStructureLocked || sectionStructureSaving}
                      className="px-3 py-1.5 bg-amber-500 text-white text-xs font-bold rounded-lg hover:bg-amber-600 disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      Save Change
                    </button>
                  )}
                  {selectedPaper && (
                    <button onClick={async () => {
                      try {
                        const res = await api.get(`/api/papers/${selectedPaper.id}/validate`);
                        alert(JSON.stringify(res.data, null, 2));
                      } catch { }
                    }} className="rounded-lg bg-amber-600 px-3 py-2 text-xs font-bold text-white hover:bg-amber-700">{t.validate}</button>
                  )}
                </div>
              </div>
              {!selectedPaper ? (
                <p className="text-xs italic text-[var(--text-tertiary)]">{t.selectPaperSections}</p>
              ) : selectedPaper.processingStatus === 'PROCESSING' || selectedPaper.processingStatus === 'QUEUED' ? (
                <div className="flex items-center gap-2 text-xs italic text-[var(--text-secondary)]">
                  <span className="inline-block w-2 h-2 bg-amber-400 rounded-full animate-pulse"></span>
                  {t.processingSections}
                </div>
              ) : sections.length === 0 ? (
                <div className="text-xs italic text-[var(--text-tertiary)]">
                  <p>{t.noSectionsHelp}</p>
                </div>
              ) : (
                <DragDropContext onDragEnd={handleDragEnd}>
                  <Droppable droppableId="sections">
                    {(provided) => (
                      <div ref={provided.innerRef} {...provided.droppableProps} className="space-y-2 max-h-[60vh] overflow-y-auto custom-scrollbar pr-1">
                        {sections.map((s, index) => (
                          <Draggable
                            key={s.id}
                            draggableId={String(s.id)}
                            index={index}
                            isDragDisabled={sectionStructureLocked || sectionStructureSaving}
                          >
                            {(dragProvided, snapshot) => (
                              <div
                                ref={dragProvided.innerRef}
                                {...dragProvided.draggableProps}
                                className={`flex items-center justify-between gap-3 rounded-lg px-3 py-3 text-xs sm:px-4 ${
                                  snapshot.isDragging ? 'border border-indigo-200 bg-[var(--brand-soft)] shadow-lg' : 'bg-[var(--surface-secondary)]'
                                }`}
                              >
                                <div className="flex items-center gap-3 min-w-0">
                                  <span
                                    {...dragProvided.dragHandleProps}
                                    className={`text-[var(--text-tertiary)] ${sectionStructureLocked ? 'cursor-not-allowed' : 'cursor-grab active:cursor-grabbing'}`}
                                    title={sectionStructureLocked ? t.unassignToReorder : t.dragToReorder}
                                  >
                                    {'\u283F'}
                                  </span>
                                  {editingSectionId === s.id ? (
                                    <div className="flex items-center gap-1">
                                      <input
                                        autoFocus
                                        value={editingSectionTitle}
                                        onChange={e => setEditingSectionTitle(e.target.value)}
                                        onKeyDown={e => {
                                          if (e.key === 'Enter') handleSaveSectionRename(s.id);
                                          if (e.key === 'Escape') setEditingSectionId(null);
                                        }}
                                        className="bg-transparent outline-none border-b border-indigo-300 text-xs"
                                      />
                                      <button onClick={() => handleSaveSectionRename(s.id)} disabled={sectionStructureSaving} className="rounded p-1 text-emerald-600 hover:bg-emerald-50 hover:text-emerald-800 disabled:opacity-50" title={ct.save} aria-label={ct.save}><svg aria-hidden="true" viewBox="0 0 24 24" className="h-4 w-4 fill-none stroke-current" strokeWidth="2"><path d="m5 12 4 4L19 6" /></svg></button>
                                      <button onClick={() => setEditingSectionId(null)} className="rounded p-1 text-[var(--text-tertiary)] hover:bg-[var(--surface-tertiary)] hover:text-[var(--text-primary)]" title={ct.cancel} aria-label={ct.cancel}><svg aria-hidden="true" viewBox="0 0 24 24" className="h-4 w-4 fill-none stroke-current" strokeWidth="2"><path d="M6 6l12 12M18 6 6 18" /></svg></button>
                                    </div>
                                  ) : (
                                    <span className="font-medium truncate">{s.sectionTitle}</span>
                                  )}
                                  {s.version > 1 && <span className="text-[9px] bg-amber-100 text-amber-700 px-1.5 py-0.5 rounded font-bold">v{s.version}</span>}
                                  {s.assignedUserId && (
                                    <span className="flex items-center gap-1 rounded bg-[var(--surface-tertiary)] px-1.5 py-0.5 text-[9px] font-bold text-[var(--text-secondary)]">
                                      <svg aria-hidden="true" viewBox="0 0 24 24" className="h-3 w-3 fill-none stroke-current" strokeWidth="2"><rect x="5" y="10" width="14" height="10" rx="2" /><path d="M8 10V7a4 4 0 0 1 8 0v3" /></svg>
                                      {displayName(projectMembers.find(m => m.userId === s.assignedUserId))}
                                    </span>
                                  )}
                                </div>
                                <div className="flex items-center gap-2">
                                  {!sectionStructureLocked && editingSectionId !== s.id && (
                                    <button onClick={() => handleStartSectionRename(s)} disabled={sectionStructureSaving} className="rounded p-1 text-[var(--text-tertiary)] hover:bg-[var(--brand-soft)] hover:text-[var(--brand-foreground)] disabled:opacity-50" title={t.rename} aria-label={t.rename}><svg aria-hidden="true" viewBox="0 0 24 24" className="h-4 w-4 fill-none stroke-current" strokeWidth="2"><path d="m4 16-1 5 5-1L19 9l-4-4L4 16Z" /><path d="m13 7 4 4" /></svg></button>
                                  )}
                                  {!sectionStructureLocked && (
                                    <button onClick={() => handleDeleteSection(s.id)} disabled={sectionStructureSaving} className="rounded p-1 text-[var(--text-tertiary)] hover:bg-rose-50 hover:text-rose-600 disabled:opacity-50" title={ct.delete} aria-label={ct.delete}><svg aria-hidden="true" viewBox="0 0 24 24" className="h-4 w-4 fill-none stroke-current" strokeWidth="2"><path d="M3 6h18M8 6V4h8v2m-9 0 1 14h8l1-14M10 10v6M14 10v6" /></svg></button>
                                  )}
                                  <select
                                    value={s.assignedUserId || ''}
                                    onChange={e => handleAssignSection(s.id, e.target.value)}
                                    disabled={projectReadOnly || sectionStructureSaving}
                                    className="max-w-36 rounded border border-[var(--border)] bg-[var(--surface)] px-2 py-1 text-xs outline-none disabled:bg-[var(--surface-tertiary)] disabled:text-[var(--text-tertiary)] sm:max-w-none"
                                  >
                                    <option value="">{t.unassigned}</option>
                                    {projectMembers
                                      .filter(member => users.some(user => String(user.id) === String(member.userId)))
                                      .map(member => (
                                        <option key={member.id} value={member.userId}>{displayName(member)}</option>
                                      ))}
                                  </select>
                                </div>
                              </div>
                            )}
                          </Draggable>
                        ))}
                        {provided.placeholder}
                      </div>
                    )}
                  </Droppable>
                </DragDropContext>
              )}
            </div>
          </div>
        )}

        {/* Tab: Review */}
        {activeTab === 'review' && (
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <div className="rounded-2xl border border-[var(--border)] bg-[var(--surface)] p-4 shadow-sm sm:p-6">
              <h2 className="mb-4 text-sm font-bold text-[var(--brand-foreground)]">{t.feedbackRequests}</h2>
              {feedbackRequests.length === 0 ? (
                <p className="text-xs italic text-[var(--text-tertiary)]">{t.noReviewRequests}</p>
              ) : (
                <div className="space-y-2">
                  {feedbackRequests.map(fb => (
                    <div key={fb.id} className="rounded-lg bg-[var(--surface-secondary)] px-3 py-2 text-xs">
                      <div className="flex justify-between items-center">
                        <StatusBadge status={fb.status} />
                        <span className="text-[var(--text-tertiary)]">{fb.requestedAt ? new Date(fb.requestedAt).toLocaleDateString(language === 'vi' ? 'vi-VN' : 'en-US') : ''}</span>
                      </div>
                      <p className="mt-1 text-[var(--text-secondary)]">{t.studentLabel.replace('{{student}}', fb.studentName || fb.studentId)}</p>
                    </div>
                  ))}
                </div>
              )}
            </div>
            <div className="rounded-2xl border border-[var(--border)] bg-[var(--surface)] p-4 shadow-sm sm:p-6">
              <div className="mb-4 flex flex-wrap items-center justify-between gap-2">
                <h2 className="text-sm font-bold text-[var(--brand-foreground)]">
                  {reviewPane === 'distribution' ? t.claimTypeDistribution : t.evidenceMap}
                </h2>
                <div className="flex overflow-hidden rounded-lg border border-[var(--border)]">
                  {['distribution', 'map'].map(mode => (
                    <button key={mode} onClick={() => setReviewPane(mode)}
                      className={`px-3 py-2 text-xs font-bold transition ${reviewPane === mode ? 'bg-[var(--brand)] text-white' : 'bg-[var(--surface)] text-[var(--text-secondary)] hover:text-[var(--brand-foreground)]'}`}>
                      {mode === 'distribution' ? t.distribution : t.evidenceMap}
                    </button>
                  ))}
                </div>
              </div>
              {reviewPane === 'distribution'
                ? <FunctionalTypeRadar stats={claimStats} />
                : <EvidenceGraph traceabilityData={traceability} height={500} />}
            </div>
          </div>
        )}

        {/* Tab: Project Process Report */}
        {activeTab === 'progress' && (
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {progressReport?.readiness && (
              <div className="rounded-2xl border border-[var(--border)] bg-[var(--surface)] p-4 shadow-sm sm:p-6 lg:col-span-3">
                <h2 className="mb-4 text-sm font-bold text-[var(--brand-foreground)]">{t.readiness}</h2>
                <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
                  {[
                    { label: t.overallScore, value: `${progressReport.readiness.score} / 100` },
                    { label: t.contentCoverage, value: `${progressReport.readiness.contentCoveragePercent}%` },
                    { label: t.claimsPresent, value: `${progressReport.readiness.claimsPresentPercent}%` },
                    { label: t.claimsWithEvidence, value: `${progressReport.readiness.claimsWithEvidencePercent}%` },
                  ].map(stat => (
                    <div key={stat.label} className="rounded-xl bg-[var(--surface-secondary)] p-4 text-center">
                      <p className="text-2xl font-black text-[var(--brand-foreground)]">{stat.value}</p>
                      <p className="mt-1 text-[10px] font-bold uppercase tracking-wider text-[var(--text-tertiary)]">{stat.label}</p>
                    </div>
                  ))}
                </div>
                {progressReport.readiness.metrics?.length > 0 && (
                  <div className="mt-4 space-y-2">
                    {progressReport.readiness.metrics.map(metric => (
                      <div key={metric.code} className="flex items-center gap-3">
                        <span className="w-40 text-[10px] font-bold uppercase tracking-wider text-[var(--text-secondary)]">
                          {metric.label} <span className="text-[var(--text-tertiary)]">({metric.weightPercent}%)</span>
                        </span>
                        <div className="h-2 flex-1 overflow-hidden rounded-full bg-[var(--surface-tertiary)]">
                          <div className="h-full rounded-full bg-[var(--brand)]" style={{ width: `${metric.valuePercent}%` }} />
                        </div>
                        <span className="w-10 text-right text-xs font-bold text-[var(--brand-foreground)]">{metric.valuePercent}%</span>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}

            <div className="rounded-2xl border border-[var(--border)] bg-[var(--surface)] p-4 shadow-sm sm:p-6 lg:col-span-2">
              <div className="mb-4 flex flex-wrap items-center justify-between gap-2">
                <h2 className="text-sm font-bold text-[var(--brand-foreground)]">
                  {reportPane === 'matrix' ? t.claimMatrix : t.changesSinceCheckpoint}
                  {reportSectionId && <span className="ml-2 rounded bg-[var(--brand-soft)] px-1.5 py-0.5 text-[10px] font-bold text-[var(--brand-foreground)]">{t.filteredBySection}</span>}
                </h2>
                <div className="flex overflow-hidden rounded-lg border border-[var(--border)]">
                  {['matrix', 'diff'].map(mode => (
                    <button key={mode} onClick={() => setReportPane(mode)}
                      className={`px-3 py-2 text-xs font-bold transition ${reportPane === mode ? 'bg-[var(--brand)] text-white' : 'bg-[var(--surface)] text-[var(--text-secondary)] hover:text-[var(--brand-foreground)]'}`}>
                      {mode === 'matrix' ? t.claimMatrix : t.checkpointDiff}
                    </button>
                  ))}
                </div>
              </div>
              {reportPane === 'matrix' ? (
                !progressReport ? <p className="text-xs italic text-[var(--text-tertiary)]">{ct.loading}</p> : sectionMatrix.length === 0 ? (
                  <p className="text-xs italic text-[var(--text-tertiary)]">{reportSectionId ? t.noClaimsInSection : t.noClaimsYet}</p>
                ) : (
                  <div className="overflow-x-auto">
                    <table className="w-full text-xs">
                      <thead>
                        <tr className="border-b border-[var(--border)] text-left text-[10px] font-bold uppercase tracking-wider text-[var(--text-tertiary)]">
                          <th className="py-2 pr-3">{t.claim}</th>
                          <th className="py-2 pr-3">{t.section}</th>
                          <th className="py-2 pr-3">{ct.status}</th>
                          <th className="py-2 pr-3">{t.evidence}</th>
                          <th className="py-2 pr-3">{t.strongestMatch}</th>
                          <th className="py-2">{t.author}</th>
                        </tr>
                      </thead>
                      <tbody>
                        {sectionMatrix.map(row => (
                          <tr key={row.claimId} className="border-b border-[var(--border-light)]">
                            <td className="max-w-[220px] py-2 pr-3 text-[var(--text-primary)]"><span className="line-clamp-2">{row.content}</span></td>
                            <td className="py-2 pr-3 text-[var(--text-secondary)]">{row.sectionTitle}</td>
                            <td className="py-2 pr-3"><StatusBadge status={row.contentStatus} /></td>
                            <td className="py-2 pr-3 text-[var(--text-primary)]">{row.activeEvidenceCount}</td>
                            <td className="py-2 pr-3 text-[var(--text-primary)]">{row.strongestRelation || '-'}{row.strongestScore != null ? ` (${row.strongestScore}%)` : ''}</td>
                            <td className="py-2 text-[var(--text-secondary)]">{row.createdByName || row.createdById}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )
              ) : (
                !sectionDiff ? (
                  <p className="text-xs italic text-[var(--text-tertiary)]">{t.noCheckpointComparison}</p>
                ) : (
                  <div className="space-y-4 text-xs">
                    <div className="flex flex-wrap gap-4 text-[var(--text-secondary)]">
                      <span>{t.fromLabel}: {sectionDiff.from ? new Date(sectionDiff.from).toLocaleString(language === 'vi' ? 'vi-VN' : 'en-US') : t.startLabel} ({sectionDiff.fromTrigger || t.initialLabel})</span>
                      <span>{t.toLabel}: {sectionDiff.to ? new Date(sectionDiff.to).toLocaleString(language === 'vi' ? 'vi-VN' : 'en-US') : t.nowLabel} ({sectionDiff.toTrigger || t.latestLabel})</span>
                    </div>
                    <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
                      {[
                        { label: t.claimsAdded, value: (sectionDiff.claimsAdded || []).length, color: 'text-emerald-600' },
                        { label: t.claimsRemoved, value: (sectionDiff.claimsRemoved || []).length, color: 'text-rose-600' },
                        { label: t.claimsChanged, value: (sectionDiff.claimsChanged || []).length, color: 'text-amber-600' },
                        { label: t.wordCountDelta, value: (sectionDiff.sectionWordDeltas || []).reduce((sum, d) => sum + (d.toWords - d.fromWords), 0), color: 'text-[var(--brand-foreground)]' },
                      ].map(stat => (
                        <div key={stat.label} className="rounded-xl bg-[var(--surface-secondary)] p-4 text-center">
                          <p className={`text-2xl font-black ${stat.color}`}>{stat.value}</p>
                          <p className="mt-1 text-[10px] font-bold uppercase tracking-wider text-[var(--text-tertiary)]">{stat.label}</p>
                        </div>
                      ))}
                    </div>
                    {[
                      { label: t.mappingsAccepted, value: sectionDiff.mappingsAcceptedDelta },
                      { label: t.mappingsRejected, value: sectionDiff.mappingsRejectedDelta },
                      { label: t.feedbackAnswered, value: sectionDiff.feedbackAnsweredDelta },
                    ].map(item => (
                      <div key={item.label} className="flex items-center justify-between rounded-lg bg-[var(--surface-secondary)] px-3 py-2">
                        <span className="text-[var(--text-secondary)]">{item.label}</span>
                        <span className={`font-black ${item.value > 0 ? 'text-emerald-600' : item.value < 0 ? 'text-rose-600' : 'text-[var(--text-tertiary)]'}`}>{item.value > 0 ? `+${item.value}` : item.value}</span>
                      </div>
                    ))}
                    {(sectionDiff.sectionWordDeltas || []).filter(d => d.toWords !== d.fromWords).length > 0 && (
                      <div>
                        <p className="mb-2 text-[10px] font-bold uppercase tracking-wider text-[var(--text-tertiary)]">{t.wordCountBySection}</p>
                        <div className="space-y-1">
                          {(sectionDiff.sectionWordDeltas || []).filter(d => d.toWords !== d.fromWords).map(d => (
                            <div key={d.sectionId} className="flex items-center justify-between rounded-lg bg-[var(--surface-secondary)] px-3 py-1.5 text-[10px]">
                              <span className="text-[var(--text-secondary)]">{t.section} {String(d.sectionId).slice(0, 8)}</span>
                              <span className="text-[var(--text-primary)]">{d.fromWords} → {d.toWords}</span>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}
                    {(sectionDiff.claimsChanged || []).length > 0 && (
                      <div>
                        <p className="mb-2 text-[10px] font-bold uppercase tracking-wider text-[var(--text-tertiary)]">{t.changedClaims}</p>
                        <div className="space-y-1">
                          {(sectionDiff.claimsChanged || []).map(claim => (
                            <div key={claim.id} className="flex items-center justify-between rounded-lg bg-[var(--surface-secondary)] px-3 py-1.5 text-[10px]">
                              <span className="font-mono text-[var(--text-secondary)]">#{String(claim.id).slice(0, 8)}</span>
                              <span className="text-[var(--text-primary)]">{claim.version > 1 ? `v${claim.version - 1}` : t.newLabel} → v{claim.version}</span>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>
                )
              )}
            </div>

            <div className="rounded-2xl border border-[var(--border)] bg-[var(--surface)] p-4 shadow-sm sm:p-6 lg:col-span-1">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-sm font-bold text-[var(--brand-foreground)]">{t.projectSections}</h2>
                {reportSectionId && (
                  <button onClick={() => setReportSectionId(null)} className="text-xs font-bold text-[var(--brand-foreground)] hover:underline">{t.allSections}</button>
                )}
              </div>
              {!progressReport ? <p className="text-xs italic text-[var(--text-tertiary)]">{ct.loading}</p> : progressReport.sections?.length === 0 ? (
                <p className="text-xs italic text-[var(--text-tertiary)]">{t.noSectionsYet}</p>
              ) : (
                <div className="space-y-3 max-h-96 overflow-y-auto pr-1">
                  {(progressReport.sections || []).map(section => (
                    <button
                      key={section.sectionId}
                      onClick={() => setReportSectionId(String(reportSectionId) === String(section.sectionId) ? null : section.sectionId)}
                      className={`w-full rounded-xl bg-[var(--surface-secondary)] p-3 text-left text-xs transition ${String(reportSectionId) === String(section.sectionId) ? 'bg-[var(--brand-soft)] ring-2 ring-indigo-500/40' : 'hover:bg-[var(--brand-soft)]'}`}
                    >
                      <div className="flex justify-between items-start gap-2">
                        <span className="font-bold text-[var(--text-primary)]">{section.sectionTitle}</span>
                        <span className="rounded bg-[var(--brand-soft)] px-1.5 py-0.5 text-[9px] font-black text-[var(--brand-foreground)]">v{section.version}</span>
                      </div>
                      <p className="mt-1 text-[10px] text-[var(--text-tertiary)]">
                        {t.sectionSummary.replace('{{words}}', section.wordCount).replace('{{claims}}', section.claimCount)}{section.assignedUserName ? ` · ${section.assignedUserName}` : ''}
                      </p>
                      <p className="mt-0.5 text-[10px] text-[var(--text-tertiary)]">
                        {t.feedbackSummary.replace('{{answered}}', section.feedbackAnswered).replace('{{total}}', section.feedbackAnswered + section.feedbackUnanswered)}
                        {section.lastUpdated ? ` · ${new Date(section.lastUpdated).toLocaleDateString(language === 'vi' ? 'vi-VN' : 'en-US')}` : ''}
                      </p>
                    </button>
                  ))}
                </div>
              )}
            </div>
          </div>
        )}

        {/* Tab: Settings */}
        {activeTab === 'settings' && (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div id="status-controls" className="rounded-2xl border border-[var(--border)] bg-[var(--surface)] p-4 shadow-sm sm:p-6">
              <h2 className="mb-4 text-sm font-bold text-[var(--brand-foreground)]">{t.statusControls}</h2>
              <div className="space-y-3">
                {project.status === 'IN_PROGRESS' && (
                  <button onClick={() => handlePatch('complete')} className="w-full rounded-lg bg-[var(--brand)] px-4 py-2 text-xs font-bold text-white hover:bg-[var(--brand-hover)]">{t.markComplete}</button>
                )}
                {project.status !== 'ARCHIVED' && (
                  <button onClick={() => handlePatch('archive')} className="w-full rounded-lg bg-amber-600 px-4 py-2 text-xs font-bold text-white hover:bg-amber-700">{t.archive}</button>
                )}
                {project.status === 'ARCHIVED' && (
                  <button onClick={() => handlePatch('unarchive')} className="w-full rounded-lg bg-emerald-600 px-4 py-2 text-xs font-bold text-white hover:bg-emerald-700">{t.unarchive}</button>
                )}
                <p className="text-[10px] text-[var(--text-tertiary)]">{t.currentStatus} <StatusBadge status={project.status} /></p>
              </div>
            </div>
            <div id="project-members" className="rounded-2xl border border-[var(--border)] bg-[var(--surface)] p-4 shadow-sm sm:p-6">
              <div className="mb-4 flex items-center justify-between gap-2">
                <h2 className="text-sm font-bold text-[var(--brand-foreground)]">{t.members}</h2>
                <button onClick={() => { setShowAddMember(true); loadUsers(); }} className="rounded-lg bg-[var(--brand)] px-3 py-2 text-xs font-bold text-white hover:bg-[var(--brand-hover)]">+ {t.add}</button>
              </div>
              {projectMembers.length === 0 ? (
                <p className="text-xs italic text-[var(--text-tertiary)]">{t.noMembers}</p>
              ) : (
                <div className="space-y-2">
                  {projectMembers.map(m => (
                    <div key={m.id} className="flex flex-wrap items-center justify-between gap-3 rounded-lg bg-[var(--surface-secondary)] px-3 py-2 text-xs">
                      <div className="min-w-0 flex-1">
                        <span className="font-medium">{displayName(m)}</span>
                        <span className="block truncate text-[var(--text-tertiary)]">{m.email}</span>
                      </div>
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="text-[10px] bg-blue-100 text-blue-700 px-1.5 py-0.5 rounded font-bold">{m.userRole}</span>
                        <span className="rounded bg-[var(--surface-tertiary)] px-1.5 py-0.5 text-[10px] text-[var(--text-secondary)]">{m.role}</span>
                        <button onClick={() => handleRemoveMember(m.userId)} className="font-bold text-rose-600 hover:text-rose-800">{t.remove}</button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        )}
      </main>

      <Modal open={showAddMember} onClose={() => setShowAddMember(false)} title={t.addMember}>
        <div className="space-y-4">
           <select value={newMemberId} onChange={e => setNewMemberId(e.target.value)} className="w-full rounded-lg border border-[var(--border)] bg-[var(--surface)] px-3 py-2 text-xs outline-none">
            <option value="">{t.selectStudent}</option>
            {users.filter(u => u.role === 'STUDENT' && !projectMembers.find(m => m.userId === u.id)).map(u => (
              <option key={u.id} value={u.id}>{u.firstName || u.lastName ? `${u.firstName || ''} ${u.lastName || ''}`.trim() : u.email}</option>
            ))}
          </select>
          <select value={newMemberRole} onChange={e => setNewMemberRole(e.target.value)} className="w-full rounded-lg border border-[var(--border)] bg-[var(--surface)] px-3 py-2 text-xs outline-none">
            <option value="MEMBER">{t.memberRole}</option>
            <option value="LEADER">{t.leaderRole}</option>
          </select>
          <div className="flex justify-end gap-2">
            <button onClick={() => setShowAddMember(false)} className="rounded-lg bg-[var(--surface-tertiary)] px-4 py-2 text-xs font-semibold text-[var(--text-secondary)] hover:opacity-80">{ct.cancel}</button>
            <button onClick={handleAddMember} disabled={!newMemberId} className="rounded-lg bg-[var(--brand)] px-4 py-2 text-xs font-bold text-white hover:bg-[var(--brand-hover)] disabled:opacity-50">{ct.save}</button>
          </div>
        </div>
      </Modal>

      <Modal open={!!pendingAssign} onClose={() => setPendingAssign(null)} title={t.assignSection}>
        <div className="space-y-4 text-xs">
          <p className="text-[var(--text-secondary)]">{t.assignSectionQuestion.replace('{{student}}', pendingAssign?.userName || '')}</p>
          <p className="text-amber-700 bg-amber-50 border border-amber-200 rounded-lg px-3 py-2">
            {t.assignSectionWarning}
          </p>
          <div className="flex justify-end gap-2">
            <button onClick={() => setPendingAssign(null)} className="rounded-lg bg-[var(--surface-tertiary)] px-4 py-2 text-xs font-semibold text-[var(--text-secondary)] hover:opacity-80">{ct.cancel}</button>
            <button onClick={() => handleConfirmAssign(pendingAssign?.userId, pendingAssign?.sectionId)} className="rounded-lg bg-[var(--brand)] px-4 py-2 text-xs font-bold text-white hover:bg-[var(--brand-hover)]">{ct.confirm}</button>
          </div>
        </div>
      </Modal>

      <Modal open={showSourceDetail} onClose={() => setShowSourceDetail(false)} title={t.sourceDetail}>
        {sourceDetail && (
          <div className="space-y-3 text-xs">
            <div><span className="font-bold text-[var(--text-secondary)]">{t.titleLabel}</span> <span>{sourceDetail.title || '-'}</span></div>
            <div><span className="font-bold text-[var(--text-secondary)]">{t.filenameLabel}</span> <span>{sourceDetail.originalFilename || '-'}</span></div>
            <div><span className="font-bold text-[var(--text-secondary)]">DOI:</span> <span className="font-mono">{sourceDetail.doi || '-'}</span></div>
            <div><span className="font-bold text-[var(--text-secondary)]">{ct.status}:</span> <StatusBadge status={sourceDetail.processingStatus || 'READY'} /></div>
            <div><span className="font-bold text-[var(--text-secondary)]">{t.typeLabel}</span> <span>{sourceDetail.docType || 'SOURCE'}</span></div>
            <div><span className="font-bold text-[var(--text-secondary)]">ID:</span> <span className="font-mono text-[9px]">{sourceDetail.id}</span></div>
            <div className="flex justify-end gap-2 pt-2">
              <button onClick={() => setShowSourceDetail(false)} className="rounded-lg bg-[var(--surface-tertiary)] px-4 py-2 text-xs font-semibold text-[var(--text-secondary)] hover:opacity-80">{ct.close}</button>
            </div>
          </div>
        )}
      </Modal>

      <Modal open={showAddSource} onClose={() => { setShowAddSource(false); setDoiInput(''); }} title={t.addSource}>
        <div className="space-y-5 text-xs">
          <div className="space-y-3 rounded-xl border border-[var(--border)] p-4">
            <h3 className="font-bold text-[var(--brand-foreground)]">{t.importByDoi}</h3>
            <div className="flex gap-2 mb-2">
              <button
                onClick={() => setAddSourceDocType('SOURCE')}
                className={`flex-1 rounded-lg border px-3 py-2 text-xs font-bold transition ${addSourceDocType === 'SOURCE' ? 'border-[var(--brand)] bg-[var(--brand)] text-white' : 'border-[var(--border)] bg-[var(--surface)] text-[var(--text-secondary)] hover:border-indigo-300'}`}
              >
                {t.asSource}
              </button>
              <button
                onClick={() => setAddSourceDocType('PAPER')}
                className={`flex-1 rounded-lg border px-3 py-2 text-xs font-bold transition ${addSourceDocType === 'PAPER' ? 'border-[var(--brand)] bg-[var(--brand)] text-white' : 'border-[var(--border)] bg-[var(--surface)] text-[var(--text-secondary)] hover:border-indigo-300'}`}
              >
                {t.asPaper}
              </button>
            </div>
            <div className="flex gap-2">
              <input value={doiInput} onChange={e => setDoiInput(e.target.value)} placeholder="10.1000/xyz123" className="min-w-0 flex-1 rounded-lg border border-[var(--border)] bg-[var(--surface)] px-3 py-2 text-xs outline-none" />
              <button onClick={() => handleImportDoiUnified(addSourceDocType === 'SOURCE')} disabled={addSourceLoading || !doiInput.trim()} className="rounded-lg bg-[var(--brand)] px-3 py-2 text-xs font-bold text-white hover:bg-[var(--brand-hover)] disabled:opacity-50">
                {addSourceLoading ? '...' : t.import}
              </button>
            </div>
            {addSourceDocType === 'SOURCE' && (
              <p className="text-[10px] italic text-[var(--text-tertiary)]">{t.sourcesAutoClassified}</p>
            )}
          </div>
          <div className="space-y-3 rounded-xl border border-[var(--border)] p-4">
            <h3 className="font-bold text-[var(--text-primary)]">{t.uploadSourceFile}</h3>
            <input type="file" accept=".pdf,.docx" onChange={async (e) => { await handleUploadSource(e); setShowAddSource(false); }} className="text-xs" />
          </div>
          <div className="space-y-3 rounded-xl border border-[var(--border)] p-4">
            <h3 className="font-bold text-[var(--text-primary)]">{t.shareFromCollection}</h3>
            <button onClick={() => { setShowShareCollection(true); loadCollections(); setShowAddSource(false); }} className="rounded-lg bg-[var(--brand)] px-3 py-2 font-bold text-white hover:bg-[var(--brand-hover)]">{t.browseCollections}</button>
          </div>
          <div className="flex justify-end gap-2">
            <button onClick={() => setShowAddSource(false)} className="rounded-lg bg-[var(--surface-tertiary)] px-4 py-2 text-xs font-semibold text-[var(--text-secondary)] hover:opacity-80">{ct.cancel}</button>
          </div>
        </div>
      </Modal>

      <Modal open={showSetUpPaper} onClose={() => setShowSetUpPaper(false)} title={t.setUpPaper}>
        {sectionStructureLocked ? (
          <div className="space-y-4 text-xs">
            <div className="flex items-center gap-2 rounded-lg border border-amber-200 bg-amber-50 px-4 py-3">
              <svg aria-hidden="true" viewBox="0 0 24 24" className="h-4 w-4 shrink-0 fill-none stroke-amber-800" strokeWidth="2"><rect x="5" y="10" width="14" height="10" rx="2" /><path d="M8 10V7a4 4 0 0 1 8 0v3" /></svg>
              <span className="text-amber-800">
                {projectReadOnly ? t.setupLockedReadOnly : t.setupLockedAssigned}
              </span>
            </div>
            <div className="flex justify-end">
              <button onClick={() => setShowSetUpPaper(false)} className="rounded-lg bg-[var(--surface-tertiary)] px-4 py-2 text-xs font-semibold text-[var(--text-secondary)] hover:opacity-80">{ct.close}</button>
            </div>
          </div>
        ) : (
          <div className="space-y-5 text-xs">
            <div className="flex gap-1 rounded-lg bg-[var(--surface-tertiary)] p-1">
              <button onClick={() => setSetupMode('standard')}
                className={`flex flex-1 items-center justify-center gap-2 rounded-md px-3 py-2 text-xs font-bold transition ${setupMode === 'standard' ? 'bg-[var(--surface)] text-[var(--brand-foreground)] shadow-sm' : 'text-[var(--text-secondary)] hover:text-[var(--text-primary)]'}`}>
                <svg aria-hidden="true" viewBox="0 0 24 24" className="h-4 w-4 fill-none stroke-current" strokeWidth="2"><rect x="5" y="4" width="14" height="17" rx="2" /><path d="M9 2h6v4H9zM8 10h8M8 14h8M8 18h5" /></svg>
                {t.chooseStandard}
              </button>
              <button onClick={() => setSetupMode('paper')}
                className={`flex flex-1 items-center justify-center gap-2 rounded-md px-3 py-2 text-xs font-bold transition ${setupMode === 'paper' ? 'bg-[var(--surface)] text-[var(--brand-foreground)] shadow-sm' : 'text-[var(--text-secondary)] hover:text-[var(--text-primary)]'}`}>
                <svg aria-hidden="true" viewBox="0 0 24 24" className="h-4 w-4 fill-none stroke-current" strokeWidth="2"><path d="M6 2h8l4 4v16H6zM14 2v5h5M9 13h6M12 10v6" /></svg>
                {t.uploadPaper}
              </button>
            </div>

            {setupMode === 'standard' && (
              <div className="space-y-3 rounded-xl border border-[var(--border)] p-4">
                <h3 className="font-bold text-[var(--brand-foreground)]">{t.chooseStandard}</h3>
                <p className="text-[var(--text-tertiary)]">{t.chooseStandardDesc}</p>
                <select value={standard} onChange={e => setStandard(e.target.value)} className="w-full rounded-lg border border-[var(--border)] bg-[var(--surface)] px-3 py-2 outline-none">
                  <option value="">{t.noStandard}</option>
                  {STANDARDS.map(s => <option key={s} value={s}>{s}</option>)}
                </select>
                <button onClick={handleUpdateStandard} disabled={saving} className="rounded-lg bg-[var(--brand)] px-4 py-2 font-bold text-white hover:bg-[var(--brand-hover)] disabled:opacity-50">{saving ? ct.saving : t.saveStandard}</button>
              </div>
            )}

            {setupMode === 'paper' && (
              <div className="space-y-3 rounded-xl border border-[var(--border)] p-4">
                <h3 className="font-bold text-[var(--brand-foreground)]">{t.uploadPaper}</h3>
                <p className="text-[var(--text-tertiary)]">{t.uploadPaperDesc}</p>
                <input type="file" accept=".pdf,.docx" onChange={(e) => { handleUploadPaper(e); setShowSetUpPaper(false); }} className="text-xs" />
              </div>
            )}

            <div className="flex justify-end gap-2">
              <button onClick={() => setShowSetUpPaper(false)} className="rounded-lg bg-[var(--surface-tertiary)] px-4 py-2 text-xs font-semibold text-[var(--text-secondary)] hover:opacity-80">{ct.cancel}</button>
            </div>
          </div>
        )}
      </Modal>

      <Modal open={showShareCollection} onClose={() => { setShowShareCollection(false); setSelectedCollectionId(''); }} title={t.shareFromCollection}>
        <div className="space-y-4 text-xs">
          {collections.length === 0 ? (
            <p className="italic text-[var(--text-tertiary)]">{t.noCollectionsFound}</p>
          ) : (
            <select value={selectedCollectionId} onChange={e => setSelectedCollectionId(e.target.value)} className="w-full rounded-lg border border-[var(--border)] bg-[var(--surface)] px-3 py-2 text-xs outline-none">
              <option value="">{t.selectCollection}</option>
              {collections.map(c => {
                const linked = linkedCollections.some(item => String(item.id) === String(c.id));
                return <option key={c.id} value={c.id}>{c.name || c.title || c.id}{linked ? ` — ${t.collectionLinked}` : ''}</option>;
              })}
            </select>
          )}
          {selectedCollectionId && (
            <p className="rounded-lg bg-[var(--surface-secondary)] px-3 py-2 text-[var(--text-secondary)]">
              {linkedCollections.some(c => String(c.id) === String(selectedCollectionId))
                ? t.collectionLinked : t.collectionNotLinked}
            </p>
          )}
          {projectReadOnly && (
            <p className="rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-amber-800">{t.collectionSyncPaused}</p>
          )}
          {selectedCollectionId && (
            <button onClick={handleToggleCollection} disabled={projectReadOnly || shareLoadingId !== null} className="w-full rounded-lg bg-[var(--brand)] px-3 py-2 text-xs font-bold text-white hover:bg-[var(--brand-hover)] disabled:cursor-not-allowed disabled:opacity-50">
              {shareLoadingId === selectedCollectionId
                ? ct.saving
                : linkedCollections.some(c => String(c.id) === String(selectedCollectionId))
                  ? t.stopCollectionSync : t.shareEntireCollection}
            </button>
          )}
          <div className="flex justify-end gap-2">
            <button onClick={() => { setShowShareCollection(false); setSelectedCollectionId(''); }} className="rounded-lg bg-[var(--surface-tertiary)] px-4 py-2 text-xs font-semibold text-[var(--text-secondary)] hover:opacity-80">{ct.close}</button>
          </div>
        </div>
      </Modal>

      <Modal open={showExportModal} onClose={() => setShowExportModal(false)} title={t.export}>
        <div className="space-y-3 text-xs">
          <button onClick={async () => {
            try {
              const r = await api.get(`/api/projects/${id}/export?format=tex`, { responseType: 'blob' });
              const url = URL.createObjectURL(r.data);
              const a = document.createElement('a'); a.href = url; a.download = `papers-${project?.title || 'export'}.zip`;
              a.click(); URL.revokeObjectURL(url);
              const warningCount = Number(r.headers?.['x-claim-warning-count'] || 0);
              if (warningCount > 0) alert(t.exportWarning.replace('{{count}}', warningCount));
              setShowExportModal(false);
            } catch { alert(t.exportFailed); }
          }} className="w-full rounded-lg bg-emerald-50 px-4 py-3 text-left font-medium text-emerald-800 transition hover:bg-emerald-100">
            {t.paperArchive}
            <span className="block text-[10px] font-normal text-emerald-900/70">{t.paperArchiveDesc}</span>
          </button>
          <button onClick={async () => {
            try {
              const r = await api.get(`/api/projects/${id}/traceability`);
              const blob = new Blob([JSON.stringify(r.data, null, 2)], { type: 'application/json' });
              const url = URL.createObjectURL(blob);
              const a = document.createElement('a'); a.href = url; a.download = `traceability-${project?.title || 'export'}.json`;
              a.click(); URL.revokeObjectURL(url);
              setShowExportModal(false);
            } catch { alert(t.exportFailed); }
          }} className="w-full rounded-lg bg-emerald-50 px-4 py-3 text-left font-medium text-emerald-800 transition hover:bg-emerald-100">
            {t.traceabilityJson}
            <span className="block text-[10px] font-normal text-emerald-900/70">{t.traceabilityJsonDesc}</span>
          </button>
          <button onClick={async () => {
            try {
              const r = await api.get(`/api/projects/${id}/traceability/csv`, { responseType: 'blob' });
              const url = URL.createObjectURL(r.data);
              const a = document.createElement('a'); a.href = url; a.download = `traceability-${project?.title || 'export'}.csv`;
              a.click(); URL.revokeObjectURL(url);
              setShowExportModal(false);
            } catch { alert(t.exportFailed); }
          }} className="w-full rounded-lg bg-emerald-50 px-4 py-3 text-left font-medium text-emerald-800 transition hover:bg-emerald-100">
            {t.traceabilityCsv}
            <span className="block text-[10px] font-normal text-emerald-900/70">{t.traceabilityCsvDesc}</span>
          </button>
          <div className="flex justify-end">
            <button onClick={() => setShowExportModal(false)} className="rounded-lg bg-[var(--surface-tertiary)] px-4 py-2 text-xs font-semibold text-[var(--text-secondary)] hover:opacity-80">{ct.cancel}</button>
          </div>
        </div>
      </Modal>

      {uploadState && (
        <Modal open={true} onClose={() => {}} title="">
          <Marker role="status">
            <MarkerIcon>
              <Spinner className="animate-spin h-8 w-8 text-indigo-600" />
            </MarkerIcon>
            <MarkerContent className="shimmer-text">
              {uploadState === 'uploading' ? t.uploadingPaper : t.processingSections}
            </MarkerContent>
          </Marker>
        </Modal>
      )}
    </div>
  );
}
