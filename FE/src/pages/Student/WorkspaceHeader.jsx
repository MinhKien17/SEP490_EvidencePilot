import { Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { useTranslation } from 'react-i18next';

export default function WorkspaceHeader({ project, navigate, notifications, unreadCount, showNotifications, setShowNotifications, onMarkNotificationRead, historyDisabled, handleRunAiReview, loadingAiReview, selectedPaper, onShowHistory, showExportMenu, setShowExportMenu, handleExportTexArchive, handleExportTraceabilityJson, handleExportTraceabilityCsv, handleExportGraphCsv }) {
  const { user } = useAuth();
  const { theme, toggleTheme } = useTheme();
  const { t, i18n } = useTranslation();
  const toggleLang = () => i18n.changeLanguage(i18n.language === 'en' ? 'vi' : 'en');

  return (
    <header className="h-14 border-b border-(--border) bg-(--header-bg) backdrop-blur-md flex items-center px-4 shrink-0 shadow-sm relative z-50">
      <div className="flex items-center gap-3 shrink-0">
        <Link to="/student/projects" data-tour="header-back" className="p-1.5 hover:bg-slate-100 dark:hover:bg-(--surface-tertiary) rounded-lg text-(--text-secondary) transition-colors">
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
          </svg>
        </Link>
        <div data-tour="header-logo" className="w-7 h-7 bg-indigo-600 text-white rounded-md text-xs flex items-center justify-center font-bold shadow-sm shadow-indigo-200 shrink-0">EP</div>
      </div>

      <div className="flex-1 flex justify-center">
        <span data-tour="header-project-name" className="text-sm font-bold text-(--text-primary) truncate max-w-[280px]">{project?.title || 'Project'}</span>
      </div>

      <div className="flex items-center gap-1 shrink-0">
        <div className="relative">
          <button data-tour="header-notifications" onClick={() => setShowNotifications(!showNotifications)} className="relative p-2 hover:bg-slate-100 dark:hover:bg-(--surface-tertiary) rounded-lg text-(--text-secondary) transition-colors" title={t('notifications')}>
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" /></svg>
            {unreadCount > 0 && <span className="absolute -top-0.5 -right-0.5 bg-rose-500 text-white text-[9px] font-bold w-4 h-4 flex items-center justify-center rounded-full shadow-sm">{unreadCount > 9 ? '9+' : unreadCount}</span>}
          </button>
          {showNotifications && (
            <div className="absolute right-0 top-full mt-2 w-80 bg-(--surface) border border-(--border) rounded-xl shadow-xl z-[99999] max-h-96 overflow-y-auto animate-in fade-in slide-in-from-top-2 duration-200">
              <div className="sticky top-0 bg-(--surface) border-b border-(--border-light) px-4 py-2.5 flex justify-between items-center">
                <span className="text-xs font-bold text-(--text-primary)">{t('notifications')}</span>
                <button onClick={() => setShowNotifications(false)} className="text-(--text-secondary) hover:text-(--text-primary)"><svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" /></svg></button>
              </div>
              {notifications.length === 0 ? (
                <div className="text-xs text-(--text-tertiary) italic text-center py-8">{t('noNotifications')}</div>
              ) : notifications.map(n => (
                <div key={n.id} onClick={() => { if (!n.read) onMarkNotificationRead(n.id); }} className={`px-4 py-3 border-b border-(--border-light) cursor-pointer hover:bg-(--surface-secondary) transition-colors ${n.read ? 'opacity-60' : 'bg-indigo-50/30 dark:bg-indigo-900/20'}`}>
                  <p className="text-xs font-semibold text-(--text-primary)">{n.message || n.title || t('notifications')}</p>
                  <p className="text-[10px] text-(--text-tertiary) mt-0.5">{n.createdAt ? new Date(n.createdAt).toLocaleString() : ''}</p>
                </div>
              ))}
            </div>
          )}
        </div>

        <button data-tour="header-history" onClick={onShowHistory} disabled={historyDisabled} className="p-2 hover:bg-slate-100 dark:hover:bg-(--surface-tertiary) rounded-lg text-(--text-secondary) transition-colors disabled:opacity-30" title={t('versionHistory')}>
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
        </button>

        <button data-tour="header-dark-mode" onClick={toggleTheme} className="p-2 hover:bg-slate-100 dark:hover:bg-(--surface-tertiary) rounded-lg text-(--text-secondary) transition-colors" title={theme === 'light' ? t('darkMode') : t('lightMode')}>
          {theme === 'light' ? (
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z" /></svg>
          ) : (
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z" /></svg>
          )}
        </button>

        <button data-tour="header-language" onClick={toggleLang} className="px-2 py-1 hover:bg-slate-100 dark:hover:bg-(--surface-tertiary) rounded-lg text-xs font-bold text-(--text-secondary) transition-colors" title={i18n.language === 'en' ? t('switchToVietnamese') : t('switchToEnglish')}>
          {i18n.language === 'en' ? 'VI' : 'EN'}
        </button>

        <button data-tour="header-ai-review" onClick={handleRunAiReview} disabled={loadingAiReview || !selectedPaper} className="bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 text-white px-2.5 py-1.5 rounded-md text-xs font-bold flex items-center gap-1 shadow-sm transition-colors" title={t('aiReview')}>
          <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 01-2 2h0a2 2 0 01-2-2v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" /></svg>
          {loadingAiReview ? t('loading') : t('aiReview')}
        </button>

        <div className="relative">
          <button data-tour="header-export" onClick={() => setShowExportMenu(!showExportMenu)} className="bg-emerald-600 hover:bg-emerald-700 text-white px-2.5 py-1.5 rounded-md text-xs font-bold flex items-center gap-1 shadow-sm transition-colors" title={t('export')}>
            <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" /></svg>
            {t('export')}
          </button>
          {showExportMenu && (
            <div className="absolute right-0 top-full mt-2 w-56 bg-(--surface) border border-(--border) rounded-xl shadow-xl z-[99999] py-1 animate-in fade-in slide-in-from-top-2 duration-200">
              <button onClick={() => { handleExportTexArchive(); setShowExportMenu(false); }} className="w-full text-left px-4 py-2.5 text-xs font-semibold text-(--text-primary) hover:bg-(--surface-secondary) flex items-center gap-2 transition-colors">
                <svg className="w-4 h-4 text-slate-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 8h14M5 8a2 2 0 110-4h14a2 2 0 110 4M5 8v10a2 2 0 002 2h10a2 2 0 002-2V8m-9 4h4" /></svg>
                {t('exportTex')}
              </button>
              <button onClick={() => { handleExportTraceabilityJson(); setShowExportMenu(false); }} className="w-full text-left px-4 py-2.5 text-xs font-semibold text-(--text-primary) hover:bg-(--surface-secondary) flex items-center gap-2 transition-colors">
                <svg className="w-4 h-4 text-amber-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" /></svg>
                {t('exportTraceability')}
              </button>
              <button onClick={() => { handleExportTraceabilityCsv(); setShowExportMenu(false); }} className="w-full text-left px-4 py-2.5 text-xs font-semibold text-(--text-primary) hover:bg-(--surface-secondary) flex items-center gap-2 transition-colors">
                <svg className="w-4 h-4 text-emerald-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3 10h18M3 14h18m-9-4v8m-7 0h14a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z" /></svg>
                {t('exportGraphCsv')}
              </button>
            </div>
          )}
        </div>

        <div data-tour="header-avatar" className="w-7 h-7 bg-indigo-600 text-white rounded-full text-xs flex items-center justify-center font-bold shrink-0" title={user?.firstName ? `${user.firstName} ${user.lastName || ''}` : 'Profile'}>
          {user?.firstName?.charAt(0)?.toUpperCase() || user?.email?.charAt(0)?.toUpperCase() || 'U'}
        </div>
      </div>
    </header>
  );
}