import axios from 'axios';

export const baseURL = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/+$/, '');

const api = axios.create({
  baseURL,
  timeout: 30000,
  headers: {
    'ngrok-skip-browser-warning': 'true',
  },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  config._authToken = token;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

const LEAD_MS = 60 * 1000;
const NEAR_MS = 10 * 60 * 1000;

let refreshPromise = null;
let refreshTimeout = null;

function decodeExp(token) {
  try {
    const payload = token.split('.')[1];
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
    const json = JSON.parse(atob(base64));
    return typeof json.exp === 'number' ? json.exp * 1000 : null;
  } catch {
    return null;
  }
}

async function refreshToken() {
  refreshPromise = refreshPromise || (async () => {
    const token = localStorage.getItem('token');
    if (!token) throw new Error('no-token');
    const r = await axios.post(`${baseURL}/api/auth/refresh`, null, {
      headers: { Authorization: `Bearer ${token}` },
      timeout: 15000,
    });
    localStorage.setItem('token', r.data.token);
    if (r.data.user?.role) localStorage.setItem('role', r.data.user.role);
    window.dispatchEvent(new CustomEvent('auth:refreshed', { detail: r.data }));
    armProactiveRefresh();
    return r.data.token;
  })().finally(() => { refreshPromise = null; });
  return refreshPromise;
}

function armProactiveRefresh() {
  window.clearTimeout(refreshTimeout);
  const token = localStorage.getItem('token');
  if (!token) return;
  const expMs = decodeExp(token);
  if (!expMs) return;
  refreshTimeout = window.setTimeout(async () => {
    try {
      await refreshToken();
    } catch { /* interceptor + auth:expired handle the rest */ }
  }, Math.max(0, expMs - Date.now() - LEAD_MS));
}

function refreshIfNearExpiry() {
  const token = localStorage.getItem('token');
  if (!token) return;
  const expMs = decodeExp(token);
  if (!expMs) return;
  armProactiveRefresh();
  if (expMs - Date.now() < NEAR_MS) {
    refreshToken().catch(() => {});
  }
}

window.addEventListener('focus', refreshIfNearExpiry);
document.addEventListener('visibilitychange', () => {
  if (document.visibilityState === 'visible') refreshIfNearExpiry();
});
armProactiveRefresh();

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const { config, response } = error;
    const isAuthCall = config.url?.startsWith('/api/auth/');
    const onLoginPage = window.location.pathname.startsWith('/login');
    if (response?.status === 401 && !config._retried && !isAuthCall && !onLoginPage) {
      config._retried = true;
      const currentToken = localStorage.getItem('token');
      if (config._authToken && currentToken && config._authToken !== currentToken) {
        // token rotated by another tab — retry with the current one, no refresh
        config.headers.Authorization = `Bearer ${currentToken}`;
        return api(config);
      }
      try {
        const token = await refreshToken();
        config.headers.Authorization = `Bearer ${token}`;
        return api(config);
      } catch {
        // refresh failed, but the token may have changed mid-flight (concurrent tab refresh)
        const nowToken = localStorage.getItem('token');
        if (nowToken && nowToken !== config._authToken) {
          config.headers.Authorization = `Bearer ${nowToken}`;
          return api(config);
        }
        window.dispatchEvent(new CustomEvent('auth:expired'));
      }
    }
    return Promise.reject(error);
  }
);

export { armProactiveRefresh };
export default api;
