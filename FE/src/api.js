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
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

let refreshPromise = null;

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const { config, response } = error;
    const isAuthCall = config.url?.startsWith('/api/auth/');
    const onLoginPage = window.location.pathname.startsWith('/login');
    if (response?.status === 401 && !config._retried && !isAuthCall && !onLoginPage) {
      config._retried = true;
      try {
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
          return r.data.token;
        })().finally(() => { refreshPromise = null; });
        config.headers.Authorization = `Bearer ${await refreshPromise}`;
        return api(config);
      } catch {
        window.dispatchEvent(new CustomEvent('auth:expired'));
      }
    }
    return Promise.reject(error);
  }
);

export default api;
