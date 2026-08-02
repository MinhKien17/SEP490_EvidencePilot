import { useState, useEffect, useCallback } from 'react';
import { PageSkeleton } from './shared.jsx';
function QueueSection({ lang, api }) {
  const [queue, setQueue] = useState(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [activeTab, setActiveTab] = useState('Failed');
  const [searchQuery, setSearchQuery] = useState('');

  const fetch = useCallback(async (signal) => {
    try {
      const r = await api.get('/api/admin/documents/extraction-queue', { signal });
      setQueue(r.data);
    } catch (e) { /* silent */ }
  }, [api]);

  const [config, setConfig] = useState(null);

  useEffect(() => {
    const ac = new AbortController();
    api.get('/api/admin/config', { signal: ac.signal })
      .then(r => setConfig(r.data))
      .catch(() => { /* silent */ });
    return () => ac.abort();
  }, [api]);

  const handleRefresh = async () => {
    setRefreshing(true);
    await fetch();
    setRefreshing(false);
  };

  useEffect(() => {
    const ac = new AbortController();
    setLoading(true);
    fetch(ac.signal).finally(() => {
      if (!ac.signal.aborted) setLoading(false);
    });
    return () => ac.abort();
  }, [fetch]);

  const [retryingId, setRetryingId] = useState(null);
  const [toast, setToast] = useState(null);

  const showToast = (message, type = 'success') => {
    setToast({ message, type });
    setTimeout(() => setToast(null), 3000);
  };

  const doRetry = async (id) => {
    setRetryingId(id);
    try {
      await api.post(`/api/documents/${id}/re-extract`);
      showToast('Document extraction job re-queued successfully!', 'success');
      await fetch();
    } catch (e) {
      showToast(e.response?.data?.message || e.message || 'Failed to re-queue document.', 'error');
    } finally {
      setRetryingId(null);
    }
  };

  if (loading) return <PageSkeleton />;

  const counts = queue?.counts || {};
  const totalInQueue = ['QUEUED', 'PROCESSING', 'FAILED', 'READY'].reduce((a, k) => a + (counts[k] || 0), 0);
  const readyCount = counts.READY ?? 0;
  const processingCount = counts.PROCESSING ?? 0;
  const failedCount = counts.FAILED ?? 0;

  const toRow = (d, status) => ({
    id: d.id,
    originalFilename: d.originalFilename || '—',
    project: d.projectName || '—',
    errorType: d.processingError || '—',
    attempts: d.attempts ? `${d.attempts} / 3` : '—',
    timestamp: d.createdAt ? d.createdAt.replace('T', ' ').slice(0, 19) : '—',
    status
  });

  const failedList = (queue?.failed || []).map(d => toRow(d, 'Failed'));
  const queuedList = (queue?.queued || []).map(d => toRow(d, 'Queued'));
  const processingList = (queue?.processing || []).map(d => toRow(d, 'Processing'));
  const readyList = (queue?.ready || []).map(d => toRow(d, 'Ready'));

  let combinedList = [];
  if (activeTab === 'All') {
    combinedList = [...failedList, ...queuedList, ...processingList, ...readyList];
  } else if (activeTab === 'Failed') {
    combinedList = failedList;
  } else if (activeTab === 'Processing') {
    combinedList = processingList;
  } else if (activeTab === 'Ready') {
    combinedList = readyList;
  }

  const filteredDocs = combinedList.filter(d => 
    d.originalFilename.toLowerCase().includes(searchQuery.toLowerCase()) ||
    d.project.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div className="p-8 space-y-6 bg-[#f8fafc]">
      {/* Title Area */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-gray-200 pb-5">
        <div>
          <h1 className="text-3xl font-extrabold text-[#1e3a8a] tracking-tight">{lang.extractionQueue}</h1>
          <p className="text-gray-500 text-xs mt-1">{lang.queueSub}</p>
        </div>
        <div>
          {config?.rabbitMqManagementUrl && (
            <a href={config.rabbitMqManagementUrl} target="_blank" rel="noreferrer"
              className="inline-flex items-center gap-1.5 px-4 py-2 text-xs font-bold text-slate-700 bg-white border border-gray-200 rounded-xl hover:bg-gray-50 shadow-sm transition">
              <svg className="w-4 h-4 text-rose-500" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M13 10V3L4 14h7v7l9-11h-7z" />
              </svg>
              RabbitMQ Console
            </a>
          )}
          <button 
            onClick={handleRefresh} 
            disabled={refreshing}
            className="flex items-center gap-1.5 px-4 py-2 text-xs font-bold text-white bg-[#0c162e] hover:bg-[#152447] rounded-xl transition shadow-sm disabled:opacity-50"
          >
            <svg className={`w-4 h-4 ${refreshing ? 'animate-spin' : ''}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 1121.2 8H17" />
            </svg>
            <span>{refreshing ? lang.refreshing : lang.refreshQueue}</span>
          </button>
        </div>
      </div>

      {/* KPI Cards Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Card 1: Total in Queue */}
        <div className="bg-white rounded-xl border border-gray-200 p-5 shadow-sm flex flex-col justify-between h-36">
          <div className="flex justify-between items-start">
            <div className="w-10 h-10 rounded-xl bg-blue-50 border border-blue-100 flex items-center justify-center text-blue-600">
              <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
              </svg>
            </div>
          </div>
          <div className="mt-2">
            <span className="text-[10px] font-bold text-gray-400 block tracking-wider uppercase">Total in Queue</span>
            <span className="text-2xl font-extrabold text-slate-800">{totalInQueue}</span>
          </div>
        </div>

        {/* Card 2: Ready for Extraction */}
        <div className="bg-white rounded-xl border border-gray-200 p-5 shadow-sm flex flex-col justify-between h-36 border-l-4 border-l-emerald-500">
          <div className="flex justify-between items-start">
            <span className="text-[10px] font-bold text-emerald-600 bg-emerald-50 border border-emerald-100 px-2 py-0.5 rounded uppercase tracking-wider">Ready</span>
          </div>
          <div className="mt-2">
            <span className="text-[10px] font-bold text-gray-400 block tracking-wider uppercase">Ready for Extraction</span>
            <span className="text-2xl font-extrabold text-slate-800">{readyCount}</span>
          </div>
        </div>

        {/* Card 3: Currently Processing */}
        <div className="bg-white rounded-xl border border-gray-200 p-5 shadow-sm flex flex-col justify-between h-36">
          <div className="flex justify-between items-start">
            <span className="text-[10px] font-bold text-amber-600 bg-amber-50 border border-amber-100 px-2 py-0.5 rounded uppercase tracking-wider">Processing</span>
          </div>
          <div className="mt-2">
            <span className="text-[10px] font-bold text-gray-400 block tracking-wider uppercase">Currently Processing</span>
            <span className="text-2xl font-extrabold text-slate-800">{processingCount}</span>
          </div>
        </div>

        {/* Card 4: Total Failed */}
        <div className="bg-white rounded-xl border border-gray-200 p-5 shadow-sm flex flex-col justify-between h-36 border-l-4 border-l-rose-500">
          <div className="flex justify-between items-start">
            <span className="px-2 py-0.5 rounded text-[9px] font-bold bg-rose-50 text-rose-700 border border-rose-100 uppercase tracking-wider">Failed</span>
          </div>
          <div className="mt-2">
            <span className="text-[10px] font-bold text-gray-400 block tracking-wider uppercase">Total Failed</span>
            <span className="text-2xl font-extrabold text-slate-800">{failedCount}</span>
          </div>
        </div>
      </div>

      {/* Main Table Card */}
      <div className="bg-white rounded-2xl border border-gray-200 shadow-sm overflow-hidden">
        {/* Table Header and Filters */}
        <div className="px-6 py-4.5 border-b border-gray-100 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div className="flex items-center gap-4">
            <h3 className="text-lg font-bold text-slate-800">{lang.failedDocuments}</h3>
            {/* Status Tabs */}
            <div className="flex bg-slate-100 p-0.5 rounded-xl text-xs font-bold text-slate-600">
              {['All', 'Failed', 'Processing', 'Ready'].map(tab => (
                <button
                  key={tab}
                  onClick={() => setActiveTab(tab)}
                  className={`px-3 py-1.5 rounded-lg transition-all ${
                    activeTab === tab 
                      ? 'bg-white text-slate-800 shadow-sm' 
                      : 'hover:text-slate-800'
                  }`}
                >
                  {tab}
                </button>
              ))}
            </div>
          </div>
          {/* Search Box */}
          <div className="relative w-full sm:w-64">
            <svg className="w-4 h-4 text-gray-400 absolute left-3 top-2.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
            <input 
              type="text" 
              placeholder={lang.searchDocuments} 
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-9 pr-4 py-2 bg-slate-50 border border-gray-200 rounded-xl text-xs font-semibold text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-500" 
            />
          </div>
        </div>

        {/* Table Grid */}
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-xs">
            <thead>
              <tr className="bg-slate-50 text-slate-400 font-bold uppercase border-b border-gray-100">
                <th className="px-6 py-3.5">Document Name</th>
                <th className="px-6 py-3.5">Project</th>
                <th className="px-6 py-3.5">Error Type</th>
                <th className="px-6 py-3.5">Attempts</th>
                <th className="px-6 py-3.5">Timestamp</th>
                <th className="px-6 py-3.5 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100 text-slate-700 font-semibold">
              {filteredDocs.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-6 py-12 text-center text-gray-400 font-medium">
                    {lang.noDocuments}
                  </td>
                </tr>
              ) : filteredDocs.map((d, index) => (
                <tr key={d.id} className="hover:bg-slate-50/50 transition">
                  {/* Document Name - NO ICON! */}
                  <td className="px-6 py-4">
                    <span className="font-bold text-slate-800 block truncate max-w-xs sm:max-w-sm">{d.originalFilename}</span>
                  </td>

                  {/* Project */}
                  <td className="px-6 py-4 text-slate-600 font-bold">
                    {d.project}
                  </td>

                  {/* Error Type */}
                  <td className="px-6 py-4">
                    {d.status === 'Failed' ? (
                      <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                        d.errorType === 'Timeout' 
                          ? 'bg-orange-50 text-orange-700 border border-orange-100' 
                          : 'bg-rose-50 text-rose-700 border border-rose-100'
                      }`}>
                        {d.errorType}
                      </span>
                    ) : (
                      <span className="text-gray-400 font-normal">—</span>
                    )}
                  </td>

                  {/* Attempts */}
                  <td className="px-6 py-4 text-slate-500 font-medium">
                    {d.attempts}
                  </td>

                  {/* Timestamp */}
                  <td className="px-6 py-4 text-slate-500 font-mono font-medium">
                    {d.timestamp}
                  </td>

                  {/* Actions */}
                  <td className="px-6 py-4 text-right">
                    <div className="flex justify-end gap-2.5">
                      <button 
                        onClick={() => doRetry(d.id)} 
                        disabled={retryingId === d.id}
                        title="Retry Extraction"
                        className="w-8 h-8 rounded-xl bg-slate-50 border border-slate-200 text-slate-500 hover:bg-blue-50 hover:text-blue-600 hover:border-blue-200 flex items-center justify-center transition shadow-sm cursor-pointer disabled:opacity-50"
                      >
                        <svg className="w-4.5 h-4.5" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" d="M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0l3.181 3.183a8.25 8.25 0 0013.803-3.7M4.031 9.865a8.25 8.25 0 0113.803-3.7l3.181 3.182m0-4.991v4.99" />
                        </svg>
                      </button>
                      <button 
                        onClick={() => alert(`Error details: ${d.errorType}`)} 
                        title="View Error Details"
                        className="w-8 h-8 rounded-xl bg-slate-50 border border-slate-200 text-slate-500 hover:bg-slate-100 hover:text-slate-800 hover:border-slate-350 flex items-center justify-center transition shadow-sm cursor-pointer"
                      >
                        <svg className="w-4 h-4" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" d="M11.25 11.25l.041-.02a.75.75 0 111.063.852l-.708 2.836a.75.75 0 001.063.852l.041-.021M21 12a9 9 0 11-18 0 9 9 0 0118 0zm-9-3.75h.008v.008H12V8.25z" />
                        </svg>
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Footer / Pagination */}
        <div className="flex items-center justify-between px-6 py-3.5 border-t border-gray-100 bg-gray-50/50 text-xs font-semibold text-gray-500">
          <span>Showing {filteredDocs.length} of {combinedList.length} documents</span>
        </div>
      </div>
    </div>
  );
}


export { QueueSection };
