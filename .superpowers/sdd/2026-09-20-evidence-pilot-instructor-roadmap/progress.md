# Evidence Pilot instructor-safety roadmap execution

## Task ledger

- [x] A — safety and contract foundations
- [x] B — lifecycle and assignment workflows
- [x] C — structure, references, and extraction (C1 structural lock + C2 explicit section kind + C3 safe project-scoped source unsharing + C4 staged extraction complete)
- [x] D — request/review workspace
- [x] E — realtime, deterministic time, and i18n
- [x] F — complexity-debt ledger
- [x] G — low-priority UX polish

## Rulings

- C4 re-extraction now stages one candidate, preserves live text/chunks/vectors until activation, rejects meaningful work/history and stale file fingerprints at request or activation, restores the prior live status on candidate failure, and cleans the old checkpoint only after activation.

- The isolated checkout is the app-managed worktree at `C:/Users/Admin/.codex/worktrees/evidence-pilot-instructor-safety/SEP490_EvidencePilot` because the original checkout's Git metadata is read-only.
- The repository currently contains a Java migration `V37_1__SeedReviewSnapshots` in addition to SQL migrations; migration-count assertions now match the 43-migration runtime (fresh=43, baseline=38, after=4).
- The paper-reference MySQL slice now supplies its `PaperStandardService` collaborator and passes its integration assertions.
- C1 structural edits now reject active sections with current work or meaningful history, including sections that are currently unassigned.
- C2 section kind is persisted as STANDARD/REFERENCE, backfilled only from explicit English reference headings; subsequent assignment, readiness, progress, reference checking, citation-review exemption, and UI behavior use the enum rather than mutable titles.
- C3 source removal is a transactional, all-or-nothing bulk operation. It removes only the selected project's association/ownership link, retains the document/library row, and blocks active paper references, evidence-review traces, and in-flight extraction.

- D request queue is instructor-scoped on the server, returns only the current request per project, supports project/status/date/search filters, uses stable newest-first ordering, and exposes page metadata. The instructor page no longer slices a broad client-side list and includes an explicit guide.
- E notification reloads are reconciled after reconnect, incoming events are deduplicated, Mark All Read is a single current-user bulk update, membership removal is surfaced before redirect, and shared timestamps use the fixed Vietnam timezone formatter.
- F the former complexity-marker comments were reviewed and categorized as implemented behavior, compatibility rationale, or deliberately bounded simple behavior. OTP cleanup and admin extraction date filtering were implemented; all remaining notes are ordinary rationale comments, and the repository-wide marker-token scan is empty.
- G instructor project, collection, and review-request action bars remain visible below the shared 16px header while scrolling.

## Minor issues

- Frontend dependencies are installed in the isolated worktree. Focused and full Node verification passes, and the production build passes; the earlier Windows spawn/native-binding failures were environment/setup issues.
