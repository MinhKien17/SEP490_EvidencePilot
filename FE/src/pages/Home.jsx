import { useState, useEffect, useRef, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useLanguage } from '../context/LanguageContext';
import { homeText } from '../locales/home';
import api from '../api';

const roles = [
  { key: 'student', icon: 'M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zM7.07 18.28c.43-.9 3.05-1.78 4.93-1.78s4.5.88 4.93 1.78C15.57 19.36 13.86 20 12 20s-3.57-.64-4.93-1.72zm11.29-1.45c-1.43-1.74-4.9-2.33-6.36-2.33s-4.93.59-6.36 2.33A7.95 7.95 0 014 12c0-4.41 3.59-8 8-8s8 3.59 8 8c0 1.82-.62 3.49-1.64 4.83zM12 6c-1.94 0-3.5 1.56-3.5 3.5S10.06 13 12 13s3.5-1.56 3.5-3.5S13.94 6 12 6z' },
  { key: 'instructor', icon: 'M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H5.17L4 17.17V4h16v12zm-5-5c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm-4 4h8v-1c0-1.33-2.67-2-4-2s-4 .67-4 2v1z' },
  { key: 'researcher', icon: 'M9 3L5 6.99h3V14h2V6.99h3L9 3zm7 14.01V10h-2v7.01h-3L15 21l4-3.99h-3z' },
];

const workflowSteps = [
  { key: 'step1', icon: 'M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 17.93c-3.95-.49-7-3.85-7-7.93 0-.62.08-1.21.21-1.79L9 15v1c0 1.1.9 2 2 2v1.93zm6.9-2.54c-.26-.81-1-1.39-1.9-1.39h-1v-3c0-.55-.45-1-1-1H8v-2h2c.55 0 1-.45 1-1V7h2c1.1 0 2-.9 2-2v-.41c2.93 1.19 5 4.06 5 7.41 0 2.08-.8 3.97-2.1 5.39z' },
  { key: 'step2', icon: 'M20 6h-8l-2-2H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2zm-6 10H6v-2h8v2zm4-4H6v-2h12v2z' },
  { key: 'step3', icon: 'M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-5 14H7v-2h7v2zm3-4H7v-2h10v2zm0-4H7V7h10v2z' },
  { key: 'step4', icon: 'M21.99 4c0-1.1-.89-2-1.99-2H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h14l4 4-.01-18zM18 14H6v-2h12v2zm0-3H6V9h12v2zm0-3H6V6h12v2z' },
];

const featuresList = [
  'structuredData', 'claimTracking', 'feedback', 'aiExtraction', 'vectorSearch', 'realtime'
];

function AnimateIn({ children, delay = 0, className = '' }) {
  const ref = useRef(null);
  const [visible, setVisible] = useState(false);
  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    const obs = new IntersectionObserver(([e]) => { if (e.isIntersecting) { setVisible(true); obs.unobserve(el); } }, { threshold: 0.1 });
    obs.observe(el);
    return () => obs.disconnect();
  }, []);
  return (
    <div ref={ref} className={`transition-all duration-700 ease-out ${visible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-8'} ${className}`} style={{ transitionDelay: `${delay}ms` }}>
      {children}
    </div>
  );
}

function SvgIcon({ path, className = 'w-6 h-6' }) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="currentColor">
      <path d={path} />
    </svg>
  );
}

function RoleCard({ role, t, index }) {
  const r = role;
  return (
    <AnimateIn delay={100 * index} className="group">
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-8 h-full hover:shadow-lg hover:border-indigo-100 transition-all duration-300">
        <div className="w-12 h-12 bg-indigo-50 text-[#1e3a8a] rounded-xl flex items-center justify-center mb-5 group-hover:bg-[#1e3a8a] group-hover:text-white transition-all duration-300">
          <SvgIcon path={r.icon} className="w-6 h-6" />
        </div>
        <h3 className="text-lg font-bold text-gray-900 mb-3">{t.roles[r.key].title}</h3>
        <p className="text-sm text-gray-600 leading-relaxed">{t.roles[r.key].desc}</p>
      </div>
    </AnimateIn>
  );
}

function WorkflowStep({ step, t, index }) {
  return (
    <AnimateIn delay={100 * index} className="flex-1 min-w-[200px]">
      <div className="flex flex-col items-center text-center">
        <div className="relative mb-4">
          <div className="w-16 h-16 bg-[#1e3a8a] text-white rounded-2xl flex items-center justify-center shadow-lg shadow-indigo-200">
            <SvgIcon path={step.icon} className="w-7 h-7" />
          </div>
          <div className="absolute -top-2 -right-2 w-7 h-7 bg-amber-400 text-white rounded-full flex items-center justify-center text-xs font-bold shadow">
            {index + 1}
          </div>
        </div>
        <h4 className="font-bold text-gray-900 mb-2">{t.workflow[step.key].title}</h4>
        <p className="text-sm text-gray-500 leading-relaxed max-w-[220px]">{t.workflow[step.key].desc}</p>
      </div>
    </AnimateIn>
  );
}

function FeatureCard({ feature, t, index }) {
  const f = t.features[feature];
  const colors = ['from-blue-50 to-indigo-50', 'from-amber-50 to-yellow-50', 'from-emerald-50 to-teal-50', 'from-purple-50 to-pink-50', 'from-cyan-50 to-sky-50', 'from-rose-50 to-orange-50'];
  const dots = ['bg-blue-500', 'bg-amber-500', 'bg-emerald-500', 'bg-purple-500', 'bg-cyan-500', 'bg-rose-500'];
  return (
    <AnimateIn delay={80 * index}>
      <div className={`bg-gradient-to-br ${colors[index % colors.length]} rounded-2xl p-6 border border-gray-100/50 hover:shadow-md transition-all duration-300`}>
        <div className={`w-3 h-3 rounded-full ${dots[index % dots.length]} mb-4`} />
        <h3 className="font-bold text-gray-900 mb-2">{f.title}</h3>
        <p className="text-sm text-gray-600 leading-relaxed">{f.desc}</p>
      </div>
    </AnimateIn>
  );
}

function StatusSection({ t }) {
  const [health, setHealth] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const ctrl = new AbortController();
    api.get('/api/health', { signal: ctrl.signal })
      .then(r => setHealth(r.data))
      .catch(() => setHealth(null))
      .finally(() => setLoading(false));
    return () => ctrl.abort();
  }, []);

  return (
    <section className="py-16 bg-white border-t border-gray-100">
      <div className="max-w-6xl mx-auto px-6">
        <AnimateIn>
          <h2 className="text-2xl font-bold text-center text-gray-900 mb-8">{t.status.heading}</h2>
        </AnimateIn>
        <AnimateIn delay={150}>
          <div className="max-w-lg mx-auto bg-gray-50 rounded-2xl p-6 border border-gray-100 text-center">
            {loading ? (
              <div className="flex flex-col items-center gap-3">
                <div className="w-8 h-8 border-4 border-indigo-200 border-t-indigo-600 rounded-full animate-spin" />
                <span className="text-sm text-gray-400">{t.status.checking}</span>
              </div>
            ) : health ? (
              <div className="space-y-3">
                <div className="flex items-center justify-center gap-2 text-emerald-600">
                  <span className="w-3 h-3 bg-emerald-400 rounded-full animate-pulse" />
                  <span className="font-bold text-sm">{t.status.online}</span>
                </div>
                <div className="grid grid-cols-2 gap-3 text-xs text-gray-500">
                  <div className="flex items-center gap-2 bg-white rounded-lg px-3 py-2 border border-gray-100">
                    <span className="w-2 h-2 rounded-full bg-emerald-400" />
                    <span>{t.status.backend} <span className="font-semibold text-emerald-600">UP</span></span>
                  </div>
                  <div className="flex items-center gap-2 bg-white rounded-lg px-3 py-2 border border-gray-100">
                    <span className={`w-2 h-2 rounded-full ${health.aiService ? 'bg-emerald-400' : 'bg-rose-400'}`} />
                    <span>{t.status.ai} <span className={`font-semibold ${health.aiService ? 'text-emerald-600' : 'text-rose-600'}`}>{health.aiService ? 'UP' : 'DOWN'}</span></span>
                  </div>
                </div>
              </div>
            ) : (
              <div className="flex items-center justify-center gap-2 text-rose-500">
                <span className="w-3 h-3 bg-rose-400 rounded-full" />
                <span className="font-bold text-sm">{t.status.offline}</span>
              </div>
            )}
          </div>
        </AnimateIn>
      </div>
    </section>
  );
}

export default function Home() {
  const { isAuthenticated, role, logout } = useAuth();
  const { language, toggleLanguage } = useLanguage();
  const t = homeText[language];
  const [menuOpen, setMenuOpen] = useState(false);

  const wsLink = !isAuthenticated ? '/login' :
    role === 'ADMIN' ? '/admin/dashboard' :
    role === 'INSTRUCTOR' ? '/instructor/dashboard' :
    '/student/projects';

  const wsLabel = !isAuthenticated ? t.nav.login : t.nav.workspace;

  return (
    <div className="min-h-screen bg-[#fcfcfc] text-[#333] font-sans">
      {/* Header */}
      <header className="fixed top-0 left-0 right-0 z-50 bg-white/80 backdrop-blur-md border-b border-gray-100/80">
        <div className="max-w-7xl mx-auto px-6 h-16 flex items-center justify-between">
          <Link to="/" className="flex items-center gap-2.5">
            <div className="w-8 h-8 bg-[#1e3a8a] rounded-lg flex items-center justify-center">
              <span className="text-white font-bold text-sm">EP</span>
            </div>
            <span className="font-bold text-gray-900 hidden sm:inline">Evidence Pilot</span>
          </Link>

          <nav className="hidden md:flex items-center gap-6 text-sm">
            <Link to="/" className="text-gray-600 hover:text-[#1e3a8a] font-medium transition">{t.nav.home}</Link>
            <a href="#features" className="text-gray-600 hover:text-[#1e3a8a] font-medium transition">{t.nav.features}</a>
            <a href="#about" className="text-gray-600 hover:text-[#1e3a8a] font-medium transition">{t.nav.about}</a>
            <button onClick={toggleLanguage} className="text-xs font-bold text-gray-400 hover:text-[#1e3a8a] transition px-2 py-1 border border-gray-200 rounded-lg">{t.nav.lang}</button>
            {isAuthenticated ? (
              <button onClick={logout} className="text-xs font-bold text-gray-500 hover:text-rose-600 transition px-3 py-1.5 border border-gray-200 rounded-lg">{t.nav.signOut}</button>
            ) : (
              <Link to="/register" className="text-xs font-bold text-white bg-[#1e3a8a] hover:bg-[#1e40af] transition px-4 py-1.5 rounded-lg shadow-sm">{t.nav.register}</Link>
            )}
            <Link to={wsLink} className="text-xs font-bold text-[#1e3a8a] bg-indigo-50 hover:bg-indigo-100 transition px-4 py-1.5 rounded-lg">{wsLabel}</Link>
          </nav>

          <button className="md:hidden p-2 text-gray-600" onClick={() => setMenuOpen(!menuOpen)}>
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              {menuOpen ? <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" /> : <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />}
            </svg>
          </button>
        </div>
        {menuOpen && (
          <div className="md:hidden bg-white border-t border-gray-100 px-6 py-4 space-y-3 text-sm">
            <Link to="/" className="block text-gray-600 font-medium" onClick={() => setMenuOpen(false)}>{t.nav.home}</Link>
            <a href="#features" className="block text-gray-600 font-medium" onClick={() => setMenuOpen(false)}>{t.nav.features}</a>
            <a href="#about" className="block text-gray-600 font-medium" onClick={() => setMenuOpen(false)}>{t.nav.about}</a>
            <button onClick={() => { toggleLanguage(); setMenuOpen(false); }} className="block text-xs font-bold text-gray-400 border border-gray-200 rounded-lg px-3 py-1.5">{t.nav.lang}</button>
            {isAuthenticated ? (
              <button onClick={() => { logout(); setMenuOpen(false); }} className="block text-xs font-bold text-rose-600 border border-gray-200 rounded-lg px-3 py-1.5">{t.nav.signOut}</button>
            ) : (
              <div className="flex gap-2">
                <Link to="/login" className="flex-1 text-center text-xs font-bold text-gray-600 border border-gray-200 rounded-lg px-3 py-1.5" onClick={() => setMenuOpen(false)}>{t.nav.login}</Link>
                <Link to="/register" className="flex-1 text-center text-xs font-bold text-white bg-[#1e3a8a] rounded-lg px-3 py-1.5" onClick={() => setMenuOpen(false)}>{t.nav.register}</Link>
              </div>
            )}
          </div>
        )}
      </header>

      {/* Hero */}
      <section className="relative pt-32 pb-24 md:pt-40 md:pb-32 overflow-hidden">
        <div className="absolute inset-0 bg-gradient-to-br from-indigo-50 via-white to-blue-50" />
        <div className="absolute top-0 left-1/2 -translate-x-1/2 w-[800px] h-[800px] bg-gradient-to-br from-indigo-200/20 to-blue-200/10 rounded-full blur-3xl" />
        <div className="relative z-10 max-w-5xl mx-auto px-6 text-center">
          <AnimateIn>
            <div className="inline-flex items-center gap-2 bg-indigo-50 border border-indigo-100 rounded-full px-4 py-1.5 mb-8">
              <span className="w-2 h-2 bg-emerald-400 rounded-full animate-pulse" />
              <span className="text-xs font-semibold text-indigo-700">{t.hero.stats}</span>
            </div>
          </AnimateIn>
          <AnimateIn delay={100}>
            <h1 className="text-4xl md:text-6xl lg:text-7xl font-light text-gray-900 leading-tight mb-6">
              {t.hero.titleStart}{' '}
              <span className="font-bold bg-gradient-to-r from-[#1e3a8a] to-indigo-500 bg-clip-text text-transparent">
                {t.hero.titleHighlight}
              </span>
              <br className="hidden md:block" />
              {t.hero.titleEnd}
            </h1>
          </AnimateIn>
          <AnimateIn delay={200}>
            <p className="text-lg text-gray-500 leading-relaxed max-w-2xl mx-auto mb-10">
              {t.hero.subtitle}
            </p>
          </AnimateIn>
          <AnimateIn delay={300}>
            <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
              {isAuthenticated ? (
                <Link to={wsLink} className="bg-[#1e3a8a] hover:bg-[#1e40af] text-white px-8 py-3.5 rounded-xl font-semibold shadow-lg shadow-indigo-200 transition-all duration-300 hover:shadow-xl hover:-translate-y-0.5">
                  {t.nav.workspace}
                </Link>
              ) : (
                <>
                  <Link to="/register" className="bg-[#1e3a8a] hover:bg-[#1e40af] text-white px-8 py-3.5 rounded-xl font-semibold shadow-lg shadow-indigo-200 transition-all duration-300 hover:shadow-xl hover:-translate-y-0.5">
                    {t.hero.cta}
                  </Link>
                  <Link to="/login" className="text-sm font-medium text-gray-500 hover:text-[#1e3a8a] transition border border-gray-200 px-6 py-3.5 rounded-xl">
                    {t.nav.login}
                  </Link>
                </>
              )}
            </div>
            <p className="text-xs text-gray-400 mt-4">{t.hero.ctaSub}</p>
          </AnimateIn>
        </div>
      </section>

      {/* Roles */}
      <section className="py-20 bg-white">
        <div className="max-w-6xl mx-auto px-6">
          <AnimateIn>
            <h2 className="text-2xl md:text-3xl font-bold text-center text-gray-900 mb-12">{t.roles.heading}</h2>
          </AnimateIn>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {roles.map((role, i) => <RoleCard key={role.key} role={role} t={t} index={i} />)}
          </div>
        </div>
      </section>

      {/* Workflow */}
      <section className="py-20 bg-gray-50">
        <div className="max-w-6xl mx-auto px-6">
          <AnimateIn>
            <h2 className="text-2xl md:text-3xl font-bold text-center text-gray-900 mb-16">{t.workflow.heading}</h2>
          </AnimateIn>
          <div className="flex flex-wrap justify-center gap-8 md:gap-12">
            {workflowSteps.map((step, i) => <WorkflowStep key={step.key} step={step} t={t} index={i} />)}
          </div>
        </div>
      </section>

      {/* Features */}
      <section id="features" className="py-20 bg-white">
        <div className="max-w-6xl mx-auto px-6">
          <AnimateIn>
            <h2 className="text-2xl md:text-3xl font-bold text-center text-gray-900 mb-3">{t.features.heading}</h2>
            <p className="text-gray-500 text-center mb-12 max-w-xl mx-auto">{t.features.subheading}</p>
          </AnimateIn>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
            {featuresList.map((f, i) => <FeatureCard key={f} feature={f} t={t} index={i} />)}
          </div>
        </div>
      </section>

      {/* System Status */}
      <StatusSection t={t} />

      {/* About */}
      <section id="about" className="py-20 bg-gray-50">
        <div className="max-w-4xl mx-auto px-6 text-center">
          <AnimateIn>
            <h2 className="text-2xl md:text-3xl font-bold text-gray-900 mb-6">{t.about.heading}</h2>
            <p className="text-gray-600 leading-relaxed max-w-2xl mx-auto">{t.about.desc}</p>
          </AnimateIn>
        </div>
      </section>

      {/* CTA */}
      <section className="py-20 bg-gradient-to-br from-[#1e3a8a] to-indigo-700">
        <div className="max-w-3xl mx-auto px-6 text-center">
          <AnimateIn>
            <h2 className="text-3xl md:text-4xl font-bold text-white mb-4">{t.cta.heading}</h2>
            <p className="text-indigo-200 mb-8 max-w-lg mx-auto">{t.cta.subtitle}</p>
            {!isAuthenticated && (
              <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
                <Link to="/register" className="bg-white text-[#1e3a8a] hover:bg-indigo-50 px-8 py-3.5 rounded-xl font-bold shadow-lg transition-all duration-300 hover:shadow-xl hover:-translate-y-0.5">
                  {t.cta.button}
                </Link>
                <Link to="/login" className="text-sm font-medium text-indigo-200 hover:text-white transition border border-indigo-400 px-6 py-3.5 rounded-xl">
                  {t.cta.login}
                </Link>
              </div>
            )}
          </AnimateIn>
        </div>
      </section>

      {/* Footer */}
      <footer className="bg-[#0f172a] text-gray-400 py-12">
        <div className="max-w-6xl mx-auto px-6">
          <div className="flex flex-col md:flex-row items-center justify-between gap-6 mb-8">
            <div className="flex items-center gap-2.5">
              <div className="w-7 h-7 bg-indigo-500 rounded-lg flex items-center justify-center">
                <span className="text-white font-bold text-xs">EP</span>
              </div>
              <span className="font-bold text-white">Evidence Pilot</span>
            </div>
            <div className="flex items-center gap-6 text-xs">
              <a href="#" className="hover:text-white transition">{t.footer.privacy}</a>
              <a href="#" className="hover:text-white transition">{t.footer.terms}</a>
              <a href="#" className="hover:text-white transition">{t.footer.contact}</a>
              <button onClick={toggleLanguage} className="text-xs font-bold text-gray-500 hover:text-white transition px-2 py-1 border border-gray-700 rounded-lg">{t.nav.lang}</button>
            </div>
          </div>
          <p className="text-xs text-center text-gray-600">{t.footer.tagline}</p>
          <p className="text-xs text-center text-gray-600 mt-1">&copy; 2026 {t.footer.copyright}</p>
        </div>
      </footer>
    </div>
  );
}
