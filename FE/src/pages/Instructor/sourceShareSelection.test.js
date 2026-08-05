import assert from 'node:assert/strict';
import test from 'node:test';

import { getSourceShareChanges } from './sourceShareSelection.js';

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
