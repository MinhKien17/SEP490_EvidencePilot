import { createContext, useContext, useState, useCallback } from 'react';

const ToastCtx = createContext(null);

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);

  const add = useCallback((message, type = 'info') => {
    const id = Date.now();
    setToasts(prev => [...prev, { id, message, type }]);
    setTimeout(() => setToasts(prev => prev.filter(t => t.id !== id)), 3500);
  }, []);

  const remove = (id) => setToasts(prev => prev.filter(t => t.id !== id));

  return (
    <ToastCtx.Provider value={{ toast: add }}>
      {children}
      <div className="fixed bottom-5 right-5 z-[100] flex flex-col gap-2 max-w-xs">
        {toasts.map(t => (
          <div
            key={t.id}
            onClick={() => remove(t.id)}
            className={`cursor-pointer text-sm px-4 py-2.5 rounded-xl shadow-lg text-white animate-[fadeIn_0.3s_ease] ${
              t.type === 'error' ? 'bg-rose-600' :
              t.type === 'success' ? 'bg-emerald-600' :
              'bg-slate-800'
            }`}
          >
            {t.message}
          </div>
        ))}
      </div>
    </ToastCtx.Provider>
  );
}

export function useToast() {
  const ctx = useContext(ToastCtx);
  if (!ctx) throw new Error('useToast must be used within ToastProvider');
  return ctx;
}
