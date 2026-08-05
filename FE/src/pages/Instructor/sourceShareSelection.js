export function getSourceShareChanges(collectionSources, projectId, selectedSourceIds) {
  const targetProjectId = String(projectId);
  const selected = new Set(selectedSourceIds.map(String));
  const shared = new Set(collectionSources
    .filter(source => (source.projectIds || []).some(id => String(id) === targetProjectId))
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
