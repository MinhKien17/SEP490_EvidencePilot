import { useTranslation } from 'react-i18next';
import CompiledPaperPreview from '../../components/CompiledPaperPreview.jsx';

export default function FullPaperPreview({
  paperId,
  paperTitle,
  sectionId,
  contentTex,
  canOverrideSection,
  onClose,
}) {
  const { t } = useTranslation();

  return (
    <div className="fixed inset-0 z-50 flex bg-slate-950/65 p-3 backdrop-blur-sm sm:p-6">
      <div className="m-auto flex h-full max-h-[94vh] w-full max-w-[1500px] flex-col overflow-hidden rounded-2xl border border-(--border) bg-(--surface) shadow-2xl">
        <div className="flex h-12 shrink-0 items-center justify-between border-b border-(--border) px-4">
          <div className="min-w-0">
            <h2 className="truncate text-sm font-bold text-(--text-primary)">{t('previewFullPaper')}</h2>
            <p className="truncate text-[10px] text-(--text-tertiary)">{paperTitle}</p>
          </div>
          <button onClick={onClose} className="rounded-lg border border-(--border) px-3 py-1.5 text-xs font-semibold text-(--text-secondary) hover:bg-(--surface-secondary)">
            {t('close')}
          </button>
        </div>
        <CompiledPaperPreview
          paperId={paperId}
          sectionId={canOverrideSection ? sectionId : null}
          contentTex={contentTex}
        />
      </div>
    </div>
  );
}
