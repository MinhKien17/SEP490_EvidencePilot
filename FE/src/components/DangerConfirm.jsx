import { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react';
import Modal from './Modal.jsx';
import { commonText } from '../locales';
import { useLanguage } from '../context/LanguageContext';

const DangerConfirmContext = createContext(null);

export function DangerConfirmProvider({ children }) {
  const [state, setState] = useState(null);
  const resolverRef = useRef(null);

  const confirmDanger = useCallback((message, seconds = 10) => new Promise((resolve) => {
    resolverRef.current = resolve;
    setState({ message, seconds });
  }), []);

  const close = useCallback((result) => {
    setState(null);
    const resolver = resolverRef.current;
    resolverRef.current = null;
    resolver?.(result);
  }, []);

  return (
    <DangerConfirmContext.Provider value={confirmDanger}>
      {children}
      <DangerConfirmModal state={state} onCancel={() => close(false)} onConfirm={() => close(true)} />
    </DangerConfirmContext.Provider>
  );
}

export function useDangerConfirm() {
  return useContext(DangerConfirmContext);
}

function DangerConfirmModal({ state, onCancel, onConfirm }) {
  const { language } = useLanguage();
  const ct = commonText[language];
  const [remaining, setRemaining] = useState(10);

  useEffect(() => {
    if (!state) return;
    setRemaining(state.seconds ?? 10);
    const id = setInterval(() => setRemaining(r => Math.max(0, r - 1)), 1000);
    return () => clearInterval(id);
  }, [state]);

  return (
    <Modal open={!!state} onClose={onCancel} title={ct.confirm} closeLabel={ct.cancel}>
      <div className="space-y-4 text-xs">
        <p className="text-(--text-secondary) whitespace-pre-wrap">{state?.message}</p>
        <div className="flex gap-3 pt-2">
          <button type="button" onClick={onCancel}
            className="flex-1 py-3 bg-(--surface-secondary) hover:bg-(--surface-tertiary) text-(--text-secondary) rounded-xl transition-colors border border-(--border) cursor-pointer">
            {ct.cancel}
          </button>
          <button type="button" onClick={onConfirm} disabled={remaining > 0}
            className="flex-1 py-3 bg-rose-600 text-white rounded-xl hover:bg-rose-700 transition-colors disabled:opacity-50 cursor-pointer">
            {remaining > 0 ? `${ct.confirm} (${remaining})` : ct.confirm}
          </button>
        </div>
      </div>
    </Modal>
  );
}