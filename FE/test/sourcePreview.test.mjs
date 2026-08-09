import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

test('source preview uses the authenticated download route and releases its blob URL', async () => {
  const contextPanel = await readFile(new URL('../src/pages/Student/ContextPanel.jsx', import.meta.url), 'utf8');
  const fileViewer = await readFile(new URL('../src/components/FileViewerModal.jsx', import.meta.url), 'utf8');

  assert.match(contextPanel, /fileUrl: `\/api\/documents\/\$\{src\.id\}\/download`/);
  assert.match(fileViewer, /api\.get\(fileUrl, \{ responseType: 'blob' \}\)/);
  assert.match(fileViewer, /URL\.createObjectURL\(response\.data\)/);
  assert.match(fileViewer, /URL\.revokeObjectURL\(objectUrl\)/);
});
