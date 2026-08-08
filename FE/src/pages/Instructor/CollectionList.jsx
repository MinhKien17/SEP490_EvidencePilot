import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { EntityCard, Modal, EmptyState, TourLauncher, AppHeader } from '../../components';
import { instructorText, commonText } from '../../locales';
import { useLanguage } from '../../context/LanguageContext';
import { useCollections } from '../../hooks/useCollections';
import api from '../../api';

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
  const [deletingId, setDeletingId] = useState(null);
  const [deleting, setDeleting] = useState(false);
  const tourSteps = [
    { element: '#collection-grid', popover: { title: t.browseCollections, description: t.browseCollectionsDesc, side: 'top', align: 'start' } },
    { element: '#create-collection-btn', popover: { title: t.createCollection, description: t.createCollectionDesc, side: 'left', align: 'center' } },
  ];

  const handleEdit = (col) => {
    setEditing(col.id);
    setName(col.name);
    setDescription(col.description || '');
    setCategoryId(col.categoryId || '');
    setModalOpen(true);
  };

  const handleDelete = async () => {
    if (!deletingId || deleting) return;
    setDeleting(true);
    try {
      await api.delete(`/api/collections/${deletingId}`);
      await refetch();
      setDeletingId(null);
    } catch { alert(t.deleteCollectionFailed); }
    finally { setDeleting(false); }
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
      await refetch();
      resetForm(); setModalOpen(false);
    } catch { alert(t.saveCollectionFailed); }
    finally { setSubmitting(false); }
  };

  return (
    <div className="min-h-screen bg-(--page-bg) text-(--text-primary)">
      <AppHeader />
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 mb-8 border-b border-(--border) pb-6">
          <div>
            <div className="mb-2">
              <Link to="/instructor/dashboard" className="text-xs font-bold text-(--text-tertiary) hover:text-(--brand-foreground) transition-colors">&larr; {ct.back}</Link>
            </div>
            <h1 className="text-3xl font-black text-(--brand-foreground) tracking-tight">{t.collections}</h1>
            <p className="text-xs text-(--text-tertiary) mt-1">{t.collectionsManagerDesc}</p>
          </div>
          <div className="flex flex-col sm:flex-row w-full md:w-auto items-stretch sm:items-center gap-3">
            <input type="text" value={search} onChange={(e) => { setSearch(e.target.value); }}
              placeholder={ct.search}
              className="px-3 py-2 bg-(--surface) border border-(--border) rounded-xl text-xs text-(--text-primary) font-medium focus:outline-none focus:ring-2 focus:ring-(--focus) w-full sm:w-48" />
            <select value={categoryFilter} onChange={(e) => { setCategoryFilter(e.target.value); }}
              className="px-3 py-2 bg-(--surface) border border-(--border) rounded-xl text-xs text-(--text-primary) font-medium focus:outline-none focus:ring-2 focus:ring-(--focus) w-full sm:w-40">
              <option value="">{t.filterCategory}</option>
              {categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
            <button id="create-collection-btn" onClick={() => setModalOpen(true)}
              className="px-5 py-2.5 bg-(--brand) text-(--on-brand) font-black text-xs rounded-xl hover:bg-(--brand-hover) transition-colors shadow-sm whitespace-nowrap">
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
              {[1,2,3,4,5,6].map(i => <div key={i} className="h-28 bg-(--surface-tertiary) rounded-xl animate-pulse" />)}
            </div>
          ) : collections.length === 0 ? (
            <EmptyState title={t.noCollections} description={t.createCollection}
              action={<button onClick={() => setModalOpen(true)}
                className="px-4 py-2 bg-(--brand) text-(--on-brand) font-bold text-xs rounded-xl hover:bg-(--brand-hover) transition-colors">{t.createCollection}</button>} />
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {collections.map(col => (
                <EntityCard key={col.id}
                  title={col.name}
                  subtitle={col.description || '\u2014'}
                  onClick={() => navigate(`/instructor/collections/${col.id}`)}
                  onEdit={() => handleEdit(col)}
                  onDelete={() => setDeletingId(col.id)}
                  editLabel={ct.edit}
                  deleteLabel={ct.delete}>
                  <div className="flex items-center gap-3 text-[10px] text-(--text-tertiary) font-mono">
                    {col.categoryName && <span className="bg-indigo-50 text-indigo-600 px-1.5 py-0.5 rounded border border-indigo-200">{col.categoryName}</span>}
                    <span>{t.created}: {new Date(col.createdAt).toLocaleDateString(language === 'vi' ? 'vi-VN' : 'en-US')}</span>
                  </div>
                </EntityCard>
              ))}
            </div>
          )}
        </div>
      </main>

      <Modal open={modalOpen} onClose={() => { setModalOpen(false); resetForm(); }} title={editing ? t.editCollection : t.createCollection} closeLabel={ct.close}>
        <form onSubmit={handleSubmit} className="space-y-4 text-xs">
          <div className="space-y-1.5">
            <label className="text-(--text-secondary) font-black uppercase tracking-wide text-[10px]">{ct.name} <span className="text-rose-500">*</span></label>
            <input type="text" required value={name} onChange={(e) => setName(e.target.value)}
              placeholder={t.collectionNameExample}
              className="w-full px-4 py-3 bg-(--surface-secondary) border border-(--border) rounded-xl font-medium text-(--text-primary) focus:outline-none focus:ring-2 focus:ring-(--focus) transition-colors" />
          </div>
          <div className="space-y-1.5">
            <label className="text-(--text-secondary) font-black uppercase tracking-wide text-[10px]">{ct.description}</label>
            <textarea rows="3" value={description} onChange={(e) => setDescription(e.target.value)}
              placeholder={t.collectionDescription}
              className="w-full px-4 py-3 bg-(--surface-secondary) border border-(--border) rounded-xl font-medium text-(--text-primary) focus:outline-none focus:ring-2 focus:ring-(--focus) transition-colors" />
          </div>
          <div className="space-y-1.5">
            <label className="text-(--text-secondary) font-black uppercase tracking-wide text-[10px]">{t.category}</label>
            <select value={categoryId} onChange={(e) => setCategoryId(e.target.value)}
              className="w-full px-4 py-3 bg-(--surface-secondary) border border-(--border) rounded-xl font-medium text-(--text-primary) focus:outline-none focus:ring-2 focus:ring-(--focus) transition-colors">
              <option value="">{t.noCategory}</option>
              {categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
          </div>
          <div className="flex items-center gap-3 pt-4 border-t border-(--border-light) font-bold">
            <button type="button" onClick={() => { setModalOpen(false); resetForm(); }}
              className="flex-1 py-3 bg-(--surface-secondary) hover:bg-(--surface-tertiary) text-(--text-secondary) rounded-xl transition-colors text-center border border-(--border)">{ct.cancel}</button>
            <button type="submit" disabled={submitting}
              className="flex-1 py-3 bg-(--brand) text-(--on-brand) rounded-xl hover:bg-(--brand-hover) transition-colors shadow-md disabled:opacity-50 text-center">
              {submitting ? ct.saving : ct.create}
            </button>
          </div>
        </form>
      </Modal>

      <Modal open={!!deletingId} onClose={() => { if (!deleting) setDeletingId(null); }} title={ct.delete} closeLabel={ct.close}>
        <div className="space-y-4 text-xs">
          <p className="text-(--text-secondary)">{t.deleteConfirm}</p>
          <div className="flex gap-3 pt-2">
            <button type="button" onClick={() => setDeletingId(null)} disabled={deleting}
              className="flex-1 py-3 bg-(--surface-secondary) hover:bg-(--surface-tertiary) text-(--text-secondary) rounded-xl transition-colors border border-(--border) disabled:opacity-50">{ct.cancel}</button>
            <button type="button" onClick={handleDelete} disabled={deleting}
              className="flex-1 py-3 bg-rose-600 text-white rounded-xl hover:bg-rose-700 transition-colors disabled:opacity-50">{deleting ? ct.saving : ct.confirm}</button>
          </div>
        </div>
      </Modal>

      <TourLauncher steps={tourSteps} tourKey="instructor-collections" />
    </div>
  );
}
