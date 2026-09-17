import { test, expect } from '@playwright/test';

test.use({ channel: 'chrome', viewport: { width: 1440, height: 1000 } });

const baseUrl = 'http://localhost:5173';

const BEFORE = 'Machine learning systems require testing.';
const AFTER = 'Machine learning systems require extensive testing.';
const CONTENT = [AFTER, ...Array.from({ length: 60 }, (_, i) => `Filler line ${i}.`)].join('\n\n');

async function setupDiff(page, comparisonSource) {
  const projectId = 'diff-source-project';
  const paperId = 'diff-paper';
  const sectionId = 'diff-section';
  const roundId = 'diff-round';
  const state = { errors: [], unhandled: [], comparisonCalls: [], snapshotCalls: [] };

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
          contentTex: CONTENT, contentVersion: 2,
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

  return { projectId, state };
}

async function openReviewEditor(page, projectId) {
  await page.goto(`${baseUrl}/instructor/requests/${projectId}`);
  await expect(page.locator('.cm-content')).toContainText('extensive testing');
  await expect.poll(() => page.evaluate(
    () => document.querySelector('.cm-editor')?.__cmView?.state.doc.length)).toBe(CONTENT.length);
}

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
