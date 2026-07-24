import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { driver } from 'driver.js';
import 'driver.js/dist/driver.css';
import { initialMockData } from '../../mockData.js';
import AppHeader from '../../components/AppHeader.jsx';
import { instructorText, commonText } from '../../locales';
import { useLanguage } from '../../context/LanguageContext';

export default function InstructorDashboard() {
  const { language } = useLanguage();
  const t = instructorText[language];
  const ct = commonText[language];

  // --- 1. STATES MANAGEMENT ---
  const [categories, setCategories] = useState([]);
  const [selectedCategoryId, setSelectedCategoryId] = useState("");
  const [collections, setCollections] = useState([]);
  const [selectedCollection, setSelectedCollection] = useState(null);
  const [associatedDocs, setAssociatedDocs] = useState([]);

  const [loading, setLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");

  // --- 2. INITIAL DATA LOADING ---
  useEffect(() => {
    setLoading(true);

    const localProjects = localStorage.getItem('projects')
      ? JSON.parse(localStorage.getItem('projects'))
      : (initialMockData.projects || []);

    setCategories(localProjects);

    if (localProjects.length > 0) {
      setSelectedCategoryId(localProjects[0].id);
    }
    setLoading(false);
  }, []);

  // --- 3. FILTERING COLLECTIONS ---
  useEffect(() => {
    if (!selectedCategoryId) return;

    const currentCollections = localStorage.getItem('collections')
      ? JSON.parse(localStorage.getItem('collections'))
      : (initialMockData.collections || []);

    let filtered = currentCollections.filter(
      (col) => col.projectId === selectedCategoryId
    );

    if (searchTerm) {
      filtered = filtered.filter((col) =>
        col.title.toLowerCase().includes(searchTerm.toLowerCase())
      );
    }

    setCollections(filtered);

    if (selectedCollection && !filtered.some((c) => c.id === selectedCollection.id)) {
      setSelectedCollection(null);
      setAssociatedDocs([]);
    }
  }, [selectedCategoryId, searchTerm]);

  // --- 4. HANDLE SELECT COLLECTION ---
  const handleSelectCollection = (collection) => {
    setSelectedCollection(collection);

    const currentDocs = localStorage.getItem('referenceDocuments')
      ? JSON.parse(localStorage.getItem('referenceDocuments'))
      : (initialMockData.referenceDocuments || []);

    const docs = currentDocs.filter(
      (doc) => doc.collectionId === collection.id
    );
    setAssociatedDocs(docs);
  };

  // --- 5. DYNAMIC DOC COUNT ---
  const getActualDocCount = (collectionId) => {
    const currentDocs = localStorage.getItem('referenceDocuments')
      ? JSON.parse(localStorage.getItem('referenceDocuments'))
      : (initialMockData.referenceDocuments || []);

    return currentDocs.filter(
      (doc) => doc.collectionId === collectionId
    ).length;
  };

  return (
    <div className="min-h-screen bg-[#f8fafc] text-[#0f172a] font-sans">
      <AppHeader />
      <div className="max-w-7xl mx-auto p-8">

        {/* HEADER SECTION */}
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
                { element: '#right-panel', popover: { title: t.tourCollectionDetail, description: t.tourCollectionDetailDesc, side: 'left', align: 'center' } },
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

        {/* QUICK NAVIGATION TILES */}
        <div id="nav-tiles" className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8 text-xs">
          {/* Tile 1: Project Manager */}
          <div className="bg-white border border-gray-200 rounded-2xl p-5 shadow-sm hover:shadow-md transition flex flex-col justify-between min-h-[140px]">
            <div className="space-y-1.5">
              <h3 className="text-sm font-black text-[#1e3a8a] flex items-center gap-1.5">📊 {t.projectManager}</h3>
              <p className="text-gray-400 font-medium leading-relaxed">{t.projectManagerDesc}</p>
            </div>
            <div className="pt-2">
              <Link to="/instructor/projects" className="text-blue-600 font-bold hover:underline">{t.manageProjects} →</Link>
            </div>
          </div>

          {/* Tile 2: Review Requests */}
          <div className="bg-white border border-gray-200 rounded-2xl p-5 shadow-sm hover:shadow-md transition flex flex-col justify-between min-h-[140px]">
            <div className="space-y-1.5">
              <h3 className="text-sm font-black text-[#1e3a8a] flex items-center gap-1.5">📋 {t.reviewRequests}</h3>
              <p className="text-gray-400 font-medium leading-relaxed">{t.reviewRequestsDesc}</p>
            </div>
            <div className="pt-2">
              <Link to="/instructor/requests" className="text-blue-600 font-bold hover:underline">{t.reviewSubmissions} →</Link>
            </div>
          </div>

          {/* Tile 3: Collections Manager */}
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

        {/* COMBINED FILTER BAR: category dropdown + search */}
        <div id="filter-bar" className="bg-white p-4 rounded-2xl border border-gray-200 shadow-sm mb-6 flex flex-col md:flex-row gap-4 items-center">
          <div className="flex items-center gap-2 w-full md:w-auto">
            <span className="text-[10px] font-bold text-gray-400 uppercase tracking-wider">{ct.filter}:</span>
            <select
              value={selectedCategoryId}
              onChange={(e) => setSelectedCategoryId(e.target.value)}
              className="px-3 py-1.5 bg-gray-50 border border-gray-200 rounded-lg font-medium text-gray-700 text-xs focus:outline-none"
            >
              {categories.map((cat) => (
                <option key={cat.id} value={cat.id}>{cat.title}</option>
              ))}
            </select>
          </div>
          <div className="w-full md:flex-1">
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
               placeholder={ct.search}
              className="w-full px-4 py-2 bg-gray-50 border border-gray-200 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-[#1e3a8a]"
            />
          </div>
        </div>

        {/* Layout Grid: Left List / Right Inspection Panel */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 items-start">

          {/* LEFT PANEL */}
          <div id="left-panel" className="lg:col-span-2 space-y-4">
            <div className="bg-white rounded-2xl border border-gray-200 shadow-sm overflow-hidden">
              <div className="p-4 bg-gray-50 border-b border-gray-100 flex justify-between items-center">
                <span className="text-xs font-black text-gray-500 uppercase tracking-wider">{t.collections} ({collections.length})</span>
              </div>

              <div className="divide-y divide-gray-100">
                {loading ? (
                  <div className="p-8 text-center text-gray-400 text-xs font-semibold">{ct.loading}</div>
                ) : collections.length === 0 ? (
                  <div className="p-8 text-center text-gray-400 text-xs italic font-medium">{t.noCollections}</div>
                ) : (
                  collections.map((col) => {
                    const dynamicDocCount = getActualDocCount(col.id);

                    return (
                      <div
                        key={col.id}
                        onClick={() => handleSelectCollection(col)}
                        className={`p-4 transition cursor-pointer flex justify-between items-center ${
                          selectedCollection?.id === col.id ? 'bg-blue-50/50 border-l-4 border-[#1e3a8a]' : 'hover:bg-gray-50/40'
                        }`}
                      >
                        <div className="space-y-1 pr-4">
                          <h3 className="font-bold text-gray-900 text-sm tracking-tight">{col.title}</h3>
                          <p className="text-gray-500 text-xs line-clamp-1">{col.description}</p>
                          <div className="flex items-center gap-4 text-[10px] font-mono text-gray-400 mt-1">
                            <span>ID: {col.id}</span>
                            <span>Created: {col.createdAt}</span>
                          </div>
                        </div>
                        <div className="flex items-center gap-2 shrink-0">
                          <span className={`px-2 py-0.5 text-[9px] font-bold rounded border ${
                            dynamicDocCount > 0
                              ? 'bg-blue-50 text-blue-700 border-blue-200'
                              : 'bg-gray-100 text-gray-500 border-gray-200'
                          }`}>
                            {dynamicDocCount} {dynamicDocCount === 1 ? 'File' : 'Files'}
                          </span>
                          <span className="text-xs text-gray-400">➔</span>
                        </div>
                      </div>
                    );
                  })
                )}
              </div>
            </div>
          </div>

          {/* RIGHT PANEL */}
          <div id="right-panel" className="bg-white rounded-2xl border border-gray-200 shadow-sm p-6 space-y-6 min-h-[400px]">
            {!selectedCollection ? (
              <div className="h-full flex flex-col items-center justify-center text-center p-8 text-gray-400 my-auto">
                <span className="text-3xl block mb-2">📂</span>
                <p className="text-xs font-semibold">{t.noCollections}</p>
              </div>
            ) : (
              <div className="space-y-5 animate-fadeIn">
                <div className="border-b border-gray-100 pb-3">
                  <span className="text-[9px] font-black text-blue-700 bg-blue-50 border border-blue-100 px-2 py-0.5 rounded uppercase">Collection Metadata</span>
                  <h2 className="text-sm font-black text-gray-900 mt-2 tracking-tight">{selectedCollection.title}</h2>
                  <p className="text-xs text-gray-500 mt-2 leading-relaxed">{selectedCollection.description}</p>
                </div>

                <div className="space-y-2.5">
                  <h4 className="text-[10px] font-black text-gray-400 uppercase tracking-wider">{t.collections}</h4>
                  {associatedDocs.length === 0 ? (
                    <p className="text-xs text-gray-400 italic bg-gray-50 p-3 rounded-xl border border-gray-100">{ct.noData}</p>
                  ) : (
                    associatedDocs.map((doc) => (
                      <div
                        key={doc.id}
                        className="p-3 rounded-xl border border-gray-200 bg-gray-50/50 hover:bg-gray-50 transition text-xs flex flex-col space-y-1.5"
                      >
                        <div className="flex justify-between items-start">
                          <span className="font-bold text-gray-800 truncate pr-2">📄 {doc.name}</span>
                          <span className="text-[9px] font-mono text-gray-400 shrink-0">ID: {doc.id}</span>
                        </div>
                        <div className="flex justify-between items-center text-[10px] text-gray-400 font-mono pt-1">
                          <span>Uploaded: {doc.uploadedAt}</span>
                          <a
                            href={doc.fileUrl}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="text-blue-600 font-bold hover:underline"
                          >
                            Open ↗
                          </a>
                        </div>
                      </div>
                    ))
                  )}
                </div>
              </div>
            )}
          </div>

        </div>

      </div>
    </div>
  );
}
