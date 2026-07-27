import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useLanguage } from '../../context/LanguageContext';
import { useTheme } from '../../context/ThemeContext';

export default function Navbar({ t }) {
  const { isAuthenticated, role, logout } = useAuth();
  const { toggleLanguage } = useLanguage();
  const { theme, toggleTheme } = useTheme();
  const [menuOpen, setMenuOpen] = useState(false);

  const wsLink = !isAuthenticated ? '/login'
    : role === 'ADMIN' ? '/admin/dashboard'
      : role === 'INSTRUCTOR' ? '/instructor/dashboard'
        : '/student/projects';

  const wsLabel = !isAuthenticated ? t.nav.login : t.nav.workspace;

  return (
    <header className="fixed top-0 left-0 right-0 z-50 bg-white/80 backdrop-blur-md border-b border-gray-100/80">
      <div className="max-w-7xl mx-auto px-6 h-16 flex items-center justify-between">
        <Link to="/" className="flex items-center gap-2.5">
          <div className="w-8 h-8 bg-[#1e3a8a] rounded-lg flex items-center justify-center">
            <span className="text-white font-bold text-sm">EP</span>
          </div>
          <span className="font-bold text-gray-900 hidden sm:inline">Evidence Pilot</span>
        </Link>

        <nav className="hidden md:flex items-center gap-5 text-sm">
          <Link to="/" className="text-gray-600 hover:text-[#1e3a8a] font-medium transition">{t.nav.home}</Link>
          <Link to="/about" className="text-gray-600 hover:text-[#1e3a8a] font-medium transition">{t.nav.about}</Link>
          <Link to="/terms" className="text-gray-600 hover:text-[#1e3a8a] font-medium transition">{t.nav.terms}</Link>
          <Link to="/privacy" className="text-gray-600 hover:text-[#1e3a8a] font-medium transition">{t.nav.privacy}</Link>
          <button onClick={toggleTheme} className="p-1.5 text-gray-400 hover:text-[#1e3a8a] transition rounded-lg" title={theme === 'light' ? 'Dark mode' : 'Light mode'}>
            {theme === 'light' ? (
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z" /></svg>
            ) : (
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z" /></svg>
            )}
          </button>
          <button onClick={toggleLanguage} className="text-xs font-bold text-gray-400 hover:text-[#1e3a8a] transition px-2 py-1 border border-gray-200 rounded-lg">{t.nav.lang}</button>
          {isAuthenticated ? (
            <>
              <Link to="/profile" className="text-xs font-bold text-gray-500 hover:text-[#1e3a8a] transition px-3 py-1.5 border border-gray-200 rounded-lg">{t.nav.profile}</Link>
              <button onClick={logout} className="text-xs font-bold text-gray-500 hover:text-rose-600 transition px-3 py-1.5 border border-gray-200 rounded-lg">{t.nav.signOut}</button>
            </>
          ) : (
            <Link to="/register" className="text-xs font-bold text-white bg-[#1e3a8a] hover:bg-[#1e40af] transition px-4 py-1.5 rounded-lg shadow-sm">{t.nav.register}</Link>
          )}
          <Link to={wsLink} className="text-xs font-bold text-[#1e3a8a] bg-indigo-50 hover:bg-indigo-100 transition px-4 py-1.5 rounded-lg">{wsLabel}</Link>
        </nav>

        <button className="md:hidden p-2 text-gray-600" onClick={() => setMenuOpen(!menuOpen)}>
          <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            {menuOpen
              ? <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              : <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />}
          </svg>
        </button>
      </div>
      {menuOpen && (
        <div className="md:hidden bg-white border-t border-gray-100 px-6 py-4 space-y-3 text-sm">
          <Link to="/" className="block text-gray-600 font-medium" onClick={() => setMenuOpen(false)}>{t.nav.home}</Link>
          <Link to="/about" className="block text-gray-600 font-medium" onClick={() => setMenuOpen(false)}>{t.nav.about}</Link>
          <div className="flex gap-4 pt-1 border-t border-gray-100">
            <Link to="/terms" className="text-gray-600 font-medium" onClick={() => setMenuOpen(false)}>{t.nav.terms}</Link>
            <Link to="/privacy" className="text-gray-600 font-medium" onClick={() => setMenuOpen(false)}>{t.nav.privacy}</Link>
          </div>
          <button onClick={() => { toggleTheme(); setMenuOpen(false); }} className="block text-xs font-bold text-gray-400 border border-gray-200 rounded-lg px-3 py-1.5">{theme === 'light' ? '🌙' : '☀️'}</button>
          <button onClick={() => { toggleLanguage(); setMenuOpen(false); }} className="block text-xs font-bold text-gray-400 border border-gray-200 rounded-lg px-3 py-1.5">{t.nav.lang}</button>
          {isAuthenticated ? (
            <>
              <Link to="/profile" className="block text-xs font-bold text-gray-500 border border-gray-200 rounded-lg px-3 py-1.5" onClick={() => setMenuOpen(false)}>{t.nav.profile}</Link>
              <button onClick={() => { logout(); setMenuOpen(false); }} className="block text-xs font-bold text-rose-600 border border-gray-200 rounded-lg px-3 py-1.5">{t.nav.signOut}</button>
            </>
          ) : (
            <div className="flex gap-2">
              <Link to="/login" className="flex-1 text-center text-xs font-bold text-gray-600 border border-gray-200 rounded-lg px-3 py-1.5" onClick={() => setMenuOpen(false)}>{t.nav.login}</Link>
              <Link to="/register" className="flex-1 text-center text-xs font-bold text-white bg-[#1e3a8a] rounded-lg px-3 py-1.5" onClick={() => setMenuOpen(false)}>{t.nav.register}</Link>
            </div>
          )}
        </div>
      )}
    </header>
  );
}
