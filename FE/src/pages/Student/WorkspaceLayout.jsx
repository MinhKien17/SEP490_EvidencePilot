import React, { useState, useRef, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useLanguage } from '../../context/LanguageContext';
import { UI_TEXT } from '../../constants/uiText';
import TourLauncher from '../../components/TourLauncher';
import FileViewerModal from '../../components/FileViewerModal';
import api from '../../api.js';
import { Client } from '@stomp/stompjs';
import WorkspaceHeader from './WorkspaceHeader.jsx';
import FilePanel from './FilePanel.jsx';
import EditorPanel from './EditorPanel.jsx';
import ContextPanel from './ContextPanel.jsx';

const TOUR_STEPS = [
  { element: '#project-selector', popover: { title: 'Select Project', description: 'Switch between your assigned projects.', side: 'bottom', align: 'start' } },
  { element: '#workspace-container', popover: { title: 'Workspace', description: 'Browse files, edit sections, and view context in one place.', side: 'top', align: 'center' } },
];

const DEFAULT_SAMPLE_LATEX = `% Select a paper from the file panel to start editing.`;

const RichTextEditor = React.memo(({ initialHtml, onHtmlChange }) => (
  <div className="flex-1 bg-white text-slate-800 p-8 overflow-y-auto leading-relaxed custom-scrollbar selection:bg-indigo-100 outline-none"
    contentEditable suppressContentEditableWarning
    onInput={(e) => onHtmlChange(e.target)}
    dangerouslySetInnerHTML={{ __html: initialHtml }} />
), () => true);

export default function WorkspaceLayout() {
  const { projectId } = useParams();
  const navigate = useNavigate();
  const { logout, user, role } = useAuth();
  const { language, toggleLanguage } = useLanguage();
  const [activeTab, setActiveTab] = useState(() => localStorage.getItem('student_workspace_active_tab') || 'Source');
  const [editorMode, setEditorMode] = useState('Code');
  const [showHistoryModal, setShowHistoryModal] = useState(false);
  const [showReviseModal, setShowReviseModal] = useState(false);
  const [toastMessage, setToastMessage] = useState('');

  const [project, setProject] = useState(null);
  const [projects, setProjects] = useState([]);
  const [sources, setSources] = useState([]);
  const [papers, setPapers] = useState([]);
  const [selectedPaper, setSelectedPaper] = useState(null);
  const [claims, setClaims] = useState([]);
  const [feedbacks, setFeedbacks] = useState([]);
  const [graphData, setGraphData] = useState(null);
  const [loadingProject, setLoadingProject] = useState(false);
  const [isUploading, setIsUploading] = useState(false);
  const [viewerFile, setViewerFile] = useState(null);
  const [currentUser, setCurrentUser] = useState(null);

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
  const [hoveredNodeId, setHoveredNodeId] = useState(null);

  const [showSubmitReviewModal, setShowSubmitReviewModal] = useState(false);
  const [showAiReviewModal, setShowAiReviewModal] = useState(false);
  const [selectedInstructorId, setSelectedInstructorId] = useState('');
  const [instructorsList, setInstructorsList] = useState([]);
  const [loadingAiReview, setLoadingAiReview] = useState(false);
  const [aiReviewResult, setAiReviewResult] = useState(null);
  const [newClaimContent, setNewClaimContent] = useState('');
  const [editingClaim, setEditingClaim] = useState(null);
  const [editClaimContent, setEditClaimContent] = useState('');
  const [selectedClaim, setSelectedClaim] = useState(null);
  const [claimMatches, setClaimMatches] = useState([]);
  const [loadingMatches, setLoadingMatches] = useState(false);
  const [sections, setSections] = useState([]);
  const [selectedSectionId, setSelectedSectionId] = useState('');
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [showNotifications, setShowNotifications] = useState(false);
  const stompRef = useRef(null);

  const preRef = useRef(null);

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
  useEffect(() => {
    api.get('/api/users/profile').then(r => setCurrentUser(r.data)).catch(() => {});
  }, []);

  useEffect(() => {
    api.get('/api/users', { params: { role: 'INSTRUCTOR' } }).then(r => setInstructorsList(r.data || [])).catch(() => {});
  }, []);

  const loadProjectData = async (projId) => {
    if (!projId) return;
    try {
      const projRes = await api.get(`/api/projects/${projId}`);
      setProject(projRes.data);
      try { const r = await api.get(`/api/projects/${projId}/sources`); setSources(r.data?.content || []); } catch {}
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
      try { const r = await api.get(`/api/projects/${projId}/traceability`); setGraphData(r.data); } catch {}
    } catch (err) { console.error('loadProjectData error:', err); }
  };

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
  }, [projectId]);

  useEffect(() => {
    if (!selectedPaper) { setSections([]); return; }
    api.get(`/api/papers/${selectedPaper.id}/sections`)
      .then(r => setSections(r.data || []))
      .catch(() => setSections([]));
  }, [selectedPaper]);

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
            showToast(n.message || 'New notification');
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
      const r = await api.get(`/api/projects/${project.id}/export`, { params: { format: 'tex' }, responseType: 'blob' });
      const url = URL.createObjectURL(new Blob([r.data]));
      const a = document.createElement('a'); a.href = url; a.download = `${project.title || 'project'}-export.zip`;
      a.click(); URL.revokeObjectURL(url);
      showToast('Exported .tex archive.');
    } catch { showToast('Export failed.'); }
  };

  const fetchSources = useCallback(async () => {
    if (!project) return;
    try { const r = await api.get(`/api/projects/${project.id}/sources`); setSources(r.data?.content || []); } catch {}
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
    if (!file || !project || !currentUser) return;
    showToast(`Uploading ${file.name}...`);
    const fd = new FormData();
    fd.append('file', file); fd.append('projectId', project.id);
    try {
      await api.post('/api/sources', fd, { headers: { 'Content-Type': 'multipart/form-data' } });
      showToast("Source uploaded.");
      const r = await api.get(`/api/projects/${project.id}/sources`);
      setSources(r.data?.content || []);
      const g = await api.get(`/api/projects/${project.id}/traceability`);
      setGraphData(g.data);
    } catch { showToast("Upload failed."); }
  };

  const handleDeleteSource = async (sourceId) => {
    if (!window.confirm("Delete this source?")) return;
    try {
      await api.delete(`/api/sources/${sourceId}`);
      showToast("Source deleted.");
      const r = await api.get(`/api/projects/${project.id}/sources`);
      setSources(r.data?.content || []);
      const g = await api.get(`/api/projects/${project.id}/traceability`);
      setGraphData(g.data);
    } catch { showToast("Delete failed."); }
  };

  const handleCreateClaim = async () => {
    if (!newClaimContent.trim() || !project || !selectedSectionId) return;
    try {
      await api.post('/api/claims', { sectionId: selectedSectionId, content: newClaimContent });
      showToast("Claim added.");
      setNewClaimContent('');
      const r = await api.get(`/api/projects/${project.id}/claims`);
      setClaims(r.data?.content || []);
      const g = await api.get(`/api/projects/${project.id}/traceability`);
      setGraphData(g.data);
    } catch { showToast("Add claim failed."); }
  };

  const handleUpdateClaim = async () => {
    if (!editingClaim || !editClaimContent.trim()) return;
    try {
      await api.put(`/api/claims/${editingClaim.id}`, { id: editingClaim.id, content: editClaimContent, active: true, aiConfidenceScore: editingClaim.aiConfidenceScore });
      showToast("Claim updated.");
      setEditingClaim(null); setEditClaimContent('');
      const r = await api.get(`/api/projects/${project.id}/claims`);
      setClaims(r.data?.content || []);
      const g = await api.get(`/api/projects/${project.id}/traceability`);
      setGraphData(g.data);
    } catch { showToast("Update failed."); }
  };

  const handleDeleteClaim = async (claimId) => {
    if (!window.confirm("Delete this claim?")) return;
    try {
      await api.delete(`/api/claims/${claimId}`);
      showToast("Claim deleted.");
      const r = await api.get(`/api/projects/${project.id}/claims`);
      setClaims(r.data?.content || []);
      const g = await api.get(`/api/projects/${project.id}/traceability`);
      setGraphData(g.data);
      if (selectedClaim && selectedClaim.id === claimId) { setSelectedClaim(null); setClaimMatches([]); }
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
    if (!window.confirm('Rollback this section to its previous version?')) return;
    try {
      await api.post(`/api/papers/${selectedPaper.id}/sections/${sectionId}/rollback`);
      showToast('Section rolled back.');
      const r = await api.get(`/api/papers/${selectedPaper.id}/sections`);
      setSections(r.data || []);
    } catch { showToast('Rollback failed.'); }
  };

  const canEditClaim = (claim) => {
    if (role === 'ADMIN' || role === 'INSTRUCTOR') return true;
    return sections.filter(s => s.assignedUserId === currentUser?.id).map(s => s.id).includes(claim.sectionId);
  };

  const handleSaveDraft = async () => {
    if (!selectedPaper) { showToast("No paper selected."); return; }
    setSaveStatus('saving');
    try {
      await api.put(`/api/documents/${selectedPaper.id}/text`, codeContent, { headers: { 'Content-Type': 'text/plain' } });
      setSaveStatus('saved'); setLastSaved(new Date());
      setTimeout(() => setSaveStatus(''), 3000);
    } catch { setSaveStatus('error'); showToast("Save failed."); }
  };

  const [saveStatus, setSaveStatus] = useState('');
  const [lastSaved, setLastSaved] = useState(null);

  const handleExportJson = () => {
    if (!graphData) return;
    const blob = new Blob([JSON.stringify(graphData, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a'); a.href = url; a.download = `traceability-${project?.title || 'export'}.json`;
    a.click(); URL.revokeObjectURL(url);
  };

  const handleExportCsv = () => {
    if (!graphData) return;
    const esc = (s) => `"${(s || '').replace(/"/g, '""')}"`;
    const rows = [['Claim ID', 'Claim Content', 'Verdict', 'Confidence', 'Section', 'Source File', 'Excerpt', 'Score', 'Explanation', 'Source Page']];
    const srcMap = {}; (graphData.sources || []).forEach(s => { srcMap[s.id] = s.filename; });
    (graphData.claims || []).forEach(c => {
      const g = c.graphData || {}; const verdict = g.verdict || ''; const conf = g.confidence ? (g.confidence * 100).toFixed(0) : '';
      if (c.matches && c.matches.length > 0) { c.matches.forEach(m => { rows.push([esc(c.id), esc(c.content), esc(verdict), conf, esc(c.sectionTitle || ''), esc(srcMap[m.sourceId] || m.filename || ''), esc(m.excerpt), m.score ? (m.score * 100).toFixed(0) : '', esc(m.explanation || ''), m.page || '']); }); }
      else { rows.push([esc(c.id), esc(c.content), esc(verdict), conf, esc(c.sectionTitle || ''), '', '', '', '', '']); }
    });
    const csv = rows.map(r => r.join(',')).join('\n');
    const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a'); a.href = url; a.download = `traceability-${project?.title || 'export'}.csv`;
    a.click(); URL.revokeObjectURL(url);
  };

  const fetchGraphData = useCallback(async (projId) => {
    try { const r = await api.get(`/api/projects/${projId}/traceability`); setGraphData(r.data); } catch {}
  }, []);

  useEffect(() => {
    if (activeTab === 'Graph' && project?.id && !graphData) fetchGraphData(project.id);
  }, [activeTab, project?.id, graphData, fetchGraphData]);

  const handleAnalyzeClaim = async (claimId) => {
    showToast("AI analyzing...");
    try {
      await api.post(`/api/claims/${claimId}/suggestions/generate`);
      showToast("AI analysis complete.");
      const r = await api.get(`/api/projects/${project.id}/claims`);
      setClaims(r.data?.content || []);
      const g = await api.get(`/api/projects/${project.id}/traceability`);
      setGraphData(g.data);
      if (selectedClaim && selectedClaim.id === claimId) handleFetchMatches(claimId);
    } catch { showToast("AI analysis failed."); }
  };

  const handleFetchMatches = async (claimId) => {
    setLoadingMatches(true);
    try { const r = await api.get(`/api/claims/${claimId}/suggestions`); setClaimMatches(r.data || []); } catch { showToast("Fetch matches failed."); }
    finally { setLoadingMatches(false); }
  };

  const handleRunAiReview = async () => {
    if (!selectedPaper) { showToast("Select a paper first."); return; }
    setLoadingAiReview(true); setShowAiReviewModal(true);
    try {
      const r = await api.post(`/api/papers/${selectedPaper.id}/review`);
      setAiReviewResult(r.data); showToast("AI Review complete.");
    } catch {
      showToast("AI Review failed.");
      setAiReviewResult({ styleFeedback: "AI service unavailable.", structureFeedback: "Connection error." });
    } finally { setLoadingAiReview(false); }
  };

  const handleSubmitReview = async () => {
    if (!project) return;
    if (!selectedInstructorId) { showToast("Select an instructor."); return; }
    try {
      await api.post(`/api/projects/${project.id}/reviews`, { instructorId: selectedInstructorId });
      showToast("Submitted for review.");
      setShowSubmitReviewModal(false);
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
    if (selectedPaper?.status === 'APPROVED') { showToast('Document is approved, cannot edit.'); return; }
    const ta = document.getElementById('latex-textarea');
    if (!ta) return;
    const start = ta.selectionStart, end = ta.selectionEnd;
    const text = codeContent, sel = text.substring(start, end);
    let insertion = '', offset = 0;
    const m = { bold: [`\\textbf{${sel || 'text'}}`, 8], italic: [`\\textit{${sel || 'text'}}`, 8], section: [`\\section{${sel || 'Title'}}`, 9], subsection: [`\\subsection{${sel || 'Subtitle'}}`, 12], subsubsection: [`\\subsubsection{${sel || 'Subtitle2'}}`, 15], large: [`{\\large ${sel || 'text'}}`, 8], small: [`{\\small ${sel || 'text'}}`, 8], 'inline-math': [`$${sel || 'E=mc^2'}$`, 1], list: [`\n\\begin{itemize}\n  \\item ${sel || 'item'}\n\\end{itemize}\n`, 21], equation: [`\\begin{equation}\n  ${sel || 'E = mc^2'}\n\\end{equation}`, 18], comment: [`% ${sel || 'comment'}`, 2], hl: [`\\hl{${sel || 'highlight'}}`, 4] };
    if (m[tagType]) { insertion = m[tagType][0]; offset = m[tagType][1]; }
    else if (tagType === 'label') { const n = prompt('Label name:', 'sec:label') || 'sec:label'; insertion = `\\label{${n}}`; offset = insertion.length; }
    else if (tagType === 'cite') { const k = prompt('Citation key:', 'author2026') || 'key'; insertion = `\\cite{${k}}`; offset = insertion.length; }
    else if (tagType === 'link') { const url = prompt('URL:', 'https://') || 'https://'; const l = sel || prompt('Link label:', 'Link') || 'Link'; insertion = `\\href{${url}}{${l}}`; offset = insertion.length; }
    else if (tagType === 'figure') { insertion = `\n\\begin{figure}[h]\n  \\centering\n  \\includegraphics[width=0.8\\textwidth]{image.png}\n  \\caption{${sel || 'Caption'}}\n  \\label{fig:label}\n\\end{figure}\n`; offset = 83; }
    else if (tagType === 'table') { insertion = `\n\\begin{table}[h]\n  \\centering\n  \\begin{tabular}{|c|c|}\n    \\hline\n    Col1 & Col2 \\\\\n    \\hline\n    ${sel || 'Row1'} & Row1 \\\\\n    Row2 & Row2 \\\\\n    \\hline\n  \\end{tabular}\n  \\caption{Table caption}\n  \\label{tab:table}\n\\end{table}\n`; offset = 120; }
    const newContent = text.substring(0, start) + insertion + text.substring(end);
    updateCode(newContent);
    setTimeout(() => { ta.focus(); ta.setSelectionRange(start + offset, start + offset); }, 50);
  };

  const insertSymbol = (sym) => {
    if (selectedPaper?.status === 'APPROVED') { showToast('Cannot edit.'); return; }
    const ta = document.getElementById('latex-textarea');
    if (!ta) return;
    const start = ta.selectionStart, end = ta.selectionEnd;
    const newContent = codeContent.substring(0, start) + sym + codeContent.substring(end);
    updateCode(newContent);
    setTimeout(() => { ta.focus(); ta.setSelectionRange(start + sym.length, start + sym.length); }, 50);
  };

  const handleUndo = () => {
    if (historyIndex > 0) { setHistoryIndex(historyIndex - 1); setCodeContent(codeHistory[historyIndex - 1]); }
  };

  const handleRedo = () => {
    if (historyIndex < codeHistory.length - 1) { setHistoryIndex(historyIndex + 1); setCodeContent(codeHistory[historyIndex + 1]); }
  };

  const handleFindReplace = (replaceAll = false) => {
    if (selectedPaper?.status === 'APPROVED') { showToast('Cannot edit.'); return; }
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

  const getPaperCategory = (paper) => {
    const t = ((paper.title || paper.name || '') + ' ' + (paper.content || '')).toLowerCase();
    if (t.includes('react')) return 'ReactJS';
    if (t.includes('devops') || t.includes('agile') || t.includes('scrum') || t.includes('cicd') || t.includes('test')) return 'DevOps';
    if (t.includes('microservice') || t.includes('gateway') || t.includes('consensus') || t.includes('raft') || t.includes('kafka')) return 'Microservices';
    return 'General';
  };

  const getCategoryColor = (cat) => ({ ReactJS: '#38bdf8', DevOps: '#10b981', Microservices: '#ec4899', General: '#818cf8' }[cat] || '#818cf8');

  const sortedPapers = [...papers].sort((a, b) => new Date(a.uploadedAt) - new Date(b.uploadedAt));
  const clusterCounts = {};
  sortedPapers.forEach(p => { const cat = getPaperCategory(p); clusterCounts[cat] = (clusterCounts[cat] || 0) + 1; });
  const tempNodes = [];
  const cc2 = {};
  sortedPapers.forEach((paper, index) => {
    const cat = getPaperCategory(paper);
    const numInCluster = cc2[cat] || 0; cc2[cat] = numInCluster + 1;
    let summary = 'Research draft.';
    if (paper.extractedText) summary = paper.extractedText.replace(/\\hl\{([^}]+)\}/g, '$1').slice(0, 160) + '...';
    tempNodes.push({ id: paper.id, num: index + 1, name: paper.filename || paper.name || paper.originalFilename || 'document.tex', title: paper.title || paper.originalFilename || 'document.tex', category: cat, color: getCategoryColor(cat), created: paper.uploadedAt ? new Date(paper.uploadedAt).toLocaleString() : 'Unknown', summary, clusterIndex: numInCluster });
  });

  const clusterCenters = { ReactJS: { x: 95, y: 95 }, DevOps: { x: 245, y: 95 }, Microservices: { x: 170, y: 225 }, General: { x: 170, y: 160 } };

  const dynamicNodes = tempNodes.map(node => {
    const total = clusterCounts[node.category] || 1;
    const center = clusterCenters[node.category] || { x: 170, y: 160 };
    if (total <= 1) return { ...node, x: center.x, y: center.y };
    const angle = (node.clusterIndex / total) * 2 * Math.PI;
    return { ...node, x: Math.round(center.x + 35 * Math.cos(angle)), y: Math.round(center.y + 35 * Math.sin(angle)) };
  });

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

  const t = UI_TEXT[language];

  return (
    <div className="h-screen w-full flex flex-col bg-slate-50 overflow-hidden font-sans antialiased text-slate-800">
      <WorkspaceHeader projects={projects} project={project} navigate={navigate} feedbacks={feedbacks} toggleLanguage={toggleLanguage} language={language} setShowHistoryModal={setShowHistoryModal} setShowReviseModal={setShowReviseModal} logout={logout}
        notifications={notifications} unreadCount={unreadCount} showNotifications={showNotifications} setShowNotifications={setShowNotifications} onMarkNotificationRead={handleMarkNotificationRead} onExportTexArchive={handleExportTexArchive} />

      <div id="workspace-container" className="flex-1 flex overflow-hidden">
        <div className="w-14 bg-indigo-900 flex flex-col items-center py-4 shrink-0 z-20 border-r border-indigo-950 shadow-[2px_0_8px_-2px_rgba(0,0,0,0.2)]">
          <button onClick={() => setIsFileTreeOpen(!isFileTreeOpen)} className="w-full flex justify-center relative cursor-pointer mb-6 group outline-none" title="Toggle File Sidebar">
            <div className={`absolute left-0 top-0 bottom-0 w-1 rounded-r-md transition-colors ${isFileTreeOpen ? 'bg-white shadow-[0_0_8px_rgba(255,255,255,0.8)]' : 'bg-transparent'}`}></div>
            <svg className={`w-[22px] h-[22px] transition-colors ${isFileTreeOpen ? 'text-white' : 'text-indigo-300 group-hover:text-white'}`} fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" /></svg>
          </button>
          <div onClick={() => setIsDrawerOpen(!isDrawerOpen)} className="w-full flex justify-center cursor-pointer mb-6 group relative" title="Toggle Right Drawer">
            <div className={`absolute left-0 top-0 bottom-0 w-1 rounded-r-md transition-colors ${isDrawerOpen ? 'bg-indigo-400' : 'bg-transparent'}`}></div>
            <svg className={`w-[22px] h-[22px] transition-colors ${isDrawerOpen ? 'text-white' : 'text-indigo-300 group-hover:text-white'}`} fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
          </div>
          <div onClick={() => showToast('Settings')} className="w-full flex justify-center cursor-pointer group relative">
            <div className="absolute left-0 top-0 bottom-0 w-1 bg-transparent group-hover:bg-indigo-400 rounded-r-md transition-colors"></div>
            <svg className="w-[22px] h-[22px] text-indigo-300 group-hover:text-white transition-colors" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" /><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" /></svg>
          </div>
        </div>

        <FilePanel isOpen={isFileTreeOpen} width={fileTreeWidth} onResizeStart={handleLeftDividerMouseDown} papers={papers} selectedPaper={selectedPaper} onSelectPaper={(p) => { setSelectedPaper(p); loadCode(p.extractedText || ''); }} onUploadPaper={handleUploadPaper} onDeletePaper={handleDeletePaper} sources={sources} onUploadSource={handleUploadSource} onDeleteSource={handleDeleteSource} showToast={showToast} language={language} />

        <EditorPanel selectedPaper={selectedPaper} displayContent={displayContent} updateCode={updateCode} codeHistory={codeHistory} historyIndex={historyIndex} editorMode={editorMode} setEditorMode={setEditorMode} editorWidth={editorWidth} onEditorResizeStart={handleMouseDown} saveStatus={saveStatus} lastSaved={lastSaved} handleSaveDraft={handleSaveDraft} handleRunAiReview={handleRunAiReview} insertLatexTag={insertLatexTag} insertSymbol={insertSymbol} handleUndo={handleUndo} handleRedo={handleRedo} handleFindReplace={handleFindReplace} handleDownloadTex={handleDownloadTex} showSymbolMenu={showSymbolMenu} setShowSymbolMenu={setShowSymbolMenu} showTextSizeMenu={showTextSizeMenu} setShowTextSizeMenu={setShowTextSizeMenu} showSearchPanel={showSearchPanel} setShowSearchPanel={setShowSearchPanel} searchQuery={searchQuery} setSearchQuery={setSearchQuery} replaceQuery={replaceQuery} setReplaceQuery={setReplaceQuery} textSize={textSize} setTextSize={setTextSize} preRef={preRef} generateRichTextHtml={generateRichTextHtml} parseHtmlToLatex={parseHtmlToLatex} showToast={showToast} language={language} />

        <ContextPanel isOpen={isDrawerOpen} width={rightDrawerWidth} onResizeStart={handleRightDividerMouseDown} activeTab={activeTab} setActiveTab={(tab) => { setActiveTab(tab); localStorage.setItem('student_workspace_active_tab', tab); }} language={language} showToast={showToast}
          sources={sources} isUploading={isUploading} setIsUploading={setIsUploading} project={project} setViewerFile={setViewerFile} fetchSources={fetchSources}
          sections={sections} selectedSectionId={selectedSectionId} setSelectedSectionId={setSelectedSectionId}
          newClaimContent={newClaimContent} setNewClaimContent={setNewClaimContent} handleCreateClaim={handleCreateClaim}
          claims={claims} selectedClaim={selectedClaim} claimMatches={claimMatches} loadingMatches={loadingMatches}
          handleFetchMatches={handleFetchMatches} handleAnalyzeClaim={handleAnalyzeClaim} canEditClaim={canEditClaim}
          editingClaim={editingClaim} setEditingClaim={setEditingClaim} editClaimContent={editClaimContent} setEditClaimContent={setEditClaimContent} handleDeleteClaim={handleDeleteClaim}
          feedbacks={feedbacks} setShowSubmitReviewModal={setShowSubmitReviewModal}
          graphData={graphData} fetchGraphData={fetchGraphData} dynamicNodes={dynamicNodes} hoveredNodeId={hoveredNodeId} setHoveredNodeId={setHoveredNodeId}
          papers={papers} selectedPaperDetail={selectedPaperDetail} setSelectedPaperDetail={setSelectedPaperDetail}
          handleExportCsv={handleExportCsv} handleExportJson={handleExportJson} setSelectedPaper={setSelectedPaper} loadCode={loadCode}
          renderModalPaperPdf={renderModalPaperPdf} />
      </div>

      {/* History Modal */}
      {showHistoryModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-sm animate-in fade-in duration-200">
          <div className="bg-white rounded-xl shadow-2xl w-full max-w-lg p-6 transform transition-all max-h-[85vh] flex flex-col">
            <div className="flex justify-between items-center mb-4 shrink-0">
              <h2 className="text-lg font-bold text-slate-800">Version History</h2>
              <button onClick={() => setShowHistoryModal(false)} className="text-slate-400 hover:text-slate-600 transition-colors">
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" /></svg>
              </button>
            </div>
            <div className="flex-1 overflow-y-auto space-y-3">
              {sections.length === 0 ? (
                <div className="text-xs text-slate-400 italic text-center py-8">No sections yet. Upload a paper first.</div>
              ) : sections.map(sec => (
                <div key={sec.id} className="border border-slate-200 rounded-lg p-3 hover:border-indigo-200 transition-colors">
                  <div className="flex justify-between items-start mb-1">
                    <h3 className="text-sm font-bold text-slate-800 truncate">{sec.sectionTitle || 'Untitled'}</h3>
                    <span className="text-[10px] font-bold text-indigo-600 bg-indigo-50 px-1.5 py-0.5 rounded shrink-0 ml-2">v{sec.version || 1}</span>
                  </div>
                  <p className="text-xs text-slate-500 line-clamp-2 mb-2 font-mono">{(sec.contentTex || '').substring(0, 120)}{(sec.contentTex || '').length > 120 ? '...' : ''}</p>
                  <div className="flex items-center justify-between text-[10px]">
                    <span className="text-slate-400">Assigned: {sec.assignedUserName || sec.assignedUserId || 'Unassigned'}</span>
                    <div className="flex gap-2">
                      {sec.previousContentTex && (
                        <button onClick={() => handleRollbackSection(sec.id)} className="text-amber-600 hover:text-amber-800 font-bold">Rollback</button>
                      )}
                      <button onClick={() => { setSelectedSectionId(sec.id); showToast('Section selected'); }} className="text-indigo-600 hover:text-indigo-800 font-bold">Select</button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* Revise Modal */}
      {showReviseModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-sm animate-in fade-in duration-200">
          <div className="bg-white rounded-xl shadow-2xl w-full max-w-md p-6 transform transition-all">
            <div className="flex justify-between items-center mb-4">
              <h2 className="text-lg font-bold text-slate-800">Auto Revise</h2>
              <button onClick={() => setShowReviseModal(false)} className="text-slate-400 hover:text-slate-600 transition-colors">
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" /></svg>
              </button>
            </div>
            <p className="text-sm text-slate-600 mb-4">Select sections for AI revision based on instructor feedback.</p>
            <div className="space-y-2 mb-4 max-h-60 overflow-y-auto">
              {sections.map(sec => (
                <label key={sec.id} className="flex items-center gap-3 p-2.5 border border-slate-200 rounded-lg cursor-pointer hover:bg-slate-50 transition-colors">
                  <input type="checkbox" className="w-4 h-4 text-indigo-600 rounded border-slate-300 focus:ring-indigo-500" defaultChecked />
                  <span className="text-sm font-medium text-slate-700">{sec.sectionTitle} <span className="text-[10px] text-slate-400">v{sec.version || 1}</span></span>
                </label>
              ))}
              {sections.length === 0 && <div className="text-xs text-slate-400 italic text-center py-4">No sections available.</div>}
            </div>
            <div className="flex justify-end gap-3">
              <button onClick={() => setShowReviseModal(false)} className="px-4 py-2 text-sm font-semibold text-slate-600 hover:bg-slate-100 rounded-lg transition-colors">Cancel</button>
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
          <div className="bg-white rounded-xl shadow-2xl w-full max-w-md p-6 transform transition-all">
            <div className="flex justify-between items-center mb-4">
              <h2 className="text-lg font-bold text-slate-800">Submit for Review</h2>
              <button onClick={() => setShowSubmitReviewModal(false)} className="text-slate-400 hover:text-slate-600 transition-colors">
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" /></svg>
              </button>
            </div>
            <p className="text-sm text-slate-600 mb-4">Select an instructor to review your draft.</p>
            <div className="mb-6">
              <label className="block text-xs font-bold text-slate-400 uppercase tracking-wider mb-2">Instructor</label>
              <select value={selectedInstructorId || ''} onChange={(e) => setSelectedInstructorId(e.target.value)} className="w-full p-2.5 bg-white border border-slate-200 rounded-lg text-sm text-slate-700 focus:outline-none focus:ring-2 focus:ring-indigo-500">
                {instructorsList.map(inst => <option key={inst.id} value={inst.id}>{inst.firstName} {inst.lastName} ({inst.email})</option>)}
                {instructorsList.length === 0 && <option value="">No instructors</option>}
              </select>
            </div>
            <div className="flex justify-end gap-3">
              <button onClick={() => setShowSubmitReviewModal(false)} className="px-4 py-2 text-sm font-semibold text-slate-600 hover:bg-slate-100 rounded-lg transition-colors">Cancel</button>
              <button onClick={handleSubmitReview} className="px-4 py-2 text-sm font-bold text-white bg-indigo-600 hover:bg-indigo-700 rounded-lg shadow-sm shadow-indigo-200 transition-colors">Submit</button>
            </div>
          </div>
        </div>
      )}

      {/* AI Review Modal */}
      {showAiReviewModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-sm animate-in fade-in duration-200">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-2xl overflow-hidden flex flex-col max-h-[85vh] animate-in zoom-in-95 duration-200">
            <div className="bg-indigo-900 text-white px-6 py-4 flex justify-between items-center shrink-0">
              <div className="flex items-center gap-2">
                <svg className="w-5 h-5 text-indigo-300 animate-pulse" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 01-2 2h0a2 2 0 01-2-2v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" /></svg>
                <h2 className="text-base font-bold tracking-wide">AI Review Report</h2>
              </div>
              <button onClick={() => setShowAiReviewModal(false)} className="text-indigo-200 hover:text-white transition-colors">
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" /></svg>
              </button>
            </div>
            <div className="flex-1 overflow-y-auto p-6 bg-slate-50/50 space-y-6 custom-scrollbar">
              {loadingAiReview ? (
                <div className="flex flex-col items-center justify-center py-16 space-y-4">
                  <div className="w-10 h-10 border-4 border-indigo-200 border-t-indigo-600 rounded-full animate-spin"></div>
                  <div className="text-center">
                    <p className="text-sm font-bold text-slate-700">AI is analyzing...</p>
                    <p className="text-xs text-slate-400 mt-1">Evaluating structure, evidence, and academic tone...</p>
                  </div>
                </div>
              ) : aiReviewResult ? (
                <>
                  <div className="bg-white border border-slate-200 rounded-xl p-5 shadow-sm hover:border-indigo-200 transition-colors">
                    <div className="flex justify-between items-start mb-3">
                      <h3 className="text-sm font-bold text-slate-800 flex items-center gap-1.5"><span className="w-1.5 h-3 bg-indigo-600 rounded"></span>1. Academic Tone</h3>
                      <span className="bg-emerald-50 text-emerald-700 border border-emerald-200 text-[10px] font-bold px-2 py-0.5 rounded">Pass</span>
                    </div>
                    <p className="text-xs text-slate-600 leading-relaxed bg-slate-50 p-3.5 rounded-lg border border-slate-100 italic">"{aiReviewResult.styleFeedback}"</p>
                  </div>
                  <div className="bg-white border border-slate-200 rounded-xl p-5 shadow-sm hover:border-indigo-200 transition-colors">
                    <div className="flex justify-between items-start mb-3">
                      <h3 className="text-sm font-bold text-slate-800 flex items-center gap-1.5"><span className="w-1.5 h-3 bg-indigo-600 rounded"></span>2. Evidence Mapping</h3>
                      <span className="bg-amber-50 text-amber-700 border border-amber-200 text-[10px] font-bold px-2 py-0.5 rounded">Gaps Found</span>
                    </div>
                    <p className="text-xs text-slate-600 leading-relaxed bg-slate-50 p-3.5 rounded-lg border border-slate-100 italic">"{aiReviewResult.structureFeedback}"</p>
                  </div>
                </>
              ) : null}
            </div>
            <div className="px-6 py-4 border-t border-slate-100 bg-slate-50/50 flex justify-end gap-3 shrink-0">
              <button onClick={() => setShowAiReviewModal(false)} className="px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-lg transition-colors border border-slate-200 bg-white cursor-pointer">Close</button>
            </div>
          </div>
        </div>
      )}

      {viewerFile && <FileViewerModal fileUrl={viewerFile.fileUrl} fileName={viewerFile.fileName} onClose={() => setViewerFile(null)} />}

      {/* Paper Detail Modal */}
      {selectedPaperDetail && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-sm animate-in fade-in duration-200 p-4">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-4xl overflow-hidden transform transition-all border border-slate-100 flex flex-col h-[85vh]">
            <div className="px-6 py-4 border-b border-slate-100 bg-slate-50 flex justify-between items-center shrink-0">
              <div className="flex items-center gap-2">
                <span className="text-[10px] font-black text-white px-2 py-0.5 rounded-full uppercase tracking-wider" style={{ backgroundColor: selectedPaperDetail.color }}>Paper #{selectedPaperDetail.num}</span>
                <span className="text-[10px] font-bold text-slate-400 font-mono">{selectedPaperDetail.name}</span>
              </div>
              <button onClick={() => setSelectedPaperDetail(null)} className="text-slate-400 hover:text-slate-600 transition-colors p-1.5 hover:bg-slate-100 rounded-lg">
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" /></svg>
              </button>
            </div>
            <div className="flex-1 flex overflow-hidden">
              <div className="w-1/2 p-6 overflow-y-auto custom-scrollbar space-y-4 border-r border-slate-150">
                <h3 className="text-base font-extrabold text-slate-800 leading-snug">{selectedPaperDetail.title}</h3>
                <p className="text-[10px] text-slate-400">Created: {selectedPaperDetail.created}</p>
                <div className="flex gap-2 items-center">
                  <span className="text-xs font-bold text-slate-500">Category:</span>
                  <span className="text-[10px] font-bold px-2 py-0.5 rounded-md text-white shadow-sm" style={{ backgroundColor: selectedPaperDetail.color }}>{selectedPaperDetail.category}</span>
                </div>
                <div className="bg-slate-50 rounded-xl p-4 border border-slate-200/60">
                  <h4 className="text-[10px] font-bold text-slate-400 uppercase tracking-widest mb-1.5">Summary</h4>
                  <p className="text-xs text-slate-600 leading-relaxed font-medium">{selectedPaperDetail.summary}</p>
                </div>
              </div>
              <div className="w-1/2 p-6 bg-slate-100 flex flex-col overflow-hidden">
                <h4 className="text-[10px] font-black text-slate-500 uppercase tracking-widest mb-3 flex items-center gap-1.5 shrink-0">
                  <svg className="w-3.5 h-3.5 text-red-500" fill="currentColor" viewBox="0 0 20 20"><path fillRule="evenodd" d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4z" clipRule="evenodd" /></svg>
                  PDF Preview
                </h4>
                <div className="flex-1 overflow-y-auto custom-scrollbar pr-1">{renderModalPaperPdf(selectedPaperDetail.name)}</div>
              </div>
            </div>
            <div className="px-6 py-4 border-t border-slate-100 bg-slate-50 flex justify-end shrink-0">
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

      <TourLauncher steps={TOUR_STEPS} tourKey="student-workspace" />
    </div>
  );
}
