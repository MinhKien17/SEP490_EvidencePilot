import assert from 'node:assert/strict';
import test from 'node:test';

import { selectPreviousCard } from '../historySelector.js';

const requests = [
  { id: 'round-c', requestedAt: '2026-09-18T08:00:00Z' },
  { id: 'round-b', requestedAt: '2026-09-17T08:00:00Z' },
  { id: 'round-a', requestedAt: '2026-09-16T08:00:00Z' },
];

const item = (overrides = {}) => ({
  id: 'one',
  requestId: 'round-b',
  sectionId: 'section-1',
  content: 'Previous feedback.',
  createdAt: '2026-09-17T09:00:00Z',
  updatedAt: '2026-09-17T09:00:00Z',
  publishedAt: '2026-09-17T09:05:00Z',
  ...overrides,
});

test('returns the latest created published root of the previous round', () => {
  const items = [
    item({ id: 'older', createdAt: '2026-09-17T08:00:00Z' }),
    item({ id: 'newer', createdAt: '2026-09-17T10:00:00Z' }),
  ];
  assert.equal(selectPreviousCard(items, requests, 'round-c', 'section-1')?.id, 'newer');
});

test('excludes drafts, other sections, and other rounds', () => {
  const items = [
    item({ id: 'draft', publishedAt: null, createdAt: '2026-09-17T11:00:00Z' }),
    item({ id: 'wrong-section', sectionId: 'section-9' }),
    item({ id: 'current-round', requestId: 'round-c' }),
    item({ id: 'older-round', requestId: 'round-a' }),
  ];
  assert.equal(selectPreviousCard(items, requests, 'round-c', 'section-1'), null);
});

test('updatedAt edits never reorder history', () => {
  const items = [
    item({ id: 'older', createdAt: '2026-09-17T08:00:00Z', updatedAt: '2026-09-17T12:00:00Z' }),
    item({ id: 'newer', createdAt: '2026-09-17T10:00:00Z', updatedAt: '2026-09-17T10:00:00Z' }),
  ];
  assert.equal(selectPreviousCard(items, requests, 'round-c', 'section-1')?.id, 'newer');
});

test('previous round means array position after active, not timestamp scan', () => {
  const items = [
    item({ id: 'x', requestId: 'round-a' }),
    item({ id: 'y', requestId: 'round-b' }),
  ];
  // Active round-c → previous is round-b (position 1), so 'y' wins even
  // though both items exist.
  assert.equal(selectPreviousCard(items, requests, 'round-c', 'section-1')?.id, 'y');
  // Active round-b → previous is round-a.
  assert.equal(selectPreviousCard(items, requests, 'round-b', 'section-1')?.id, 'x');
});

test('single round or unknown active yields null', () => {
  assert.equal(selectPreviousCard([item({ requestId: 'round-c' })], requests, 'round-c', 'section-1'), null);
  assert.equal(selectPreviousCard([item()], requests, 'missing', 'section-1'), null);
  assert.equal(selectPreviousCard([], requests, 'round-c', 'section-1'), null);
});
