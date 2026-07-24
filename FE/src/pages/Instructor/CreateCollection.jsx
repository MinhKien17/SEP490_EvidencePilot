import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { LoadingSkeleton, TourLauncher, AppHeader } from '../../components';
import { instructorText, commonText } from '../../locales';
import { useLanguage } from '../../context/LanguageContext';
import api from '../../api.js';

const TOUR_STEPS = [
  { element: '#collection-name', popover: { title: 'Collection Name', description: 'Give your evidence collection a descriptive name.', side: 'top', align: 'start' } },
  { element: '#collection-desc', popover: { title: 'Description', description: 'Describe the purpose and scope of this collection.', side: 'top', align: 'center' } },
  { element: '#collection-project', popover: { title: 'Project Association', description: 'Optionally link this collection to a student project.', side: 'top', align: 'end' } },
];

export default function CreateCollection() {
  const navigate = useNavigate();
  const { language } = useLanguage();
  const t = instructorText[language];
  const ct = commonText[language];

  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [projectId, setProjectId] = useState('');
  const [projects, setProjects] = useState([]);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  useEffect(() => {
    const fetchActiveProjects = async () => {
      setLoading(true);
      try { const res = await api.get('/api/projects?size=100'); setProjects(res.data?.content || []); }
      catch { setErrorMessage('Failed to load projects.'); }
      finally { setLoading(false); }
    };
    fetchActiveProjects();
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!name.trim()) return;
    setSubmitting(true); setErrorMessage('');
    try {
      await api.post('/api/collections', { name: name.trim(), description: description.trim() || null, projectId: projectId || null });
      navigate('/instructor/collections');
    } catch (err) {
      setErrorMessage(err.response?.data?.message || 'Failed to create collection.');
    } finally { setSubmitting(false); }
  };

  return (
    <div className="min-h-screen bg-[#f8fafc] text-[#0f172a] font-sans">
      <AppHeader />
      <div className="max-w-2xl mx-auto p-8">
        <button type="button" onClick={() => navigate('/instructor/collections')}
          className="text-xs font-bold text-gray-400 hover:text-[#1e3a8a] transition flex items-center gap-1 mb-4">&larr; {ct.back}</button>

        <div className="mb-8 border-b border-gray-200 pb-6">
          <h1 className="text-3xl font-black text-[#1e3a8a] tracking-tight">{t.createCollection}</h1>
          <p className="text-xs text-gray-400 mt-1">{t.collectionDescription}</p>
        </div>

        {errorMessage && (
          <div className="p-4 mb-6 rounded-2xl bg-rose-50 border border-rose-100 text-rose-700 text-xs font-bold">{errorMessage}</div>
        )}

        <div className="bg-white rounded-3xl border border-gray-200 shadow-sm p-8">
          {loading ? (
            <LoadingSkeleton count={4} height="h-12" />
          ) : (
            <form onSubmit={handleSubmit} className="space-y-6 text-xs">
              <div className="space-y-1.5">
                <label className="text-gray-500 font-black uppercase tracking-wide text-[10px]">{ct.name} <span className="text-rose-500">*</span></label>
                <input id="collection-name" type="text" required value={name} onChange={(e) => setName(e.target.value)}
                  placeholder="e.g., Autumn 2026 Research Collection"
                  className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl font-medium text-gray-800 focus:outline-none focus:ring-2 focus:ring-[#1e3a8a] focus:bg-white transition" />
              </div>

              <div className="space-y-1.5">
                <label className="text-gray-500 font-black uppercase tracking-wide text-[10px]">{ct.description}</label>
                <textarea id="collection-desc" rows="5" value={description} onChange={(e) => setDescription(e.target.value)}
                  placeholder={t.collectionDescription}
                  className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl font-medium text-gray-800 focus:outline-none focus:ring-2 focus:ring-[#1e3a8a] focus:bg-white transition" />
              </div>

              <div className="space-y-1.5">
                <label className="text-gray-500 font-black uppercase tracking-wide text-[10px]">{t.project} <span className="text-gray-400 font-normal">({ct.no})</span></label>
                <select id="collection-project" value={projectId} onChange={(e) => setProjectId(e.target.value)}
                  className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl font-medium text-gray-700 focus:outline-none focus:ring-2 focus:ring-[#1e3a8a] focus:bg-white transition">
                  <option value="">-- {ct.no} --</option>
                  {projects.map(p => <option key={p.id} value={p.id}>{p.title}</option>)}
                </select>
              </div>

              <div className="flex items-center gap-3 pt-4 border-t border-gray-100 font-bold">
                <button type="button" onClick={() => navigate('/instructor/collections')}
                  className="flex-1 py-3 bg-gray-50 hover:bg-gray-100 text-gray-600 rounded-xl transition text-center border border-gray-200/60">{ct.cancel}</button>
                <button type="submit" disabled={submitting}
                  className="flex-1 py-3 bg-[#1e3a8a] text-white rounded-xl hover:bg-blue-800 transition shadow-md disabled:opacity-50 text-center">
                  {submitting ? ct.saving : ct.create}
                </button>
              </div>
            </form>
          )}
        </div>
      </div>
      <TourLauncher steps={TOUR_STEPS} tourKey="instructor-create-collection" />
    </div>
  );
}
