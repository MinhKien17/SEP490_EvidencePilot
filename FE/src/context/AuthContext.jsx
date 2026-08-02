import { createContext, useContext, useState, useCallback, useEffect, useRef } from 'react';
import api from '../api.js';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem('token'));
  const [role, setRole] = useState(() => localStorage.getItem('role'));
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const verifyPromiseRef = useRef(null);

  const verifySession = useCallback(() => {
    // single-flight — mount + login share one in-flight profile fetch
    if (!verifyPromiseRef.current) {
      verifyPromiseRef.current = api.get('/api/users/profile')
        .then((res) => { setUser(res.data); return res.data; })
        .finally(() => { verifyPromiseRef.current = null; });
    }
    return verifyPromiseRef.current;
  }, []);

  useEffect(() => {
    const storedToken = localStorage.getItem('token');
    const storedRole = localStorage.getItem('role');
    if (storedToken) {
      setToken(storedToken);
      setRole(storedRole || '');
      verifySession()
        .catch(() => {
          localStorage.removeItem('token');
          localStorage.removeItem('role');
          setToken(null);
          setRole('');
        })
        .finally(() => setLoading(false));
    } else {
      setLoading(false);
    }
  }, [verifySession]);

  const login = useCallback((newToken, newRole) => {
    localStorage.setItem('token', newToken);
    if (newRole) {
      localStorage.setItem('role', newRole);
    }
    setToken(newToken);
    setRole(newRole || '');
    verifySession().catch(() => {});
  }, [verifySession]);

  const logout = useCallback(() => {
    localStorage.removeItem('token');
    localStorage.removeItem('role');
    setToken(null);
    setRole('');
    setUser(null);
  }, []);

  useEffect(() => {
    const onAuthExpired = () => {
      if (!window.location.pathname.startsWith('/login')) {
        const origin = window.location.pathname + window.location.search;
        sessionStorage.setItem('login_origin', origin);
        sessionStorage.setItem('auth_expired_notice', 'Your session expired. Please sign in again.');
        logout();
      }
    };
    const onAuthRefreshed = (e) => {
      setToken(e.detail?.token ?? null);
      setUser(e.detail?.user ?? null);
      if (e.detail?.user?.role) setRole(e.detail.user.role);
    };
    window.addEventListener('auth:expired', onAuthExpired);
    window.addEventListener('auth:refreshed', onAuthRefreshed);
    return () => {
      window.removeEventListener('auth:expired', onAuthExpired);
      window.removeEventListener('auth:refreshed', onAuthRefreshed);
    };
  }, [logout]);

  const isAuthenticated = !!token;

  return (
    <AuthContext.Provider value={{ token, role, user, isAuthenticated, loading, login, logout, verifySession }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider');
  return ctx;
}

export default AuthContext;
