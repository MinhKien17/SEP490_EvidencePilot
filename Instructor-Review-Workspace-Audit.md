# Evidence Pilot — Instructor Review Workspace Audit

> Branch: `main` · HEAD: `78003a7ebbaf5419aab19915c4436a6b9ff19472` · Working tree: `M BE/src/test/java/com/evidencepilot/controller/ProjectRouteMappingTest.java`
> Scope: Instructor Page → Review Request → Review Workspace · Type: READ-ONLY audit, no code changes.

## 1. Executive Summary

**Current state.** The Instructor Review Workspace is a working one-way-review skeleton with an active two-way conversation layer bolted on top. Route chain: `App.jsx` → `ReviewRequests.jsx` → shared `WorkspaceLayout.jsx (workspaceMode="review")` → `useInstructorReview.js` → `EditorPanel.jsx` + `InstructorFeedbackPanel.jsx` → `FeedbackThreadsTab.jsx`, backed by `FeedbackController.java` → `FeedbackServiceImpl.java` → `FeedbackRequest` / `InstructorFeedback` / `FeedbackReply` / `FeedbackAttachment` / `ReviewSectionSnapshot` entities. The one-way backbone (submit → comment as unpublished draft → Return publishes drafts + writes `BASELINE` → student resubmits as a new request with `SUBMITTED`) works and is covered by `FeedbackRevisionMySqlTest.java:86-141`.

**Highest-risk issue (P0/P1): Show Diff is structurally broken after any revision cycle (Finding B).** The frontend diffs `BASELINE` vs `SUBMITTED` **within the single active request** (`FE/src/hooks/useInstructorReview.js:260-268`), but the backend writes `BASELINE` onto the *returned* request A and `SUBMITTED` onto the *new* request B (`BE/.../service/impl/FeedbackServiceImpl.java:141-142,451-453`). Result: Request A diffs a submission against itself (≈empty), Request B has no baseline (diff renders nothing). The real revision (A-content vs B-content) is never compared. No end-to-end test covers it.

**Largest simplification opportunity (P2, low risk soft-first): the reply/acknowledgement layer (Finding A).** `POST /instructor-feedback/{id}/replies`, the instructor reply composer, thread `RESOLVED`/`REJECTED`/`OPEN` buttons, and student `IMPLEMENTED`/`WONT_FIX` ack are all live on both sides — contrary to the intended one-way contract. They can be switched off at UI + API-validation level with **zero schema change**; physical table deletion is what carries risk (rejection-rationale replies, legacy rows, dual-FK attachments cascade).

**Can implementation begin? `YES AFTER PREREQUISITES`.** Two small design decisions first (diff source-of-truth semantics; History "last card" definition), and reply removal as *deactivate-first, drop-schema-later*. Nothing is `BLOCKED BY UNKNOWN DEPENDENCY`.

## 2. Audit Scope

- **Repo / branch / HEAD:** `SEP490_EvidencePilot`, `main`, `78003a7ebbaf5419aab19915c4436a6b9ff19472` ("Remove small audit report").
- **Working tree:** one modified file, `BE/src/test/java/com/evidencepilot/controller/ProjectRouteMappingTest.java` (M). `git diff --check` clean.
- **Relevant recent commits:** `4db50ed` Re-structure Instructor Review Request (Partial) · `0bf411c` Optimize Review Workspace · `be12f4b` restore feedback filters · `3a906e7` Re-design Review Feedback · `eb3b305` narrow review workflow · `40fb352` canonical handoff · `08c8599` enforce review rounds · `a362f20` FeedbackService txn+reply tests · `8405876` retire feedback conversations (partial — replies later reintroduced cycle-scoped).
- **Workflow inspected:** Instructor Page (`/instructor/requests`) → Review Request list → Review Workspace (`/instructor/requests/:projectId?review=:id`) → center editor/Preview/Show Diff + Return/Approve/Reject + right panel (Overview/Feedback/Findings/History) → feedback composer (`Use editor selection`, Save feedback).
- **Files inspected (first-hand):** FE `App.jsx`, `pages/Instructor/ReviewRequests.jsx`, `pages/Student/WorkspaceLayout.jsx`, `hooks/useInstructorReview.js` (full, 735 lines), `hooks/useProjectFeedback.js`, `components/Student/EditorPanel.jsx`, `components/features/PreviewPane.jsx`, `components/features/LatexEditor.jsx`, `components/Instructor/InstructorFeedbackPanel.jsx`, `components/Instructor/review/FeedbackThreadsTab.jsx`, `components/Student/FeedbackPanel.jsx`, `components/features/MediaAssetPicker.jsx`, `utils/instructor/wordDiff.js`, `utils/feedbackAnchors.js`, `stores/reanchorStore.js`; BE `controller/FeedbackController.java`, `controller/PaperController.java`, `controller/CheckpointController.java`, `service/impl/FeedbackServiceImpl.java:112-511`, `service/FeedbackAnchorService.java`, `service/FeedbackAttachmentService.java`, `service/SubmissionReadinessService.java`, entities `FeedbackRequest`, `InstructorFeedback`, `FeedbackReply`, `FeedbackAttachment`, `ReviewSectionSnapshot`, `PaperSection`, `Project`, enums `FeedbackStatus`, `FeedbackThreadState`, `StudentStatus`, `SnapshotType`, `ReplyAuthorRole`, all feedback DTOs, all review repositories; DB migrations V1–V37 + `V37_1__SeedReviewSnapshots.java` (`schema.sql` confirmed absent); tests `FeedbackRevisionMySqlTest`, `FeedbackServiceImplTest`, `FeedbackPublicationMySqlTest`, `FeedbackAttachmentServiceTest`, `CheckpointServiceImplTest`, `SubmissionReadinessServiceTest`, `FeedbackControllerTest`, `ProjectRouteMappingTest`, `FlywayMigrationMySqlTest`, FE `wordDiff.test.js`, `sourcePreview.test.mjs`, `feedback-anchor-fab.spec.js`.
- **Commands run (read-only):** `git status`, `git diff --check`, `git log`, `java -version` (21.0.11 LTS), `mvn -version` (3.9.16), `node` (v24.18.0) / `npm` (11.16.0). **Not run:** `mvn compile/test`, `npm run build`, test suites (Docker-gated MySQL tests cannot pass here; running them yields environment noise, not signal). Stated limitation, not a pass claim.
- **Limitations:** (a) No live DB sampling — historical-row claims rest on DDL + cascade annotations + seed code. (b) Screenshot used only as surface map; all behavior claims code-traced. (c) Student code inspected only where it shares the thread DTO/service.

## 3. Current Instructor Review Architecture

```text
Route (/instructor/requests/:projectId?review=:id)
→ ReviewRequests.jsx (list; Link to ?review=<requestId>, :220-221,246-247)
→ WorkspaceLayout.jsx workspaceMode="review" (:100-104,153-164; forces readOnly/locked :721)
→ useInstructorReview.js (single source of truth; {workspace, workflow} :652-735)
→ EditorPanel.jsx (center: LatexEditor + PreviewPane + Show Diff :263-274,457-467; right: InstructorFeedbackPanel :552-556)
→ InstructorFeedbackPanel.jsx (Return/Approve/Reject :50-56; tabs overview/feedback/findings/history :60-72)
→ FeedbackThreadsTab.jsx (canonical <li class="rounded-xl border…"> card :153-163) / ReviewOverviewBlock / SectionEvidenceTab / inline History <ul> (:88-102)
→ api (axios) → FeedbackController (/api/…) → FeedbackServiceImpl → repositories → tables
→ full thread DTOs (replies+attachments) → mergeThread (:326-331) → rendered card
```

**Review state lifecycle (actual names, nothing invented):**

```text
Project: CREATED → ASSIGNED → IN_PROGRESS → SUBMITTED_FOR_REVIEW ⇄ RETURNED → APPROVED → ARCHIVED
FeedbackRequest: PENDING → RETURNED | REVIEWED | REJECTED   (new row per submit/resubmit = "round")
FeedbackThread: OPEN ⇄ RESOLVED | REJECTED (two-phase: pendingState staged via PATCH …/state, committed by Return/Approve)
StudentAck (thread columns, never threadState): NULL → IMPLEMENTED | WONT_FIX (+note)
```

| Transition | Frontend trigger | Controller/API | Backend service | DB mutation | Snapshot mutation | Permission |
|---|---|---|---|---|---|---|
| Submit / resubmit (student) | Student workspace submit | `POST /projects/{projectId}/reviews` (`FeedbackController.java:69-75`) | `submitForReview:112-148` | `INSERT feedback_requests(PENDING, submissionSnapshotJson)`; `UPDATE projects→SUBMITTED_FOR_REVIEW` | `INSERT review_section_snapshots(SUBMITTED)` `:141-142` | `requireProjectWriteAccess` (student path) |
| Return for Revision | Return button → modal (`InstructorFeedbackPanel.jsx:52,107-124`) → `handleTransitionStatus` (`useInstructorReview.js:537-553`) | `PATCH /feedback-requests/{id}/status?status=RETURNED` (`:235-240`) | `returnReview:423-457` (latest + PENDING + not-readOnly) | Publish this request's drafts; `applyPendingStates`; `→RETURNED` / `projects→RETURNED` | `INSERT review_section_snapshots(BASELINE)` on **same** request `:451-453` | `requireFeedbackAccessForUpdate(...,instructorOnly=true)` |
| Approve | Approve button → same modal | `…/status?status=REVIEWED` | `approveReview:459-485` (**all threads RESOLVED\|REJECTED** `:476-478`; snapshot revalidated `:473`) | `→REVIEWED`, `projects→APPROVED` | none (checkpoint row) | instructor-only; drafts block |
| Reject (request-level) | Reject button → same modal | `…/status?status=REJECTED` | `rejectReview:487-508` (**no** closed-thread requirement) | `→REJECTED`, `projects→IN_PROGRESS` + notify | none | instructor-only; drafts block |
| Feedback-level resolve/reject/reopen | `resolveThread/rejectThread/reopenThread` (`FeedbackThreadsTab.jsx:228-252`; reject needs note `:288-315`) | `PATCH /instructor-feedback/{id}/state` (`:167-171`) | `prepareFeedbackState:306-328` stages `pendingState`; committed by Return/Approve `:632-644` | `pendingState` now; `threadState/By/At` at commit; REJECTED-note inserts a reply (`:388-409`) | none | instructor-only + `expectedRevision` optimistic check |
| Student ack | `markFixed/wontFix` (`FeedbackPanel.jsx:217-234`) → `handleStudentState` (`WorkspaceLayout.jsx:421-441`) | same `PATCH …/state` + `studentStatus/studentNote` | `applyStudentState:334-354` (WONT_FIX requires note) | `studentStatus/studentNote` immediately | none | student-member branch (`:309-310`) |
| Reply post | Reply composer (`FeedbackThreadsTab.jsx:317-343`) → `postReply` (`useInstructorReview.js:475-492`) | `POST /instructor-feedback/{id}/replies` (`:216-223`, 201 vs 200 idempotent) | `postReply:256-303` (published + `RETURNED` cycle + writable; idempotency replay `:277-286`) | `INSERT feedback_replies` + optional `feedback_attachments(reply_id)` | none | student-member OR (admin \| request-instructor); **no assignee gate** |

Structural facts: (1) **rounds are rows, not a Round entity** — `ReviewRequest`/`ReviewRound` classes/controllers **do not exist**; successive `feedback_requests` rows are rounds. (2) **No MapStruct mappers** (`**/*Mapper*.java` empty) — static `from/fromEntity` DTO methods. (3) Auth on review endpoints is **programmatic in `FeedbackServiceImpl`**, not `@PreAuthorize` (`SecurityConfig.java:51-82` leaves review paths to service checks).

## 4. Finding A — Reply / Acknowledgement Removal

### Current architecture

```text
Instructor reply composer (FeedbackThreadsTab.jsx:317-343, gated publishedAt && status==='RETURNED')
→ postReply (useInstructorReview.js:475-492) → POST /instructor-feedback/{id}/replies {requestId, content, idempotencyKey, mediaAssetIds?}
→ FeedbackController.postReply (:216-223) → FeedbackServiceImpl.postReply (:256-303)
→ feedback_replies row + feedback_attachments(reply_id) + notifyReply
→ getThread → InstructorFeedbackResponseDto (replies[]+attachments[]) → mergeThread → sub-<ul> (:213-226) + Student mirror (FeedbackPanel.jsx:189-200)
Thread resolve/reject/reopen: prepareState (:457-473) → PATCH …/state → prepareFeedbackState (:306-328) → [REJECTED → requireRejectionNote auto-reply (:388-409)]
Student ack: FeedbackPanel.jsx:201-236 → WorkspaceLayout.jsx:421-441 → PATCH …/state → applyStudentState (:334-354)
```

Verified live: composer renders, `postReply` called, `FeedbackControllerTest.java:138-145` + `FeedbackServiceImplTest.java:453-632` exercise reply/state. Prior audit's "contrary to one-way contract" **confirmed on HEAD** — extended: `IMPLEMENTED`/`WONT_FIX` (`StudentStatus.java:3-6`, `V37:28-32`) + `RESOLVED`/`REJECTED` (`FeedbackThreadState.java:3-7`, `V34:13-16`) all reachable from UI.

### Reply removal inventory

| Symbol / File / Table | Layer | Current caller(s) | Historical dependency | Proposed action | Reason |
|---|---|---|---|---|---|
| `FeedbackThreadsTab.jsx:317-343` reply composer + `replyDrafts/replyAttachments` + `sendReply:64-78` | FE Instructor UI | Instructor on RETURNED threads | None (new rows only) | **DELETE NOW** | Active chat behavior |
| `FeedbackThreadsTab.jsx:213-226` replies sub-`<ul>` | FE Instructor UI | `item.replies` | **Yes — legacy display** | **RETAIN READ-ONLY** | Hides old discussion if removed |
| `FeedbackThreadsTab.jsx:228-252` resolve/reject/reopen + `runState` + reject-note `:288-315` | FE Instructor UI | `prepareState` | Rationale notes are reply rows | **DELETE NOW** (buttons); keep state chip `:171-178` read-only | Obsolete workflow; chip is history |
| `FeedbackPanel.jsx:189-200` student replies view + `:201-236` ack composer | FE Student UI | `onStudentState` (`WorkspaceLayout.jsx:1672`) | Replies view = legacy; ack = active mutation | View **RETAIN READ-ONLY**; ack **DELETE NOW** | Student never mutates feedback |
| `useInstructorReview.js:475-492 postReply` + `:457-473 prepareState` | FE hooks | Above components | None (`student` param has no UI caller) | **DELETE NOW** (`mergeThread` stays) | Dead after UI removal |
| `WorkspaceLayout.jsx:421-441 handleStudentState` | FE Student hook | `FeedbackPanel onStudentState` | None (current-row writes) | **DELETE NOW** | Sole student-mutation path |
| `useProjectFeedback.js:5-18` per-round fan-out | FE Student fetch | Student panel | Replies embedded; no replies GET exists | **KEEP** | One-way read needs it |
| `MediaAssetPicker.jsx` reply instance (`:319-325`) | FE attachments | `postReply …mediaAssetIds` | Reply-attachment rows exist | **DELETE NOW** (reply picker); keep root picker | Root attachments are legitimate |
| `POST /instructor-feedback/{id}/replies` (`FeedbackController.java:216-223`) | BE controller | FE `postReply` only (sole BE caller) | Legacy rows remain | **Disable first (410/403), delete later** | Behavior off, zero schema risk |
| `FeedbackServiceImpl.postReply:256-303` + `notifyReply` | BE service | Controller only | Same | **DELETE AFTER MIGRATION** | Keep until mapping removal ships |
| `FeedbackReply` entity + `feedback_replies` table (`V27:19-39`, `V34:19-37`) | Entity / DB | `postReply`, `requireRejectionNote:402`, lazy `feedback.getReplies()` (`:767-768`) | **Yes — legacy rows; FK target of `reply_id`** | **RETAIN READ-ONLY** | Drop cascades history (Q-B) |
| `FeedbackReplyRepository.java` | BE repo | Writes + idempotency; `findByFeedbackIdOrderByCreatedAtAsc` has **no BE caller** (dead) | None beyond above | Dead finder now; interface **DELETE AFTER MIGRATION** | P3 cleanup deferred |
| `FeedbackReplyRequest` / `PostReplyResult` | BE DTO | Reply path | None | **DELETE AFTER MIGRATION** | Dead with endpoint |
| `FeedbackReplyResponseDto` + `replies[]` JSON | BE DTO | Thread reads | History reads | **RETAIN** while replies render | Breaking change otherwise |
| `FeedbackAttachment.reply_id` (`V34:51-53` CASCADE; `V35`; `V36`) | Entity / DB | `linkMediaAssets(...,reply)` (`:90-92`) | **Yes — real reply-file rows** | **RETAIN — STILL REQUIRED** | Drop orphans/destroys files |
| `FeedbackThreadState` + `pendingState` machine + approve all-closed gate (`:476-478`) | Enum/workflow | Buttons; approve gate | Closed-state history | Display **RETAIN**; machinery **DELETE AFTER MIGRATION**; gate **NEEDS DECISION** (§8.9) | Gate meaningless without resolve |
| `StudentStatus` + `applyStudentState:334-354` + `studentStatus/studentNote` DTO fields | Enum/service/DTO | Student buttons | Submitted ack history in columns | Acceptance **DELETE NOW** (400/410); columns **RETAIN READ-ONLY** | Stop writes, keep history |
| `requireRejectionNote:388-409` | BE validator | REJECTED branch `:321` | Writes reply rows | **DELETE AFTER MIGRATION** | Coupled to removed state |
| Reply CHECKs/UNIQUE/INDEXes (`V27:35-37`, `V34:35-37`) | DB | Reply-write integrity | Protect legacy rows | **RETAIN** while table lives | Load-bearing |
| Locale reply/ack strings (+ orphan `doneFeedback/markDone`, never imported — Done button does not exist) | FE locale | Composer/buttons | None | Prune with components | Dead strings |

### Question A — classification

No persistence item qualifies for unconditional `DELETE NOW`. Feature code (composers, buttons, hooks, endpoint mapping, ack acceptance) = `DELETE NOW`. History-storing items (entity/repo/service/DTO-response, table, `reply_id`, state/status columns+checks) = `RETAIN READ-ONLY FOR LEGACY HISTORY`, hard deletion = `DELETE AFTER MIGRATION`. `UNKNOWN — NEEDS DECISION`: approve all-closed gate; legacy replies inline vs History-only.

### Question B — would deleting reply persistence destroy history? **Yes (P0).**

(1) `feedback_replies.feedback_id → instructor_feedbacks CASCADE` (`V27:29-30`) — dropping the table deletes the rationale side of threads. (2) Rejection rationale **is** a reply row (`requireRejectionNote:391-402`). (3) `feedback_attachments.reply_id → feedback_replies CASCADE` (`V34:52-53`) — reply files go with them. (4) Both renderers read `item.replies`; `FeedbackRevisionMySqlTest.java:144` pins "preserves legacy replies" as intended behavior.

### Question C — does History depend on reply data? **No — and that is the History defect.**

`InstructorFeedbackPanel.jsx:88-102` renders `orderedRequests` metadata only, zero `replies/*Status` references. History survives reply removal untouched but shows nothing useful (§7).

### Question D — root vs reply attachments: **distinguished, with a rendering gap.**

`linkMediaAssets:54-101`: root = (`feedback_id` set, `reply_id` NULL; from `comment:178-179`, `update:208-209`); reply = (both set; from `postReply:300`, `:90-92`). Quota `MAX_PER_MESSAGE=5:30` per target. Ownership sound (same-project `:70-74`, mime allowlist, server-side clone, `media_asset_id` provenance-only `SET NULL`). Gap: **reply attachments accepted but never rendered** (no `reply.attachments` branch in either panel). After removal: keep resolving via `storageKey` (`readUrl:104-106`); decide display (read-only legacy renderer or fold URLs into parent view) before any hard drop.

### Question E — later migrations depending on reply objects: **yes, a real chain.**

`V27` creates → `V34` alters + creates `feedback_attachments` (FKs to both) → `V35` alters → `V36` alters. `V37:2-3` only ADDs. Validated by `FlywayMigrationMySqlTest.java:54-225,312-393`. Removal needs a **new** migration; never edit V27–V36.

### Question F — soft-removal vs hard deletion: **soft first, unanimously.**

Disable UI/API + retain tables/DTO-response/legacy render = 100% of product goal, ~zero data risk, independently testable. Physical deletion buys little, risks P0 loss + Flyway-chain break + file migration. Hard deletion = Phase 6 (§11).

### "Reject" distinction — do NOT delete request-level Reject

| Concept | Implementation | Action |
|---|---|---|
| Thread-level `REJECTED` (+`RESOLVED`/`OPEN`, `canMarkDone/canReopen`) via `PATCH …/state`; requires note→reply | Obsolete workflow | **Remove** controls; keep display chip |
| Request-level `REJECTED` via `PATCH …/status` → `rejectReview:487-508` → `projects→IN_PROGRESS` + notify; button `rejectSubmission` | Whole-submission refusal — **must stay** | **KEEP** (different table/column; no thread-closure requirement) |
| Student `WONT_FIX` | Obsolete ack | Remove acceptance; retain column |
| Other `reject*` (resubmission-guard test names) | Submission validation | **KEEP** |

## 5. Finding B — Show Diff (P1, core workflow broken)

### Lifecycle (verified)

1. Submit → `submitForReview:112-148`: new `FeedbackRequest(PENDING)` + `writeSnapshots(…, SUBMITTED):141-142`.
2. Return → `returnReview:423-457`: publish drafts + `writeSnapshots(same request, BASELINE):451-453`.
3. Resubmit → new `FeedbackRequest` B + its `SUBMITTED`. Old feedback pinned to A (`requireRootDraftEditable:593-596`).
4. Open B + Show Diff → `useInstructorReview.js:251-272` fetches `GET /api/feedback-requests/${activeRequest.id}/section-snapshots?sectionId=` (**active request only**; no round param exists) → picks `find(BASELINE)` vs `find(SUBMITTED)` (`:266-267`). Backend `:357-364` returns those rows; **no server diff, no cross-request logic** (controller `:87-90` confirms single-request design). Client `wordDiff` (`wordDiff.js:52-87`, LCS, `MAX_TOKENS=2000`) → `changeRanges`; 3 views highlight same ranges (editor `LatexEditor.jsx:735-748`, LaTeX preview `latexHtml.js:165-181`, markdown `markdownBlocks.js:229-269`). `diffOps/diffTruncated` exported, **never consumed** — no side-by-side view exists.

### Concrete example

Section 5 original: `Machine learning systems require extensive testing.` Revised: `Machine learning systems require extensive testing and monitoring.`

| Row | Owner | Content | Diff result |
|---|---|---|---|
| A.SUBMITTED | Request A | original | paired with A.BASELINE → original-vs-original = **empty** |
| A.BASELINE (at Return) | Request A | original (copied live ≈ submitted) | see above |
| B.SUBMITTED | Request B | revised | **no BASELINE on B** → `baseline=null` → `diffResult=null` (`:295`) = **nothing** |

Frontend on B asks `GET /api/feedback-requests/B/section-snapshots?sectionId=5` → `[B.SUBMITTED]` → `find(BASELINE)` = null. The real revision (A→B) is never queried. Prior audit §4.2 concern **confirmed on HEAD**.

### Correct semantics

**`previous submitted revision vs current submitted revision`** = `A.SUBMITTED (fallback A.BASELINE) vs B.SUBMITTED` (same `sectionId`). Evidence: matches submit→return→edit→resubmit product flow; both rows exist with stable `PaperSection.id` + `UNIQUE(request,section,type)`; `A.BASELINE` ≈ `A.SUBMITTED` by construction (copied at Return from locked submission), so "baseline-vs-current" coincides normally — but previous-submitted is principled (written atomically with its submission; BASELINE is later, from mutable live state). Recorded conflict: code comments (`useInstructorReview.js:258-259`, controller `:87-90`) assert single-request design — that design **is** the bug.

### Snapshot integrity — partial

`review_section_snapshots` (`V37:9-24`): `request_id + section_id + content_tex + content_version? + type + createdAt`; `CASCADE` from request and section. Missing: `title/order/assignee/document/project` (mutable joins — renames/reorders alias history); `content_version` nullable. Section/document deletion erases snapshots + feedback (CASCADEs `V37:19-20`, `V1:241`, `V1:161`). Legacy blob `submission_snapshot_json` (`V19:16`, seeded by `V37_1`) mitigates content loss but is opaque. Recommend freezing `title/order` additively with the fix; section-delete cascade = known P2 limitation.

### Involved files / gap / verdict

FE `useInstructorReview.js:251-301`, `EditorPanel.jsx:263-274,433,499`, `LatexEditor.jsx:76-78,256,735-748`, `PreviewPane.jsx:76-99`, `latexHtml.js:165-181`, `markdownBlocks.js:229-269`, `wordDiff.js`; BE `getSectionSnapshots:357-364`, `writeSnapshots:366-381`, `submitForReview:141-142`, `returnReview:451-453`, `ReviewSectionSnapshotRepository`, `FeedbackController.java:87-102`; DB `V37:9-24`, `V37_1:55-92`. Gap: `FeedbackRevisionMySqlTest:86-141` never asserts snapshots/diff (checkpoints `@MockBean:80`); `CheckpointServiceImplTest:108` mocks-only; `wordDiff.test.js` pure-function-only; routes-only assertions elsewhere. Prior "lifecycle not covered" **still true**. **Verdict: `NEEDS DESIGN DECISION`** (lock `prev.SUBMITTED ?? prev.BASELINE` vs `curr.SUBMITTED`, prev = latest earlier `requestedAt`, + fallbacks; then small implementation reusing `wordDiff` + renderers).

## 6. Finding C — Preview Feedback

**Selection architecture:** CodeMirror (`LatexEditor.jsx:322-384`) → `captureSourceSelection` (`useInstructorReview.js:367-389`, CRLF→LF) → anchor `{from, to, contentVersion, fingerprint, representation:'latex-source-lf-v1', offsetUnit:'utf16'}` + `sectionId` → composer (`FeedbackThreadsTab.jsx:92-145`) → `POST …/feedback` (`:333-358`) → `anchorJson` (`V26`), validated (`FeedbackAnchorService.create:137-143`), re-resolved (`resolveAnchor→selectRange`, `:503-515`). Anchors come **only** from the Editor (`EditorPanel.jsx:143`).

**Preview architecture:** `PreviewPane.jsx:66-167`, **client-only** (no server preview): LaTeX → `renderLatexToHtml` + `dangerouslySetInnerHTML`; else `ReactMarkdown + remark/rehype + KaTeX`; `useDeferredValue`; shared `changeRanges`.

**Why unavailable (exact):** `describePreviewSelection` (`PreviewPane.jsx:53-64`) returns `{kind, rect}`, **no offsets** ("unmappable by construction"). Parent (`EditorPanel.jsx:135-144,504-533`) shows amber `role="alert"` (`previewUnavailableTitle/Action`) whose action `lockPreviewSelection` clears selection and switches to Editor. FAB wires to editor only (`:433`). Preview selection **can never** anchor — by design (phantom-duplicate rationale `:132-134`).

**Strategies:** (1) Map preview→source — **reject** (KaTeX has no source map; only block `data-src-*`; string-match reintroduces phantom duplicates; would fork renderer). (2) Preview-native anchor — **reject** (new columns/migration/DTOs/backfill for what Strategy 3 gives free; YAGNI). (3) **Preserve editor selection while Preview visible — RECOMMEND** (FE-only: `selectedAnchor` already focus-independent; composer + whole-section fallback exist; no DTO/DB/API change). (4) Existing mapping — **absent** (only `data-src-start/end` on asset-toggle + scroll anchors). State clearly: exact preview-text→offset mapping is unsafe on this renderer; Strategy 3 is correct scope (later optional: block-level "attach to whole preview block"). **Backend/API/DB changes: none. Verdict: `READY WITH PREREQUISITES`** (one-line UX lock on Strategy 3; then FE-only + new test: select → preview → save keeps offsets).

## 7. Finding D — History Tab

**Data source:** none on tab switch — reuses metadata-only `orderedRequests` (`InstructorFeedbackPanel.jsx:90`, from `GET /api/feedback-requests`, `FeedbackRequestResponseDto` without content). **Renderer:** inline `<ul>` (`:89-101`): `requestedAt` + `status` per request; zero `replies/*Status` references. **Why metadata:** needed data is **already loaded and ignored** — `loadFeedback` (`useInstructorReview.js:303-318`) fans out `GET …/{id}/feedback` over all rounds; each is the **full** DTO (`getFeedbackItems:184-191` → `response:748-772`: content/anchor/replies[]/attachments[] + presigned URLs). Backend returns full content today; History never reads `feedbackItems`. No backend change needed for v1.

**Reusable card (do not reimplement):** `FeedbackThreadsTab.jsx:153-163` (`export FeedbackThreadsTab`, props `{review, selectedSection, projectId, composerFocusToken}`) — draft badge, state chip, location, `originalContext`, student display, thumbs, replies, action row. Reuse read-only (existing `canCreateRoot=false` on historical rounds + new `readOnly` prop suppressing action row/reply box) or extract `<li>` body into shared `FeedbackCard`. Student `FeedbackPanel.jsx:148-151` `<article>` is NOT the target.

**"Last card" definition:** recommended = **latest root by `createdAt` from immediately previous request** (prev = latest `requestedAt` < active, same project), filtered to `sectionId`, published-only. Matches "last card of previous feedback" + Feedback tab's `createdAt`-asc order (last = max). Rejected: `updatedAt` (edits bump it). Recorded ambiguity: multiple feedbacks per section per round — show one (max `createdAt`); siblings reachable via round switch; attachments + legacy replies render inside reused card. **API/DB: none v1** (client filter; optional `?sectionId` later). **Verdict: `READY`.**

## 8. Cross-Cutting Risks

1. **Data loss (P0):** dropping reply tables/columns destroys rationale + legacy discussion + reply files (CASCADEs §4). → soft-removal; hard drop only via migration (Phase 6).
2. **Migrations:** V27→V34→V35→V36 validated chain; V37 additive-only. Never edit applied; diff-fix `title/order` freeze = new additive migration.
3. **Stale APIs:** disabled endpoints must fail closed (410/400), not silently ignore. Keep `replies[]/studentStatus` in thread JSON until Phase 6.
4. **Student dependencies:** reads shared + unaffected; writes only via `PATCH …/state` student branch (disabled with ack). No assignee gate exists (notify/display-only) — nothing stranded. Students read published threads during review (by design) but not drafts (404 `:233-235`) — keep.
5. **Permissions:** delete `postReply` dual-role branch + `prepareFeedbackState` student branch; keep read matrix (request-instructor OR student-member, admin bypass). Do NOT narrow to instructor-read-only.
6. **Attachments:** sound today; resolve reply-attachment display gap (§4-D) before hard delete.
7. **Snapshots:** die with request/section (CASCADE); no delete-request endpoint exists, so live risk = section/document deletion. Known P2 limitation; don't redesign cascades here.
8. **Lifecycle invariants:** `requireLatestRequest` + PENDING-only Return + resubmit-new-request underpin diff + History selectors. A future "reopen old round" breaks both — record constraint.
9. **Approve gate entanglement (NEEDS DECISION):** `approveReview` requires all threads `RESOLVED|REJECTED` (`:476-478`) — unsatisfiable once resolve is removed → Approve deadlocks on any feedback-bearing project. **Must decide with removal: (a) drop gate [recommended — one-way needs no closure] or (b) auto-close on Return.** Removal must not ship without this.

## 9. Delete / Keep / Migrate Matrix

**Dependency matrix:**

| Change | Frontend | Backend | DB | Existing data | Tests | Risk | Verdict |
|---|---|---|---|---|---|---|---|
| Remove replies/ack | Delete composers/buttons/hooks | Disable `POST …/replies` + `studentStatus`; keep thread reads | None (retain read-only) | Preserved; legacy read-only | Invert reply/ack tests; add disabled-tests | Low soft / P0 hard | `READY WITH PREREQUISITES` |
| Fix Show Diff | Two-request fetch + pick; renderers unchanged | Optional aggregate endpoint | Optional additive title/order freeze | Preserved; old rounds comparable | New regression test | Medium | `NEEDS DESIGN DECISION` |
| Preview feedback | Keep `selectedAnchor` armed w/ Preview visible | None | None | Unaffected | New FE anchoring test | Low | `READY WITH PREREQUISITES` |
| History card | Filter `feedbackItems` + reuse card read-only | None v1 | None | Surfaced | New selector + render tests | Low | `READY` |

| Component | Delete | Keep | Migrate | Reason |
|---|---|---|---|---|
| Reply composer + `sendReply/postReply` + reply picker | ✅ | | | Active chat; no history dep |
| Resolve/reject/reopen buttons + `runState` + reject-note box | ✅ | | | Obsolete machine; keep chip display |
| Student ack buttons + `handleStudentState` + note box | ✅ | | | One-way never mutates |
| `POST …/replies` mapping | ✅ (410/403 now) | | | Zero schema risk |
| `studentStatus/studentNote` acceptance | ✅ (400 now) | | | Columns retained |
| Approve all-closed gate | decision | decision | | Deadlocks Approve — product call |
| Replies renderers (both panels) | | ✅ read-only | | Legacy display |
| `FeedbackReply` + table + constraints | | ✅ read-only | Later hard-drop | P0 loss if dropped |
| `FeedbackReplyRepository` (minus dead finder) + reply response DTO + `replies[]` | | ✅ | Later | History reads |
| `FeedbackReplyRequest` + `PostReplyResult` | | | ✅ remove w/ endpoint | Dead after disable |
| `reply_id` column + dual-FK + reply files | | ✅ | Display-migrate first | CASCADE file loss |
| State/status columns + CHECKs | | ✅ read-only | Later | History display |
| `requireRejectionNote` | | ✅ interim | Then delete | Coupled to REJECTED |
| Snapshots + writes | | ✅ | Additive freeze (opt) | Diff builds on model |
| Request-level Reject | | ✅ | | Unrelated refusal path |
| History metadata `<ul>` | ✅ (replaced) | | Reuse card | Shows no content |
| Duplicate History card / preview-mapper / preview-anchor system | ✅ (don't build) | | | YAGNI |
| `diffOps/diffTruncated`, dead finder, orphan locale, citation-round code in review scope | ✅ (P3) | | | Verify each: no caller + no history + no API + no migration dep |
| `submission_snapshot_json` + snapshot/anchor path | | ✅ | | Legitimacy + version pinning |

## 10. Test Gap Analysis

| Priority | Existing coverage | Gap |
|---|---|---|
| Submit→feedback→Return | `FeedbackRevisionMySqlTest:86-141`; `FeedbackPublicationMySqlTest`; `FeedbackServiceImplTest`; `FeedbackControllerTest:40-165` | Covered ✔ |
| Edit→resubmit | `FeedbackRevisionMySqlTest:115-140`; `SubmissionReadinessServiceTest:98-234`; `PaperProcessingServiceImplTest` | Covered ✔ |
| **Return→edit→resubmit→diff** | Closest `FeedbackRevisionMySqlTest:86-141` mocks checkpoints (`:80`), asserts no snapshots/diff; `CheckpointServiceImplTest:108` mocks-only; `wordDiff.test.js` pure-only; routes-only elsewhere | **NOT COVERED (P1).** Required: submit(A)→comment→RETURNED→assert A SUBMITTED+BASELINE→edit→resubmit(B)→assert B SUBMITTED/no BASELINE→GET both→fixed selector→ranges contain inserted phrase |
| History | Routes-only (`ProjectRouteMappingTest:112,154-155`); checkpoint baselines (not review history) | **NOT COVERED.** Required: prev-round max-`createdAt` selector (multi-feedback, drafts/wrong-section excluded) + render test |
| Preview | `sourcePreview.test.mjs` (download only); `scrollSync`; `feedback-anchor-fab.spec.js` (editor FAB); nothing imports `PreviewPane` | **NOT COVERED.** Required: select→preview→save keeps offsets; preview-only selection never arms anchor |
| Reply removal | `FeedbackControllerTest:120-145`; `FeedbackServiceImplTest:226-233,453-632`; `FeedbackRevisionMySqlTest:144`; `FlywayMigrationMySqlTest:195-225` | Must **invert**: keep preservation + DDL; assert `POST …/replies → 410`, `studentStatus → 400`, buttons absent, legacy renders |
| Full chain | Pieces only | **NOT COVERED.** Required: submission → feedback → Return → read → edit → resubmit → new-round diff → History card → Preview-anchored feedback |

FE: no vitest/jest (`package.json:6-18`; `node:test` + 1 Playwright spec). BE: H2/Flyway-off unit profile (`src/test/resources/application.yml:2-17`); MySQL tests `disabledWithoutDocker`. Missing Docker = environmental, not product defect; gate regression test accordingly.

## 11. Recommended Implementation Sequence

**Phase 1 — Lock invariants (docs/ADR only, no code).** Record: (a) diff truth = `prev.SUBMITTED ?? prev.BASELINE` vs `curr.SUBMITTED` (prev = max earlier `requestedAt`, same project) + fallbacks; (b) History = max-`createdAt` published root for `(prevRequest, sectionId)`; (c) reply soft-first + approve-gate choice. Done when all three written.
**Phase 2 — Soft-remove reply/ack.** Delete composers/buttons/hooks/endpoint acceptance (§4); keep tables/DTO-response/legacy render; implement approve-gate decision. Tests: inverted reply tests; preservation test green. Done when: no write path remains, disabled endpoints fail closed, legacy renders.
**Phase 3 — Fix diff lifecycle.** Two-request fetch + pick (`useInstructorReview.js:251-301`); renderers untouched; optional additive `title/order` freeze. Test: P1 regression (§10). Done when: B shows A→B delta; A shows honest empty state.
**Phase 4 — History card.** Replace `:88-102` with filtered reuse of `FeedbackThreadsTab` card read-only (+`readOnly` prop). No BE/DB. Tests: selector + render. Done when: last prev-round card renders identically to Feedback tab.
**Phase 5 — Preview-visible feedback (Strategy 3).** Keep `selectedAnchor`/FAB armed with Preview mounted (`EditorPanel.jsx:132-159,504-533`); composer unchanged; no BE/DB. Test: anchoring test. Done when: editor selection survives Preview → Save keeps offsets; preview-only selection keeps honest notice.
**Phase 6 — Hard-delete dead persistence (post-retention-decision).** New migration (drop FKs → handle files → drop objects); remove endpoint/service/repo/entity/request-DTO; update DDL tests. Park indefinitely if retention is indefinite — acceptable steady state.
**Phase 7 — Regression verification.** §10 chain test (Docker profile) + manual walkthrough through Approve/Reject.

## 12. Final Verdict

```text
Remove reply workflow: READY WITH PREREQUISITES
Show Diff:              NEEDS DESIGN DECISION
Preview feedback:       READY WITH PREREQUISITES
History card:           READY
```

**Can implementation begin now? `YES AFTER PREREQUISITES`** — Phase-1 paperwork (diff semantics, History definition, soft-first + approve-gate choice), hours not sprints. No `BLOCKED BY UNKNOWN DEPENDENCY`: every event traced UI → component → hook → API → controller → service → repository → table → response → render (absent layers stated as absent: `ReviewRequest`/`ReviewRound` classes, mappers, server diff, preview source map, History fetch). Simplest correct architecture: **current workspace minus the conversation layer (mutations deleted, tables read-only) + two-request diff selector + History reusing the existing card + Preview that stops disarming editor selection — no new anchor system, no new card, no schema surgery until retention is decided.**
