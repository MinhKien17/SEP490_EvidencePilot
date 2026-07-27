import { Link } from 'react-router-dom';
import { useLanguage } from '../../context/LanguageContext';

export default function FooterSection({ t }) {
  const { toggleLanguage } = useLanguage();

  return (
    <footer className="bg-[#0f172a] text-gray-400 py-12">
      <div className="max-w-6xl mx-auto px-6">
        <div className="flex flex-col md:flex-row items-center justify-between gap-6 mb-8">
          <Link to="/" className="flex items-center gap-2.5">
            <div className="w-7 h-7 bg-indigo-500 rounded-lg flex items-center justify-center">
              <span className="text-white font-bold text-xs">EP</span>
            </div>
            <span className="font-bold text-white">Evidence Pilot</span>
          </Link>
          <div className="flex items-center gap-6 text-xs">
            <span className="text-gray-600 cursor-default">{t.footer.contact}</span>
            <button onClick={toggleLanguage} className="text-xs font-bold text-gray-500 hover:text-white transition px-2 py-1 border border-gray-700 rounded-lg">{t.nav.lang}</button>
          </div>
        </div>
        <p className="text-xs text-center text-gray-600">{t.footer.tagline}</p>
        <p className="text-xs text-center text-gray-600 mt-1">&copy; 2026 {t.footer.copyright}</p>
      </div>
    </footer>
  );
}
