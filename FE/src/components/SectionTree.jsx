export default function SectionTree({ sections, selectedId, onSelect, currentUserId }) {
  if (!sections || sections.length === 0) {
    return (
      <div className="text-xs text-slate-400 italic text-center py-8">
        No sections available. Upload a paper first.
      </div>
    );
  }

  return (
    <div className="space-y-0.5">
      {sections.map((s) => {
        const isSelected = s.id === selectedId;
        const isAssigned = s.assignedUserId === currentUserId;
        const isAssignedToOther = s.assignedUserId && s.assignedUserId !== currentUserId;
        const hasPrevious = s.previousContentTex != null;

        return (
          <button
            key={s.id}
            onClick={() => onSelect(s)}
            className={`w-full text-left px-3 py-2 rounded-md text-xs transition-all flex items-center justify-between gap-2 ${
              isSelected
                ? 'bg-indigo-50 text-indigo-700 border border-indigo-200 shadow-sm'
                : 'text-slate-600 hover:bg-slate-50 border border-transparent'
            } ${isAssignedToOther ? 'opacity-50' : ''}`}
          >
            <div className="flex items-center gap-2 min-w-0">
              <span className="text-[10px] font-mono text-slate-400 w-4 shrink-0">
                {s.sectionOrder != null ? s.sectionOrder + 1 : '?'}
              </span>
              <span className="truncate font-medium">{s.sectionTitle}</span>
            </div>
            <div className="flex items-center gap-1.5 shrink-0">
              {isAssigned && (
                <span className="text-[9px] bg-green-100 text-green-700 px-1.5 py-0.5 rounded font-bold">
                  Yours
                </span>
              )}
              {hasPrevious && (
                <span className="text-[9px] bg-amber-100 text-amber-700 px-1.5 py-0.5 rounded font-bold">
                  v{s.version || '?'}
                </span>
              )}
            </div>
          </button>
        );
      })}
    </div>
  );
}
