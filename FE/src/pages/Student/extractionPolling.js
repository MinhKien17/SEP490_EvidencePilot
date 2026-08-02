const ACTIVE_EXTRACTION_STATES = new Set([
  'PENDING_UPLOAD',
  'UPLOADED',
  'METADATA_FETCHED',
  'PDF_DOWNLOADED',
  'QUEUED',
  'PROCESSING',
  'RAW_EXTRACTED',
]);

export const hasActiveExtraction = (sources = []) =>
  sources.some(source => ACTIVE_EXTRACTION_STATES.has(source.processingStatus));
