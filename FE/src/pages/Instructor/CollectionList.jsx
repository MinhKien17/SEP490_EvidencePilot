import { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { DataTable, LoadingSkeleton, EmptyState, TourLauncher, AppHeader } from '../../components';
import { instructorText, commonText } from '../../locales';
import { useLanguage } from '../../context/LanguageContext';
import api from '../../api.js';

const TOUR_STEPS = [
  { element: '#project-filter', popover: { title: 'Project Filter', description: 'Select a project to view its evidence collections.', side: 'bottom', align: 'start' } },
  { element: '#collection-table', popover: { title: 'Collections', description: 'Browse, manage, and archive evidence collections.', side: 'top', align: 'start' } },
  { element: '#create-collection-btn', popover: { title: 'Create Collection', description: 'Create a new evidence collection for a project.', side: 'left', align: 'center' } },
];

export default function CollectionList() {
  const navigate = useNavigate();
  const { language } = useLanguage();
  const t = instructorText[language];
  const ct = commonText[language];

  const [collections, setCollections] = useState([]);
  const [projects, setProjects] = useState([]);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [selectedProjectId, setSelectedProjectId] = useState('');

  const fetchInitialData = async () => {
    setLoading(true); setErrorMessage('');
    try {
      const projectRes = await api.get('/api/projects?size=100');
      const projectList = projectRes.data?.content || [];
      setProjects(projectList);
      if (projectList.length > 0) {
        const firstId = projectList[0].id;
        setSelectedProjectId(firstId);
        const colRes = await api.get(`/api/projects/${firstId}/collections`);
        setCollections(Array.isArray(colRes.data) ? colRes.data : colRes.data?.content || []);
      }
    } catch { setErrorMessage('Failed to load collections.'); }
    finally { setLoading(false); }
  };

  const handleProjectFilterChange = async (pId) => {
    setSelectedProjectId(pId);
    if (!pId) return;
    setLoading(true);
    try {
      const res = await api.get(`/api/projects/${pId}/collections`);
      setCollections(Array.isArray(res.data) ? res.data : res.data?.content || []);
    } catch { setErrorMessage('Failed to filter collections.'); }
    finally { setLoading(false); }
  };

  const handleDeleteCollection = async (id) => {
    if (!window.confirm(t.deleteConfirm)) return;
    try { await api.delete(`/api/collections/${id}`); setCollections(prev => prev.filter(item => item.id !== id)); }
    catch { setErrorMessage('Failed to delete collection.'); }
  };

  useEffect(() => { fetchInitialData(); }, []);

  const columns = [
    { key: 'id', label: 'ID', render: (row) => <span className="font-mono text-gray-400 text-[11px]">{row.id}</span> },
    { key: 'name', label: ct.name, render: (row) => <span className="font-bold text-gray-900">{row.name || row.title}</span> },
    { key: 'description', label: ct.description, render: (row) => <span className="text-gray-500 max-w-xs truncate block">{row.description || '\u2014'}</span> },
    {
      key: 'actions', label: '', sortable: false,
      render: (row) => (
        <button onClick={(e) => { e.stopPropagation(); handleDeleteCollection(row.id); }}
          className="text-xs font-bold text-rose-600 hover:underline">{ct.delete}</button>
      ),
    },
  ];

  return (
    <div className="min-h-screen bg-[#f8fafc] text-[#0f172a]">
      <AppHeader />
      <div className="max-w-7xl mx-auto p-8">
        <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 mb-8 border-b border-gray-200 pb-6">
          <div>
            <div className="mb-2">
              <Link to="/instructor/dashboard" className="text-xs font-bold text-gray-400 hover:text-[#1e3a8a] transition-colors">&larr; {ct.back}</Link>
            </div>
            <h1 className="text-3xl font-black text-[#1e3a8a] tracking-tight">{t.collections}</h1>
            <p className="text-xs text-gray-400 mt-1">{t.createCollection}</p>
          </div>
          <button id="create-collection-btn" onClick={() => navigate('/instructor/collections/create')}
            className="px-5 py-2.5 bg-[#1e3a8a] text-white font-black text-xs rounded-xl hover:bg-blue-800 transition shadow-sm">
            + {t.createCollection}
          </button>
        </div>

        {errorMessage && (
          <div className="p-4 mb-6 rounded-xl bg-rose-50 border border-rose-100 text-rose-700 text-xs font-bold">{errorMessage}</div>
        )}

        <div id="project-filter" className="bg-white p-4 rounded-2xl border border-gray-200 shadow-sm mb-6 flex items-center gap-4">
          <label className="text-xs font-black text-gray-500 uppercase tracking-wide">{t.project}:</label>
          <select value={selectedProjectId} onChange={(e) => handleProjectFilterChange(e.target.value)}
            className="px-3 py-1.5 bg-gray-50 border border-gray-200 text-xs rounded-lg text-gray-800 font-medium focus:outline-none">
            {projects.map(p => <option key={p.id} value={p.id}>{p.title}</option>)}
          </select>
        </div>

        <div id="collection-table">
          {loading ? <LoadingSkeleton count={4} height="h-12" /> : collections.length === 0 ? (
            <EmptyState title={t.noCollections} description={t.createCollection}
              action={<button onClick={() => navigate('/instructor/collections/create')}
                className="px-4 py-2 bg-[#1e3a8a] text-white font-bold text-xs rounded-xl hover:bg-blue-800 transition">{t.createCollection}</button>} />
          ) : <DataTable columns={columns} data={collections} pageSize={10} />}
        </div>
      </div>
      <TourLauncher steps={TOUR_STEPS} tourKey="instructor-collections" />
    </div>
  );
}
