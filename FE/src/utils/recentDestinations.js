import api from '../services/api.js';

// Phase B: record an intentionally opened destination server-side.
// Debounced per destination (remounts within 30s are ignored); failures are
// silent by design — a shortcut list must never break navigation.
const recentSent = new Map();

export function recordRecentDestination(kind, refId, tab) {
  if (!kind) return;
  const key = `${kind}|${refId || ''}|${tab || ''}`;
  const now = Date.now();
  if (recentSent.has(key) && now - recentSent.get(key) < 30_000) return;
  recentSent.set(key, now);
  api.put('/api/users/me/recent-destinations', {
    kind,
    refId: refId || null,
    tab: tab || null,
  }).catch(() => {});
}
