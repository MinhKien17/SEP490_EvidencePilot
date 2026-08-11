import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import api from '../api.js';

const DEBOUNCE_MS = 900;

async function compileError(error) {
  const data = error?.response?.data;
  if (data instanceof Blob) {
    try {
      return JSON.parse(await data.text());
    } catch {
      return { message: error.message };
    }
  }
  return data || { message: error?.message };
}

export default function CompiledPaperPreview({
  paperId,
  sectionId,
  contentTex = '',
  shellOverride,
  className = '',
}) {
  const { t } = useTranslation();
  const [pdfUrl, setPdfUrl] = useState('');
  const [status, setStatus] = useState('idle');
  const [error, setError] = useState(null);
  const mountedRef = useRef(true);
  const pdfUrlRef = useRef('');
  const activeRef = useRef(false);
  const pendingRef = useRef(null);
  const generationRef = useRef(0);
  const drainRef = useRef(() => {});
  const paperIdRef = useRef(paperId);

  const request = useMemo(() => {
    if (!paperId) return null;
    if (shellOverride) {
      return {
        paperId,
        body: {
          shellOverride: {
            kind: shellOverride.kind,
            contentTex: shellOverride.contentTex || '',
          },
        },
      };
    }
    return {
      paperId,
      body: sectionId
        ? { sectionOverride: { sectionId, contentTex: contentTex || '' } }
        : {},
    };
  }, [paperId, sectionId, contentTex, shellOverride?.kind, shellOverride?.contentTex]);

  const requestKey = useMemo(() => JSON.stringify(request), [request]);

  const replacePdf = useCallback((blob) => {
    const nextUrl = URL.createObjectURL(blob);
    const previousUrl = pdfUrlRef.current;
    pdfUrlRef.current = nextUrl;
    setPdfUrl(nextUrl);
    if (previousUrl) window.setTimeout(() => URL.revokeObjectURL(previousUrl), 0);
  }, []);

  drainRef.current = async () => {
    if (activeRef.current || !pendingRef.current) return;
    const item = pendingRef.current;
    pendingRef.current = null;
    activeRef.current = true;
    if (mountedRef.current) {
      setStatus('compiling');
      setError(null);
    }
    try {
      const response = await api.post(
        `/api/papers/${item.request.paperId}/preview`,
        item.request.body,
        { responseType: 'blob', headers: { Accept: 'application/pdf, application/json' } },
      );
      if (mountedRef.current && item.generation === generationRef.current) {
        replacePdf(response.data);
        setStatus('success');
      }
    } catch (compileFailure) {
      const detail = await compileError(compileFailure);
      if (mountedRef.current && item.generation === generationRef.current) {
        setError(detail);
        setStatus('error');
      }
    } finally {
      activeRef.current = false;
      if (pendingRef.current) drainRef.current();
    }
  };

  useEffect(() => {
    const generation = ++generationRef.current;
    pendingRef.current = null;
    if (!request) {
      setStatus('idle');
      setError(null);
      return undefined;
    }
    const timer = window.setTimeout(() => {
      pendingRef.current = { request, generation };
      drainRef.current();
    }, DEBOUNCE_MS);
    return () => window.clearTimeout(timer);
  }, [requestKey]);

  useEffect(() => {
    if (paperIdRef.current === paperId) return;
    paperIdRef.current = paperId;
    if (pdfUrlRef.current) URL.revokeObjectURL(pdfUrlRef.current);
    pdfUrlRef.current = '';
    setPdfUrl('');
  }, [paperId]);

  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
      pendingRef.current = null;
      if (pdfUrlRef.current) URL.revokeObjectURL(pdfUrlRef.current);
    };
  }, []);

  const diagnostic = error?.diagnostics?.[0];
  const errorMessage = diagnostic?.message || error?.message || t('texCompileFailed');

  return (
    <div className={`relative flex min-h-0 flex-1 flex-col overflow-hidden bg-slate-200 dark:bg-slate-950 ${className}`} aria-busy={status === 'compiling'}>
      <div className="flex h-8 shrink-0 items-center justify-between gap-3 border-b border-(--border) bg-(--surface) px-3 text-[11px]">
        <span className="truncate text-(--text-secondary)">
          {status === 'compiling' && t('compilingPaper')}
          {status === 'success' && t('compiledPaperReady')}
          {status === 'idle' && t('selectPaperToPreview')}
          {status === 'error' && errorMessage}
        </span>
        {diagnostic?.file && (
          <span className="shrink-0 font-mono text-rose-600">
            {diagnostic.file}{diagnostic.line ? `:${diagnostic.line}` : ''}
          </span>
        )}
      </div>
      {pdfUrl ? (
        <iframe
          src={pdfUrl}
          title={t('compiledPaperPreview')}
          className="h-full min-h-0 w-full flex-1 border-0 bg-slate-200"
        />
      ) : (
        <div className="flex flex-1 items-center justify-center p-6 text-center text-sm text-slate-500">
          {status === 'error' ? errorMessage : status === 'compiling' ? t('compilingPaper') : t('selectPaperToPreview')}
        </div>
      )}
      {status === 'compiling' && pdfUrl && (
        <div className="pointer-events-none absolute right-4 top-12 rounded-full bg-slate-900/80 px-3 py-1 text-[11px] font-semibold text-white shadow-lg">
          {t('compilingPaper')}
        </div>
      )}
    </div>
  );
}
