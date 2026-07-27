import { Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import AnimateIn from '../../components/AnimateIn';

export default function CtaSection({ t }) {
  const { isAuthenticated } = useAuth();

  return (
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
  );
}
