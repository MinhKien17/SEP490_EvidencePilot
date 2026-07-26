import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { EntityCard, EmptyState, LoadingSkeleton, TourLauncher, AppHeader } from '../../components';
import { studentText, commonText } from '../../locales';
import { useLanguage } from '../../context/LanguageContext';
import api from '../../api.js';

const TOUR_STEPS = [
  { element: '#projects-grid', popover: { title: 'Your Projects', description: 'Browse and manage all your research projects in one place.', side: 'bottom', align: 'start' } },
  { element: '.project-card:first', popover: { title: 'Open Workspace', description: 'Click any project card to open its full workspace with papers, sources, claims, and feedback.', side: 'top', align: 'center' } },
];

export default function Projects() {
  const navigate = useNavigate();
  const { logout } = useAuth();
  const { language } = useLanguage();
  const t = studentText[language];
  const ct = commonText[language];
  const [projects, setProjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const fetchProjects = async () => {
    try {
      setLoading(true);
      const res = await api.get('/api/projects');
      setProjects(Array.isArray(res.data?.content) ? res.data.content : []);
      setError('');
    } catch (err) {
      console.error('Failed to fetch projects:', err);
      setError('Failed to load projects.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchProjects(); }, []);

  return (
    <div className="min-h-screen bg-slate-50 font-sans">
      <AppHeader />

      <main className="max-w-5xl mx-auto px-6 py-8">
        <div className="mb-8">
          <h1 className="text-2xl font-bold text-slate-800">{t.projects}</h1>
          <p className="text-sm text-slate-500 mt-1">{t.workspaceDescription}</p>
        </div>

        {loading ? <LoadingSkeleton count={6} height="h-28" /> : error ? (
          <div className="bg-red-50 border border-red-200 text-red-700 p-4 rounded-lg text-center">
            <p className="font-semibold">{error}</p>
            <button onClick={fetchProjects} className="mt-3 px-4 py-1.5 bg-red-600 text-white rounded text-xs font-bold hover:bg-red-700 transition">{ct.retry}</button>
          </div>
        ) : projects.length === 0 ? (
          <EmptyState icon="📁" title={t.noProjects} description="Your instructor will assign you to a project." />
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

      <TourLauncher steps={TOUR_STEPS} tourKey="projects" />
    </div>
  );
}
