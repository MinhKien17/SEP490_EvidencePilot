# Walkthrough v4 Execution Report — Main, Java, and Python

Execution date: 2026-07-20
Language: English
Conclusion: **PASS**

## 1. Tested scope and version

| Property | Value |
|---|---|
| Backend worktree | `C:\Users\HoangAnhDo\.codex\worktrees\ca9d\EvidencePilot` |
| Baseline commit | `5ff26a4f8221e2a74a00719de37110f0ea7412a6` |
| Baseline subject | `fix: align document extraction request contract` |
| Git state | Branch `dev-v4`; diff tested on the baseline above |
| Python repository | `E:\Code\SEP490\EvidencePilot_models-main-test` |
| Python commit | `a3f606a` on `main`; repository clean after testing |
| Runtime | Docker Compose, backend image rebuilt from the current worktree |
| Runtime environment | Existing backend `.env`; no secrets recorded in this report |
| Data prefix | `Walkthrough v4 20260720-132854` |

The change scope contains 7 production files and 8 Java test files. The Python repository, API paths, and request shapes were not changed. The `output/` directory is locally excluded; the four requested artifacts are added explicitly to this PR.

## 2. Result summary

| Gate | Result |
|---|---|
| Targeted Java tests for each defect | PASS |
| Full Java suite | `61` suites, `270/270` tests, `0` failures, `0` errors, `0` skipped |
| Python suite | `21/21` tests passed in `0.54s` |
| Backend image rebuild | PASS |
| Main API/lifecycle | `40/40` steps passed |
| Auxiliary, negative authorization, and polling | PASS |
| Upload format matrix M1–M8 | `8/8` HTTP contract checks passed |
| Fix acceptance P0/P1/P2/E1 | PASS |
| Final readiness | `200`, `UP` |

The Java count shown here is the actual total aggregated after execution, not a hardcoded prerequisite value.

## 3. Implemented changes

### 3.1 Instructor approval

`ProjectServiceImpl.completeProject()` now requires the `INSTRUCTOR`/`ADMIN` role and project access instead of passing through the shared mutation guard. An instructor member can therefore move `SUBMITTED_FOR_REVIEW` to `APPROVED`. Other mutations still use the existing guard, so editing a locked project continues to return `409`.

### 3.2 Suggestion strength scoring and accepted mapping

`EvidenceScoringService.computeScore` now accepts `EvidenceRelation`, `DocumentChunk`, references, and `linkReachable`. Generated candidates use `SUPPORTS`, empty references, and `linkReachable=false`; the response includes `strengthScore`, `strengthBand`, `rubricVersion`, and a JSON `scoreBreakdown`. The `score` field continues to hold Qdrant similarity.

When a suggestion is accepted, the mapping copies its `relation`, strength score/band, and breakdown. Traceability in the acceptance run returned exactly one match with `SUPPORTS` and confidence/strength `45`.

### 3.3 Paper sections after extraction

The paper upload HTTP path no longer detects sections synchronously. The worker stores extraction, upserts Qdrant, detects/persists sections, and only then marks the document `READY`. If the paper already has sections, a retry returns the existing records instead of inserting duplicates.

## 4. TDD evidence and automated tests

### 4.1 Red tests before each fix

| Group | Reproduced failure evidence |
|---|---|
| Lifecycle | Integration returned `409` instead of `200`; the unit test showed `requireRole` was not invoked in the approval path |
| Scoring | Generated suggestion had `relation=null`; accepted mapping had `relation=null` and lacked strength metadata |
| Sections | Controller invoked detection synchronously; processing did not check existing sections; worker lacked the section-detection dependency |

### 4.2 Test files

- Lifecycle: `ProjectServiceImplLifecycleTest`, `ProjectWriteAccessIntegrationTest`.
- Scoring: `EvidenceScoringServiceTest`, `ClaimMatchingServiceImplTest`, `ClaimServiceImplAccessTest`.
- Sections: `PaperControllerTest`, `DocumentExtractionWorkerTest`, `PaperProcessingServiceImplTest`.

All targeted tests passed after the fixes. Final full-gate command:

```powershell
cd BE
mvn test -q
```

Surefire result: `61` suites and `270` tests, with no failures, errors, or skipped tests.

Python ran in a temporary virtual environment outside its repository:

```text
21 passed in 0.54s
```

`git status --short` for the model repository was unchanged before and after the test.

## 5. Main HTTP results — 40 steps

| ID | Endpoint | Expected | Actual |
|---|---|---|---|
| A1 | `POST /api/auth/login` | `200`, `ADMIN` | `200`, `ADMIN` |
| A2 | `GET /api/health/live` | `200`, `UP` | `200`, `UP` |
| A3 | `GET /api/health/ready` | `200`, `UP` | `200`, `UP` |
| A4 | `GET /api/health` | `200` | `200` |
| A5 | `GET /api/admin/source-categories` | `200` | `200` |
| A6 | `POST /api/admin/source-categories` | `201` | `201` |
| I1 | `POST /api/auth/login` | `200`, `INSTRUCTOR` | `200`, `INSTRUCTOR` |
| I2 | `POST /api/projects` | `201`, `ASSIGNED` | `201`, `ASSIGNED` |
| I3 | `POST /api/projects/{projectId}/members?userId={studentId}&role=EDITOR` | `201` | `201` |
| I4 | `POST /api/collections` | `201` | `201` |
| I5 | `POST /api/sources?collectionId={collectionId}` | `201` | `201` |
| I6 | `POST /api/sources?collectionId={collectionId}` | `201` | `201`; poll `READY` |
| I7 | `POST /api/collections/{collectionId}/sources/{sourceId}/share-to-project/{projectId}` | `200` | `200` |
| I8 | `GET /api/projects/{projectId}/sources` | `200` | `200`, `sources=1` |
| S1 | `POST /api/auth/login` | `200`, `STUDENT` | `200`, `STUDENT` |
| S2 | `GET /api/projects` | `200` | `200`, project visible |
| S3 | `POST /api/papers?projectId={projectId}` | `201`, `PAPER` | `201`, `PAPER` |
| S4 | `GET /api/papers/{paperId}/sections` | `200`, `>=1` section | `200`, `4` sections |
| S5 | `PUT /api/papers/{paperId}/sections/{sectionId}` | `200` | `200` |
| S6 | `GET /api/papers/{paperId}/validate` | `200` | `200` |
| S7 | `POST /api/documents/ingest/doi` | `202` | `202` |
| S8 | `POST /api/claims` | `201` | `201` |
| S9 | `PUT /api/claims/{claimId}` | `200` | `200`, version=`2` |
| S10 | `POST /api/claims/{claimId}/suggestions/generate` | `201`, non-empty | `201`, `8` suggestions |
| S11 | `GET /api/claims/{claimId}/suggestions` | `200`, all valid | `200`, `8/8` valid |
| S12 | `PATCH /api/claims/suggestions/{suggestionId}/status?status=ACCEPTED` | `204` | `204` |
| S13 | `PATCH /api/claims/suggestions/{suggestionId}/status?status=ACCEPTED` | `204`, idempotent | `204`, idempotent |
| S14 | `GET /api/claims/{claimId}/mappings` | `200`, exactly 1 | `200`, exactly 1 |
| S15 | `GET /api/sources/{sharedSourceId}` | `200` | `200` |
| S16 | `POST /api/projects/{projectId}/reviews` | `201`, `PENDING` | `201`, `PENDING` |
| S17 | `GET /api/projects/{projectId}/traceability` | `200`, exactly 1 match | `200`, exactly 1 match |
| I9 | `GET /api/projects/{projectId}/documents` | `200` | `200` |
| I10 | `GET /api/projects/{projectId}/traceability` | `200`, exactly 1 match | `200`, exactly 1 match |
| I11 | `PATCH /api/claims/mappings/{mappingId}/review` | `200` | `200` |
| I12 | `POST /api/feedback-requests/{requestId}/feedback` | `200` | `200` |
| I13 | `PATCH /api/feedback-requests/{requestId}/status?status=RETURNED` | `200`, `RETURNED` | `200`, `RETURNED` |
| S18 | `PUT /api/claims/{claimId}` | `200` | `200` |
| S19 | `POST /api/projects/{projectId}/reviews` | `201`, `PENDING` | `201`, `PENDING` |
| I14 | `PATCH /api/projects/{projectId}/complete` | `200`, `APPROVED` | `200`, `APPROVED`; instructor token, no fallback |
| I15 | `PUT /api/projects/{projectId}` | `409` | `409` |

## 6. Auxiliary and fix-specific HTTP results

| ID | Endpoint / action | Expected | Actual |
|---|---|---|---|
| P1-7-DOWN | Stop Qdrant; `GET /api/health/ready` | `503`, `DOWN` | `503`, `DOWN` |
| P1-7-UP | Start Qdrant; `GET /api/health/ready` | `200`, `UP` | `200`, `UP` |
| P0-2 | `GET /api/projects/{projectId}/documents` while active | `200` | `200` |
| S3-R | `GET /api/documents/{paperId}` | `200`, `READY` | `200`, `READY` |
| M1-R | Poll project-scoped PDF | `200`, `READY` | `200`, `READY` |
| S12-R | Reject the remaining 7 suggestions with `?status=REJECTED` | Each request `204` | `7/7` returned `204` |
| P0-3-S | Student `PATCH /api/claims/mappings/{mappingId}/review` | `403` | `403` |
| F1 | Final `GET /api/health/ready` | `200`, `UP` | `200`, `UP` |

S11 verified that every generated suggestion had provenance, `relation=SUPPORTS`, a strength score/band, rubric `1.0`, a parseable JSON breakdown, and Qdrant similarity preserved in `score`. The accepted suggestion had `score=0.740785`, `strengthScore=45`, and `strengthBand=MEDIUM`. After `1` accepted and `7` rejected, S17/I10 returned exactly `1` traceability match.

## 7. Upload format matrix

| ID | File / Content-Type | Expected HTTP | Actual HTTP | Actual async status |
|---|---|---|---|---|
| M1 | PDF / `application/pdf` | `202` | `202` | `READY` |
| M2 | DOCX / OOXML | `202` | `202` | `FAILED` |
| M3 | TeX / `application/x-tex` | `202` | `202` | `FAILED` |
| M4 | TeX / `text/plain` | `202` | `202` | `FAILED` |
| M5 | TeX / `application/octet-stream` | `202` | `202` | `FAILED` |
| M6 | Markdown / `text/markdown` | `202` | `202` | `FAILED` |
| M7 | DOC / `application/msword` | `415` | `415` | Not queued |
| M8 | PDF > 50 MiB | `413` | `413` | Not queued |

M2–M6 are **HTTP upload contract PASS** but **asynchronous extraction is unsupported** by the current worker. A `202` is not reported as an extraction pass.

## 8. P2-1 — Flyway and temporary schema

Acceptance reflects the current configuration: Flyway is disabled and no new migration runs. A legacy table may remain on the persistent database, but it must remain unchanged.

| Check | Result |
|---|---|
| Persistent fingerprint before test | `1 | 8 | BASELINE | 1 | 1784477275` |
| Temporary schema | `evidencepilot_v4_tmp_ca9d_20260720` |
| Temporary backend | Same v4 image; `SPRING_FLYWAY_ENABLED=false`; message listener disabled |
| Hibernate tables in temporary schema | `20` |
| `flyway_schema_history` in temporary schema | `0` tables |
| Cleanup | Stopped temporary backend and dropped only the temporary schema |
| Persistent fingerprint after test | `1 | 8 | BASELINE | 1 | 1784477275` — unchanged |

The persistent database's `flyway_schema_history` was neither deleted nor altered.

## 9. Entity IDs from the acceptance run

| Entity | ID |
|---|---|
| Source category | `b12b3e69-72cd-4ae3-ae73-5f2d5a27ba94` |
| Project | `dea4bddc-2dbd-4741-ba0d-35ada92964f6` |
| Collection | `33d0e927-e3fd-48d6-88d1-e8896782798f` |
| Collection TeX source | `991e0260-faa7-4607-ae09-84cf82641e36` |
| Collection PDF source | `025db73a-260a-4052-b27f-f82bf1a49996` |
| Paper | `f25d7e75-79cd-4c5c-b0a4-4a1c893e3eec` |
| Edited section | `85d466d1-44dc-4ec0-802a-77106ed24c8d` |
| Claim | `10cbdf24-be0d-4ced-befa-f07ca35c6c72` |
| Accepted suggestion | `440111f3-1654-488e-b1e7-3ed00eb2141d` |
| Accepted mapping | `2e221688-2420-4ad3-90ec-962c8756e567` |
| Initial feedback request | `f449689a-01f8-49bb-b89b-099729e96c2b` |
| Resubmission request | `a34bb7ae-dc2f-4f82-b19e-0218e0fbb562` |

These IDs are not credentials and are recorded for data re-verification. JWTs, passwords, and secrets are not stored.

## 10. Known limitations

1. The current AI worker extracts PDF only. DOCX/TEX/Markdown are accepted by the backend with `202` and then move to `FAILED` during asynchronous processing.
2. A vector created for a collection source is not re-scoped to the project after sharing. The acceptance run used the M1 PDF uploaded directly to the project for matching; the collection/share access contract still passed.
3. DOI ingestion was verified only at the `202` queue contract; the run did not wait for the external PDF to finish.
4. Non-PDF support in `EvidencePilot_models` is outside v4 scope, and that repository was not modified.

## 11. Final state

- The acceptance-run project is `APPROVED` through instructor step I14, with no admin fallback.
- The paper is `READY`, with sections present before S5.
- Traceability has exactly one match from the accepted suggestion; rejected suggestions are absent.
- Backend, DB, MinIO, RabbitMQ, and Qdrant are running; final readiness is `UP`.
- Branch `dev-v4` is prepared for review and has not been merged into `main`.
