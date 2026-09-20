# Evidence Pilot instructor-safety roadmap execution

## Task ledger

- [ ] A — safety and contract foundations
- [ ] B — lifecycle and assignment workflows
- [ ] C — structure, references, and extraction (C1 structural lock + C2 explicit section kind + C3 safe project-scoped source unsharing complete)
- [ ] D — request/review workspace
- [ ] E — realtime, deterministic time, and i18n
- [ ] F — Ponytail debt ledger
- [ ] G — low-priority UX polish

## Rulings

- C4 re-extraction now stages one candidate, preserves live text/chunks/vectors until activation, rejects meaningful work/history and stale file fingerprints at request or activation, restores the prior live status on candidate failure, and cleans the old checkpoint only after activation.

- The isolated checkout is the app-managed worktree at `C:/Users/Admin/.codex/worktrees/evidence-pilot-instructor-safety/SEP490_EvidencePilot` because the original checkout's Git metadata is read-only.
- The repository currently contains a Java migration `V37_1__SeedReviewSnapshots` in addition to SQL migrations. Existing migration-count assertions (39 and 37) are stale against the current 40-migration runtime and will be updated once roadmap migrations are complete.
- The baseline MySQL paper-reference integration test fails before assertions because its `@DataJpaTest` slice does not provide `PaperStandardService`; this is a test-context defect to isolate separately from roadmap behavior.
- C1 structural edits now reject active sections with current work or meaningful history, including sections that are currently unassigned.
- C2 section kind is persisted as STANDARD/REFERENCE, backfilled only from explicit English reference headings; subsequent assignment, readiness, progress, reference checking, citation-review exemption, and UI behavior use the enum rather than mutable titles.
- C3 source removal is a transactional, all-or-nothing bulk operation. It removes only the selected project's association/ownership link, retains the document/library row, and blocks active paper references, evidence-review traces, and in-flight extraction.

## Minor issues

- Frontend dependencies are installed in the isolated worktree. The full Node test suite now passes (182/182), and the production build passes; the earlier Windows spawn/native-binding failures were environment/setup issues.
