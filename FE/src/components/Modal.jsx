import { useEffect, useRef } from 'react';

export default function Modal({ open, onClose, title, children, wide, className }) {
  const ref = useRef();

  useEffect(() => {
    if (!open) return;
    const handler = (e) => { if (e.key === 'Escape') onClose(); };
    document.addEventListener('keydown', handler);
    return () => document.removeEventListener('keydown', handler);
  }, [open, onClose]);

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40" onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}>
      <div ref={ref} className={`bg-white rounded-2xl shadow-2xl max-h-[90vh] overflow-y-auto ${wide ? 'max-w-3xl w-[94%]' : 'max-w-lg w-[94%]'} ${className || ''}`}>
        <div className="flex items-center justify-between px-5 py-3.5 border-b border-slate-200">
          <h2 className="text-base font-bold text-slate-800">{title}</h2>
          <button onClick={onClose} className="text-slate-400 hover:text-slate-700 text-xl leading-none">&times;</button>
        </div>
        <div className="p-5">{children}</div>
      </div>
    </div>
  );
}
