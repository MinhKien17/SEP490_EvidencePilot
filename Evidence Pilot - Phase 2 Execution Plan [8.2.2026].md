# Evidence Pilot — Phase 2 Execution Plan [8.2.2026]

> Tracking plan for the Phase 2 audit + planned remediation (Phase 1 CRITICALs are DONE).
> Status: **PLANNED — READY FOR APPROVAL** · Scope: **FE + BE** (approved fixes & wiring) · Deliverable of this phase: this plan; execution follows on approval.
> All line numbers verified against the current working tree 8.2.2026.

---

## System snapshot

- Repo: `D:\FPT\FA26\SEP490\Prototype_3\SEP490_EvidencePilot` (git `main`)
- Stack: Java 21 / Spring Boot 3.3.5 (BE) · React 19 + Vite 8 + Tailwind 4 (FE) · MySQL / Qdrant / MinIO / RabbitMQ
- BE paths below relative to `BE/src/main/java/com/evidencepilot/` · FE paths relative to `FE/src/`
- NOTE: filename uses `-` instead of `:` (colon is illegal on Windows)

---

## 1. [APPROVED] Immediate UI/UX Fixes — `pages/Admin/AdminDashboard.jsx`

### 1.1 Sidebar Sign-Out / Collapse buttons disappear intermittently
- **Root cause (found):** `<aside>` (`:3981`) is `fixed lg:static` with **no height bound**. On desktop the shell is `min-h-screen` flex + body scroll; tall sections (Users, Settings, …) stretch the sidebar so the bottom block — Sign Out (`:4012`), Collapse (`:4018-4020`) — scrolls below the fold. Not a mount/state bug: buttons are always in the DOM; the aside just scrolls away. Intermittent because section heights vary.
- **Fix (1-line class change):** aside gains `lg:h-screen lg:sticky lg:top-0` → `<nav>` (`flex-1 overflow-y-auto`, `:3999`) scrolls internally, bottom stays pinned. No state changes; `collapsed`/`mobileOpen` logic untouched.
- **Verified facts:** state at `:3881`; nav items `:4000-4007`; no `window.innerWidth` listener; aside always rendered (no conditional unmount).

### 1.2 i18n — hardcoded English in admin UI
- **Root cause (found):** there are **no locale files** — locales are inline objects `t.en` (`:9-40`) / `t.vi` (`~:110-171`). 9 section headers + several buttons bypass the `lang` prop and hardcode English, so the EN/VN toggle (header `:4061-4066`) does not translate them.
- Hardcoded headers: User Accounts (`:603`), Research Projects (`:1077`), Papers Overview (`:1606`), Audit Logs (`:1780`), System Health (`:2020`), Extraction Queue (`:2264`), Broadcast Notification (`:2548`), Collections Library (`:3019`), System Settings (`:3497`).
- Hardcoded labels: Process Guide (`:611`), Create User (`:615`), Guide (`:4057`), Refresh Logs (`:1788`), breadcrumb "Admin" (`:4036`), search placeholder (`:4047`), profile "Admin User"/"System Manager" (`:4071-4072`), footer `v2.4.1` tagline (`:4088`).
- **Fix:** use existing keys (`userAccounts`, `auditLogs`, `systemHealth`, `broadcast`, `settings`, `collections`, `processGuide`, `createUser`, `tourGuide`, …); add missing keys (projects, papersOverview/overview, extractionQueue, refreshLogs, …) to **both** `t.en` and `t.vi`.

### 1.3 Invisible scrollbars app-wide
- **Root cause (found):** `index.css:89-96` — global `*::-webkit-scrollbar { width: 0; height: 0 }` + `* { scrollbar-width: none; -ms-overflow-style: none }` hides every scrollbar in the app; users scroll blind in tall admin lists.
- **Fix:** delete `:89-96`; add a subtle default thumb (`::-webkit-scrollbar { width: 6px }` + thumb `rgba(0,0,0,.15)`, rounded). `.hide-scrollbar` (`:81-87`) stays as the opt-in for elements that must not show one; `.custom-scrollbar` (`:70-79`) already works and is unaffected.

### 1.4 Maintenance banner (new) — static i18n, admin only
- **Fix:** dismissible banner at the top of the shell root div (`:3976`), message from new `lang` keys (EN/VI), **one-time-per-session** dismissal persisted in `sessionStorage` (`admin_maint_banner_dismissed`). No BE work (user-approved scope).

### 1.5 Audit Log filters → server-side
- **Root cause (found):** `AuditLogsSection` (`:1684-1773`) fetches only `page/size` (`:1697`) and then filters **client-side across the single fetched page** (`filteredLogs`, `:1764-1773`) — with pagination this hides/misses rows and breaks totals.
- **Fix:** wire `userFilter` → `actorId` query param (DTO `AdminAuditLogResponse` already returns `actorId`; BE `getAuditLogs` accepts it — `service/AdminService.java:200-215`). Keep `q` text + `actionFilter` client-side (BE has no keyword/action params). Note: BE **ignores** `entityType` without `entityId` (`AdminService.java:204-213`) — only send actor/entityId combos the BE honors.

### 1.6 Papers → "Documents" rename (admin UI)
- **Fix:** NAV label `papers: 'Papers'` → `'Documents'` (`:11` EN, `:111` VI), header `:1606` → "Documents Overview" (via lang keys). FE-only; BE already uses `documents` tables/`docType PAPER|SOURCE`.

---

## 2. [APPROVED] API Wiring & Diagnostics

### 2.1 Enhanced `/api/health` — audit result: **already implemented, no BE change**
- **Found:** `service/HealthService.java:33-96` already checks all five infra components with latency: `database` (JDBC ping, `:38-46`), `aiWorker` (LLM worker `GET {baseUrl}/health`, `:53`), `qdrant` (`/collections/source_chunks`, `:61`), `minio` (`bucketExists`, `:74`), `rabbitmq` (`queueDeclarePassive("extraction.queue")`, `:84-87`); aggregation DOWN vs DEGRADED at `:49/:57/:65-69/:79/:92-93`. No actuator dependency (`BE/pom.xml`) and none is being added — API-level only, per scope.
- **FE work:** System Health tab (`:2020`) binds to real `GET /api/health` (+ `AdminDashboardResponse.infrastructureReadiness`) and renders the per-component `status`/`latencyMs` the API already returns.

### 2.2 Pipeline Diagnostics Viewer (Papers tab)
- **Found:** raw OpenAlex metadata is **not persisted** — only flattened columns on `Document` (`service/impl/OpenAlexIngestionServiceImpl.java:107-126`); `DocumentResponse` omits `doi`/`title`/`processingError` (`controller/DocumentController.java:53-57`); extraction JSON is checkpointed to MinIO `documents/processed/{id}/extraction.json` (`service/impl/DocumentExtractionWorkerImpl.java:66,101`); errors land in `Document.processingError` (no error entity — `DocumentPersistenceService.java:136-142`); `extractionQuality` JSON column exists but is dead code.
- **Plan — one new ADMIN endpoint** `GET /api/documents/{id}/diagnostics` returning `{ document (rich DTO incl. doi, title, processingStatus, processingError, chunkCount, processedAt), openAlexRaw (live re-fetch via `OpenAlexClient.fetchWork(doi)` — `client/openalex/OpenAlexClientImpl.java:49-70`), extractionJson (streamed from MinIO), errorLog (processingError) }`. `@PreAuthorize("hasRole('ADMIN')")`.
- **FE work:** Documents tab gets a "Diagnostics" view per row: doc status + UUID, three JSON trees (raw OpenAlex metadata, system extraction output), extraction error log banner.

### 2.3 Extraction Queue — RabbitMQ console link + failed-entry enrichment
- **Found:** failed entries expose only `id, originalFilename, processingError, createdAt` (`service/AdminService.java:234-249`); no `projectName`; `attempts` is **not persisted** (retry count exists only in listener config, `application.yml:58` → skip attempts).
- **Plan:** `AdminController /config` (`:66-77`) gains `rabbitMqManagementUrl` from env `RABBITMQ_MANAGEMENT_URL`; FE queue tab header (`:2264`) renders a "RabbitMQ Console" link. Failed-entry payload gains `projectName` (join). Absorbs Phase-1 doc item **#6** (link part); DLX/DLQ + `GET /api/admin/queue-metrics` remain Phase 3.

---

## 3. [INVESTIGATION REQUIRED] Constraints & Limits — audit results

### 3.1 Admin project-member removal → notification with reason
- **Found — already implemented:** `service/impl/ProjectServiceImpl.java:308-313` sends in-app `PROJECT_MEMBER_REMOVED` (actor + reason in message) via `SystemNotificationServiceImpl` (persists + STOMP push to `/queue/notifications`); guards refuse instructor (`:295-298`) and last leader (`:299-307`) removal; `addMember` mirrors it (`:273-278`).
- **Gap:** no email on removal — `JavaMailSender` only exists for verification/reset (`EmailVerificationServiceImpl.java:32`, `PasswordResetService.java:32`).
- **Decision:** in-app notification already satisfies the requirement; email notification **deferred** (mail infra is verification-only today).

### 3.2 Platform resource limits engine
- **Found — none exists:** no `/api/limits` or `/api/admin/platform-limits` endpoint, no usage-counter/limit columns in any entity, no tier/plan code (grep across BE). Only real guardrails: upload `max-file-size: 50MB` (`application.yml:39-40`), bundle caps 100 MiB / 200 MiB (`ExtractionBundle.java:75,100`), per-entry text cap (`:221,226`), plus DB `UNIQUE`/`CHECK` constraints (`resources/schema.sql`).
- **Decision:** remove the static FE card "Platform Resource Limits (Capstone Constraint Display)" (`AdminDashboard.jsx:3616-3623`) — it is presentation-only with no BE counterpart. No BE code added.

---

## 4. [EPIC ANALYSIS] Admin AI Helper (Low Priority) — analysis only, zero code

- **Stack reality:** there is **no Gemini** in the codebase — the LLM is a Python worker reached via `service/AiModelClient` (`impl/AiModelClientImpl.java:37-42`, `GET {baseUrl}/health`); the epic would extend that worker, not add a Gemini SDK.
- **Concept:** ADMIN-only chat over sanitized system data (dashboard metrics, health, audit summaries) with caching-RAG (Qdrant `source_chunks` for prior Q&A).
- **Steps:** ① new `AdminAiService` (ADMIN-gated) → ② Python worker endpoint for admin queries → ③ answer cache keyed by query hash → ④ FE chat panel in admin console.
- **Pros:** ops efficiency, natural-language drill-down, reuses existing AI worker + Qdrant.
- **Cons:** audit-log data exposure risk, latency on live metrics, hallucination risk on numbers, scope creep into worker service.
- **Verdict:** **PARK — Phase 4 epic. Does NOT block the current release.** Tracked here only as required.

---

## 5. [REJECTED] Architectural Pushback

- **RabbitMQ UI recreation → REJECTED.** No custom RabbitMQ web console. Phase 3 instead: basic queue metrics (message/consumer counts via AMQP) + hyperlink to the native RabbitMQ Management Console (`RABBITMQ_MANAGEMENT_URL`, see 2.3). Absorbs Phase-1 doc item **#6** partially.
- **DB backup/restore → REJECTED.** Phase 1 already deleted the FE card; no BE backup endpoints exist and none will be added. DR stays at infra level (MySQL snapshots / cloud PITR) — same rationale as Phase 1 Decisions ledger.

---

## Scope & non-goals

- **In:** all of Sections 1–2 (FE fixes + diagnostics endpoint + queue enrichment), Section 3.2 FE card removal. Email on member removal: **out** (3.1). Admin AI Helper: **out** (Section 4).
- Phase-1 doc carry-over: **#6** queue console link absorbed (2.3); **#8** audit server-side filters absorbed (1.5) — CSV export stays Phase 3; **#5/#7/#9/#10** remain in Phase 1 doc as-is.

## Verification (after execution)

1. `cd FE && npm run build` — must pass.
2. `cd BE && mvn compile` (MapStruct) then `mvn test` for the new diagnostics endpoint.
3. Grep sweep: no hardcoded `>User Accounts<`-style headers remain; no global scrollbar-hide rule in `index.css`.
4. Manual smoke: sidebar Sign-Out/Collapse visible on tall sections at 1280×720; EN/VN toggle translates all 9 section headers; audit filters hit the server (Network tab shows `actorId`); RabbitMQ Console link opens the management UI.

---

## Decisions ledger

| Decision | Resolution |
|---|---|
| Maintenance banner | Static i18n message, admin console only, `sessionStorage` one-time dismissal (user-approved) |
| Resource limits engine | **None exists in BE** → remove FE static card (`AdminDashboard.jsx:3616-3623`) |
| Member-removal email | **Deferred** — in-app notification already sent (`ProjectServiceImpl.java:308-313`) |
| Health endpoint | **No BE change** — all 5 components already checked (`HealthService.java:33-96`) |
| Admin AI Helper | **Parked (Phase 4)** — does not block release |
| RabbitMQ UI / DB backup-restore | **Rejected** — see Section 5 |

---

## Progress tracker

| Task | Status |
|---|---|
| Section 1 — UI/UX fixes (sidebar, i18n, scrollbar, banner, audit filters, rename) | DONE (executed 2.8.2026; FE build green) |
| Section 2 — diagnostics endpoint, queue enrichment, health wiring | DONE (executed 2.8.2026; BE compiles, route + diagnostics tests green) |
| Section 3 — investigation (limits card removal) | DONE (card removed from SettingsSection) |
| Section 4 — Admin AI Helper epic | PARKED (analysis done, no code) |
| Section 5 — rejected items | REJECTED (no work) |
