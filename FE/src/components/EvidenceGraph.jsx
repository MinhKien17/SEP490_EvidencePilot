import { useEffect, useRef } from 'react';
import { Network } from 'vis-network';
import { DataSet } from 'vis-data';

const VERDICT_COLORS = {
  SUPPORTS: { background: '#34d399', border: '#059669' },
  EXTENDS: { background: '#34d399', border: '#059669' },
  DETAILS: { background: '#34d399', border: '#059669' },
  GENERALIZES: { background: '#34d399', border: '#059669' },
  CONTRADICTS: { background: '#fb7185', border: '#e11d48' },
  NEUTRAL: { background: '#fbbf24', border: '#d97706' },
  UNKNOWN: { background: '#94a3b8', border: '#64748b' },
};

export default function EvidenceGraph({ traceabilityData, onClaimClick, onSourceClick, height = 400 }) {
  const containerRef = useRef(null);
  const networkRef = useRef(null);

  useEffect(() => {
    if (!containerRef.current || !traceabilityData) return;

    const claims = traceabilityData.claims || [];
    const sources = traceabilityData.sources || [];

    if (claims.length === 0 && sources.length === 0) return;

    const nodes = [];
    const edges = [];

    claims.forEach((c, i) => {
      const g = c.graphData || {};
      const verdict = g.verdict || 'UNKNOWN';
      const color = VERDICT_COLORS[verdict] || VERDICT_COLORS.UNKNOWN;
      nodes.push({
        id: `claim-${c.id}`,
        label: c.content ? c.content.slice(0, 50) + '...' : 'Claim',
        title: `<b>Claim:</b> ${c.content || ''}<br/><b>Verdict:</b> ${verdict}${g.confidence != null ? ` (${g.confidence}%)` : ''}`,
        shape: 'box',
        color: { background: color.background, border: color.border },
        font: { color: '#1e293b', size: 10 },
        group: 'claims',
        x: -300,
        y: i * 100,
      });
    });

    sources.forEach((s, i) => {
      nodes.push({
        id: `source-${s.id}`,
        label: s.filename ? s.filename.slice(0, 25) + '...' : 'Source',
        title: `<b>Source:</b> ${s.filename || ''}<br/><b>References:</b> ${s.referenceCount || 0}`,
        shape: 'ellipse',
        color: { background: '#6366f1', border: '#4338ca' },
        font: { color: '#e2e8f0', size: 10 },
        group: 'sources',
        x: 300,
        y: i * 100,
      });
    });

    (traceabilityData.edges || []).forEach((e) => {
      const verdict = e.relation || 'UNKNOWN';
      const color = VERDICT_COLORS[verdict] || VERDICT_COLORS.UNKNOWN;
      edges.push({
        from: `claim-${e.sourceId}`,
        to: `source-${e.targetId}`,
        color: { color: color.border, opacity: 0.6 },
        width: e.score != null ? Math.max(1, e.score / 25) : 1,
        title: `Score: ${e.score ?? '?'}%`,
      });
    });

    const data = { nodes: new DataSet(nodes), edges: new DataSet(edges) };
    const options = {
      physics: { solver: 'repulsion', repulsion: { nodeDistance: 250 } },
      interaction: { hover: true, tooltipDelay: 200 },
      groups: {
        claims: { shape: 'box' },
        sources: { shape: 'ellipse' },
      },
    };

    networkRef.current = new Network(containerRef.current, data, options);

    networkRef.current.on('click', (params) => {
      if (params.nodes.length > 0) {
        const nodeId = params.nodes[0];
        if (nodeId.startsWith('claim-') && onClaimClick) {
          const claim = claims.find((c) => `claim-${c.id}` === nodeId);
          if (claim) onClaimClick(claim);
        } else if (nodeId.startsWith('source-') && onSourceClick) {
          const source = sources.find((s) => `source-${s.id}` === nodeId);
          if (source) onSourceClick(source);
        }
      }
    });

    return () => { if (networkRef.current) networkRef.current.destroy(); };
  }, [traceabilityData, onClaimClick, onSourceClick]);

  if (!traceabilityData) {
    return (
      <div className="flex items-center justify-center h-full text-xs text-slate-400 italic">
        Run AI analysis on claims to see the evidence map.
      </div>
    );
  }

  return (
    <div>
      <div ref={containerRef} style={{ height, border: '1px solid #e2e8f0', borderRadius: '8px' }} />
      <div className="flex gap-4 mt-2 text-[10px] text-slate-500">
        <span className="flex items-center gap-1"><span className="w-3 h-3 rounded bg-emerald-400" /> SUPPORTIVE</span>
        <span className="flex items-center gap-1"><span className="w-3 h-3 rounded bg-amber-400" /> NEUTRAL</span>
        <span className="flex items-center gap-1"><span className="w-3 h-3 rounded bg-rose-400" /> CONTRADICTS</span>
      </div>
    </div>
  );
}
