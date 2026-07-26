import React from 'react';
import { UI_TEXT } from '../../constants/uiText';

export default function EditorPanel({
  selectedPaper, displayContent, updateCode, codeHistory, historyIndex,
  editorMode, setEditorMode, editorWidth, onEditorResizeStart,
  saveStatus, lastSaved, handleSaveDraft, handleRunAiReview,
  insertLatexTag, insertSymbol, handleUndo, handleRedo, handleFindReplace, handleDownloadTex,
  showSymbolMenu, setShowSymbolMenu, showTextSizeMenu, setShowTextSizeMenu,
  showSearchPanel, setShowSearchPanel, searchQuery, setSearchQuery, replaceQuery, setReplaceQuery,
  textSize, setTextSize, preRef, generateRichTextHtml, parseHtmlToLatex, showToast, language
}) {
  const t = UI_TEXT[language];
  return (
    <div id="editor-preview-container" className="flex-1 flex overflow-hidden bg-slate-200/50 p-2 gap-2">
      <div style={{ width: `${editorWidth}%`, flexGrow: 0, flexShrink: 0 }} className="bg-white rounded-lg shadow-sm border border-slate-200 flex flex-col overflow-hidden">
        <div className="h-10 border-b border-slate-100 flex items-center justify-between px-3 bg-white shadow-sm shrink-0 z-10">
          <div className="flex items-center gap-2 truncate">
            <span className="text-[10px] font-bold text-indigo-600 bg-indigo-50 px-1.5 py-0.5 rounded tracking-wide font-mono">LaTeX</span>
            <span className="text-xs font-bold text-slate-700 truncate">{selectedPaper ? selectedPaper.originalFilename : 'document.tex'}</span>
          </div>
          <div className="flex items-center gap-3">
            {selectedPaper && (
              <button onClick={handleRunAiReview} className="bg-indigo-600 hover:bg-indigo-700 text-white px-2.5 py-1 rounded-md text-xs font-bold flex items-center gap-1 shadow-sm transition-colors animate-pulse" title="AI Review paper structure and style">
                <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 01-2 2h0a2 2 0 01-2-2v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" /></svg>
                {t.aiReview}
              </button>
            )}
            <button onClick={handleSaveDraft} disabled={saveStatus === 'saving'} className={`flex items-center gap-1 px-2.5 py-1 rounded-md text-xs font-bold transition-colors disabled:opacity-50 ${saveStatus === 'saving' ? 'bg-amber-100 text-amber-700' : saveStatus === 'saved' ? 'bg-emerald-100 text-emerald-700' : saveStatus === 'error' ? 'bg-rose-100 text-rose-700' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'}`}>
              <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M8 7H5a2 2 0 00-2 2v9a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-3m-1 4l-3 3m0 0l-3-3m3 3V4" /></svg>
              {saveStatus === 'saving' ? 'Saving...' : saveStatus === 'saved' ? 'Saved' : saveStatus === 'error' ? 'Error' : 'Save'}
              {lastSaved && saveStatus !== 'saving' && <span className="text-[9px] opacity-60 ml-0.5">{lastSaved.toLocaleTimeString()}</span>}
            </button>
            <div className="flex bg-slate-100 rounded-lg p-0.5 border border-slate-200">
              <button onClick={() => setEditorMode('Code')} className={`px-2.5 py-0.5 rounded-md text-xs font-bold transition-colors ${editorMode === 'Code' ? 'bg-white shadow-sm text-slate-800' : 'text-slate-500 hover:text-slate-700'}`}>Code</button>
              <button onClick={() => setEditorMode('Rich Text')} className={`px-2.5 py-0.5 rounded-md text-xs font-bold transition-colors ${editorMode === 'Rich Text' ? 'bg-white shadow-sm text-slate-800' : 'text-slate-500 hover:text-slate-700'}`}>Visual</button>
            </div>
          </div>
        </div>
        <div className="bg-slate-50 border-b border-slate-200 flex flex-col shrink-0 select-none">
          <div className="h-9 flex items-center justify-between px-3 border-b border-slate-100 gap-1">
            <div className="flex-1 flex items-center gap-1 overflow-x-auto min-w-0 pr-2">
              <button onClick={handleUndo} disabled={historyIndex <= 0} className="w-7 h-7 flex items-center justify-center hover:bg-slate-200 rounded text-slate-600 disabled:text-slate-300 disabled:hover:bg-transparent transition-colors cursor-pointer" title="Undo"><span className="text-xs">↶</span></button>
              <button onClick={handleRedo} disabled={historyIndex >= codeHistory.length - 1} className="w-7 h-7 flex items-center justify-center hover:bg-slate-200 rounded text-slate-600 disabled:text-slate-300 disabled:hover:bg-transparent transition-colors cursor-pointer" title="Redo"><span className="text-xs">↷</span></button>
              <div className="w-px h-4 bg-slate-200 mx-1"></div>
              <div className="relative">
                <button onClick={() => { setShowTextSizeMenu(!showTextSizeMenu); setShowSymbolMenu(false); }} className="h-7 px-1.5 flex items-center gap-1 hover:bg-slate-200 rounded text-slate-700 font-extrabold text-[11px] transition-colors cursor-pointer" title="Heading / Font size">
                  <span>TT</span><span className="text-[7px]">▼</span>
                </button>
                {showTextSizeMenu && (
                  <div className="absolute left-0 mt-1 bg-white border border-slate-200 rounded-lg shadow-xl py-1 w-32 z-50 animate-in fade-in duration-105">
                    <button onClick={() => { insertLatexTag('section'); setShowTextSizeMenu(false); }} className="w-full text-left px-3 py-1.5 hover:bg-slate-100 text-xs font-bold text-slate-700 cursor-pointer">Section</button>
                    <button onClick={() => { insertLatexTag('subsection'); setShowTextSizeMenu(false); }} className="w-full text-left px-3 py-1.5 hover:bg-slate-100 text-xs font-semibold text-slate-700 cursor-pointer">Sub-section</button>
                    <button onClick={() => { insertLatexTag('subsubsection'); setShowTextSizeMenu(false); }} className="w-full text-left px-3 py-1.5 hover:bg-slate-100 text-xs text-slate-700 cursor-pointer">Sub-sub-section</button>
                    <hr className="border-slate-150 my-1" />
                    <button onClick={() => { insertLatexTag('large'); setShowTextSizeMenu(false); }} className="w-full text-left px-3 py-1.5 hover:bg-slate-100 text-xs text-slate-700 cursor-pointer">Large font</button>
                    <button onClick={() => { insertLatexTag('small'); setShowTextSizeMenu(false); }} className="w-full text-left px-3 py-1.5 hover:bg-slate-100 text-[10px] text-slate-700 cursor-pointer">Small font</button>
                  </div>
                )}
              </div>
              <button onClick={() => insertLatexTag('bold')} className="w-7 h-7 flex items-center justify-center hover:bg-slate-200 rounded text-slate-700 font-extrabold font-serif cursor-pointer font-bold" title="Bold">B</button>
              <button onClick={() => insertLatexTag('italic')} className="w-7 h-7 flex items-center justify-center hover:bg-slate-200 rounded text-slate-700 italic font-serif cursor-pointer" title="Italic">I</button>
              <button onClick={() => insertLatexTag('hl')} className="w-7 h-7 flex items-center justify-center hover:bg-slate-200 rounded text-amber-600 font-bold cursor-pointer" title="Highlight">Hl</button>
              <button onClick={() => insertLatexTag('inline-math')} className="w-7 h-7 flex items-center justify-center hover:bg-slate-200 rounded text-slate-700 font-serif text-xs cursor-pointer" title="Inline math ($)">$</button>
              <button onClick={() => insertLatexTag('equation')} className="w-7 h-7 flex items-center justify-center hover:bg-slate-200 rounded text-slate-700 font-serif text-xs cursor-pointer" title="Equation block">∑</button>
              <div className="relative">
                <button onClick={() => { setShowSymbolMenu(!showSymbolMenu); setShowTextSizeMenu(false); }} className="w-7 h-7 flex items-center justify-center hover:bg-slate-200 rounded text-slate-700 font-bold cursor-pointer" title="Greek symbols">Ω</button>
                {showSymbolMenu && (
                  <div className="absolute left-0 mt-1 bg-white border border-slate-200 rounded-lg shadow-xl p-2 w-48 z-50 animate-in fade-in duration-105">
                    <div className="grid grid-cols-4 gap-1">
                      {[{ code: '\\alpha', char: 'α' }, { code: '\\beta', char: 'β' }, { code: '\\gamma', char: 'γ' }, { code: '\\delta', char: 'δ' }, { code: '\\epsilon', char: 'ε' }, { code: '\\theta', char: 'θ' }, { code: '\\lambda', char: 'λ' }, { code: '\\pi', char: 'π' }, { code: '\\omega', char: 'ω' }, { code: '\\sigma', char: 'σ' }, { code: '\\infty', char: '∞' }, { code: '\\pm', char: '±' }, { code: '\\approx', char: '≈' }, { code: '\\neq', char: '≠' }, { code: '\\le', char: '≤' }, { code: '\\ge', char: '≥' }].map(sym => (
                        <button key={sym.code} onClick={() => { insertSymbol(sym.code); setShowSymbolMenu(false); }} className="h-7 hover:bg-slate-100 rounded text-xs font-semibold text-slate-700 flex items-center justify-center cursor-pointer hover:text-indigo-600" title={sym.code}>{sym.char}</button>
                      ))}
                    </div>
                  </div>
                )}
              </div>
              <div className="w-px h-4 bg-slate-200 mx-1"></div>
              <button onClick={() => insertLatexTag('link')} className="w-7 h-7 flex items-center justify-center hover:bg-slate-200 rounded text-slate-700 cursor-pointer" title="Insert link">
                <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1" /></svg>
              </button>
              <button onClick={() => insertLatexTag('comment')} className="w-7 h-7 flex items-center justify-center hover:bg-slate-200 rounded text-slate-700 cursor-pointer" title="Insert comment">
                <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" /></svg>
              </button>
              <button onClick={() => insertLatexTag('label')} className="w-7 h-7 flex items-center justify-center hover:bg-slate-200 rounded text-slate-700 cursor-pointer" title="Insert label">
                <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M7 7h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" /></svg>
              </button>
              <button onClick={() => insertLatexTag('cite')} className="w-7 h-7 flex items-center justify-center hover:bg-slate-200 rounded text-slate-700 cursor-pointer" title="Insert citation">
                <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" /></svg>
              </button>
              <button onClick={() => insertLatexTag('figure')} className="w-7 h-7 flex items-center justify-center hover:bg-slate-200 rounded text-slate-700 cursor-pointer" title="Insert figure">
                <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" /></svg>
              </button>
              <button onClick={() => insertLatexTag('table')} className="w-7 h-7 flex items-center justify-center hover:bg-slate-200 rounded text-slate-700 cursor-pointer" title="Insert table">
                <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3 10h18M3 14h18m-9-4v8m-7 0h14a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z" /></svg>
              </button>
            </div>
            <div className="flex items-center gap-1.5">
              <button onClick={() => setShowSearchPanel(!showSearchPanel)} className={`w-7 h-7 flex items-center justify-center rounded transition-colors ${showSearchPanel ? 'bg-indigo-100 text-indigo-700' : 'hover:bg-slate-200 text-slate-700'}`} title="Find & Replace">
                <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" /></svg>
              </button>
            </div>
          </div>
          <div className="h-8 flex items-center justify-between px-3 bg-slate-50/70 border-t border-slate-100 gap-1">
            <div className="flex items-center gap-1.5">
              <span className="text-[9px] text-slate-400 font-extrabold tracking-wider">TEXT SIZE:</span>
              <input type="range" min="10" max="24" value={textSize} onChange={(e) => setTextSize(parseInt(e.target.value))} className="w-16 h-1 bg-slate-200 rounded-lg appearance-none cursor-pointer accent-indigo-600" title="Editor font size" />
              <span className="text-[10px] text-slate-500 font-mono font-bold">{textSize}px</span>
            </div>
            <button onClick={handleDownloadTex} className="text-[10px] font-bold text-indigo-600 hover:text-indigo-850 flex items-center gap-1 cursor-pointer" title="Download .tex file">
              <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" /></svg>
              Download .tex
            </button>
          </div>
          {showSearchPanel && (
            <div className="bg-slate-100 border-t border-slate-200 p-2 flex flex-col gap-2 animate-in slide-in-from-top duration-200">
              <div className="flex items-center gap-2">
                <input type="text" placeholder="Search..." value={searchQuery} onChange={(e) => setSearchQuery(e.target.value)} className="flex-1 bg-white border border-slate-200 rounded px-2 py-1 text-xs outline-none focus:border-indigo-400 font-mono" />
                <input type="text" placeholder="Replace with..." value={replaceQuery} onChange={(e) => setReplaceQuery(e.target.value)} className="flex-1 bg-white border border-slate-200 rounded px-2 py-1 text-xs outline-none focus:border-indigo-400 font-mono" />
              </div>
              <div className="flex justify-end gap-2">
                <button onClick={() => handleFindReplace(false)} className="bg-white border border-slate-200 hover:bg-slate-50 text-slate-700 text-[10px] font-bold px-2 py-1 rounded cursor-pointer">Replace</button>
                <button onClick={() => handleFindReplace(true)} className="bg-indigo-600 hover:bg-indigo-700 text-white text-[10px] font-bold px-2 py-1 rounded cursor-pointer shadow-sm">Replace All</button>
              </div>
            </div>
          )}
        </div>
        {editorMode === 'Code' ? (
          <div className="relative flex-1 bg-[#0d1117] overflow-hidden group">
            <textarea id="latex-textarea" value={displayContent} onChange={(e) => updateCode(e.target.value)} onScroll={(e) => { if (preRef.current) { preRef.current.scrollTop = e.target.scrollTop; preRef.current.scrollLeft = e.target.scrollLeft; } }} style={{ fontSize: `${textSize}px` }} spellCheck={false} className="absolute inset-0 w-full h-full bg-transparent text-transparent caret-white resize-none outline-none z-10 m-0 border-0 font-mono p-5 whitespace-pre-wrap break-words leading-relaxed overflow-auto custom-scrollbar" />
            <pre ref={preRef} style={{ fontSize: `${textSize}px` }} className="absolute inset-0 w-full h-full pointer-events-none text-slate-300 m-0 border-0 font-mono p-5 whitespace-pre-wrap break-words leading-relaxed overflow-auto custom-scrollbar" aria-hidden="true">
              {displayContent.split(/(\\[a-zA-Z]+|\{[^{}]*\})/g).map((part, j) => {
                if (!part) return null;
                if (part.startsWith('\\')) return <span key={j} className="text-[#ff7b72]">{part}</span>;
                if (part.startsWith('{') && part.endsWith('}')) return <span key={j} className="text-[#a5d6ff]"><span className="text-slate-400">{'{'}</span>{part.slice(1, -1)}<span className="text-slate-400">{'}'}</span></span>;
                return <span key={j} className="text-slate-100">{part}</span>;
              })}
            </pre>
          </div>
        ) : (
          <div className="flex-1 bg-white text-slate-800 p-8 overflow-y-auto leading-relaxed custom-scrollbar selection:bg-indigo-100 outline-none" contentEditable={true} suppressContentEditableWarning={true} onInput={(e) => {
            const html = generateRichTextHtml(displayContent);
            const newCode = parseHtmlToLatex(e.target);
            updateCode(newCode);
          }} dangerouslySetInnerHTML={{ __html: generateRichTextHtml(displayContent) }} />
        )}
      </div>
      <div onMouseDown={onEditorResizeStart} className="w-1.5 hover:bg-indigo-500 cursor-col-resize self-stretch transition-all shrink-0 z-30 relative group flex items-center justify-center border-l border-r border-slate-200" title="Drag to resize">
        <div className="h-6 w-0.5 bg-slate-300 group-hover:bg-indigo-500 rounded"></div>
      </div>
      <div style={{ width: `${100 - editorWidth}%`, flexGrow: 0, flexShrink: 0 }} className="bg-white rounded-xl shadow-sm border border-slate-200 flex flex-col overflow-hidden">
        <div className="h-11 border-b border-slate-100 flex items-center justify-between px-4 bg-white">
          <div className="flex items-center gap-2 text-sm font-bold text-slate-700">
            <svg className="w-4 h-4 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" /><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" /></svg>
            PDF Preview
          </div>
          <button onClick={() => showToast('Recompiling PDF...')} className="bg-emerald-500 hover:bg-emerald-600 text-white px-3 py-1.5 rounded-lg text-xs font-semibold flex items-center gap-1.5 shadow-sm shadow-emerald-500/20 transition-all hover:-translate-y-0.5">
            <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" /></svg>
            Recompile
          </button>
        </div>
        <div className="flex-1 bg-slate-100/50 p-6 overflow-y-auto flex justify-center custom-scrollbar">
          <PreviewInner content={displayContent} />
        </div>
      </div>
    </div>
  );
}

function PreviewInner({ content }) {
  const titleMatch = content.match(/\\title\{([^}]+)\}/);
  const authorMatch = content.match(/\\author\{([^}]+)\}/);
  let body = content.replace(/\\documentclass.*?\n/g, '').replace(/\\usepackage.*?\n/g, '').replace(/\\title\{.*?\}/g, '').replace(/\\author\{.*?\}/g, '').replace(/\\date\{.*?\}/g, '').replace(/\\begin\{document\}/g, '').replace(/\\end\{document\}/g, '').replace(/\\maketitle/g, '');
  const sections = body.split(/\\section\{([^}]+)\}/);
  const parsedElements = [];
  const parseText = (text) => {
    const parts = text.split(/\\hl\{([^}]+)\}/g);
    return parts.map((part, index) => index % 2 === 1 ? <span key={index} className="bg-yellow-200/80 px-1 rounded-sm border-b border-yellow-400">{part}</span> : part);
  };
  if (sections[0] && sections[0].trim()) parsedElements.push(<p key="intro" className="text-[14px] mb-8 leading-[1.8] text-slate-700 font-serif text-justify">{parseText(sections[0].trim())}</p>);
  for (let i = 1; i < sections.length; i += 2) {
    const st = sections[i], sc = sections[i + 1] || '';
    parsedElements.push(<h2 key={`h2-${i}`} className="font-bold text-lg mb-4 text-slate-800 font-serif">{Math.ceil(i / 2)}. {st}</h2>);
    sc.split('\n\n').filter(p => p.trim()).forEach((p, pIdx) => parsedElements.push(<p key={`p-${i}-${pIdx}`} className="text-[14px] mb-8 leading-[1.8] text-slate-700 font-serif text-justify">{parseText(p.trim())}</p>));
  }
  return (
    <div className="bg-white shadow-xl shadow-slate-200/50 ring-1 ring-slate-200 rounded-md w-full max-w-lg p-12 h-max min-h-[105%] transition-transform transform origin-top hover:scale-[1.01] duration-300">
      {titleMatch && <h1 className="text-2xl font-serif font-bold text-center mb-3 leading-snug text-slate-900">{titleMatch[1].split('\\\\').map((line, i) => <React.Fragment key={i}>{line}<br /></React.Fragment>)}</h1>}
      {authorMatch && <p className="text-center text-sm mb-10 text-slate-600 font-serif italic">{authorMatch[1]}</p>}
      {parsedElements}
    </div>
  );
}
