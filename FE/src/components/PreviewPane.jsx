import { useMemo, useEffect, useState, useRef, forwardRef, useImperativeHandle } from 'react';
import api from '../api.js';
import { renderLatexToHtml } from './latexHtml.js';

const PreviewPane = forwardRef(function PreviewPane({
  latex,
  mediaAssets,
  citationNumbers,
  generatedReferences = [],
  referencesTitle = 'References',
}, ref) {
  const containerRef = useRef(null);
  const [mediaUrlMap, setMediaUrlMap] = useState({});

  useEffect(() => {
    if (!mediaAssets || mediaAssets.length === 0) {
      setMediaUrlMap({});
      return;
    }
    let cancelled = false;
    (async () => {
      try {
        const r = await api.post('/api/media/urls', { ids: mediaAssets.map(a => a.id) });
        const urls = r.data || {};
        if (cancelled) return;
        const map = {};
        for (const asset of mediaAssets) {
          const url = urls[asset.id];
          if (url) map[asset.texFilename] = url;
        }
        setMediaUrlMap(map);
      } catch {
        if (!cancelled) setMediaUrlMap({});
      }
    })();
    return () => { cancelled = true; };
  }, [mediaAssets]);

  const html = useMemo(
    () => (!latex && generatedReferences.length > 0
      ? ''
      : renderLatexToHtml(latex, mediaUrlMap, citationNumbers)),
    [citationNumbers, generatedReferences.length, latex, mediaUrlMap],
  );

  useImperativeHandle(ref, () => ({
    getScrollInfo: () => {
      const el = containerRef.current;
      if (!el) return { top: 0, height: 0, clientHeight: 0 };
      return {
        top: el.scrollTop,
        height: el.scrollHeight,
        clientHeight: el.clientHeight,
      };
    },
    scrollTo: (top) => {
      const el = containerRef.current;
      if (el) el.scrollTop = top;
    },
  }));

  return (
    <div ref={containerRef} className="h-full overflow-y-auto bg-white p-8">
      {html && <div className="max-w-prose mx-auto whitespace-pre-wrap break-words preview-content" dangerouslySetInnerHTML={{ __html: html }} />}
      {generatedReferences.length > 0 && (
        <section className="max-w-prose mx-auto text-slate-700">
          <h2 className="text-lg font-bold mt-6 mb-3 text-slate-800">{referencesTitle}</h2>
          <ol className="space-y-3 text-sm">
            {generatedReferences.map(reference => (
              <li key={reference.key} className="flex gap-2 leading-relaxed">
                <span className="shrink-0 text-indigo-700">[{reference.number}]</span>
                <span>{reference.reference}</span>
              </li>
            ))}
          </ol>
        </section>
      )}
    </div>
  );
});

export default PreviewPane;
