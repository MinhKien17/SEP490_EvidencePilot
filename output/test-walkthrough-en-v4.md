# EvidencePilot — API Test Walkthrough v4 (English)

Release date: 2026-07-20
Baseline: `5ff26a4f8221e2a74a00719de37110f0ea7412a6` (`fix: align document extraction request contract`)
Scope: the current Java backend, the existing PDF AI worker, and HTTP upload-contract tests for non-PDF formats.

## 1. Goals and rules

This walkthrough verifies the three fixes delivered in v4:

1. An instructor or admin with project access can approve a submitted project; all other mutations remain locked in `SUBMITTED_FOR_REVIEW` and `APPROVED`.
2. Every generated suggestion includes its relation, evidence-strength scoring, and traceable breakdown; an accepted mapping preserves those values.
3. Paper sections are detected only after extraction and Qdrant succeed, before the document becomes `READY`; a retry does not create duplicate sections.

Never write JWTs, passwords, or secrets to logs or reports. Load the three test accounts from the existing environment and keep tokens only in process-local test variables.

## 2. Preparation

### 2.1 Automated gates

Run the Java suite from `BE/`:

```powershell
mvn test -q
```

Do not use a hardcoded test count as the pass condition. After each run, aggregate the actual suite/test totals from `BE/target/surefire-reports` and record them in the execution report.

Run the Python suite in a temporary virtual environment outside the model repository:

```powershell
py -3.12 -m venv "$env:TEMP\evidencepilot-models-v4"
& "$env:TEMP\evidencepilot-models-v4\Scripts\python.exe" -m pip install -r "E:\Code\SEP490\EvidencePilot_models-main-test\requirements-dev.txt"
Push-Location "E:\Code\SEP490\EvidencePilot_models-main-test"
& "$env:TEMP\evidencepilot-models-v4\Scripts\python.exe" -m pytest -q
Pop-Location
```

The Python repository must remain clean after testing; this walkthrough does not modify the model repository.

### 2.2 Rebuild the current backend

```powershell
Push-Location BE
docker compose --env-file "E:\Code\SEP490\EvidencePilot\BE\.env" build backend
docker compose --env-file "E:\Code\SEP490\EvidencePilot\BE\.env" up -d --no-deps --force-recreate backend
Pop-Location
```

Wait until `GET /api/health/ready` returns `200` with `status=UP`. Use a unique data prefix such as `Walkthrough v4 <timestamp>`.

### 2.3 Test files

- A valid paper PDF with `Abstract`, `Introduction`, `Method`, and `Results` headings so the worker can produce sections.
- One source PDF uploaded directly to the project, giving its Qdrant vector project scope for matching.
- One source PDF uploaded to a collection and then shared to the project to test the access contract.
- `.docx`, three `.tex` variants, `.md`, `.doc`, and a PDF larger than 50 MiB for the format matrix.

Current limitation: sharing a collection source to a project does not re-scope its Qdrant vector to the project. M1 therefore uses a PDF uploaded directly to the project for suggestion matching; I6–I8 still verify the collection/share flow.

## 3. Main procedure

Each `{...}` placeholder is an ID obtained from an earlier response.

### 3.1 Admin and health

| ID | Action | Endpoint | Expected | Actual on 2026-07-20 |
|---|---|---|---|---|
| A1 | Log in as admin | `POST /api/auth/login` | `200`, role=`ADMIN` | `200`, role=`ADMIN` |
| A2 | Check liveness | `GET /api/health/live` | `200`, `UP` | `200`, `UP` |
| A3 | Check readiness | `GET /api/health/ready` | `200`, `UP` | `200`, `UP` |
| A4 | Check aggregate health | `GET /api/health` | `200` | `200` |
| A5 | List source categories | `GET /api/admin/source-categories` | `200` | `200` |
| A6 | Create a v4-prefixed source category | `POST /api/admin/source-categories` | `201` | `201` |

Verify readiness recovery separately:

| ID | Action | Endpoint | Expected | Actual on 2026-07-20 |
|---|---|---|---|---|
| P1-7-DOWN | Stop Qdrant and call readiness | `GET /api/health/ready` | `503`, `DOWN` | `503`, `DOWN` |
| P1-7-UP | Start Qdrant and wait for recovery | `GET /api/health/ready` | `200`, `UP` | `200`, `UP` |

### 3.2 Instructor creates the project and collection

| ID | Action | Endpoint | Expected | Actual on 2026-07-20 |
|---|---|---|---|---|
| I1 | Log in as instructor | `POST /api/auth/login` | `200`, role=`INSTRUCTOR` | `200`, role=`INSTRUCTOR` |
| I2 | Create a project with `targetStandard=IEEE` | `POST /api/projects` | `201`, status=`ASSIGNED` | `201`, status=`ASSIGNED` |
| I3 | Add the student as editor | `POST /api/projects/{projectId}/members?userId={studentId}&role=EDITOR` | `201` | `201` |
| I4 | Create a collection | `POST /api/collections` | `201` | `201` |
| I5 | Upload `.tex` to the collection | `POST /api/sources?collectionId={collectionId}` | `201` — HTTP contract accepted | `201`; async `FAILED` because the worker is PDF-only |
| I6 | Upload a PDF to the collection | `POST /api/sources?collectionId={collectionId}` | `201`, then `READY` | `201`, then `READY` |
| I7 | Share the PDF source to the project | `POST /api/collections/{collectionId}/sources/{sourceId}/share-to-project/{projectId}` | `200` | `200` |
| I8 | List project sources | `GET /api/projects/{projectId}/sources` | `200`, shared source present | `200`, `sources=1` |

| ID | Action | Endpoint | Expected | Actual on 2026-07-20 |
|---|---|---|---|---|
| P0-2 | Instructor reads documents while the project is active | `GET /api/projects/{projectId}/documents` | `200` | `200` |

### 3.3 Student uploads the paper, creates a claim, and processes suggestions

| ID | Action | Endpoint | Expected | Actual on 2026-07-20 |
|---|---|---|---|---|
| S1 | Log in as student | `POST /api/auth/login` | `200`, role=`STUDENT` | `200`, role=`STUDENT` |
| S2 | List projects | `GET /api/projects` | `200`, v4 project visible | `200`, v4 project visible |
| S3 | Upload the paper PDF | `POST /api/papers?projectId={projectId}` | `201`, `docType=PAPER` | `201`, `docType=PAPER` |
| S3-R | Poll paper status | `GET /api/documents/{paperId}` | `200`, eventually `READY` | `200`, `READY` |
| S4 | List sections before editing | `GET /api/papers/{paperId}/sections` | `200`, at least 1 section | `200`, `sections=4` |
| S5 | Update the first section | `PUT /api/papers/{paperId}/sections/{sectionId}` | `200` | `200` |
| S6 | Validate the paper | `GET /api/papers/{paperId}/validate` | `200` | `200` |
| S7 | Ingest a source by DOI | `POST /api/documents/ingest/doi` | `202` — queue contract only | `202` |
| S8 | Create a claim linked to the section | `POST /api/claims` | `201` | `201` |
| S9 | Update the claim | `PUT /api/claims/{claimId}` | `200`, version increments | `200`, version=`2` |
| M1 | Upload a source PDF directly to the project | `POST /api/documents?projectId={projectId}` | `202`, then `READY` | `202`, then `READY` |
| S10 | Generate suggestions | `POST /api/claims/{claimId}/suggestions/generate` | `201`, non-empty list | `201`, `suggestions=8` |
| S11 | Fetch and validate every suggestion | `GET /api/claims/{claimId}/suggestions` | `200`; every item satisfies scoring/provenance contract | `200`; `8/8` satisfy it |
| S12 | Accept exactly one suggestion | `PATCH /api/claims/suggestions/{suggestionId}/status?status=ACCEPTED` | `204` | `204` |
| S12-R | Reject every remaining suggestion | `PATCH /api/claims/suggestions/{suggestionId}/status?status=REJECTED` | Each request `204` | `7/7` requests returned `204` |
| S13 | Accept the already accepted suggestion again | `PATCH /api/claims/suggestions/{suggestionId}/status?status=ACCEPTED` | `204`, idempotent | `204`, idempotent |
| S14 | List mappings | `GET /api/claims/{claimId}/mappings` | `200`, exactly 1 mapping | `200`, `mappings=1` |
| S15 | Student reads the shared source | `GET /api/sources/{sharedSourceId}` | `200` | `200` |

At S11, verify all of the following for every generated suggestion:

- `modelName`, `modelVersion`, `promptVersion`, `rubricVersion`, and `evaluatedAt` are populated.
- `relation=SUPPORTS`.
- `strengthScore` is a valid integer and `strengthBand` is one of `LOW|MEDIUM|HIGH`.
- `scoreBreakdown` parses as JSON and contains the required rubric components.
- `score` remains a numeric Qdrant similarity; it is not replaced by the strength score.

The accepted suggestion in this run had Qdrant `score=0.740785`, `relation=SUPPORTS`, `strengthScore=45`, `strengthBand=MEDIUM`, and `rubricVersion=1.0`.

### 3.4 Submit, review, return, resubmit, and instructor approval

| ID | Action | Endpoint | Expected | Actual on 2026-07-20 |
|---|---|---|---|---|
| S16 | Student submits the project | `POST /api/projects/{projectId}/reviews` | `201`, request=`PENDING` | `201`, request=`PENDING` |
| S17 | Fetch traceability after rejecting the rest | `GET /api/projects/{projectId}/traceability` | `200`, exactly 1 match | `200`, `matches=1` |
| I9 | Instructor lists project documents | `GET /api/projects/{projectId}/documents` | `200` | `200` |
| I10 | Instructor fetches traceability | `GET /api/projects/{projectId}/traceability` | `200`, exactly 1 match | `200`, `matches=1` |
| P0-3-S | Student attempts to review the mapping | `PATCH /api/claims/mappings/{mappingId}/review` | `403` | `403` |
| I11 | Instructor reviews the mapping | `PATCH /api/claims/mappings/{mappingId}/review` | `200` | `200` |
| I12 | Instructor sends feedback | `POST /api/feedback-requests/{requestId}/feedback` | `200` | `200` |
| I13 | Instructor returns the project | `PATCH /api/feedback-requests/{requestId}/status?status=RETURNED` | `200`, project=`RETURNED` | `200`, project=`RETURNED` |
| S18 | Student updates the claim | `PUT /api/claims/{claimId}` | `200` | `200` |
| S19 | Student resubmits | `POST /api/projects/{projectId}/reviews` | `201`, request=`PENDING` | `201`, request=`PENDING` |
| I14 | Instructor approves without an admin fallback | `PATCH /api/projects/{projectId}/complete` | `200`, project=`APPROVED` | `200`, project=`APPROVED` |
| I15 | Student attempts to edit the locked project | `PUT /api/projects/{projectId}` | `409` | `409` |
| F1 | Final readiness | `GET /api/health/ready` | `200`, `UP` | `200`, `UP` |

At S17/I10, the single match must preserve `relation=SUPPORTS`, confidence/strength=`45`, and must not be marked weak. The required lifecycle is submit → return → resubmit → instructor approve → `APPROVED`; do not log in as admin to substitute for I14.

## 4. Upload format matrix

Every request uses `POST /api/documents?projectId={projectId}` unless stated otherwise. HTTP acceptance does not mean that the worker extracted the file successfully.

| ID | File / Content-Type | Expected HTTP | Actual HTTP on 2026-07-20 | Current async status |
|---|---|---|---|---|
| M1 | `.pdf` / `application/pdf` | `202` | `202` | `READY` |
| M2 | `.docx` / `application/vnd.openxmlformats-officedocument.wordprocessingml.document` | `202` | `202` | `FAILED` — PDF-only worker |
| M3 | `.tex` / `application/x-tex` | `202` | `202` | `FAILED` — PDF-only worker |
| M4 | `.tex` / `text/plain` | `202` | `202` | `FAILED` — PDF-only worker |
| M5 | `.tex` / `application/octet-stream` | `202` | `202` | `FAILED` — PDF-only worker |
| M6 | `.md` / `text/markdown` | `202` | `202` | `FAILED` — PDF-only worker |
| M7 | `.doc` / `application/msword` | `415` | `415` | Not queued |
| M8 | PDF larger than 50 MiB | `413` | `413` | Not queued |

## 5. Acceptance criteria by fix

| ID | Criterion | Verification | Result on 2026-07-20 |
|---|---|---|---|
| P0-1 | Extraction request preserves the Python contract | PDF reaches `READY`; no field-contract error | PASS |
| P0-2 | Instructor project access is not blocked by the mutation lock | `GET /api/projects/{projectId}/documents` returns `200` | PASS |
| P0-3 | Only an instructor can review a mapping | Student gets `403`; instructor gets `200` | PASS |
| P1-4 | Student can read a shared source | S15 returns `200` | PASS |
| P1-5 | Suggestions include provenance and strength scoring | S11 validates `8/8` items | PASS |
| P1-6 | Traceability excludes REJECTED suggestions | After 1 accepted + 7 rejected, S17 has exactly 1 match | PASS |
| P1-7 | Readiness reflects dependency outage | Qdrant down=`503`, recovered=`200` | PASS |
| P1-8 | Upload format/size contract | M1–M8 return the expected statuses | PASS |
| P1-9 | Return/resubmit/instructor approval | S16 → I13 → S19 → I14, no admin fallback | PASS |
| P2-1 | Flyway is disabled and no new migration runs | Temporary MySQL schema has app tables but no `flyway_schema_history`; only the temporary schema is dropped; persistent legacy table is unchanged | PASS |
| E1 | A locked project returns conflict | I15 returns `409`, not `403` | PASS |

### P2-1 — safe procedure

1. Fingerprint `flyway_schema_history` on the persistent database before testing; do not delete or alter it.
2. Create a uniquely named temporary MySQL schema.
3. Start the same v4 backend image against the temporary schema with `SPRING_FLYWAY_ENABLED=false`, Hibernate `ddl-auto=update`, and RabbitMQ consumers disabled for the schema test.
4. Confirm that application tables are created and `flyway_schema_history` does not exist in the temporary schema.
5. Stop the temporary backend and drop only the temporary schema.
6. Confirm that the persistent database fingerprint is unchanged.

This criterion permits a legacy table to remain in an existing database, but it must not change. Never delete `flyway_schema_history` from the persistent database.

## 6. Reference execution record

- Prefix: `Walkthrough v4 20260720-132854`.
- Main lifecycle/API: `40/40` steps passed; all auxiliary polling, negative authorization, readiness recovery, and matrix checks also passed.
- Java: the actual run produced `61` suites, `270/270` tests, and `0` failures/errors/skipped.
- Python: `21/21` tests passed in a temporary virtual environment; the model repository remained clean.
- Paper: `READY`, with `4` sections detected before S5 and no fallback section.
- Suggestions: `8` generated, `1` accepted, `7` rejected, and traceability returned `1` match.
- Final lifecycle: instructor step I14 returned `200`, project=`APPROVED`; F1 readiness was `200 UP`.

## 7. Known limitations

- The AI worker used by v4 extracts PDF only. A `202` for DOCX/TEX/Markdown verifies the upload contract, not successful extraction.
- Sharing a collection source to a project does not currently re-scope its vector to the project in Qdrant. Use the M1 project-scoped PDF for matching until that behavior is fixed separately.
- S7 verifies only that DOI ingestion is accepted and queued with `202`; the walkthrough does not wait for an external DOI document to finish processing.
- DOCX/TEX/Markdown extraction support in the model repository is outside the v4 scope.
