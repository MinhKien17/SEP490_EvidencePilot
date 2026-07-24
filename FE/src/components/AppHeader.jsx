import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useLanguage } from '../context/LanguageContext';
import { instructorText } from '../locales';

export default function AppHeader() {
  const navigate = useNavigate();
  const location = useLocation();
  const { role, logout } = useAuth();
  const { language, toggleLanguage } = useLanguage();
  const t = instructorText[language];

  const roleLinks = role === 'INSTRUCTOR'
    ? [
        { label: t.dashboard, path: '/instructor/dashboard' },
        { label: t.requests, path: '/instructor/requests' },
        { label: t.collections, path: '/instructor/collections' },
        { label: t.projects, path: '/instructor/projects' },
      ]
    : role === 'ADMIN'
      ? [{ label: 'Dashboard', path: '/admin/dashboard' }]
      : [{ label: 'Projects', path: '/student/projects' }];

  return (
    <header className="bg-[#1e3a8a] text-white px-6 h-14 flex items-center justify-between shadow-sm shrink-0">
      <div className="flex items-center gap-3 min-w-0">
        <div className="w-7 h-7 bg-indigo-500 rounded-md text-xs flex items-center justify-center font-bold shrink-0">EP</div>
        <span className="font-bold text-sm tracking-wider truncate">Evidence Pilot</span>
        <nav className="hidden md:flex items-center gap-1 ml-4">
          {roleLinks.map(link => {
            const active = location.pathname === link.path;
            return (
              <button key={link.path} onClick={() => navigate(link.path)}
                className={`text-xs font-medium px-2 py-1 rounded transition relative ${active ? 'text-white' : 'text-blue-200 hover:text-white'}`}>
                {link.label}
                {active && <span className="absolute -bottom-1 left-1/2 -translate-x-1/2 w-3/4 h-0.5 bg-white/80 rounded-full" />}
              </button>
            );
          })}
        </nav>
      </div>
      <div className="flex items-center gap-3 shrink-0">
        <button onClick={toggleLanguage}
          className="text-[11px] font-bold text-blue-200 border border-blue-400/30 px-2 py-1 rounded hover:bg-blue-800/30 transition">
          {language === 'vi' ? 'EN' : 'VI'}
        </button>
        <button onClick={() => navigate('/profile')}
          className="text-xs font-medium text-blue-200 hover:text-white transition">
          Profile
        </button>
        <button onClick={() => { logout(); navigate('/'); }}
          className="text-xs font-medium text-blue-200 hover:text-white transition">
          Sign Out
        </button>
      </div>
    </header>
  );
}
