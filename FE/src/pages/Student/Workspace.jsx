import React, { useState, useRef, useEffect } from 'react';
import { Link, useParams, useNavigate } from 'react-router-dom';
import api from '../../api.js';

const RichTextEditor = React.memo(({ initialHtml, onHtmlChange }) => {
  return (
    <div 
      className="flex-1 bg-white text-slate-800 p-8 overflow-y-auto leading-relaxed custom-scrollbar selection:bg-indigo-100 outline-none"
      contentEditable={true}
      suppressContentEditableWarning={true}
      onInput={(e) => onHtmlChange(e.target)}
      dangerouslySetInnerHTML={{ __html: initialHtml }}
    />
  );
}, () => true);

export default function Workspace() {
  const { projectId } = useParams();
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState('Source');
  const [editorMode, setEditorMode] = useState('Code');
  const [showHistoryModal, setShowHistoryModal] = useState(false);
  const [showReviseModal, setShowReviseModal] = useState(false);
  const [toastMessage, setToastMessage] = useState('');

  // Backend state
  const [project, setProject] = useState(null);
  const [sources, setSources] = useState([]);
  const [claims, setClaims] = useState([]);
  const [feedbacks, setFeedbacks] = useState([]);
  const [graphData, setGraphData] = useState(null);
  const [loadingProject, setLoadingProject] = useState(false);

  const [codeContent, setCodeContent] = useState(`\\documentclass{article}
\\usepackage[utf-8]{inputenc}
\\usepackage{xcolor}
\\usepackage{soul}

\\title{Evidence Traceability in Modern Agile Environments}
\\author{Minh Nguyen}
\\date{\\today}

\\begin{document}

\\maketitle

\\section{Introduction}

Agile software development depends on fast communication between stakeholders, product owners, and delivery teams. However, project risk increases when feedback loops are informal or delayed.

\\section{Communication Protocols and Risk Reduction}

Clear communication protocols reduce project risk \\hl{because teams can identify blockers earlier and align decisions before sprint goals are missed}.

Risk management in agile projects should combine lightweight documentation, frequent review meetings, and traceable evidence for important project claims.

\\section{Addressing Common Assumptions}

Some teams assume daily meetings alone are enough to control project uncertainty, but this claim still needs stronger evidence from the uploaded sources.`);
  useEffect(() => {
    async function loadData() {
      try {
        setLoadingProject(true);
        let currentProjectId = projectId;
        
        // If no projectId in URL, try to fetch the first project
        if (!currentProjectId) {
          const res = await api.get('/api/projects');
          if (res.data && res.data.length > 0) {
            currentProjectId = res.data[0].id;
            // Optionally redirect to the project URL: navigate(`/student/workspace/${currentProjectId}`);
          }
        }

        if (currentProjectId) {
          // Fetch project details
          const projRes = await api.get(`/api/projects/${currentProjectId}`);
          setProject(projRes.data);

          // Fetch sources
          try {
            const srcRes = await api.get(`/api/projects/${currentProjectId}/sources`);
            setSources(srcRes.data);
          } catch (e) { console.error('Failed to fetch sources', e); }

          // Fetch claims
          try {
            const claimRes = await api.get(`/api/claims/by-project/${currentProjectId}`);
            setClaims(claimRes.data);
          } catch (e) { console.error('Failed to fetch claims', e); }

          // Fetch feedback
          // Note: FeedbackController has /api/feedback/feedback-requests.
          // We can fetch all and filter, or if there's a better endpoint we use that.
          try {
            const fbRes = await api.get('/api/feedback/feedback-requests');
            // Assuming the response is a list and has project info or we just use it as is
            setFeedbacks(fbRes.data);
          } catch (e) { console.error('Failed to fetch feedback', e); }

          // Fetch graph
          try {
            const graphRes = await api.get(`/api/traceability/${currentProjectId}/traceability-export`);
            setGraphData(graphRes.data?.graphData || graphRes.data);
          } catch (e) { console.error('Failed to fetch graph data', e); }
        }
      } catch (err) {
        console.error('Error loading project data:', err);
      } finally {
        setLoadingProject(false);
      }
    }

    loadData();
  }, [projectId]);

  const preRef = useRef(null);

  const handleScroll = (e) => {
    if (preRef.current) {
      preRef.current.scrollTop = e.target.scrollTop;
      preRef.current.scrollLeft = e.target.scrollLeft;
    }
  };

  const showToast = (msg) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(''), 3000);
  };

  const renderPreview = () => {
    const titleMatch = codeContent.match(/\\title\{([^}]+)\}/);
    const authorMatch = codeContent.match(/\\author\{([^}]+)\}/);
    
    let body = codeContent.replace(/\\documentclass.*?\n/g, '')
                        .replace(/\\usepackage.*?\n/g, '')
                        .replace(/\\title\{.*?\}/g, '')
                        .replace(/\\author\{.*?\}/g, '')
                        .replace(/\\date\{.*?\}/g, '')
                        .replace(/\\begin\{document\}/g, '')
                        .replace(/\\end\{document\}/g, '')
                        .replace(/\\maketitle/g, '');

    const sections = body.split(/\\section\{([^}]+)\}/);
    const parsedElements = [];
    
    const parseText = (text) => {
       const parts = text.split(/\\hl\{([^}]+)\}/g);
       return parts.map((part, index) => {
          if (index % 2 === 1) {
             return <span key={index} className="bg-yellow-200/80 px-1 rounded-sm border-b border-yellow-400">{part}</span>;
          }
          return part;
       });
    };

    if (sections[0] && sections[0].trim()) {
       parsedElements.push(<p key="intro" className="text-[14px] mb-8 leading-[1.8] text-slate-700 font-serif text-justify">{parseText(sections[0].trim())}</p>);
    }

    for (let i = 1; i < sections.length; i += 2) {
       const sectionTitle = sections[i];
       const sectionContent = sections[i+1] || '';
       
       parsedElements.push(
         <h2 key={`h2-${i}`} className="font-bold text-lg mb-4 text-slate-800 font-serif">
            {Math.ceil(i/2)}. {sectionTitle}
         </h2>
       );
       
       const paragraphs = sectionContent.split('\n\n').filter(p => p.trim());
       paragraphs.forEach((p, pIndex) => {
          parsedElements.push(
             <p key={`p-${i}-${pIndex}`} className="text-[14px] mb-8 leading-[1.8] text-slate-700 font-serif text-justify">
                {parseText(p.trim())}
             </p>
          );
       });
    }

    return (
       <div className="bg-white shadow-xl shadow-slate-200/50 ring-1 ring-slate-200 rounded-md w-full max-w-lg p-12 h-max min-h-[105%] transition-transform transform origin-top hover:scale-[1.01] duration-300">
          {titleMatch && (
             <h1 className="text-2xl font-serif font-bold text-center mb-3 leading-snug text-slate-900">
                {titleMatch[1].split('\\\\').map((line, i) => <React.Fragment key={i}>{line}<br/></React.Fragment>)}
             </h1>
          )}
          {authorMatch && (
             <p className="text-center text-sm mb-10 text-slate-600 font-serif italic">{authorMatch[1]}</p>
          )}
          {parsedElements}
       </div>
    );
  };

  const generateRichTextHtml = (latexCode) => {
    const titleMatch = latexCode.match(/\\title\{([^}]+)\}/);
    const authorMatch = latexCode.match(/\\author\{([^}]+)\}/);
    
    let body = latexCode.replace(/\\documentclass.*?\n/g, '')
                        .replace(/\\usepackage.*?\n/g, '')
                        .replace(/\\title\{.*?\}/g, '')
                        .replace(/\\author\{.*?\}/g, '')
                        .replace(/\\date\{.*?\}/g, '')
                        .replace(/\\begin\{document\}/g, '')
                        .replace(/\\end\{document\}/g, '')
                        .replace(/\\maketitle/g, '');

    const sections = body.split(/\\section\{([^}]+)\}/);
    let html = '';

    if (titleMatch) {
       html += `<h1 class="text-3xl font-bold mb-2 text-slate-900">${titleMatch[1].replace(/\\\\/g, ' ')}</h1>`;
    }
    if (authorMatch) {
       html += `<p class="text-sm text-slate-500 mb-8 italic">By ${authorMatch[1]}</p>`;
    }

    const parseText = (text) => {
       let parsed = text;
       parsed = parsed.replace(/\\hl\{([^}]+)\}/g, '<span class="bg-yellow-200/50 px-1.5 rounded text-slate-800 border-b border-yellow-300">$1</span>');
       return parsed;
    };

    if (sections[0] && sections[0].trim()) {
       html += `<p class="mb-6 text-[15px] text-slate-700">${parseText(sections[0].trim())}</p>`;
    }

    for (let i = 1; i < sections.length; i += 2) {
       const sectionTitle = sections[i];
       const sectionContent = sections[i+1] || '';
       
       html += `<h2 class="text-xl font-bold mb-3 text-slate-800">${sectionTitle}</h2>`;
       
       const paragraphs = sectionContent.split('\n\n').filter(p => p.trim());
       paragraphs.forEach(p => {
          html += `<p class="mb-6 text-[15px] text-slate-700">${parseText(p.trim())}</p>`;
       });
    }

    return html;
  };

  const parseHtmlToLatex = (container) => {
    let newLatex = `\\documentclass{article}\n\\usepackage[utf-8]{inputenc}\n\\usepackage{xcolor}\n\\usepackage{soul}\n\n`;
    Array.from(container.children).forEach(child => {
       if (child.tagName === 'H1') {
          newLatex += `\\title{${child.innerText}}\n`;
       } else if (child.tagName === 'P' && child.innerText.startsWith('By ')) {
          newLatex += `\\author{${child.innerText.substring(3)}}\n\\date{\\today}\n\n\\begin{document}\n\n\\maketitle\n\n`;
       } else if (child.tagName === 'H2') {
          newLatex += `\\section{${child.innerText}}\n\n`;
       } else if (child.tagName === 'P') {
          let text = child.innerHTML.replace(/<span[^>]*>(.*?)<\/span>/g, '\\hl{$1}').replace(/&nbsp;/g, ' ');
          text = text.replace(/<br\s*\/?>/gi, '\n');
          text = text.replace(/<[^>]*>?/gm, '');
          if (text.trim()) newLatex += `${text}\n\n`;
       }
    });
    return newLatex.trim();
  };

  return (
    <div className="h-screen w-full flex flex-col bg-slate-50 overflow-hidden font-sans antialiased text-slate-800">
      {/* Top Navigation Bar */}
      <header className="h-14 border-b border-slate-200 bg-white/80 backdrop-blur-md flex items-center justify-between px-4 shrink-0 shadow-sm z-20">
        <div className="flex items-center gap-4">
          <Link to="/" className="p-1.5 hover:bg-slate-100 rounded-lg text-slate-500 transition-colors">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
            </svg>
          </Link>
          <div className="flex items-center gap-3">
            <div className="w-7 h-7 bg-indigo-600 text-white rounded-md text-xs flex items-center justify-center font-bold shadow-sm shadow-indigo-200">EP</div>
            <h1 className="font-semibold text-slate-800 text-sm truncate max-w-[300px] xl:max-w-md">Evidence Traceability in Modern Agile Environments</h1>
          </div>
        </div>
        <div className="flex items-center gap-3">
          <div className="hidden md:block text-xs text-slate-400 mr-2 font-medium">Student workspace with returned Instructor feedback</div>
          <div className="flex gap-1.5 bg-rose-50 border border-rose-100 rounded-full px-1 py-1">
             <span className="text-[11px] px-2 py-0.5 text-rose-700 font-semibold rounded-full bg-white shadow-sm">Returned with Feedback</span>
             <span className="text-[11px] px-2 py-0.5 text-rose-700 font-semibold rounded-full bg-white shadow-sm flex items-center gap-1">
               <div className="w-1.5 h-1.5 rounded-full bg-rose-500 animate-pulse"></div>
               2 open feedback
             </span>
          </div>
          <button 
            onClick={() => setShowHistoryModal(true)}
            className="flex items-center gap-1.5 text-xs font-semibold text-slate-600 hover:text-slate-900 border border-slate-200 px-3 py-1.5 rounded-lg hover:bg-slate-50 transition-all shadow-sm ml-2">
            <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            History
          </button>
          <button 
            onClick={() => setShowReviseModal(true)}
            className="text-xs font-semibold text-white bg-indigo-600 hover:bg-indigo-700 px-4 py-1.5 rounded-lg flex items-center gap-1.5 shadow-md shadow-indigo-600/20 transition-all hover:shadow-indigo-600/40 transform hover:-translate-y-0.5">
            <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
            Revise
          </button>
        </div>
      </header>

      {/* Main Workspace Area */}
      <div className="flex-1 flex overflow-hidden">
        {/* Activity Bar (Branded style) */}
        <div className="w-14 bg-indigo-900 flex flex-col items-center py-4 shrink-0 z-20 border-r border-indigo-950 shadow-[2px_0_8px_-2px_rgba(0,0,0,0.2)]">
           {/* Active Icon (Files) */}
           <div className="w-full flex justify-center relative cursor-pointer mb-6 group">
              <div className="absolute left-0 top-0 bottom-0 w-1 bg-white rounded-r-md shadow-[0_0_8px_rgba(255,255,255,0.8)]"></div>
              <svg className="w-[22px] h-[22px] text-white transition-colors" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" /></svg>
           </div>
           {/* Inactive Icon (History) */}
           <div onClick={() => showToast('Global History opened')} className="w-full flex justify-center cursor-pointer mb-6 group relative">
              <div className="absolute left-0 top-0 bottom-0 w-1 bg-transparent group-hover:bg-indigo-400 rounded-r-md transition-colors"></div>
              <svg className="w-[22px] h-[22px] text-indigo-300 group-hover:text-white transition-colors" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
           </div>
           {/* Inactive Icon (Settings) */}
           <div onClick={() => showToast('Settings opened')} className="w-full flex justify-center cursor-pointer group relative">
              <div className="absolute left-0 top-0 bottom-0 w-1 bg-transparent group-hover:bg-indigo-400 rounded-r-md transition-colors"></div>
              <svg className="w-[22px] h-[22px] text-indigo-300 group-hover:text-white transition-colors" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" /><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" /></svg>
           </div>
        </div>

        {/* Left Sidebar: File Outline */}
        <aside className="w-56 bg-slate-50/50 border-r border-slate-200 flex flex-col shrink-0 z-10 backdrop-blur-sm">
          <div className="px-4 py-3 border-b border-slate-200 flex justify-between items-center">
             <span className="text-[11px] font-bold text-slate-500 tracking-wider uppercase">File Outline</span>
             <button onClick={() => showToast('Add new file')} className="text-slate-400 hover:text-indigo-600 transition-colors"><svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4v16m8-8H4"/></svg></button>
          </div>
          <div className="p-2 flex-1 overflow-y-auto">
            <div className="flex items-center gap-2.5 text-sm font-medium text-indigo-700 p-2 bg-indigo-50 rounded-md cursor-pointer transition-colors border border-indigo-100 shadow-sm">
              <svg className="w-4 h-4 text-indigo-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" /></svg>
              main.tex
            </div>
            <div onClick={() => showToast('File opened: references.bib')} className="flex items-center gap-2.5 text-sm font-medium text-slate-600 p-2 hover:bg-slate-100 rounded-md cursor-pointer transition-colors mt-0.5">
              <svg className="w-4 h-4 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" /></svg>
              references.bib
            </div>
            <div className="mt-3">
              <div onClick={() => showToast('Folder expanded')} className="flex items-center gap-2 text-sm font-medium text-slate-700 p-1.5 cursor-pointer hover:text-indigo-600 transition-colors group">
                <svg className="w-3 h-3 text-slate-400 group-hover:text-indigo-500 transition-colors" fill="currentColor" viewBox="0 0 20 20"><path fillRule="evenodd" d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z" clipRule="evenodd"/></svg>
                <svg className="w-4 h-4 text-amber-400" fill="currentColor" viewBox="0 0 20 20"><path d="M2 6a2 2 0 012-2h5l2 2h5a2 2 0 012 2v6a2 2 0 01-2 2H4a2 2 0 01-2-2V6z"/></svg>
                figures
              </div>
              <div className="pl-6 text-sm text-slate-600 flex flex-col gap-0.5 mt-1 relative before:content-[''] before:absolute before:left-3.5 before:top-0 before:bottom-2 before:w-px before:bg-slate-200">
                <div onClick={() => showToast('Image opened: architecture.png')} className="flex items-center gap-2.5 p-1.5 hover:bg-slate-100 rounded-md cursor-pointer transition-colors relative z-10">
                  <div className="absolute left-[-11px] top-1/2 w-2.5 h-px bg-slate-200"></div>
                  <svg className="w-4 h-4 text-emerald-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" /></svg>
                  architecture.png
                </div>
                <div onClick={() => showToast('File opened: process-flow.pdf')} className="flex items-center gap-2.5 p-1.5 hover:bg-slate-100 rounded-md cursor-pointer transition-colors relative z-10">
                  <div className="absolute left-[-11px] top-1/2 w-2.5 h-px bg-slate-200"></div>
                  <svg className="w-4 h-4 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" /></svg>
                  process-flow.pdf
                </div>
              </div>
            </div>
          </div>
        </aside>

        {/* Center Panes: Editor & Preview */}
        <div className="flex-1 flex overflow-hidden bg-slate-200/50 p-3 gap-3">
          
          {/* Editor Pane */}
          <div className="flex-1 bg-white rounded-xl shadow-sm border border-slate-200 flex flex-col overflow-hidden">
            <div className="h-11 border-b border-slate-100 flex items-center justify-between px-3 bg-white">
               <div className="flex gap-1 border-r border-slate-200 pr-2">
                 <button onClick={() => showToast('Bold applied')} className="w-7 h-7 flex items-center justify-center hover:bg-slate-100 rounded-md text-slate-600 transition-colors"><span className="font-bold font-serif">B</span></button>
                 <button onClick={() => showToast('Italic applied')} className="w-7 h-7 flex items-center justify-center hover:bg-slate-100 rounded-md text-slate-600 transition-colors"><span className="italic font-serif">I</span></button>
               </div>
               <div className="flex gap-1 border-r border-slate-200 px-2">
                 <button onClick={() => showToast('List applied')} className="w-7 h-7 flex items-center justify-center hover:bg-slate-100 rounded-md text-slate-600 transition-colors"><svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 6h16M4 12h16M4 18h16" /></svg></button>
                 <button onClick={() => showToast('List applied')} className="w-7 h-7 flex items-center justify-center hover:bg-slate-100 rounded-md text-slate-600 transition-colors"><svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 6h16M4 10h16M4 14h16M4 18h16" /></svg></button>
               </div>
               <div className="flex gap-1 px-2 flex-1">
                 <button onClick={() => showToast('Equation editor')} className="w-7 h-7 flex items-center justify-center hover:bg-slate-100 rounded-md text-slate-600 font-serif text-sm transition-colors">∑</button>
                 <button onClick={() => showToast('Link inserted')} className="w-7 h-7 flex items-center justify-center hover:bg-slate-100 rounded-md text-slate-600 transition-colors"><svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1" /></svg></button>
               </div>
               <div className="flex items-center gap-4 text-xs font-medium text-slate-500">
                 <span onClick={() => showToast('Document saved')} className="flex items-center gap-1 cursor-pointer"><div className="w-1.5 h-1.5 rounded-full bg-emerald-500"></div> Saved</span>
                 <div className="flex bg-slate-100 rounded-lg p-0.5 border border-slate-200">
                   <button 
                     onClick={() => setEditorMode('Code')}
                     className={`px-3 py-1 rounded-md text-xs font-semibold transition-colors ${editorMode === 'Code' ? 'bg-white shadow-sm text-slate-800' : 'text-slate-500 hover:text-slate-700'}`}>
                     Code
                   </button>
                   <button 
                     onClick={() => setEditorMode('Rich Text')}
                     className={`px-3 py-1 rounded-md text-xs font-semibold transition-colors ${editorMode === 'Rich Text' ? 'bg-white shadow-sm text-slate-800' : 'text-slate-500 hover:text-slate-700'}`}>
                     Rich Text
                   </button>
                 </div>
               </div>
            </div>
             {/* Editor Area (Mock) */}
             {editorMode === 'Code' ? (
               <div className="relative flex-1 bg-[#0d1117] overflow-hidden group">
                 <textarea
                   value={codeContent}
                   onChange={(e) => setCodeContent(e.target.value)}
                   onScroll={(e) => {
                     if (preRef.current) {
                        preRef.current.scrollTop = e.target.scrollTop;
                        preRef.current.scrollLeft = e.target.scrollLeft;
                     }
                   }}
                   spellCheck={false}
                   className="absolute inset-0 w-full h-full bg-transparent text-transparent caret-white resize-none outline-none z-10 m-0 border-0 font-mono text-sm p-5 whitespace-pre-wrap break-words leading-relaxed overflow-auto custom-scrollbar"
                 />
                 <pre 
                   ref={preRef}
                   className="absolute inset-0 w-full h-full pointer-events-none text-slate-300 m-0 border-0 font-mono text-sm p-5 whitespace-pre-wrap break-words leading-relaxed overflow-auto custom-scrollbar" 
                   aria-hidden="true"
                 >
                   {codeContent.split(/(\\[a-zA-Z]+|\{[^{}]*\})/g).map((part, j) => {
                     if (!part) return null;
                     if (part.startsWith('\\')) return <span key={j} className="text-[#ff7b72]">{part}</span>;
                     if (part.startsWith('{') && part.endsWith('}')) {
                       return <span key={j} className="text-[#a5d6ff]">
                         <span className="text-slate-400">{'{'}</span>
                         {part.slice(1, -1)}
                         <span className="text-slate-400">{'}'}</span>
                       </span>;
                     }
                     return <span key={j} className="text-slate-100">{part}</span>;
                   })}
                 </pre>
               </div>
             ) : (
               <RichTextEditor 
                 initialHtml={generateRichTextHtml(codeContent)} 
                 onHtmlChange={(target) => {
                   const newCode = parseHtmlToLatex(target);
                   setCodeContent(newCode);
                 }}
               />
             )}
           </div>

          {/* PDF Preview Pane */}
          <div className="flex-1 bg-white rounded-xl shadow-sm border border-slate-200 flex flex-col overflow-hidden">
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
                {/* Mock Document */}
                {renderPreview()}
             </div>
          </div>
        </div>

        {/* Right Sidebar: Tools */}
        <aside className="w-[340px] bg-white border-l border-slate-200 flex flex-col shrink-0 shadow-[-4px_0_15px_-3px_rgba(0,0,0,0.05)] z-10">
           {/* Search */}
           <div className="p-4 border-b border-slate-100 bg-slate-50/50">
             <div className="relative group">
                <svg className="w-4 h-4 text-slate-400 absolute left-3.5 top-2.5 group-focus-within:text-indigo-500 transition-colors" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" /></svg>
                <input type="text" placeholder="Search Sources and Papers..." className="w-full pl-10 pr-4 py-2 bg-white border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 transition-all shadow-sm" />
             </div>
           </div>
           
           {/* Tabs */}
           <div className="flex border-b border-slate-200 bg-white relative">
             <button 
               onClick={() => setActiveTab('Source')}
               className={`flex-1 py-3.5 text-[10px] font-bold uppercase tracking-wider flex flex-col justify-center items-center gap-1 transition-all relative ${activeTab === 'Source' ? 'text-indigo-600' : 'text-slate-500 hover:text-slate-800 hover:bg-slate-50'}`}>
               <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" /></svg>
               Source
               {activeTab === 'Source' && <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-indigo-600 shadow-[0_-2px_8px_rgba(79,70,229,0.5)]"></div>}
             </button>
             <button 
               onClick={() => setActiveTab('Claims')}
               className={`flex-1 py-3.5 text-[10px] font-bold uppercase tracking-wider flex flex-col justify-center items-center gap-1 transition-all relative ${activeTab === 'Claims' ? 'text-indigo-600' : 'text-slate-500 hover:text-slate-800 hover:bg-slate-50'}`}>
               <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01" /></svg>
               Claims
               {activeTab === 'Claims' && <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-indigo-600 shadow-[0_-2px_8px_rgba(79,70,229,0.5)]"></div>}
             </button>
             <button 
               onClick={() => setActiveTab('Feedback')}
               className={`flex-1 py-3.5 text-[10px] font-bold uppercase tracking-wider flex flex-col justify-center items-center gap-1 transition-all relative ${activeTab === 'Feedback' ? 'text-indigo-600' : 'text-slate-500 hover:text-slate-800 hover:bg-slate-50'}`}>
               <div className="relative">
                 <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z" /></svg>
                 <span className="absolute -top-1.5 -right-2 w-3 h-3 bg-rose-500 text-white flex items-center justify-center text-[8px] rounded-full font-bold">2</span>
               </div>
               Feedback
               {activeTab === 'Feedback' && <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-indigo-600 shadow-[0_-2px_8px_rgba(79,70,229,0.5)]"></div>}
             </button>
             <button 
               onClick={() => setActiveTab('Graph')}
               className={`flex-1 py-3.5 text-[10px] font-bold uppercase tracking-wider flex flex-col justify-center items-center gap-1 transition-all relative ${activeTab === 'Graph' ? 'text-indigo-600' : 'text-slate-500 hover:text-slate-800 hover:bg-slate-50'}`}>
               <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M7 16V4m0 0L3 8m4-4l4 4m6 0v12m0 0l4-4m-4 4l-4-4" /></svg>
               Graph
               {activeTab === 'Graph' && <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-indigo-600 shadow-[0_-2px_8px_rgba(79,70,229,0.5)]"></div>}
             </button>
           </div>

           {/* Tab Content */}
           <div className="flex-1 overflow-y-auto bg-slate-50/50">
             
             {/* SOURCE TAB */}
             {activeTab === 'Source' && (
               <div className="p-5 flex flex-col gap-6 animate-in fade-in duration-300">
                  <label className="w-full flex justify-center items-center gap-2 border-2 border-dashed border-indigo-200 hover:border-indigo-400 bg-indigo-50/50 hover:bg-indigo-50 rounded-xl p-6 transition-all group mb-6 shadow-sm cursor-pointer">
                    <input 
                      type="file" 
                      className="hidden" 
                      accept=".pdf,.doc,.docx" 
                      onChange={async (e) => {
                        if (e.target.files && e.target.files.length > 0) {
                          const file = e.target.files[0];
                          showToast(`Uploading ${file.name}...`);
                          if (!project) {
                            showToast('No project selected to upload to.');
                            return;
                          }
                          const formData = new FormData();
                          formData.append('file', file);
                          formData.append('projectId', project.id);
                          try {
                            await api.post('/api/sources/upload', formData, {
                              headers: { 'Content-Type': 'multipart/form-data' }
                            });
                            showToast(`${file.name} uploaded successfully.`);
                            // Refresh sources
                            const srcRes = await api.get(`/api/projects/${project.id}/sources`);
                            setSources(srcRes.data);
                          } catch (err) {
                            console.error('Upload failed', err);
                            showToast(`Failed to upload ${file.name}`);
                          }
                        }
                      }} 
                    />
                    <div className="bg-white p-2 rounded-full shadow-sm group-hover:scale-110 transition-transform">
                      <svg className="w-5 h-5 text-indigo-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12" /></svg>
                    </div>
                    <span className="text-sm font-semibold text-indigo-700">Upload PDF / DOCX</span>
                  </label>

                  <div>
                     <h3 className="text-[11px] font-bold text-slate-400 tracking-widest mb-3 uppercase flex items-center gap-2">
                       <div className="h-px bg-slate-200 flex-1"></div> Shared Resources <div className="h-px bg-slate-200 flex-1"></div>
                     </h3>
                     <div className="bg-white border border-slate-200 rounded-xl shadow-sm mb-3 hover:border-indigo-300 hover:shadow-md transition-all overflow-hidden group">
                        <div className="p-3.5 border-b border-slate-100 flex justify-between items-start bg-slate-50/50">
                           <div>
                              <h4 className="font-bold text-sm text-slate-800 group-hover:text-indigo-700 transition-colors">Agile Risk Evidence Pack</h4>
                              <p className="text-xs text-slate-500 mt-1 line-clamp-2 leading-relaxed">Instructor-curated sources for communication, sprint feedback, and agile risk claims.</p>
                           </div>
                           <span className="bg-indigo-100 text-indigo-700 text-xs font-bold px-2 py-1 rounded-md shadow-sm">4</span>
                        </div>
                        <div className="p-0">
                           <div onClick={() => showToast('Opening resource: instructor-agile-risk-framework.pdf')} className="px-4 py-2.5 border-b border-slate-100 bg-white hover:bg-slate-50 transition-colors cursor-pointer">
                              <p className="text-sm font-semibold text-slate-700 flex items-center gap-2"><svg className="w-3.5 h-3.5 text-red-500" fill="currentColor" viewBox="0 0 20 20"><path fillRule="evenodd" d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4z" clipRule="evenodd"/></svg>instructor-agile-risk-framework.pdf</p>
                              <p className="text-[11px] text-slate-400 mt-1 pl-5">Risk control improves when agile teams define escalation paths...</p>
                           </div>
                           <div onClick={() => showToast('Opening resource: feedback-loop-benchmark.docx')} className="px-4 py-2.5 border-b border-slate-100 bg-white hover:bg-slate-50 transition-colors cursor-pointer">
                              <p className="text-sm font-semibold text-slate-700 flex items-center gap-2"><svg className="w-3.5 h-3.5 text-blue-500" fill="currentColor" viewBox="0 0 20 20"><path fillRule="evenodd" d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4z" clipRule="evenodd"/></svg>feedback-loop-benchmark.docx</p>
                              <p className="text-[11px] text-slate-400 mt-1 pl-5">Teams with structured sprint feedback loops identify blockers...</p>
                           </div>
                           <div onClick={() => showToast('Loading more sources...')} className="px-4 py-2 bg-slate-50/50 hover:bg-slate-100 text-[11px] text-indigo-600 font-bold text-center uppercase tracking-wider cursor-pointer transition-colors">
                              Show 2 more...
                           </div>
                        </div>
                     </div>
                  </div>

                  <div>
                     <h3 className="text-[11px] font-bold text-slate-400 tracking-widest mb-3 uppercase flex items-center gap-2">
                       <div className="h-px bg-slate-200 flex-1"></div> Uploaded Sources <div className="h-px bg-slate-200 flex-1"></div>
                     </h3>
                     <div className="flex flex-col gap-3">
                        {sources.length === 0 ? (
                          <div className="text-sm text-slate-500 italic text-center p-4">No uploaded sources yet.</div>
                        ) : (
                          sources.map(src => (
                            <div key={src.id} onClick={() => showToast(`Opening source: ${src.originalFilename}`)} className="bg-white border border-slate-200 rounded-xl p-3.5 hover:shadow-md hover:border-indigo-300 transition-all cursor-pointer transform hover:-translate-y-0.5">
                               <p className="text-sm font-bold text-slate-800 flex items-center gap-2"><svg className="w-4 h-4 text-red-500" fill="currentColor" viewBox="0 0 20 20"><path fillRule="evenodd" d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4z" clipRule="evenodd"/></svg>{src.originalFilename}</p>
                               <p className="text-xs text-slate-500 mt-1.5 line-clamp-2 leading-relaxed">Source file uploaded to this project.</p>
                            </div>
                          ))
                        )}
                     </div>
                  </div>
               </div>
             )}

             {/* CLAIMS TAB */}
             {activeTab === 'Claims' && (
               <div className="p-5 animate-in fade-in duration-300">
                  <h3 className="text-[11px] font-bold text-slate-400 tracking-widest mb-4 uppercase flex items-center gap-2">
                     <div className="h-px bg-slate-200 flex-1"></div> Extracted Claims <div className="h-px bg-slate-200 flex-1"></div>
                  </h3>
                  
                  <div className="flex flex-col gap-4">
                     {claims.length === 0 ? (
                       <div className="text-sm text-slate-500 italic text-center p-4">No extracted claims.</div>
                     ) : (
                       claims.map(claim => (
                         <div key={claim.id} onClick={() => showToast('Claim navigation')} className="bg-white border border-emerald-200 rounded-xl p-4 shadow-sm hover:shadow-md transition-shadow relative overflow-hidden group cursor-pointer">
                            <div className="absolute left-0 top-0 bottom-0 w-1 bg-emerald-500"></div>
                            <div className="flex justify-between items-start mb-2">
                               <span className="text-[10px] font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded border border-emerald-100 uppercase tracking-wider">{claim.status || 'Claim'}</span>
                            </div>
                            <p className="text-sm font-bold text-slate-800 mb-3 leading-snug">
                               {claim.content}
                            </p>
                            <button onClick={(e) => { e.stopPropagation(); showToast('Searching for evidence...'); }} className="w-full mt-2 py-1.5 bg-slate-50 hover:bg-indigo-50 border border-slate-200 hover:border-indigo-200 text-xs font-semibold text-slate-600 hover:text-indigo-600 rounded transition-colors">
                               Find Evidence
                            </button>
                         </div>
                       ))
                     )}
                  </div>
               </div>
             )}

             {/* FEEDBACK TAB */}
             {activeTab === 'Feedback' && (
               <div className="p-5 animate-in fade-in duration-300">
                  <h3 className="text-[11px] font-bold text-slate-400 tracking-widest mb-4 uppercase flex items-center gap-2">
                     <div className="h-px bg-slate-200 flex-1"></div> Instructor's Feedback <div className="h-px bg-slate-200 flex-1"></div>
                  </h3>
                  
                  <div className="flex flex-col gap-5">
                     {(!Array.isArray(feedbacks) || feedbacks.length === 0) ? (
                       <div className="text-sm text-slate-500 italic text-center p-4">No feedbacks available.</div>
                     ) : (
                       feedbacks.map((fb, idx) => (
                         <div key={fb.id || idx} className="bg-white border border-slate-200 rounded-xl shadow-sm overflow-hidden hover:shadow-md transition-shadow">
                            <div className="bg-emerald-50/50 border-b border-emerald-100 p-3 flex justify-between items-start">
                               <div className="flex items-center gap-2.5">
                                  <div className="w-8 h-8 rounded-full bg-emerald-100 text-emerald-700 flex items-center justify-center font-bold shadow-sm border border-emerald-200">I</div>
                                  <div>
                                     <p className="text-sm font-bold text-slate-800 leading-none mb-1">Instructor</p>
                                     <p className="text-[10px] text-slate-500 font-medium">{fb.createdAt || fb.requestedAt || ''}</p>
                                  </div>
                               </div>
                               <span className="bg-white border border-emerald-200 text-emerald-700 text-[10px] px-2 py-1 rounded-md font-bold uppercase tracking-wide shadow-sm">{fb.status || 'Feedback'}</span>
                            </div>
                            <div className="p-4">
                               <p className="text-[13px] text-slate-800 leading-relaxed">
                                  {fb.content || 'Instructor left a comment on your project.'}
                               </p>
                            </div>
                         </div>
                       ))
                     )}
                  </div>
               </div>
             )}

             {/* GRAPH TAB */}
             {activeTab === 'Graph' && (
               <div className="p-5 flex flex-col gap-5 animate-in fade-in duration-300">
                  {/* Graph Visual Mockup */}
                  <div className="bg-white border border-slate-200 rounded-xl p-4 shadow-sm">
                     <div className="flex justify-between items-center mb-4">
                        <div>
                           <h4 className="font-bold text-sm text-slate-800 flex items-center gap-2">
                              <svg className="w-4 h-4 text-indigo-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 002-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" /></svg>
                              Source Network
                           </h4>
                           <p className="text-[11px] text-slate-500 mt-1 font-medium">Visualizing connection density</p>
                        </div>
                     </div>
                     <div className="h-56 bg-slate-900 rounded-lg border border-slate-800 relative overflow-hidden flex items-center justify-center shadow-inner">
                        {/* Glow effect */}
                        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-32 h-32 bg-indigo-500/20 blur-2xl rounded-full"></div>
                        
                        {/* Mock Graph using absolutely positioned SVG and circles */}
                        <svg className="absolute inset-0 w-full h-full stroke-slate-700/50">
                           <line x1="50%" y1="50%" x2="30%" y2="20%" strokeWidth="1.5" />
                           <line x1="50%" y1="50%" x2="75%" y2="30%" strokeWidth="2" className="stroke-indigo-500/50" />
                           <line x1="50%" y1="50%" x2="80%" y2="60%" strokeWidth="1.5" />
                           <line x1="50%" y1="50%" x2="60%" y2="80%" strokeWidth="2" className="stroke-indigo-500/50" />
                           <line x1="50%" y1="50%" x2="30%" y2="70%" strokeWidth="1" />
                           <line x1="30%" y1="70%" x2="20%" y2="85%" strokeWidth="1" />
                           <line x1="60%" y1="80%" x2="80%" y2="85%" strokeWidth="1" />
                        </svg>
                        
                        {/* Nodes */}
                        <div className="absolute top-[20%] left-[30%] w-7 h-7 bg-slate-800 rounded-full border-2 border-slate-600 shadow-sm flex items-center justify-center -translate-x-1/2 -translate-y-1/2 hover:scale-110 transition-transform cursor-pointer"><svg className="w-3.5 h-3.5 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" /></svg></div>
                        <div className="absolute top-[30%] left-[75%] w-9 h-9 bg-indigo-500 rounded-full border-2 border-indigo-300 shadow-[0_0_15px_rgba(99,102,241,0.5)] flex items-center justify-center -translate-x-1/2 -translate-y-1/2 hover:scale-110 transition-transform cursor-pointer z-10"><svg className="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" /></svg></div>
                        <div className="absolute top-[60%] left-[80%] w-8 h-8 bg-slate-700 rounded-full border-2 border-slate-500 shadow-sm flex items-center justify-center -translate-x-1/2 -translate-y-1/2 hover:scale-110 transition-transform cursor-pointer"><svg className="w-4 h-4 text-slate-300" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" /></svg></div>
                        <div className="absolute top-[80%] left-[60%] w-9 h-9 bg-indigo-500 rounded-full border-2 border-indigo-300 shadow-[0_0_15px_rgba(99,102,241,0.5)] flex items-center justify-center -translate-x-1/2 -translate-y-1/2 hover:scale-110 transition-transform cursor-pointer z-10"><svg className="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" /></svg></div>
                        <div className="absolute top-[70%] left-[30%] w-8 h-8 bg-slate-700 rounded-full border-2 border-slate-500 shadow-sm flex items-center justify-center -translate-x-1/2 -translate-y-1/2 hover:scale-110 transition-transform cursor-pointer"><svg className="w-4 h-4 text-slate-300" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" /></svg></div>
                        
                        <div className="absolute top-[85%] left-[20%] w-6 h-6 bg-slate-800 rounded-full border-2 border-slate-600 shadow-sm flex items-center justify-center -translate-x-1/2 -translate-y-1/2 hover:scale-110 transition-transform cursor-pointer"><svg className="w-3 h-3 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" /></svg></div>
                        <div className="absolute top-[85%] left-[80%] w-6 h-6 bg-slate-800 rounded-full border-2 border-slate-600 shadow-sm flex items-center justify-center -translate-x-1/2 -translate-y-1/2 hover:scale-110 transition-transform cursor-pointer"><svg className="w-3 h-3 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" /></svg></div>

                        {/* Central Node */}
                        <div className="absolute top-[50%] left-[50%] w-12 h-12 bg-indigo-600 rounded-full border-[3px] border-indigo-200 shadow-[0_0_20px_rgba(79,70,229,0.8)] flex items-center justify-center -translate-x-1/2 -translate-y-1/2 z-20 hover:scale-110 transition-transform cursor-pointer"><svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" /></svg></div>
                     </div>
                     <div className="flex gap-2 mt-4 justify-center">
                        <span className="text-[10px] uppercase tracking-wider bg-slate-100 text-slate-500 px-2 py-1 rounded font-bold">Light (0-1)</span>
                        <span className="text-[10px] uppercase tracking-wider bg-indigo-100 text-indigo-600 px-2 py-1 rounded font-bold">Medium (2)</span>
                        <span className="text-[10px] uppercase tracking-wider bg-indigo-600 text-white px-2 py-1 rounded font-bold shadow-sm shadow-indigo-500/30">Dark (3+)</span>
                     </div>
                  </div>

                  {/* Node Info */}
                  <div className="bg-white border border-slate-200 rounded-xl p-5 shadow-sm hover:shadow-md transition-shadow">
                     <div className="flex justify-between items-start mb-3">
                        <h4 className="font-bold text-sm text-slate-800 truncate" title="agile-risk-management.pdf">agile-risk-management.pdf</h4>
                        <span className="bg-indigo-50 text-indigo-700 border border-indigo-200 text-[10px] px-2.5 py-1 rounded-full font-bold uppercase tracking-wider shrink-0 shadow-sm">8 links</span>
                     </div>
                     <div className="flex items-center gap-2 mb-4">
                       <span className="bg-slate-100 text-slate-600 text-[10px] px-2 py-0.5 rounded font-bold uppercase tracking-wider">PDF</span>
                       <span className="text-emerald-600 text-[11px] font-semibold flex items-center gap-1"><div className="w-1.5 h-1.5 bg-emerald-500 rounded-full"></div> Ready</span>
                     </div>
                     <p className="text-[13px] text-slate-600 mb-5 leading-relaxed bg-slate-50 p-3 rounded-lg border border-slate-100">
                        Structured communication practices improve stakeholder alignment and reduce unresolved delivery risks in agile teams.
                     </p>

                     <div className="bg-gradient-to-r from-indigo-50 to-white rounded-lg p-3.5 border border-indigo-100 mb-5 flex items-center justify-between shadow-sm">
                        <div className="flex items-center gap-2 text-[11px] font-bold text-indigo-600 uppercase tracking-widest">
                           <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1" /></svg>
                           Connected Sources
                        </div>
                        <div className="text-2xl font-black text-indigo-700 drop-shadow-sm">8</div>
                     </div>

                     <div>
                        <h5 className="text-[11px] font-bold text-slate-400 uppercase tracking-widest mb-3 flex items-center gap-2">
                          <div className="h-px bg-slate-200 flex-1"></div> Claims <div className="h-px bg-slate-200 flex-1"></div>
                        </h5>
                        <div className="bg-white border border-slate-200 rounded-xl p-4 shadow-sm hover:border-emerald-300 transition-colors group relative overflow-hidden">
                           <div className="absolute top-0 left-0 w-1 h-full bg-emerald-500"></div>
                           <div className="flex justify-between items-start mb-2">
                             <span className="text-[10px] border border-emerald-200 text-emerald-700 px-2 py-0.5 rounded-full bg-emerald-50 font-bold uppercase tracking-wider">Mapped</span>
                             <span className="text-[11px] font-bold text-emerald-600 bg-emerald-50 px-2 py-0.5 rounded">91% Match</span>
                           </div>
                           <p className="text-[13px] text-slate-800 font-bold mb-3 leading-snug">
                              Clear communication protocols reduce project risk
                           </p>
                           <div className="bg-slate-50 p-2.5 rounded text-xs text-slate-600 italic border-l-2 border-slate-300">
                              "Structured communication practices improve stakeholder alignment and reduce unresolved delivery risks in agile teams."
                           </div>
                        </div>
                     </div>
                  </div>
               </div>
             )}

           </div>
        </aside>
      </div>

      {/* HISTORY MODAL */}
      {showHistoryModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-sm animate-in fade-in duration-200">
          <div className="bg-white rounded-xl shadow-2xl w-full max-w-md p-6 transform transition-all">
            <div className="flex justify-between items-center mb-4">
              <h2 className="text-lg font-bold text-slate-800">Version History</h2>
              <button onClick={() => setShowHistoryModal(false)} className="text-slate-400 hover:text-slate-600 transition-colors">
                 <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12"/></svg>
              </button>
            </div>
            <div className="space-y-3">
              <div className="flex justify-between items-center p-3 bg-indigo-50 border border-indigo-100 rounded-lg">
                 <div>
                   <p className="text-sm font-bold text-slate-800">Current Version</p>
                   <p className="text-xs text-slate-500 mt-0.5">Just now</p>
                 </div>
                 <span className="text-[10px] font-bold uppercase tracking-wider text-indigo-600 bg-indigo-100 px-2 py-1 rounded">Active</span>
              </div>
              <div className="flex justify-between items-center p-3 hover:bg-slate-50 border border-slate-100 rounded-lg cursor-pointer transition-colors group">
                 <div>
                   <p className="text-sm font-bold text-slate-800">Added introduction</p>
                   <p className="text-xs text-slate-500 mt-0.5">2 hours ago by Minh Nguyen</p>
                 </div>
                 <button className="text-xs font-semibold text-indigo-600 opacity-0 group-hover:opacity-100 hover:text-indigo-800 transition-all">Restore</button>
              </div>
              <div className="flex justify-between items-center p-3 hover:bg-slate-50 border border-slate-100 rounded-lg cursor-pointer transition-colors group">
                 <div>
                   <p className="text-sm font-bold text-slate-800">Initial Draft</p>
                   <p className="text-xs text-slate-500 mt-0.5">Yesterday by Minh Nguyen</p>
                 </div>
                 <button className="text-xs font-semibold text-indigo-600 opacity-0 group-hover:opacity-100 hover:text-indigo-800 transition-all">Restore</button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* REVISE MODAL */}
      {showReviseModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-sm animate-in fade-in duration-200">
          <div className="bg-white rounded-xl shadow-2xl w-full max-w-md p-6 transform transition-all">
            <div className="flex justify-between items-center mb-4">
              <h2 className="text-lg font-bold text-slate-800">Auto-Revise Document</h2>
              <button onClick={() => setShowReviseModal(false)} className="text-slate-400 hover:text-slate-600 transition-colors">
                 <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12"/></svg>
              </button>
            </div>
            <p className="text-sm text-slate-600 mb-6">Select the areas you want the AI assistant to help you revise based on instructor feedback and mapped claims.</p>
            
            <div className="space-y-3 mb-6">
              <label className="flex items-center gap-3 p-3 border border-slate-200 rounded-lg cursor-pointer hover:bg-slate-50 transition-colors">
                <input type="checkbox" className="w-4 h-4 text-indigo-600 rounded border-slate-300 focus:ring-indigo-500" defaultChecked />
                <span className="text-sm font-medium text-slate-700">Fix unmapped claims (Section 3)</span>
              </label>
              <label className="flex items-center gap-3 p-3 border border-slate-200 rounded-lg cursor-pointer hover:bg-slate-50 transition-colors">
                <input type="checkbox" className="w-4 h-4 text-indigo-600 rounded border-slate-300 focus:ring-indigo-500" defaultChecked />
                <span className="text-sm font-medium text-slate-700">Address Instructor Feedback (Formatting)</span>
              </label>
            </div>

            <div className="flex justify-end gap-3">
              <button onClick={() => setShowReviseModal(false)} className="px-4 py-2 text-sm font-semibold text-slate-600 hover:bg-slate-100 rounded-lg transition-colors">Cancel</button>
              <button onClick={() => {
                setShowReviseModal(false);
                alert("Revision request sent! The AI is processing your document.");
              }} className="px-4 py-2 text-sm font-bold text-white bg-indigo-600 hover:bg-indigo-700 rounded-lg shadow-sm shadow-indigo-200 transition-colors">
                Start Revision
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
