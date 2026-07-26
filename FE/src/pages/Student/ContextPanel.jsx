import { UI_TEXT } from '../../constants/uiText';
import api from '../../api.js';

export default function ContextPanel({
  isOpen, width, onResizeStart,
  activeTab, setActiveTab,
  language, showToast,
  // Source tab
  sources, isUploading, setIsUploading, project, setViewerFile, fetchSources,
  // Claims tab
  sections, selectedSectionId, setSelectedSectionId,
  newClaimContent, setNewClaimContent, handleCreateClaim,
  claims, selectedClaim, claimMatches, loadingMatches,
  handleFetchMatches, handleAnalyzeClaim, canEditClaim,
  editingClaim, setEditingClaim, editClaimContent, setEditClaimContent, handleDeleteClaim, handleUpdateClaim,
  // Feedback tab
  feedbacks, setShowSubmitReviewModal,
  // Graph tab
  graphData, fetchGraphData, dynamicNodes, hoveredNodeId, setHoveredNodeId,
  papers, selectedPaperDetail, setSelectedPaperDetail,
  handleExportCsv, handleExportJson, setSelectedPaper, loadCode,
  renderModalPaperPdf,
}) {
  const t = UI_TEXT[language];
  if (!isOpen) return null;

  const activeClass = (tab) =>
    `flex-1 py-3 text-[10px] font-bold uppercase tracking-wider flex flex-col justify-center items-center gap-1 transition-all relative ${activeTab === tab ? 'text-indigo-600' : 'text-slate-500 hover:text-slate-800 hover:bg-slate-50'}`;

  return (
    <>
      <div onMouseDown={onResizeStart} className="w-1 hover:w-1.5 bg-slate-200 hover:bg-slate-400 cursor-col-resize self-stretch transition-all shrink-0 z-30 relative group flex items-center justify-center border-l border-slate-200/80">
        <div className="h-6 w-0.5 bg-slate-400 group-hover:bg-slate-500 rounded"></div>
      </div>
      <aside style={{ width }} className="bg-white border-l border-slate-200 flex flex-col shrink-0 shadow-[-4px_0_15px_-3px_rgba(0,0,0,0.05)] z-10 overflow-hidden">
        <div className="flex border-b border-slate-200 bg-white relative shrink-0">
          <button onClick={() => setActiveTab('Source')} className={activeClass('Source')}>
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
            {t.tabInfo}
            {activeTab === 'Source' && <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-indigo-600 shadow-[0_-2px_8px_rgba(79,70,229,0.5)]"></div>}
          </button>
          <button onClick={() => setActiveTab('Claims')} className={activeClass('Claims')}>
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01" /></svg>
            {t.tabClaims}
            {activeTab === 'Claims' && <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-indigo-600 shadow-[0_-2px_8px_rgba(79,70,229,0.5)]"></div>}
          </button>
          <button onClick={() => setActiveTab('Feedback')} className={activeClass('Feedback')}>
            <div className="relative">
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z" /></svg>
              {feedbacks.length > 0 && <span className="absolute -top-1.5 -right-2 bg-rose-500 text-white flex items-center justify-center text-[9px] w-4 h-4 rounded-full font-bold animate-pulse">{feedbacks.length}</span>}
            </div>
            {t.tabFeedback}
            {activeTab === 'Feedback' && <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-indigo-600 shadow-[0_-2px_8px_rgba(79,70,229,0.5)]"></div>}
          </button>
          <button onClick={() => setActiveTab('Graph')} className={activeClass('Graph')}>
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M7 16V4m0 0L3 8m4-4l4 4m6 0v12m0 0l4-4m-4 4l-4-4" /></svg>
            {t.tabGraph}
            {activeTab === 'Graph' && <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-indigo-600 shadow-[0_-2px_8px_rgba(79,70,229,0.5)]"></div>}
          </button>
        </div>

        <div className="flex-1 overflow-y-auto bg-slate-50/50 p-4">
          {activeTab === 'Source' && (
            <div className="p-5 flex flex-col gap-6 animate-in fade-in duration-300">
              <label className={`w-full flex justify-center items-center gap-2 border-2 border-dashed rounded-xl p-6 transition-all group mb-6 shadow-sm cursor-pointer ${isUploading ? 'border-indigo-300 bg-indigo-100/50 opacity-60 pointer-events-none' : 'border-indigo-200 hover:border-indigo-400 bg-indigo-50/50 hover:bg-indigo-50'}`}>
                <input type="file" className="hidden" accept=".pdf,.docx" disabled={isUploading}
                  onChange={async (e) => {
                    if (!e.target.files?.[0] || !project) return;
                    setIsUploading(true);
                    const fd = new FormData(); fd.append('file', e.target.files[0]);
                    try { await api.post('/api/sources', fd); showToast(`${e.target.files[0].name} uploaded.`); if (fetchSources) fetchSources(); } catch (err) { showToast('Upload failed.'); } finally { setIsUploading(false); }
                  }}
                />
                <div className="bg-white p-2 rounded-full shadow-sm group-hover:scale-110 transition-transform">
                  {isUploading ? <div className="animate-spin w-5 h-5 border-2 border-indigo-500 border-t-transparent rounded-full"></div> : (
                    <svg className="w-5 h-5 text-indigo-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12" /></svg>
                  )}
                </div>
                <span className="text-sm font-semibold text-indigo-700">{isUploading ? 'Uploading...' : 'Upload PDF / DOCX'}</span>
              </label>

              <div>
                <h3 className="text-[11px] font-bold text-slate-400 tracking-widest mb-3 uppercase flex items-center gap-2"><div className="h-px bg-slate-200 flex-1"></div> Uploaded Sources <div className="h-px bg-slate-200 flex-1"></div></h3>
                <div className="flex flex-col gap-3">
                  {sources.length === 0 ? <div className="text-sm text-slate-500 italic text-center p-4">No uploaded sources yet.</div> : (
                    sources.map(src => (
                      <div key={src.id} onClick={() => src.fileUrl ? setViewerFile({ fileUrl: src.fileUrl, fileName: src.originalFilename }) : showToast('File URL not available')} className="bg-white border border-slate-200 rounded-xl p-3.5 hover:shadow-md hover:border-indigo-300 transition-all cursor-pointer transform hover:-translate-y-0.5">
                        <p className="text-sm font-bold text-slate-800 flex items-center gap-2"><svg className="w-4 h-4 text-red-500" fill="currentColor" viewBox="0 0 20 20"><path fillRule="evenodd" d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4z" clipRule="evenodd" /></svg>{src.originalFilename}</p>
                        <p className="text-xs text-slate-500 mt-1.5 line-clamp-2 leading-relaxed">Source file uploaded to this project.</p>
                      </div>
                    ))
                  )}
                </div>
              </div>
            </div>
          )}

          {activeTab === 'Claims' && (
            <div className="space-y-3">
              <div className="bg-white border border-slate-200 rounded-xl p-3.5 shadow-sm">
                <h4 className="text-[11px] font-bold text-slate-500 mb-2 uppercase tracking-wider">Add Claim</h4>
                <div className="flex gap-2 flex-wrap">
                  <select value={selectedSectionId} onChange={(e) => setSelectedSectionId(e.target.value)} className="text-xs border border-slate-200 rounded-lg px-2 py-1.5 bg-white outline-none focus:ring-1 focus:ring-indigo-500">
                    {sections.length === 0 && <option value="">No sections</option>}
                    {sections.map(s => <option key={s.id} value={s.id}>{s.sectionTitle}{s.assignedUserId === 'current' ? ' (Yours)' : ''}</option>)}
                  </select>
                  <input value={newClaimContent} onChange={(e) => setNewClaimContent(e.target.value)} placeholder="Claim content..." className="flex-1 text-xs border border-slate-200 rounded-lg px-2 py-1.5 outline-none focus:ring-1 focus:ring-indigo-500 min-w-[120px]" />
                  <button onClick={handleCreateClaim} disabled={!newClaimContent.trim() || !selectedSectionId} className="text-xs font-bold text-white bg-indigo-600 hover:bg-indigo-700 disabled:bg-slate-300 px-3 py-1.5 rounded-lg transition-colors">Add</button>
                </div>
              </div>
              {claims.length === 0 ? <div className="text-xs text-slate-400 italic text-center py-8">No claims yet.</div> : (
                claims.map(claim => {
                  const isSelected = selectedClaim?.id === claim.id;
                  return (
                    <div key={claim.id} onClick={() => { handleFetchMatches(claim.id); }} className={`bg-white border rounded-xl p-3.5 shadow-sm hover:shadow-md transition-all relative overflow-hidden group cursor-pointer ${isSelected ? 'border-indigo-400 ring-1 ring-indigo-400/20' : 'border-slate-200'}`}>
                      <div className="absolute left-0 top-0 bottom-0 w-1.5 bg-indigo-500"></div>
                      <div className="flex justify-between items-center mb-1.5 pl-1">
                        <span className="text-[9px] font-black text-indigo-700 bg-indigo-50 px-1.5 py-0.5 rounded border border-indigo-100 uppercase tracking-wide">ID: {claim.id}</span>
                        {claim.aiConfidenceScore !== null ? (
                          <span className={`text-[10px] font-bold px-1.5 py-0.5 rounded ${claim.aiConfidenceScore >= 0.7 ? 'bg-emerald-50 text-emerald-700 border border-emerald-100' : claim.aiConfidenceScore >= 0.4 ? 'bg-amber-50 text-amber-700 border-amber-100' : 'bg-rose-50 text-rose-700 border border-rose-100'}`}>
                            Confidence: {(claim.aiConfidenceScore * 100).toFixed(0)}%
                          </span>
                        ) : <span className="text-[10px] text-slate-400 italic">Unanalyzed</span>}
                      </div>
                      <p className="text-xs font-semibold text-slate-800 pl-1 leading-relaxed">{claim.content}</p>
                      <div className="flex gap-2 mt-3 pt-2.5 border-t border-slate-100 pl-1">
                        <button onClick={(e) => { e.stopPropagation(); handleAnalyzeClaim(claim.id); }} className="text-[10px] font-bold text-indigo-600 hover:text-indigo-800 flex items-center gap-1">
                          <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 10V3L4 14h7v7l9-11h-7z" /></svg>
                          AI Analyze
                        </button>
                        {canEditClaim(claim) && <>
                          <button onClick={(e) => { e.stopPropagation(); setEditingClaim(claim); setEditClaimContent(claim.content); }} className="text-[10px] text-slate-500 hover:text-slate-700 flex items-center gap-0.5 ml-auto">Edit</button>
                          <button onClick={(e) => { e.stopPropagation(); handleDeleteClaim(claim.id); }} className="text-[10px] text-rose-500 hover:text-rose-700 flex items-center gap-0.5">Delete</button>
                        </>}
                      </div>
                      {isSelected && (
                        <div className="mt-3 pt-3 border-t border-dashed border-slate-200 animate-in fade-in slide-in-from-top-1 duration-200">
                          <h4 className="text-[10px] font-bold text-slate-400 uppercase tracking-widest mb-2">Matching Evidence</h4>
                          {loadingMatches ? <div className="text-center py-2 text-[10px] text-slate-400 italic">Searching...</div> : claimMatches.length === 0 ? (
                            <div className="text-center py-2 text-[10px] text-slate-400 italic">No matches found.</div>
                          ) : (
                            <div className="space-y-2">
                              {claimMatches.map((m, idx) => (
                                <div key={idx} className="bg-slate-50 border border-slate-200 rounded p-2 text-[11px] hover:bg-indigo-50/30 transition-colors">
                                  <div className="flex justify-between items-center mb-1 text-[9px] font-medium text-slate-500">
                                    <span className="truncate max-w-[150px] font-bold text-slate-700 flex items-center gap-1"><svg className="w-2.5 h-2.5 text-red-400" fill="currentColor" viewBox="0 0 20 20"><path fillRule="evenodd" d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4z" clipRule="evenodd" /></svg>{m.sourceFilename}</span>
                                    <span className="text-indigo-600 font-bold bg-indigo-50 px-1 rounded">{(m.score * 100).toFixed(0)}% match</span>
                                  </div>
                                  <p className="text-[10px] text-slate-600 line-clamp-3 italic leading-relaxed">"{m.excerpt}"</p>
                                </div>
                              ))}
                            </div>
                          )}
                        </div>
                      )}
                      {editingClaim && editingClaim.id === claim.id && (
                        <div className="mt-3 pt-3 border-t border-dashed border-slate-200">
                          <input value={editClaimContent} onChange={(e) => setEditClaimContent(e.target.value)} className="w-full text-xs border border-slate-200 rounded-lg px-2 py-1.5 outline-none focus:ring-1 focus:ring-indigo-500 mb-2" />
                          <div className="flex gap-2 justify-end">
                            <button onClick={() => setEditingClaim(null)} className="text-[10px] text-slate-500 hover:text-slate-700 font-bold">Cancel</button>
                            <button onClick={handleUpdateClaim} className="text-[10px] font-bold text-white bg-indigo-600 hover:bg-indigo-700 px-2 py-1 rounded-lg">Save</button>
                          </div>
                        </div>
                      )}
                    </div>
                  );
                })
              )}
            </div>
          )}

          {activeTab === 'Feedback' && (
            <div className="flex flex-col gap-4 animate-in fade-in duration-200">
              <div className="flex justify-between items-center mb-1 bg-white border border-slate-200 rounded-xl p-3.5 shadow-sm">
                <div>
                  <p className="text-[10px] text-slate-400 uppercase tracking-wider font-bold">Project Status</p>
                  <p className="text-sm font-bold text-slate-800 mt-0.5">{project?.status || 'Unknown'}</p>
                </div>
                {project?.status === 'ACTIVE' && <button onClick={() => setShowSubmitReviewModal(true)} className="bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-bold px-3 py-1.5 rounded-lg shadow-sm transition-all">Submit for Review</button>}
              </div>
              <h3 className="text-[11px] font-bold text-slate-400 tracking-widest uppercase flex items-center gap-2 mt-2"><div className="h-px bg-slate-200 flex-1"></div> Review History <div className="h-px bg-slate-200 flex-1"></div></h3>
              <div className="space-y-4">
                {feedbacks.length === 0 ? <div className="text-xs text-slate-400 italic text-center py-8">No reviews yet.</div> : (
                  feedbacks.map((fb, idx) => (
                    <div key={fb.id || idx} className="bg-white border border-slate-200 rounded-xl shadow-sm overflow-hidden">
                      <div className="bg-slate-50 border-b border-slate-100 p-3 flex justify-between items-start">
                        <div className="flex items-center gap-2">
                          <div className="w-7 h-7 rounded-full bg-indigo-100 text-indigo-700 flex items-center justify-center font-bold text-xs border border-indigo-200">I</div>
                          <div>
                            <p className="text-xs font-bold text-slate-700">Instructor (ID: {fb.instructorId})</p>
                            <p className="text-[9px] text-slate-400 font-medium">{fb.requestedAt ? new Date(fb.requestedAt).toLocaleString() : ''}</p>
                          </div>
                        </div>
                        <span className={`text-[9px] px-2 py-0.5 rounded font-black border uppercase ${fb.status === 'PENDING' ? 'bg-amber-50 text-amber-700 border-amber-200' : fb.status === 'RETURNED' ? 'bg-rose-50 text-rose-700 border-rose-200' : fb.status === 'REVIEWED' ? 'bg-emerald-50 text-emerald-700 border-emerald-200' : 'bg-rose-50 text-rose-700'}`}>{fb.status}</span>
                      </div>
                      <div className="p-3 text-xs leading-relaxed text-slate-700">
                        {fb.status === 'PENDING' && <p className="text-amber-600 font-medium italic">Project submitted. Waiting for instructor review.</p>}
                        {fb.status === 'RETURNED' && <p className="text-rose-600 font-medium">Instructor returned the project. Please review and resubmit.</p>}
                        {fb.status === 'REVIEWED' && <p className="text-emerald-600 font-medium">Instructor approved the project.</p>}
                        {fb.status === 'REJECTED' && <p className="text-red-600 font-medium">Review request was rejected.</p>}
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>
          )}

          {activeTab === 'Graph' && (
            <div className="flex flex-col gap-4 animate-in fade-in duration-200">
              <div className="bg-slate-900 rounded-xl p-4 border border-slate-800 text-slate-200 shadow-lg">
                <div className="flex justify-between items-center mb-3">
                  <h4 className="font-bold text-xs text-indigo-400 flex items-center gap-1">
                    <svg className="w-3.5 h-3.5 animate-pulse" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7" /></svg>
                    Paper Connection Map
                  </h4>
                  <span className="text-[9px] text-slate-500 font-mono font-semibold">Total: {papers.length} files</span>
                </div>
                <div className="bg-slate-950/50 border border-slate-800 rounded-lg px-3 py-2.5 mb-3 min-h-[54px] flex items-center justify-center transition-all duration-300">
                  {hoveredNodeId ? ((() => {
                    const node = dynamicNodes.find(p => p.id === hoveredNodeId);
                    if (!node) return null;
                    return (
                      <div className="w-full flex items-center justify-between gap-3 text-left animate-in fade-in duration-200">
                        <div className="truncate">
                          <div className="flex items-center gap-2 mb-0.5">
                            <span className="text-[9px] font-black text-white px-1.5 py-0.2 rounded uppercase tracking-wider" style={{ backgroundColor: node.color }}>Paper #{node.num}</span>
                            <span className="text-[9px] text-slate-400 font-bold font-mono truncate">{node.name}</span>
                          </div>
                          <p className="text-[11px] font-bold text-slate-200 line-clamp-1 leading-snug">{node.title}</p>
                        </div>
                        <span className="text-[9px] text-indigo-400 font-bold shrink-0">View detail</span>
                      </div>
                    );
                  })()) : (
                    <p className="text-[11px] text-slate-400 italic text-center font-medium">Hover over numbers to inspect draft titles...</p>
                  )}
                </div>
                <div className="bg-slate-950 border border-slate-800 rounded-lg p-2 flex justify-center items-center relative overflow-hidden select-none">
                  <svg className="w-full max-w-[340px] h-[320px]" viewBox="0 0 340 320">
                    {(() => {
                      const links = [];
                      const cats = {};
                      dynamicNodes.forEach(n => { if (!cats[n.category]) cats[n.category] = []; cats[n.category].push(n); });
                      Object.values(cats).forEach(nodes => { for (let i = 0; i < nodes.length; i++) { for (let j = i + 1; j < nodes.length; j++) { links.push({ source: nodes[i].id, target: nodes[j].id, color: nodes[i].color }); } } });
                      return links.map((link, idx) => {
                        const sn = dynamicNodes.find(p => p.id === link.source), tn = dynamicNodes.find(p => p.id === link.target);
                        if (!sn || !tn) return null;
                        const h = hoveredNodeId === null || hoveredNodeId === link.source || hoveredNodeId === link.target;
                        return <line key={idx} x1={sn.x} y1={sn.y} x2={tn.x} y2={tn.y} stroke={link.color} strokeWidth={h ? 2.5 : 1} strokeOpacity={h ? 0.75 : 0.08} className="transition-all duration-300" />;
                      });
                    })()}
                    {dynamicNodes.map(node => {
                      const h = hoveredNodeId === null || hoveredNodeId === node.id;
                      return (
                        <g key={node.id} className="cursor-pointer transition-all duration-300" style={{ opacity: h ? 1 : 0.25 }}
                          onMouseEnter={() => setHoveredNodeId(node.id)}
                          onMouseLeave={() => setHoveredNodeId(null)}
                          onClick={() => {
                            const mp = papers.find(p => p.id === node.id);
                            if (mp) { setSelectedPaper(mp); loadCode(mp.content || mp.extractedText || ''); showToast(`Switched to: ${node.title}`); }
                          }}>
                          <circle cx={node.x} cy={node.y} r={16} fill="#1e293b" stroke={node.color} strokeWidth={hoveredNodeId === node.id ? 3.5 : 2} className="transition-all duration-300" />
                          <text x={node.x} y={node.y + 4} textAnchor="middle" fill="#f8fafc" fontSize="11px" fontWeight="bold" fontFamily="sans-serif">{node.num}</text>
                        </g>
                      );
                    })}
                  </svg>
                  <div className="absolute bottom-2 left-2 right-2 bg-slate-900/95 border border-slate-800 rounded-md p-1.5 flex justify-between text-[9px] text-slate-400">
                    <div className="flex items-center gap-1"><span className="w-1.5 h-1.5 rounded-full bg-[#38bdf8]"></span> ReactJS</div>
                    <div className="flex items-center gap-1"><span className="w-1.5 h-1.5 rounded-full bg-[#10b981]"></span> DevOps</div>
                    <div className="flex items-center gap-1"><span className="w-1.5 h-1.5 rounded-full bg-[#ec4899]"></span> Microservices</div>
                  </div>
                </div>
              </div>
              {graphData && graphData.claims && graphData.claims.length > 0 ? (
                <div className="space-y-4">
                  <div className="bg-slate-900 rounded-xl p-4 border border-slate-800 text-slate-200">
                    <div className="flex justify-between items-center mb-3">
                      <h4 className="font-bold text-xs text-indigo-400 flex items-center gap-1">
                        <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 002-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" /></svg>
                        Source-claim Network
                      </h4>
                      <div className="flex items-center gap-2">
                        <button onClick={handleExportCsv} className="text-[9px] text-emerald-400 hover:text-emerald-300 flex items-center gap-1 font-medium" title="Export CSV">
                          <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" /></svg>
                          CSV
                        </button>
                        <button onClick={handleExportJson} className="text-[9px] text-indigo-400 hover:text-indigo-300 flex items-center gap-1 font-medium" title="Export JSON">
                          <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" /></svg>
                          JSON
                        </button>
                      </div>
                    </div>
                    <svg width="100%" viewBox={`0 0 600 ${Math.max(graphData.claims.length, graphData.sources?.length || 0) * 80 + 100}`} className="overflow-visible">
                      {graphData.claims.map((c, ci) => (c.graphData?.matched_source_ids || []).map(sid => {
                        const si = (graphData.sources || []).findIndex(s => s.id === sid);
                        if (si < 0) return null;
                        const y1 = ci * 80 + 50, y2 = si * 80 + 50;
                        const color = c.graphData?.verdict === 'SUPPORTED' ? '#34d399' : c.graphData?.verdict === 'REFUTED' ? '#fb7185' : '#fbbf24';
                        return <path key={`e-${ci}-${si}`} d={`M 150 ${y1} Q 300 ${(y1 + y2) / 2}, 450 ${y2}`} stroke={color} strokeWidth="1.5" fill="none" opacity="0.5" />;
                      }))}
                      {graphData.claims.map((c, ci) => {
                        const verdict = c.graphData?.verdict;
                        const bc = verdict === 'SUPPORTED' ? '#34d399' : verdict === 'REFUTED' ? '#fb7185' : verdict ? '#fbbf24' : '#334155';
                        return (
                          <g key={`c-${ci}`} onClick={() => { setSelectedPaper(c); handleFetchMatches(c.id); }} style={{ cursor: 'pointer' }}>
                            <rect x="10" y={ci * 80 + 10} width="140" height="80" rx="8" fill="#1e293b" stroke={bc} strokeWidth="1.5" />
                            <foreignObject x="15" y={ci * 80 + 15} width="130" height="45">
                              <div style={{ color: '#e2e8f0', fontSize: '10px', lineHeight: '1.3', overflow: 'hidden' }}>{c.content}</div>
                            </foreignObject>
                            <text x="80" y={ci * 80 + 75} fill={bc} fontSize="9" textAnchor="middle" fontWeight="bold">{verdict || 'Unanalyzed'}{c.graphData?.confidence ? ` (${(c.graphData.confidence * 100).toFixed(0)}%)` : ''}</text>
                          </g>
                        );
                      })}
                      {(graphData.sources || []).map((s, si) => (
                        <g key={`s-${si}`}>
                          <rect x="450" y={si * 80 + 10} width="140" height="80" rx="8" fill="#1e293b" stroke="#475569" />
                          <text x="520" y={si * 80 + 35} fill="#94a3b8" fontSize="9" textAnchor="middle">{s.filename?.slice(0, 22)}</text>
                          <text x="520" y={si * 80 + 55} fill="#64748b" fontSize="8" textAnchor="middle">Cited</text>
                          <text x="520" y={si * 80 + 72} fill="#6366f1" fontSize="10" textAnchor="middle" fontWeight="bold">{s.referenceCount} times</text>
                        </g>
                      ))}
                    </svg>
                    <div className="flex gap-4 mt-3 pt-2 border-t border-slate-800">
                      <div className="flex items-center gap-1.5 text-[10px]"><div className="w-3 h-0.5 rounded bg-emerald-400" /><span className="text-slate-400">SUPPORTED</span></div>
                      <div className="flex items-center gap-1.5 text-[10px]"><div className="w-3 h-0.5 rounded bg-amber-400" /><span className="text-slate-400">NEUTRAL</span></div>
                      <div className="flex items-center gap-1.5 text-[10px]"><div className="w-3 h-0.5 rounded bg-rose-400" /><span className="text-slate-400">REFUTED</span></div>
                    </div>
                  </div>
                  <div className="bg-white border border-slate-200 rounded-xl p-4 shadow-sm">
                    <h4 className="font-bold text-xs text-slate-700 mb-3">Connection Details</h4>
                    {loadingMatches ? <div className="text-xs text-slate-400 italic">Loading...</div> : claimMatches.length === 0 ? <div className="text-xs text-slate-400 italic">No connections.</div> : (
                      <div className="space-y-2 max-h-60 overflow-y-auto">
                        {claimMatches.map((m, i) => (
                          <div key={i} className="flex items-start gap-2 p-2 bg-slate-50 rounded-lg text-xs">
                            <div className={`mt-1 w-2 h-2 rounded-full shrink-0 ${m.score >= 0.7 ? 'bg-emerald-400' : m.score >= 0.4 ? 'bg-amber-400' : 'bg-rose-400'}`} />
                            <div className="flex-1 min-w-0">
                              <div className="font-medium text-slate-700">{m.filename}{m.page ? ` (p.${m.page})` : ''}</div>
                              <div className="text-slate-500 text-[10px] mt-0.5 line-clamp-2">"{m.excerpt}"</div>
                              {m.explanation && <div className="text-indigo-600 text-[10px] mt-0.5 italic">{m.explanation}</div>}
                            </div>
                            <span className="text-[10px] font-bold text-slate-500 shrink-0">{(m.score * 100).toFixed(0)}%</span>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                  <div className="bg-white border border-slate-200 rounded-xl p-4 shadow-sm">
                    <h4 className="font-bold text-xs text-slate-700 mb-3">Source Summary</h4>
                    <div className="space-y-2">
                      {graphData.sources?.map((s, i) => (
                        <div key={i} className="flex justify-between items-center text-xs p-2 bg-slate-50 border border-slate-100 rounded-lg">
                          <span className="truncate max-w-[200px] font-medium text-slate-700 flex items-center gap-1"><svg className="w-3.5 h-3.5 text-indigo-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" /></svg>{s.filename}</span>
                          <span className="text-[10px] text-slate-500 bg-slate-200/60 px-2 py-0.5 rounded-full font-bold">{s.referenceCount} Citations</span>
                        </div>
                      ))}
                    </div>
                  </div>
                </div>
              ) : (
                <div className="text-xs text-slate-400 italic text-center py-8">No graph data yet. Use "AI Analyze" on your claims.</div>
              )}
            </div>
          )}
        </div>
      </aside>
    </>
  );
}
