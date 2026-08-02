import React, { useState, useRef, useEffect, useCallback, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useTranslation } from 'react-i18next';
import TourLauncher from '../../components/TourLauncher';
import FileViewerModal from '../../components/FileViewerModal';
import api from '../../api.js';
import { Client } from '@stomp/stompjs';
import WorkspaceHeader from './WorkspaceHeader.jsx';
import FilePanel from './FilePanel.jsx';
import EditorPanel from './EditorPanel.jsx';
import ContextPanel from './ContextPanel.jsx';
import FullPaperPreview from './FullPaperPreview.jsx';
import { hasActiveExtraction } from './extractionPolling.js';

const DEFAULT_SAMPLE_LATEX = `% Select a paper from the file panel to start editing.`;

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

export default function WorkspaceLayout() {
  const { projectId } = useParams();
  const navigate = useNavigate();
  const { logout, user, role } = useAuth();
  const { t, i18n } = useTranslation();
  const [activeTab, setActiveTab] = useState(() => localStorage.getItem('student_workspace_active_tab') || 'Source');
  const [showHistoryModal, setShowHistoryModal] = useState(false);
  const [showOverview, setShowOverview] = useState(false);
  const [sectionsExpanded, setSectionsExpanded] = useState(true);
  const [assignedExpanded, setAssignedExpanded] = useState(true);
  const [showReviseModal, setShowReviseModal] = useState(false);
  const [toastMessage, setToastMessage] = useState('');

  const [project, setProject] = useState(null);
  const [projects, setProjects] = useState([]);
  const [sources, setSources] = useState([]);
  const [mediaAssets, setMediaAssets] = useState([]);
  const [papers, setPapers] = useState([]);
  const [selectedPaper, setSelectedPaper] = useState(null);
  const [claims, setClaims] = useState([]);
  const [feedbacks, setFeedbacks] = useState([]);
  const [graphData, setGraphData] = useState(null);
  const [graphScope, setGraphScope] = useState('all');
  const [exports, setExports] = useState([]);
  const [loadingProject, setLoadingProject] = useState(false);
  const [isUploading, setIsUploading] = useState(false);
  const [viewerFile, setViewerFile] = useState(null);
const [showFullPaperPreview, setShowFullPaperPreview] = useState(false);

  const [codeContent, setCodeContent] = useState('');

  const [showSymbolMenu, setShowSymbolMenu] = useState(false);
  const [showTextSizeMenu, setShowTextSizeMenu] = useState(false);
  const [showSearchPanel, setShowSearchPanel] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [replaceQuery, setReplaceQuery] = useState('');
  const [editorWidth, setEditorWidth] = useState(50);
  const [fileTreeWidth, setFileTreeWidth] = useState(256);
  const [rightDrawerWidth, setRightDrawerWidth] = useState(380);
  const [isDrawerOpen, setIsDrawerOpen] = useState(true);
  const [isFileTreeOpen, setIsFileTreeOpen] = useState(true);
  const [textSize, setTextSize] = useState(14);
  const [codeHistory, setCodeHistory] = useState(['']);
  const [historyIndex, setHistoryIndex] = useState(0);

  const [selectedPaperDetail, setSelectedPaperDetail] = useState(null);

  const [showSubmitReviewModal, setShowSubmitReviewModal] = useState(false);
  const [showExportMenu, setShowExportMenu] = useState(false);
  const [showAiReviewModal, setShowAiReviewModal] = useState(false);
  const [citationResult, setCitationResult] = useState(null);
  const [loadingCitation, setLoadingCitation] = useState(false);

  const [loadingAiReview, setLoadingAiReview] = useState(false);
  const [rollingBack, setRollingBack] = useState(false);
  const [aiReviewResult, setAiReviewResult] = useState(null);
  const [aiReviewError, setAiReviewError] = useState(null);
  const [newClaimContent, setNewClaimContent] = useState('');
  const [newClaimFunctionalType, setNewClaimFunctionalType] = useState('EMPIRICAL');
  const [claimEvaluation, setClaimEvaluation] = useState(null);
  const [evaluatedClaimContent, setEvaluatedClaimContent] = useState('');
  const [evaluatedClaimSectionId, setEvaluatedClaimSectionId] = useState('');
  const [evaluatingClaim, setEvaluatingClaim] = useState(false);
  const [claimEvaluationError, setClaimEvaluationError] = useState('');
  const [editingClaim, setEditingClaim] = useState(null);
  const [editClaimContent, setEditClaimContent] = useState('');
  const [editClaimFunctionalType, setEditClaimFunctionalType] = useState('EMPIRICAL');
  const [claimStats, setClaimStats] = useState(null);
  const [selectedClaim, setSelectedClaim] = useState(null);
  const [claimMatches, setClaimMatches] = useState([]);
  const [claimMappings, setClaimMappings] = useState([]);
  const [loadingMatches, setLoadingMatches] = useState(false);
  const [claimCandidates, setClaimCandidates] = useState([]);
  const [loadingCandidates, setLoadingCandidates] = useState(false);
  const [evaluatingChunkId, setEvaluatingChunkId] = useState(null);
  const [updatingSuggestionId, setUpdatingSuggestionId] = useState(null);
  const [sections, setSections] = useState([]);
  const [selectedSectionId, setSelectedSectionId] = useState('');
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [showNotifications, setShowNotifications] = useState(false);
  const stompRef = useRef(null);
  const editorRef = useRef(null);
  const claimEvaluationRequestRef = useRef(0);

  const updateCode = (newVal) => {
    setCodeContent(newVal);
    const nextHistory = codeHistory.slice(0, historyIndex + 1);
    nextHistory.push(newVal);
    setCodeHistory(nextHistory);
    setHistoryIndex(nextHistory.length - 1);
  };

  const loadCode = (newVal) => {
    const text = newVal || '';
    setCodeContent(text);
    setCodeHistory([text]);
    setHistoryIndex(0);
  };

  const displayContent = selectedPaper ? codeContent : DEFAULT_SAMPLE_LATEX;

  const showToast = (msg) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(''), 3000);
  };

  // Resize handlers
  const handleMouseDown = (e) => {
    e.preventDefault();
    document.body.style.cursor = 'col-resize';
    document.body.style.userSelect = 'none';
    const onMouseMove = (me) => {
      const container = document.getElementById('editor-preview-container');
      if (!container) return;
      const cr = container.getBoundingClientRect();
      let pct = ((me.clientX - cr.left) / cr.width) * 100;
      if (pct < 15) pct = 15;
      if (pct > 85) pct = 85;
      setEditorWidth(pct);
    };
    const onMouseUp = () => { document.removeEventListener('mousemove', onMouseMove); document.removeEventListener('mouseup', onMouseUp); document.body.style.cursor = ''; document.body.style.userSelect = ''; };
    document.addEventListener('mousemove', onMouseMove);
    document.addEventListener('mouseup', onMouseUp);
  };

  const handleLeftDividerMouseDown = (e) => {
    e.preventDefault();
    document.body.style.cursor = 'col-resize';
    document.body.style.userSelect = 'none';
    const onMouseMove = (me) => {
      let nw = me.clientX;
      const p = document.getElementById('workspace-container');
      if (p) nw = me.clientX - p.getBoundingClientRect().left - 56;
      if (nw < 160) nw = 160;
      if (nw > 450) nw = 450;
      setFileTreeWidth(nw);
    };
    const onMouseUp = () => { document.removeEventListener('mousemove', onMouseMove); document.removeEventListener('mouseup', onMouseUp); document.body.style.cursor = ''; document.body.style.userSelect = ''; };
    document.addEventListener('mousemove', onMouseMove);
    document.addEventListener('mouseup', onMouseUp);
  };

  const handleRightDividerMouseDown = (e) => {
    e.preventDefault();
    document.body.style.cursor = 'col-resize';
    document.body.style.userSelect = 'none';
    const onMouseMove = (me) => {
      let nw = 380;
      const p = document.getElementById('workspace-container');
      if (p) nw = p.getBoundingClientRect().right - me.clientX;
      if (nw < 250) nw = 250;
      if (nw > 600) nw = 600;
      setRightDrawerWidth(nw);
    };
    const onMouseUp = () => { document.removeEventListener('mousemove', onMouseMove); document.removeEventListener('mouseup', onMouseUp); document.body.style.cursor = ''; document.body.style.userSelect = ''; };
    document.addEventListener('mousemove', onMouseMove);
    document.addEventListener('mouseup', onMouseUp);
  };

  // Data loading

  const loadProjectData = useCallback(async (projId) => {
    if (!projId) return;
    try {
      claimEvaluationRequestRef.current += 1;
      setEvaluatingClaim(false);
      setSections([]);
      setSelectedPaper(null);
      setSelectedClaim(null);
      setClaimMatches([]);
      setClaimMappings([]);
      setClaimCandidates([]);
      setCitationResult(null);
      setAiReviewResult(null);
      setAiReviewError(null);
      setClaimEvaluation(null);
      setEvaluatedClaimContent('');
      setEvaluatedClaimSectionId('');
      setClaimEvaluationError('');
      setGraphData(null);
      const projRes = await api.get(`/api/projects/${projId}`);
      setProject(projRes.data);
      try { setSources(await loadAllProjectSources(projId)); } catch {}
      try { const r = await api.get(`/api/media/projects/${projId}`); setMediaAssets(r.data || []); } catch {}
      try {
        const r = await api.get(`/api/projects/${projId}/papers`);
        const list = r.data || [];
        setPapers(list);
        if (list.length > 0) { setSelectedPaper(list[0]); loadCode(list[0].extractedText || ''); }
        else { setSelectedPaper(null); loadCode(''); }
      } catch {}
      try { const r = await api.get(`/api/projects/${projId}/claims`); setClaims(r.data?.content || []); } catch {}
      try {
        const r = await api.get('/api/feedback-requests');
        const all = r.data || [];
        setFeedbacks(all.filter(fb => fb.projectId === parseInt(projId)));
      } catch {}
      try { const r = await api.get(`/api/projects/${projId}/graph?scope=${graphScope}`); setGraphData(r.data); } catch {}
    } catch (err) { console.error('loadProjectData error:', err); }
  }, [graphScope]);

  useEffect(() => {
    (async () => {
      try {
        setLoadingProject(true);
        let pid = projectId;
        const listRes = await api.get('/api/projects');
        const active = listRes.data?.content || [];
        setProjects(active);
        if (!pid && active.length > 0) { pid = active[0].id; navigate(`/student/projects/${pid}`, { replace: true }); return; }
        if (pid) await loadProjectData(pid);
      } catch (err) { console.error(err); }
      finally { setLoadingProject(false); }
    })();
  }, [projectId, loadProjectData]);

  useEffect(() => {
    if (!project?.id || !hasActiveExtraction(sources)) return undefined;

    let cancelled = false;
    let timer;
    const refresh = async () => {
      try {
        const [sourceList, mediaResponse] = await Promise.all([
          loadAllProjectSources(project.id),
          api.get(`/api/media/projects/${project.id}`),
        ]);
        if (!cancelled) {
          setSources(sourceList);
          setMediaAssets(mediaResponse.data || []);
        }
      } catch {
        if (!cancelled) timer = window.setTimeout(refresh, 5000);
        return;
      }
      if (!cancelled) timer = window.setTimeout(refresh, 5000);
    };

    timer = window.setTimeout(refresh, 5000);
    return () => {
      cancelled = true;
      window.clearTimeout(timer);
    };
  }, [project?.id, sources]);

  const assignedSections = user ? sections.filter(s => String(s.assignedUserId) === String(user.id)) : [];
  const isLocked = project?.status === 'SUBMITTED_FOR_REVIEW' || project?.status === 'APPROVED' || project?.status === 'ARCHIVED';
  const canEditSection = (section) => {
    if (isLocked || !section) return false;
    return role === 'STUDENT'
      && Boolean(section.assignedUserId)
      && String(section.assignedUserId) === String(user?.id);
  };
  const currentSection = sections.find(section =>
    String(section.id) === String(selectedSectionId));
  const canEditCurrentSection = canEditSection(currentSection);
  const canAddEvaluatedClaim = Boolean(
    claimEvaluation
    && evaluatedClaimContent === newClaimContent.trim()
    && String(evaluatedClaimSectionId) === String(selectedSectionId)
  );
  const requireEditableCurrentSection = () => {
    if (canEditCurrentSection) return true;
    showToast('This section is read-only.');
    return false;
  };

  useEffect(() => {
    claimEvaluationRequestRef.current += 1;
    setEvaluatingClaim(false);
    setClaimEvaluation(null);
    setEvaluatedClaimContent('');
    setEvaluatedClaimSectionId('');
    setClaimEvaluationError('');
  }, [selectedSectionId]);

  useEffect(() => {
    if (!selectedPaper) { setSections([]); return; }
    api.get(`/api/papers/${selectedPaper.id}/sections`)
      .then(r => {
        const list = r.data || [];
        setSections(list);
        const mine = user ? list.filter(s => String(s.assignedUserId) === String(user.id)) : [];
        if (mine.length > 0) { setSelectedSectionId(mine[0].id); loadCode(mine[0].contentTex || ''); }
        else { setSelectedSectionId(''); loadCode(''); }
      })
      .catch(() => setSections([]));
  }, [selectedPaper, user]);

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (!token) return;
    const base = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
    const wsUrl = base.replace(/^http/, 'ws') + '/ws';
    const client = new Client({
      brokerURL: wsUrl,
      connectHeaders: { Authorization: 'Bearer ' + token },
      onConnect: () => {
        client.subscribe('/user/queue/notifications', msg => {
          try {
            const n = JSON.parse(msg.body);
            setNotifications(prev => [n, ...prev]);
            setUnreadCount(c => c + 1);
            if (n.actionType === 'EXPORT_READY') {
              showToast('Export is ready for download!');
              if (project) fetchExports();
            } else {
              showToast(n.message || 'New notification');
            }
          } catch {}
        });
      },
      reconnectDelay: 5000,
    });
    client.activate();
    stompRef.current = client;
    return () => { client.deactivate(); };
  }, []);

  useEffect(() => {
    api.get('/api/notifications/unread-count').then(r => setUnreadCount(r.data?.count || 0)).catch(() => {});
    api.get('/api/notifications').then(r => setNotifications(r.data || [])).catch(() => {});
  }, [projectId]);

  const handleMarkNotificationRead = async (id) => {
    try {
      await api.patch(`/api/notifications/${id}/read`);
      setNotifications(prev => prev.map(n => n.id === id ? { ...n, read: true } : n));
      setUnreadCount(c => Math.max(0, c - 1));
    } catch {}
  };

  const handleExportTexArchive = async () => {
    if (!project) return;
    try {
      const r = await api.get(`/api/projects/${project.id}/export?format=tex`, { responseType: 'blob' });
      const url = URL.createObjectURL(r.data);
      const a = document.createElement('a'); a.href = url; a.download = `papers-${project?.title || 'export'}.zip`;
      a.click(); URL.revokeObjectURL(url);
      const warningCount = Number(r.headers?.['x-claim-warning-count'] || 0);
      showToast(warningCount > 0
        ? `Downloaded with ${warningCount} Claim usage warning${warningCount > 1 ? 's' : ''}; see CLAIM_WARNINGS.md.`
        : 'Downloaded paper archive.');
    } catch { showToast('Export failed.'); }
  };

  const fetchExports = useCallback(async () => {
    if (!project) return;
    try { const r = await api.get('/api/exports', { params: { projectId: project.id } }); setExports(r.data || []); } catch {}
  }, [project]);

  const fetchSources = useCallback(async () => {
    if (!project) return;
    try { setSources(await loadAllProjectSources(project.id)); } catch {}
  }, [project]);

  // CRUD handlers
  const handleUploadPaper = async (file) => {
    if (!file || !project) return;
    showToast(`Uploading ${file.name}...`);
    const fd = new FormData();
    fd.append('file', file); fd.append('projectId', project.id);
    try {
      await api.post('/api/papers', fd, { headers: { 'Content-Type': 'multipart/form-data' } });
      showToast("Paper uploaded.");
      const r = await api.get(`/api/projects/${project.id}/papers`);
      const list = r.data || [];
      setPapers(list);
      if (list.length > 0) { setSelectedPaper(list[list.length - 1]); loadCode(list[list.length - 1].extractedText || ''); }
    } catch { showToast("Upload failed."); }
  };

  const handleDeletePaper = async (paperId) => {
    if (!window.confirm("Delete this draft?")) return;
    try {
      await api.delete(`/api/papers/${paperId}`);
      showToast("Paper deleted.");
      const r = await api.get(`/api/projects/${project.id}/papers`);
      const list = r.data || [];
      setPapers(list);
      if (selectedPaper && selectedPaper.id === paperId) {
        if (list.length > 0) { setSelectedPaper(list[0]); loadCode(list[0].extractedText || ''); }
        else { setSelectedPaper(null); loadCode(''); }
      }
    } catch { showToast("Delete failed."); }
  };

  const handleUploadSource = async (file) => {
    if (isLocked) { showToast("Project is locked."); return; }
    if (!file || !project || !user) return;
    showToast(`Uploading ${file.name}...`);
    const fd = new FormData();
    fd.append('file', file); fd.append('projectId', project.id);
    try {
      await api.post('/api/sources', fd, { headers: { 'Content-Type': 'multipart/form-data' } });
      showToast("Source uploaded.");
      setSources(await loadAllProjectSources(project.id));
      const g = await api.get(`/api/projects/${project.id}/graph?scope=${graphScope}`);
      setGraphData(g.data);
    } catch { showToast("Upload failed."); }
  };

  const handleDeleteSource = async (sourceId) => {
    if (!window.confirm("Delete this source?")) return;
    try {
      await api.delete(`/api/documents/${sourceId}`);
      showToast("Source deleted.");
      setSources(await loadAllProjectSources(project.id));
      const g = await api.get(`/api/projects/${project.id}/graph?scope=${graphScope}`);
      setGraphData(g.data);
    } catch { showToast("Delete failed."); }
  };

  const handleUploadMedia = async (file) => {
    if (isLocked) { showToast("Project is locked."); return; }
    if (!file || !project) return;
    showToast(`Uploading ${file.name}...`);
    const fd = new FormData();
    fd.append('file', file); fd.append('projectId', project.id);
    try {
      await api.post('/api/media', fd, { headers: { 'Content-Type': 'multipart/form-data' } });
      showToast("Media uploaded.");
      const r = await api.get(`/api/media/projects/${project.id}`);
      setMediaAssets(r.data || []);
    } catch { showToast("Upload failed."); }
  };

  const handleDeleteMedia = async (mediaId) => {
    if (!window.confirm("Delete this media?")) return;
    try {
      await api.delete(`/api/media/${mediaId}`);
      showToast("Media deleted.");
      const r = await api.get(`/api/media/projects/${project.id}`);
      setMediaAssets(r.data || []);
    } catch { showToast("Delete failed."); }
  };

  const handleInsertMedia = (texFilename) => {
    if (!requireEditableCurrentSection()) return;
    const ed = editorRef.current;
    if (!ed) return;
    ed.insertAtCursor(`\\includegraphics{${texFilename}}`, 0);
    showToast(`Inserted ${texFilename}`);
  };

  const handleNewClaimContentChange = (content) => {
    claimEvaluationRequestRef.current += 1;
    setNewClaimContent(content);
    setEvaluatingClaim(false);
    setClaimEvaluation(null);
    setEvaluatedClaimContent('');
    setEvaluatedClaimSectionId('');
    setClaimEvaluationError('');
  };

  const handleEvaluateClaim = async () => {
    if (isLocked) { showToast("Project is locked."); return; }
    const content = newClaimContent.trim();
    if (!content) { showToast("Enter a Claim first."); return; }
    if (!currentSection) { showToast("Select a section first."); return; }
    if (!canEditSection(currentSection)) {
      showToast("You cannot edit claims in this section.");
      return;
    }
    const requestId = claimEvaluationRequestRef.current + 1;
    claimEvaluationRequestRef.current = requestId;
    setEvaluatingClaim(true);
    setClaimEvaluationError('');
    try {
      const response = await api.post('/api/claims/evaluate', {
        sectionId: selectedSectionId,
        content,
      });
      if (claimEvaluationRequestRef.current !== requestId) return;
      setClaimEvaluation(response.data);
      setEvaluatedClaimContent(content);
      setEvaluatedClaimSectionId(selectedSectionId);
      setNewClaimFunctionalType(response.data.suggestedFunctionalType || 'EMPIRICAL');
    } catch (error) {
      if (claimEvaluationRequestRef.current !== requestId) return;
      const status = error.response?.status;
      const message = status === 503
        ? 'AI service is unavailable. Retry when it is back online.'
        : status === 502
          ? 'AI returned an invalid evaluation. Please retry.'
          : 'AI Evaluate failed. Please retry.';
      setClaimEvaluation(null);
      setEvaluatedClaimContent('');
      setEvaluatedClaimSectionId('');
      setClaimEvaluationError(message);
    } finally {
      if (claimEvaluationRequestRef.current === requestId) setEvaluatingClaim(false);
    }
  };

  const handleCreateClaim = async () => {
    if (isLocked) { showToast("Project is locked."); return; }
    if (!newClaimContent.trim() || !project) return;
    const section = currentSection;
    if (!section) { showToast("Select a section first."); return; }
    if (!canEditSection(section)) {
      showToast("You cannot edit claims in this section.");
      return;
    }
    if (!canAddEvaluatedClaim) {
      showToast("Run AI Evaluate for the current Claim before adding it.");
      return;
    }
    const sectionId = selectedSectionId;
    try {
      const rawSelection = editorRef.current?.getSelection() || '';
      const created = await api.post('/api/claims', { sectionId, content: newClaimContent.trim(), functionalType: newClaimFunctionalType });
      let nextContent = codeContent;
      if (rawSelection.trim() && rawSelection.trim() === newClaimContent.trim()) {
        nextContent = editorRef.current?.insertAtCursor(
          `\\epclaim{${created.data.id}}{${rawSelection}}`,
        ) ?? codeContent;
      }
      if (nextContent !== (section.contentTex || '')) {
        await api.put(`/api/papers/${selectedPaper.id}/sections/${sectionId}`, null, {
          params: { content: nextContent },
        });
      }
      showToast("Claim added.");
      setNewClaimContent('');
      setClaimEvaluation(null);
      setEvaluatedClaimContent('');
      setEvaluatedClaimSectionId('');
      setClaimEvaluationError('');
      const [claimResponse, sectionResponse, graphResponse, statsResponse] = await Promise.all([
        api.get(`/api/projects/${project.id}/claims`),
        api.get(`/api/papers/${selectedPaper.id}/sections`),
        api.get(`/api/projects/${project.id}/graph?scope=${graphScope}`),
        api.get(`/api/projects/${project.id}/graph/claim-stats`),
      ]);
      setClaims(claimResponse.data?.content || []);
      setSections(sectionResponse.data || []);
      setGraphData(graphResponse.data);
      setClaimStats(statsResponse.data);
    } catch { showToast("Add claim failed."); }
  };

  const handleUseSelectedText = () => {
    const selectedText = editorRef.current?.getSelection()?.trim();
    if (!selectedText) {
      showToast("Select text in the editor first.");
      return;
    }
    handleNewClaimContentChange(selectedText);
    setActiveTab('Claims');
  };

  const handleUpdateClaim = async () => {
    if (!editingClaim || !editClaimContent.trim()) return;
    try {
      const updated = await api.put(`/api/claims/${editingClaim.id}`, { id: editingClaim.id, content: editClaimContent, active: true, aiConfidenceScore: editingClaim.aiConfidenceScore, functionalType: editClaimFunctionalType });
      showToast("Claim updated.");
      setEditingClaim(null); setEditClaimContent('');
      if (selectedClaim?.id === editingClaim.id) {
        setSelectedClaim(updated.data);
        setClaimCandidates([]);
        setClaimMatches([]);
        setClaimMappings([]);
      }
      const r = await api.get(`/api/projects/${project.id}/claims`);
      setClaims(r.data?.content || []);
      const g = await api.get(`/api/projects/${project.id}/graph?scope=${graphScope}`);
      setGraphData(g.data);
      const s = await api.get(`/api/projects/${project.id}/graph/claim-stats`);
      setClaimStats(s.data);
    } catch { showToast("Update failed."); }
  };

  const handleDeleteClaim = async (claimId) => {
    if (!window.confirm("Delete this claim?")) return;
    try {
      await api.delete(`/api/claims/${claimId}`);
      showToast("Claim deleted.");
      const r = await api.get(`/api/projects/${project.id}/claims`);
      setClaims(r.data?.content || []);
      const g = await api.get(`/api/projects/${project.id}/graph?scope=${graphScope}`);
      setGraphData(g.data);
      const s = await api.get(`/api/projects/${project.id}/graph/claim-stats`);
      setClaimStats(s.data);
      if (selectedClaim && selectedClaim.id === claimId) {
        setSelectedClaim(null);
        setClaimMatches([]);
        setClaimMappings([]);
        setClaimCandidates([]);
      }
    } catch { showToast("Delete failed."); }
  };

  const handleAssignSection = async (sectionId, assignedUserId) => {
    if (!selectedPaper) return;
    try {
      await api.put(`/api/papers/${selectedPaper.id}/sections/${sectionId}/assign`, null, { params: { assignedUserId } });
      showToast(assignedUserId ? 'Section assigned.' : 'Section unassigned.');
      const r = await api.get(`/api/papers/${selectedPaper.id}/sections`);
      setSections(r.data || []);
    } catch { showToast('Assign failed.'); }
  };

  const handleRollbackSection = async (sectionId) => {
    if (!selectedPaper) return;
    if (!window.confirm('Restore this previous version?')) return;
    setRollingBack(true);
    try {
      const res = await api.post(`/api/papers/${selectedPaper.id}/sections/${sectionId}/rollback`);
      const updated = res.data;
      setSections(prev => prev.map(s => String(s.id) === String(updated.id) ? updated : s));
      if (String(updated.id) === String(selectedSectionId)) {
        loadCode(updated.contentTex || '');
      }
      showToast('Version restored.');
      setTimeout(() => { setShowHistoryModal(false); setRollingBack(false); }, 300);
    } catch { showToast('Restore failed.'); setRollingBack(false); }
  };

  const canEditClaim = (claim) => {
    const section = sections.find(s => String(s.id) === String(claim.sectionId));
    return canEditSection(section);
  };

  const handleSaveDraft = async () => {
    if (!requireEditableCurrentSection()) return;
    if (!selectedPaper) { showToast("No paper selected."); return; }
    if (!selectedSectionId) { showToast("No section selected."); return; }
    setSaveStatus('saving');
    try {
      await api.put(`/api/papers/${selectedPaper.id}/sections/${selectedSectionId}`, null, { params: { content: codeContent } });
      setSaveStatus('saved'); setLastSaved(new Date());
      const [sectionResponse, claimResponse, graphResponse] = await Promise.all([
        api.get(`/api/papers/${selectedPaper.id}/sections`),
        api.get(`/api/projects/${project.id}/claims`),
        api.get(`/api/projects/${project.id}/graph?scope=${graphScope}`),
      ]);
      setSections(sectionResponse.data || []);
      setClaims(claimResponse.data?.content || []);
      setGraphData(graphResponse.data);
      setTimeout(() => setSaveStatus(''), 3000);
    } catch { setSaveStatus('error'); showToast("Save failed."); }
  };

  const [saveStatus, setSaveStatus] = useState('');
  const [lastSaved, setLastSaved] = useState(null);

  const handleExportTraceabilityJson = async () => {
    if (!project) return;
    try {
      const r = await api.get(`/api/projects/${project.id}/traceability`);
      const blob = new Blob([JSON.stringify(r.data, null, 2)], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a'); a.href = url; a.download = `traceability-${project.title || 'export'}.json`;
      a.click(); URL.revokeObjectURL(url);
      showToast('Downloaded traceability report.');
    } catch { showToast('Export failed.'); }
  };

  const handleExportTraceabilityCsv = async () => {
    if (!project) return;
    try {
      const r = await api.get(`/api/projects/${project.id}/traceability/csv`, { responseType: 'blob' });
      const url = URL.createObjectURL(r.data);
      const a = document.createElement('a'); a.href = url; a.download = `traceability-${project.title || 'export'}.csv`;
      a.click(); URL.revokeObjectURL(url);
      showToast('Downloaded traceability CSV.');
    } catch { showToast('Export failed.'); }
  };

  const handleExportCsv = () => {
    if (!graphData) return;
    const esc = (s) => `"${(s || '').replace(/"/g, '""')}"`;
    const rows = [['Claim ID', 'Claim Content', 'Verdict', 'Confidence', 'Section', 'Matched Sources']];
    const srcMap = {}; (graphData.sources || []).forEach(s => { srcMap[s.id] = s.filename; });
    (graphData.claims || []).forEach(c => {
      const g = c.graphData || {}; const verdict = g.verdict || ''; const conf = g.confidence ?? '';
      const sourceNames = (g.matched_source_ids || []).map(sid => srcMap[sid] || sid).join('; ');
      rows.push([esc(c.id), esc(c.content), esc(verdict), conf, esc(c.sectionTitle || ''), esc(sourceNames)]);
    });
    const csv = rows.map(r => r.join(',')).join('\n');
    const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a'); a.href = url; a.download = `graph-${project?.title || 'export'}.csv`;
    a.click(); URL.revokeObjectURL(url);
  };

  const fetchGraphData = useCallback(async (projId, scope = graphScope) => {
    try { const r = await api.get(`/api/projects/${projId}/graph?scope=${scope}`); setGraphData(r.data); } catch {}
  }, [graphScope]);

  const fetchClaimStats = useCallback(async (projId) => {
    try { const r = await api.get(`/api/projects/${projId}/graph/claim-stats`); setClaimStats(r.data); } catch {}
  }, []);

  const handleGraphScopeToggle = () => {
    const next = graphScope === 'own' ? 'all' : 'own';
    setGraphScope(next);
    if (project) fetchGraphData(project.id, next);
  };

  useEffect(() => {
    if (activeTab === 'Graph' && project?.id && !graphData) fetchGraphData(project.id);
  }, [activeTab, project?.id, graphData, fetchGraphData]);

  useEffect(() => {
    if (activeTab === 'Graph' && project?.id && !claimStats) fetchClaimStats(project.id);
  }, [activeTab, project?.id, claimStats, fetchClaimStats]);

  const handleSearchClaimMatches = async (claim) => {
    setSelectedClaim(claim);
    setClaimCandidates([]);
    setLoadingCandidates(true);
    await handleFetchMatches(claim.id);
    try {
      const response = await api.post(`/api/claims/${claim.id}/matches/search`);
      const candidates = response.data || [];
      setClaimCandidates(candidates);
      showToast(candidates.length > 0
        ? `Found ${candidates.length} source match${candidates.length > 1 ? 'es' : ''}.`
        : 'No matches found. Check that source extraction is ready.');
    } catch { showToast("Find matches failed."); }
    finally { setLoadingCandidates(false); }
  };

  const handleFetchMatches = async (claimId) => {
    setLoadingMatches(true);
    setClaimMatches([]);
    setClaimMappings([]);
    try {
      const [suggestionResponse, mappingResponse] = await Promise.all([
        api.get(`/api/claims/${claimId}/suggestions`),
        api.get(`/api/claims/${claimId}/mappings`),
      ]);
      setClaimMatches(suggestionResponse.data || []);
      setClaimMappings(mappingResponse.data || []);
    } catch { showToast("Fetch matches failed."); }
    finally { setLoadingMatches(false); }
  };

  const handleEvaluateMatch = async (claimId, documentChunkId) => {
    setEvaluatingChunkId(documentChunkId);
    try {
      await api.post(`/api/claims/${claimId}/suggestions/evaluate`, { documentChunkId });
      showToast("AI evaluation complete. Review it before accepting.");
      await handleFetchMatches(claimId);
    } catch { showToast("AI evaluation failed."); }
    finally { setEvaluatingChunkId(null); }
  };

  const handleSuggestionStatus = async (suggestionId, status) => {
    if (!selectedClaim) return;
    setUpdatingSuggestionId(suggestionId);
    try {
      await api.patch(`/api/claims/suggestions/${suggestionId}/status`, null, { params: { status } });
      showToast(status === 'ACCEPTED' ? 'Evidence accepted.' : 'Suggestion rejected.');
      await handleFetchMatches(selectedClaim.id);
      if (status === 'ACCEPTED') {
        const graph = await api.get(`/api/projects/${project.id}/graph?scope=${graphScope}`);
        setGraphData(graph.data);
      }
    } catch { showToast("Update suggestion failed."); }
    finally { setUpdatingSuggestionId(null); }
  };

  const handleSelectClaim = async (claim) => {
    if (selectedClaim?.id !== claim.id) {
      setClaimCandidates([]);
    }
    setSelectedClaim(claim);
    await handleFetchMatches(claim.id);
    if (!claim.sectionId) return;
    const sec = sections.find(s => String(s.id) === String(claim.sectionId));
    if (!sec) return;
    setSelectedSectionId(claim.sectionId);
    loadCode(sec.contentTex || '');
  };

  const handleScanCitations = async () => {
    if (isLocked) { showToast("Project is locked."); return; }
    if (!selectedPaper) { showToast("Select a paper first."); return; }
    setLoadingCitation(true);
    setCitationResult(null);
    try {
      const r = await api.get(`/api/papers/${selectedPaper.id}/format-scan`);
      setCitationResult(r.data);
    } catch { showToast("Format scan failed."); }
    finally { setLoadingCitation(false); }
  };

  const handleRunAiReview = async () => {
    if (isLocked) { showToast("Project is locked."); return; }
    if (!selectedPaper) { showToast("Select a paper first."); return; }
    setLoadingAiReview(true);
    setAiReviewError(null);
    setShowAiReviewModal(true);
    try {
      const r = await api.post(`/api/papers/${selectedPaper.id}/review`);
      setAiReviewResult(r.data);
      showToast("AI Review complete.");
      if (project) fetchGraphData(project.id);
    } catch (error) {
      const status = error.response?.status;
      const message = status === 503
        ? 'AI Review worker is unavailable or timed out.'
        : status === 502
          ? 'AI Review returned an invalid response after retrying.'
          : 'AI Review failed. Please retry.';
      setAiReviewError({ status, message });
      showToast(message);
    } finally { setLoadingAiReview(false); }
  };

  const handleSubmitReview = async () => {
    if (!project) return;
    setShowSubmitReviewModal(false);
    if (canEditSection(sections.find(section => String(section.id) === String(selectedSectionId)))) {
      await handleSaveDraft();
    }
    try {
      await api.post(`/api/projects/${project.id}/reviews`);
      showToast("Submitted for review.");
      await loadProjectData(project.id);
    } catch { showToast("Submit failed."); }
  };

  const handleDownloadTex = () => {
    const blob = new Blob([displayContent], { type: 'text/plain;charset=utf-8' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = selectedPaper ? `${selectedPaper.originalFilename?.replace(/\.pdf|\.docx/g, '') || 'document'}.tex` : 'document.tex';
    a.click(); URL.revokeObjectURL(a.href);
    showToast('Downloaded .tex');
  };

  const insertLatexTag = (tagType) => {
    if (!requireEditableCurrentSection()) return;
    const ed = editorRef.current;
    if (!ed) return;
    const sel = ed.getSelection() || '';
    let insertion = '', offset = 0;
    const m = { bold: [`\\textbf{${sel || 'text'}}`, 8], italic: [`\\textit{${sel || 'text'}}`, 8], section: [`\\section{${sel || 'Title'}}`, 9], subsection: [`\\subsection{${sel || 'Subtitle'}}`, 12], subsubsection: [`\\subsubsection{${sel || 'Subtitle2'}}`, 15], large: [`{\\large ${sel || 'text'}}`, 8], small: [`{\\small ${sel || 'text'}}`, 8], 'inline-math': [`$${sel || 'E=mc^2'}$`, 1], list: [`\n\\begin{itemize}\n  \\item ${sel || 'item'}\n\\end{itemize}\n`, 21], equation: [`\\begin{equation}\n  ${sel || 'E = mc^2'}\n\\end{equation}`, 18], comment: [`% ${sel || 'comment'}`, 2], hl: [`\\hl{${sel || 'highlight'}}`, 4] };
    if (m[tagType]) { insertion = m[tagType][0]; offset = m[tagType][1]; }
    else if (tagType === 'label') { const n = prompt('Label name:', 'sec:label') || 'sec:label'; insertion = `\\label{${n}}`; offset = insertion.length; }
    else if (tagType === 'cite') { const k = prompt('Citation key:', 'author2026') || 'key'; insertion = `\\cite{${k}}`; offset = insertion.length; }
    else if (tagType === 'link') { const url = prompt('URL:', 'https://') || 'https://'; const l = sel || prompt('Link label:', 'Link') || 'Link'; insertion = `\\href{${url}}{${l}}`; offset = insertion.length; }
    else if (tagType === 'figure') { insertion = `\n\\begin{figure}[h]\n  \\centering\n  \\includegraphics[width=0.8\\textwidth]{image.png}\n  \\caption{${sel || 'Caption'}}\n  \\label{fig:label}\n\\end{figure}\n`; offset = 83; }
    else if (tagType === 'table') { insertion = `\n\\begin{table}[h]\n  \\centering\n  \\begin{tabular}{|c|c|}\n    \\hline\n    Col1 & Col2 \\\\\n    \\hline\n    ${sel || 'Row1'} & Row1 \\\\\n    Row2 & Row2 \\\\\n    \\hline\n  \\end{tabular}\n  \\caption{Table caption}\n  \\label{tab:table}\n\\end{table}\n`; offset = 120; }
    ed.insertAtCursor(insertion, offset);
  };

  const insertSymbol = (sym) => {
    if (!requireEditableCurrentSection()) return;
    const ed = editorRef.current;
    if (!ed) return;
    ed.insertAtCursor(sym);
  };

  const handleUndo = () => {
    if (historyIndex > 0) { setHistoryIndex(historyIndex - 1); setCodeContent(codeHistory[historyIndex - 1]); }
  };

  const handleRedo = () => {
    if (historyIndex < codeHistory.length - 1) { setHistoryIndex(historyIndex + 1); setCodeContent(codeHistory[historyIndex + 1]); }
  };

  const handleFindReplace = (replaceAll = false) => {
    if (!requireEditableCurrentSection()) return;
    if (!searchQuery) return;
    const text = codeContent;
    if (replaceAll) { updateCode(text.replaceAll(searchQuery, replaceQuery)); showToast('Replaced all.'); return; }
    const ta = document.getElementById('latex-textarea');
    if (ta) {
      const start = ta.selectionStart, end = ta.selectionEnd, selText = text.substring(start, end);
      if (selText === searchQuery) { updateCode(text.substring(0, start) + replaceQuery + text.substring(end)); setTimeout(() => { ta.focus(); ta.setSelectionRange(start, start + replaceQuery.length); }, 50); return; }
    }
    const idx = text.indexOf(searchQuery);
    if (idx !== -1) { updateCode(text.substring(0, idx) + replaceQuery + text.substring(idx + searchQuery.length)); } else showToast('Not found.');
  };

  const generateRichTextHtml = (latexCode) => {
    let body = latexCode.replace(/\\documentclass.*?\n/g, '').replace(/\\usepackage.*?\n/g, '').replace(/\\title\{.*?\}/g, '').replace(/\\author\{.*?\}/g, '').replace(/\\date\{.*?\}/g, '').replace(/\\begin\{document\}/g, '').replace(/\\end\{document\}/g, '').replace(/\\maketitle/g, '');
    const titleMatch = latexCode.match(/\\title\{([^}]+)\}/);
    const authorMatch = latexCode.match(/\\author\{([^}]+)\}/);
    const sections = body.split(/\\section\{([^}]+)\}/);
    let html = '';
    if (titleMatch) html += `<h1 class="text-3xl font-bold mb-2">${titleMatch[1].replace(/\\\\/g, ' ')}</h1>`;
    if (authorMatch) html += `<p class="text-sm text-slate-500 mb-8 italic">By ${authorMatch[1]}</p>`;
    const parse = (text) => text.replace(/\\hl\{([^}]+)\}/g, '<span class="bg-yellow-200/50 px-1.5 rounded">$1</span>');
    if (sections[0]?.trim()) html += `<p class="mb-6">${parse(sections[0].trim())}</p>`;
    for (let i = 1; i < sections.length; i += 2) {
      html += `<h2 class="text-xl font-bold mb-3">${sections[i]}</h2>`;
      (sections[i + 1] || '').split('\n\n').filter(p => p.trim()).forEach(p => { html += `<p class="mb-6">${parse(p.trim())}</p>`; });
    }
    return html;
  };

  const parseHtmlToLatex = (container) => {
    let latex = `\\documentclass{article}\n\\usepackage[utf8]{inputenc}\n\\usepackage{xcolor}\n\\usepackage{soul}\n\n`;
    Array.from(container.children).forEach(child => {
      if (child.tagName === 'H1') latex += `\\title{${child.innerText}}\n`;
      else if (child.tagName === 'P' && child.innerText.startsWith('By ')) latex += `\\author{${child.innerText.substring(3)}}\n\\date{\\today}\n\n\\begin{document}\n\n\\maketitle\n\n`;
      else if (child.tagName === 'H2') latex += `\\section{${child.innerText}}\n\n`;
      else if (child.tagName === 'P') {
        let text = child.innerHTML.replace(/<span[^>]*>(.*?)<\/span>/g, '\\hl{$1}').replace(/&nbsp;/g, ' ');
        text = text.replace(/<br\s*\/?>/gi, '\n').replace(/<[^>]*>?/gm, '');
        if (text.trim()) latex += `${text}\n\n`;
      }
    });
    return latex.trim();
  };

  const renderModalPaperPdf = (paperName) => {
    const dbPaper = papers.find(p => p.filename === paperName || p.name === paperName);
    const content = dbPaper?.content || '';
    if (!content) return <div className="text-center py-8 text-xs text-slate-400 italic">No content.</div>;
    const pages = content.split(/\\newpage|\\clearpage/);
    return pages.map((pageContent, pageIndex) => {
      const titleMatch = pageContent.match(/\\title\{([^}]+)\}/);
      const authorMatch = pageContent.match(/\\author\{([^}]+)\}/);
      let body = pageContent.replace(/\\documentclass.*?\n/g, '').replace(/\\usepackage.*?\n/g, '').replace(/\\title\{.*?\}/g, '').replace(/\\author\{.*?\}/g, '').replace(/\\date\{.*?\}/g, '').replace(/\\begin\{document\}/g, '').replace(/\\end\{document\}/g, '').replace(/\\maketitle/g, '');
      const sections = body.split(/\\section\{([^}]+)\}/);
      const elements = [];
      const parse = (text) => text.split(/\\hl\{([^}]+)\}/g).map((part, idx) => idx % 2 === 1 ? <span key={idx} className="bg-yellow-100 px-1 rounded-sm border-b border-yellow-300 font-bold">{part}</span> : part);
      if (titleMatch || authorMatch) elements.push(<div key="hdr" className="text-center mb-6">{titleMatch && <h1 className="text-lg font-bold font-serif">{titleMatch[1]}</h1>}{authorMatch && <p className="text-xs text-slate-500">{authorMatch[1]}</p>}</div>);
      if (sections[0]?.trim()) elements.push(<p key="intro" className="text-[12px] mb-4 leading-relaxed font-serif text-justify">{parse(sections[0].trim())}</p>);
      for (let i = 1; i < sections.length; i += 2) {
        const st = sections[i], sc = sections[i + 1] || '';
        elements.push(<h2 key={`h2-${i}`} className="font-bold text-xs mb-2 text-indigo-700 font-serif uppercase tracking-wider mt-3 border-b border-slate-100 pb-1">{st}</h2>);
        sc.split('\n\n').filter(p => p.trim()).forEach((p, pi) => elements.push(<p key={`p-${i}-${pi}`} className="text-[11px] mb-3 leading-relaxed text-slate-600 font-serif text-justify">{parse(p.trim())}</p>));
      }
      return <div key={pageIndex} className="bg-white border border-slate-200/80 shadow-sm rounded-xl p-5 mb-4 max-h-[350px] overflow-y-auto custom-scrollbar font-serif select-text">{elements}</div>;
    });
  };

  const tourSteps = useMemo(() => [
    { element: '[data-tour="header-project-name"]', popover: { title: t('tour.projectName'), description: t('tour.projectNameDesc'), side: 'bottom' } },
    { element: '[data-tour="header-history"]', popover: { title: t('tour.versionHistory'), description: t('tour.versionHistoryDesc'), side: 'bottom' } },
    { element: '[data-tour="header-ai-review"]', popover: { title: t('tour.aiReview'), description: t('tour.aiReviewDesc'), side: 'bottom' } },
    { element: '[data-tour="sidebar-left"]', popover: { title: t('tour.sidebarLeft'), description: t('tour.sidebarLeftDesc'), side: 'right' } },
    { element: '[data-tour="file-panel"]', popover: { title: t('tour.filePanel'), description: t('tour.filePanelDesc'), side: 'right' } },
    { element: '[data-tour="editor-toolbar"]', popover: { title: t('tour.editorToolbar'), description: t('tour.editorToolbarDesc'), side: 'bottom' } },
    { element: '[data-tour="editor-section-name"]', popover: { title: t('tour.editorSectionName'), description: t('tour.editorSectionNameDesc'), side: 'bottom' } },
    { element: '[data-tour="context-panel"]', popover: { title: t('tour.contextPanel'), description: t('tour.contextPanelDesc'), side: 'left' } },
    { element: '[data-tour="context-claims-tab"]', popover: { title: t('tour.claims'), description: t('tour.claimsDesc'), side: 'left' } },
    { element: '[data-tour="context-feedback-tab"]', popover: { title: t('tour.feedback'), description: t('tour.feedbackDesc'), side: 'left' } },
    { element: '[data-tour="context-graph-tab"]', popover: { title: t('tour.graph'), description: t('tour.graphDesc'), side: 'left' } },
    { element: '[data-tour="header-dark-mode"]', popover: { title: t('tour.darkMode'), description: t('tour.darkModeDesc'), side: 'bottom' } },
    { element: '[data-tour="header-language"]', popover: { title: t('tour.language'), description: t('tour.languageDesc'), side: 'bottom' } },
  ], [t]);
  const blockingClaimAlerts = claims
    .filter(claim => claim.contentStatus && claim.contentStatus !== 'PRESENT')
    .map(claim => ({ claimId: claim.id, type: claim.contentStatus }));

  return (
    <div className="h-screen w-full flex flex-col bg-(--surface-secondary) overflow-hidden font-sans antialiased text-(--text-primary)">
      <WorkspaceHeader project={project} navigate={navigate} selectedPaper={selectedPaper} handleRunAiReview={handleRunAiReview} loadingAiReview={loadingAiReview} isLocked={isLocked} onShowHistory={() => setShowHistoryModal(true)} historyDisabled={assignedSections.length === 0}
        notifications={notifications} unreadCount={unreadCount} showNotifications={showNotifications} setShowNotifications={setShowNotifications} onMarkNotificationRead={handleMarkNotificationRead}
        showExportMenu={showExportMenu} setShowExportMenu={setShowExportMenu} handleExportTexArchive={handleExportTexArchive} handleExportTraceabilityJson={handleExportTraceabilityJson} handleExportTraceabilityCsv={handleExportTraceabilityCsv} handleExportGraphCsv={handleExportCsv} />

      <div id="workspace-container" className="flex-1 flex overflow-hidden">
        <div data-tour="sidebar-left" className="w-14 bg-indigo-900 dark:bg-(--accent-bar) flex flex-col items-center py-4 shrink-0 z-20 border-r border-indigo-950 dark:border-(--border) shadow-[2px_0_8px_-2px_rgba(0,0,0,0.2)]">
          <button onClick={() => setIsFileTreeOpen(!isFileTreeOpen)} className="w-full flex justify-center relative cursor-pointer mb-6 group outline-none" title="Toggle File Sidebar">
            <div className={`absolute left-0 top-0 bottom-0 w-1 rounded-r-md transition-colors ${isFileTreeOpen ? 'bg-white shadow-[0_0_8px_rgba(255,255,255,0.8)]' : 'bg-transparent'}`}></div>
            <svg className={`w-[22px] h-[22px] transition-colors ${isFileTreeOpen ? 'text-white' : 'text-indigo-300 group-hover:text-white'}`} fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" /></svg>
          </button>
          <div onClick={() => setShowOverview(!showOverview)} className="w-full flex justify-center cursor-pointer mb-6 group relative" title="Overview">
            <div className={`absolute left-0 top-0 bottom-0 w-1 rounded-r-md transition-colors ${showOverview ? 'bg-indigo-400' : 'bg-transparent'}`}></div>
            <svg className={`w-[22px] h-[22px] transition-colors ${showOverview ? 'text-white' : 'text-indigo-300 group-hover:text-white'}`} fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zm10 0a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zm10 0a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z" /></svg>
          </div>
          <div onClick={() => setIsDrawerOpen(!isDrawerOpen)} className="w-full flex justify-center cursor-pointer mb-6 group relative" title="Toggle Right Drawer">
            <div className={`absolute left-0 top-0 bottom-0 w-1 rounded-r-md transition-colors ${isDrawerOpen ? 'bg-indigo-400' : 'bg-transparent'}`}></div>
            <svg className={`w-[22px] h-[22px] transition-colors ${isDrawerOpen ? 'text-white' : 'text-indigo-300 group-hover:text-white'}`} fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
          </div>
        </div>

        <FilePanel isOpen={isFileTreeOpen} width={fileTreeWidth} onResizeStart={handleLeftDividerMouseDown} sections={sections} assignedSections={assignedSections} selectedSectionId={selectedSectionId} onSelectSection={(sec) => { setSelectedSectionId(sec.id); loadCode(sec.contentTex || ''); }} selectedPaper={selectedPaper} onSelectPaper={(p) => { setSelectedPaper(p); setShowHistoryModal(false); loadCode(p.extractedText || ''); }} papers={papers} onUploadPaper={isLocked ? undefined : handleUploadPaper} sources={sources} onUploadSource={isLocked ? undefined : handleUploadSource} onDeleteSource={handleDeleteSource} mediaAssets={mediaAssets} onUploadMedia={isLocked ? undefined : handleUploadMedia} onDeleteMedia={handleDeleteMedia} onInsertMedia={canEditCurrentSection ? handleInsertMedia : undefined} showToast={showToast} isLocked={isLocked} />

        <EditorPanel editorRef={editorRef} selectedPaper={selectedPaper} selectedSectionId={selectedSectionId} assignedSections={assignedSections} canEditCurrentSection={canEditCurrentSection} currentSection={currentSection} displayContent={displayContent} updateCode={isLocked ? undefined : updateCode} editorWidth={editorWidth} onEditorResizeStart={handleMouseDown} saveStatus={saveStatus} lastSaved={lastSaved} handleSaveDraft={handleSaveDraft} handleScanCitations={handleScanCitations} insertLatexTag={insertLatexTag} insertSymbol={insertSymbol} handleFindReplace={handleFindReplace} handleDownloadTex={handleDownloadTex} showSymbolMenu={showSymbolMenu} setShowSymbolMenu={setShowSymbolMenu} showTextSizeMenu={showTextSizeMenu} setShowTextSizeMenu={setShowTextSizeMenu} showSearchPanel={showSearchPanel} setShowSearchPanel={setShowSearchPanel} searchQuery={searchQuery} setSearchQuery={setSearchQuery} replaceQuery={replaceQuery} setReplaceQuery={setReplaceQuery} textSize={textSize} setTextSize={setTextSize} showToast={showToast} mediaAssets={mediaAssets} isLocked={isLocked} />

        <ContextPanel isOpen={isDrawerOpen} width={rightDrawerWidth} onResizeStart={handleRightDividerMouseDown} activeTab={activeTab} setActiveTab={(tab) => { setActiveTab(tab); localStorage.setItem('student_workspace_active_tab', tab); }} showToast={showToast}
          sources={sources} isUploading={isUploading} setIsUploading={setIsUploading} project={project} setViewerFile={setViewerFile} fetchSources={fetchSources} isLocked={isLocked}
          newClaimContent={newClaimContent} onNewClaimContentChange={handleNewClaimContentChange} newClaimFunctionalType={newClaimFunctionalType} setNewClaimFunctionalType={setNewClaimFunctionalType} claimEvaluation={claimEvaluation} evaluatingClaim={evaluatingClaim} claimEvaluationError={claimEvaluationError} handleEvaluateClaim={handleEvaluateClaim} canAddEvaluatedClaim={canAddEvaluatedClaim} handleCreateClaim={handleCreateClaim} handleUseSelectedText={handleUseSelectedText} canCreateClaim={canEditCurrentSection}
          claims={claims} selectedClaim={selectedClaim} claimMatches={claimMatches} claimMappings={claimMappings} loadingMatches={loadingMatches}
          claimCandidates={claimCandidates} loadingCandidates={loadingCandidates} evaluatingChunkId={evaluatingChunkId} updatingSuggestionId={updatingSuggestionId}
          handleSearchClaimMatches={handleSearchClaimMatches} handleEvaluateMatch={handleEvaluateMatch} handleSuggestionStatus={handleSuggestionStatus} canEditClaim={canEditClaim}
          editingClaim={editingClaim} setEditingClaim={setEditingClaim} editClaimContent={editClaimContent} setEditClaimContent={setEditClaimContent} editClaimFunctionalType={editClaimFunctionalType} setEditClaimFunctionalType={setEditClaimFunctionalType} handleDeleteClaim={handleDeleteClaim} handleUpdateClaim={handleUpdateClaim}
          onSelectClaim={handleSelectClaim}
          feedbacks={feedbacks} setShowSubmitReviewModal={setShowSubmitReviewModal} userProjectRole={project?.currentUserRole}
          graphData={graphData} graphScope={graphScope} onGraphScopeToggle={handleGraphScopeToggle} claimStats={claimStats} />
      </div>

      {/* Version History Modal */}
      {showHistoryModal && (
        <div data-tour="history-modal" className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-sm animate-in fade-in duration-200">
          <div className="bg-(--surface) rounded-xl shadow-2xl w-full max-w-lg overflow-hidden flex flex-col max-h-[85vh] animate-in zoom-in-95 duration-200">
            <div className="flex justify-between items-center px-6 py-4 border-b border-(--border-light) shrink-0">
              <h2 className="text-base font-bold text-(--text-primary) flex items-center gap-2">
                <svg className="w-4 h-4 text-indigo-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
                Version History
              </h2>
              <button onClick={() => setShowHistoryModal(false)} className="text-(--text-tertiary) hover:text-(--text-secondary) transition-colors p-1 rounded-lg hover:bg-(--surface-tertiary)">
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" /></svg>
              </button>
            </div>
            <div className="flex-1 overflow-y-auto p-5 space-y-4">
              {assignedSections.length === 0 ? (
                <div className="text-xs text-(--text-tertiary) italic text-center py-8">No section is assigned to you.</div>
              ) : (() => {
                const sec = sections.find(s => String(s.id) === String(selectedSectionId)) || assignedSections[0];
                return sec ? (
                  <>
                    {sec.previousContentTex && (
                      <div className="border border-(--border) rounded-xl p-4 bg-(--surface-secondary)/50 hover:border-amber-300 transition-colors">
                        <div className="flex items-start justify-between mb-2">
                          <div>
                            <span className="text-[10px] font-bold text-amber-700 bg-amber-50 dark:bg-amber-900/30 px-2 py-0.5 rounded-full border border-amber-200">Version {sec.version}</span>
                            <p className="text-[10px] text-(--text-tertiary) mt-1.5">Updated at: {sec.updatedAt ? new Date(sec.updatedAt).toLocaleString('en-US', { month: 'short', day: 'numeric', hour: 'numeric', minute: '2-digit' }) : 'Unknown'}</p>
                          </div>
                        </div>
                        <p className="text-[11px] text-(--text-secondary) leading-relaxed font-mono bg-(--surface) rounded-lg p-2.5 border border-(--border-light)">{(sec.previousContentTex || '').substring(0, 140)}{(sec.previousContentTex || '').length > 140 ? '...' : ''}</p>
                        <button onClick={handleRollbackSection.bind(null, sec.id)} disabled={rollingBack} className="mt-3 w-full bg-amber-50 hover:bg-amber-100 text-amber-700 border border-amber-200 text-xs font-bold px-3 py-2 rounded-lg transition-colors flex items-center justify-center gap-1.5 disabled:opacity-50 cursor-pointer">
                          {rollingBack ? (
                            <span className="flex items-center gap-1.5"><span className="w-3 h-3 border-2 border-amber-500 border-t-transparent rounded-full animate-spin"></span> Restoring...</span>
                          ) : (
                            <><svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3 10h10a5 5 0 015 5v2a5 5 0 01-5 5H6m0-10l4-4m-4 4l4 4" /></svg> Restore this version</>
                          )}
                        </button>
                      </div>
                    )}
                    <div className="border border-indigo-200 dark:border-indigo-800 rounded-xl p-4 bg-indigo-50/30 dark:bg-indigo-900/10">
                      <div className="flex items-start justify-between mb-2">
                        <div>
                          <span className="text-[10px] font-bold text-indigo-700 bg-indigo-50 dark:bg-indigo-900/30 px-2 py-0.5 rounded-full border border-indigo-200 dark:border-indigo-800">Version {sec.version + 1} (current)</span>
                          <p className="text-[10px] text-(--text-tertiary) mt-1.5">Updated at: {sec.updatedAt ? new Date(sec.updatedAt).toLocaleString('en-US', { month: 'short', day: 'numeric', hour: 'numeric', minute: '2-digit' }) : 'Unknown'}</p>
                        </div>
                      </div>
                      <p className="text-[11px] text-(--text-secondary) leading-relaxed font-mono bg-(--surface) rounded-lg p-2.5 border border-(--border-light)">{(sec.contentTex || '').substring(0, 140)}{(sec.contentTex || '').length > 140 ? '...' : ''}</p>
                      <div className="mt-2 text-[10px] text-(--text-tertiary) italic flex items-center gap-1"><svg className="w-3 h-3 text-indigo-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7" /></svg> This is the current version being edited.</div>
                    </div>
                  </>
                ) : (
                  <div className="text-xs text-(--text-tertiary) italic text-center py-8">Select a section to see version history.</div>
                );
              })()}
            </div>
            <div className="px-6 py-3 border-t border-(--border-light) flex justify-end shrink-0 bg-(--surface-secondary)/50">
              <button onClick={() => setShowHistoryModal(false)} className="text-xs font-semibold text-(--text-secondary) hover:text-(--text-primary) px-4 py-1.5 rounded-lg hover:bg-(--surface-tertiary) transition-colors cursor-pointer border border-(--border) bg-(--surface)">Close</button>
            </div>
          </div>
        </div>
      )}

      {/* Overview — Sections accordion, assigned sections, preview button */}
      {showOverview && (
        <div className="absolute left-14 top-14 bottom-0 w-72 bg-(--surface) border-r border-(--border) shadow-xl z-30 animate-in slide-in-from-left duration-200 flex flex-col overflow-hidden">
          <div className="flex items-center justify-between px-4 py-3 border-b border-(--border) shrink-0">
            <h2 className="text-sm font-bold text-(--text-primary)">{t('sections')}</h2>
            <button onClick={() => setShowOverview(false)} className="text-(--text-tertiary) hover:text-(--text-secondary) transition-colors">
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" /></svg>
            </button>
          </div>
          <div className="flex-1 overflow-y-auto p-3 space-y-1">
            {assignedSections.length > 0 && (
              <>
                <button onClick={() => setAssignedExpanded(!assignedExpanded)}
                  className="w-full flex items-center justify-between px-2 py-1.5 text-[10px] font-bold text-(--text-secondary) tracking-wider uppercase cursor-pointer hover:bg-(--surface-secondary) rounded-lg">
                  <span className="flex items-center gap-1.5">{assignedExpanded ? '▼' : '▶'} {t('assignedToYou')} ({assignedSections.length})</span>
                </button>
                {assignedExpanded && (
                  <div className="space-y-1 pl-1">
                    {assignedSections.map(sec => (
                      <div key={sec.id}
                        onClick={() => { setSelectedSectionId(sec.id); loadCode(sec.contentTex || ''); setShowOverview(false); }}
                        className="flex items-center justify-between p-2.5 rounded-lg text-xs border cursor-pointer bg-emerald-50 dark:bg-emerald-900/20 border-emerald-200 dark:border-emerald-800">
                        <div className="flex items-center gap-2 truncate min-w-0">
                          <span className="w-2 h-2 rounded-full bg-emerald-500 shrink-0"></span>
                          <span className="truncate font-medium text-(--text-primary)">{sec.sectionTitle || 'Untitled'}</span>
                          <span className="text-[9px] text-(--text-tertiary) font-mono shrink-0">#{sec.sectionOrder}</span>
                        </div>
                        <span className="text-[9px] font-bold text-indigo-600 bg-indigo-50 dark:bg-indigo-900/30 px-1.5 py-0.5 rounded">v{sec.version || 1}</span>
                      </div>
                    ))}
                  </div>
                )}
              </>
            )}
            <button onClick={() => setSectionsExpanded(!sectionsExpanded)}
              className="w-full flex items-center justify-between px-2 py-1.5 text-[10px] font-bold text-(--text-secondary) tracking-wider uppercase cursor-pointer hover:bg-(--surface-secondary) rounded-lg">
              <span className="flex items-center gap-1.5">{sectionsExpanded ? '▼' : '▶'} {t('sections')} ({sections.length})</span>
            </button>
            {sectionsExpanded && (
              <div className="space-y-1 pl-1">
                {sections.length === 0 ? (
                  <p className="text-xs text-(--text-tertiary) italic text-center py-4">{t('noSections')}</p>
                ) : (
                  sections.map(sec => {
                    const isMySection = assignedSections.some(s => String(s.id) === String(sec.id));
                    return (
                      <div key={sec.id}
                        onClick={() => { setSelectedSectionId(sec.id); loadCode(sec.contentTex || ''); setShowOverview(false); }}
                        className={`flex items-center justify-between p-2.5 rounded-lg text-xs border cursor-pointer transition-all ${isMySection ? 'bg-emerald-50 dark:bg-emerald-900/20 border-emerald-200 dark:border-emerald-800' : 'bg-(--surface-secondary) border-(--border) hover:bg-(--surface-tertiary)'}`}>
                        <div className="flex items-center gap-2 truncate min-w-0">
                          {isMySection && <span className="w-2 h-2 rounded-full bg-emerald-500 shrink-0" title={t('assignedToYou')}></span>}
                          <span className="truncate font-medium text-(--text-primary)">{sec.sectionTitle || 'Untitled'}</span>
                          <span className="text-[9px] text-(--text-tertiary) font-mono shrink-0">#{sec.sectionOrder}</span>
                        </div>
                        <div className="flex items-center gap-1.5 shrink-0">
                          <span className="text-[9px] font-bold text-indigo-600 bg-indigo-50 dark:bg-indigo-900/30 px-1.5 py-0.5 rounded">v{sec.version || 1}</span>
                        </div>
                      </div>
                    );
                  })
                )}
              </div>
            )}
          </div>
          <div className="px-3 py-3 border-t border-(--border) shrink-0">
            <button onClick={() => setShowFullPaperPreview(true)} disabled={sections.length === 0}
              className="w-full bg-indigo-600 hover:bg-indigo-700 disabled:opacity-40 text-white text-xs font-bold px-3 py-2 rounded-lg transition-colors flex items-center justify-center gap-2">
              <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" /><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" /></svg>
              {t('previewFullPaper')}
            </button>
          </div>
        </div>
      )}

      {/* Revise Modal */}
      {showReviseModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-sm animate-in fade-in duration-200">
          <div className="bg-(--surface) rounded-xl shadow-2xl w-full max-w-md p-6 transform transition-all">
            <div className="flex justify-between items-center mb-4">
              <h2 className="text-lg font-bold text-(--text-primary)">Auto Revise</h2>
              <button onClick={() => setShowReviseModal(false)} className="text-(--text-tertiary) hover:text-(--text-secondary) transition-colors">
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" /></svg>
              </button>
            </div>
            <p className="text-sm text-(--text-secondary) mb-4">Select sections for AI revision based on instructor feedback.</p>
            <div className="space-y-2 mb-4 max-h-60 overflow-y-auto">
              {sections.map(sec => (
                <label key={sec.id} className="flex items-center gap-3 p-2.5 border border-(--border) rounded-lg cursor-pointer hover:bg-(--surface-secondary) transition-colors">
                  <input type="checkbox" className="w-4 h-4 text-indigo-600 rounded border-(--border) focus:ring-indigo-500" defaultChecked />
                  <span className="text-sm font-medium text-(--text-primary)">{sec.sectionTitle} <span className="text-[10px] text-(--text-tertiary)">v{sec.version || 1}</span></span>
                </label>
              ))}
              {sections.length === 0 && <div className="text-xs text-(--text-tertiary) italic text-center py-4">No sections available.</div>}
            </div>
            <div className="flex justify-end gap-3">
              <button onClick={() => setShowReviseModal(false)} className="px-4 py-2 text-sm font-semibold text-(--text-secondary) hover:bg-(--surface-tertiary) rounded-lg transition-colors">Cancel</button>
              <button onClick={async () => {
                if (!selectedPaper) { showToast('Select a paper first.'); return; }
                setShowReviseModal(false);
                handleRunAiReview();
              }} className="px-4 py-2 text-sm font-bold text-white bg-indigo-600 hover:bg-indigo-700 rounded-lg shadow-sm shadow-indigo-200 transition-colors">Start Revision</button>
            </div>
          </div>
        </div>
      )}

      {/* Submit Review Modal */}
      {showSubmitReviewModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-sm animate-in fade-in duration-200">
          <div className="bg-(--surface) rounded-xl shadow-2xl w-full max-w-md p-6 transform transition-all">
            <div className="flex justify-between items-center mb-4">
              <h2 className="text-lg font-bold text-(--text-primary)">{t('submitReview')}</h2>
              <button onClick={() => setShowSubmitReviewModal(false)} className="text-(--text-tertiary) hover:text-(--text-secondary) transition-colors">
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" /></svg>
              </button>
            </div>
            <p className="text-sm text-(--text-secondary) mb-6 leading-relaxed">
              This will seal your entire paper for the instructor to review. No further edits will be possible until the instructor returns it. Are you sure?
            </p>
            {blockingClaimAlerts.length > 0 && (
              <div className="mb-5 rounded-lg border border-amber-200 bg-amber-50 p-3 text-xs text-amber-900">
                <p className="font-bold">Submission blocked: {blockingClaimAlerts.length} claim{blockingClaimAlerts.length > 1 ? 's are' : ' is'} missing from the owning section.</p>
                <ul className="mt-2 list-disc space-y-1 pl-4">
                  {blockingClaimAlerts.slice(0, 5).map(alert => <li key={`${alert.claimId}-${alert.type}`}>{alert.claimId}: {alert.type}</li>)}
                </ul>
              </div>
            )}
            <div className="flex justify-end gap-3">
              <button onClick={() => setShowSubmitReviewModal(false)} className="px-4 py-2 text-sm font-semibold text-(--text-secondary) hover:bg-(--surface-tertiary) rounded-lg transition-colors">{t('cancel')}</button>
              <button onClick={handleSubmitReview} disabled={blockingClaimAlerts.length > 0} className="px-4 py-2 text-sm font-bold text-white bg-indigo-600 hover:bg-indigo-700 disabled:cursor-not-allowed disabled:opacity-40 rounded-lg shadow-sm shadow-indigo-200 transition-colors">{t('submitReview')}</button>
            </div>
          </div>
        </div>
      )}

      {/* AI Review Modal */}
      {showAiReviewModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-sm animate-in fade-in duration-200">
          <div className="bg-(--surface) rounded-2xl shadow-2xl w-full max-w-2xl overflow-hidden flex flex-col max-h-[85vh] animate-in zoom-in-95 duration-200">
            <div className="bg-indigo-900 dark:bg-(--accent-bar) text-white px-6 py-4 flex justify-between items-center shrink-0">
              <div className="flex items-center gap-2">
                <svg className="w-5 h-5 text-indigo-300 animate-pulse" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 01-2 2h0a2 2 0 01-2-2v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" /></svg>
                <h2 className="text-base font-bold tracking-wide">AI Review Report</h2>
              </div>
              <button onClick={() => setShowAiReviewModal(false)} className="text-indigo-200 hover:text-white transition-colors">
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" /></svg>
              </button>
            </div>
            <div className="flex-1 overflow-y-auto p-6 bg-(--surface-secondary)/50 space-y-6 custom-scrollbar">
              {aiReviewError && !loadingAiReview && (
                <div className="rounded-xl border border-rose-200 bg-rose-50 p-4 text-rose-800">
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <p className="text-xs font-bold">{aiReviewError.status ? `AI Review error ${aiReviewError.status}` : 'AI Review error'}</p>
                      <p className="mt-1 text-[11px]">{aiReviewError.message}</p>
                      {aiReviewResult && <p className="mt-1 text-[10px] text-rose-600">The last successful review remains below.</p>}
                    </div>
                    <button onClick={handleRunAiReview} className="shrink-0 rounded-lg bg-rose-700 px-3 py-1.5 text-[10px] font-bold text-white hover:bg-rose-800">Retry</button>
                  </div>
                </div>
              )}
              {loadingAiReview ? (
                <div className="flex flex-col items-center justify-center py-16 space-y-4">
                  <div className="w-10 h-10 border-4 border-indigo-200 border-t-indigo-600 rounded-full animate-spin"></div>
                  <div className="text-center">
                    <p className="text-sm font-bold text-(--text-primary)">AI is analyzing...</p>
                    <p className="text-xs text-(--text-tertiary) mt-1">Scanning every Section chunk, stored Claim, active Source mapping, and instructor feedback...</p>
                  </div>
                </div>
              ) : aiReviewResult ? (
                <>
                  <div className="bg-(--surface) border border-(--border) rounded-xl p-5 shadow-sm hover:border-indigo-200 transition-colors">
                    <div className="flex justify-between items-start mb-3">
                      <h3 className="text-sm font-bold text-(--text-primary) flex items-center gap-1.5"><span className="w-1.5 h-3 bg-indigo-600 rounded"></span>Project assessment</h3>
                      <span className={`border text-[10px] font-bold px-2 py-0.5 rounded ${aiReviewResult.direction === 'ON_TRACK' ? 'bg-emerald-50 text-emerald-700 border-emerald-200' : aiReviewResult.direction === 'NEEDS_ATTENTION' ? 'bg-amber-50 text-amber-700 border-amber-200' : 'bg-slate-50 text-slate-600 border-slate-200'}`}>{(aiReviewResult.direction || 'UNKNOWN').replaceAll('_', ' ')}</span>
                    </div>
                    <p className="text-xs text-(--text-secondary) leading-relaxed bg-(--surface-secondary) p-3.5 rounded-lg border border-(--border-light)">{aiReviewResult.summary}</p>
                    {aiReviewResult.rubricScore != null && (
                      <div className={`mt-3 rounded-xl border p-3.5 flex items-center justify-between ${aiReviewResult.passes ? 'border-emerald-200 bg-emerald-50' : 'border-amber-200 bg-amber-50'}`}>
                        <div>
                          <p className="text-[10px] font-bold uppercase tracking-wider text-(--text-tertiary)">Rubric Score</p>
                          <p className={`text-lg font-black ${aiReviewResult.passes ? 'text-emerald-700' : 'text-amber-700'}`}>{aiReviewResult.rubricScore.toFixed(1)} / 5.0</p>
                        </div>
                        <span className={`text-[10px] font-black px-2.5 py-1 rounded-full border ${aiReviewResult.passes ? 'bg-emerald-600 text-white border-emerald-600' : 'bg-amber-500 text-white border-amber-500'}`}>
                          {aiReviewResult.passes ? 'PASS' : 'REVISE'}
                        </span>
                      </div>
                    )}
                    {aiReviewResult.coverage && (
                      <div className="mt-3 grid grid-cols-3 gap-2 text-center text-[10px]">
                        <div className="rounded-lg bg-indigo-50 p-2 text-indigo-700"><strong>{aiReviewResult.coverage.sectionsScanned}/{aiReviewResult.coverage.totalSections}</strong><br />Sections</div>
                        <div className="rounded-lg bg-indigo-50 p-2 text-indigo-700"><strong>{aiReviewResult.coverage.chunksScanned}/{aiReviewResult.coverage.totalChunks}</strong><br />Chunks</div>
                        <div className="rounded-lg bg-indigo-50 p-2 text-indigo-700"><strong>{aiReviewResult.coverage.claimsChecked}/{aiReviewResult.coverage.totalClaims}</strong><br />Claims</div>
                      </div>
                    )}
                  </div>
                  {(aiReviewResult.findings || []).map((finding, index) => (
                    <button key={`${finding.claimId || 'general'}-${index}`} type="button" onClick={() => {
                      const claim = claims.find(item => String(item.id) === String(finding.claimId));
                      if (claim) {
                        handleSelectClaim(claim);
                        setActiveTab('Claims');
                        setShowAiReviewModal(false);
                      }
                    }} className="w-full text-left bg-(--surface) border border-(--border) rounded-xl p-5 shadow-sm hover:border-indigo-300 focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-colors">
                      <div className="flex justify-between items-start gap-3 mb-2">
                        <h3 className="text-sm font-bold text-(--text-primary)">{(finding.type || 'OTHER').replaceAll('_', ' ')}</h3>
                        <span className="flex items-center gap-1.5 shrink-0">
                          {finding.score != null && <span className="text-[10px] font-black px-2 py-0.5 rounded bg-slate-900 text-white">{finding.score}/5</span>}
                          <span className={`text-[10px] font-bold px-2 py-0.5 rounded border ${finding.severity === 'CRITICAL' ? 'bg-rose-50 text-rose-700 border-rose-200' : finding.severity === 'WARNING' ? 'bg-amber-50 text-amber-700 border-amber-200' : 'bg-indigo-50 text-indigo-700 border-indigo-200'}`}>{finding.severity}</span>
                        </span>
                      </div>
                      <p className="text-xs text-(--text-secondary) leading-relaxed">{finding.message}</p>
                      {finding.excerpt && <blockquote className="mt-2 border-l-2 border-indigo-300 pl-2 text-[11px] italic text-(--text-tertiary)">“{finding.excerpt}”</blockquote>}
                      <p className="mt-2 text-xs font-semibold text-indigo-700">Next: {finding.recommendedAction}</p>
                      <div className="mt-3 flex flex-wrap gap-2 text-[9px] text-(--text-tertiary)">
                        {finding.claimId && <span>Claim {finding.claimId}</span>}
                        {finding.sourceIds?.length > 0 && <span>{finding.sourceIds.length} source{finding.sourceIds.length > 1 ? 's' : ''}</span>}
                        {finding.feedbackIds?.length > 0 && <span>{finding.feedbackIds.length} feedback item{finding.feedbackIds.length > 1 ? 's' : ''}</span>}
                      </div>
                    </button>
                  ))}
                  {(aiReviewResult.findings || []).length === 0 && (
                    <div className={`rounded-xl border p-4 text-xs ${aiReviewResult.direction === 'INSUFFICIENT_DATA' ? 'border-slate-200 bg-slate-50 text-slate-700' : 'border-emerald-200 bg-emerald-50 text-emerald-800'}`}>
                      <p>{aiReviewResult.direction === 'INSUFFICIENT_DATA'
                        ? 'Review not evaluated because the paper does not have enough processed content.'
                        : 'No specific findings were returned.'}</p>
                      {aiReviewResult.direction === 'INSUFFICIENT_DATA' && (
                        <button onClick={handleRunAiReview} className="mt-2 rounded-lg bg-slate-700 px-3 py-1.5 text-[10px] font-bold text-white hover:bg-slate-800">Retry</button>
                      )}
                    </div>
                  )}
                  {(aiReviewResult.limitations || []).length > 0 && (
                    <div className="rounded-xl border border-slate-200 bg-slate-50 p-4">
                      <h3 className="mb-2 text-xs font-bold text-slate-700">Limitations</h3>
                      <ul className="list-disc space-y-1 pl-4 text-[11px] text-slate-600">
                        {aiReviewResult.limitations.map((limitation, index) => <li key={index}>{limitation}</li>)}
                      </ul>
                    </div>
                  )}
                </>
              ) : null}
            </div>
            <div className="px-6 py-4 border-t border-(--border-light) bg-(--surface-secondary)/50 flex justify-end gap-3 shrink-0">
              <button onClick={() => setShowAiReviewModal(false)} className="px-4 py-2 text-xs font-semibold text-(--text-secondary) hover:bg-(--surface-tertiary) rounded-lg transition-colors border border-(--border) bg-(--surface) cursor-pointer">Close</button>
            </div>
          </div>
        </div>
      )}

      {citationResult && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-sm animate-in fade-in duration-200">
          <div className="bg-(--surface) rounded-2xl shadow-2xl w-full max-w-lg overflow-hidden flex flex-col max-h-[80vh] animate-in zoom-in-95 duration-200">
            <div className="flex justify-between items-center px-6 py-4 border-b border-(--border-light) bg-(--surface-secondary) shrink-0">
              <h2 className="text-sm font-bold text-(--text-primary) flex items-center gap-2">
                <svg className="w-4 h-4 text-indigo-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
                Format Scan
              </h2>
              <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${citationResult.findings?.length === 0 ? 'bg-emerald-50 text-emerald-700 border border-emerald-200' : 'bg-amber-50 text-amber-700 border border-amber-200'}`}>
                {citationResult.findings?.length === 0 ? 'ALL GOOD' : citationResult.findings?.length + ' ISSUES'}
              </span>
            </div>
            <div className="flex-1 overflow-y-auto p-6 space-y-3 text-xs">
              {(citationResult.findings || []).length === 0 ? (
                <div className="bg-emerald-50 border border-emerald-100 rounded-lg p-4 text-center">
                  <p className="text-emerald-700 font-bold">No issues found — paper looks good!</p>
                </div>
              ) : (
                (citationResult.findings || []).map((f, i) => {
                  const sevColor = f.severity === 'ERROR' ? 'border-l-rose-500 bg-rose-50' : f.severity === 'WARN' ? 'border-l-amber-500 bg-amber-50' : 'border-l-indigo-500 bg-indigo-50';
                  const sevLabel = f.severity === 'ERROR' ? 'Error' : f.severity === 'WARN' ? 'Warning' : 'Info';
                  return (
                    <div key={i} className={`border-l-4 ${sevColor} rounded-r-lg p-3 text-[11px]`}>
                      <div className="flex justify-between items-start mb-1">
                        <span className="font-bold text-(--text-primary) uppercase tracking-wider text-[10px]">{f.category}</span>
                        <span className={`text-[9px] font-bold px-1.5 py-0.5 rounded ${f.severity === 'ERROR' ? 'bg-rose-200 text-rose-800' : f.severity === 'WARN' ? 'bg-amber-200 text-amber-800' : 'bg-indigo-200 text-indigo-800'}`}>{sevLabel}</span>
                      </div>
                      {f.section && <div className="text-[10px] text-(--text-tertiary) mb-0.5 font-mono">{f.section}</div>}
                      <div className="text-(--text-primary) font-medium">{f.message}</div>
                      {f.suggestion && <div className="text-(--text-secondary) mt-1 italic">Tip: {f.suggestion}</div>}
                    </div>
                  );
                })
              )}
            </div>
            <div className="px-6 py-4 border-t border-(--border-light) bg-(--surface-secondary) flex justify-end shrink-0">
              <button onClick={() => setCitationResult(null)} className="px-4 py-2 text-xs font-semibold text-(--text-secondary) hover:bg-(--surface-tertiary) rounded-lg transition-colors border border-(--border) bg-(--surface) cursor-pointer">Close</button>
            </div>
          </div>
        </div>
      )}

      {viewerFile && <FileViewerModal fileUrl={viewerFile.fileUrl} fileName={viewerFile.fileName} onClose={() => setViewerFile(null)} />}

      {/* Paper Detail Modal */}
      {selectedPaperDetail && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-sm animate-in fade-in duration-200 p-4">
          <div className="bg-(--surface) rounded-2xl shadow-2xl w-full max-w-4xl overflow-hidden transform transition-all border border-(--border-light) flex flex-col h-[85vh]">
            <div className="px-6 py-4 border-b border-(--border-light) bg-(--surface-secondary) flex justify-between items-center shrink-0">
              <div className="flex items-center gap-2">
                <span className="text-[10px] font-black text-white px-2 py-0.5 rounded-full uppercase tracking-wider" style={{ backgroundColor: selectedPaperDetail.color }}>Paper #{selectedPaperDetail.num}</span>
                <span className="text-[10px] font-bold text-(--text-tertiary) font-mono">{selectedPaperDetail.name}</span>
              </div>
              <button onClick={() => setSelectedPaperDetail(null)} className="text-(--text-tertiary) hover:text-(--text-secondary) transition-colors p-1.5 hover:bg-(--surface-tertiary) rounded-lg">
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" /></svg>
              </button>
            </div>
            <div className="flex-1 flex overflow-hidden">
              <div className="w-1/2 p-6 overflow-y-auto custom-scrollbar space-y-4 border-r border-(--border)">
                <h3 className="text-base font-extrabold text-(--text-primary) leading-snug">{selectedPaperDetail.title}</h3>
                <p className="text-[10px] text-(--text-tertiary)">Created: {selectedPaperDetail.created}</p>
                <div className="flex gap-2 items-center">
                  <span className="text-xs font-bold text-(--text-secondary)">Category:</span>
                  <span className="text-[10px] font-bold px-2 py-0.5 rounded-md text-white shadow-sm" style={{ backgroundColor: selectedPaperDetail.color }}>{selectedPaperDetail.category}</span>
                </div>
                <div className="bg-(--surface-secondary) rounded-xl p-4 border border-(--border)/60">
                  <h4 className="text-[10px] font-bold text-(--text-tertiary) uppercase tracking-widest mb-1.5">Summary</h4>
                  <p className="text-xs text-(--text-secondary) leading-relaxed font-medium">{selectedPaperDetail.summary}</p>
                </div>
              </div>
              <div className="w-1/2 p-6 bg-(--surface-tertiary) flex flex-col overflow-hidden">
                <h4 className="text-[10px] font-black text-(--text-secondary) uppercase tracking-widest mb-3 flex items-center gap-1.5 shrink-0">
                  <svg className="w-3.5 h-3.5 text-red-500" fill="currentColor" viewBox="0 0 20 20"><path fillRule="evenodd" d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4z" clipRule="evenodd" /></svg>
                  PDF Preview
                </h4>
                <div className="flex-1 overflow-y-auto custom-scrollbar pr-1">{renderModalPaperPdf(selectedPaperDetail.name)}</div>
              </div>
            </div>
            <div className="px-6 py-4 border-t border-(--border-light) bg-(--surface-secondary) flex justify-end shrink-0">
              <button onClick={() => setSelectedPaperDetail(null)} className="px-4 py-2 text-xs font-bold text-white bg-indigo-600 hover:bg-indigo-700 rounded-lg shadow-md transition-colors">Close</button>
            </div>
          </div>
        </div>
      )}

      {toastMessage && (
        <div className="fixed bottom-5 right-5 z-[9999] bg-slate-900 text-white text-xs font-semibold px-4.5 py-3 rounded-xl shadow-2xl border border-slate-800 flex items-center gap-2.5 animate-in fade-in slide-in-from-bottom-5 duration-200">
          <span className="text-indigo-400">✦</span>
          <span>{toastMessage}</span>
        </div>
      )}

      <TourLauncher steps={tourSteps} tourKey="student-workspace" />

      {showFullPaperPreview && (
        <FullPaperPreview
          sections={sections}
          paperTitle={selectedPaper?.originalFilename || 'Paper'}
          mediaAssets={mediaAssets}
          onClose={() => setShowFullPaperPreview(false)}
        />
      )}
    </div>
  );
}
