import { useMemo, useEffect, useState } from 'react';
import katex from 'katex';
import api from '../api.js';

function escHtml(s) {
  if (!s) return '';
  return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

function renderLatexToHtml(latex, mediaUrlMap) {
  if (!latex) return '<p class="text-slate-400 italic">No content to preview.</p>';

  let body = latex
    .replace(/\\documentclass.*?\n/g, '')
    .replace(/\\usepackage.*?\n/g, '')
    .replace(/\\title\{([^}]*)\}/g, '<h1 class="text-2xl font-bold text-center mb-2">$1</h1>')
    .replace(/\\author\{([^}]*)\}/g, '<p class="text-sm text-center text-slate-500 mb-8">$1</p>')
    .replace(/\\date\{[^}]*\}/g, '')
    .replace(/\\maketitle/g, '')
    .replace(/\\begin\{document\}/g, '')
    .replace(/\\end\{document\}/g, '')
    .replace(/\\section\{([^}]*)\}/g, '<h2 class="text-lg font-bold mt-6 mb-3 text-slate-800">$1</h2>')
    .replace(/\\subsection\{([^}]*)\}/g, '<h3 class="text-base font-semibold mt-4 mb-2 text-slate-700">$1</h3>')
    .replace(/\\textbf\{([^}]*)\}/g, '<strong>$1</strong>')
    .replace(/\\textit\{([^}]*)\}/g, '<em>$1</em>')
    .replace(/\\hl\{([^}]*)\}/g, '<span class="bg-yellow-200 px-1 rounded">$1</span>')
    .replace(/\\cite\{([^}]*)\}/g, '<span class="text-indigo-600 text-xs">[$1]</span>')
    .replace(/\\label\{([^}]*)\}/g, '')

    // includegraphics → <img>
    .replace(/\\includegraphics(?:\[.*?\])?\{([^}]+)\}/g, (_, fn) => {
      const url = mediaUrlMap ? mediaUrlMap[fn] : null;
      const alt = escHtml(fn);
      if (!url) return `<span class="text-red-500 text-xs">[missing image: ${alt}]</span>`;
      return `<img src="${url}" alt="${alt}" class="max-w-full my-2 rounded border" />`;
    })

    // tabular → <table> (strip outer table environment)
    .replace(/\\begin\{table\}(?:\[[^\]]*\])?[\s\S]*?\\end\{table\}/g, (match) => {
      const tabularMatch = match.match(/\\begin\{tabular\}\{([^}]*)\}([\s\S]*?)\\end\{tabular\}/);
      if (!tabularMatch) return match;
      return tabularMatch[0]; // keep only the tabular part, strip \caption etc.
    });

  // Convert tabular to HTML table
  body = body.replace(/\\begin\{tabular\}\{([^}]*)\}([\s\S]*?)\\end\{tabular\}/g, (_, colSpec, rowsContent) => {
    const rows = rowsContent.split(/\\\\/).map(r => r.trim()).filter(Boolean);
    const htmlRows = rows.map((row, ri) => {
      const cells = row.replace(/\\hline\s*/g, '').split('&').map(c => c.trim()).filter(Boolean);
      if (cells.length === 0) return '';
      const tag = ri === 0 ? 'th' : 'td';
      return `<tr>${cells.map(c => `<${tag} class="border border-gray-300 px-2 py-1">${escHtml(c)}</${tag}>`).join('')}</tr>`;
    }).filter(Boolean);
    return `<table class="w-full border-collapse my-4 text-xs">${htmlRows.join('')}</table>`;
  });

  body = body.replace(/\\\[(.*?)\\\]/gs, (_, eq) => {
    try { return katex.renderToString(eq.trim(), { displayMode: true, throwOnError: false }); }
    catch { return `<div class="text-red-500">${eq}</div>`; }
  });
  body = body.replace(/\\begin{equation\*?}([\s\S]*?)\\end{equation\*?}/g, (_, eq) => {
    try { return katex.renderToString(eq.trim(), { displayMode: true, throwOnError: false }); }
    catch { return `<div class="text-red-500">${eq}</div>`; }
  });
  body = body.replace(/\\begin{align\*?}([\s\S]*?)\\end{align\*?}/g, (_, eq) => {
    try { return katex.renderToString(eq.trim(), { displayMode: true, throwOnError: false }); }
    catch { return `<div class="text-red-500">${eq}</div>`; }
  });
  body = body.replace(/\\begin{aligned\*?}([\s\S]*?)\\end{aligned\*?}/g, (_, eq) => {
    try { return katex.renderToString(eq.trim(), { displayMode: true, throwOnError: false }); }
    catch { return `<div class="text-red-500">${eq}</div>`; }
  });
  body = body.replace(/\$\$(.*?)\$\$/gs, (_, eq) => {
    try { return katex.renderToString(eq.trim(), { displayMode: true, throwOnError: false }); }
    catch { return `<div class="text-red-500">${eq}</div>`; }
  });

  body = body.replace(/\$(.*?)\$/g, (_, eq) => {
    try { return katex.renderToString(eq.trim(), { displayMode: false, throwOnError: false }); }
    catch { return `<span class="text-red-500">${eq}</span>`; }
  });

  const tables = [];
  body = body.replace(/<table[\s\S]*?<\/table>|<tr[\s\S]*?<\/tr>/gi, (m) => {
    tables.push(m);
    return `\u0000TBL${tables.length - 1}\u0000`;
  });
  const paragraphs = body.split(/\n\n+/).filter(p => p.trim());
  return paragraphs.map(p => {
    if (p.includes('\u0000TBL')) {
      let chunk = p.replace(/\u0000TBL(\d+)\u0000/g, (_, i) => tables[Number(i)]);
      if (!/<\/table>/i.test(chunk)) chunk = `<table class="w-full">${chunk}</table>`;
      return `<div class="mb-4 overflow-x-auto">${chunk}</div>`;
    }
    return `<p class="mb-4 leading-relaxed text-slate-700">${p}</p>`;
  }).join('\n');
}

export default function PreviewPane({ latex, mediaAssets }) {
  const [mediaUrlMap, setMediaUrlMap] = useState({});

  useEffect(() => {
    if (!mediaAssets || mediaAssets.length === 0) {
      setMediaUrlMap({});
      return;
    }
    let cancelled = false;
    (async () => {
      const map = {};
      for (const asset of mediaAssets) {
        if (cancelled) return;
        try {
          const r = await api.get(`/api/media/${asset.id}/url`);
          map[asset.texFilename] = r.data?.url;
        } catch { /* skip */ }
      }
      if (!cancelled) setMediaUrlMap(map);
    })();
    return () => { cancelled = true; };
  }, [mediaAssets]);

  const html = useMemo(() => renderLatexToHtml(latex, mediaUrlMap), [latex, mediaUrlMap]);

  return (
    <div className="h-full overflow-y-auto bg-white p-8">
      <div className="max-w-prose mx-auto whitespace-pre-wrap break-words preview-content" dangerouslySetInnerHTML={{ __html: html }} />
    </div>
  );
}