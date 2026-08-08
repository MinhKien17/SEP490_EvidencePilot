const SHAREABLE_STATUSES = ['READY', 'COMPLETED'];

export function isSourceShareable(source) {
  return SHAREABLE_STATUSES.includes(source?.processingStatus);
}

export function isSourceSharedWithProject(source, projectId) {
  return (source?.projectIds || []).some(id => String(id) === String(projectId));
}

export function getBlockedSources(collectionSources, selectedSourceIds) {
  const selected = new Set(selectedSourceIds.map(String));
  return collectionSources
    .filter(source => selected.has(String(source.id)) && !isSourceShareable(source))
    .map(source => ({
      id: source.id,
      title: source.title || source.originalFilename || source.id,
      status: source.processingStatus || 'UNKNOWN',
    }));
}

export function getSourceShareChanges(collectionSources, projectId, selectedSourceIds) {
  const selected = new Set(selectedSourceIds.map(String));
  const shared = new Set(collectionSources
    .filter(source => isSourceSharedWithProject(source, projectId))
    .map(source => String(source.id)));

  return {
    toShare: collectionSources
      .filter(source => selected.has(String(source.id)) && !shared.has(String(source.id)))
      .map(source => source.id),
    toUnshare: collectionSources
      .filter(source => shared.has(String(source.id)) && !selected.has(String(source.id)))
      .map(source => source.id),
  };
}
