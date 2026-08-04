import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useLanguage } from '../context/LanguageContext';
import { useTheme } from '../context/ThemeContext';
import { commonText, instructorText, studentText } from '../locales';

function ThemeIcon({ theme }) {
  return theme === 'light' ? (
    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z" />
    </svg>
  ) : (
    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z" />
    </svg>
  );
}

export default function AppHeader() {
  const navigate = useNavigate();
  const location = useLocation();
  const { role, logout } = useAuth();
  const { language, toggleLanguage } = useLanguage();
  const { theme, toggleTheme } = useTheme();
  const [menuOpen, setMenuOpen] = useState(false);
  const t = instructorText[language];
  const st = studentText[language];
  const ct = commonText[language];

  const roleLinks = role === 'INSTRUCTOR'
    ? [
        { label: t.dashboard, path: '/instructor/dashboard' },
        { label: t.requests, path: '/instructor/requests' },
        { label: t.collections, path: '/instructor/collections' },
        { label: t.projects, path: '/instructor/projects' },
      ]
    : role === 'ADMIN'
      ? [{ label: 'Dashboard', path: '/admin/dashboard' }]
      : [{ label: st.projects, path: '/student/projects' }];

  const isActive = (path) => location.pathname === path
    || (path !== '/instructor/dashboard' && location.pathname.startsWith(`${path}/`));

  const go = (path) => {
    setMenuOpen(false);
    navigate(path);
  };

  const signOut = () => {
    setMenuOpen(false);
    logout();
    navigate('/');
  };

  const themeLabel = theme === 'light' ? ct.darkMode : ct.lightMode;

  return (
    <header className="sticky top-0 z-50 h-16 shrink-0 border-b border-(--header-border) bg-(--header-bg) text-(--text-primary) shadow-sm backdrop-blur-md">
      <div className="h-full max-w-7xl mx-auto px-4 sm:px-6 flex items-center justify-between gap-4">
        <div className="flex items-center gap-3 min-w-0">
          <button type="button" onClick={() => go('/')} className="flex items-center gap-2.5 shrink-0 rounded-lg">
            <span className="w-8 h-8 bg-(--brand) text-(--on-brand) rounded-lg text-xs flex items-center justify-center font-bold">EP</span>
            <span className="hidden sm:inline font-bold text-sm text-(--text-primary) whitespace-nowrap">Evidence Pilot</span>
          </button>

          <nav className="hidden md:flex items-center gap-1 ml-2" aria-label={ct.primaryNavigation}>
            {roleLinks.map(link => (
              <button
                key={link.path}
                type="button"
                onClick={() => go(link.path)}
                className={`text-xs font-semibold px-3 py-2 rounded-lg transition-colors ${isActive(link.path) ? 'bg-(--brand-soft) text-(--brand-foreground)' : 'text-(--text-secondary) hover:bg-(--surface-secondary) hover:text-(--brand-foreground)'}`}
              >
                {link.label}
              </button>
            ))}
          </nav>
        </div>

        <div className="hidden md:flex items-center gap-1.5 shrink-0">
          <button type="button" onClick={toggleTheme} className="p-2 text-(--text-secondary) hover:text-(--brand-foreground) hover:bg-(--surface-secondary) rounded-lg transition-colors" title={themeLabel} aria-label={themeLabel}>
            <ThemeIcon theme={theme} />
          </button>
          <button type="button" onClick={toggleLanguage} className="min-w-9 px-2 py-1.5 text-xs font-bold text-(--text-secondary) border border-(--border) rounded-lg hover:bg-(--surface-secondary) hover:text-(--brand-foreground) transition-colors">
            {language === 'vi' ? 'EN' : 'VI'}
          </button>
          <button type="button" onClick={() => navigate('/profile')} className="px-3 py-1.5 text-xs font-semibold text-(--text-secondary) hover:text-(--brand-foreground) rounded-lg hover:bg-(--surface-secondary) transition-colors">
            {ct.profile}
          </button>
          <button type="button" onClick={signOut} className="px-3 py-1.5 text-xs font-semibold text-(--text-secondary) hover:text-rose-600 rounded-lg hover:bg-rose-50 dark:hover:bg-rose-950/30 transition-colors">
            {ct.signOut}
          </button>
        </div>

        <button
          type="button"
          className="md:hidden p-2 text-(--text-secondary) hover:text-(--brand-foreground) hover:bg-(--surface-secondary) rounded-lg"
          onClick={() => setMenuOpen(value => !value)}
          aria-expanded={menuOpen}
          aria-controls="app-mobile-navigation"
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
        <div id="app-mobile-navigation" className="md:hidden absolute inset-x-0 top-full border-b border-(--border) bg-(--surface) shadow-xl px-4 py-4">
          <nav className="space-y-1" aria-label={ct.mobileNavigation}>
            {roleLinks.map(link => (
              <button
                key={link.path}
                type="button"
                onClick={() => go(link.path)}
                className={`w-full text-left text-sm font-semibold px-3 py-2.5 rounded-xl ${isActive(link.path) ? 'bg-(--brand-soft) text-(--brand-foreground)' : 'text-(--text-secondary) hover:bg-(--surface-secondary)'}`}
              >
                {link.label}
              </button>
            ))}
          </nav>
          <div className="mt-3 pt-3 border-t border-(--border-light) grid grid-cols-2 gap-2">
            <button type="button" onClick={() => { toggleTheme(); setMenuOpen(false); }} className="flex items-center justify-center gap-2 px-3 py-2.5 text-xs font-semibold text-(--text-secondary) border border-(--border) rounded-xl hover:bg-(--surface-secondary)">
              <ThemeIcon theme={theme} /> {themeLabel}
            </button>
            <button type="button" onClick={() => { toggleLanguage(); setMenuOpen(false); }} className="px-3 py-2.5 text-xs font-bold text-(--text-secondary) border border-(--border) rounded-xl hover:bg-(--surface-secondary)">
              {language === 'vi' ? 'EN' : 'VI'}
            </button>
            <button type="button" onClick={() => go('/profile')} className="px-3 py-2.5 text-xs font-semibold text-(--text-secondary) border border-(--border) rounded-xl hover:bg-(--surface-secondary)">
              {ct.profile}
            </button>
            <button type="button" onClick={signOut} className="px-3 py-2.5 text-xs font-semibold text-rose-600 border border-rose-200 dark:border-rose-900 rounded-xl hover:bg-rose-50 dark:hover:bg-rose-950/30">
              {ct.signOut}
            </button>
          </div>
        </div>
      )}
    </header>
  );
}
