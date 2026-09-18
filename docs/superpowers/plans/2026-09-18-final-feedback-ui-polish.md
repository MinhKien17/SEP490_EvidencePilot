# Final Feedback UI Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Finish the Student and Instructor feedback UI polish without changing the working backend workflow.

**Architecture:** Reuse the existing feedback/selection/attachment state and simplify presentation. Student feedback becomes a normal stacked-card panel with compact same-row filters. Instructor create/edit reuses current attachment and passage-selection logic while removing redundant selection controls.

**Tech Stack:** React 19, Vite 8, Tailwind 4, inline SVGs, current FE test stack (node --test + Playwright + `vite build`).

## Global Constraints

- Frontend only unless inspection proves otherwise.
- No backend/database changes.
- No new dependencies for icons or styling.
- Preserve current feedback persistence and round behavior.
- Reuse existing selection/FAB positioning logic.
- Keep every commit green.

## Verified baseline (HEAD `ee0d869`, `main`, tree clean)

- `npm test`: 167 pass / 0 fail. No Bootstrap / Bootstrap Icons / `btn-*` in FE (grep-verified). Danger precedent: solid `bg-rose-600 text-white` (`MediaAssetPicker.jsx:69`).
- Student defaults already correct, no logic change: `WorkspaceLayout.jsx:174` (`scope='section'`), `:177-182` (latest `RETURNED` auto-selected, requests newest-first).
- `getFeedbackPositions` / LatexEditor `feedbackRanges` field / `onFeedbackChange` serve `WorkspaceLayout.jsx:424,916` (card-to-editor reveal) — they STAY. Only EditorPanel's panel-feeding measurement goes.
- `captureSourceSelection` (`useInstructorReview.js:379,726`) stays for the EditorPanel FAB create path (`EditorPanel.jsx:174`).
- E2E inventory (`FE/e2e/`): `student-feedback-visibility`, `review-feedback-selection`, `feedback-anchor-fab`, `preview-source-mapping`, `preview-parity`, `preview-refusal`. No new spec files.

### Task 0: Verify FE baseline

No code. No commit. Run `git status`, `git rev-parse HEAD`, `git diff --check`, `npm test`. Run `npm run build` if the execution environment permits normal build artifacts.

### Task 1: Student feedback panel simplification

**Files**
- Modify: `FE/src/components/Student/FeedbackPanel.jsx`, `FE/src/components/Student/EditorPanel.jsx` (remove `positions` state/setter, `overlapIds` state, `measureFeedback` body/call-sites; keep resize observer for `availableWidth`, keep `handleFeedbackClick`→select-first, keep `narrow` for EditorPanel's own layout), `FE/src/utils/student/feedbackAnchors.js` (delete `placeFeedbackCards`, now caller-free), `FE/src/utils/student/feedbackAnchors.test.js` (drop its cases), `FE/src/locales/en.json`, `FE/src/locales/vi.json` (delete `studentFeedback.navigate`, `studentFeedback.overlap`, `studentFeedback.offscreen`, `studentFeedback.filters` after grep-confirming no other callers)
- Delete: none (shrink in place)
- Test: `FE/e2e/student-feedback-visibility.spec.js` (extend existing fixtures)

**Behavior**
- Card list always `space-y-3` normal flow. Delete: `anchored` flag, absolute positioning, `layout`/`sizeVersion`/`scrollTop`/`byId`/`visibleItems`/`listHeight`, `useLayoutEffect` packing, connector SVG (`FeedbackPanel.jsx:145-147`), viewport culling, offscreen note. Active `border-teal-600 ring-1` stays.
- Delete navigate select (`:116-120`) and overlap buttons (`:121-125`). Verified routing outcome: editor-highlight click still selects first match (`EditorPanel.jsx:139-143` minus `setOverlapIds`; `WorkspaceLayout.jsx:418-429` needs no `overlapIds`); multi-overlap reached via visible cards. No new navigation added.
- Filters: delete `<details>` wrapper; always-visible `grid grid-cols-1 sm:grid-cols-2` row with scope select + round select (leader) / round label (member). No panel overflow. Defaults untouched.
- `canNavigate` simplifies to cross-section check (position data gone). `positions`, `narrow`, `overlapIds` props removed from panel signature.
- [ ] Step 1: extend visibility spec — filter row directly above list (DOM order), `select[aria-label="Go to feedback"]` absent, `article[data-feedback-card] svg` count 0 (narrowly scoped, not page-wide), offscreen text absent, defaults This section + latest RETURNED, scope+round share one grid container
- [ ] Step 2: run spec, confirm new assertions fail
- [ ] Step 3: implement minimal deletions/reposition
- [ ] Step 4: spec PASS
- [ ] Step 5: `npm test` + `student-feedback-visibility` Playwright + `npm run build`
- [ ] Step 6: commit green code

### Task 2: Instructor feedback form/card layout polish

**Files**
- Modify: `FE/src/components/Instructor/review/FeedbackThreadsTab.jsx` (composerForm only), `FE/src/locales/en.json`, `FE/src/locales/vi.json` (add `instructor.review.attachments`: "Attachments" / "Tệp đính kèm"; delete `instructor.review.useSelection` after caller grep)
- Test: `FE/e2e/review-feedback-selection.spec.js` (extend)

**Behavior**
- Move the whole `MediaAssetPicker` below the textarea (no picker refactor, no new props). Above it, an `Attachments` header row; strip hidden when empty, Add-media button always discoverable (renders under strip per picker's own order — accepted YAGNI deviation from top-right mock).
- Passage row: label + `[Change passage]` (arrows SVG `FeedbackThreadsTab.jsx:102` deleted) + `[Remove passage]` restyled solid `bg-rose-600 text-white hover:bg-rose-700`, same `rounded-lg px-2.5 py-1.5 text-[10px] font-black` shape as the teal button. Wrap in `data-testid="passage-controls"`.
- Delete `Use editor selection` button only. KEEP `Use new selection` + `useNewSelection` through this task: it is adjustment mode's sole confirm path until Task 3 replaces it (removing it here would land a green-but-unusable adjust flow). `Keep current` also stays through this task for the same reason.
- Applies to create + edit automatically (shared `composerForm`).
- [ ] Step 1: e2e — create with 3 images: textarea precedes `Attachments` precedes thumbs precedes Save (DOM-order assertions); × removes one; edit card: Change+Remove share one row container, `Use editor selection` absent, Remove matches `/rose/`
- [ ] Step 2: confirm fail
- [ ] Step 3: implement reorder/restyle/label
- [ ] Step 4: PASS
- [ ] Step 5: `npm test` + Playwright + build
- [ ] Step 6: commit green code

### Task 3: Floating passage-confirmation interaction

**Files**
- Modify: `FE/src/hooks/useInstructorReview.js` (add `isAdjustingPassage`, `startPassageAdjust()`, `cancelPassageAdjust()`, `confirmPassageSelection()` built on existing `sourceEditorRef` + `updateFeedbackDraft`; no new store), `FE/src/components/Student/EditorPanel.jsx` (FAB branches on `review.isAdjustingPassage && editing`; suppress FAB for edit-mode non-adjust selections), `FE/src/components/Instructor/review/FeedbackThreadsTab.jsx` (Change passage toggles adjust via hook; delete `Use new selection`, `useNewSelection`, `Keep current`, local `adjustingPassage`; Escape exits adjust via local keydown, draft untouched), `FE/src/locales/en.json`, `FE/src/locales/vi.json` (add `instructor.review.useThisPassage`: "Use this passage" / "Dùng đoạn văn này"; delete `useNewSelection`, `keepCurrent` if caller-free)
- Test: `FE/e2e/review-feedback-selection.spec.js` (extend change-passage cases)

**Behavior**
- `Change passage` toggles adjust mode (click again exits, draft unchanged) — covers pointer users; `Escape` exits adjust with draft unchanged, edit stays open — covers keyboard users. Both replace `Keep current` (deleted; no a11y loss: toggle is a focusable button, Escape is keyboard-native, Cancel-edit remains).
- In adjust mode, editor selection raises the existing FAB portal with an inline chat/message SVG (accessible name "Use this passage", never the icon name). Click → draft anchor = canonical range, label updates, adjust exits. No `Use new selection` button anywhere.
- Edit mode without adjust: selections raise no FAB and change nothing (close the current `openComposerFromFab` overwrite path). `autoCaptureSelection` already skips edits — unchanged.
- Confirm-then-Cancel → persisted unchanged; Remove→`Whole section` draft-only until Update; Update persists same id + new range.
- [ ] Step 1: e2e — confirm flow, Escape-adjust-exit, Cancel-after-confirm, Update-same-id-new-passage, ordinary-selection no-op (draft + no confirm FAB)
- [ ] Step 2: confirm fail
- [ ] Step 3: implement hook flag + FAB branch + suppression + deletions
- [ ] Step 4: PASS
- [ ] Step 5: `npm test` + full review e2e (`review-feedback-selection`, `feedback-anchor-fab`, `preview-*`) + build
- [ ] Step 6: commit green code

### Task 4: Final verification

No implementation bucket. No empty commit. Run `npm test`, all Playwright specs in `FE/e2e/`, `npm run build`, and grep for known-deleted symbols/locale keys (`useSelection|useNewSelection|keepCurrent|studentFeedback.navigate|studentFeedback.overlap|studentFeedback.offscreen|studentFeedback.filters|placeFeedbackCards|overlapIds`). Any failure returns to Tasks 1–3. Commit only if a correction was required.

## Changes From Previous Plan

| Previous | Refined | Reason |
|---|---|---|
| Separate attachment/control tasks | One Instructor layout task (Task 2) | Same file/test surface |
| Generic final cleanup task | Cleanup in owning tasks; Task 4 verify-only | Keep responsibilities local |
| `whatever sweep finds` / `grep first` | Exact files/symbols/lines named | No placeholders |
| `no SVG` assertion | `article[data-feedback-card] svg` count 0 | Avoid false failures |
| Keep `Keep current` | Escape + Change-passage toggle; delete it | Fewer redundant controls; both exits verified reachable |
| Possible picker refactor | Move picker whole; button under strip accepted | YAGNI |
| Blind overlap-state deletion | Verified: `onSelectFeedback` needs no `overlapIds`; highlight-click keeps select-first | Avoid regression |
| Remove `Use new selection` in Task 2 | Keep through Task 2, delete in Task 3 | Otherwise adjust mode lands unusable between commits |
| Exact-Tailwind danger test | `/rose/` substring + row-container assertions | Resilient |
| New e2e files implied | Extend the two existing specs only | Inventory verified |
