import { Link } from 'react-router-dom';
import { UI_TEXT } from '../../constants/uiText';

export default function WorkspaceHeader({ projects, project, navigate, feedbacks, toggleLanguage, language, setShowHistoryModal, setShowReviseModal, logout, notifications, unreadCount, showNotifications, setShowNotifications, onMarkNotificationRead, onExportTexArchive }) {
  const t = UI_TEXT[language];
  return (
    <header className="h-14 border-b border-slate-200 bg-white/80 backdrop-blur-md flex items-center justify-between px-4 shrink-0 shadow-sm z-20">
      <div className="flex items-center gap-4">
        <Link to="/" className="p-1.5 hover:bg-slate-100 rounded-lg text-slate-500 transition-colors">
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
          </svg>
        </Link>
          <div className="flex items-center gap-3">
          <div className="w-7 h-7 bg-indigo-600 text-white rounded-md text-xs flex items-center justify-center font-bold shadow-sm shadow-indigo-200">EP</div>
          {projects.length > 0 ? (
            <div className="flex items-center gap-2">
              <select id="project-selector"
                value={project?.id || ''}
                onChange={(e) => navigate(`/student/projects/${e.target.value}`)}
                className="bg-white border border-slate-200 rounded-lg px-2 py-1 text-xs font-semibold text-slate-800 outline-none focus:ring-1 focus:ring-indigo-500 max-w-[200px]"
              >
                {projects.map((p) => (
                  <option key={p.id} value={p.id}>{p.title}</option>
                ))}
              </select>
            </div>
          ) : (
            <span className="text-xs font-semibold text-slate-500">{t.noProjects}</span>
          )}
        </div>
      </div>
      <div className="flex items-center gap-3">
        <div className="hidden md:block text-xs text-slate-400 mr-2 font-medium">{t.workspaceDescription}</div>
        <div className="flex gap-1.5 bg-rose-50 border border-rose-100 rounded-full px-1 py-1">
          <span className="text-[11px] px-2 py-0.5 text-rose-700 font-semibold rounded-full bg-white shadow-sm">{t.returnedWithFeedback}</span>
          <span className="text-[11px] px-2 py-0.5 text-rose-700 font-semibold rounded-full bg-white shadow-sm flex items-center gap-1">
            <div className="w-1.5 h-1.5 rounded-full bg-rose-500 animate-pulse"></div>
            {feedbacks.length} {t.feedbacks}
          </span>
        </div>
        <div className="relative">
          <button onClick={() => setShowNotifications(!showNotifications)} className="relative p-2 hover:bg-slate-100 rounded-lg text-slate-500 transition-colors" title="Notifications">
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" /></svg>
            {unreadCount > 0 && <span className="absolute -top-0.5 -right-0.5 bg-rose-500 text-white text-[9px] font-bold w-4 h-4 flex items-center justify-center rounded-full shadow-sm">{unreadCount > 9 ? '9+' : unreadCount}</span>}
          </button>
          {showNotifications && (
            <div className="absolute right-0 top-full mt-2 w-80 bg-white border border-slate-200 rounded-xl shadow-xl z-50 max-h-96 overflow-y-auto animate-in fade-in slide-in-from-top-2 duration-200">
              <div className="sticky top-0 bg-white border-b border-slate-100 px-4 py-2.5 flex justify-between items-center">
                <span className="text-xs font-bold text-slate-700">Notifications</span>
                <button onClick={() => setShowNotifications(false)} className="text-slate-400 hover:text-slate-600"><svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" /></svg></button>
              </div>
              {notifications.length === 0 ? (
                <div className="text-xs text-slate-400 italic text-center py-8">No notifications yet.</div>
              ) : notifications.map(n => (
                <div key={n.id} onClick={() => { if (!n.read) onMarkNotificationRead(n.id); }} className={`px-4 py-3 border-b border-slate-100 cursor-pointer hover:bg-slate-50 transition-colors ${n.read ? 'opacity-60' : 'bg-indigo-50/30'}`}>
                  <p className="text-xs font-semibold text-slate-800">{n.message || n.title || 'Notification'}</p>
                  <p className="text-[10px] text-slate-400 mt-0.5">{n.createdAt ? new Date(n.createdAt).toLocaleString() : ''}</p>
                </div>
              ))}
            </div>
          )}
        </div>
        <button onClick={() => setShowHistoryModal(true)} className="flex items-center gap-1.5 text-xs font-semibold text-slate-600 hover:text-slate-900 border border-slate-200 px-3 py-1.5 rounded-lg hover:bg-slate-50 transition-all shadow-sm ml-2">
          <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          {t.history}
        </button>
        <button onClick={() => setShowReviseModal(true)} className="text-xs font-semibold text-white bg-indigo-600 hover:bg-indigo-700 px-4 py-1.5 rounded-lg flex items-center gap-1.5 shadow-md shadow-indigo-600/20 transition-all hover:shadow-indigo-600/40 transform hover:-translate-y-0.5">
          <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
          </svg>
          {t.revise}
        </button>
        <button onClick={onExportTexArchive} className="text-xs font-semibold text-emerald-600 border border-emerald-200 px-2.5 py-1.5 rounded-lg hover:bg-emerald-50 transition-all ml-1" title="Export .tex archive">
          <svg className="w-3.5 h-3.5 inline mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" /></svg>
          Export .tex
        </button>
        <button onClick={toggleLanguage} className="text-xs font-semibold text-slate-600 border border-slate-200 px-2.5 py-1.5 rounded-lg hover:bg-slate-100 transition-all ml-1" title={language === 'vi' ? 'Switch to English' : 'Chuyển sang Tiếng Việt'}>
          {language === 'vi' ? 'EN' : 'VI'}
        </button>
        <button onClick={() => { logout(); navigate('/'); }} className="text-xs font-medium text-slate-500 hover:text-red-600 border border-slate-200 px-3 py-1.5 rounded-lg hover:border-red-200 hover:bg-red-50 transition-all ml-1">
          {t.signOut}
        </button>
      </div>
    </header>
  );
}
