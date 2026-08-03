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
    <div className="min-h-screen bg-(--page-bg) text-(--text-primary) font-sans">
      <AppHeader />
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">

        <div className="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-4 mb-8 border-b border-(--border) pb-5">
          <div>
            <h1 className="text-3xl font-extrabold text-(--brand-foreground) tracking-tight">{t.instructorControlDashboard}</h1>
            <p className="text-(--text-secondary) text-sm mt-1">{t.instructorDashboardDesc}</p>
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
          }} className="px-4 py-2 bg-(--surface) border border-(--border) rounded-xl text-xs font-bold text-(--text-secondary) hover:bg-(--brand-soft) hover:text-(--brand-foreground) transition-colors shadow-sm flex items-center gap-1.5 self-start sm:self-auto">
            <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" /></svg>
            {t.userGuidance}
          </button>
        </div>

        <div id="nav-tiles" className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8 text-xs">
          <div className="bg-(--surface) border border-(--border) rounded-2xl p-5 shadow-sm hover:shadow-md transition-shadow flex flex-col justify-between min-h-[140px]">
            <div className="space-y-1.5">
              <h3 className="text-sm font-black text-(--brand-foreground) flex items-center gap-2"><svg className="w-4 h-4 text-(--brand)" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 19h16M6 16V8m6 8V5m6 11v-4" /></svg>{t.projectManager}</h3>
              <p className="text-(--text-secondary) font-medium leading-relaxed">{t.projectManagerDesc}</p>
            </div>
            <div className="pt-2">
              <Link to="/instructor/projects" className="text-(--brand) font-bold hover:underline">{t.manageProjects} &rarr;</Link>
            </div>
          </div>

          <div className="bg-(--surface) border border-(--border) rounded-2xl p-5 shadow-sm hover:shadow-md transition-shadow flex flex-col justify-between min-h-[140px]">
            <div className="space-y-1.5">
              <h3 className="text-sm font-black text-(--brand-foreground) flex items-center gap-2"><svg className="w-4 h-4 text-(--brand)" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12h6m-6 4h6M9 8h6M5 4h14a2 2 0 012 2v14H3V6a2 2 0 012-2z" /></svg>{t.reviewRequests}</h3>
              <p className="text-(--text-secondary) font-medium leading-relaxed">{t.reviewRequestsDesc}</p>
            </div>
            <div className="pt-2">
              <Link to="/instructor/requests" className="text-(--brand) font-bold hover:underline">{t.reviewSubmissions} &rarr;</Link>
            </div>
          </div>

          <div className="bg-(--surface) border border-(--border) rounded-2xl p-5 shadow-sm hover:shadow-md transition-shadow flex flex-col justify-between min-h-[140px]">
            <div className="space-y-1.5">
              <h3 className="text-sm font-black text-(--brand-foreground) flex items-center gap-2"><svg className="w-4 h-4 text-(--brand)" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 5a2 2 0 012-2h12a2 2 0 012 2v14a2 2 0 01-2 2H6a2 2 0 01-2-2V5zm4 2h8M8 11h8M8 15h5" /></svg>{t.collectionsManager}</h3>
              <p className="text-(--text-secondary) font-medium leading-relaxed">{t.collectionsManagerDesc}</p>
            </div>
            <div className="pt-2">
              <Link to="/instructor/collections" className="text-(--brand) font-bold hover:underline">{t.manageCollectionsLink} &rarr;</Link>
            </div>
          </div>
        </div>

        <div id="filter-bar" className="bg-(--surface) p-4 rounded-2xl border border-(--border) shadow-sm mb-6 flex flex-col md:flex-row gap-3 items-center">
          <div className="w-full md:flex-1">
            <input type="text" value={searchTerm} onChange={(e) => { setSearchTerm(e.target.value); setPage(0); }}
              placeholder={ct.search}
              className="w-full px-4 py-2 bg-(--surface-secondary) border border-(--border) rounded-xl text-xs text-(--text-primary) focus:outline-none focus:ring-2 focus:ring-(--focus)" />
          </div>
          <select value={categoryFilter} onChange={(e) => { setCategoryFilter(e.target.value); setPage(0); }}
            className="w-full md:w-48 px-4 py-2 bg-(--surface-secondary) border border-(--border) rounded-xl text-xs text-(--text-primary) focus:outline-none focus:ring-2 focus:ring-(--focus)">
            <option value="">{t.filterCategory}</option>
            {categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
        </div>

        <div id="left-panel" className="space-y-4">
          <div className="bg-(--surface) rounded-2xl border border-(--border) shadow-sm overflow-hidden">
            <div className="p-4 bg-(--surface-secondary) border-b border-(--border-light) flex justify-between items-center">
              <span className="text-xs font-black text-(--text-secondary) uppercase tracking-wider">{t.collections} ({totalElements})</span>
            </div>
            <div className="divide-y divide-(--border-light)">
              {colLoading ? (
                <div className="p-8 text-center text-(--text-tertiary) text-xs font-semibold">{ct.loading}</div>
              ) : collections.length === 0 ? (
                <div className="p-8 text-center text-(--text-tertiary) text-xs italic font-medium">{t.noCollections}</div>
              ) : (
                collections.map(col => (
                  <Link key={col.id} to={`/instructor/collections/${col.id}`}
                    className="p-4 transition-colors cursor-pointer flex justify-between items-center hover:bg-(--surface-secondary) block">
                    <div className="space-y-1 pr-4">
                      <h3 className="font-bold text-(--text-primary) text-sm tracking-tight">{col.name}</h3>
                      <p className="text-(--text-secondary) text-xs line-clamp-1">{col.description}</p>
                      <div className="flex items-center gap-4 text-[10px] font-mono text-(--text-tertiary) mt-1">
                        {col.categoryName && <span className="bg-indigo-50 text-indigo-600 px-1.5 py-0.5 rounded border border-indigo-200">{col.categoryName}</span>}
                        <span>{t.created}: {new Date(col.createdAt).toLocaleDateString(language === 'vi' ? 'vi-VN' : 'en-US')}</span>
                      </div>
                    </div>
                    <span className="text-xs text-(--text-tertiary)" aria-hidden="true">&rarr;</span>
                  </Link>
                ))
              )}
            </div>
          </div>

          {totalPages > 1 && (
            <div className="flex items-center justify-center gap-2 text-xs">
              <button disabled={page === 0} onClick={() => setPage(page - 1)}
                className="px-3 py-1.5 bg-(--surface) border border-(--border) rounded-lg font-bold text-(--text-secondary) hover:bg-(--surface-secondary) transition-colors disabled:opacity-30 disabled:cursor-not-allowed">{t.prev}</button>
              <span className="px-3 py-1.5 font-mono font-bold text-(--text-secondary)">{t.page} {page + 1} {t.of} {totalPages}</span>
              <button disabled={page >= totalPages - 1} onClick={() => setPage(page + 1)}
                className="px-3 py-1.5 bg-(--surface) border border-(--border) rounded-lg font-bold text-(--text-secondary) hover:bg-(--surface-secondary) transition-colors disabled:opacity-30 disabled:cursor-not-allowed">{t.next}</button>
            </div>
          )}
        </div>
      </main>
    </div>
  );
}
