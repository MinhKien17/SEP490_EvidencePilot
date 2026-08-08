import assert from 'node:assert/strict';
import test from 'node:test';

import { getSourceShareChanges, getBlockedSources, isSourceShareable } from './sourceShareSelection.js';

test('changes only sources from the selected collection for the target project', () => {
  const sources = [
    { id: 'keep', projectIds: ['project-a'] },
    { id: 'remove', projectIds: ['project-a'] },
    { id: 'add', projectIds: [] },
    { id: 'shared-elsewhere', projectIds: ['project-b'] },
  ];

  assert.deepEqual(
    getSourceShareChanges(sources, 'project-a', ['keep', 'add', 'shared-elsewhere', 'outside']),
    { toShare: ['add', 'shared-elsewhere'], toUnshare: ['remove'] },
  );
});

test('isSourceShareable accepts only READY or COMPLETED', () => {
  assert.equal(isSourceShareable({ processingStatus: 'READY' }), true);
  assert.equal(isSourceShareable({ processingStatus: 'COMPLETED' }), true);
  assert.equal(isSourceShareable({ processingStatus: 'PROCESSING' }), false);
  assert.equal(isSourceShareable({ processingStatus: 'METADATA_FETCHED' }), false);
  assert.equal(isSourceShareable({ processingStatus: undefined }), false);
});

test('getBlockedSources lists selected non-shareable sources with title and status', () => {
  const sources = [
    { id: 'ready', title: 'Ready paper', processingStatus: 'READY' },
    { id: 'busy', title: 'Still processing', processingStatus: 'PROCESSING' },
    { id: 'no-status', title: null, originalFilename: 'legacy.pdf', processingStatus: null },
  ];

  assert.deepEqual(
    getBlockedSources(sources, ['ready', 'busy', 'no-status', 'missing']),
    [
      { id: 'busy', title: 'Still processing', status: 'PROCESSING' },
      { id: 'no-status', title: 'legacy.pdf', status: 'UNKNOWN' },
    ],
  );
});
