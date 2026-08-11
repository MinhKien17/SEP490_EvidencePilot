import { useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import api from '../../api.js';
import CompiledPaperPreview from '../../components/CompiledPaperPreview.jsx';
import LatexEditor from '../../components/LatexEditor.jsx';

export default function PaperTexShellModal({ paperId, paperTitle, readOnly, onClose }) {
  const { t } = useTranslation();
  const [shell, setShell] = useState(null);
  const [selected, setSelected] = useState('PREAMBLE');
  const [status, setStatus] = useState('loading');
  const [message, setMessage] = useState('');

  useEffect(() => {
    let active = true;
    setStatus('loading');
    api.get(`/api/papers/${paperId}/tex-shell`)
      .then(({ data }) => {
        if (!active) return;
        setShell(data);
        setStatus('ready');
      })
      .catch(() => {
        if (!active) return;
        setMessage(t('texShellLoadFailed'));
        setStatus('error');
      });
    return () => { active = false; };
  }, [paperId, t]);

  const content = selected === 'PREAMBLE' ? shell?.preambleTex || '' : shell?.frontMatterTex || '';
  const override = useMemo(() => ({ kind: selected, contentTex: content }), [selected, content]);

  const updateContent = (value) => {
    setShell(current => selected === 'PREAMBLE'
      ? { ...current, preambleTex: value }
      : { ...current, frontMatterTex: value });
    setStatus('dirty');
    setMessage('');
  };

  const save = async () => {
    if (!shell || readOnly) return;
    setStatus('saving');
    try {
      const { data } = await api.put(`/api/papers/${paperId}/tex-shell`, shell);
      setShell(data);
      setStatus('ready');
      setMessage(t('texShellSaved'));
    } catch (error) {
      setStatus('error');
      setMessage(error?.response?.data?.message || t('texShellSaveFailed'));
    }
  };

  return (
    <div className="fixed inset-0 z-[70] flex bg-slate-950/65 p-3 backdrop-blur-sm sm:p-6">
      <div className="m-auto flex h-full max-h-[92vh] w-full max-w-[1500px] flex-col overflow-hidden rounded-2xl border border-(--border) bg-(--surface) shadow-2xl">
        <div className="flex h-12 shrink-0 items-center justify-between border-b border-(--border) px-4">
          <div className="min-w-0">
            <h2 className="truncate text-sm font-bold text-(--text-primary)">{t('paperTexLayout')}</h2>
            <p className="truncate text-[10px] text-(--text-tertiary)">{paperTitle}</p>
          </div>
          <div className="flex items-center gap-2">
            {message && <span className="max-w-72 truncate text-xs text-(--text-secondary)">{message}</span>}
            {!readOnly && (
              <button onClick={save} disabled={!shell || status === 'saving' || status === 'loading'} className="rounded-lg bg-indigo-600 px-3 py-1.5 text-xs font-bold text-white hover:bg-indigo-700 disabled:opacity-40">
                {status === 'saving' ? t('saving') : t('save')}
              </button>
            )}
            <button onClick={onClose} className="rounded-lg border border-(--border) px-3 py-1.5 text-xs font-semibold text-(--text-secondary) hover:bg-(--surface-secondary)">
              {t('close')}
            </button>
          </div>
        </div>
        <div className="flex min-h-0 flex-1 flex-col md:flex-row">
          <div className="flex min-h-0 flex-1 flex-col border-b border-(--border) md:w-1/2 md:border-b-0 md:border-r">
            <div className="flex h-10 shrink-0 items-end gap-1 border-b border-(--border) bg-(--surface-secondary) px-3">
              {[
                ['PREAMBLE', 'preamble.tex'],
                ['FRONT_MATTER', 'frontmatter.tex'],
              ].map(([kind, filename]) => (
                <button key={kind} onClick={() => setSelected(kind)} className={`h-9 border-b-2 px-3 font-mono text-xs ${selected === kind ? 'border-indigo-600 text-indigo-700' : 'border-transparent text-(--text-secondary)'}`}>
                  {filename}
                </button>
              ))}
            </div>
            <div className="min-h-0 flex-1">
              {shell ? (
                <LatexEditor content={content} onChange={readOnly ? undefined : updateContent} readOnly={readOnly} fontSize={13} />
              ) : (
                <div className="flex h-full items-center justify-center text-sm text-(--text-tertiary)">{status === 'error' ? message : t('loading')}</div>
              )}
            </div>
          </div>
          <div className="flex min-h-0 flex-1 md:w-1/2">
            <CompiledPaperPreview paperId={paperId} shellOverride={readOnly ? null : override} />
          </div>
        </div>
      </div>
    </div>
  );
}
