import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

export default function NotFound() {
  const { t } = useTranslation();

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 px-6">
      <div className="text-center max-w-md">
        <p className="text-6xl font-bold text-gray-900 mb-4">404</p>
        <h1 className="text-2xl font-semibold text-gray-900 mb-2">{t('notFound')}</h1>
        <p className="text-gray-600 mb-8">{t('notFoundMessage')}</p>
        <Link
          to="/"
          className="inline-block px-6 py-2.5 rounded-lg bg-blue-600 text-white font-medium hover:bg-blue-700"
        >
          {t('backToHome')}
        </Link>
      </div>
    </div>
  );
}
