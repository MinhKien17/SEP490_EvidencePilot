import { useState, useEffect, useRef } from 'react';
import api from '../../api';
import AnimateIn from '../../components/AnimateIn';

function AnimatedCounter({ target, suffix = '', label }) {
  const ref = useRef(null);
  const [count, setCount] = useState(0);

  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    const obs = new IntersectionObserver(([e]) => {
      if (!e.isIntersecting) return;
      obs.unobserve(el);
      const duration = 1200;
      const steps = 40;
      const increment = Math.ceil(target / steps);
      let current = 0;
      const timer = setInterval(() => {
        current += increment;
        if (current >= target) {
          setCount(target);
          clearInterval(timer);
        } else {
          setCount(current);
        }
      }, duration / steps);
    }, { threshold: 0.3 });
    obs.observe(el);
    return () => obs.disconnect();
  }, [target]);

  return (
    <div ref={ref} className="text-center">
      <div className="text-4xl md:text-5xl font-bold text-[#1e3a8a] mb-2">
        {Intl.NumberFormat().format(count)}{suffix}
      </div>
      <div className="text-sm text-gray-500 font-medium">{label}</div>
    </div>
  );
}

export default function StatsSection({ t }) {
  const [stats, setStats] = useState(null);

  useEffect(() => {
    api.get('/api/public/stats')
      .then(r => setStats(r.data))
      .catch(() => setStats({ totalUsers: 0, totalProjects: 0, totalDocuments: 0 }));
  }, []);

  return (
    <section className="py-20 bg-gradient-to-br from-indigo-50 to-blue-50 border-t border-gray-100">
      <div className="max-w-6xl mx-auto px-6">
        <AnimateIn>
          <h2 className="text-2xl md:text-3xl font-bold text-center text-gray-900 mb-3">{t.stats.heading}</h2>
          <p className="text-gray-500 text-center mb-16 max-w-lg mx-auto">{t.stats.subheading}</p>
        </AnimateIn>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-12">
          <AnimateIn delay={100}>
            <AnimatedCounter target={stats?.totalUsers ?? 0} label={t.stats.usersLabel} />
          </AnimateIn>
          <AnimateIn delay={200}>
            <AnimatedCounter target={stats?.totalProjects ?? 0} label={t.stats.projectsLabel} />
          </AnimateIn>
          <AnimateIn delay={300}>
            <AnimatedCounter target={stats?.totalDocuments ?? 0} label={t.stats.documentsLabel} />
          </AnimateIn>
        </div>
      </div>
    </section>
  );
}
