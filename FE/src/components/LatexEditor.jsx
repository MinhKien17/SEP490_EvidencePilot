import { useEffect, useRef } from 'react';
import { EditorView, basicSetup } from 'codemirror';
import { EditorState } from '@codemirror/state';
import { oneDark } from '@codemirror/theme-one-dark';
import { latex } from 'codemirror-lang-latex';

export default function LatexEditor({ content, onChange, readOnly = false, fontSize = 14 }) {
  const containerRef = useRef(null);
  const viewRef = useRef(null);

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
        oneDark,
        EditorView.editable.of(!readOnly),
        updateListener,
        EditorView.theme({
          '&': { fontSize: `${fontSize}px` },
          '.cm-scroller': { fontFamily: '"JetBrains Mono", "Fira Code", monospace' },
        }),
      ],
    });

    viewRef.current = new EditorView({ state, parent: containerRef.current });

    return () => { if (viewRef.current) viewRef.current.destroy(); };
  }, [readOnly, fontSize]);

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

  return <div ref={containerRef} className="h-full overflow-auto" />;
}
