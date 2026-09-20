# Evidence Pilot instructor-safety roadmap execution

## Task ledger

- [ ] A — safety and contract foundations
- [ ] B — lifecycle and assignment workflows
- [ ] C — structure, references, and extraction
- [ ] D — request/review workspace
- [ ] E — realtime, deterministic time, and i18n
- [ ] F — Ponytail debt ledger
- [ ] G — low-priority UX polish

## Rulings

- The isolated checkout is the app-managed worktree at `C:/Users/Admin/.codex/worktrees/evidence-pilot-instructor-safety/SEP490_EvidencePilot` because the original checkout's Git metadata is read-only.
- The repository currently contains a Java migration `V37_1__SeedReviewSnapshots` in addition to SQL migrations. Existing migration-count assertions (39 and 37) are stale against the current 40-migration runtime and will be updated once roadmap migrations are complete.
- The baseline MySQL paper-reference integration test fails before assertions because its `@DataJpaTest` slice does not provide `PaperStandardService`; this is a test-context defect to isolate separately from roadmap behavior.

## Minor issues

- Frontend tests/build currently hit Windows `spawn EPERM` and a Tailwind native-binding issue; frontend setup and verification remain outstanding.
