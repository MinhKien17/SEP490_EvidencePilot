# Evidence Pilot — System Audit [8.2.2026] [Phase 1]

> Tracking plan for the 4-phase audit + remediation of the Evidence Pilot system.
> Filename bracket = current phase; update `[Phase N]` as phases complete.
> Status: **FINALIZED — READY TO EXECUTE** · Scope: **CRITICAL items 1–4 (FE only)** · No BE changes in this phase.

---

## System snapshot

- Repo: `D:\FPT\FA26\SEP490\Prototype_3\SEP490_EvidencePilot` (git `main`)
- Stack: Java 21 / Spring Boot 3.3.5 (BE) · React 19 + Vite 8 + Tailwind 4 (FE) · MySQL / Qdrant / MinIO / RabbitMQ
- BE entry: `com.evidencepilot.EvidencePilotApplication` · FE entry: `src/main.jsx`
- Build: `mvn package` (BE) · `npm run build` (FE) · Tests: `mvn test` (H2, ~70 files; no FE tests)
- NOTE: filename uses `-` instead of `:` (colon is illegal on Windows)

---

## Phase 0 — Audit (DONE)

Full 4-phase audit performed 8.2.2026.

### CRITICAL findings
| # | Finding | Location |
|---|---|---|
| C1 | **Login backdoor** — FPT Google login first tries `/api/auth/login` with hardcoded seed password `'Password@123'`; full account bypass if any account still has the seed password | `FE/src/pages/Login.jsx:70-97` |
| C2 | **Systemic mock data in AdminDashboard** (4,872-line file): MOCK_* arrays + hardcoded KPIs in 9 of 10 tabs; hash-faked PI names/codes (`getPIForProject`/`getProjCodeForProject`) shown for real projects; hardcoded "Recent System Logs" (2023 rows); fake infra hostnames/uptimes; fabricated queue rows | `FE/src/pages/Admin/AdminDashboard.jsx` |
| C3 | **Queue retry is fake** — `setTimeout(1200ms)` + toast "re-queued and completed successfully!"; real endpoint `POST /api/documents/{id}/re-extract` (BE `DocumentController:188`) never called | `AdminDashboard.jsx:2827-2833` |
| C4 | **Backup/restore theater** — fake `.sql` download containing only `SELECT 'Backup Successful';`; restore = file picker + 1.5s toast, nothing happens. No BE backup endpoints exist | `AdminDashboard.jsx:4173-4196` |

### WARNING findings
- Dead UI buttons: Export CSV/Records (`:571-579, :2218, :1334`), Upload Paper (`:1921`), View All 24 Services (`:2783`), Full Diagnostics (`:481`, driver.js tour)
- `Home/StatsSection.jsx:49` — renders zeros on API error instead of error state
- `WorkspaceLayout.jsx` — unreachable Revise/Paper-Detail modals (`:1337-1366, :1558-1596`), dead undo/redo (`:1084-1090`)
- `Profile.jsx:1-226` — large commented-out dead block
- No rate limiting anywhere in BE; unarchive admin-only while instructor UI shows the button (403); no DLX/DLQ in RabbitMQ (retry-exhausted messages silently dropped)

### PASS findings
- Every FE route maps to an existing BE endpoint (inversion: endpoints exist, FE fakes them)
- BE admin endpoints all DB-backed via `AdminService` (users CRUD, audit-logs, extraction-queue, dashboard, broadcast, broadcast-history, config)
- `AdminDashboardResponse` (real DTO): `totalUsers`, `usersByRole`, `usersByStatus`, `activeProjects`, `activeProjectsByStatus`, `activeCollectionCategories`, `activeCollections`, `activeSourceDocuments`, `activePaperDocuments`, `infrastructureReadiness`
- Health endpoints public & real: `GET /api/health`, `/api/health/live`, `/api/health/ready` (`HealthController` / `HealthService`, includes AI worker + Qdrant checks)
- RabbitMQ retry exists: 3 attempts, 5s → 2x → 30s, `default-requeue-rejected: false` (`application.yml:49-59`)

---

## Phase 1 — CRITICAL remediation (FINALIZED)

Scope decision (user-approved): **all 4 CRITICAL items** · Empty-state policy: **honest** (no fabricated numbers, ever).

### Task 1 — Remove login backdoor · `FE/src/pages/Login.jsx`
- **Change:** delete the `try { await api.post('/api/auth/login', { email, password: 'Password@123' }) }` block inside `handleFptGoogleLogin` (`:70-97`); handler proceeds directly to the manual-password entry flow. No fake Google session, no `localStorage` token write.
- **Verify:** `grep -rn "Password@123" FE/` → zero hits.

### Task 2 — Purge mock data from `AdminDashboard.jsx` (honest empty states)
| Section | Delete | Bind to |
|---|---|---|
| Dashboard KPIs | fallbacks `8,432/452/1,240/15,680` (`:419-459`); storage bar, LLM latency, DB uptime, S:I donut (`:488-553`) | real `AdminDashboardResponse` fields: `totalUsers`, `usersByRole`, `usersByStatus`, `activeProjects`, `activeProjectsByStatus`, `activeCollections`, `activeSourceDocuments`, `activePaperDocuments` |
| "Recent System Logs" | entire table (`:596-642`) — no real endpoint; audit-logs tab is the honest home | — (removed) |
| Fake loading delays | `setTimeout` stubs (`:390`) | — |
| Users tab | `MOCK_GUIDE_USERS` injection (`:684-688`, fallback `:799-801`) | real `GET /api/admin/users`; empty row "No users" |
| Projects tab | `MOCK_PROJECTS` (`:1096-1100`), `getPIForProject`/`getProjCodeForProject` (`:1282-1302`, use sites `:1456-1459`), KPIs `24/118/1.4k/82%` (`:1323-1386`), dead Export Records (`:1334`) | real `GET /api/projects`; `—` for unknown PI; real counts from dashboard DTO |
| Papers tab | `MOCK_PAPERS` (`:1842-1870`), stats `'02'` (`:1893-1899`), "1-3 of 12" (`:2083`), Upload Paper (`:1921`) | honest empty state "No paper pipeline data available" (no BE admin papers endpoint) |
| Audit Logs | `MOCK_LOGS` (`:2115-2121`), totals `1,248/12/850` (`:2244-2283`), mock filter emails (`:2330-2331`), Export CSV (`:2218`) | real `GET /api/admin/audit-logs`; totals from fetched rows; filters from fetched data |
| Infra tab | fake services/hostnames, `43ms/99.998%`, snapshot card, bar chart, View All 24 (`:2459-2783`) | real `infrastructureReadiness` (from dashboard API, HealthService-backed) + `GET /api/health/live` + `GET /api/health/ready` |
| Queue tab | mock counts `1248/24/12` (`:2837-2840`), mock `processingList`/`readyList` (`:2858-2866`), mock-fail rows + fake project names/dates (`:2842-2856`) | real `GET /api/admin/documents/extraction-queue` only; show fields the API returns (doc id/title/error), never fabricated |
| Notifications | fake recipient fallbacks (`:3180-3199`), "1,240 users / 42 institutions" (`:3368`), analytics modal 100%/87.4% (`:3548-3564`) | real `broadcast-history` entries; analytics modal removed |
| Collections tab | **LEAVE AS-IS** (local-only CRUD is HIGH #7, deferred) | — |

### Task 3 — Bind queue retry to real endpoint · `AdminDashboard.jsx`
- **Change:** `doRetry` (`:2827-2833`): replace `setTimeout(1200)` + fake toast with `await api.post('/api/documents/${id}/re-extract')` → toast "re-queued" + refetch queue. Confirm failed-row render carries the real document id (BE: `DocumentController:188`).

### Task 4 — Remove backup/restore theater · `AdminDashboard.jsx`
- **Change:** delete `handleCreateBackup` (`:4173-4183`) and `handleRestoreBackup` (`:4185-4196`) + their buttons + "Last Snapshot / Encrypted / SYSTEM READY" card (`:4286, :4383-4389`). Keep `exportEnvFile` (`:4198-4211`) — it is real.
- **Rationale (from audit §4.2):** app-level restores = accident vector (truncate-before-validate), FK-consistency risk, no writer coordination, credential surface, false assurance. DR belongs at infra level (MySQL snapshots / cloud PITR).

### Verification (after Tasks 1–4)
1. `cd FE && npm run build` — must pass.
2. Grep sweep: `Password@123`, `MOCK_`, `mock-fail`, `Backup Successful`, `re-queued and completed` → no hits in `FE/src`.
3. Manual smoke (dev + BE): Admin page shows only DB-backed numbers; empty DB → empty states; Retry triggers real re-extract (FAILED → QUEUED).

---

## Phase 2 — HIGH items (PENDING)
- [ ] **#5** Unarchive: BE guard `requireRole(ADMIN)` → `requireProjectManageAccess` (`ProjectServiceImpl:198`); bind admin FE button to `PATCH /api/projects/{id}/unarchive` (`AdminDashboard.jsx:1133`)
- [ ] **#6** DLX/DLQ for all 3 queues (`RabbitMQConfig`) + retry-exhaustion logging (`DocumentExtractionWorkerImpl` markFailed path) + `GET /api/admin/queue-metrics` + RabbitMQ console link (env `RABBITMQ_MANAGEMENT_URL`)
- [ ] **#7** Collections create/delete → real `POST/DELETE /api/admin/collections` (+ BE endpoints if missing) (`:3636-3660`); source-category CRUD → BE persistence (`:4152-4171`)

## Phase 3 — MEDIUM/LOW items (PENDING)
- [ ] **#8** Audit tab: wire server-side filters (BE supports actorId/entityType/entityId) + CSV export
- [ ] **#9** Auth rate limiter (in-memory per-IP) + ban history; impersonation endpoint (JWT `IMP` claim + audit); admin alert STOMP topic
- [ ] **#10** Dead buttons cleanup; `Profile.jsx` dead block removal

---

## Decisions ledger
| Decision | Resolution |
|---|---|
| Empty-state policy | **Honest** — "No data" / 0 with clear note; no fabricated numbers, ever |
| Backup/restore | **Removed from app** — DR at infra level (MySQL snapshots / cloud PITR) |
| Marketing pages (Testimonials/Preview) | Static copy — **accepted**, no change |
| Collections local-only CRUD | **Deferred** to HIGH #7 |
| Backend changes in Phase 1 | **None** — FE only |

---

## Progress tracker
| Task | Status |
|---|---|
| Phase 0 — Audit | DONE |
| Task 1 — Login backdoor | DONE |
| Task 2 — AdminDashboard purge | DONE |
| Task 3 — Queue retry binding | DONE |
| Task 4 — Backup/restore removal | DONE |
| Verification | DONE (build passes, grep clean) |
