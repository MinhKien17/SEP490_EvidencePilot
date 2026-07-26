import { useMemo } from 'react';
import katex from 'katex';

function renderLatexToHtml(latex) {
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
    .replace(/\\label\{([^}]*)\}/g, '');

  body = body.replace(/\$\$(.*?)\$\$/gs, (_, eq) => {
    try { return katex.renderToString(eq.trim(), { displayMode: true, throwOnError: false }); }
    catch { return `<div class="text-red-500">${eq}</div>`; }
  });

  body = body.replace(/\$(.*?)\$/g, (_, eq) => {
    try { return katex.renderToString(eq.trim(), { displayMode: false, throwOnError: false }); }
    catch { return `<span class="text-red-500">${eq}</span>`; }
  });

  const paragraphs = body.split(/\n\n+/).filter(p => p.trim());
  return paragraphs.map(p => `<p class="mb-4 leading-relaxed text-slate-700">${p}</p>`).join('\n');
}

export default function PreviewPane({ latex }) {
  const html = useMemo(() => renderLatexToHtml(latex), [latex]);

  return (
    <div className="h-full overflow-y-auto bg-white p-8">
      <div className="max-w-prose mx-auto" dangerouslySetInnerHTML={{ __html: html }} />
    </div>
  );
}
