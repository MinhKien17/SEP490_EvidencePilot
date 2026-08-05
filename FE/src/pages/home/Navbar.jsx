import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useLanguage } from '../../context/LanguageContext';
import { useTheme } from '../../context/ThemeContext';
import { commonText } from '../../locales';

function ThemeIcon({ theme }) {
  return theme === 'light' ? (
    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z" /></svg>
  ) : (
    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z" /></svg>
  );
}

export default function Navbar({ t }) {
  const { isAuthenticated, role, logout } = useAuth();
  const { language, toggleLanguage } = useLanguage();
  const { theme, toggleTheme } = useTheme();
  const [menuOpen, setMenuOpen] = useState(false);
  const ct = commonText[language];

  const wsLink = !isAuthenticated ? '/login'
    : role === 'ADMIN' ? '/admin/dashboard'
      : role === 'INSTRUCTOR' ? '/instructor/dashboard'
        : '/student/projects';
  const wsLabel = !isAuthenticated ? t.nav.login : t.nav.workspace;
  const themeLabel = theme === 'light' ? ct.darkMode : ct.lightMode;
  const closeMenu = () => setMenuOpen(false);

  return (
    <header className="fixed top-0 left-0 right-0 z-50 h-16 bg-(--header-bg) backdrop-blur-md border-b border-(--header-border) shadow-sm">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 h-full flex items-center justify-between gap-4">
        <Link to="/" className="flex items-center gap-2.5 rounded-lg">
          <span className="w-8 h-8 bg-(--brand) rounded-lg flex items-center justify-center text-(--on-brand) font-bold text-sm">EP</span>
          <span className="font-bold text-(--text-primary) hidden sm:inline">Evidence Pilot</span>
        </Link>

        <nav className="hidden md:flex items-center gap-4 text-sm" aria-label={ct.primaryNavigation}>
          <Link to="/" className="text-(--text-secondary) hover:text-(--brand-foreground) font-medium transition-colors">{t.nav.home}</Link>
          <Link to="/about" className="text-(--text-secondary) hover:text-(--brand-foreground) font-medium transition-colors">{t.nav.about}</Link>
          <Link to="/terms" className="text-(--text-secondary) hover:text-(--brand-foreground) font-medium transition-colors">{t.nav.terms}</Link>
          <Link to="/privacy" className="text-(--text-secondary) hover:text-(--brand-foreground) font-medium transition-colors">{t.nav.privacy}</Link>
          <button type="button" onClick={toggleTheme} className="p-2 text-(--text-secondary) hover:text-(--brand-foreground) hover:bg-(--surface-secondary) transition-colors rounded-lg" title={themeLabel} aria-label={themeLabel}>
            <ThemeIcon theme={theme} />
          </button>
          <button type="button" onClick={toggleLanguage} className="text-xs font-bold text-(--text-secondary) hover:text-(--brand-foreground) transition-colors px-2 py-1.5 border border-(--border) rounded-lg">{t.nav.lang}</button>
          {isAuthenticated ? (
            <>
              <Link to="/profile" className="text-xs font-bold text-(--text-secondary) hover:text-(--brand-foreground) transition-colors px-3 py-1.5 border border-(--border) rounded-lg">{t.nav.profile}</Link>
              <button type="button" onClick={logout} className="text-xs font-bold text-(--text-secondary) hover:text-rose-600 transition-colors px-3 py-1.5 border border-(--border) rounded-lg">{t.nav.signOut}</button>
            </>
          ) : (
            null
          )}
          <Link to={wsLink} className="text-xs font-bold text-(--brand-foreground) bg-(--brand-soft) hover:brightness-95 transition px-4 py-2 rounded-lg">{wsLabel}</Link>
        </nav>

        <button
          type="button"
          className="md:hidden p-2 text-(--text-secondary) hover:bg-(--surface-secondary) rounded-lg"
          onClick={() => setMenuOpen(value => !value)}
          aria-expanded={menuOpen}
          aria-controls="home-mobile-navigation"
          aria-label={menuOpen ? ct.closeMenu : ct.openMenu}
        >
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
            {menuOpen
              ? <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
              : <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 6h16M4 12h16M4 18h16" />}
          </svg>
        </button>
      </div>

      {menuOpen && (
        <div id="home-mobile-navigation" className="md:hidden bg-(--surface) border-b border-(--border) px-4 sm:px-6 py-4 space-y-2 text-sm shadow-xl">
          <Link to="/" className="block text-(--text-secondary) font-medium rounded-lg px-3 py-2 hover:bg-(--surface-secondary)" onClick={closeMenu}>{t.nav.home}</Link>
          <Link to="/about" className="block text-(--text-secondary) font-medium rounded-lg px-3 py-2 hover:bg-(--surface-secondary)" onClick={closeMenu}>{t.nav.about}</Link>
          <div className="grid grid-cols-2 gap-2">
            <Link to="/terms" className="text-(--text-secondary) font-medium rounded-lg px-3 py-2 hover:bg-(--surface-secondary)" onClick={closeMenu}>{t.nav.terms}</Link>
            <Link to="/privacy" className="text-(--text-secondary) font-medium rounded-lg px-3 py-2 hover:bg-(--surface-secondary)" onClick={closeMenu}>{t.nav.privacy}</Link>
          </div>
          <div className="grid grid-cols-2 gap-2 pt-3 border-t border-(--border-light)">
            <button type="button" onClick={() => { toggleTheme(); closeMenu(); }} className="flex items-center justify-center gap-2 text-xs font-bold text-(--text-secondary) border border-(--border) rounded-xl px-3 py-2.5"><ThemeIcon theme={theme} /> {themeLabel}</button>
            <button type="button" onClick={() => { toggleLanguage(); closeMenu(); }} className="text-xs font-bold text-(--text-secondary) border border-(--border) rounded-xl px-3 py-2.5">{t.nav.lang}</button>
          </div>
          {isAuthenticated ? (
            <div className="grid grid-cols-2 gap-2 pt-1">
              <Link to="/profile" className="text-center text-xs font-bold text-(--text-secondary) border border-(--border) rounded-xl px-3 py-2.5" onClick={closeMenu}>{t.nav.profile}</Link>
              <button type="button" onClick={() => { logout(); closeMenu(); }} className="text-xs font-bold text-rose-600 border border-rose-200 dark:border-rose-900 rounded-xl px-3 py-2.5">{t.nav.signOut}</button>
              <Link to={wsLink} className="col-span-2 text-center text-xs font-bold text-(--on-brand) bg-(--brand) rounded-xl px-3 py-2.5" onClick={closeMenu}>{wsLabel}</Link>
            </div>
          ) : (
            <div className="pt-1">
              <Link to="/login" className="block text-center text-xs font-bold text-(--on-brand) bg-(--brand) rounded-xl px-3 py-2.5" onClick={closeMenu}>{t.nav.login}</Link>
            </div>
          )}
        </div>
      )}
    </header>
  );
}
