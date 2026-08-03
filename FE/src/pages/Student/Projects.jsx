import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AppHeader, EmptyState, EntityCard, LoadingSkeleton, TourLauncher } from '../../components';
import { commonText, studentText } from '../../locales';
import { useLanguage } from '../../context/LanguageContext';
import api from '../../api.js';

export default function Projects() {
  const navigate = useNavigate();
  const { language } = useLanguage();
  const t = studentText[language];
  const ct = commonText[language];
  const [projects, setProjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const tourSteps = [
    { element: '#projects-grid', popover: { title: t.tourProjects, description: t.tourProjectsDesc, side: 'bottom', align: 'start' } },
    { element: '.project-card:first', popover: { title: t.tourWorkspace, description: t.tourWorkspaceDesc, side: 'top', align: 'center' } },
  ];

  const fetchProjects = async () => {
    try {
      setLoading(true);
      const res = await api.get('/api/projects');
      setProjects(Array.isArray(res.data?.content) ? res.data.content : []);
      setError(false);
    } catch (err) {
      console.error('Failed to fetch projects:', err);
      setError(true);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchProjects(); }, []);

  return (
    <div className="min-h-screen bg-(--page-bg) text-(--text-primary) font-sans">
      <AppHeader />

      <main className="max-w-6xl mx-auto px-4 sm:px-6 py-8">
        <div className="mb-8">
          <h1 className="text-2xl font-bold text-(--text-primary)">{t.projects}</h1>
          <p className="text-sm text-(--text-secondary) mt-1">{t.workspaceDescription}</p>
        </div>

        {loading ? <LoadingSkeleton count={6} height="h-28" /> : error ? (
          <div className="bg-rose-50 dark:bg-rose-950/30 border border-rose-200 dark:border-rose-900 text-rose-700 dark:text-rose-300 p-4 rounded-xl text-center">
            <p className="font-semibold">{t.projectsLoadFailed}</p>
            <button onClick={fetchProjects} className="mt-3 px-4 py-2 bg-rose-600 text-white rounded-lg text-xs font-bold hover:bg-rose-700 transition-colors">{ct.retry}</button>
          </div>
        ) : projects.length === 0 ? (
          <EmptyState title={t.noProjects} description={t.noProjectsDescription} />
        ) : (
          <div id="projects-grid" className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
            {projects.map((project) => (
              <div key={project.id} className="project-card">
                <EntityCard
                  title={project.title}
                  subtitle={project.description || t.noDescription}
                  status={project.status}
                  onClick={() => navigate(`/student/projects/${project.id}`)}
                >
                  <div className="flex items-center justify-between text-[11px] text-(--text-tertiary) pt-2 mt-2 border-t border-(--border-light)">
                    <span>{t.createdOn.replace('{{date}}', project.createdAt ? new Date(project.createdAt).toLocaleDateString(language === 'vi' ? 'vi-VN' : 'en-US') : '—')}</span>
                    <span className="text-(--brand) font-semibold" aria-hidden="true">&rarr;</span>
                  </div>
                </EntityCard>
              </div>
            ))}
          </div>
        )}
      </main>

      <TourLauncher steps={tourSteps} tourKey="projects" />
    </div>
  );
}
