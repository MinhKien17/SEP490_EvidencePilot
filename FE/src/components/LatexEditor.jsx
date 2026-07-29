import { useEffect, useRef, useState, forwardRef, useImperativeHandle } from 'react';
import { EditorView, basicSetup } from 'codemirror';
import { EditorState } from '@codemirror/state';
import { oneDark } from '@codemirror/theme-one-dark';
import { latex } from 'codemirror-lang-latex';

const lightTheme = EditorView.theme({
  '&': { backgroundColor: '#ffffff' },
  '.cm-scroller': { fontFamily: '"JetBrains Mono", "Fira Code", monospace' },
});

const LatexEditor = forwardRef(function LatexEditor({ content, onChange, readOnly = false, fontSize = 14 }, ref) {
  const containerRef = useRef(null);
  const viewRef = useRef(null);
  const [isDark, setIsDark] = useState(() =>
    typeof document !== 'undefined' && document.documentElement.classList.contains('dark')
  );

  useImperativeHandle(ref, () => ({
    getSelection: () => {
      const v = viewRef.current;
      if (!v) return '';
      return v.state.sliceDoc(v.state.selection.main.from, v.state.selection.main.to);
    },
    insertAtCursor: (text, cursorOffset) => {
      const v = viewRef.current;
      if (!v) return;
      const from = v.state.selection.main.from;
      v.dispatch({
        changes: { from, to: v.state.selection.main.to, insert: text },
        selection: { anchor: cursorOffset != null ? from + cursorOffset : from + text.length },
      });
    },
  }));

  useEffect(() => {
    const el = document.documentElement;
    const cb = () => setIsDark(el.classList.contains('dark'));
    const mo = new MutationObserver(cb);
    mo.observe(el, { attributes: true, attributeFilter: ['class'] });
    return () => mo.disconnect();
  }, []);

  useEffect(() => {
    if (!containerRef.current) return;
    if (viewRef.current) viewRef.current.destroy();

    const updateListener = EditorView.updateListener.of((update) => {
      if (update.docChanged && onChange) {
        onChange(update.state.doc.toString());
      }
    });

    const state = EditorState.create({
      doc: content || '',
      extensions: [
        basicSetup,
        latex(),
        isDark ? oneDark : lightTheme,
        EditorView.editable.of(!readOnly),
        EditorView.lineWrapping,
        updateListener,
        EditorView.theme({
          '&': { fontSize: `${fontSize}px`, backgroundColor: isDark ? '#0f172a' : '#ffffff', color: isDark ? '#f8fafc' : '#000000', height: '100%' },
          '.cm-editor': { width: '100%', maxWidth: '100%', height: '100%', overflow: 'hidden' },
          '.cm-scroller': { fontFamily: '"JetBrains Mono", "Fira Code", monospace', width: '100%', height: '100%', overflow: 'auto' },
          '.cm-content': { color: isDark ? '#f8fafc' : '#000000', breakWords: 'break-word', overflowWrap: 'anywhere' },
          '.cm-line': { color: isDark ? '#f8fafc' : '#000000', wordBreak: 'break-word', overflowWrap: 'anywhere' },
          '.cm-content *, .cm-line *': { color: isDark ? '#f8fafc !important' : '#000000 !important' },
          '.cm-activeLine': { backgroundColor: isDark ? '#0f172a !important' : '#ffffff !important' },
          '.cm-activeLineGutter': { backgroundColor: isDark ? '#0f172a !important' : '#ffffff !important' },
          '&.cm-focused .cm-selectionBackground, .cm-selectionBackground': { backgroundColor: isDark ? 'rgba(99, 102, 241, 0.35) !important' : 'rgba(224, 231, 255, 0.6) !important' },
          '.cm-lintRange': { wordBreak: 'break-word', overflowWrap: 'anywhere', maxWidth: '100%' },
          '.cm-lintRange-warning': { backgroundColor: 'transparent', borderBottom: '2px solid #eab308' },
          '.cm-lintRange-error': { backgroundColor: 'transparent', borderBottom: '2px solid #ef4444' },
          '.cm-gutters': { display: 'none' },
        }),
      ],
    });

    viewRef.current = new EditorView({ state, parent: containerRef.current });

    return () => { if (viewRef.current) viewRef.current.destroy(); };
  }, [readOnly, fontSize, isDark]);

  useEffect(() => {
    if (viewRef.current && content !== undefined) {
      const current = viewRef.current.state.doc.toString();
      if (current !== content) {
        viewRef.current.dispatch({
          changes: { from: 0, to: current.length, insert: content || '' },
        });
      }
    }
  }, [content]);

  return <div ref={containerRef} className="h-full w-full overflow-hidden" />;
});

export default LatexEditor;
