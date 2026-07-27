import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { AppHeader, LoadingSkeleton, StatusBadge, Modal, TourLauncher, SectionTree, EvidenceGraph, LatexEditor } from '../../components';
import { instructorText, commonText } from '../../locales';
import { useLanguage } from '../../context/LanguageContext';
import { useAuth } from '../../context/AuthContext';
import api from '../../api';

const STANDARDS = ['IEEE', 'ACM', 'SPRINGER_LNCS', 'APA', 'MLA', 'CUSTOM'];

export default function ProjectDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { language } = useLanguage();
  const ct = commonText[language];
  const t = instructorText[language];
  const { user: currentUser } = useAuth();

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
  const [users, setUsers] = useState([]);
  const [showAddMember, setShowAddMember] = useState(false);
  const [newMemberId, setNewMemberId] = useState('');

  // Setup tab state
  const [doiInput, setDoiInput] = useState('');
  const [importingDoi, setImportingDoi] = useState(false);
  const [standard, setStandard] = useState('');
  const [sources, setSources] = useState([]);
  const [showSourceDetail, setShowSourceDetail] = useState(false);
  const [sourceDetail, setSourceDetail] = useState(null);
  const [showAddSource, setShowAddSource] = useState(false);
  const [showShareCollection, setShowShareCollection] = useState(false);
  const [collections, setCollections] = useState([]);
  const [selectedCollectionId, setSelectedCollectionId] = useState('');
  const [collectionSources, setCollectionSources] = useState([]);
  const [showSetUpPaper, setShowSetUpPaper] = useState(false);
  const [autoGenLoading, setAutoGenLoading] = useState(false);
  const [importDoiSourceLoading, setImportDoiSourceLoading] = useState(false);
  const [setupMode, setSetupMode] = useState('standard');
  const [shareLoading, setShareLoading] = useState(false);
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
    } catch {}
  }, [id]);

  const loadSections = useCallback(async (paperId) => {
    try {
      const res = await api.get(`/api/papers/${paperId}/sections`);
      setSections(res.data || []);
    } catch { setSections([]); }
  }, []);

  const loadFeedbackAndTraceability = useCallback(async () => {
    try {
      const [fbRes, traceRes] = await Promise.all([
        api.get('/api/feedback-requests'),
        api.get(`/api/projects/${id}/traceability`).catch(() => null),
      ]);
      const projectFbs = (fbRes.data || []).filter(fb => fb.projectId === id);
      setFeedbackRequests(projectFbs);
      setTraceability(traceRes?.data || null);
    } catch {}
  }, [id]);

  const loadUsers = useCallback(async () => {
    try {
      const res = await api.get('/api/users?role=STUDENT');
      setUsers(res.data || []);
    } catch {}
  }, []);

  const loadSources = useCallback(async () => {
    try {
      const res = await api.get(`/api/sources/projects/${id}`);
      setSources(res.data || []);
    } catch {}
  }, [id]);

  const loadCollections = useCallback(async () => {
    try {
      const res = await api.get('/api/collections');
      setCollections(res.data?.content || res.data || []);
    } catch {}
  }, []);

  useEffect(() => { loadProject(); }, [loadProject]);
  useEffect(() => { if (project) { loadPapers(); loadSources(); loadUsers(); } }, [project, loadPapers, loadSources, loadUsers]);

  useEffect(() => {
    if (activeTab === 'review') loadFeedbackAndTraceability();
  }, [activeTab, loadFeedbackAndTraceability]);

  const handleUpdateStandard = async () => {
    if (!standard || !project) return;
    setSaving(true);
    try {
      await api.put(`/api/projects/${id}`, { ...project, targetStandard: standard });
      await api.post(`/api/projects/${id}/papers/init`);
      await loadProject();
      await loadPapers();
      if (selectedPaper) {
        const secRes = await api.get(`/api/papers/${selectedPaper.id}/sections`);
        if ((secRes.data || []).length === 0) {
          await api.post(`/api/papers/${selectedPaper.id}/sections/create?standard=${standard}&title=Auto`);
          await loadSections(selectedPaper.id);
        }
      }
    } catch { alert('Failed to update standard'); }
    finally { setSaving(false); }
  };

  const handleImportDoi = async () => {
    if (!doiInput.trim()) return;
    setImportingDoi(true);
    try {
      await api.post('/api/documents/ingest/doi', { doi: doiInput.trim(), projectId: id });
      setDoiInput('');
      await loadPapers();
    } catch { alert('DOI import failed.'); }
    finally { setImportingDoi(false); }
  };

  const handleUploadSource = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const formData = new FormData();
    formData.append('file', file);
    formData.append('projectId', id);
    try {
      await api.post('/api/sources', formData);
      await loadPapers();
    } catch { alert('Upload failed.'); }
  };

  const handleUploadPaper = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const formData = new FormData();
    formData.append('file', file);
    formData.append('projectId', id);
    try {
      const { data: doc } = await api.post('/api/papers', formData);
      if (standard) {
        const secRes = await api.get(`/api/papers/${doc.id}/sections`);
        if ((secRes.data || []).length === 0) {
          await api.post(`/api/papers/${doc.id}/sections/create?standard=${standard}&title=Auto`);
        }
        await loadSections(doc.id);
      }
      await loadPapers();
    } catch { alert('Upload failed.'); }
  };

  const handleImportDoiSource = async () => {
    if (!doiInput.trim()) return;
    setImportDoiSourceLoading(true);
    try {
      await api.post('/api/documents/ingest/doi', { doi: doiInput.trim(), projectId: id, docType: 'SOURCE' });
      setDoiInput('');
      await loadSources();
    } catch { alert('DOI+Source import failed.'); }
    finally { setImportDoiSourceLoading(false); }
  };

  const handleShareSource = async (sourceId) => {
    if (!selectedCollectionId) return;
    setShareLoading(true);
    try {
      await api.post(`/api/collections/${selectedCollectionId}/sources/${sourceId}/share-to-project/${id}`);
      await loadSources();
    } catch { alert('Share failed.'); }
    finally { setShareLoading(false); }
  };

  const handleAutoGenerateSections = async () => {
    if (!selectedPaper) return;
    setAutoGenLoading(true);
    try {
      await api.post(`/api/papers/${selectedPaper.id}/sections/create?standard=${standard}&title=Auto`);
      await loadSections(selectedPaper.id);
    } catch { alert('Auto-generation failed.'); }
    finally { setAutoGenLoading(false); }
  };

  const handleDetectSections = async () => {
    if (!selectedPaper) return;
    try {
      await api.post(`/api/papers/${selectedPaper.id}/sections/create?title=Detected`);
      await loadSections(selectedPaper.id);
    } catch { alert('Detection failed.'); }
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
    } catch { alert('Assignment failed.'); }
  };

  const handleAddMember = async () => {
    if (!newMemberId) return;
    try {
      await api.post(`/api/projects/${id}/members`, null, { params: { userId: newMemberId, role: 'EDITOR' } });
      setShowAddMember(false);
      setNewMemberId('');
      loadProject();
    } catch { alert('Failed to add member.'); }
  };

  const handleRemoveMember = async (userId) => {
    try {
      await api.delete(`/api/projects/${id}/members/${userId}`);
      loadProject();
    } catch { alert('Failed to remove member.'); }
  };

  const handlePatch = async (action) => {
    try {
      await api.patch(`/api/projects/${id}/${action}`);
      loadProject();
    } catch { alert(`Failed to ${action} project.`); }
  };

  const TOUR_STEPS = [
    { element: '#project-header', popover: { title: 'Project', description: 'This is the title description of your project', side: 'bottom', align: 'start' } },
    { element: '#tab-setup', popover: { title: 'Setup', description: 'Import source documents, choose a paper standard (IEEE/ACM/etc), or upload a student paper.', side: 'bottom', align: 'center' } },
    { element: '#tab-sections', popover: { title: 'Sections', description: 'Auto-generate sections from the standard chosen in Setup, detect from an uploaded paper, or assign sections to students.', side: 'bottom', align: 'center' } },
    { element: '#tab-review', popover: { title: 'Review', description: 'Review student feedback requests and view the evidence traceability map.', side: 'bottom', align: 'center' } },
    { element: '#tab-settings', popover: { title: 'Settings', description: 'Project status controls (complete/archive) and member management.', side: 'bottom', align: 'center' } },
    { element: '#source-documents', popover: { title: 'Source Documents', description: 'Reference sources imported via DOI, file upload, or shared collections.', side: 'top', align: 'start' } },
    { element: '#set-up-paper', popover: { title: 'Set up Paper', description: 'Choose a paper standard to define required sections (enables Auto-gen in Sections tab), or upload a student paper for section detection.', side: 'top', align: 'start' } },
    { element: '#project-members', popover: { title: 'Members', description: 'Add or remove student members on this project.', side: 'top', align: 'start' } },
    { element: '#status-controls', popover: { title: 'Status', description: 'Change project status: mark complete, archive, or unarchive.', side: 'top', align: 'start' } },
  ];

  useEffect(() => {
    if (papers.length > 0 && !selectedPaper) {
      setSelectedPaper(papers[0]);
    }
  }, [papers]);

  useEffect(() => {
    if (selectedPaper) loadSections(selectedPaper.id);
  }, [selectedPaper]);

  if (loading) return <div className="min-h-screen bg-[#f8fafc]"><AppHeader /><div className="max-w-6xl mx-auto p-8"><LoadingSkeleton count={6} /></div></div>;
  if (!project) return null;

  const projectMembers = members;
  const displayName = m => [m.firstName, m.lastName].filter(Boolean).join(' ') || m.email || m.userId?.slice(0, 8);
  const hasAssignedSections = sections.some(s => s.assignedUserId);

  return (
    <div className="min-h-screen bg-[#f8fafc] text-[#0f172a] font-sans">
      <AppHeader />
      <div className="max-w-6xl mx-auto p-8">
        <div id="project-header" className="mb-6">
          <Link to="/instructor/projects" className="text-xs font-bold text-gray-400 hover:text-[#1e3a8a] transition-colors">&larr; {ct.back}</Link>
          <div className="flex items-center justify-between mt-2">
            <div>
              <h1 className="text-2xl font-black text-[#1e3a8a]">{project.title}</h1>
              {project.description && <p className="text-sm text-gray-500 mt-1">{project.description}</p>}
              <p className="text-xs text-gray-400 mt-1">ID: {project.id} &middot; <StatusBadge status={project.status} /></p>
            </div>
            <TourLauncher steps={TOUR_STEPS} tourKey="instructor-project-detail"
              className="w-8 h-8 rounded-full bg-white border border-slate-300 shadow-sm flex items-center justify-center text-sm font-bold text-slate-500 hover:bg-indigo-50 hover:text-indigo-600 hover:border-indigo-300 transition-all shrink-0" />
          </div>
        </div>

        {/* Tabs */}
        <div className="flex gap-1 border-b border-gray-200 mb-6">
          {[
            { key: 'setup', label: 'Setup' },
            { key: 'sections', label: 'Sections' },
            { key: 'review', label: 'Review' },
            { key: 'settings', label: 'Settings' },
          ].map(tab => (
            <button
              key={tab.key}
              id={`tab-${tab.key}`}
              onClick={() => setActiveTab(tab.key)}
              className={`px-4 py-2 text-xs font-bold rounded-t-lg transition ${
                activeTab === tab.key ? 'bg-white text-[#1e3a8a] border border-b-white border-gray-200 -mb-px' : 'text-gray-500 hover:text-gray-700'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {/* Tab: Setup */}
        {activeTab === 'setup' && (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div id="source-documents" className="bg-white rounded-2xl border border-gray-200 p-6 shadow-sm">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-sm font-bold text-[#1e3a8a]">Source Documents</h2>
                <button onClick={() => setShowAddSource(true)} className="px-3 py-1.5 bg-indigo-600 text-white text-xs font-bold rounded-lg hover:bg-indigo-700">+ Add Source</button>
              </div>
              {sources.length === 0 ? (
                <p className="text-xs text-gray-400 italic">No source documents yet. Click "Add Source" to import.</p>
              ) : (
                <div className="space-y-1">
                  {sources.map(s => (
                    <button key={s.id} onClick={() => { setSourceDetail(s); setShowSourceDetail(true); }} className="w-full text-left bg-gray-50 rounded-lg px-3 py-2 text-xs hover:bg-gray-100 transition flex items-center justify-between">
                      <span className="font-medium">{s.title || s.originalFilename || s.id}</span>
                      <StatusBadge status={s.processingStatus || 'READY'} />
                    </button>
                  ))}
                </div>
              )}
            </div>
            <div id="set-up-paper" className="bg-white rounded-2xl border border-gray-200 p-6 shadow-sm">
              <h2 className="text-sm font-bold text-[#1e3a8a] mb-4">Set up Paper</h2>
              {standard && (
                <div className="mb-3 bg-indigo-50 rounded-lg px-3 py-2 text-xs flex items-center justify-between">
                  <span className="font-medium text-indigo-700">Standard: {standard}</span>
                  <button onClick={() => { setSetupMode('standard'); setShowSetUpPaper(true); }} className="text-indigo-600 hover:text-indigo-800 font-bold text-[10px]">Change</button>
                </div>
              )}
              {papers.length > 0 && (
                <div className="mb-3 space-y-1">
                  <p className="text-[10px] font-bold text-gray-400 uppercase tracking-wider">Uploaded Papers</p>
                  {papers.map(p => (
                    <div key={p.id} className="flex items-center justify-between bg-gray-50 rounded-lg px-3 py-2 text-xs">
                      <span className="font-medium">{p.originalFilename || p.title}</span>
                      <StatusBadge status={p.processingStatus || 'READY'} />
                    </div>
                  ))}
                </div>
              )}
              {!standard && papers.length === 0 && (
                <p className="text-xs text-gray-400 italic mb-3">No standard or paper configured.</p>
              )}
              {hasAssignedSections ? (
                <div className="w-full px-4 py-2 bg-gray-200 text-gray-500 text-xs font-bold rounded-lg text-center flex items-center justify-center gap-1">
                  {'\u{1F512}'} Setup locked — sections have been assigned
                </div>
              ) : (
                <button onClick={() => { setSetupMode(standard ? 'standard' : 'paper'); setShowSetUpPaper(true); }} className="w-full px-4 py-2 bg-emerald-600 text-white text-xs font-bold rounded-lg hover:bg-emerald-700">
                  {standard || papers.length > 0 ? 'Update Setup' : 'Set up Paper'}
                </button>
              )}
            </div>
          </div>
        )}

        {/* Tab: Sections */}
        {activeTab === 'sections' && (
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <div className="lg:col-span-1 bg-white rounded-2xl border border-gray-200 p-6 shadow-sm">
              <div className="flex justify-between items-center mb-4">
                <h2 className="text-sm font-bold text-[#1e3a8a]">Papers</h2>
              </div>
              {papers.length === 0 ? (
                <p className="text-xs text-gray-400 italic">Upload a paper in Setup tab first.</p>
              ) : (
                <div className="space-y-1">
                  {papers.map(p => (
                    <button
                      key={p.id}
                      onClick={() => { setSelectedPaper(p); loadSections(p.id); }}
                      className={`w-full text-left px-3 py-2 rounded-lg text-xs transition ${
                        selectedPaper?.id === p.id ? 'bg-indigo-50 text-indigo-700 border border-indigo-200' : 'hover:bg-gray-50'
                      }`}
                    >
                      <span className="font-medium">{p.originalFilename || p.title}</span>
                    </button>
                  ))}
                </div>
              )}
            </div>
            <div className="lg:col-span-2 bg-white rounded-2xl border border-gray-200 p-6 shadow-sm">
              <div className="flex justify-between items-center mb-4">
                <h2 className="text-sm font-bold text-[#1e3a8a]">Sections</h2>
                <div className="flex gap-2">
                  {standard && selectedPaper && sections.length === 0 && (
                    <button onClick={handleAutoGenerateSections} disabled={autoGenLoading} className="px-3 py-1.5 bg-indigo-600 text-white text-xs font-bold rounded-lg hover:bg-indigo-700 disabled:opacity-50">{autoGenLoading ? '...' : 'Auto-gen'}</button>
                  )}
                  {!standard && selectedPaper && sections.length === 0 && (
                    <button onClick={handleDetectSections} className="px-3 py-1.5 bg-indigo-600 text-white text-xs font-bold rounded-lg hover:bg-indigo-700">Detect</button>
                  )}
                  {selectedPaper && (
                    <button onClick={async () => {
                      try {
                        const res = await api.get(`/api/papers/${selectedPaper.id}/validate`);
                        alert(JSON.stringify(res.data, null, 2));
                      } catch {}
                    }} className="px-3 py-1.5 bg-amber-600 text-white text-xs font-bold rounded-lg hover:bg-amber-700">Validate</button>
                  )}
                </div>
              </div>
              {!selectedPaper ? (
                <p className="text-xs text-gray-400 italic">Select a paper to see sections.</p>
              ) : sections.length === 0 ? (
                <div className="text-xs text-gray-400 italic">
                  {standard ? (
                    <p>Standard <strong>{standard}</strong> selected. Click <strong>Auto-gen</strong> to create empty section templates.</p>
                  ) : (
                    <p>Upload a paper in <strong>Setup</strong>, then click <strong>Detect</strong> to extract sections from content.</p>
                  )}
                </div>
              ) : (
                <div className="space-y-2">
                  {sections.map(s => (
                    <div key={s.id} className="flex items-center justify-between bg-gray-50 rounded-lg px-4 py-3 text-xs">
                      <div className="flex items-center gap-3">
                        <span className="font-mono text-gray-400 w-4">{s.sectionOrder != null ? s.sectionOrder + 1 : '?'}</span>
                        <span className="font-medium">{s.sectionTitle}</span>
                        {s.version > 1 && <span className="text-[9px] bg-amber-100 text-amber-700 px-1.5 py-0.5 rounded font-bold">v{s.version}</span>}
                        {s.assignedUserId && (
                          <span className="flex items-center gap-1 text-[9px] bg-gray-200 text-gray-600 px-1.5 py-0.5 rounded font-bold">
                            {'\u{1F512}'} {displayName(projectMembers.find(m => m.userId === s.assignedUserId))}
                          </span>
                        )}
                      </div>
                      <div className="flex items-center gap-2">
                        <select
                          value={s.assignedUserId || ''}
                          onChange={e => handleAssignSection(s.id, e.target.value)}
                          className="border border-gray-200 rounded px-2 py-1 text-[10px] outline-none"
                        >
                          <option value="">Unassigned</option>
                          {projectMembers.filter(m => m.userId !== currentUser?.id).map(m => (
                            <option key={m.id} value={m.userId}>{displayName(m)} <span className="text-gray-400">({m.role})</span></option>
                          ))}
                        </select>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        )}

        {/* Tab: Review */}
        {activeTab === 'review' && (
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <div className="lg:col-span-1 bg-white rounded-2xl border border-gray-200 p-6 shadow-sm">
              <h2 className="text-sm font-bold text-[#1e3a8a] mb-4">Feedback Requests</h2>
              {feedbackRequests.length === 0 ? (
                <p className="text-xs text-gray-400 italic">No review requests yet.</p>
              ) : (
                <div className="space-y-2">
                  {feedbackRequests.map(fb => (
                    <div key={fb.id} className="bg-gray-50 rounded-lg px-3 py-2 text-xs">
                      <div className="flex justify-between items-center">
                        <StatusBadge status={fb.status} />
                        <span className="text-gray-400">{fb.requestedAt ? new Date(fb.requestedAt).toLocaleDateString() : ''}</span>
                      </div>
                      <p className="text-gray-500 mt-1">Student: {fb.studentId}</p>
                    </div>
                  ))}
                </div>
              )}
            </div>
            <div className="lg:col-span-2 bg-white rounded-2xl border border-gray-200 p-6 shadow-sm">
              <h2 className="text-sm font-bold text-[#1e3a8a] mb-4">Evidence Map</h2>
              <EvidenceGraph traceabilityData={traceability} height={500} />
            </div>
          </div>
        )}

        {/* Tab: Settings */}
        {activeTab === 'settings' && (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div id="status-controls" className="bg-white rounded-2xl border border-gray-200 p-6 shadow-sm">
              <h2 className="text-sm font-bold text-[#1e3a8a] mb-4">Status Controls</h2>
              <div className="space-y-3">
                {project.status === 'IN_PROGRESS' && (
                  <button onClick={() => handlePatch('complete')} className="w-full px-4 py-2 bg-blue-600 text-white text-xs font-bold rounded-lg hover:bg-blue-700">Mark Complete → APPROVED</button>
                )}
                {project.status !== 'ARCHIVED' && (
                  <button onClick={() => handlePatch('archive')} className="w-full px-4 py-2 bg-amber-600 text-white text-xs font-bold rounded-lg hover:bg-amber-700">Archive</button>
                )}
                {project.status === 'ARCHIVED' && (
                  <button onClick={() => handlePatch('unarchive')} className="w-full px-4 py-2 bg-emerald-600 text-white text-xs font-bold rounded-lg hover:bg-emerald-700">Unarchive</button>
                )}
                <p className="text-[10px] text-gray-400">Current status: <StatusBadge status={project.status} /></p>
              </div>
            </div>
            <div id="project-members" className="bg-white rounded-2xl border border-gray-200 p-6 shadow-sm">
              <div className="flex justify-between items-center mb-4">
                <h2 className="text-sm font-bold text-[#1e3a8a]">Members</h2>
                <button onClick={() => { setShowAddMember(true); loadUsers(); }} className="px-3 py-1.5 bg-indigo-600 text-white text-xs font-bold rounded-lg hover:bg-indigo-700">+ Add</button>
              </div>
              {projectMembers.length === 0 ? (
                <p className="text-xs text-gray-400 italic">No members.</p>
              ) : (
                <div className="space-y-2">
                  {projectMembers.map(m => (
                    <div key={m.id} className="flex items-center justify-between bg-gray-50 rounded-lg px-3 py-2 text-xs">
                      <div className="flex flex-col">
                        <span className="font-medium">{displayName(m)}</span>
                        <span className="text-gray-400">{m.email}</span>
                      </div>
                      <div className="flex items-center gap-3">
                        <span className="text-[10px] bg-blue-100 text-blue-700 px-1.5 py-0.5 rounded font-bold">{m.userRole}</span>
                        <span className="text-[10px] bg-gray-100 text-gray-600 px-1.5 py-0.5 rounded">{m.role}</span>
                        <button onClick={() => handleRemoveMember(m.userId)} className="text-rose-600 hover:text-rose-800 font-bold">Remove</button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        )}
      </div>

      <Modal open={showAddMember} onClose={() => setShowAddMember(false)} title="Add Member">
        <div className="space-y-4">
          <select value={newMemberId} onChange={e => setNewMemberId(e.target.value)} className="w-full border border-gray-200 rounded-lg px-3 py-2 text-xs outline-none">
            <option value="">Select student...</option>
            {users.filter(u => u.role === 'STUDENT' && !projectMembers.find(m => m.userId === u.id)).map(u => (
              <option key={u.id} value={u.id}>{u.firstName || u.lastName ? `${u.firstName || ''} ${u.lastName || ''}`.trim() : u.email}</option>
            ))}
          </select>
          <div className="flex justify-end gap-2">
            <button onClick={() => setShowAddMember(false)} className="px-4 py-2 text-xs font-semibold text-gray-600 bg-gray-100 rounded-lg hover:bg-gray-200">{ct.cancel}</button>
            <button onClick={handleAddMember} disabled={!newMemberId} className="px-4 py-2 text-xs font-bold text-white bg-emerald-600 rounded-lg hover:bg-emerald-700 disabled:opacity-50">{ct.save}</button>
          </div>
        </div>
      </Modal>

      <Modal open={!!pendingAssign} onClose={() => setPendingAssign(null)} title="Assign Section">
        <div className="space-y-4 text-xs">
          <p className="text-gray-600">
            Assign section to <strong>{pendingAssign?.userName}</strong>?
          </p>
          <p className="text-amber-700 bg-amber-50 border border-amber-200 rounded-lg px-3 py-2">
            Once assigned, only the assigned student can edit this section. Instructors will have read-only access.
          </p>
          <div className="flex justify-end gap-2">
            <button onClick={() => setPendingAssign(null)} className="px-4 py-2 text-xs font-semibold text-gray-600 bg-gray-100 rounded-lg hover:bg-gray-200">{ct.cancel}</button>
            <button onClick={() => handleConfirmAssign(pendingAssign?.userId, pendingAssign?.sectionId)} className="px-4 py-2 text-xs font-bold text-white bg-emerald-600 rounded-lg hover:bg-emerald-700">Confirm</button>
          </div>
        </div>
      </Modal>

      <Modal open={showSourceDetail} onClose={() => setShowSourceDetail(false)} title="Source Detail">
        {sourceDetail && (
          <div className="space-y-3 text-xs">
            <div><span className="font-bold text-gray-500">Title:</span> <span className="text-gray-800">{sourceDetail.title || '-'}</span></div>
            <div><span className="font-bold text-gray-500">Filename:</span> <span className="text-gray-800">{sourceDetail.originalFilename || '-'}</span></div>
            <div><span className="font-bold text-gray-500">DOI:</span> <span className="text-gray-800 font-mono">{sourceDetail.doi || '-'}</span></div>
            <div><span className="font-bold text-gray-500">Status:</span> <StatusBadge status={sourceDetail.processingStatus || 'READY'} /></div>
            <div><span className="font-bold text-gray-500">Type:</span> <span className="text-gray-800">{sourceDetail.docType || 'SOURCE'}</span></div>
            <div><span className="font-bold text-gray-500">ID:</span> <span className="text-gray-800 font-mono text-[9px]">{sourceDetail.id}</span></div>
            <div className="flex justify-end gap-2 pt-2">
              <button onClick={() => setShowSourceDetail(false)} className="px-4 py-2 text-xs font-semibold text-gray-600 bg-gray-100 rounded-lg hover:bg-gray-200">Close</button>
            </div>
          </div>
        )}
      </Modal>

      <Modal open={showAddSource} onClose={() => setShowAddSource(false)} title="Add Source">
        <div className="space-y-5 text-xs">
          <div className="border border-gray-200 rounded-xl p-4 space-y-3">
            <h3 className="font-bold text-indigo-700">① Import by DOI</h3>
            <div className="flex gap-2">
              <input value={doiInput} onChange={e => setDoiInput(e.target.value)} placeholder="10.1000/xyz123" className="flex-1 border border-gray-200 rounded-lg px-3 py-2 outline-none focus:ring-1 focus:ring-indigo-500" />
              <button onClick={() => { handleImportDoi(); setShowAddSource(false); }} disabled={importingDoi} className="px-3 py-2 bg-indigo-600 text-white font-bold rounded-lg hover:bg-indigo-700 disabled:opacity-50">Fetch</button>
            </div>
          </div>
          <div className="border border-gray-200 rounded-xl p-4 space-y-3">
            <h3 className="font-bold text-amber-700">② Upload Source (PDF/DOCX)</h3>
            <input type="file" accept=".pdf,.docx" onChange={(e) => { handleUploadSource(e); setShowAddSource(false); }} className="text-xs" />
          </div>
          <div className="border border-gray-200 rounded-xl p-4 space-y-3">
            <h3 className="font-bold text-emerald-700">③ Import DOI as Source</h3>
            <div className="flex gap-2">
              <input value={doiInput} onChange={e => setDoiInput(e.target.value)} placeholder="10.1000/xyz123" className="flex-1 border border-gray-200 rounded-lg px-3 py-2 outline-none focus:ring-1 focus:ring-indigo-500" />
              <button onClick={() => { handleImportDoiSource(); setShowAddSource(false); }} disabled={importDoiSourceLoading} className="px-3 py-2 bg-emerald-600 text-white font-bold rounded-lg hover:bg-emerald-700 disabled:opacity-50">Go</button>
            </div>
          </div>
          <div className="border border-gray-200 rounded-xl p-4 space-y-3">
            <h3 className="font-bold text-rose-700">④ Share from Collection</h3>
            <button onClick={() => { setShowShareCollection(true); loadCollections(); setShowAddSource(false); }} className="px-3 py-2 bg-rose-600 text-white font-bold rounded-lg hover:bg-rose-700">Browse Collections</button>
          </div>
          <div className="flex justify-end gap-2">
            <button onClick={() => setShowAddSource(false)} className="px-4 py-2 text-xs font-semibold text-gray-600 bg-gray-100 rounded-lg hover:bg-gray-200">{ct.cancel}</button>
          </div>
        </div>
      </Modal>

      <Modal open={showSetUpPaper} onClose={() => setShowSetUpPaper(false)} title="Set up Paper">
        {hasAssignedSections ? (
          <div className="space-y-4 text-xs">
            <div className="bg-amber-50 border border-amber-200 rounded-lg px-4 py-3 flex items-center gap-2">
              <span>{'\u{1F512}'}</span>
              <span className="text-amber-800">Setup is locked because sections have been assigned to students. Unassign all sections to make changes.</span>
            </div>
            <div className="flex justify-end">
              <button onClick={() => setShowSetUpPaper(false)} className="px-4 py-2 text-xs font-semibold text-gray-600 bg-gray-100 rounded-lg hover:bg-gray-200">{ct.close || 'Close'}</button>
            </div>
          </div>
        ) : (
        <div className="space-y-5 text-xs">
          <div className="flex gap-1 bg-gray-100 rounded-lg p-1">
            <button onClick={() => setSetupMode('standard')}
              className={`flex-1 px-3 py-2 rounded-md text-xs font-bold transition ${setupMode === 'standard' ? 'bg-white text-indigo-700 shadow-sm' : 'text-gray-500 hover:text-gray-700'}`}>
              📋 Choose Standard
            </button>
            <button onClick={() => setSetupMode('paper')}
              className={`flex-1 px-3 py-2 rounded-md text-xs font-bold transition ${setupMode === 'paper' ? 'bg-white text-amber-700 shadow-sm' : 'text-gray-500 hover:text-gray-700'}`}>
              📄 Upload Paper
            </button>
          </div>

          {setupMode === 'standard' && (
            <div className="border border-gray-200 rounded-xl p-4 space-y-3">
              <h3 className="font-bold text-indigo-700">Choose Standard</h3>
              <p className="text-gray-400">Select a paper format standard. This creates empty section templates.</p>
              <select value={standard} onChange={e => setStandard(e.target.value)} className="w-full border border-gray-200 rounded-lg px-3 py-2 outline-none focus:ring-1 focus:ring-indigo-500">
                <option value="">No standard</option>
                {STANDARDS.map(s => <option key={s} value={s}>{s}</option>)}
              </select>
              <button onClick={() => { handleUpdateStandard(); setShowSetUpPaper(false); }} disabled={saving} className="px-4 py-2 bg-emerald-600 text-white font-bold rounded-lg hover:bg-emerald-700 disabled:opacity-50">{saving ? ct.saving : 'Save Standard'}</button>
            </div>
          )}

          {setupMode === 'paper' && (
            <div className="border border-gray-200 rounded-xl p-4 space-y-3">
              <h3 className="font-bold text-amber-700">Upload Paper</h3>
              <p className="text-gray-400">Upload a student paper. System will detect sections from content.</p>
              <input type="file" accept=".pdf,.docx" onChange={(e) => { handleUploadPaper(e); setShowSetUpPaper(false); }} className="text-xs" />
            </div>
          )}

          <div className="flex justify-end gap-2">
            <button onClick={() => setShowSetUpPaper(false)} className="px-4 py-2 text-xs font-semibold text-gray-600 bg-gray-100 rounded-lg hover:bg-gray-200">{ct.cancel}</button>
          </div>
        </div>
        )}
      </Modal>

      <Modal open={showShareCollection} onClose={() => { setShowShareCollection(false); setSelectedCollectionId(''); setCollectionSources([]); }} title="Share from Collection">
        <div className="space-y-4 text-xs">
          {collections.length === 0 ? (
            <p className="text-gray-400 italic">No collections found.</p>
          ) : (
            <select value={selectedCollectionId} onChange={async (e) => {
              setSelectedCollectionId(e.target.value);
              if (e.target.value) {
                try {
                  const res = await api.get(`/api/collections/${e.target.value}/sources`);
                  setCollectionSources(res.data?.content || res.data || []);
                } catch { setCollectionSources([]); }
              } else { setCollectionSources([]); }
            }} className="w-full border border-gray-200 rounded-lg px-3 py-2 text-xs outline-none">
              <option value="">Select collection...</option>
              {collections.map(c => <option key={c.id} value={c.id}>{c.name || c.title || c.id}</option>)}
            </select>
          )}
          {selectedCollectionId && collectionSources.length === 0 && (
            <p className="text-gray-400 italic">No sources in this collection.</p>
          )}
          {collectionSources.length > 0 && (
            <div className="max-h-48 overflow-y-auto space-y-1 border border-gray-100 rounded-lg p-1">
              {collectionSources.map(s => (
                <div key={s.id} className="flex items-center justify-between px-3 py-2 bg-gray-50 rounded-lg">
                  <span className="font-medium">{s.title || s.originalFilename || s.id}</span>
                  <button onClick={() => handleShareSource(s.id)} disabled={shareLoading} className="px-2 py-1 bg-indigo-600 text-white text-[10px] font-bold rounded hover:bg-indigo-700 disabled:opacity-50">{shareLoading ? '...' : 'Share'}</button>
                </div>
              ))}
            </div>
          )}
          <div className="flex justify-end gap-2">
            <button onClick={() => { setShowShareCollection(false); setSelectedCollectionId(''); setCollectionSources([]); }} className="px-4 py-2 text-xs font-semibold text-gray-600 bg-gray-100 rounded-lg hover:bg-gray-200">Close</button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
