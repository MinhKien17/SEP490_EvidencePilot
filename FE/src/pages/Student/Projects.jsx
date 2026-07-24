import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { EntityCard, StatusBadge, EmptyState, LoadingSkeleton, Modal, TourLauncher, AppHeader } from '../../components';
import { studentText, commonText } from '../../locales';
import { useLanguage } from '../../context/LanguageContext';
import api from '../../api.js';

const TOUR_STEPS = [
  { element: '#projects-grid', popover: { title: 'Your Projects', description: 'Browse and manage all your research projects in one place.', side: 'bottom', align: 'start' } },
  { element: '#create-project-btn', popover: { title: 'Create New Project', description: 'Start a new research project and define your scope.', side: 'left', align: 'center' } },
  { element: '.project-card:first', popover: { title: 'Open Workspace', description: 'Click any project card to open its full workspace with papers, sources, claims, and feedback.', side: 'top', align: 'center' } },
];

export default function Projects() {
  const navigate = useNavigate();
  const { logout, role } = useAuth();
  const { language } = useLanguage();
  const t = studentText[language];
  const ct = commonText[language];
  const [projects, setProjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [newTitle, setNewTitle] = useState('');
  const [newDescription, setNewDescription] = useState('');
  const [creating, setCreating] = useState(false);

  const fetchProjects = async () => {
    try {
      setLoading(true);
      const res = await api.get('/api/projects');
      setProjects(Array.isArray(res.data) ? res.data : []);
      setError('');
    } catch (err) {
      console.error('Failed to fetch projects:', err);
      setError('Failed to load projects.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchProjects(); }, []);

  const handleCreateProject = async (e) => {
    e.preventDefault();
    if (!newTitle.trim()) return;
    setCreating(true);
    try {
      const res = await api.post('/api/projects', { title: newTitle.trim(), description: newDescription.trim() });
      setShowCreateModal(false); setNewTitle(''); setNewDescription('');
      navigate(`/student/projects/${res.data.id}`);
    } catch (err) {
      console.error('Failed to create project:', err);
      alert(err.response?.data?.message || 'Failed to create project.');
    } finally { setCreating(false); }
  };

  return (
    <div className="min-h-screen bg-slate-50 font-sans">
      <AppHeader />

      <main className="max-w-5xl mx-auto px-6 py-8">
        <div className="flex justify-between items-center mb-8">
          <div>
            <h1 className="text-2xl font-bold text-slate-800">{t.projects}</h1>
            <p className="text-sm text-slate-500 mt-1">{t.workspaceDescription}</p>
          </div>
          {role === 'INSTRUCTOR' && (
            <button id="create-project-btn" onClick={() => setShowCreateModal(true)}
              className="px-5 py-2.5 bg-[#1e3a8a] text-white rounded-lg font-semibold hover:bg-[#152e75] transition shadow-sm flex items-center gap-2 text-sm">
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4v16m8-8H4" /></svg>
              {t.newProject}
            </button>
          )}
        </div>

        {loading ? <LoadingSkeleton count={6} height="h-28" /> : error ? (
          <div className="bg-red-50 border border-red-200 text-red-700 p-4 rounded-lg text-center">
            <p className="font-semibold">{error}</p>
            <button onClick={fetchProjects} className="mt-3 px-4 py-1.5 bg-red-600 text-white rounded text-xs font-bold hover:bg-red-700 transition">{ct.retry}</button>
          </div>
        ) : projects.length === 0 ? (
          <EmptyState icon="📁" title={t.noProjects} description="Create your first project to get started."
            action={role === 'INSTRUCTOR' ? <button onClick={() => setShowCreateModal(true)} className="px-6 py-2.5 bg-[#1e3a8a] text-white rounded-lg font-semibold hover:bg-[#152e75] transition">{t.createProject}</button> : undefined} />
        ) : (
          <div id="projects-grid" className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
            {projects.map((project) => (
              <div key={project.id} className="project-card">
                <EntityCard title={project.title} subtitle={project.description || 'No description provided.'}
                  status={project.status}
                  onClick={() => navigate(`/student/projects/${project.id}`)}>
                  <div className="flex items-center justify-between text-[11px] text-slate-400 pt-2 mt-2 border-t border-slate-100">
                    <span>Created {project.createdAt ? new Date(project.createdAt).toLocaleDateString() : 'N/A'}</span>
                    <span className="text-indigo-600 font-semibold">&rarr;</span>
                  </div>
                </EntityCard>
              </div>
            ))}
          </div>
        )}
      </main>

      {role === 'INSTRUCTOR' && <Modal open={showCreateModal} onClose={() => setShowCreateModal(false)} title={t.createProject}>
        <form onSubmit={handleCreateProject} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">{t.projectName} <span className="text-red-500">*</span></label>
            <input type="text" required value={newTitle} onChange={(e) => setNewTitle(e.target.value)}
              placeholder={t.projectName} className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" />
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">{t.projectDescription}</label>
            <textarea value={newDescription} onChange={(e) => setNewDescription(e.target.value)}
              placeholder={t.projectDescription} rows={3}
              className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" />
          </div>
          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={() => setShowCreateModal(false)}
              className="px-4 py-2 text-sm font-semibold text-slate-600 bg-slate-100 rounded-lg hover:bg-slate-200 transition">{ct.cancel}</button>
            <button type="submit" disabled={creating || !newTitle.trim()}
              className="px-5 py-2 text-sm font-semibold text-white bg-[#1e3a8a] rounded-lg hover:bg-[#152e75] transition disabled:opacity-50">
              {creating ? ct.saving : ct.create}
            </button>
          </div>
        </form>
      </Modal>}

      <TourLauncher steps={TOUR_STEPS} tourKey="projects" />
    </div>
  );
}
