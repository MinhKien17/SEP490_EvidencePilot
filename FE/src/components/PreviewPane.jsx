import { useMemo, useEffect, useState } from 'react';
import api from '../api.js';
import { renderLatexToHtml } from './latexHtml.js';

export default function PreviewPane({ latex, mediaAssets }) {
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

  const html = useMemo(() => renderLatexToHtml(latex, mediaUrlMap), [latex, mediaUrlMap]);

  return (
    <div className="h-full overflow-y-auto bg-white p-8">
      <div className="max-w-prose mx-auto whitespace-pre-wrap break-words preview-content" dangerouslySetInnerHTML={{ __html: html }} />
    </div>
  );
}