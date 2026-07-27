import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { driver } from 'driver.js';
import 'driver.js/dist/driver.css';
import AppHeader from '../../components/AppHeader.jsx';
import { instructorText, commonText } from '../../locales';
import { useLanguage } from '../../context/LanguageContext';
import { useCollections } from '../../hooks/useCollections';
import api from '../../api';

export default function InstructorDashboard() {
  const { language } = useLanguage();
  const t = instructorText[language];
  const ct = commonText[language];
  const [categories, setCategories] = useState([]);

  useEffect(() => {
    api.get('/api/collection-categories').then(r => setCategories(r.data)).catch(() => {});
  }, []);

  const [page, setPage] = useState(0);
  const size = 10;
  const [searchTerm, setSearchTerm] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('');
  const { content: collections, totalPages, totalElements, loading: colLoading } = useCollections(page, size, 'createdAt,desc', searchTerm || undefined, categoryFilter || undefined);

  return (
    <div className="min-h-screen bg-[#f8fafc] text-[#0f172a] font-sans">
      <AppHeader />
      <div className="max-w-7xl mx-auto p-8">

        <div className="flex justify-between items-center mb-8 border-b border-gray-200 pb-5">
          <div>
            <h1 className="text-3xl font-extrabold text-[#1e3a8a] tracking-tight">{t.instructorControlDashboard}</h1>
            <p className="text-gray-500 text-sm mt-1">{t.instructorDashboardDesc}</p>
          </div>

          <button onClick={() => {
            const d = driver({
              steps: [
                { element: '#nav-tiles', popover: { title: t.tourQuickNav, description: t.tourQuickNavDesc, side: 'bottom', align: 'start' } },
                { element: '#filter-bar', popover: { title: t.tourFilter, description: t.tourFilterDesc, side: 'bottom', align: 'start' } },
                { element: '#left-panel', popover: { title: t.tourCollections, description: t.tourCollectionsDesc, side: 'right', align: 'center' } },
              ],
              showProgress: true,
              showButtons: ['next', 'previous', 'close'],
            });
            d.drive();
          }} className="px-4 py-2 bg-white border border-gray-200 rounded-xl text-xs font-bold text-gray-600 hover:bg-indigo-50 hover:text-indigo-700 hover:border-indigo-200 transition shadow-sm flex items-center gap-1.5">
            <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" /></svg>
            {t.userGuidance}
          </button>
        </div>

        <div id="nav-tiles" className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8 text-xs">
          <div className="bg-white border border-gray-200 rounded-2xl p-5 shadow-sm hover:shadow-md transition flex flex-col justify-between min-h-[140px]">
            <div className="space-y-1.5">
              <h3 className="text-sm font-black text-[#1e3a8a] flex items-center gap-1.5">📊 {t.projectManager}</h3>
              <p className="text-gray-400 font-medium leading-relaxed">{t.projectManagerDesc}</p>
            </div>
            <div className="pt-2">
              <Link to="/instructor/projects" className="text-blue-600 font-bold hover:underline">{t.manageProjects} →</Link>
            </div>
          </div>

          <div className="bg-white border border-gray-200 rounded-2xl p-5 shadow-sm hover:shadow-md transition flex flex-col justify-between min-h-[140px]">
            <div className="space-y-1.5">
              <h3 className="text-sm font-black text-[#1e3a8a] flex items-center gap-1.5">📋 {t.reviewRequests}</h3>
              <p className="text-gray-400 font-medium leading-relaxed">{t.reviewRequestsDesc}</p>
            </div>
            <div className="pt-2">
              <Link to="/instructor/requests" className="text-blue-600 font-bold hover:underline">{t.reviewSubmissions} →</Link>
            </div>
          </div>

          <div className="bg-white border border-gray-200 rounded-2xl p-5 shadow-sm hover:shadow-md transition flex flex-col justify-between min-h-[140px]">
            <div className="space-y-1.5">
              <h3 className="text-sm font-black text-[#1e3a8a] flex items-center gap-1.5">📚 {t.collectionsManager}</h3>
              <p className="text-gray-400 font-medium leading-relaxed">{t.collectionsManagerDesc}</p>
            </div>
            <div className="pt-2">
              <Link to="/instructor/collections" className="text-blue-600 font-bold hover:underline">{t.manageCollectionsLink} →</Link>
            </div>
          </div>
        </div>

        <div id="filter-bar" className="bg-white p-4 rounded-2xl border border-gray-200 shadow-sm mb-6 flex flex-col md:flex-row gap-3 items-center">
          <div className="w-full md:flex-1">
            <input type="text" value={searchTerm} onChange={(e) => { setSearchTerm(e.target.value); setPage(0); }}
              placeholder={ct.search}
              className="w-full px-4 py-2 bg-gray-50 border border-gray-200 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-[#1e3a8a]" />
          </div>
          <select value={categoryFilter} onChange={(e) => { setCategoryFilter(e.target.value); setPage(0); }}
            className="w-full md:w-48 px-4 py-2 bg-gray-50 border border-gray-200 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-[#1e3a8a]">
            <option value="">{t.filterCategory}</option>
            {categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
        </div>

        <div id="left-panel" className="space-y-4">
          <div className="bg-white rounded-2xl border border-gray-200 shadow-sm overflow-hidden">
            <div className="p-4 bg-gray-50 border-b border-gray-100 flex justify-between items-center">
              <span className="text-xs font-black text-gray-500 uppercase tracking-wider">{t.collections} ({totalElements})</span>
            </div>
            <div className="divide-y divide-gray-100">
              {colLoading ? (
                <div className="p-8 text-center text-gray-400 text-xs font-semibold">{ct.loading}</div>
              ) : collections.length === 0 ? (
                <div className="p-8 text-center text-gray-400 text-xs italic font-medium">{t.noCollections}</div>
              ) : (
                collections.map(col => (
                  <Link key={col.id} to={`/instructor/collections/${col.id}`}
                    className="p-4 transition cursor-pointer flex justify-between items-center hover:bg-gray-50/40 block">
                    <div className="space-y-1 pr-4">
                      <h3 className="font-bold text-gray-900 text-sm tracking-tight">{col.name}</h3>
                      <p className="text-gray-500 text-xs line-clamp-1">{col.description}</p>
                      <div className="flex items-center gap-4 text-[10px] font-mono text-gray-400 mt-1">
                        {col.categoryName && <span className="bg-indigo-50 text-indigo-600 px-1.5 py-0.5 rounded border border-indigo-200">{col.categoryName}</span>}
                        <span>Created: {new Date(col.createdAt).toLocaleDateString()}</span>
                      </div>
                    </div>
                    <span className="text-xs text-gray-400">→</span>
                  </Link>
                ))
              )}
            </div>
          </div>

          {totalPages > 1 && (
            <div className="flex items-center justify-center gap-2 text-xs">
              <button disabled={page === 0} onClick={() => setPage(page - 1)}
                className="px-3 py-1.5 bg-white border border-gray-200 rounded-lg font-bold text-gray-600 hover:bg-gray-50 transition disabled:opacity-30 disabled:cursor-not-allowed">{t.prev}</button>
              <span className="px-3 py-1.5 font-mono font-bold text-gray-700">{t.page} {page + 1} {t.of} {totalPages}</span>
              <button disabled={page >= totalPages - 1} onClick={() => setPage(page + 1)}
                className="px-3 py-1.5 bg-white border border-gray-200 rounded-lg font-bold text-gray-600 hover:bg-gray-50 transition disabled:opacity-30 disabled:cursor-not-allowed">{t.next}</button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
