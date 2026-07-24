import { useState, useEffect } from 'react';
import { driver } from 'driver.js';
import 'driver.js/dist/driver.css';

const STORAGE_KEY = 'tour_seen';

export default function TourLauncher({ steps, tourKey, autoLaunch = false }) {
  const [show, setShow] = useState(false);

  useEffect(() => {
    if (autoLaunch && tourKey && !localStorage.getItem(`${STORAGE_KEY}_${tourKey}`)) {
      const timer = setTimeout(() => setShow(true), 600);
      return () => clearTimeout(timer);
    }
  }, [autoLaunch, tourKey]);

  useEffect(() => {
    if (!show) return;
    const d = driver({ steps, showProgress: true, showButtons: ['next', 'previous', 'close'],
      onDestroyed: () => {
        setShow(false);
        if (tourKey) localStorage.setItem(`${STORAGE_KEY}_${tourKey}`, '1');
      }
    });
    d.drive();
    return () => { try { d.destroy(); } catch {} };
  }, [show, steps, tourKey]);

  if (autoLaunch) return null;

  return (
    <button
      onClick={() => setShow(true)}
      className="fixed bottom-4 left-4 z-40 w-9 h-9 rounded-full bg-white border border-slate-300 shadow-md flex items-center justify-center text-sm font-bold text-slate-500 hover:bg-indigo-50 hover:text-indigo-600 hover:border-indigo-300 transition-all"
      title="Guide"
    >
      ?
    </button>
  );
}
