// ponytail: offset-preserving scanner for LaTeX inline constructs — returns
// exact canonical spans, never rendered text. The legacy renderer
// (renderLatexToHtml) emits plain HTML with no positions, so B5 maps a
// Preview DOM range by matching its text against these spans; escaped
// constructs (\$, \\textbf) are skipped, never mapped.

// Balanced one level: \textbf{a {b} c} matches whole; deeper nesting refuses
// (falls back to the honest banner instead of a wrong span).
const braced = name => new RegExp(`(?<!\\\\)\\\\${name}\\{(?:[^{}]|\\{[^{}]*\\})*\\}`, 'gd');
const PATTERNS = [
  { type: 'cite', re: /(?<!\\)\\cite\{[^}]*\}/gd },
  { type: 'ref', re: /(?<!\\)\\ref\{[^}]*\}/gd },
  { type: 'textbf', re: braced('textbf') },
  { type: 'textit', re: braced('textit') },
  { type: 'emph', re: braced('emph') },
  { type: 'bold', re: /\*\*(.+?)\*\*/gd },
  // Inline $…$ only: (?<!\\) guards an escaped opener, (?<!\$)/​(?!\$) on
  // both sides skips display $$…$$, and \\. lets an escaped \$ live inside
  // without closing early.
  { type: 'math', re: /(?<!\\)(?<!\$)\$(?!\$)(?:[^$\\]|\\.)+?\$(?!\$)/gd },
];

export function scanLatexInline(source) {
  const text = String(source ?? '');
  const found = [];
  for (const { type, re } of PATTERNS) {
    re.lastIndex = 0;
    let match;
    while ((match = re.exec(text)) !== null) {
      found.push({ type, from: match.indices[0][0], to: match.indices[0][1] });
    }
  }
  // First span wins on overlap (sorted by start, longest first) — a bold
  // wrapper beats the cite inside it, never double-maps one range.
  found.sort((a, b) => a.from - b.from || b.to - a.to);
  const spans = [];
  for (const span of found) {
    if (spans.length > 0 && span.from < spans[spans.length - 1].to) continue;
    spans.push(span);
  }
  return spans;
}
