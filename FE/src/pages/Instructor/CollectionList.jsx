import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { EntityCard, Modal, EmptyState, TourLauncher, AppHeader } from '../../components';
import { instructorText, commonText } from '../../locales';
import { useLanguage } from '../../context/LanguageContext';
import { useCollections } from '../../hooks/useCollections';
import api from '../../api';

const TOUR_STEPS = [
  { element: '#collection-grid', popover: { title: 'Collections', description: 'Browse, manage, and organize evidence collections.', side: 'top', align: 'start' } },
  { element: '#create-collection-btn', popover: { title: 'Create Collection', description: 'Create a new evidence collection.', side: 'left', align: 'center' } },
];

export default function CollectionList() {
  const navigate = useNavigate();
  const { language } = useLanguage();
  const t = instructorText[language];
  const ct = commonText[language];
  const [categories, setCategories] = useState([]);

  useEffect(() => {
    api.get('/api/collection-categories').then(r => setCategories(r.data)).catch(() => {});
  }, []);

  const [search, setSearch] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('');
  const { content: collections, loading, error, refetch } = useCollections(0, 100, 'createdAt,desc', search || undefined, categoryFilter || undefined);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleEdit = (col) => {
    setEditing(col.id);
    setName(col.name);
    setDescription(col.description || '');
    setCategoryId(col.categoryId || '');
    setModalOpen(true);
  };

  const handleDelete = async (id) => {
    const shared = collections.filter(c => c.id === id)[0];
    if (!window.confirm(t.deleteConfirm)) return;
    try { await api.delete(`/api/collections/${id}`); refetch(); }
    catch { alert('Failed to delete collection.'); }
  };

  const resetForm = () => { setName(''); setDescription(''); setCategoryId(''); setEditing(null); };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!name.trim()) return;
    setSubmitting(true);
    try {
      if (editing) {
        await api.put(`/api/collections/${editing}`, { name: name.trim(), description: description.trim() || null, categoryId: categoryId || null });
      } else {
        await api.post('/api/collections', { name: name.trim(), description: description.trim() || null, categoryId: categoryId || null });
      }
      resetForm(); setModalOpen(false); refetch();
    } catch { alert('Failed to save collection.'); }
    finally { setSubmitting(false); }
  };

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
            <p className="text-xs text-gray-400 mt-1">{t.collectionsManagerDesc}</p>
          </div>
          <div className="flex items-center gap-3">
            <input type="text" value={search} onChange={(e) => { setSearch(e.target.value); }}
              placeholder={ct.search}
              className="px-3 py-2 bg-white border border-gray-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-[#1e3a8a] w-48" />
            <select value={categoryFilter} onChange={(e) => { setCategoryFilter(e.target.value); }}
              className="px-3 py-2 bg-white border border-gray-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-[#1e3a8a] w-36">
              <option value="">{t.filterCategory}</option>
              {categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
            <button id="create-collection-btn" onClick={() => setModalOpen(true)}
              className="px-5 py-2.5 bg-[#1e3a8a] text-white font-black text-xs rounded-xl hover:bg-blue-800 transition shadow-sm">
              + {t.createCollection}
            </button>
          </div>
        </div>

        {error && (
          <div className="p-4 mb-6 rounded-xl bg-rose-50 border border-rose-100 text-rose-700 text-xs font-bold">{error}</div>
        )}

        <div id="collection-grid">
          {loading ? (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {[1,2,3,4,5,6].map(i => <div key={i} className="h-28 bg-gray-100 rounded-xl animate-pulse" />)}
            </div>
          ) : collections.length === 0 ? (
            <EmptyState title={t.noCollections} description={t.createCollection}
              action={<button onClick={() => setModalOpen(true)}
                className="px-4 py-2 bg-[#1e3a8a] text-white font-bold text-xs rounded-xl hover:bg-blue-800 transition">{t.createCollection}</button>} />
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {collections.map(col => (
                <EntityCard key={col.id}
                  title={col.name}
                  subtitle={col.description || '\u2014'}
                  onClick={() => navigate(`/instructor/collections/${col.id}`)}
                  onEdit={() => handleEdit(col)}
                  onDelete={() => handleDelete(col.id)}>
                  <div className="flex items-center gap-3 text-[10px] text-gray-400 font-mono">
                    {col.categoryName && <span className="bg-indigo-50 text-indigo-600 px-1.5 py-0.5 rounded border border-indigo-200">{col.categoryName}</span>}
                    <span>Created: {new Date(col.createdAt).toLocaleDateString()}</span>
                  </div>
                </EntityCard>
              ))}
            </div>
          )}
        </div>
      </div>

      <Modal open={modalOpen} onClose={() => { setModalOpen(false); resetForm(); }} title={editing ? t.editCollection : t.createCollection}>
        <form onSubmit={handleSubmit} className="space-y-4 text-xs">
          <div className="space-y-1.5">
            <label className="text-gray-500 font-black uppercase tracking-wide text-[10px]">{ct.name} <span className="text-rose-500">*</span></label>
            <input type="text" required value={name} onChange={(e) => setName(e.target.value)}
              placeholder="e.g., Autumn 2026 Research Collection"
              className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl font-medium text-gray-800 focus:outline-none focus:ring-2 focus:ring-[#1e3a8a] focus:bg-white transition" />
          </div>
          <div className="space-y-1.5">
            <label className="text-gray-500 font-black uppercase tracking-wide text-[10px]">{ct.description}</label>
            <textarea rows="3" value={description} onChange={(e) => setDescription(e.target.value)}
              placeholder={t.collectionDescription}
              className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl font-medium text-gray-800 focus:outline-none focus:ring-2 focus:ring-[#1e3a8a] focus:bg-white transition" />
          </div>
          <div className="space-y-1.5">
            <label className="text-gray-500 font-black uppercase tracking-wide text-[10px]">{t.category}</label>
            <select value={categoryId} onChange={(e) => setCategoryId(e.target.value)}
              className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl font-medium text-gray-800 focus:outline-none focus:ring-2 focus:ring-[#1e3a8a] focus:bg-white transition">
              <option value="">{t.noCategory}</option>
              {categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
          </div>
          <div className="flex items-center gap-3 pt-4 border-t border-gray-100 font-bold">
            <button type="button" onClick={() => { setModalOpen(false); resetForm(); }}
              className="flex-1 py-3 bg-gray-50 hover:bg-gray-100 text-gray-600 rounded-xl transition text-center border border-gray-200/60">{ct.cancel}</button>
            <button type="submit" disabled={submitting}
              className="flex-1 py-3 bg-[#1e3a8a] text-white rounded-xl hover:bg-blue-800 transition shadow-md disabled:opacity-50 text-center">
              {submitting ? ct.saving : ct.create}
            </button>
          </div>
        </form>
      </Modal>

      <TourLauncher steps={TOUR_STEPS} tourKey="instructor-collections" />
    </div>
  );
}
