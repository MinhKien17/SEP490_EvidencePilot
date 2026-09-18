import { useEffect, useMemo, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import MediaAssetPicker from '../../features/MediaAssetPicker.jsx';
import FeedbackCard from './FeedbackCard.jsx';
import { findOverlaps } from '../../../utils/instructor/feedbackOverlap.js';
import { normalizeSource, selectionLines } from '../../../utils/student/feedbackAnchors.js';

export default function FeedbackThreadsTab({ review, selectedSection, projectId, composerFocusToken = 0 }) {
  const { t } = useTranslation();
  const {
    feedbackItems, activeRequestId, canCreateRoot,
    feedbackDraft, selectedAnchor, editingFeedbackId, updateFeedbackDraft,
    savingFeedback, activeFeedbackId, handleSubmitFeedback, captureSourceSelection,
    handleEditFeedback, handleCancelEdit, handleDeleteFeedback,
    selectFeedback, errorMessage, successMessage,
  } = review;
  const [pendingAttachments, setPendingAttachments] = useState({});
  const [busyId] = useState(null);
  const composerRef = useRef(null);
  useEffect(() => {
    if (composerFocusToken > 0) composerRef.current?.focus();
  }, [composerFocusToken]);
  // ponytail: picker picks keyed by message so composer/reply drafts never mix.
  const pendingKey = editingFeedbackId || 'new';
  // ponytail: human line target, never raw offsets. Create mode arms
  // automatically (see autoCaptureSelection); edit mode keeps the explicit
  // button so reviewing never clobbers a seeded passage.
  const passageLines = useMemo(() => {
    if (!selectedAnchor || !selectedSection) return null;
    return selectionLines(selectedSection.contentTex || '', selectedAnchor.from, selectedAnchor.to);
  }, [selectedAnchor, selectedSection]);
  const passageLabel = !selectedAnchor || !passageLines
    ? t('instructor.review.wholeSection')
    : passageLines.first === passageLines.last
      ? t('instructor.review.selectionLine', { line: passageLines.first })
      : t('instructor.review.selectionLines', { from: passageLines.first, to: passageLines.last });
  const mediaLabels = useMemo(() => ({
    selectMedia: t('instructor.review.addMedia'),
    title: t('instructor.review.mediaTitle'),
    empty: t('instructor.review.mediaEmpty'),
    done: t('instructor.review.mediaDone'),
  }), [t]);

  const threads = useMemo(() => (feedbackItems || [])
    .filter(item => String(item.requestId) === String(activeRequestId)
      && String(item.sectionId) === String(selectedSection?.id))
    .sort((a, b) => String(a.createdAt || '').localeCompare(String(b.createdAt || ''))),
    [feedbackItems, activeRequestId, selectedSection?.id]);

  // ponytail: overlap is valid — warn only, never block. The item under edit
  // is excluded so its own passage is not reported as a duplicate.
  const overlap = useMemo(() => {
    if (!selectedAnchor) return { count: 0, exactDuplicate: false, ids: [] };
    const others = (feedbackItems || []).filter(item => String(item.id) !== String(editingFeedbackId));
    return findOverlaps(selectedAnchor, others, { requestId: activeRequestId, sectionId: selectedSection?.id });
  }, [selectedAnchor, feedbackItems, editingFeedbackId, activeRequestId, selectedSection?.id]);

  const viewFirstOverlap = () => {
    const first = (feedbackItems || []).find(item => String(item.id) === String(overlap.ids[0]));
    if (first) selectFeedback(first);
  };

  const submitThread = async event => {
    event.preventDefault();
    const ids = (pendingAttachments[pendingKey] || []).map(entry => entry.id);
    const ok = await handleSubmitFeedback(event, ids);
    if (ok) setPendingAttachments(prev => ({ ...prev, [pendingKey]: [] }));
  };

  return (
    <div className="space-y-3">
      {errorMessage && <p role="alert" className="text-rose-700">{errorMessage}</p>}
      {successMessage && <p role="status" className="text-emerald-700">{successMessage}</p>}

      {canCreateRoot && (
        <form onSubmit={submitThread} className="space-y-2 rounded-xl border border-(--border-light) bg-(--surface-secondary)/50 p-3">
          <div className="flex flex-wrap items-center gap-2">
            {editingFeedbackId && (
              <button
                type="button"
                onClick={captureSourceSelection}
                disabled={savingFeedback}
                className="rounded-lg bg-teal-600 px-2.5 py-1.5 text-[10px] font-black text-white hover:bg-teal-700 disabled:opacity-50"
              >
                {t('instructor.review.useSelection')}
              </button>
            )}
            <span className="text-[10px] font-semibold text-(--text-secondary)">
              {passageLabel}
            </span>
            {editingFeedbackId && selectedAnchor && (
              <button
                type="button"
                onClick={() => updateFeedbackDraft({ anchor: null })}
                disabled={savingFeedback}
                className="rounded-lg border border-(--border) bg-(--surface) px-2.5 py-1.5 text-[10px] font-bold text-(--text-secondary) hover:bg-(--surface-secondary) disabled:opacity-50"
              >
                {t('instructor.review.removePassage')}
              </button>
            )}
            <span className="ml-auto">
              <MediaAssetPicker
                projectId={projectId}
                labels={mediaLabels}
                value={pendingAttachments[pendingKey] || []}
                onChange={entries => setPendingAttachments(prev => ({ ...prev, [pendingKey]: entries }))}
                disabled={savingFeedback}
              />
            </span>
          </div>
          {selectedAnchor && overlap.count > 0 && (
            <p role="note" className="text-[10px] font-semibold text-(--text-secondary)">
              {overlap.exactDuplicate
                ? t('instructor.review.overlapExact')
                : t('instructor.review.overlapNotice', { count: overlap.count })}{' '}
              <button
                type="button"
                onClick={viewFirstOverlap}
                className="font-black text-teal-700 underline hover:text-teal-800 dark:text-teal-300"
              >
                {t('instructor.review.overlapView')}
              </button>
            </p>
          )}
          <textarea
            ref={composerRef}
            value={feedbackDraft}
            onChange={event => updateFeedbackDraft({ content: event.target.value })}
            placeholder={t('instructor.review.composerPlaceholder')}
            rows={3}
            disabled={savingFeedback}
            className="w-full rounded-lg border border-(--border) bg-(--surface) px-2.5 py-2 text-xs text-(--text-primary) focus-visible:ring-2 focus-visible:ring-(--brand)"
          />
          <div className="flex gap-2">
            <button
              type="submit"
              disabled={savingFeedback || !feedbackDraft.trim()}
              className="flex-1 rounded-lg bg-indigo-600 px-3 py-2 text-[11px] font-black text-white hover:bg-indigo-700 disabled:opacity-50"
            >
              {savingFeedback ? t('saving') : editingFeedbackId ? t('instructor.review.updateFeedback') : t('instructor.review.saveFeedback')}
            </button>
            {editingFeedbackId && (
              <button
                type="button"
                onClick={handleCancelEdit}
                disabled={savingFeedback}
                className="rounded-lg border border-(--border) bg-(--surface) px-3 py-2 text-[11px] font-bold text-(--text-secondary) disabled:opacity-50"
              >
                {t('cancel')}
              </button>
            )}
          </div>
        </form>
      )}

      {threads.length === 0 && (
        <p className="py-4 text-center text-[11px] italic text-(--text-tertiary)">
          {selectedSection ? t('instructor.review.threadsEmpty') : t('instructor.review.selectSectionFeedback')}
        </p>
      )}

      <ul className="space-y-2">
        {threads.map(item => {
          const active = String(item.id) === String(activeFeedbackId);
          const busy = busyId === item.id;
          return (
            <FeedbackCard
              key={item.id}
              item={item}
              active={active}
              onSelect={selectFeedback}
              actions={(
                <div className="mt-2 flex flex-wrap gap-1.5">
                  {item.canEdit && (
                    <button
                      type="button" disabled={busy} onClick={() => handleEditFeedback(item)}
                      className="rounded-lg border border-(--border) px-2.5 py-1.5 text-[10px] font-bold text-(--text-secondary) disabled:opacity-50"
                    >
                      {t('instructor.review.edit')}
                    </button>
                  )}
                  {item.canDelete && (
                    <button
                      type="button" disabled={busy} onClick={() => handleDeleteFeedback(item.id)}
                      className="rounded-lg border border-rose-200 px-2.5 py-1.5 text-[10px] font-bold text-rose-600 disabled:opacity-50"
                    >
                      {t('delete')}
                    </button>
                  )}
                </div>
              )}
            />
          );
        })}
      </ul>
    </div>
  );
}
