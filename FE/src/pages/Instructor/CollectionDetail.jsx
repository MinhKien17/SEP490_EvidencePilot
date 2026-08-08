import { useState, useEffect, useMemo, useCallback, useRef } from 'react';
import { useParams, Link } from 'react-router-dom';
import { AppHeader, LoadingSkeleton, EmptyState, Modal, UploadZone } from '../../components';
import TourLauncher from '../../components/TourLauncher';
import { instructorText, commonText } from '../../locales';
import { useLanguage } from '../../context/LanguageContext';
import { useCollectionSources } from '../../hooks/useCollections';
import api from '../../api';
import { Network } from 'vis-network';
import { DataSet } from 'vis-data';

const TABS = ['documents', 'connectedMap', 'visualizeMap', 'analyzeCollection'];
const TAB_IDS = ['documents-tab', 'connected-map-tab', 'visualize-map-tab', 'analyze-tab'];

function statusColor(s) {
  if (s === 'READY' || s === 'COMPLETED') return 'bg-emerald-100 text-emerald-700 border-emerald-200';
  if (s === 'PROCESSING' || s === 'UPLOADED' || s === 'QUEUED') return 'bg-amber-100 text-amber-700 border-amber-200';
  if (s === 'FAILED') return 'bg-rose-100 text-rose-700 border-rose-200';
  return 'bg-gray-100 text-gray-500 border-gray-200';
}

function FileIcon({ name, className = 'w-5 h-5' }) {
  const ext = name?.split('.').pop()?.toLowerCase();
  const color = ext === 'pdf' ? 'text-rose-500' : ['doc', 'docx'].includes(ext) ? 'text-blue-500' : ext === 'tex' ? 'text-amber-500' : 'text-(--brand)';
  return <svg className={`${className} ${color}`} fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M7 3h7l5 5v13H7a2 2 0 01-2-2V5a2 2 0 012-2zm7 0v6h6M9 13h6m-6 4h6" /></svg>;
}

export default function CollectionDetail() {
  const { id } = useParams();
  const { language } = useLanguage();
  const t = instructorText[language];
  const ct = commonText[language];

  const TOUR_STEPS = useMemo(() => [
    { element: '#documents-tab', popover: { title: t.documents, description: t.tourDocumentsDesc, side: 'bottom', align: 'start' } },
    { element: '#add-doc-btn', popover: { title: t.addDocument, description: t.tourAddDocumentDesc, side: 'left', align: 'center' } },
    { element: '#documents-tab', popover: { title: t.tourDocumentList, description: t.tourDocumentListDesc, side: 'right', align: 'start' } },
    { element: '#share-to-project', popover: { title: t.shareToProject, description: t.tourShareDocumentDesc, side: 'left', align: 'center' } },
    { element: '#connected-map-tab', popover: { title: t.connectedMap, description: t.tourConnectedDesc, side: 'bottom', align: 'start' } },
    { element: '#visualize-map-tab', popover: { title: t.visualizeMap, description: t.tourVisualizeDesc, side: 'bottom', align: 'start' } },
    { element: '#analyze-tab', popover: { title: t.analyzeCollection, description: t.tourAnalyzeDesc, side: 'bottom', align: 'start' } },
  ], [t]);

  const [activeTab, setActiveTab] = useState(0);
  const { content: sources, loading: srcLoading, error: srcError, refetch: refetchSources } = useCollectionSources(id);
  const [selectedSource, setSelectedSource] = useState(null);
  const [shareModal, setShareModal] = useState({ open: false, sourceId: null });
  const [projects, setProjects] = useState([]);
  const [projectSearch, setProjectSearch] = useState('');

  const [collection, setCollection] = useState(null);
  const [collectionLoading, setCollectionLoading] = useState(true);

  const [addDocModal, setAddDocModal] = useState(false);
  const [addDocOption, setAddDocOption] = useState(null);

  const [categories, setCategories] = useState([]);
  const [editModal, setEditModal] = useState({ open: false, name: '', description: '', categoryId: '', submitting: false });
  const [graphData, setGraphData] = useState(null);
  const [graphLoading, setGraphLoading] = useState(false);
  const [selectedGraphNode, setSelectedGraphNode] = useState(null);
  const graphRef = useRef(null);
  const networkRef = useRef(null);

  useEffect(() => {
    api.get('/api/collection-categories').then(r => setCategories(r.data)).catch(() => { });
  }, []);

  useEffect(() => {
    api.get(`/api/collections/${id}`).then(r => setCollection(r.data)).catch(() => { }).finally(() => setCollectionLoading(false));
  }, [id]);

  useEffect(() => {
    api.get('/api/projects?size=100').then(r => setProjects(r.data?.content || [])).catch(() => setProjects([]));
  }, []);

  const openShare = async (sourceId) => {
    setShareModal({ open: true, sourceId });
    try {
      const res = await api.get('/api/projects?size=100');
      setProjects(res.data?.content || []);
    } catch { setProjects([]); }
  };

  const doShare = async (projectId) => {
    try {
      await api.post(`/api/collections/${id}/sources/${shareModal.sourceId}/share-to-project/${projectId}`);
      refetchSources();
      setShareModal({ open: false, sourceId: null });
    } catch (err) {
      alert(t.shareFailed);
    }
  };

  const handleUpload = async (file) => {
    const fd = new FormData();
    fd.append('file', file);
    fd.append('collectionId', id);
    try {
      await api.post('/api/sources', fd);
      refetchSources();
    } catch (err) {
      alert(t.uploadFailed);
    }
  };

  const handleDeleteSource = async (sourceId) => {
    if (!window.confirm(t.deleteSourceConfirm)) return;
    try { await api.delete(`/api/documents/${sourceId}`); refetchSources(); if (selectedSource?.id === sourceId) setSelectedSource(null); }
    catch { alert(t.deleteFailed); }
  };

  const handleDownloadSource = async (source) => {
    try {
      const response = await api.get(`/api/documents/${source.id}/download`, { responseType: 'blob' });
      const url = URL.createObjectURL(response.data);
      const link = document.createElement('a');
      link.href = url;
      link.download = source.originalFilename || 'document';
      link.click();
      URL.revokeObjectURL(url);
    } catch {
      alert(t.downloadFailed);
    }
  };

  const handleDeleteCollection = () => {
    const shared = sources.filter(s => (s.projectIds || []).length > 0);
    if (shared.length > 0 && !window.confirm(t.sharedDocsWarning)) return;
    if (!window.confirm(t.deleteConfirm)) return;
    api.delete(`/api/collections/${id}`).then(() => { window.location.href = '/instructor/collections'; }).catch(() => alert(t.deleteFailed));
  };

  const handleEditOpen = () => {
    setEditModal({ open: true, name: collection?.name || '', description: collection?.description || '', categoryId: collection?.categoryId || '', submitting: false });
  };

  const handleEditSubmit = async (e) => {
    e.preventDefault();
    if (!editModal.name.trim()) return;
    setEditModal(p => ({ ...p, submitting: true }));
    try {
      const res = await api.put(`/api/collections/${id}`, {
        name: editModal.name.trim(),
        description: editModal.description.trim() || null,
        categoryId: editModal.categoryId || null,
      });
      setCollection(res.data);
      setEditModal({ open: false, name: '', description: '', categoryId: '', submitting: false });
    } catch (err) {
      alert(err.response?.data?.message || t.uploadFailed);
      setEditModal(p => ({ ...p, submitting: false }));
    }
  };

  const filteredProjects = projects.filter(p =>
    !projectSearch || p.title?.toLowerCase().includes(projectSearch.toLowerCase())
  );

  const AddDocForm = () => {
    const [doi, setDoi] = useState('');
    const [file, setFile] = useState(null);
    const [submitting, setSubmitting] = useState(false);
    const [doiError, setDoiError] = useState('');

    const handleDoiSubmit = async () => {
      setDoiError('');
      try {
        await api.post('/api/documents/ingest/doi', { doi, collectionId: id });
        refetchSources();
        return true;
      } catch (err) {
        setDoiError(err.response?.data?.message || t.uploadFailed);
        return false;
      }
    };

    if (!addDocOption) return null;

    if (addDocOption === 'doi') {
      return (
        <form onSubmit={async (e) => {
          e.preventDefault();
          setSubmitting(true);
          try {
            if (await handleDoiSubmit()) {
              setAddDocOption(null);
              setAddDocModal(false);
            }
          } finally {
            setSubmitting(false);
          }
        }} className="space-y-4">
          <p className="text-xs text-(--text-secondary)">{t.inputDoiDescription}</p>
          <input type="text" value={doi} onChange={e => setDoi(e.target.value)} placeholder={t.doiPlaceholder} required
            className="w-full px-4 py-3 bg-(--surface-secondary) border border-(--border) rounded-xl text-(--text-primary) font-medium text-sm focus:outline-none focus:ring-2 focus:ring-(--focus) transition-colors" />
          {doiError && <p className="text-xs font-semibold text-rose-600">{doiError}</p>}
          <button type="submit" disabled={submitting || !doi.trim()}
            className="w-full py-3 bg-(--brand) text-(--on-brand) font-bold text-xs rounded-xl hover:bg-(--brand-hover) transition-colors shadow-md disabled:opacity-50">{submitting ? ct.saving : t.submitDoi}</button>
        </form>
      );
    }

    if (addDocOption === 'upload') {
      return (
        <div className="space-y-4">
          <p className="text-xs text-(--text-secondary)">{t.uploadDocumentDescription}</p>
          <UploadZone onUpload={(f) => { setFile(f); handleUpload(f); }} accept=".pdf,.docx,.md,.tex" label={t.dropFiles} />
        </div>
      );
    }

    if (addDocOption === 'doi+upload') {
      return (
        <form onSubmit={async (e) => {
          e.preventDefault();
          setSubmitting(true);
          try {
            const doiSucceeded = !doi.trim() || await handleDoiSubmit();
            if (doiSucceeded) {
              if (file) await handleUpload(file);
              setAddDocOption(null);
              setAddDocModal(false);
            }
          } finally {
            setSubmitting(false);
          }
        }} className="space-y-4">
          <p className="text-xs text-(--text-secondary)">{t.inputDoiAndUploadDesc}</p>
          <input type="text" value={doi} onChange={e => setDoi(e.target.value)} placeholder={t.doiPlaceholder}
            className="w-full px-4 py-3 bg-(--surface-secondary) border border-(--border) rounded-xl text-(--text-primary) font-medium text-sm focus:outline-none focus:ring-2 focus:ring-(--focus) transition-colors" />
          <UploadZone onUpload={f => setFile(f)} accept=".pdf,.docx,.md,.tex" label={t.dropFiles} />
          {doiError && <p className="text-xs font-semibold text-rose-600">{doiError}</p>}
          <button type="submit" disabled={submitting || (!doi.trim() && !file)}
            className="w-full py-3 bg-(--brand) text-(--on-brand) font-bold text-xs rounded-xl hover:bg-(--brand-hover) transition-colors shadow-md disabled:opacity-50">{submitting ? ct.saving : ct.submit}</button>
        </form>
      );
    }

    return null;
  };

  const renderDocuments = () => (
    <div className="grid grid-cols-1 lg:grid-cols-5 gap-6 items-start">
      <div className="lg:col-span-2 space-y-4">
        <button id="add-doc-btn" onClick={() => setAddDocModal(true)}
          className="w-full py-3 bg-(--brand) text-(--on-brand) font-black text-xs rounded-xl hover:bg-(--brand-hover) transition-colors shadow-sm">
          + {t.addDocument}
        </button>
        {srcLoading ? <LoadingSkeleton count={4} height="h-12" /> : srcError ? (
          <div className="p-4 rounded-xl bg-rose-50 text-rose-700 text-xs font-bold">{srcError}</div>
        ) : sources.length === 0 ? (
          <EmptyState title={t.noDocuments} description={t.uploadDocsToCollection} />
        ) : (
          <div className="space-y-1.5 max-h-[500px] overflow-y-auto pr-1">
            {sources.map(doc => (
              <button key={doc.id} onClick={() => setSelectedSource(doc)}
                className={`w-full text-left p-3 rounded-xl border text-xs transition flex items-center gap-3 ${selectedSource?.id === doc.id
                  ? 'bg-(--brand-soft) border-indigo-300 shadow-sm'
                  : 'bg-(--surface) border-(--border) hover:border-indigo-300 hover:bg-(--surface-secondary)'
                  }`}>
                <FileIcon name={doc.originalFilename} />
                <div className="min-w-0 flex-1">
                  <p className="font-bold text-(--text-primary) truncate">{doc.originalFilename || doc.id}</p>
                  <div className="flex items-center gap-2 mt-0.5">
                    <span className={`px-1.5 py-0.5 rounded border text-[9px] font-bold ${statusColor(doc.processingStatus)}`}>{ct.statusLabels?.[doc.processingStatus] || doc.processingStatus}</span>
                    {doc.fileSizeBytes && <span className="text-[10px] text-(--text-tertiary)">{(doc.fileSizeBytes / 1024).toFixed(0)} KB</span>}
                  </div>
                </div>
              </button>
            ))}
          </div>
        )}
      </div>

      <div className="lg:col-span-3 bg-(--surface) rounded-2xl border border-(--border) shadow-sm min-h-[400px]">
        {!selectedSource ? (
          <div className="h-full flex flex-col items-center justify-center text-center p-8 text-(--text-tertiary)">
            <svg className="w-10 h-10 mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M3 7a2 2 0 012-2h5l2 2h7a2 2 0 012 2v8a2 2 0 01-2 2H5a2 2 0 01-2-2V7z" /></svg>
            <p className="text-xs font-semibold">{t.selectDocument}</p>
          </div>
        ) : (
          <div className="p-6 space-y-5">
            <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-3">
              <div className="flex items-center gap-3">
                <FileIcon name={selectedSource.originalFilename} className="w-7 h-7" />
                <div>
                  <h3 className="text-base font-black text-(--text-primary)">{selectedSource.originalFilename || t.unnamed}</h3>
                  <p className="text-[11px] text-(--text-tertiary) font-mono">ID: {selectedSource.id}</p>
                </div>
              </div>
              <div className="flex flex-wrap items-center gap-2">
                <button id="share-to-project" onClick={() => openShare(selectedSource.id)}
                  className="px-3 py-1.5 bg-(--brand-soft) text-(--brand-foreground) border border-indigo-200 rounded-lg font-bold hover:border-indigo-300 transition-colors text-xs">{t.shareToProject}</button>
                <button onClick={() => handleDeleteSource(selectedSource.id)}
                  className="px-3 py-1.5 bg-rose-50 text-rose-700 border border-rose-200 rounded-lg font-bold hover:bg-rose-100 transition-colors text-xs">{ct.delete}</button>
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
              {[
                { label: t.sourceStatus, value: selectedSource.processingStatus, badge: statusColor(selectedSource.processingStatus) },
                { label: t.sourceSize, value: selectedSource.fileSizeBytes ? `${(selectedSource.fileSizeBytes / 1024).toFixed(1)} KB` : '-' },
                { label: t.sourceType, value: selectedSource.contentType || '-' },
                { label: t.sourceCreated, value: selectedSource.createdAt ? new Date(selectedSource.createdAt).toLocaleString(language === 'vi' ? 'vi-VN' : 'en-US') : '-' },
              ].map(s => (
                <div key={s.label} className="p-3 bg-(--surface-secondary) rounded-xl border border-(--border-light)">
                  <p className="text-[10px] font-black text-(--text-tertiary) uppercase tracking-wider">{s.label}</p>
                  {s.badge ? (
                    <span className={`inline-block mt-1 px-2 py-0.5 rounded border text-[10px] font-bold ${s.badge}`}>{s.value}</span>
                  ) : (
                    <p className="mt-1 font-medium text-(--text-primary) break-words">{s.value}</p>
                  )}
                </div>
              ))}
            </div>

            {selectedSource.openAlexTopic || selectedSource.openAlexSubfield || selectedSource.openAlexField || selectedSource.openAlexDomain ? (
              <div className="space-y-3">
                {selectedSource.openAlexTopic ? (
                  <div className="p-3 bg-(--brand-soft) rounded-xl border border-indigo-100 dark:border-indigo-900">
                    <p className="text-[10px] font-black text-(--text-tertiary) uppercase tracking-wider">{t.openAlexTopic}</p>
                    <p className="mt-1 font-semibold text-(--text-primary)">{selectedSource.openAlexTopic}</p>
                  </div>
                ) : null}
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 text-xs">
                  {[
                    { label: t.openAlexSubfield, value: selectedSource.openAlexSubfield },
                    { label: t.openAlexField, value: selectedSource.openAlexField },
                    { label: t.openAlexDomain, value: selectedSource.openAlexDomain },
                  ].map(s => s.value ? (
                    <div key={s.label} className="p-3 bg-(--surface-secondary) rounded-xl border border-(--border-light)">
                      <p className="text-[10px] font-black text-(--text-tertiary) uppercase tracking-wider">{s.label}</p>
                      <p className="mt-1 font-medium text-(--text-primary) break-words">{s.value}</p>
                    </div>
                  ) : null)}
                </div>
              </div>
            ) : null}

            {selectedSource.processingStatus === 'READY' || selectedSource.processingStatus === 'COMPLETED' ? (
              <div className="pt-2 border-t border-(--border-light)">
                <p className="text-[10px] font-black text-(--text-tertiary) uppercase tracking-wider mb-2">{t.actions}</p>
                <button type="button" onClick={() => handleDownloadSource(selectedSource)}
                  className="inline-flex items-center gap-1.5 px-4 py-2 bg-emerald-600 text-white rounded-xl text-xs font-bold hover:bg-emerald-700 transition-colors">
                  {t.downloadPdf} ↗
                </button>
              </div>
            ) : null}
          </div>
        )}
      </div>
    </div>
  );

  const renderConnectedMap = () => {
    const shared = sources.filter(s => (s.projectIds || []).length > 0);
    return (
      <div className="space-y-3">
        <p className="text-xs text-(--text-secondary) font-medium">{t.sharedDocsDesc}</p>
        {shared.length === 0 ? (
          <EmptyState title={t.noSharedDocs} description={t.shareDescription} />
        ) : (
          <div className="space-y-2">
            {shared.map(doc => (
              <div key={doc.id} className="p-3 bg-(--surface) rounded-xl border border-(--border) text-xs flex flex-col sm:flex-row sm:justify-between sm:items-center gap-1">
                <span className="font-bold text-(--text-primary) truncate">{doc.originalFilename || doc.id}</span>
                <span className="text-(--text-tertiary) sm:text-right">
                  {t.project}: {doc.projectIds
                    .map(pid => projects.find(p => String(p.id) === String(pid))?.title || pid)
                    .join(', ')}
                </span>
              </div>
            ))}
          </div>
        )}
      </div>
    );
  };

  const fetchGraph = useCallback(async () => {
    setGraphLoading(true);
    try {
      const res = await api.get(`/api/collections/${id}/citation-graph`);
      setGraphData(res.data);
    } catch { setGraphData(null); }
    finally { setGraphLoading(false); }
  }, [id]);

  useEffect(() => {
    if (activeTab === 2) fetchGraph();
  }, [activeTab, fetchGraph]);

  useEffect(() => {
    if (!graphData || !graphRef.current || graphData.nodes.length === 0) return;

    if (networkRef.current) networkRef.current.destroy();

    const CANVAS_WIDTH = 3000;
    const CANVAS_HEIGHT = 1500;

    function nodeLabel(n) {
      if (!n.title && !n.doi) return '!';
      let authorName = null;
      if (n.authors) {
        try {
          const names = JSON.parse(n.authors);
          if (names?.length) authorName = names[0].split(' ')[0].replace(/,$/, '');
        } catch { }
      }
      if (authorName) return n.publicationYear ? `${authorName}, ${n.publicationYear}` : authorName;
      if (n.title) return n.title.length > 20 ? n.title.slice(0, 18) + '…' : n.title;
      return n.doi ? n.doi.slice(0, 20) : '?';
    }

    function nodeTooltip(n) {
      const parts = [];
      if (n.title) parts.push(n.title);
      if (n.authors) {
        try {
          const names = JSON.parse(n.authors);
          if (names?.length) parts.push(t.authorsBy.replace('{{authors}}', names.join(', ')));
        } catch { }
      }
      if (n.publicationYear) parts.push(`(${n.publicationYear})`);
      if (n.citedByCount != null) parts.push(t.citedTimes.replace('{{count}}', n.citedByCount));
      if (n.doi) parts.push(`DOI: ${n.doi}`);
      if (!n.title && !n.doi) parts.push(t.unresolvedReference);
      else if (!n.hasDoi) parts.push(t.noCitationData);
      return parts.join(' · ');
    }

    const years = graphData.nodes.map(n => n.publicationYear).filter(y => y != null);
    const minYear = Math.min(...years);
    const maxYear = Math.max(...years);
    const yearDelta = maxYear - minYear || 1;

    const logVals = graphData.nodes.map(n => n.citedByCount != null ? Math.log10(n.citedByCount + 1) : null).filter(v => v != null);
    const minLog = logVals.length ? Math.min(...logVals) : 0;
    const maxLog = logVals.length ? Math.max(...logVals) : 1;
    const logDelta = maxLog - minLog || 1;

    const positioned = graphData.nodes.map(n => {
      const unresolved = !n.title && !n.doi;
      const isNoDoi = !n.hasDoi;
      const baseX = n.publicationYear != null
        ? ((n.publicationYear - minYear) / yearDelta) * CANVAS_WIDTH - (CANVAS_WIDTH / 2)
        : 0;
      const logCit = n.citedByCount != null ? Math.log10(n.citedByCount + 1) : minLog;
      const baseY = -(((logCit - minLog) / logDelta) * CANVAS_HEIGHT) + (CANVAS_HEIGHT / 2);

      let bg, border, fontColor, shape;
      if (unresolved) {
        bg = '#fffbeb'; border = '#f59e0b'; fontColor = '#92400e'; shape = 'diamond';
      } else if (isNoDoi) {
        bg = '#f1f5f9'; border = '#cbd5e1'; fontColor = '#64748b'; shape = 'dot';
      } else if (n.inCollection) {
        bg = '#eef2ff'; border = '#6366f1'; fontColor = '#4338ca'; shape = 'dot';
      } else {
        bg = '#f8fafc'; border = '#94a3b8'; fontColor = '#475569'; shape = 'dot';
      }

      return {
        id: n.id, baseX, baseY,
        label: nodeLabel(n),
        title: nodeTooltip(n),
        color: { background: bg, border },
        font: { color: '#333333', size: 16, background: 'rgba(255, 255, 255, 0.8)', vadjust: 10 },
        shape,
        value: Number(n.citedByCount != null ? n.citedByCount : 0) + 1,
        borderWidth: (unresolved || isNoDoi) ? 2 : 1,
        borderWidthSelected: 2.5,
        x: baseX, y: baseY,
      };
    });

    const collideRadius = 80;
    for (let i = 0; i < positioned.length; i++) {
      let attempts = 0;
      while (attempts < 200) {
        let collided = false;
        for (let j = 0; j < i; j++) {
          const dx = positioned[i].x - positioned[j].x;
          const dy = positioned[i].y - positioned[j].y;
          if (Math.hypot(dx, dy) < collideRadius) { collided = true; break; }
        }
        if (!collided) break;
        const angle = attempts * 0.5;
        const r = 60 + attempts * 12;
        positioned[i].x = positioned[i].baseX + r * Math.cos(angle);
        positioned[i].y = positioned[i].baseY + r * Math.sin(angle);
        attempts++;
      }
      // spiral offset up to 200 attempts per node, sufficient for < 200 nodes
    }

    const nodes = new DataSet(positioned.map(p => {
      const { baseX, baseY, ...node } = p;
      return node;
    }));

    const edges = new DataSet(graphData.edges.map(e => {
      const isCitedBy = e.type === 'CITED_BY';
      return {
        from: isCitedBy ? e.targetId : e.sourceId,
        to: isCitedBy ? e.sourceId : e.targetId,
        color: { color: isCitedBy ? '#10b981' : '#3b82f6', opacity: 0.5 },
        width: 1,
        dashes: isCitedBy,
      };
    }));

    const options = {
      physics: false,
      nodes: {
        scaling: { min: 20, max: 60 },
        font: { face: 'Inter, system-ui, sans-serif' },
      },
      edges: {
        smooth: { type: 'cubicBezier', forceDirection: 'horizontal', roundness: 0.35 },
        color: { inherit: false },
        arrows: { to: { enabled: true, scaleFactor: 0.5 } },
      },
      interaction: { dragNodes: false, hover: true, tooltipDelay: 200 },
    };

    const network = new Network(graphRef.current, { nodes, edges }, options);
    networkRef.current = network;

    network.on('zoom', () => {
      if (network.getScale() < 0.2) network.moveTo({ scale: 0.2, duration: 0 });
    });

    requestAnimationFrame(() => network.fit({ animation: true }));

    network.on('click', (params) => {
      if (params.nodes.length > 0) {
        const nodeId = params.nodes[0];
        const nodeData = graphData.nodes.find(n => n.id === nodeId);
        setSelectedGraphNode(nodeData || null);
      } else {
        setSelectedGraphNode(null);
      }
    });

    return () => { if (networkRef.current) networkRef.current.destroy(); };
  }, [graphData, t]);

  const renderVisualizeMap = () => (
    <div className="flex w-full h-[calc(100vh-3.5rem)] overflow-hidden bg-white">
      {graphLoading ? (
        <div className="flex-1 flex items-center justify-center p-6">
          <LoadingSkeleton count={6} height="h-12" />
        </div>
      ) : !graphData || graphData.nodes.length === 0 ? (
        <div className="flex-1 flex flex-col items-center justify-center text-center p-8 text-slate-400">
          <svg className="w-10 h-10 mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M10 13a5 5 0 007.54.54l2-2a5 5 0 00-7.07-7.07l-1.15 1.15m2.68 5.38a5 5 0 00-7.54-.54l-2 2a5 5 0 007.07 7.07l1.15-1.15" /></svg>
          <p className="text-xs font-semibold">{t.citationGraphEmpty}</p>
          <p className="text-[10px] mt-1">{t.visualizeDesc}</p>
        </div>
      ) : (
        <div className="flex-1 relative overflow-hidden">
          <div ref={graphRef} id="visual-map-container" className="absolute inset-0 w-full h-full z-0" />

          <div className="absolute inset-y-0 left-4 z-20 flex flex-col justify-start pt-4 text-sm font-bold text-gray-500 select-none pointer-events-none">
            <span className="flex items-center gap-1">{t.citationsAxis} <span className="text-sm font-bold text-gray-400">↑</span></span>
            <span className="text-[10px] font-normal text-gray-400">{t.higherMoreCited}</span>
          </div>
          <div className="absolute inset-x-0 bottom-4 z-20 flex justify-start pl-4 text-sm font-bold text-gray-500 select-none pointer-events-none">
            <span className="flex items-center gap-2">{t.publicationYear} <span className="text-sm font-bold text-gray-400">→</span></span>
          </div>
          <div className="absolute bottom-4 right-4 z-10 text-[8px] text-gray-400 font-medium select-none pointer-events-none text-right leading-relaxed">
            <span className="inline-flex items-center gap-1"><span className="w-2 h-2 rounded-full" style={{ background: '#eef2ff', border: '1px solid #6366f1' }} /> {t.sourceLegend}</span>
            <br />
            <span className="inline-flex items-center gap-1"><span className="w-2 h-2 rounded-full" style={{ background: '#f8fafc', border: '1px solid #94a3b8' }} /> {t.externalLegend}</span>
            <br />
            <span className="inline-flex items-center gap-1"><span className="w-2 h-2" style={{ background: '#fffbeb', border: '1px solid #f59e0b', transform: 'rotate(45deg)', display: 'inline-block' }} /> {t.unresolvedLegend}</span>
          </div>
        </div>
      )}
      {selectedGraphNode && (
        <div className="w-80 max-w-[80vw] shrink-0 border-l border-slate-200 bg-white p-5 space-y-3 overflow-y-auto">
          <div className="flex items-start justify-between">
            <span className="text-[10px] font-black text-gray-400 uppercase tracking-wider">{ct.name}</span>
            <button onClick={() => setSelectedGraphNode(null)} className="text-gray-400 hover:text-gray-600 p-1" aria-label={ct.close}><svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" /></svg></button>
          </div>
          <p className="text-sm font-semibold text-gray-900 break-words">{selectedGraphNode.title || (selectedGraphNode.inCollection ? t.unnamed : t.unresolvedReference)}</p>
          {selectedGraphNode.doi && (
            <div>
              <p className="text-[10px] font-black text-gray-400 uppercase tracking-wider">DOI</p>
              <p className="text-xs font-mono text-blue-600 break-all">{selectedGraphNode.doi}</p>
            </div>
          )}
          {selectedGraphNode.authors && (
            <div>
              <p className="text-[10px] font-black text-gray-400 uppercase tracking-wider">{t.authors}</p>
              <p className="text-xs text-gray-700">{selectedGraphNode.authors}</p>
            </div>
          )}
          {selectedGraphNode.publicationYear && (
            <div>
              <p className="text-[10px] font-black text-gray-400 uppercase tracking-wider">{t.publicationYear}</p>
              <p className="text-xs text-gray-700">{selectedGraphNode.publicationYear}</p>
            </div>
          )}
          {selectedGraphNode.hasDoi && selectedGraphNode.citedByCount != null ? (
            <div>
              <p className="text-[10px] font-black text-gray-400 uppercase tracking-wider">{t.sourceCitations}</p>
              <p className="text-xs text-gray-700">{t.citedTimes.replace('{{count}}', selectedGraphNode.citedByCount)}</p>
            </div>
          ) : !selectedGraphNode.hasDoi && (selectedGraphNode.title || !selectedGraphNode.inCollection) ? (
            <div>
              <p className="text-[10px] font-black text-gray-400 uppercase tracking-wider">{t.citationData}</p>
              <p className="text-xs text-gray-400 italic">{t.noCitationData}</p>
            </div>
          ) : null}
          <div className="pt-2 border-t border-gray-100">
            {selectedGraphNode.inCollection ? (
              <p className="text-[10px] font-semibold text-indigo-600">{t.inCollection}</p>
            ) : selectedGraphNode.title || selectedGraphNode.doi ? (
              <p className="text-[10px] font-semibold text-gray-400">{t.citationGraphExternal}</p>
            ) : (
              <p className="text-[10px] font-semibold text-rose-500">{t.unresolvedMetadata}</p>
            )}
            {selectedGraphNode.doi && (
              <a href={`https://doi.org/${selectedGraphNode.doi}`} target="_blank" rel="noopener noreferrer"
                className="inline-block mt-2 px-3 py-1.5 bg-gray-50 border border-gray-200 rounded-lg text-xs font-bold text-gray-600 hover:bg-gray-100 transition-colors">
                {t.openDoi} ↗
              </a>
            )}
          </div>
        </div>
      )}
    </div>
  );

  const renderAnalyze = () => {
    const total = sources.length;
    const ready = sources.filter(s => s.processingStatus === 'READY' || s.processingStatus === 'COMPLETED').length;
    const failed = sources.filter(s => s.processingStatus === 'FAILED').length;
    const processing = total - ready - failed;
    const totalSize = sources.reduce((sum, s) => sum + (s.fileSizeBytes || 0), 0);
    return (
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {[
          { label: t.totalDocuments, value: total },
          { label: t.processed, value: ready, color: 'text-emerald-700' },
          { label: t.processing, value: processing, color: 'text-amber-700' },
          { label: t.reject, value: failed, color: 'text-rose-700' },
          { label: t.collectionStats, value: totalSize > 0 ? `${(totalSize / (1024 * 1024)).toFixed(1)} MB` : '0 B' },
        ].map(stat => (
          <div key={stat.label} className="p-4 bg-(--surface) rounded-xl border border-(--border)">
            <p className="text-[10px] font-black text-(--text-tertiary) uppercase tracking-wider">{stat.label}</p>
            <p className={`text-2xl font-black mt-1 ${stat.color || 'text-(--text-primary)'}`}>{stat.value}</p>
          </div>
        ))}
      </div>
    );
  };

  const tabContent = [renderDocuments, renderConnectedMap, renderVisualizeMap, renderAnalyze];

  return (
    <div className="min-h-screen bg-(--page-bg) text-(--text-primary)">
      <AppHeader />
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="mb-2">
          <Link to="/instructor/collections" className="text-xs font-bold text-(--text-tertiary) hover:text-(--brand-foreground) transition-colors">&larr; {ct.back}</Link>
        </div>

        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6 border-b border-(--border) pb-4">
          <div className="min-w-0">
            {collectionLoading ? (
              <div className="space-y-1">
                <div className="h-8 w-64 max-w-full bg-(--surface-tertiary) rounded-lg animate-pulse" />
                <div className="h-4 w-96 max-w-full bg-(--surface-secondary) rounded animate-pulse" />
              </div>
            ) : collection ? (
              <>
                <h1 className="text-3xl font-black text-(--brand-foreground) tracking-tight truncate">{collection.name}</h1>
                {collection.description && <p className="text-sm text-(--text-secondary) mt-1 truncate">{collection.description}</p>}
                {collection.categoryName && <span className="inline-block mt-1.5 bg-indigo-50 text-indigo-600 px-2 py-0.5 rounded border border-indigo-200 text-[10px] font-semibold">{collection.categoryName}</span>}
              </>
            ) : (
              <h1 className="text-3xl font-black text-(--brand-foreground) tracking-tight">{t.collectionDetail}</h1>
            )}
          </div>
          <div className="flex flex-wrap items-center gap-2 shrink-0">
            {collection && (
              <>
                <button onClick={handleEditOpen}
                  className="px-3 py-1.5 bg-(--surface) border border-(--border) rounded-lg text-xs font-bold text-(--text-secondary) hover:bg-(--surface-secondary) transition-colors">{ct.edit}</button>
                <button onClick={handleDeleteCollection}
                  className="px-3 py-1.5 bg-(--surface) border border-rose-200 rounded-lg text-xs font-bold text-rose-600 hover:bg-rose-50 transition-colors">{ct.delete}</button>
              </>
            )}
            <TourLauncher steps={TOUR_STEPS} tourKey="instructor-collection-detail"
              className="w-9 h-9 rounded-full bg-(--surface) border border-(--border) shadow-sm flex items-center justify-center text-sm font-bold text-(--text-secondary) hover:bg-(--brand-soft) hover:text-(--brand) transition-colors" />
          </div>
        </div>

        <div className="flex gap-1 mb-6 border-b border-(--border) overflow-x-auto">
          {TABS.map((tab, i) => (
            <button key={tab} id={TAB_IDS[i]} onClick={() => setActiveTab(i)}
              className={`px-4 py-2 text-xs font-bold rounded-t-lg transition-colors whitespace-nowrap ${activeTab === i ? 'bg-(--surface) text-(--brand-foreground) border border-b-(--surface) border-(--border) -mb-px' : 'text-(--text-tertiary) hover:text-(--text-primary)'
                }`}>{t[tab]}</button>
          ))}
        </div>

        {tabContent[activeTab]()}
      </main>

      <Modal open={shareModal.open} onClose={() => setShareModal({ open: false, sourceId: null })} title={t.shareToProject} closeLabel={ct.close}>
        <div className="space-y-3 text-xs">
          <input type="text" value={projectSearch} onChange={(e) => setProjectSearch(e.target.value)}
            placeholder={t.searchProjects} className="w-full px-3 py-2 bg-(--surface-secondary) border border-(--border) text-(--text-primary) rounded-xl font-medium focus:outline-none focus:ring-2 focus:ring-(--focus)" />
          <div className="max-h-60 overflow-y-auto space-y-1">
            {filteredProjects.length === 0 ? (
              <p className="text-(--text-tertiary) text-center py-4 font-medium">{ct.noData}</p>
            ) : filteredProjects.map(p => (
              <button key={p.id} onClick={() => doShare(p.id)}
                className="w-full text-left px-3 py-2 rounded-lg hover:bg-(--brand-soft) transition-colors font-medium text-(--text-primary)">
                {p.title}
              </button>
            ))}
          </div>
        </div>
      </Modal>

      <Modal open={addDocModal} onClose={() => { setAddDocModal(false); setAddDocOption(null); }} title={t.addDocument} closeLabel={ct.close}>
        <div className="space-y-4 text-xs">
          <div className="grid grid-cols-1 gap-2">
            {[
              { key: 'doi', label: t.inputDoi, desc: t.inputDoiDescription },
              { key: 'upload', label: t.uploadDocument, desc: t.uploadDocumentDescription },
              { key: 'doi+upload', label: t.inputDoiAndUpload, desc: t.inputDoiAndUploadDesc },
            ].map(opt => (
              <button key={opt.key} onClick={() => setAddDocOption(opt.key)}
                className={`w-full text-left p-3 rounded-xl border transition flex items-center gap-3 ${addDocOption === opt.key
                  ? 'bg-(--brand-soft) border-indigo-300 shadow-sm'
                  : 'bg-(--surface) border-(--border) hover:border-indigo-300 hover:bg-(--surface-secondary)'
                  }`}>
                <svg className="w-5 h-5 text-(--brand) shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4v16m8-8H4" /></svg>
                <div>
                  <p className="font-bold text-(--text-primary)">{opt.label}</p>
                  <p className="text-[10px] text-(--text-tertiary) mt-0.5">{opt.desc}</p>
                </div>
              </button>
            ))}
          </div>
          <AddDocForm />
        </div>
      </Modal>

      <Modal open={editModal.open} onClose={() => setEditModal(p => ({ ...p, open: false }))} title={t.editCollection} closeLabel={ct.close}>
        <form onSubmit={handleEditSubmit} className="space-y-4 text-xs">
          <div>
            <label className="block text-[10px] font-black text-(--text-tertiary) uppercase tracking-wider mb-1">{t.collectionName}</label>
            <input type="text" value={editModal.name} onChange={e => setEditModal(p => ({ ...p, name: e.target.value }))} required maxLength={255}
              className="w-full px-4 py-3 bg-(--surface-secondary) border border-(--border) text-(--text-primary) rounded-xl font-medium text-sm focus:outline-none focus:ring-2 focus:ring-(--focus) transition-colors" />
          </div>
          <div>
            <label className="block text-[10px] font-black text-(--text-tertiary) uppercase tracking-wider mb-1">{t.collectionDescription}</label>
            <textarea value={editModal.description} onChange={e => setEditModal(p => ({ ...p, description: e.target.value }))} rows={3}
              className="w-full px-4 py-3 bg-(--surface-secondary) border border-(--border) text-(--text-primary) rounded-xl font-medium text-sm focus:outline-none focus:ring-2 focus:ring-(--focus) transition-colors resize-none" />
          </div>
          <div>
            <label className="block text-[10px] font-black text-(--text-tertiary) uppercase tracking-wider mb-1">{t.category}</label>
            <select value={editModal.categoryId} onChange={e => setEditModal(p => ({ ...p, categoryId: e.target.value }))}
              className="w-full px-4 py-3 bg-(--surface-secondary) border border-(--border) text-(--text-primary) rounded-xl font-medium text-sm focus:outline-none focus:ring-2 focus:ring-(--focus) transition-colors">
              <option value="">{t.noCategory}</option>
              {categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
          </div>
          <div className="flex gap-2 justify-end pt-2">
            <button type="button" onClick={() => setEditModal(p => ({ ...p, open: false }))}
              className="px-4 py-2 bg-(--surface-secondary) text-(--text-secondary) rounded-xl font-bold text-xs hover:bg-(--surface-tertiary) transition-colors">{ct.cancel}</button>
            <button type="submit" disabled={editModal.submitting || !editModal.name.trim()}
              className="px-4 py-2 bg-(--brand) text-(--on-brand) rounded-xl font-bold text-xs hover:bg-(--brand-hover) transition-colors disabled:opacity-50">{editModal.submitting ? ct.saving : ct.save}</button>
          </div>
        </form>
      </Modal>

    </div>
  );
}
