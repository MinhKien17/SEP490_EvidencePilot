import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import AppHeader from '../../components/AppHeader.jsx';
import api from '../../api.js';
import { commonText, instructorText } from '../../locales';
import { useLanguage } from '../../context/LanguageContext';

export default function ProjectManagement() {
  const navigate = useNavigate();
  const { language } = useLanguage();
  const ct = commonText[language];
  const t = instructorText[language];

  const [projects, setProjects] = useState([]);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(0);
  const [total, setTotal] = useState(0);
  const [showCreate, setShowCreate] = useState(false);
  const [newTitle, setNewTitle] = useState('');
  const [editId, setEditId] = useState(null);
  const [editTitle, setEditTitle] = useState('');
  const [newDescription, setNewDescription] = useState('');
  const [creating, setCreating] = useState(false);

  const fetchProjects = async () => {
    setLoading(true);
    try { const r = await api.get(`/api/projects?page=${page}&size=10`); setProjects(r.data.content || []); setTotal(r.data.totalElements || 0); }
    catch { setProjects([]); }
    finally { setLoading(false); }
  };
  useEffect(() => { fetchProjects(); }, [page]);

  const handleCreate = async () => {
    if (!newTitle.trim()) return;
    setCreating(true);
    try { await api.post('/api/projects', { title: newTitle, description: newDescription }); setShowCreate(false); setNewTitle(''); setNewDescription(''); fetchProjects(); }
    catch { alert('Failed to create project.'); }
    finally { setCreating(false); }
  };

  const handleUpdate = async (id) => {
    if (!editTitle.trim()) return;
    try { await api.put(`/api/projects/${id}`, { title: editTitle }); setEditId(null); setEditTitle(''); fetchProjects(); }
    catch { alert('Failed to update project.'); }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this project permanently?')) return;
    try { await api.delete(`/api/projects/${id}`); fetchProjects(); }
    catch { alert('Failed to delete project.'); }
  };

  const handlePatch = async (id, action) => {
    try { await api.patch(`/api/projects/${id}/${action}`); fetchProjects(); }
    catch { alert(`Failed to ${action} project.`); }
  };

  return (
    <div className="min-h-screen bg-[#f8fafc] text-[#0f172a] font-sans">
      <AppHeader />
      <div className="max-w-5xl mx-auto p-8">
        <div className="flex justify-between items-center mb-6">
          <h1 className="text-2xl font-black text-[#1e3a8a]">{t.project || 'Projects'} ({total})</h1>
          <button onClick={() => setShowCreate(true)} className="px-4 py-2 bg-emerald-600 text-white font-bold text-xs rounded-xl hover:bg-emerald-700 transition flex items-center gap-1">
            <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4v16m8-8H4" /></svg>
            {ct.create}
          </button>
        </div>

        {loading ? (
          <div className="space-y-2">{Array.from({ length: 5 }).map((_, i) => <div key={i} className="h-14 bg-gray-200 rounded-xl animate-pulse" />)}</div>
        ) : projects.length === 0 ? (
          <div className="text-xs text-gray-400 italic bg-white rounded-2xl border border-gray-200 p-8 text-center">{ct.noData}</div>
        ) : (
          <div className="bg-white rounded-2xl border border-gray-200 shadow-sm overflow-hidden">
            <div className="divide-y divide-gray-100">
              {projects.map(p => (
                <div key={p.id} className="p-4 flex items-center justify-between gap-4 hover:bg-gray-50/40 transition">
                  <div className="flex-1 min-w-0 cursor-pointer" onClick={() => navigate(`/instructor/projects/${p.id}`)}>
                    {editId === p.id ? (
                      <div className="flex gap-2 items-center" onClick={e => e.stopPropagation()}>
                        <input value={editTitle} onChange={e => setEditTitle(e.target.value)} className="flex-1 border border-gray-200 rounded-lg px-2 py-1 text-sm outline-none" autoFocus />
                        <button onClick={() => handleUpdate(p.id)} className="text-xs font-bold text-emerald-600 hover:text-emerald-800">{ct.save}</button>
                        <button onClick={() => setEditId(null)} className="text-xs text-slate-400 hover:text-slate-600">{ct.cancel}</button>
                      </div>
                    ) : (
                      <div>
                        <h3 className="font-bold text-gray-900 text-sm hover:text-indigo-600 transition-colors">{p.title}</h3>
                        <p className="text-[10px] text-gray-400 font-mono mt-0.5">{p.id}</p>
                      </div>
                    )}
                  </div>
                  <div className="text-[10px] font-bold uppercase px-2 py-0.5 rounded border bg-blue-50 text-blue-700 border-blue-200 shrink-0">{p.status}</div>
                  <div className="flex gap-1 shrink-0" onClick={e => e.stopPropagation()}>
                    <button onClick={() => navigate(`/instructor/projects/${p.id}`)} className="text-[10px] text-sky-600 hover:text-sky-800 font-bold px-1.5 py-1">Detail</button>
                    <button onClick={() => { setEditId(p.id); setEditTitle(p.title); }} className="text-[10px] text-indigo-600 hover:text-indigo-800 font-bold px-1.5 py-1">{ct.edit}</button>
                    {p.status === 'ACTIVE' && <button onClick={() => handlePatch(p.id, 'archive')} className="text-[10px] text-amber-600 hover:text-amber-800 font-bold px-1.5 py-1">Archive</button>}
                    {p.status === 'ARCHIVED' && <button onClick={() => handlePatch(p.id, 'unarchive')} className="text-[10px] text-emerald-600 hover:text-emerald-800 font-bold px-1.5 py-1">Unarchive</button>}
                    {p.status === 'ACTIVE' && <button onClick={() => handlePatch(p.id, 'complete')} className="text-[10px] text-blue-600 hover:text-blue-800 font-bold px-1.5 py-1">Complete</button>}
                    <button onClick={() => handleDelete(p.id)} className="text-[10px] text-rose-600 hover:text-rose-800 font-bold px-1.5 py-1">{ct.delete}</button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        <div className="flex justify-between items-center mt-4 text-xs">
          <button disabled={page === 0} onClick={() => setPage(p => p - 1)} className="px-3 py-1 bg-white border border-gray-200 rounded-lg disabled:opacity-40 font-bold text-gray-600">{ct.back}</button>
          <span className="text-gray-400 font-mono">{ct.page || 'Page'} {page + 1}</span>
          <button disabled={(page + 1) * 10 >= total} onClick={() => setPage(p => p + 1)} className="px-3 py-1 bg-white border border-gray-200 rounded-lg disabled:opacity-40 font-bold text-gray-600">{ct.next}</button>
        </div>
      </div>

      {showCreate && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-sm">
          <div className="bg-white rounded-xl shadow-2xl w-full max-w-md p-6">
            <div className="flex justify-between items-center mb-4">
              <h2 className="text-lg font-bold text-slate-800">{ct.create}</h2>
              <button onClick={() => setShowCreate(false)} className="text-slate-400 hover:text-slate-600">
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" /></svg>
              </button>
            </div>
            <input value={newTitle} onChange={e => setNewTitle(e.target.value)} placeholder={t.project || 'Project title...'} autoFocus className="w-full border border-slate-200 rounded-lg p-3 text-sm outline-none focus:ring-1 focus:ring-indigo-500 mb-3" />
            <textarea value={newDescription} onChange={e => setNewDescription(e.target.value)} placeholder="Description (optional)" rows={3} className="w-full border border-slate-200 rounded-lg p-3 text-sm outline-none focus:ring-1 focus:ring-indigo-500 mb-4 resize-none" />
            <div className="flex justify-end gap-3">
              <button onClick={() => setShowCreate(false)} className="px-4 py-2 text-sm font-semibold text-slate-600 hover:bg-slate-100 rounded-lg transition-colors">{ct.cancel}</button>
              <button onClick={handleCreate} disabled={creating || !newTitle.trim()} className="px-4 py-2 text-sm font-bold text-white bg-emerald-600 hover:bg-emerald-700 disabled:bg-slate-300 rounded-lg shadow-sm transition-colors">{creating ? ct.saving : ct.create}</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
