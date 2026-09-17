import { test, expect } from '@playwright/test';

test.use({ channel: 'chrome', viewport: { width: 1440, height: 1000 } });

const baseUrl = 'http://localhost:5173';

const BEFORE = 'Machine learning systems require testing.';
const AFTER = 'Machine learning systems require extensive testing.';
const CONTENT = [AFTER, ...Array.from({ length: 60 }, (_, i) => `Filler line ${i}.`)].join('\n\n');

async function setupDiff(page, comparisonSource, texts = {}) {
  const projectId = 'diff-source-project';
  const paperId = 'diff-paper';
  const sectionId = 'diff-section';
  const roundId = 'diff-round';
  const state = { errors: [], unhandled: [], comparisonCalls: [], snapshotCalls: [] };
  const content = texts.content || CONTENT;
  const probe = texts.probe || 'extensive testing';

  page.on('pageerror', error => state.errors.push(error.message));
  await page.addInitScript(() => {
    localStorage.setItem('token', 'diff-source-fixture');
    localStorage.setItem('role', 'INSTRUCTOR');
    localStorage.setItem('app_lang', 'en');
    localStorage.setItem('app_theme', 'light');
  });

  await page.route('**/api/**', async route => {
    const request = route.request();
    const url = new URL(request.url());
    const path = url.pathname;
    const method = request.method();
    let json;

    if (path === '/api/users/profile') {
      json = { id: 'instructor-one', role: 'INSTRUCTOR', firstName: 'Test', lastName: 'Instructor' };
    } else if (path === '/api/notifications') {
      json = [];
    } else if (path === '/api/notifications/unread-count') {
      json = { count: 0 };
    } else if (path === '/api/review-guides') {
      json = [];
    } else if (path === `/api/projects/${projectId}`) {
      json = { id: projectId, title: 'Diff source fixture', status: 'SUBMITTED_FOR_REVIEW' };
    } else if (path === `/api/projects/${projectId}/papers`) {
      json = [{ id: paperId, title: 'Diff paper', originalFilename: 'diff.tex', processingStatus: 'READY' }];
    } else if (path === `/api/projects/${projectId}/sources`) {
      json = { content: [], last: true };
    } else if (path === `/api/media/projects/${projectId}`) {
      json = [];
    } else if (path === `/api/projects/${projectId}/evidence-traces`) {
      json = [];
    } else if (path === '/api/feedback-requests') {
      json = [{ id: roundId, projectId, status: 'PENDING', requestedAt: '2026-09-16T08:00:00Z' }];
    } else if (path === `/api/feedback-requests/${roundId}/submission-snapshot`) {
      json = { state: 'AVAILABLE', snapshot: {
        schemaVersion: 1,
        projectId,
        papers: [{ id: paperId, title: 'Diff paper', sections: [{
          id: sectionId, title: 'Introduction', order: 0,
          contentTex: content, contentVersion: 2,
        }] }],
      } };
    } else if (path === `/api/feedback-requests/${roundId}/feedback`) {
      json = [];
    } else if (path === `/api/papers/${paperId}/references`) {
      json = [];
    } else if (path === `/api/feedback-requests/${roundId}/comparison-source`) {
      state.comparisonCalls.push(`${method} ${path}?sectionId=${url.searchParams.get('sectionId')}`);
      json = comparisonSource;
    } else if (path === `/api/feedback-requests/${roundId}/section-snapshots`) {
      state.snapshotCalls.push(`${method} ${path}`);
      json = [];
    } else {
      state.unhandled.push(`${method} ${path}`);
      return route.fulfill({ status: 404, json: { message: `Unhandled fixture request: ${method} ${path}` } });
    }

    return route.fulfill({ json });
  });

  return { projectId, state, content, probe };
}

async function openReviewEditor(page, projectId, probe = 'extensive testing', length = CONTENT.length) {
  await page.goto(`${baseUrl}/instructor/requests/${projectId}`);
  await expect(page.locator('.cm-content')).toContainText(probe);
  await expect.poll(() => page.evaluate(
    () => document.querySelector('.cm-editor')?.__cmView?.state.doc.length)).toBe(length);
}

test('Single-character change highlights only the replacement token', async ({ page }) => {
  const before = 'This is a test feedback, this is version 4';
  const after = 'This is a test feedback, this is version 5';
  const content = [after, ...Array.from({ length: 60 }, (_, i) => `Filler line ${i}.`)].join('\n\n');
  const baseline = [before, ...Array.from({ length: 60 }, (_, i) => `Filler line ${i}.`)].join('\n\n');
  const { projectId, state } = await setupDiff(page, {
    submitted: { contentTex: content, contentVersion: 5 },
    baseline: { contentTex: baseline, contentVersion: 4, origin: 'RETURN_FOR_REVISION' },
  }, { content, probe: 'version 5' });
  await openReviewEditor(page, projectId, 'version 5', content.length);

  await page.getByRole('checkbox', { name: 'Show Diff' }).first().check();
  const marks = page.locator('.cm-editor .cm-change-added');
  await expect(marks).toHaveCount(1);
  await expect(marks.first()).toHaveText('5');
  expect(state.errors).toEqual([]);
});

test('Show Diff consumes the server comparison source, not single-request snapshots', async ({ page }) => {
  const { projectId, state } = await setupDiff(page, {
    submitted: { contentTex: AFTER, contentVersion: 2 },
    baseline: { contentTex: BEFORE, contentVersion: 1, origin: 'INITIAL_ASSIGNMENT' },
  });
  await openReviewEditor(page, projectId);

  await page.getByRole('checkbox', { name: 'Show Diff' }).first().check();
  await expect.poll(() => state.comparisonCalls.length).toBe(1);
  expect(state.comparisonCalls[0]).toContain('sectionId=diff-section');
  // The obsolete single-request lookup must never fire.
  expect(state.snapshotCalls).toEqual([]);
  expect(state.errors).toEqual([]);
});

test('Show Diff with no baseline shows the honest unavailable state', async ({ page }) => {
  const { projectId, state } = await setupDiff(page, {
    submitted: { contentTex: AFTER, contentVersion: 2 },
    baseline: null,
  });
  await openReviewEditor(page, projectId);

  await page.getByRole('checkbox', { name: 'Show Diff' }).first().check();
  await expect(page.locator('[data-tour="editor-toolbar"]')
    .getByText('Comparison baseline unavailable for this section', { exact: false })).toBeVisible();
  expect(state.snapshotCalls).toEqual([]);
  expect(state.errors).toEqual([]);
});

async function setupThread(page) {
  const projectId = 'thread-read-project';
  const paperId = 'thread-paper';
  const sectionId = 'thread-section';
  const roundId = 'thread-round';
  const state = { errors: [] };

  page.on('pageerror', error => state.errors.push(error.message));
  await page.addInitScript(() => {
    localStorage.setItem('token', 'thread-read-fixture');
    localStorage.setItem('role', 'INSTRUCTOR');
    localStorage.setItem('app_lang', 'en');
    localStorage.setItem('app_theme', 'light');
  });

  await page.route('**/api/**', async route => {
    const request = route.request();
    const path = new URL(request.url()).pathname;
    let json;

    if (path === '/api/users/profile') {
      json = { id: 'instructor-one', role: 'INSTRUCTOR', firstName: 'Test', lastName: 'Instructor' };
    } else if (path === '/api/notifications' || path === '/api/review-guides'
      || path === `/api/projects/${projectId}/evidence-traces`
      || path === `/api/media/projects/${projectId}`) {
      json = [];
    } else if (path === '/api/notifications/unread-count') {
      json = { count: 0 };
    } else if (path === `/api/projects/${projectId}`) {
      json = { id: projectId, title: 'Thread read fixture', status: 'SUBMITTED_FOR_REVIEW' };
    } else if (path === `/api/projects/${projectId}/papers`) {
      json = [{ id: paperId, title: 'Thread paper', originalFilename: 'thread.tex', processingStatus: 'READY' }];
    } else if (path === `/api/projects/${projectId}/sources`) {
      json = { content: [], last: true };
    } else if (path === '/api/feedback-requests') {
      json = [{ id: roundId, projectId, status: 'PENDING', requestedAt: '2026-09-16T08:00:00Z' }];
    } else if (path === `/api/feedback-requests/${roundId}/submission-snapshot`) {
      json = { state: 'AVAILABLE', snapshot: {
        schemaVersion: 1,
        projectId,
        papers: [{ id: paperId, title: 'Thread paper', sections: [{
          id: sectionId, title: 'Introduction', order: 0,
          contentTex: CONTENT, contentVersion: 2,
        }] }],
      } };
    } else if (path === `/api/feedback-requests/${roundId}/feedback`) {
      json = [{
        id: 'thread-one', requestId: roundId, sectionId,
        content: 'Rewrite this sentence for academic tone.',
        createdAt: '2026-09-16T09:00:00Z', threadState: 'OPEN', pendingState: null,
        publishedAt: '2026-09-16T09:05:00Z', lineReference: null, anchor: null,
        studentStatus: null, studentNote: null, attachments: [],
        canMarkDone: true, canReopen: false, canEdit: false, canDelete: false,
        replies: [{ id: 'legacy-reply', authorName: 'Former Student', authorRole: 'STUDENT',
          content: 'Old discussion line.', createdAt: '2026-09-16T10:00:00Z' }],
      }];
    } else if (path === `/api/papers/${paperId}/references`
      || path === `/api/papers/${paperId}/references/check`) {
      json = [];
    } else {
      return route.fulfill({ status: 404, json: { message: 'Unhandled fixture request' } });
    }

    return route.fulfill({ json });
  });

  return { projectId, state };
}

test('Thread shows legacy replies read-only with no conversation controls', async ({ page }) => {
  const { projectId, state } = await setupThread(page);
  await openReviewEditor(page, projectId);

  await expect(page.getByText('Rewrite this sentence for academic tone.')).toBeVisible();
  await expect(page.getByText('Old discussion line.')).toBeVisible();
  await expect(page.getByPlaceholder('Reply to this thread.')).toHaveCount(0);
  await expect(page.getByRole('button', { name: 'Resolve', exact: true })).toHaveCount(0);
  await expect(page.getByRole('button', { name: 'Reopen', exact: true })).toHaveCount(0);
  // Request-level Reject is a separate workflow action and must remain —
  // exactly one Reject button (the thread-level one is gone).
  await expect(page.getByRole('button', { name: 'Reject', exact: true })).toHaveCount(1);
  expect(state.errors).toEqual([]);
});

const EDIT_SENTENCE = 'Alpha comparisons beta comparisons gamma.';
const EDIT_CONTENT = [EDIT_SENTENCE, ...Array.from({ length: 60 }, (_, i) => `Filler line ${i}.`)].join('\n\n');

async function setupEdit(page) {
  const projectId = 'edit-passage-project';
  const paperId = 'edit-paper';
  const sectionId = 'edit-section';
  const roundId = 'edit-round';
  const state = { errors: [], patches: [] };
  const draft = {
    id: 'draft-one', requestId: roundId, sectionId,
    content: 'Tighten this wording.',
    createdAt: '2026-09-16T09:00:00Z', threadState: 'OPEN', pendingState: null,
    publishedAt: null, lineReference: null,
    anchor: {
      original: { representation: 'latex-source-lf-v1', offsetUnit: 'utf16', contentVersion: 1,
        fingerprint: '0'.repeat(64), from: 6, to: 17, exact: 'comparisons',
        prefix: 'Alpha ', suffix: ' beta comparisons gamma.' },
      current: { status: 'DETACHED', contentVersion: 2, fingerprint: '1'.repeat(64), from: null, to: null },
    },
    studentStatus: null, studentNote: null, attachments: [], replies: [],
    canMarkDone: false, canReopen: false, canEdit: true, canDelete: true,
  };

  page.on('pageerror', error => state.errors.push(error.message));
  await page.addInitScript(() => {
    localStorage.setItem('token', 'edit-passage-fixture');
    localStorage.setItem('role', 'INSTRUCTOR');
    localStorage.setItem('app_lang', 'en');
    localStorage.setItem('app_theme', 'light');
  });

  await page.route('**/api/**', async route => {
    const request = route.request();
    const path = new URL(request.url()).pathname;
    const method = request.method();
    let json;

    if (path === '/api/users/profile') {
      json = { id: 'instructor-one', role: 'INSTRUCTOR', firstName: 'Test', lastName: 'Instructor' };
    } else if (path === '/api/notifications' || path === '/api/review-guides'
      || path === `/api/projects/${projectId}/evidence-traces`
      || path === `/api/media/projects/${projectId}`) {
      json = [];
    } else if (path === '/api/notifications/unread-count') {
      json = { count: 0 };
    } else if (path === `/api/projects/${projectId}`) {
      json = { id: projectId, title: 'Edit passage fixture', status: 'SUBMITTED_FOR_REVIEW' };
    } else if (path === `/api/projects/${projectId}/papers`) {
      json = [{ id: paperId, title: 'Edit paper', originalFilename: 'edit.tex', processingStatus: 'READY' }];
    } else if (path === `/api/projects/${projectId}/sources`) {
      json = { content: [], last: true };
    } else if (path === '/api/feedback-requests') {
      json = [{ id: roundId, projectId, status: 'PENDING', requestedAt: '2026-09-16T08:00:00Z' }];
    } else if (path === `/api/feedback-requests/${roundId}/submission-snapshot`) {
      json = { state: 'AVAILABLE', snapshot: {
        schemaVersion: 1,
        projectId,
        papers: [{ id: paperId, title: 'Edit paper', sections: [{
          id: sectionId, title: 'Introduction', order: 0,
          contentTex: EDIT_CONTENT, contentVersion: 1,
        }] }],
      } };
    } else if (path === `/api/feedback-requests/${roundId}/feedback`) {
      json = [draft];
    } else if (path === `/api/papers/${paperId}/references`
      || path === `/api/papers/${paperId}/references/check`) {
      json = [];
    } else if (method === 'PATCH' && path === '/api/instructor-feedback/draft-one') {
      state.patches.push(JSON.parse(request.postData() || '{}'));
      json = { ...draft, content: JSON.parse(request.postData() || '{}').content || draft.content };
    } else {
      return route.fulfill({ status: 404, json: { message: 'Unhandled fixture request' } });
    }

    return route.fulfill({ json });
  });

  return { projectId, state };
}

async function openEditEditor(page, projectId) {
  await page.goto(`http://localhost:5173/instructor/requests/${projectId}`);
  await expect(page.locator('.cm-content')).toContainText(EDIT_SENTENCE);
  await expect.poll(() => page.evaluate(
    () => document.querySelector('.cm-editor')?.__cmView?.state.doc.length)).toBe(EDIT_CONTENT.length);
}

async function beginEdit(page) {
  await page.getByRole('button', { name: 'Edit', exact: true }).click();
  await expect(page.getByRole('button', { name: 'Update feedback', exact: true })).toBeVisible();
}

test('Text-only edit preserves the stored passage', async ({ page }) => {
  const { projectId, state } = await setupEdit(page);
  await openEditEditor(page, projectId);
  await beginEdit(page);

  // Seeded passage is shown, not whole-section.
  await expect(page.getByText('Selected source range 6–17', { exact: true })).toBeVisible();
  await page.getByPlaceholder('Write feedback on the selected passage').fill('Tighten this wording please.');
  await page.getByRole('button', { name: 'Update feedback', exact: true }).click();

  await expect.poll(() => state.patches.length).toBe(1);
  expect(state.patches[0].content).toBe('Tighten this wording please.');
  expect(state.patches[0].anchor.from).toBe(6);
  expect(state.patches[0].anchor.to).toBe(17);
  expect(state.errors).toEqual([]);
});

test('Edit reselect replaces the passage on the same feedback', async ({ page }) => {
  const { projectId, state } = await setupEdit(page);
  await openEditEditor(page, projectId);
  await beginEdit(page);

  await page.evaluate(() => {
    document.querySelector('.cm-editor').__cmView.dispatch({ selection: { anchor: 23, head: 34 } });
  });
  await page.getByRole('button', { name: 'Use editor selection', exact: true }).click();
  await expect(page.getByText('Selected source range 23–34', { exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Update feedback', exact: true }).click();

  await expect.poll(() => state.patches.length).toBe(1);
  expect(state.patches[0].anchor.from).toBe(23);
  expect(state.patches[0].anchor.to).toBe(34);
  expect(state.errors).toEqual([]);
});

test('Edit remove passage converts to whole-section feedback', async ({ page }) => {
  const { projectId, state } = await setupEdit(page);
  await openEditEditor(page, projectId);
  await beginEdit(page);

  await page.getByRole('button', { name: 'Remove passage', exact: true }).click();
  await page.getByRole('button', { name: 'Update feedback', exact: true }).click();

  await expect.poll(() => state.patches.length).toBe(1);
  expect(state.patches[0].anchor).toBeNull();
  expect(state.errors).toEqual([]);
});

const OVERLAP_SENTENCE = 'The man has hooked a fish';
const OVERLAP_CONTENT = [OVERLAP_SENTENCE, ...Array.from({ length: 60 }, (_, i) => `Filler line ${i}.`)].join('\n\n');

async function setupOverlap(page) {
  const projectId = 'overlap-project';
  const paperId = 'overlap-paper';
  const sectionId = 'overlap-section';
  const roundId = 'overlap-round';
  const state = { errors: [], posts: [], patches: [] };
  const thread = (id, from, to, extra = {}) => ({
    id, requestId: roundId, sectionId,
    content: `Feedback ${id}.`,
    createdAt: '2026-09-16T09:00:00Z', threadState: 'OPEN', pendingState: null,
    publishedAt: '2026-09-16T09:05:00Z', lineReference: null,
    anchor: {
      original: { representation: 'latex-source-lf-v1', offsetUnit: 'utf16', contentVersion: 1,
        fingerprint: '0'.repeat(64), from, to, exact: 'x', prefix: '', suffix: '' },
      current: { status: 'ATTACHED', contentVersion: 1, fingerprint: '0'.repeat(64), from, to },
    },
    studentStatus: null, studentNote: null, attachments: [], replies: [],
    canMarkDone: false, canReopen: false, canEdit: false, canDelete: false,
    ...extra,
  });
  // A covers the whole sentence (0-24); B is nested (10-15).
  const threads = [
    thread('feedback-a', 0, 24, { publishedAt: null, canEdit: true, canDelete: true }),
    thread('feedback-b', 10, 15),
  ];

  page.on('pageerror', error => state.errors.push(error.message));
  await page.addInitScript(() => {
    localStorage.setItem('token', 'overlap-fixture');
    localStorage.setItem('role', 'INSTRUCTOR');
    localStorage.setItem('app_lang', 'en');
    localStorage.setItem('app_theme', 'light');
  });

  await page.route('**/api/**', async route => {
    const request = route.request();
    const path = new URL(request.url()).pathname;
    const method = request.method();
    let json;

    if (path === '/api/users/profile') {
      json = { id: 'instructor-one', role: 'INSTRUCTOR', firstName: 'Test', lastName: 'Instructor' };
    } else if (path === '/api/notifications' || path === '/api/review-guides'
      || path === `/api/projects/${projectId}/evidence-traces`
      || path === `/api/media/projects/${projectId}`) {
      json = [];
    } else if (path === '/api/notifications/unread-count') {
      json = { count: 0 };
    } else if (path === `/api/projects/${projectId}`) {
      json = { id: projectId, title: 'Overlap fixture', status: 'SUBMITTED_FOR_REVIEW' };
    } else if (path === `/api/projects/${projectId}/papers`) {
      json = [{ id: paperId, title: 'Overlap paper', originalFilename: 'overlap.tex', processingStatus: 'READY' }];
    } else if (path === `/api/projects/${projectId}/sources`) {
      json = { content: [], last: true };
    } else if (path === '/api/feedback-requests') {
      json = [{ id: roundId, projectId, status: 'PENDING', requestedAt: '2026-09-16T08:00:00Z' }];
    } else if (path === `/api/feedback-requests/${roundId}/submission-snapshot`) {
      json = { state: 'AVAILABLE', snapshot: {
        schemaVersion: 1,
        projectId,
        papers: [{ id: paperId, title: 'Overlap paper', sections: [{
          id: sectionId, title: 'Introduction', order: 0,
          contentTex: OVERLAP_CONTENT, contentVersion: 1,
        }] }],
      } };
    } else if (method === 'GET' && path === `/api/feedback-requests/${roundId}/feedback`) {
      json = threads;
    } else if (path === `/api/papers/${paperId}/references`
      || path === `/api/papers/${paperId}/references/check`) {
      json = [];
    } else if (method === 'POST' && path === `/api/feedback-requests/${roundId}/feedback`) {
      const body = JSON.parse(request.postData() || '{}');
      state.posts.push(body);
      json = thread('feedback-new', body.anchor.from, body.anchor.to, { content: body.content });
    } else if (method === 'PATCH' && path === '/api/instructor-feedback/feedback-a') {
      const body = JSON.parse(request.postData() || '{}');
      state.patches.push(body);
      json = { ...threads[0], content: body.content };
    } else {
      return route.fulfill({ status: 404, json: { message: 'Unhandled fixture request' } });
    }

    return route.fulfill({ json });
  });

  return { projectId, state };
}

async function openOverlapEditor(page, projectId) {
  await page.goto(`http://localhost:5173/instructor/requests/${projectId}`);
  await expect(page.locator('.cm-content')).toContainText(OVERLAP_SENTENCE);
  await expect.poll(() => page.evaluate(
    () => document.querySelector('.cm-editor')?.__cmView?.state.doc.length)).toBe(OVERLAP_CONTENT.length);
}

async function cmSelect(page, anchor, head) {
  await page.evaluate(({ anchor, head }) => {
    document.querySelector('.cm-editor').__cmView.dispatch({ selection: { anchor, head } });
  }, { anchor, head });
}

test('Nested overlap warns but saves anyway', async ({ page }) => {
  const { projectId, state } = await setupOverlap(page);
  await openOverlapEditor(page, projectId);

  // "fish" is 20-24, nested inside feedback-a (0-24).
  await cmSelect(page, 20, 24);
  await page.getByRole('button', { name: 'Comment', exact: true }).click();
  await expect(page.getByText('This selection overlaps 1 existing feedback item(s).', { exact: false })).toBeVisible();
  await expect(page.getByText('Feedback feedback-a.')).toBeVisible();
  await page.getByPlaceholder('Write feedback on the selected passage').fill('Use a more precise term here.');
  await page.getByRole('button', { name: 'Save feedback', exact: true }).click();

  await expect.poll(() => state.posts.length).toBe(1);
  expect(state.posts[0].anchor.from).toBe(20);
  expect(state.posts[0].anchor.to).toBe(24);
  expect(state.errors).toEqual([]);
});

test('Exact duplicate warns stronger but still saves', async ({ page }) => {
  const { projectId, state } = await setupOverlap(page);
  await openOverlapEditor(page, projectId);

  await cmSelect(page, 0, 24);
  await page.getByRole('button', { name: 'Comment', exact: true }).click();
  await expect(page.getByText('This exact passage already has feedback.', { exact: false })).toBeVisible();
  await page.getByPlaceholder('Write feedback on the selected passage').fill('Second concern on the same text.');
  await page.getByRole('button', { name: 'Save feedback', exact: true }).click();

  await expect.poll(() => state.posts.length).toBe(1);
  expect(state.posts[0].anchor.from).toBe(0);
  expect(state.posts[0].anchor.to).toBe(24);
  expect(state.errors).toEqual([]);
});

test('Edit into another feedback range is allowed', async ({ page }) => {
  const { projectId, state } = await setupOverlap(page);
  await openOverlapEditor(page, projectId);

  await page.getByRole('button', { name: 'Edit', exact: true }).click();
  await expect(page.getByRole('button', { name: 'Update feedback', exact: true })).toBeVisible();
  // Move A (0-24) into B's range (10-15): overlap with B is reported, save stays enabled.
  await cmSelect(page, 12, 14);
  await page.getByRole('button', { name: 'Use editor selection', exact: true }).click();
  await expect(page.getByText('This selection overlaps 1 existing feedback item(s).', { exact: false })).toBeVisible();
  await page.getByRole('button', { name: 'Update feedback', exact: true }).click();

  await expect.poll(() => state.patches.length).toBe(1);
  expect(state.patches[0].anchor.from).toBe(12);
  expect(state.patches[0].anchor.to).toBe(14);
  expect(state.errors).toEqual([]);
});

const PREVIEW_SENTENCE = 'Alpha comparisons beta comparisons gamma.';
const PREVIEW_CONTENT = [PREVIEW_SENTENCE, ...Array.from({ length: 60 }, (_, i) => `Filler line ${i}.`)].join('\n\n');

async function setupPreview(page) {
  const projectId = 'preview-armed-project';
  const paperId = 'preview-paper';
  const sectionId = 'preview-section';
  const roundId = 'preview-round';
  const state = { errors: [], posts: [] };

  page.on('pageerror', error => state.errors.push(error.message));
  await page.addInitScript(() => {
    localStorage.setItem('token', 'preview-armed-fixture');
    localStorage.setItem('role', 'INSTRUCTOR');
    localStorage.setItem('app_lang', 'en');
    localStorage.setItem('app_theme', 'light');
  });

  await page.route('**/api/**', async route => {
    const request = route.request();
    const path = new URL(request.url()).pathname;
    const method = request.method();
    let json;

    if (path === '/api/users/profile') {
      json = { id: 'instructor-one', role: 'INSTRUCTOR', firstName: 'Test', lastName: 'Instructor' };
    } else if (path === '/api/notifications' || path === '/api/review-guides'
      || path === `/api/projects/${projectId}/evidence-traces`
      || path === `/api/media/projects/${projectId}`) {
      json = [];
    } else if (path === '/api/notifications/unread-count') {
      json = { count: 0 };
    } else if (path === `/api/projects/${projectId}`) {
      json = { id: projectId, title: 'Preview armed fixture', status: 'SUBMITTED_FOR_REVIEW' };
    } else if (path === `/api/projects/${projectId}/papers`) {
      json = [{ id: paperId, title: 'Preview paper', originalFilename: 'preview.tex', processingStatus: 'READY' }];
    } else if (path === `/api/projects/${projectId}/sources`) {
      json = { content: [], last: true };
    } else if (path === '/api/feedback-requests') {
      json = [{ id: roundId, projectId, status: 'PENDING', requestedAt: '2026-09-16T08:00:00Z' }];
    } else if (path === `/api/feedback-requests/${roundId}/submission-snapshot`) {
      json = { state: 'AVAILABLE', snapshot: {
        schemaVersion: 1,
        projectId,
        papers: [{ id: paperId, title: 'Preview paper', sections: [{
          id: sectionId, title: 'Introduction', order: 0,
          contentTex: PREVIEW_CONTENT, contentVersion: 1,
        }] }],
      } };
    } else if (method === 'GET' && path === `/api/feedback-requests/${roundId}/feedback`) {
      json = [];
    } else if (path === `/api/papers/${paperId}/references`
      || path === `/api/papers/${paperId}/references/check`) {
      json = [];
    } else if (method === 'POST' && path === `/api/feedback-requests/${roundId}/feedback`) {
      const body = JSON.parse(request.postData() || '{}');
      state.posts.push(body);
      json = { id: 'new-one', requestId: roundId, sectionId, content: body.content,
        createdAt: '2026-09-16T09:00:00Z', threadState: 'OPEN', pendingState: null,
        publishedAt: null, lineReference: null, anchor: null,
        studentStatus: null, studentNote: null, attachments: [], replies: [],
        canMarkDone: false, canReopen: false, canEdit: true, canDelete: true };
    } else {
      return route.fulfill({ status: 404, json: { message: 'Unhandled fixture request' } });
    }

    return route.fulfill({ json });
  });

  return { projectId, state };
}

async function openPreviewEditor(page, projectId) {
  await page.goto(`http://localhost:5173/instructor/requests/${projectId}`);
  await expect(page.locator('.cm-content')).toContainText(PREVIEW_SENTENCE);
  await expect.poll(() => page.evaluate(
    () => document.querySelector('.cm-editor')?.__cmView?.state.doc.length)).toBe(PREVIEW_CONTENT.length);
}

async function previewDomSelect(page) {
  await page.evaluate(() => {
    const host = document.querySelector('.preview-content');
    const textNode = [...host.querySelectorAll('*')]
      .map(el => [...el.childNodes].find(n => n.nodeType === 3 && n.textContent.includes('comparisons')))
      .find(Boolean);
    const range = document.createRange();
    range.setStart(textNode, 6);
    range.setEnd(textNode, 17);
    const selection = window.getSelection();
    selection.removeAllRanges();
    selection.addRange(range);
  });
  await page.locator('.preview-content').first().dispatchEvent('mouseup');
}

test('Armed editor passage survives Preview and saves exactly', async ({ page }) => {
  const { projectId, state } = await setupPreview(page);
  await openPreviewEditor(page, projectId);

  // Select the SECOND "comparisons" (23-34), arm via FAB, then open Preview.
  await cmSelect(page, 23, 34);
  await page.getByRole('button', { name: 'Comment', exact: true }).click();
  await expect(page.getByText('Selected source range 23–34', { exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Preview', exact: true }).click();
  await expect(page.locator('.preview-content').getByText(PREVIEW_SENTENCE, { exact: true })).toBeVisible();

  // The armed passage is still consumable while Preview is visible.
  await expect(page.getByText('Selected source range 23–34', { exact: true })).toBeVisible();
  await page.getByPlaceholder('Write feedback on the selected passage').fill('Second comparisons is vague.');
  await page.getByRole('button', { name: 'Save feedback', exact: true }).click();

  await expect.poll(() => state.posts.length).toBe(1);
  expect(state.posts[0].anchor.from).toBe(23);
  expect(state.posts[0].anchor.to).toBe(34);
  expect(state.errors).toEqual([]);
});

test('Preview selection with an armed passage keeps it usable', async ({ page }) => {
  const { projectId, state } = await setupPreview(page);
  await openPreviewEditor(page, projectId);

  await cmSelect(page, 23, 34);
  await page.getByRole('button', { name: 'Comment', exact: true }).click();
  await page.getByRole('button', { name: 'Preview', exact: true }).click();
  await previewDomSelect(page);

  await expect(page.getByText('Your editor selection (23–34) is still armed', { exact: false })).toBeVisible();
  await expect(page.getByText('Selected source range 23–34', { exact: true })).toBeVisible();
  expect(state.errors).toEqual([]);
});

test('Preview selection with nothing armed shows honest guidance', async ({ page }) => {
  const { projectId, state } = await setupPreview(page);
  await openPreviewEditor(page, projectId);

  await page.getByRole('button', { name: 'Preview', exact: true }).click();
  await previewDomSelect(page);

  await expect(page.getByText("Select the passage in the editor to attach precise feedback.", { exact: false })).toBeVisible();
  await expect(page.getByText(/Selected source range \d+–\d+/)).toHaveCount(0);
  expect(state.errors).toEqual([]);
});

async function setupHistory(page) {
  const projectId = 'history-project';
  const paperId = 'history-paper';
  const sectionId = 'history-section';
  const state = { errors: [] };
  const snapshot = {
    state: 'AVAILABLE',
    snapshot: {
      schemaVersion: 1,
      projectId,
      papers: [{ id: paperId, title: 'History paper', sections: [{
        id: sectionId, title: 'Introduction', order: 0,
        contentTex: PREVIEW_CONTENT, contentVersion: 3,
      }] }],
    },
  };
  const item = (id, requestId, content, createdAt, published = true) => ({
    id, requestId, sectionId, content, createdAt,
    updatedAt: createdAt, threadState: 'OPEN', pendingState: null,
    publishedAt: published ? createdAt : null, lineReference: null, anchor: null,
    studentStatus: null, studentNote: null, attachments: [],
    replies: [{ id: `${id}-reply`, authorName: 'Former Student', authorRole: 'STUDENT',
      content: `Legacy reply on ${id}.`, createdAt }],
    canMarkDone: false, canReopen: false, canEdit: false, canDelete: false,
  });

  page.on('pageerror', error => state.errors.push(error.message));
  await page.addInitScript(() => {
    localStorage.setItem('token', 'history-fixture');
    localStorage.setItem('role', 'INSTRUCTOR');
    localStorage.setItem('app_lang', 'en');
    localStorage.setItem('app_theme', 'light');
  });

  await page.route('**/api/**', async route => {
    const request = route.request();
    const path = new URL(request.url()).pathname;
    const method = request.method();
    let json;

    if (path === '/api/users/profile') {
      json = { id: 'instructor-one', role: 'INSTRUCTOR', firstName: 'Test', lastName: 'Instructor' };
    } else if (path === '/api/notifications' || path === '/api/review-guides'
      || path === `/api/projects/${projectId}/evidence-traces`
      || path === `/api/media/projects/${projectId}`) {
      json = [];
    } else if (path === '/api/notifications/unread-count') {
      json = { count: 0 };
    } else if (path === `/api/projects/${projectId}`) {
      json = { id: projectId, title: 'History fixture', status: 'SUBMITTED_FOR_REVIEW' };
    } else if (path === `/api/projects/${projectId}/papers`) {
      json = [{ id: paperId, title: 'History paper', originalFilename: 'history.tex', processingStatus: 'READY' }];
    } else if (path === `/api/projects/${projectId}/sources`) {
      json = { content: [], last: true };
    } else if (path === '/api/feedback-requests') {
      json = [
        { id: 'round-b', projectId, status: 'PENDING', requestedAt: '2026-09-17T08:00:00Z' },
        { id: 'round-a', projectId, status: 'RETURNED', requestedAt: '2026-09-16T08:00:00Z' },
      ];
    } else if (path === '/api/feedback-requests/round-b/submission-snapshot') {
      json = snapshot;
    } else if (path === '/api/feedback-requests/round-a/submission-snapshot') {
      json = snapshot;
    } else if (method === 'GET' && path === '/api/feedback-requests/round-b/feedback') {
      json = [];
    } else if (method === 'GET' && path === '/api/feedback-requests/round-a/feedback') {
      json = [
        item('prev-old', 'round-a', 'Older previous feedback.', '2026-09-16T09:00:00Z'),
        item('prev-new', 'round-a', 'Newer previous feedback.', '2026-09-16T10:00:00Z'),
        item('prev-draft', 'round-a', 'Unpublished previous draft.', '2026-09-16T11:00:00Z', false),
      ];
    } else if (path === `/api/papers/${paperId}/references`
      || path === `/api/papers/${paperId}/references/check`) {
      json = [];
    } else {
      return route.fulfill({ status: 404, json: { message: 'Unhandled fixture request' } });
    }

    return route.fulfill({ json });
  });

  return { projectId, state };
}

test('History shows the previous round latest card read-only', async ({ page }) => {
  const { projectId, state } = await setupHistory(page);
  await page.goto(`http://localhost:5173/instructor/requests/${projectId}`);
  await expect(page.locator('.cm-content')).toContainText(PREVIEW_SENTENCE);

  await page.getByRole('button', { name: 'History', exact: true }).click();
  await expect(page.getByText('Newer previous feedback.')).toBeVisible();
  await expect(page.getByText('Legacy reply on prev-new.')).toBeVisible();
  await expect(page.getByText('Older previous feedback.')).toHaveCount(0);
  await expect(page.getByText('Unpublished previous draft.')).toHaveCount(0);
  // Read-only: the History tab mounts no composer and the shared card has no inputs.
  await expect(page.getByPlaceholder('Write feedback on the selected passage')).toHaveCount(0);
  expect(state.errors).toEqual([]);
});
