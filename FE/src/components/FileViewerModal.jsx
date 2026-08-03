import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import api from '../api.js';

export default function FileViewerModal({ fileUrl, fileName, onClose }) {
  const { t } = useTranslation();
  const [loadError, setLoadError] = useState(false);
  const fullUrl = fileUrl.startsWith('http') ? fileUrl : `${api.defaults.baseURL}${fileUrl}`;

  const handleDownload = () => {
    const link = document.createElement('a');
    link.href = fullUrl;
    link.download = fileName || 'document';
    link.click();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/60 backdrop-blur-sm p-4" onClick={onClose}>
      <div
        className="bg-(--surface) border border-(--border) rounded-xl shadow-2xl w-full max-w-4xl h-[90vh] flex flex-col overflow-hidden"
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
        aria-label={fileName || t('fileViewer')}
      >
        <div className="flex items-center justify-between px-4 sm:px-6 py-4 border-b border-(--border) shrink-0">
          <h3 className="font-semibold text-(--text-primary) truncate text-sm max-w-[60%] sm:max-w-[70%]">
            {fileName || t('fileViewer')}
          </h3>
          <div className="flex items-center gap-2">
            <button
              onClick={handleDownload}
              className="px-3 py-2 text-xs font-semibold text-(--text-secondary) bg-(--surface-secondary) border border-(--border) rounded-lg hover:bg-(--surface-tertiary) transition-colors flex items-center gap-1.5"
            >
              <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
              </svg>
              <span className="hidden sm:inline">{t('download')}</span>
            </button>
            <button onClick={onClose} className="p-2 text-(--text-tertiary) hover:text-(--text-primary) hover:bg-(--surface-secondary) rounded-lg transition-colors" aria-label={t('close')}>
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
        </div>

        <div className="flex-1 min-h-0 bg-(--surface-secondary) relative">
          {loadError ? (
            <div className="absolute inset-0 flex flex-col items-center justify-center text-(--text-secondary) p-8 text-center">
              <svg className="w-12 h-12 mb-4 text-(--text-tertiary)" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
              </svg>
              <p className="font-medium mb-1">{t('previewNotAvailable')}</p>
              <p className="text-sm text-(--text-tertiary) mb-4">{t('previewUnsupported')}</p>
              <button
                onClick={handleDownload}
                className="px-5 py-2 bg-(--brand) text-(--on-brand) rounded-lg font-semibold hover:bg-(--brand-hover) transition-colors text-sm"
              >
                {t('downloadFile')}
              </button>
            </div>
          ) : (
            <iframe
              src={fullUrl}
              title={fileName || t('filePreview')}
              className="w-full h-full border-0"
              onError={() => setLoadError(true)}
            />
          )}
        </div>
      </div>
    </div>
  );
}
