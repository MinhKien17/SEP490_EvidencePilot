export default function SplitPanel({ left, center, right, leftLabel, centerLabel, rightLabel }) {
  return (
    <div className="flex h-full gap-0">
      {left && (
        <div className="w-64 shrink-0 border-r border-slate-200 bg-slate-50/50 flex flex-col">
          {leftLabel && <div className="px-3 py-2 text-[10px] font-bold uppercase tracking-widest text-slate-400 border-b border-slate-200">{leftLabel}</div>}
          <div className="flex-1 overflow-y-auto p-2">{left}</div>
        </div>
      )}
      {center && (
        <div className="flex-1 min-w-0 flex flex-col">
          {centerLabel && <div className="px-3 py-2 text-[10px] font-bold uppercase tracking-widest text-slate-400 border-b border-slate-200">{centerLabel}</div>}
          <div className="flex-1 overflow-y-auto p-3">{center}</div>
        </div>
      )}
      {right && (
        <div className="w-80 shrink-0 border-l border-slate-200 bg-slate-50/50 flex flex-col">
          {rightLabel && <div className="px-3 py-2 text-[10px] font-bold uppercase tracking-widest text-slate-400 border-b border-slate-200">{rightLabel}</div>}
          <div className="flex-1 overflow-y-auto p-2">{right}</div>
        </div>
      )}
    </div>
  );
}
