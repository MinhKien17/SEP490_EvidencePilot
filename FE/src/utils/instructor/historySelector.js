// ponytail: History shows one card — the latest created published root of the
// immediately previous round for this section. Previous round = array position
// after active in the canonical desc list (no second timestamp sort).
// createdAt orders (ISO); updatedAt never does. Drafts are excluded.

export function selectPreviousCard(feedbackItems, orderedRequests, activeRequestId, sectionId) {
  const requests = orderedRequests || [];
  const activeIndex = requests.findIndex(request => String(request.id) === String(activeRequestId));
  if (activeIndex < 0 || activeIndex + 1 >= requests.length) return null;
  const previousId = requests[activeIndex + 1].id;
  let best = null;
  for (const item of feedbackItems || []) {
    if (String(item.requestId) !== String(previousId)) continue;
    if (String(item.sectionId) !== String(sectionId)) continue;
    if (!item.publishedAt) continue;
    if (!best || String(item.createdAt || '') > String(best.createdAt || '')) best = item;
  }
  return best;
}
