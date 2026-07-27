# Báo cáo thực thi Walkthrough v4 — Main, Java và Python

Ngày thực thi: 2026-07-20
Ngôn ngữ: Tiếng Việt
Kết luận: **PASS**

## 1. Phạm vi và phiên bản được kiểm thử

| Thuộc tính | Giá trị |
|---|---|
| Backend worktree | `C:\Users\HoangAnhDo\.codex\worktrees\ca9d\EvidencePilot` |
| Baseline commit | `5ff26a4f8221e2a74a00719de37110f0ea7412a6` |
| Baseline subject | `fix: align document extraction request contract` |
| Trạng thái Git | Branch `dev-v4`; diff được kiểm thử trên baseline nêu trên |
| Python repo | `E:\Code\SEP490\EvidencePilot_models-main-test` |
| Python commit | `a3f606a` trên `main`; repo sạch sau test |
| Runtime | Docker Compose, backend image rebuild từ worktree hiện tại |
| Env runtime | File `.env` hiện có của backend; không ghi secret vào báo cáo |
| Data prefix | `Walkthrough v4 20260720-132854` |

Phạm vi thay đổi gồm 7 file production và 8 file test Java. Không sửa repo Python, API path hoặc request shape. Thư mục `output/` bị exclude cục bộ; bốn artifact được thêm tường minh vào PR theo yêu cầu publish.

## 2. Kết quả tổng hợp

| Gate | Kết quả |
|---|---|
| Targeted Java tests theo từng lỗi | PASS |
| Full Java suite | `61` suites, `270/270` tests, `0` failures, `0` errors, `0` skipped |
| Python suite | `21/21` tests pass trong `0.54s` |
| Backend image rebuild | PASS |
| Main API/lifecycle | `40/40` bước pass |
| Auxiliary, negative authorization và polling | PASS |
| Upload format matrix M1–M8 | `8/8` HTTP contract checks pass |
| Fix acceptance P0/P1/P2/E1 | PASS |
| Final readiness | `200`, `UP` |

Số Java test trong bảng là số thực tế tổng hợp sau khi chạy, không phải một giá trị hardcode dùng làm prerequisite.

## 3. Thay đổi đã triển khai

### 3.1 Instructor approval

`ProjectServiceImpl.completeProject()` giờ yêu cầu role `INSTRUCTOR`/`ADMIN` và project access thay vì đi qua mutation guard dùng chung. Vì vậy instructor member có thể chuyển `SUBMITTED_FOR_REVIEW` sang `APPROVED`. Các mutation khác vẫn dùng guard cũ, nên sửa project ở trạng thái khóa tiếp tục trả `409`.

### 3.2 Suggestion strength scoring và accepted mapping

`EvidenceScoringService.computeScore` nhận `EvidenceRelation`, `DocumentChunk`, references và `linkReachable`. Candidate được generate dùng `SUPPORTS`, references rỗng và `linkReachable=false`; response có `strengthScore`, `strengthBand`, `rubricVersion` và `scoreBreakdown` JSON. Trường `score` vẫn giữ Qdrant similarity.

Khi accept suggestion, mapping sao chép `relation`, strength score/band và breakdown. Traceability của lần chạy ghi nhận đúng 1 match với `SUPPORTS`, confidence/strength `45`.

### 3.3 Paper sections sau extraction

HTTP upload paper không còn detect section đồng bộ. Worker lưu extraction, upsert Qdrant, detect/persist sections, rồi mới đánh dấu `READY`. Nếu paper đã có sections, retry trả lại dữ liệu hiện có thay vì insert trùng.

## 4. Bằng chứng TDD và automated tests

### 4.1 Red tests trước khi sửa

| Nhóm | Bằng chứng lỗi tái hiện |
|---|---|
| Lifecycle | Integration nhận `409` thay vì `200`; unit test cho thấy `requireRole` không được gọi trong approval path |
| Scoring | Generated suggestion có `relation=null`; accepted mapping có `relation=null` và thiếu strength metadata |
| Sections | Controller gọi detect đồng bộ; processing không kiểm tra sections đã tồn tại; worker chưa có dependency detect section |

### 4.2 Test files

- Lifecycle: `ProjectServiceImplLifecycleTest`, `ProjectWriteAccessIntegrationTest`.
- Scoring: `EvidenceScoringServiceTest`, `ClaimMatchingServiceImplTest`, `ClaimServiceImplAccessTest`.
- Sections: `PaperControllerTest`, `DocumentExtractionWorkerTest`, `PaperProcessingServiceImplTest`.

Targeted tests của từng nhóm đều pass sau fix. Lệnh full gate cuối:

```powershell
cd BE
mvn test -q
```

Kết quả Surefire: `61` suites, `270` tests, không có failure/error/skipped.

Python được chạy từ virtualenv tạm ngoài repo:

```text
21 passed in 0.54s
```

`git status --short` của repo model không thay đổi trước/sau test.

## 5. Kết quả HTTP main — 40 bước

| ID | Endpoint | Mong đợi | Thực tế |
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
| I14 | `PATCH /api/projects/{projectId}/complete` | `200`, `APPROVED` | `200`, `APPROVED`; instructor token, không fallback |
| I15 | `PUT /api/projects/{projectId}` | `409` | `409` |

## 6. Auxiliary và fix-specific HTTP results

| ID | Endpoint / thao tác | Mong đợi | Thực tế |
|---|---|---|---|
| P1-7-DOWN | Dừng Qdrant; `GET /api/health/ready` | `503`, `DOWN` | `503`, `DOWN` |
| P1-7-UP | Khởi động Qdrant; `GET /api/health/ready` | `200`, `UP` | `200`, `UP` |
| P0-2 | `GET /api/projects/{projectId}/documents` khi project active | `200` | `200` |
| S3-R | `GET /api/documents/{paperId}` | `200`, `READY` | `200`, `READY` |
| M1-R | Poll project-scoped PDF | `200`, `READY` | `200`, `READY` |
| S12-R | Reject 7 suggestion còn lại bằng `?status=REJECTED` | Mỗi request `204` | `7/7` trả `204` |
| P0-3-S | Student `PATCH /api/claims/mappings/{mappingId}/review` | `403` | `403` |
| F1 | `GET /api/health/ready` cuối run | `200`, `UP` | `200`, `UP` |

S11 xác minh từng generated suggestion có provenance, `relation=SUPPORTS`, strength score/band, rubric `1.0`, breakdown JSON parse được và Qdrant similarity vẫn nằm ở `score`. Accepted suggestion có `score=0.740785`, `strengthScore=45`, `strengthBand=MEDIUM`. Sau `1` accepted và `7` rejected, S17/I10 trả đúng `1` traceability match.

## 7. Upload format matrix

| ID | File / Content-Type | HTTP mong đợi | HTTP thực tế | Async thực tế |
|---|---|---|---|---|
| M1 | PDF / `application/pdf` | `202` | `202` | `READY` |
| M2 | DOCX / OOXML | `202` | `202` | `FAILED` |
| M3 | TeX / `application/x-tex` | `202` | `202` | `FAILED` |
| M4 | TeX / `text/plain` | `202` | `202` | `FAILED` |
| M5 | TeX / `application/octet-stream` | `202` | `202` | `FAILED` |
| M6 | Markdown / `text/markdown` | `202` | `202` | `FAILED` |
| M7 | DOC / `application/msword` | `415` | `415` | Không queue |
| M8 | PDF > 50 MiB | `413` | `413` | Không queue |

M2–M6 là **HTTP upload contract PASS** nhưng **async extraction không được hỗ trợ** bởi worker hiện tại. Không diễn giải `202` thành extraction pass.

## 8. P2-1 — Flyway và schema tạm

Acceptance được điều chỉnh đúng với cấu hình hiện tại: Flyway bị vô hiệu hóa, không chạy migration mới. Bảng legacy trên persistent DB được phép tồn tại nhưng phải giữ nguyên.

| Kiểm tra | Kết quả |
|---|---|
| Fingerprint persistent trước test | `1 | 8 | BASELINE | 1 | 1784477275` |
| Schema tạm | `evidencepilot_v4_tmp_ca9d_20260720` |
| Backend temp | Cùng image v4; `SPRING_FLYWAY_ENABLED=false`; consumer listener tắt |
| Hibernate tables trong schema tạm | `20` |
| `flyway_schema_history` trong schema tạm | `0` bảng |
| Cleanup | Dừng temp backend và chỉ drop schema tạm |
| Fingerprint persistent sau test | `1 | 8 | BASELINE | 1 | 1784477275` — không đổi |

Không xóa hoặc thay đổi `flyway_schema_history` trên persistent DB.

## 9. Entity IDs của acceptance run

| Entity | ID |
|---|---|
| Source category | `b12b3e69-72cd-4ae3-ae73-5f2d5a27ba94` |
| Project | `dea4bddc-2dbd-4741-ba0d-35ada92964f6` |
| Collection | `33d0e927-e3fd-48d6-88d1-e8896782798f` |
| Collection TeX source | `991e0260-faa7-4607-ae09-84cf82641e36` |
| Collection PDF source | `025db73a-260a-4052-b27f-f82bf1a49996` |
| Paper | `f25d7e75-79cd-4c5c-b0a4-4a1c893e3eec` |
| Section đã sửa | `85d466d1-44dc-4ec0-802a-77106ed24c8d` |
| Claim | `10cbdf24-be0d-4ced-befa-f07ca35c6c72` |
| Accepted suggestion | `440111f3-1654-488e-b1e7-3ed00eb2141d` |
| Accepted mapping | `2e221688-2420-4ad3-90ec-962c8756e567` |
| Feedback request lần đầu | `f449689a-01f8-49bb-b89b-099729e96c2b` |
| Resubmission request | `a34bb7ae-dc2f-4f82-b19e-0218e0fbb562` |

Các ID này không phải credential và được ghi để tái kiểm tra dữ liệu. JWT, password và secret không được lưu.

## 10. Known limitations

1. Worker AI hiện tại chỉ extract PDF. DOCX/TEX/Markdown được backend nhận với `202`, sau đó chuyển `FAILED` ở async processing.
2. Source vector tạo ở collection không được re-scope theo project sau share. Acceptance run dùng M1 PDF upload trực tiếp vào project cho matching; collection/share vẫn pass về access contract.
3. DOI ingest chỉ được xác minh ở HTTP queue contract `202`; không chờ external PDF hoàn tất.
4. Hỗ trợ non-PDF trong `EvidencePilot_models` nằm ngoài phạm vi v4 và repo đó không bị sửa.

## 11. Trạng thái cuối

- Project acceptance run ở `APPROVED` nhờ instructor I14, không dùng admin fallback.
- Paper ở `READY`, có sections trước S5.
- Traceability có đúng một match từ accepted suggestion; rejected suggestions không xuất hiện.
- Backend, DB, MinIO, RabbitMQ và Qdrant đang chạy; readiness cuối `UP`.
- Branch `dev-v4` được chuẩn bị để review; chưa merge vào `main`.
