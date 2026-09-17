# Instructor Review Workspace Major Changes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Make the Instructor Review Workspace use assignment/Return baselines for revision comparison, simplify feedback to one-way review, support editable feedback selections and overlapping feedback, enable Preview feedback through the same canonical selection model, and render meaningful previous-round History.

**Architecture:** Keep the existing backbone (`FeedbackController` → `FeedbackServiceImpl` → `FeedbackRequest`/`InstructorFeedback`/`ReviewSectionSnapshot`; hook `useInstructorReview`; card `FeedbackThreadsTab`). Add one small append-only table for the initial handoff baseline, one server-side comparison resolver + read endpoint, one pure client overlap utility, one shared read-only card. Delete reply/state/ack write paths together with their last callers. No second passage model, no overlap backend, no History endpoint, no schema deletion, no compatibility tombstones.

**Tech Stack:** BE Java 21 / Spring Boot 3.3.5, Flyway (MySQL prod; H2 MySQL-mode unit tests; Testcontainers-gated MySQL tests), JUnit 5 + AssertJ. FE React 19 + Vite 8, CodeMirror 6, `node:test` unit tests, Playwright (`FE/e2e/feedback-anchor-fab.spec.js` — follow its harness in a new spec file).

**Spec:** `Instructor-Review-Workspace-Audit.md` (evidence; where this plan differs — baseline rule, no `argmax`, no block anchors, no tombstones — **this plan wins**).

## Global Constraints

- Never edit applied Flyway migrations (V1–V37 + `V37_1`); never drop `feedback_replies`, `feedback_attachments`, reply FKs, `threadState`/`pendingState`/`studentStatus` columns or checks, `replies[]`/status DTO fields, or `FeedbackReply`/`FeedbackReplyResponseDto` reads.
- Never manufacture baseline data: absent baseline → honest `Comparison baseline unavailable`, never a relabeled submission.
- One canonical passage model everywhere: `sectionId` + `{from,to,contentVersion,fingerprint,representation:'latex-source-lf-v1',offsetUnit:'utf16'}` over LF-normalized source. No `editorAnchor`/`previewAnchor` split in any DTO, entity, or API.
- Overlap is legal in create, edit, render, and click. Never merge/split/resize/delete/block on overlap grounds; no conflict resolution, priorities, or ownership.
- Request-level Reject (`PATCH …/status?status=REJECTED` → `rejectReview:487-508`, button `rejectSubmission`) is a separate whole-submission action and must keep working; name it in every removal step so it is never touched.
- Internal "anchor" terms stay in code (`anchorJson`, `FeedbackAnchorService`); UI language is "selected passage". No "Manage Anchor" feature, no standalone re-anchor workflow once Edit covers reselection.
- Snapshot title/order freeze: NOT implemented (YAGNI — no requirement needs it).
- No `expectedRevision` on feedback update (does not exist in the update contract today; passage safety comes from existing `FeedbackAnchorService.create` validators + draft preservation on failure).
- No permanent 410 tombstones (no external clients exist — README shows only the SPA; no `/v1` paths; `OpenApiConfig` makes no stability promise). Obsolete mappings are deleted with their last callers.
- Every task below is red → implement → green → commit. No task finishes red.

---

### Task 0: Verify current state

**Files**
- Create: none
- Modify: none
- Delete: none
- Test: none (read-only)

**Interfaces**
- Consumes: git worktree, build tooling
- Produces: recorded baseline (commit nothing)

**Invariant**
- Later tasks assume HEAD `78003a7` symbols/lines cited here.

- [ ] Step 1: Run `git rev-parse HEAD; git status --short` (workdir repo root); expect `78003a7`, only pre-existing `M BE/src/test/java/com/evidencepilot/controller/ProjectRouteMappingTest.java` (leave untouched).
- [ ] Step 2: Spot-check symbols still exist: `grep -n "isThreadClosed\|postReply\|applyStudentState" BE/src/main/java/com/evidencepilot/service/impl/FeedbackServiceImpl.java`, `grep -n "postReply\|prepareState\|describePreviewSelection" FE/src/hooks/useInstructorReview.js FE/src/components/features/PreviewPane.jsx`, and read `InstructorFeedbackPanel.jsx:88-102` (History still metadata-only).
- [ ] Step 3: Record build baseline only (do not fix): `mvn -q -DskipTests compile` (workdir `BE/`), `npm run build` (workdir `FE/`).
- [ ] Step 4: Record test baseline: Docker-gated list (`FeedbackRevisionMySqlTest`, `FeedbackPublicationMySqlTest`, `FlywayMigrationMySqlTest`, `AdminSeedMySqlTest`, `PromptTemplateMySqlTest`, `AiGenerationConfigMySqlTest`, `PaperReferenceMySqlTest`, `PaperReferenceIntegrationMySqlTest` — all `disabledWithoutDocker`); H2-safe `mvn -q -Dtest='FeedbackServiceImplTest' test` (26 pass at baseline); `npm test` (105 pass at baseline).
- [ ] Step 5: No regressions (nothing changed).
- [ ] Step 6: No commit (verification only; paste results into the plan PR description).

---

### Task 1: Capture immutable first-handoff baseline

**Files**
- Create: `BE/src/main/resources/db/migration/V38__assignment_section_baselines.sql`; `BE/src/main/java/com/evidencepilot/model/AssignmentSectionBaseline.java`; `BE/src/main/java/com/evidencepilot/repository/AssignmentSectionBaselineRepository.java`; `BE/src/test/java/com/evidencepilot/service/AssignmentBaselineCaptureTest.java`
- Modify: `BE/src/main/java/com/evidencepilot/service/impl/PaperProcessingServiceImpl.java` (two hook sites); `BE/src/main/java/com/evidencepilot/service/AdminExcelSeedService.java` (one hook site); `BE/src/test/java/com/evidencepilot/migration/FlywayMigrationMySqlTest.java` (V38 assertions)
- Delete: none
- Test: `AssignmentBaselineCaptureTest`, `FlywayMigrationMySqlTest`

**Interfaces**
- Consumes: saved `PaperSection` + project + `now` at assignment events
- Produces: ≤1 row per `(project_id, section_id)`; reassignment/member ops produce nothing new

**Invariant**
- Baseline-producing events = first handoff per section + Return for Revision (Task 2 reuses Return rows). Reassignment (`Alice→Bob`), unassign, and member add/remove/role-change never alter the row. No separate `CREATED→ASSIGNED` hook: the flip occurs only inside the two hooked methods (`PaperProcessingServiceImpl.java:422-423,986-987`), so hooking assignment covers the handoff moment by construction.

- [ ] Step 1: Write `V38__assignment_section_baselines.sql`: `CREATE TABLE assignment_section_baselines (id BINARY(16) PK, project_id BINARY(16) NOT NULL → projects CASCADE, section_id BINARY(16) NOT NULL → paper_sections CASCADE, content_tex LONGTEXT NOT NULL, content_version INT NULL, created_at DATETIME(6) NOT NULL DEFAULT NOW(6), UNIQUE(project_id, section_id), INDEX idx_asb_lookup(project_id, section_id, created_at))`. No backfill, no `assigned_user_id` (nothing consumes it). Write entity (relations like `ReviewSectionSnapshot`, `@UniqueConstraint(project_id, section_id)`), repository (`boolean existsByProjectIdAndSectionId(UUID, UUID)` + `Optional<…> findByProjectIdAndSectionId(UUID, UUID)`), `FlywayMigrationMySqlTest` V38 assertions, and `AssignmentBaselineCaptureTest` (H2): single-assign writes exact content/version; batch newly-assigned only; reassignment inserts nothing (row unchanged); unassign inserts nothing and deletes nothing; member add/role-change inserts nothing; unassigned `createSection` inserts nothing; duplicate insert attempt is swallowed via `DataIntegrityViolationException` (race safety).
- [ ] Step 2: Run `mvn -q -Dtest='AssignmentBaselineCaptureTest,FlywayMigrationMySqlTest' test` (workdir `BE/`; Docker-gated may skip — record); verify failures name missing table/methods, not infra.
- [ ] Step 3: Implement private `captureInitialBaseline(project, section, now)` (exists-check → save; swallow duplicate-key on race) and call it: (a) `assignSection` after `:418 flush()`, inside `if (assignedUserId != null)` (`:420-433`, after project save `:425`, before notify `:427`); (b) batch persist loop inside the `:961` change block under `if (newAssignee != null)` (`:965`), reading `section.getContentTex()/getVersion()` after the content block (`:938-950`); (c) `AdminExcelSeedService` after `:1293` save when the CSV assignee was applied (`:1288` — the only born-assigned creation path; all other creators never set assignees). Use `saved.getVersion()` (business version; never `optVersion`). Capture must not alter status/notification/handoff behavior.
- [ ] Step 4: Run `mvn -q -Dtest='AssignmentBaselineCaptureTest,FlywayMigrationMySqlTest' test` → PASS.
- [ ] Step 5: Run `mvn -q -Dtest='PaperProcessingServiceImplTest,SubmissionReadinessServiceTest,FeedbackRevisionMySqlTest' test` (record Docker skips) and confirm `ASSIGNED→IN_PROGRESS` student-edit flow untouched.
- [ ] Step 6: Commit `feat: capture immutable first-handoff baseline per section`.

---

### Task 2: Resolve comparison source server-side

**Files**
- Create: `BE/src/main/java/com/evidencepilot/dto/response/ComparisonSourceDto.java` (`submitted{contentTex,contentVersion}`, `baseline{contentTex,contentVersion,origin}|null`, `origin ∈ INITIAL_ASSIGNMENT | RETURN_FOR_REVISION`); `BE/src/test/java/com/evidencepilot/service/ComparisonSourceTest.java`
- Modify: `BE/src/main/java/com/evidencepilot/service/impl/FeedbackServiceImpl.java` (new `getComparisonSource` + private resolver); `BE/src/main/java/com/evidencepilot/controller/FeedbackController.java` (new `GET /feedback-requests/{id}/comparison-source?sectionId=`); `BE/src/test/java/com/evidencepilot/controller/FeedbackControllerTest.java` (endpoint cases)
- Delete: none
- Test: `ComparisonSourceTest`, `FeedbackControllerTest`

**Interfaces**
- Consumes: `requestId + sectionId` (auth: existing `requireFeedbackAccess(...,false)`; 400 if section not in project)
- Produces: `{submitted (required — missing → 409 code NO_SUBMITTED_SNAPSHOT), baseline (nullable)}`; frontend must not reimplement selection

**Invariant**
- `comparisonBaseline = latest earlier RETURNED request's BASELINE for this section ?? initial assignment baseline ?? unavailable`. Strict `RETURNED`-only (never `REVIEWED`/Approve, never request-Reject — neither writes a legitimate baseline; the status guard also neutralizes `V37_1`-seeded copies on never-returned requests). No cross-event timestamp `argmax`. Previous request = position after active in same-project `requestedAt`-desc order (deterministic; ties keep DB order).

- [ ] Step 1: Write `ComparisonSourceTest` (H2): assign→submitA resolves `INITIAL_ASSIGNMENT` vs A (Case 1); ReturnA→edit→submitB resolves A-return `BASELINE` vs B (Case 2); ReturnB→submitC resolves B baseline, not A (Case 3); identical content → both sides equal (Case 4 asserts equality, emptiness asserted client-side in Task 3); legacy first-review with no assignment row and no prior RETURNED → `baseline:null` (Case 5); seeded BASELINE copy on never-returned prior request ignored; post-Reject resubmit reuses latest assignment/Return baseline (Reject writes none — `IN_PROGRESS` resubmit path stays valid). Controller tests: 200 shape, 409 missing-submitted, 400 foreign-section, 401/403 passthrough.
- [ ] Step 2: Run `mvn -q -Dtest='ComparisonSourceTest,FeedbackControllerTest' test` (workdir `BE/`); verify red names missing resolver/endpoint.
- [ ] Step 3: Implement resolver exactly: load active (404); load same-project requests desc; scan positions after active for first `status==RETURNED` with a `BASELINE` row for the section (per-section fallback continues scanning, then initial row via repo `findByProjectIdAndSectionId`); tag origin; return DTO. Touch nothing in `writeSnapshots`/`returnReview`/`submitForReview` (Return BASELINE content is already exact).
- [ ] Step 4: Focused tests → PASS.
- [ ] Step 5: Run `mvn -q -Dtest='FeedbackServiceImplTest,FeedbackRevisionMySqlTest,ProjectRouteMappingTest' test` (record Docker skips).
- [ ] Step 6: Commit `feat: server-side comparison-source resolution`.

---

### Task 3: Switch Show Diff to comparison source

**Files**
- Create: none (pure reuse)
- Modify: `FE/src/hooks/useInstructorReview.js:251-301` (fetch comparison-source; delete single-request `find(BASELINE/SUBMITTED)` lines `:266-267`); `FE/src/components/Student/EditorPanel.jsx:263-274` (unavailable notice slot); `FE/src/locales/en.json` + `vi.json` (one key `instructor.review.noComparisonBaseline`: EN "Comparison baseline unavailable for this section — showing submitted content without changes." / VI "Không có mốc so sánh cho phần này — hiển thị nội dung đã nộp, không có thay đổi.")
- Delete: the old in-effect baseline/submitted picking (lines above); `GET …/section-snapshots` endpoint itself stays (route/test churn avoided)
- Test: extend `FE/src/utils/instructor/__tests__/wordDiff.test.js` (empty identical-input case if missing) + new Playwright assertions in `FE/e2e/review-feedback-selection.spec.js` (created here; follow `feedback-anchor-fab.spec.js` harness): first-review diff highlights only post-handoff text e.g. `extensive`; revision diff highlights only post-Return text e.g. `and monitoring`; legacy shows honest notice, no crash

**Interfaces**
- Consumes: Task 2 endpoint. Produces: unchanged `changeRanges` model + `baselineUnavailable` flag

**Invariant**
- `wordDiff`/highlight renderers (`wordDiff.js`, `LatexEditor`, `PreviewPane`, `latexHtml`, `markdownBlocks`) are not modified. First submission compares against initial handoff; revision compares against latest Return.

- [ ] Step 1: Write the failing assertions (unit empty-case + spec cases above).
- [ ] Step 2: Run `node --test FE/src/utils/instructor/__tests__/wordDiff.test.js` (workdir `FE/`) and the new spec; verify red is the new behavior (old effect still single-request).
- [ ] Step 3: Rewrite the effect (comparison-source fetch, `baselineSectionId` guard kept, `baseline:null` → `diffResult=null` + notice flag); add locale keys.
- [ ] Step 4: Focused unit green; spec diff cases green.
- [ ] Step 5: `npm run build` + full `npm test` + existing anchor/diff specs green.
- [ ] Step 6: Commit `feat: Show Diff from comparison source with honest fallback`.

---

### Task 4: Remove active conversation workflow (vertical slices, reads kept)

**Files**
- Create: none
- Modify:
  - FE `FeedbackThreadsTab.jsx`: delete reply composer `:317-343`, `replyDrafts/replyAttachments` `:24-25`, `sendReply:64-78`, state buttons `:228-252`, `runState:50-62`, reject-note box `:288-315`, `rejectNotes/rejectingId` (+ `busyId` only if unused by edit/delete afterwards), drop `prepareState/postReply` from `:22`; KEEP replies list `:213-226`, state chip `:171-178`, edit/delete, root composer `:92-145`, root picker `:118-124`.
  - FE `FeedbackPanel.jsx`: delete ack block `:201-236` + `studentNotes/studentBusyId/sendStudentState`; KEEP replies view `:189-200`.
  - FE `WorkspaceLayout.jsx`: delete `handleStudentState:421-441`, pass `onStudentState={undefined}` (panel already guards).
  - FE `useInstructorReview.js`: delete `prepareState:457-473`, `postReply:475-492` (+ dead `student` param), update exports `:715-720`.
  - BE `FeedbackController.java`: delete `postReply:216-223`, `prepareFeedbackState:167-171` mappings outright (no tombstones — deployment evidence: SPA-only, unversioned `/api`, no stability promise).
  - BE `FeedbackServiceImpl.java`: delete `postReply:256-303`, `notifyReply:831` (sole caller `:301`), `prepareFeedbackState:306-328`, `applyStudentState:334-354`, `requireRejectionNote:388-409`, `isThreadClosed:817` + approve gate `:476-478`, `requireFreshPendingStates:623` + calls `:433,472`, `applyPendingStates:632` + calls `:445,475`, `canMarkDone/canReopen:756-758` + DTO fields + `effectiveState:851` (verify `:753` is its sole caller first — if anything else reads it, keep it); delete `FeedbackStateRequest`, `FeedbackReplyRequest`, `PostReplyResult`, `FeedbackReplyRepository` (incl. already-dead finder `:14`). KEEP: `FeedbackReply` entity, tables/FKs, state/status columns+checks, `FeedbackReplyResponseDto` + `replies[]`, `pendingState` field/DTO/chip, `getThread`, draft-publish loop `:437-444`, snapshot validation `:473`, `hasInstructorDrafts` gate.
  - Tests: `ProjectRouteMappingTest:160,161,163` (delete the three route strings); `FeedbackControllerTest:134-172` (reply/state/reanchor-bind tests → 404 assertions; extend existing retired-routes pattern `:117-131`); `FeedbackServiceImplTest` (delete reply/state blocks incl. closure-gate test asserting `Every feedback item must be resolved` — replace with approve-succeeds-OPEN test); `FeedbackRevisionMySqlTest:190,199` (state-related → delete; KEEP `:144` legacy-preservation).
  - Locales `en.json`+`vi.json`: delete reply-compose/resolve/reject-thread/reopen/reject-note/`markFixed`/`wontFix`/ack/`sendReply`/`replyFailed`/`replyPlaceholder` keys only after `grep -rn` proves zero references; KEEP state display chips, `studentState` legacy line, preview keys.
- Delete: as listed (each with caller-grep evidence in the commit message: no-caller + no-history-read + no-API + no-migration-dependency)
- Test: updated controller/service/route tests + new approve-succeeds-OPEN regression + Playwright absence assertions (no reply input, no resolve/reject/reopen, no ack buttons; `rejectSubmission` present; legacy replies text present)

**Interfaces**
- Consumes: same thread reads. Produces: no reply/state write traffic; Approve depends only on latest/status/drafts/snapshot/read-only checks

**Invariant**
- Request-level Reject path (`…/status?status=REJECTED`, `rejectReview`, `rejectSubmission` button) is not listed, not touched, and is re-verified in Task 9. Legacy replies/attachments/states stay readable.

- [ ] Step 1: Write/extend tests: approve-with-OPEN-feedback succeeds; removed routes 404; UI absence/presence assertions.
- [ ] Step 2: Run `mvn -q -Dtest='FeedbackControllerTest,FeedbackServiceImplTest,ProjectRouteMappingTest' test` + new spec; verify red is the specified old behavior (replies accepted, approval blocked, controls render).
- [ ] Step 3: Delete exactly the listed blocks/methods/mappings/keys (slice 4A instructor+postReply, then 4B state/ack/gate — two commits allowed, both green).
- [ ] Step 4: Focused suites + spec green.
- [ ] Step 5: `mvn -q -Dtest='FeedbackServiceImplTest,FeedbackRevisionMySqlTest,FeedbackPublicationMySqlTest,ProjectStatusConcurrencyIntegrationTest' test` + `npm run build` + `npm test` + student workspace specs.
- [ ] Step 6: Commit `feat: remove conversation write paths; approve needs no thread closure` (and `... (student ack slice)` if split).

---

### Task 5: Make selected passage part of normal Feedback Edit (delete standalone re-anchor)

**Files**
- Create: none
- Modify: `FE/src/hooks/useInstructorReview.js:391-395` (seed draft from stored original — exact fields below; keep `clearFeedbackDraft`-only-on-success `:359` and `captureSourceSelection:367-389` unchanged); `FE/src/components/Instructor/review/FeedbackThreadsTab.jsx` (excerpt line + Use-current-selection note already exists `:95-107` — add Remove-passage button + delete re-anchor button `:269-285`); `FE/src/components/Student/EditorPanel.jsx` (delete re-anchor banner `:233-253`; simplify `openComposerFromFab:153-159` to `captureSourceSelection` only); `BE/.../FeedbackController.java` (delete `reanchor:198-203` mapping — no callers left); `BE/.../FeedbackServiceImpl.java` (delete `reanchor:240-253`); tests (`FeedbackControllerTest:151-162` → 404; `FeedbackServiceImplTest:653,674,693` → delete)
- Delete: `FE/src/stores/reanchorStore.js` (after grep proves zero imports remain); service `reanchor`; mapping. KEEP `FeedbackAnchorService` (used by comment/update), `FeedbackAnchorRequest` DTO (used by `InstructorFeedbackRequest.anchor`), `anchorJson` persistence.
- Test: Playwright edit cases + BE update-path tests (existing `FeedbackServiceImplTest` update block extended: text-only preserves passage; reselect persists same ID; stale/invalid anchor → 400/409 with persisted row unchanged; remove-passage → section-level)

**Interfaces**
- Consumes: existing `PATCH /instructor-feedback/{id}` (unchanged contract — no `expectedRevision`, no new endpoint)
- Produces: same thread DTO; single-transaction atomic save (already true `:210-211`)

**Invariant**
- Seed Edit draft passage from **`item.anchor.original.{from,to,contentVersion,fingerprint}`** + literals `representation:'latex-source-lf-v1'`, `offsetUnit:'utf16'` (definitive: `original` is the immutable review-time `Target` (`FeedbackAnchor.java:5-6`, stored by `create:157-160`); `current` is live-resolved and nullable/`DETACHED`, and its version fails `create:138-143` after any section edit; highlight uses `.current` for display only). If `item.anchor.original == null` (whole-section/line-ref items, `create:129,133,148-149`) → seed `anchor:null` and keep `lineReference` (unchanged behavior). Text-only edit re-sends the seeded values, so `anchorChanged=true` re-initializes to the identical passage; if the section changed underneath, `create:138-143` fails honestly (400), the draft is kept (`:359` runs only on success), and persisted text/passage are untouched. Remove-passage sets `anchor:null` (keeps `lineReference`) → valid section-level per `create:128-130`.

- [ ] Step 1: Write failing specs/tests (text-only preserves; reselect same-ID; invalid keeps persisted row + draft; remove → section-level).
- [ ] Step 2: Run new Playwright spec + `mvn -q -Dtest=FeedbackServiceImplTest test`; verify red (anchor cleared on edit `:394`, re-anchor UI present).
- [ ] Step 3: Change seeding, add excerpt + Remove button, delete re-anchor UI/store/mapping/service method.
- [ ] Step 4: Focused tests green.
- [ ] Step 5: `npm run build`, `npm test`, anchor/FAB specs, `mvn -q -Dtest='FeedbackServiceImplTest,FeedbackControllerTest' test`.
- [ ] Step 6: Commit `feat: passage reselection inside feedback edit; standalone re-anchor removed`.

---

### Task 6: Minimal overlap awareness (reuse existing multi-ID navigation)

**Files**
- Create: `FE/src/utils/instructor/feedbackOverlap.js` (`findOverlaps(candidate{from,to}, items) → {count, exactDuplicate, ids[]}`; half-open `a.from < b.to && b.from < a.to`; scope filter helper `isOverlappable(item, activeRequestId, sectionId)` = same request + same section + root item + passage-anchored (`original?.from != null`) — whole-section items never count, previous-round items never count, drafts and published both count); `FE/src/utils/instructor/__tests__/feedbackOverlap.test.js` (none/partial/nested `fish`/exact-duplicate/whole-section-excluded/previous-round-excluded/multi-id)
- Modify: `FE/src/components/Instructor/review/FeedbackThreadsTab.jsx` (candidate = `selectedAnchor`; notice `This selection overlaps N existing feedback item(s).` / exact `This exact passage already has feedback.` EN+VI; `[View existing]` → existing `selectFeedback(firstId)` — full section list already rendered by `:44-48`, no new filter state; Save never disabled; same check pre-save in edit mode, notice-only); locales (2 keys × 2 languages)
- Delete: none. No backend, no endpoint, no new navigation state (`overlapFilter` explicitly rejected — click already yields N ids `LatexEditor.jsx:549-550`, first-selected + full list visible suffices)
- Test: util tests + Playwright (`fish` nested + exact-duplicate + edit-into-overlap `a fish` all persist; notice shown; save enabled)

**Interfaces**
- Consumes: candidate range + already-loaded `feedbackItems`. Produces: notice + select-first navigation (existing path)

**Invariant**
- `A="The man has hooked a fish"` + `B="fish"`, partial, nested, exact-duplicate, and edit-into-overlap all persist independently. Backend performs zero overlap checks (none exist in `create:124-161` — keep it that way).

- [ ] Step 1: Write util tests + spec (all red: missing module + no notice).
- [ ] Step 2: Run `node --test FE/src/utils/instructor/__tests__/feedbackOverlap.test.js` (red: missing module) + spec (red: no notice).
- [ ] Step 3: Implement pure util + composer/edit notice + View-selects-first.
- [ ] Step 4: Unit + spec green.
- [ ] Step 5: `npm test`, `npm run build`, anchor/edit specs green.
- [ ] Step 6: Commit `feat: non-blocking overlap awareness (client-side only)`.

---

### Task 7: Keep exact canonical selection usable while Preview is visible

**Files**
- Create: none
- Modify: `FE/src/components/Student/EditorPanel.jsx` (keep editor mounted-hidden `:255` + key `:431` excluding `showPreview` — verify, do not change; keep right panel/composer mounted in preview `:552-556` gated only on `canCreateRoot`; retarget preview-select banner `:504-533`: never clear/disarm `draft.anchor`; when a preview selection occurs with an armed passage, banner states the armed passage stays usable + offers Back-to-editor; with nothing armed, guidance copy `Select the passage in the editor to attach precise feedback.` EN+VI — keep existing locale keys, change text); `FE/src/hooks/useInstructorReview.js` (no new state; assert no `clear/updateFeedbackDraft({anchor:null})` in `switchReviewMode`/preview effects — add regression coverage instead of code)
- Delete: none in Preview (`describePreviewSelection` geometry-only stays; no `indexOf`, no text search, no block ranges). No LF-normalization (was only needed for block anchoring — dropped). Diff/highlight code untouched.
- Test: Playwright (arm in editor → open Preview → draft still armed → Save persists exact offsets; preview-select with armed passage does not disarm; with nothing armed shows guidance; edit-while-Preview-open can consume armed passage; `A fish…another fish` never string-matched)

**Interfaces**
- Consumes: existing `draft.anchor` + `canCreateRoot`. Produces: identical PATCH bodies as editor flow (single persistence model)

**Invariant**
- Preview never synthesizes offsets: a selected word is never stored as its paragraph. Exactness comes only from `selection.main.from/to` (`LatexEditor.jsx:341-344`) via `captureSourceSelection`. Direct Preview→offset mapping is OUT OF SCOPE until a renderer source map exists (acceptance would be second-`fish` exactness, not block identity).

- [ ] Step 1: Write failing specs (armed-survives-preview is the key red: current banner path routes back via `lockPreviewSelection` — assert Save-from-preview works without returning).
- [ ] Step 2: Run new spec; verify red names the banner/disarm behavior.
- [ ] Step 3: Minimal banner/copy/gating adjustments only (no new selection machinery).
- [ ] Step 4: Spec green.
- [ ] Step 5: `npm run build`, `npm test`, all editor/anchor/overlap specs green.
- [ ] Step 6: Commit `feat: exact editor selection stays armed while Preview is visible`.

---

### Task 8: Render previous feedback card in History

**Files**
- Create: `FE/src/components/Instructor/review/FeedbackCard.jsx` (pure presentational extraction of `FeedbackThreadsTab.jsx:160-226` body: props `{item, active, onSelect, readOnly}` — badges, content, location, `originalContext`, `studentStatus` legacy line, attachment thumbs, replies list; no action row, no reply box, no re-anchor); `FE/src/utils/instructor/historySelector.js` (`selectPreviousCard(feedbackItems, orderedRequests, activeId, sectionId)` — previous = array position after active in canonical desc order; latest-created published root for section; `updatedAt` ignored); `FE/src/utils/instructor/__tests__/historySelector.test.js`
- Modify: `FE/src/components/Instructor/review/FeedbackThreadsTab.jsx` (render `FeedbackCard` with actions outside); `FE/src/components/Instructor/review/../InstructorFeedbackPanel.jsx:88-102` (replace metadata `<ul>` with previous-card-or-empty render)
- Delete: metadata-only list markup (replaced)
- Test: selector unit (multi-item → latest-created published; drafts excluded; wrong-section excluded; `updatedAt` edits ignored; single/no-request → null/empty) + spec (card parity with Feedback tab, read-only: no inputs/buttons)

**Interfaces**
- Consumes: already-loaded `feedbackItems` + `orderedRequests` (canonical desc order — previous = array position after active, no new timestamp sort). Produces: read-only card render, no fetch, no endpoint

**Invariant**
- `previous = orderedRequests[indexOf(activeId)+1]` (same project guaranteed by `:129` filter); card = max-`createdAt` published root for the section; `updatedAt` never orders. No card-markup duplication (single `FeedbackCard`).

- [ ] Step 1: Write selector unit + render spec (red: missing modules/markup).
- [ ] Step 2: Run `node --test …/historySelector.test.js` + spec; verify specified reds.
- [ ] Step 3: Extract card, replace tab, wire selector.
- [ ] Step 4: Unit + spec green (visual parity via shared component).
- [ ] Step 5: `npm run build`, `npm test`, panel specs green.
- [ ] Step 6: Commit `feat: History shows previous-round card via shared component`.

---

### Task 9: Full regression chain

**Files**
- Modify: `BE/src/test/java/com/evidencepilot/service/ComparisonSourceTest.java` (chain data assertions) + `FE/e2e/review-feedback-selection.spec.js` (single spec file created in Task 3 — reuse it, do not create another)
- Delete: none
- Test: the chain below (BE asserts data/state, spec asserts UI)

**Interfaces**
- Consumes: all prior tasks. Produces: green chain or a reopened owning task (never chain-specific shortcuts)

**Invariant**
- Mirrors acceptance verbatim: initial handoff → baseline row; edit → submit A; diff = initial vs A (`extensive` only); Feedback A on `The man has hooked a fish` + Feedback B on `fish` → overlap warning, both persist; edit B → `a fish`, A unchanged; Return A → new baseline; student reads both, reply/state/re-anchor attempts fail (404s), no ack UI; edit → submit B; diff = A-return `BASELINE` vs B (`and monitoring` only); History = Request-A latest card; Preview uses preserved exact selection; Approve succeeds with OPEN threads; request-level Reject separately verified working on a scratch project.

- [ ] Step 1: Encode any uncovered chain link as a failing assertion (expect green if Tasks 1–8 complete).
- [ ] Step 2: Run `mvn -q -Dtest='ComparisonSourceTest,AssignmentBaselineCaptureTest' test` + `npx playwright test e2e/review-feedback-selection.spec.js`; record per-link results.
- [ ] Step 3: Fix exclusively via owning tasks (no test-only accommodations, no production shortcuts).
- [ ] Step 4: Chain fully green.
- [ ] Step 5: Full runnable suites: `mvn -q -Dtest='<H2-safe list incl. FeedbackServiceImplTest,FeedbackControllerTest>' test`, `npm test`, `npx playwright test`, `npm run build`; Docker-gated names listed as environment-skipped, never as PASS.
- [ ] Step 6: Commit `test: full review-cycle acceptance chain`.

---

## Planning Decisions Confirmed

| Decision | Refined Design | Evidence |
|---|---|---|
| Initial baseline | Insert-if-absent row per `(project, section)` at section-assignment events (`assignSection`, batch assign-change, admin-seed born-assigned); `UNIQUE(project_id, section_id)`; no assignee column | No immutable handoff store exists (checkpoints template-only/best-effort, audit counts-only, snapshots submit-time); creation paths never born-assigned except `AdminExcelSeedService:1288`; `setAssignedUser` call sites exhaustive (`:411,413,963` + seed) |
| Handoff boundary | Assignment-event hooks (status flip occurs only inside them: `:422-423,986-987`); no separate status hook | Flip strictly conditional on assignment present; member ops never touch status/sections |
| Reassignment | Does not reset baseline (UNIQUE no-ops; no delete on unassign) | Default product rule; no evidence of handoff-on-reassign |
| Return baseline | Existing `writeSnapshots(BASELINE)` content/timing kept exactly | `returnReview:451-453` already freezes all sections at Return |
| Comparison resolver | Latest earlier `RETURNED` BASELINE → initial → unavailable; no `argmax`; no `REVIEWED`/Reject baselines | Seeded-copy hazard (`V37_1`); neither Approve nor Reject writes snapshots (`:459-508`); no active request can postdate `REVIEWED` (read-only blocks resubmit) |
| Feedback edit | Seed `item.anchor.original.{from,to,contentVersion,fingerprint}` (+ literals); `original==null` → `anchor:null`; text-only re-sends seed; invalid → 400/409, draft + row intact; single-tx atomicity kept | `FeedbackAnchor` shape (`:3-8`), `create` validators (`:137-143`) fail resolved-current after edits, `updateFeedbackItem:201-205` paths, draft cleared only on success (`:359`); highlight uses `.current` for display only |
| Overlap | Allow + warn; pure `findOverlaps`, half-open rule, scope = same request + same section + roots + passage-anchored (drafts + published; whole-section and previous-round excluded); View = existing `selectFeedback(firstId)`; no new state/endpoint | No backend range checks (`create:124-161`); decorations stack + click yields N ids (`LatexEditor:29-54,545-554`); full section list already rendered (`:44-48`) |
| Preview | Preserve armed exact editor selection across Preview; Preview synthesizes nothing; honest guidance when unarmed; block pseudo-anchors rejected | Editor stays mounted-hidden (`EditorPanel:255,431`), draft store preview-independent (`:57-64`, clearing only on save/cancel/edit-start), composer preview-independent (`:552-556`, `canCreateRoot:153`); exact mapping infeasible (no char map, KaTeX reorder, duplicate hazard) |
| Reply removal | Delete mappings/services/DTOs/repo with last callers (no 410s); keep entity, tables/FKs, state/status columns+checks, `replies[]`/status reads, `getThread`, request-Reject | CASCADE/DDL preservation need; zero-caller proofs listed per symbol; deployment has no external clients (SPA-only README, unversioned `/api`, no stability promise) |
| Approve | No thread-closure gate, no pending staging; real invariants kept (latest/status/drafts/snapshot/read-only) | Gate unsatisfiable post-removal (`:476-478`); one-way needs no closure |
| History | Extracted pure `FeedbackCard`; previous = array position after active in canonical `orderedRequests`; latest-created published root; `updatedAt` ignored; read-only; no endpoint | Card body self-contained (`:160-226`); full DTOs already loaded; same-project filter `:129` |

## Scope Removed From Previous Plan

- Reassignment baseline lineage (reassignment/unassign/member ops produce no rows; no `assigned_user_id` column).
- Event-time `argmax()` resolver (replaced by ordered RETURNED-scan → initial → unavailable).
- Tests-only red commits and implementation-only follow-ups (every task is red → green → commit).
- Standalone no-op decision tasks (title/order freeze lives here as a Global Constraint, not a task).
- Preview block-range anchoring, Preview LF-normalization-for-anchoring, duplicate-text block tests, preview-candidate composer wiring (replaced by preserve-exact-selection Task 7; direct Preview mapping is future work gated on a renderer source map).
- New `expectedRevision` on feedback update (absent from today's contract; validators + draft retention suffice).
- New `overlapFilter` navigation state (existing multi-ID click + full section list suffice).
- Giant deferred dead-code cleanup (deletion rides with last-caller removal in Tasks 4–5).
- Permanent 410 compatibility handlers (mappings deleted; no external clients require deprecation).
- `REVIEWED`-as-baseline and Reject-creates-baseline (neither writes one; strict RETURNED-only).

## Remaining Blockers

None. Every design above is determined by this prompt, the repository, the audit, tests, or git history. (Worker micro-verifications embedded as task steps — `effectiveState:753` sole-caller check, locale-key grep-before-delete, `LatexEditor` key/container re-confirmation — are confirmations, not blockers.)

## Implementation Readiness

```text
READY TO IMPLEMENT
```

Nine dependency-ordered tasks (0–9), each independently reviewable with exact files/symbols at HEAD `78003a7`, failing-test-first with exact commands, green commits, and per-task revert safety. No migration rewrites, no history destruction, no manufactured baselines, no second passage model.
