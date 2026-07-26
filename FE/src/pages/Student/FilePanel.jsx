import { UI_TEXT } from '../../constants/uiText';

export default function FilePanel({ isOpen, width, onResizeStart, papers, selectedPaper, onSelectPaper, onUploadPaper, onDeletePaper, sources, onUploadSource, onDeleteSource, showToast, language }) {
  const t = UI_TEXT[language];
  if (!isOpen) return null;
  return (
    <>
      <aside style={{ width }} className="bg-slate-50/50 border-r border-slate-200 flex flex-col shrink-0 z-10 backdrop-blur-sm relative">
        <div className="px-4 py-3 border-b border-slate-200 flex justify-between items-center bg-slate-100/40">
          <span className="text-[11px] font-bold text-slate-500 tracking-wider uppercase">{t.paperDrafts}</span>
          <label className="text-slate-400 hover:text-indigo-600 transition-colors cursor-pointer" title="Upload new draft">
            <input type="file" className="hidden" accept=".pdf,.docx" onChange={(e) => { if (e.target.files?.[0]) onUploadPaper(e.target.files[0]); }} />
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4v16m8-8H4" /></svg>
          </label>
        </div>
        <div className="p-2 flex-1 max-h-[45%] overflow-y-auto border-b border-slate-200 custom-scrollbar">
          {papers.length === 0 ? (
            <div className="text-xs text-slate-400 italic text-center py-4">No drafts uploaded.</div>
          ) : (
            papers.map(p => (
              <div key={p.id} onClick={() => onSelectPaper(p)} className={`flex items-center justify-between text-xs font-medium p-2 rounded-md cursor-pointer transition-all mt-1 group ${selectedPaper?.id === p.id ? 'bg-indigo-50 text-indigo-700 border border-indigo-100 shadow-sm' : 'text-slate-600 hover:bg-slate-100'}`}>
                <div className="flex items-center gap-2 truncate">
                  <svg className="w-3.5 h-3.5 shrink-0 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" /></svg>
                  <span className="truncate" title={p.originalFilename}>{p.originalFilename}</span>
                </div>
                <button onClick={(e) => { e.stopPropagation(); onDeletePaper(p.id); }} className="opacity-0 group-hover:opacity-100 hover:text-red-600 transition-all p-0.5" title="Delete draft">
                  <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" /></svg>
                </button>
              </div>
            ))
          )}
        </div>
        <div className="px-4 py-3 border-b border-slate-200 flex justify-between items-center bg-slate-100/40">
          <span className="text-[11px] font-bold text-slate-500 tracking-wider uppercase">{t.sources}</span>
          <label className="text-slate-400 hover:text-indigo-600 transition-colors cursor-pointer" title="Upload new source">
            <input type="file" className="hidden" accept=".pdf,.docx" onChange={(e) => { if (e.target.files?.[0]) onUploadSource(e.target.files[0]); }} />
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4v16m8-8H4" /></svg>
          </label>
        </div>
        <div className="p-2 flex-1 overflow-y-auto custom-scrollbar">
          {sources.length === 0 ? (
            <div className="text-xs text-slate-400 italic text-center py-4">No sources uploaded.</div>
          ) : (
            sources.map(src => (
              <div key={src.id} onClick={() => showToast(`Viewing: ${src.originalFilename}`)} className="flex items-center justify-between text-xs font-medium p-2 rounded-md hover:bg-slate-100 cursor-pointer transition-all mt-1 group text-slate-600">
                <div className="flex items-center gap-2 truncate">
                  <svg className="w-3.5 h-3.5 shrink-0 text-indigo-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" /></svg>
                  <span className="truncate" title={src.originalFilename}>{src.originalFilename}</span>
                </div>
                <button onClick={(e) => { e.stopPropagation(); onDeleteSource(src.id); }} className="opacity-0 group-hover:opacity-100 hover:text-red-600 transition-all p-0.5" title="Delete source">
                  <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" /></svg>
                </button>
              </div>
            ))
          )}
        </div>
      </aside>
      <div onMouseDown={onResizeStart} className="w-1 hover:w-1.5 bg-slate-200 hover:bg-slate-400 cursor-col-resize self-stretch transition-all shrink-0 z-30 relative group flex items-center justify-center border-r border-slate-200/80" title="Drag to resize">
        <div className="h-6 w-0.5 bg-slate-400 group-hover:bg-slate-500 rounded"></div>
      </div>
    </>
  );
}
