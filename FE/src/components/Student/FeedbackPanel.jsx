import { useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { placeFeedbackCards } from '../../utils/student/feedbackAnchors.js';
import { roundNumberFor } from '../../utils/reviewRounds.js';

const control = 'min-w-0 rounded-md border border-(--border) bg-(--surface) px-2 py-1.5 text-xs text-(--text-primary) focus-visible:ring-2 focus-visible:ring-(--brand)';
const FEEDBACK_REQUEST_STATUSES = new Set(['PENDING', 'RETURNED', 'REVIEWED', 'REJECTED']);

export default function FeedbackPanel({ feedback, sectionId, activeId, onSelect, onClose, visible,
  positions, narrow, requestId, setRequestId, scope, setScope, overlapIds = [], projectId,
  userProjectRole = 'MEMBER', currentUserId = null }) {
  const { t, i18n } = useTranslation();
  const isLeader = userProjectRole === 'LEADER';
  const [layout, setLayout] = useState([]);
  const scrollerRef = useRef(null);
  const cardsRef = useRef(new Map());
  const [sizeVersion, setSizeVersion] = useState(0);
  const [scrollTop, setScrollTop] = useState(0);
  const filtered = useMemo(() => feedback.items.filter(item => {
    if (requestId && String(item.requestId) !== String(requestId)) return false;
    if (scope !== 'project' && String(item.sectionId) !== String(sectionId)) return false;
    // Members see only their own assigned sections (mirrors the server rule);
    // leaders see the whole round. Unknown user degrades to unfiltered display.
    if (!isLeader && currentUserId != null && String(item.assignedUserId ?? '') !== String(currentUserId)) return false;
    return true;
  }), [feedback.items, requestId, scope, sectionId, isLeader, currentUserId]);
  const byId = useMemo(() => new Map(positions.map(position => [position.id, position])), [positions]);
  const anchored = !narrow && scope === 'section';
  const visibleItems = anchored ? filtered.filter(item => {
    const position = byId.get(item.id);
    return item.id === activeId || (position?.top != null && position.bottom >= position.viewportTop - 40
      && position.top <= position.viewportBottom + 40) || position?.from == null;
  }) : filtered;
  const ids = visibleItems.map(item => item.id).join(',');

  useEffect(() => {
    if (!visible) return;
    let frame;
    const observer = new ResizeObserver(() => {
      cancelAnimationFrame(frame);
      frame = requestAnimationFrame(() => setSizeVersion(version => version + 1));
    });
    if (scrollerRef.current) observer.observe(scrollerRef.current);
    cardsRef.current.forEach(card => observer.observe(card));
    return () => { observer.disconnect(); cancelAnimationFrame(frame); };
  }, [ids, visible, activeId]);

  useLayoutEffect(() => {
    if (!visible || !anchored || !scrollerRef.current) return;
    const viewport = scrollerRef.current.getBoundingClientRect();
    const scroll = scrollerRef.current.scrollTop;
    const contentTop = viewport.top + parseFloat(getComputedStyle(scrollerRef.current).paddingTop);
    const measured = visibleItems.filter(item => byId.get(item.id)?.top != null).map(item => ({
      id: item.id, top: byId.get(item.id).top - contentTop + scroll, scroll,
      height: cardsRef.current.get(item.id)?.getBoundingClientRect().height ?? 96,
    }));
    const packed = placeFeedbackCards(measured, activeId);
    let bottom = Math.max(0, ...packed.map(card => card.y + card.height + 12));
    visibleItems.filter(item => !measured.some(card => card.id === item.id)).forEach(item => {
      const height = cardsRef.current.get(item.id)?.getBoundingClientRect().height ?? 96;
      packed.push({ id: item.id, top: null, y: bottom, height });
      bottom += height + 12;
    });
    setLayout(previous => JSON.stringify(previous) === JSON.stringify(packed) ? previous : packed);
  }, [positions, ids, visible, anchored, activeId, sizeVersion]);

  useEffect(() => {
    if (!visible || !activeId || !scrollerRef.current) return;
    const card = cardsRef.current.get(activeId);
    const viewport = scrollerRef.current.getBoundingClientRect();
    const bounds = card?.getBoundingClientRect();
    if (bounds && (bounds.bottom < viewport.top || bounds.top > viewport.bottom)) {
      scrollerRef.current.scrollTop += bounds.top - viewport.top - 12;
    }
  }, [activeId, visible, ids]);

  const select = item => onSelect(item);
  const listHeight = Math.max(0, ...layout.map(card => card.y + card.height + 12));
  const layoutById = new Map(layout.map(card => [card.id, card]));
  const date = value => value ? new Date(value).toLocaleString(i18n.language === 'vi' ? 'vi-VN' : 'en-US') : '';
  return <section aria-label={t('studentFeedback.title')} className="flex h-full min-h-0 flex-col text-(--text-primary)"
    onKeyDown={event => { if (event.key === 'Escape' && !event.defaultPrevented) { event.preventDefault(); event.stopPropagation(); onClose(); } }}>
    <div className="shrink-0 border-b border-(--border) bg-(--surface) px-3 py-2 space-y-2">
      <div className="flex items-center justify-between gap-2">
        <h2 className="text-sm font-bold">{t('studentFeedback.title')}</h2>
        <div className="flex items-center gap-1">
          <button type="button" className={control} onClick={feedback.refresh} disabled={feedback.loading} aria-label={t('studentFeedback.refresh')}>↻</button>
          <button type="button" className={control} onClick={onClose} aria-label={t('studentFeedback.close')}>×</button>
        </div>
      </div>
      <details className="text-xs">
      <summary className="cursor-pointer py-1 text-(--text-secondary)">{t('studentFeedback.filters')}</summary>
      <div className="grid grid-cols-2 gap-2 mt-1">
        <select className={control} value={scope} onChange={event => setScope(event.target.value)} aria-label={t('studentFeedback.scope')}>
          <option value="section">{t('studentFeedback.thisSection')}</option><option value="project">{t('studentFeedback.wholeProject')}</option>
        </select>

      </div>
      {isLeader ? (
        <select className={`${control} w-full mt-2`} value={requestId || ''} onChange={event => setRequestId(event.target.value || null)} aria-label={t('studentFeedback.roundFilter')}>
          {feedback.requests.map((request) => <option key={request.id} value={request.id}>
            {t('studentFeedback.round', { number: roundNumberFor(feedback.requests, request.id) ?? '?' })} · {t(`status.${FEEDBACK_REQUEST_STATUSES.has(request.status) ? request.status : 'UNKNOWN'}`)}
          </option>)}
        </select>
      ) : (
        <p className="mt-2 text-[11px] font-semibold text-(--text-secondary)">
          {(() => {
            const current = feedback.requests.find(request => String(request.id) === String(requestId));
            if (!current) return t('studentFeedback.empty');
            const number = roundNumberFor(feedback.requests, current.id) ?? '?';
            return `${t('studentFeedback.round', { number })} · ${t(`status.${FEEDBACK_REQUEST_STATUSES.has(current.status) ? current.status : 'UNKNOWN'}`)}`;
          })()}
        </p>
      )}
      </details>
      {!!filtered.length && <select className={`${control} w-full`} value={filtered.some(item => item.id === activeId) ? activeId : ''}
        onChange={event => { const item = filtered.find(entry => entry.id === event.target.value); if (item) select(item); }} aria-label={t('studentFeedback.navigate')}>
        <option value="">{t('studentFeedback.navigate')} ({filtered.length})</option>
        {filtered.map((item, index) => <option key={item.id} value={item.id}>{index + 1}. {item.sectionTitle} · {item.content.slice(0, 65)}</option>)}
      </select>}
      {overlapIds.length > 1 && <div className="flex flex-wrap items-center gap-1 text-xs" aria-label={t('studentFeedback.overlap')}>
        <span>{t('studentFeedback.overlap')}:</span>
        {overlapIds.map((id, index) => <button type="button" key={id} className={`${control} ${id === activeId ? 'font-bold ring-1 ring-teal-600' : ''}`}
          onClick={() => { const item = feedback.items.find(entry => entry.id === id); if (item) select(item); }}>{index + 1}</button>)}
      </div>}
    </div>
    {feedback.error && <div role="alert" className="m-3 rounded-lg border border-rose-300 bg-rose-50 dark:bg-rose-950/40 p-3 text-xs text-rose-700 dark:text-rose-300">
      <p>{t(feedback.error === 403 ? 'studentFeedback.accessDenied' : 'studentFeedback.loadError')}</p>
      <button type="button" onClick={feedback.refresh} className="mt-2 underline font-bold">{t('retry')}</button>
    </div>}
    <div ref={scrollerRef} onScroll={event => setScrollTop(event.currentTarget.scrollTop)} className="min-h-0 flex-1 overflow-y-auto overflow-x-hidden bg-(--surface-secondary)/50 p-3" data-testid="feedback-scroller">
      {feedback.loading && <p role="status" className="py-3 text-xs text-(--text-secondary)">{t('studentFeedback.loading')}</p>}
      {!feedback.loading && !feedback.error && filtered.length === 0 && <p className="py-6 text-center text-sm text-(--text-secondary)">{t('studentFeedback.empty')}</p>}
      <div className={anchored ? 'relative' : 'space-y-3'} style={anchored ? { height: listHeight || undefined, minHeight: visibleItems.length ? 100 : 0 } : undefined}>
        {visibleItems.map(item => {
          const position = byId.get(item.id);
          const anchor = position?.anchor || item.anchor;
          const placed = layoutById.get(item.id);
          const active = item.id === activeId;
          const canNavigate = item.sectionId && (String(item.sectionId) !== String(sectionId) || position?.from != null);
          return <article key={item.id} ref={node => { if (node) cardsRef.current.set(item.id, node); else cardsRef.current.delete(item.id); }}
            data-feedback-card={item.id} aria-label={t('studentFeedback.card', { section: item.sectionTitle || '' })}
            className={`rounded-lg border bg-(--surface) p-3 text-xs shadow-sm ${active ? 'border-teal-600 ring-1 ring-teal-600' : 'border-(--border)'}`}
            style={anchored ? { position: 'absolute', left: 10, right: 0, top: placed?.y ?? 0 } : undefined}>
            {anchored && active && placed?.top != null && <svg aria-hidden="true" className="pointer-events-none absolute overflow-visible" style={{ left: -11, top: 0, width: 11, height: 1 }}>
              <path d={`M 0 ${placed.top - placed.y + scrollTop - placed.scroll} H 4 V 16 H 11`} fill="none" stroke="currentColor" strokeWidth="1.5" className="text-teal-600" />
            </svg>}
            <button type="button" className="w-full text-left rounded focus-visible:ring-2 focus-visible:ring-(--brand)" onClick={() => select(item)} aria-expanded={active}>
              <span className="flex justify-between gap-2 font-semibold">
                <span>{item.instructorName || t('instructor')} · {item.sectionTitle}{isLeader && item.assignedUserName ? ` · ${t('feedbackAssignee')}: ${item.assignedUserName}` : ''} · {t('studentFeedback.round', { number: item.roundNumber || '?' })}</span>
                <span className="shrink-0 text-[10px] font-normal text-(--text-tertiary)">{date(item.createdAt)}</span>
              </span>
              <span className={`mt-2 block whitespace-pre-wrap break-words leading-relaxed ${active ? '' : 'line-clamp-3'}`}>{item.content}</span>
            </button>
            <div className="mt-2 rounded-md border border-(--border) bg-(--surface-secondary) px-2 py-1.5">
              <p className="text-[10px] font-bold uppercase tracking-wide text-(--text-tertiary)">{t('studentFeedback.passage')}</p>
              {anchor?.original?.exact
                ? <p className="mt-1 whitespace-pre-wrap break-words font-mono leading-relaxed">“{anchor.original.exact}”</p>
                : <p className="mt-1 italic text-(--text-secondary)">{t('studentFeedback.wholeSection')}{item.lineReference ? ` · ${item.lineReference}` : ''}</p>}
            </div>
            {active && <div className="mt-3 space-y-3">
              {canNavigate && <button type="button" className={`${control} font-semibold`} onClick={() => select(item)}>{t('studentFeedback.goToText')}</button>}
              {(item.attachments || []).length > 0 && (
                <div>
                  <p className="mb-1 text-[10px] font-bold uppercase tracking-wide text-(--text-tertiary)">{t('studentFeedback.providedImages')}</p>
                  <div className="flex flex-wrap gap-1.5">
                    {item.attachments.map(attachment => (
                      <a key={attachment.id} href={attachment.url} target="_blank" rel="noreferrer" title={attachment.mimeType}>
                        <img src={attachment.url} alt="" loading="lazy" decoding="async" className="h-14 w-14 rounded-md border border-(--border) object-cover" />
                      </a>
                    ))}
                  </div>
                </div>
              )}
              {(item.replies || []).length > 0 && (
                <details className="rounded-md border border-(--border) bg-(--surface-secondary) px-2 py-1.5">
                  <summary className="cursor-pointer font-medium">{t('studentFeedback.legacyDiscussion')}</summary>
                  <ul className="mt-2 space-y-1.5">
                    {item.replies.map(reply => (
                      <li key={reply.id} className="rounded-md bg-(--surface-secondary)/70 px-2 py-1.5">
                        <p className="text-[9px] font-bold text-(--text-tertiary)">
                          {reply.authorName || reply.authorRole} · {date(reply.createdAt)}
                        </p>
                        <p className="mt-0.5 whitespace-pre-wrap break-words leading-relaxed">{reply.content}</p>
                      </li>
                    ))}
                  </ul>
                </details>
              )}
            </div>}
          </article>;
        })}
      </div>
      {anchored && filtered.length > visibleItems.length && <p className="py-3 text-[11px] text-(--text-secondary)">{t('studentFeedback.offscreen', { count: filtered.length - visibleItems.length })}</p>}
    </div>
  </section>;
}
