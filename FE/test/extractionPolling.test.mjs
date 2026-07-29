import test from 'node:test';
import assert from 'node:assert/strict';
import { hasActiveExtraction } from '../src/pages/Student/extractionPolling.js';

test('polls only for non-terminal extraction states', () => {
  assert.equal(hasActiveExtraction([{ processingStatus: 'QUEUED' }]), true);
  assert.equal(hasActiveExtraction([{ processingStatus: 'PROCESSING' }]), true);
  assert.equal(hasActiveExtraction([{ processingStatus: 'RAW_EXTRACTED' }]), true);
  assert.equal(hasActiveExtraction([{ processingStatus: 'READY' }]), false);
  assert.equal(hasActiveExtraction([{ processingStatus: 'FAILED' }]), false);
  assert.equal(hasActiveExtraction([{ processingStatus: 'METADATA_FETCHED' }]), false);
});
