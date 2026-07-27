# EvidencePilot — Hướng dẫn kiểm thử API v4 (Tiếng Việt)

Ngày phát hành: 2026-07-20
Baseline: `5ff26a4f8221e2a74a00719de37110f0ea7412a6` (`fix: align document extraction request contract`)
Phạm vi: backend Java hiện tại, worker AI PDF hiện có, kiểm thử HTTP contract cho các định dạng không phải PDF và bao phủ toàn bộ route backend hiện hành.

## 1. Mục tiêu và nguyên tắc

Walkthrough này xác minh ba lỗi được sửa trong v4:

1. Giảng viên hoặc admin có quyền truy cập dự án có thể phê duyệt dự án đã nộp; các mutation khác vẫn bị khóa ở `SUBMITTED_FOR_REVIEW` và `APPROVED`.
2. Mọi generated suggestion có relation, evidence-strength scoring và breakdown có thể truy vết; mapping được chấp nhận giữ lại dữ liệu đó.
3. Paper sections chỉ được phát hiện sau khi extraction và Qdrant hoàn tất, trước khi tài liệu chuyển sang `READY`; retry không tạo section trùng.

Không ghi JWT, mật khẩu hoặc secret vào log hay báo cáo. Lấy ba tài khoản kiểm thử từ env hiện có và chỉ lưu token trong biến tạm của tiến trình kiểm thử.

## 2. Chuẩn bị

### 2.1 Automated gates

Chạy Java suite từ `BE/`:

```powershell
mvn test -q
```

Không dùng một số test hardcode làm điều kiện pass. Sau mỗi lần chạy, tổng hợp số suite/test thực tế từ `BE/target/surefire-reports` và ghi vào báo cáo thực thi.

Chạy Python suite trong virtualenv tạm ngoài repo model:

```powershell
py -3.12 -m venv "$env:TEMP\evidencepilot-models-v4"
& "$env:TEMP\evidencepilot-models-v4\Scripts\python.exe" -m pip install -r "E:\Code\SEP490\EvidencePilot_models-main-test\requirements-dev.txt"
Push-Location "E:\Code\SEP490\EvidencePilot_models-main-test"
& "$env:TEMP\evidencepilot-models-v4\Scripts\python.exe" -m pytest -q
Pop-Location
```

Repo Python phải vẫn sạch sau kiểm thử; walkthrough này không sửa repo model.

### 2.2 Rebuild backend hiện tại

```powershell
Push-Location BE
docker compose --env-file "E:\Code\SEP490\EvidencePilot\BE\.env" build backend
docker compose --env-file "E:\Code\SEP490\EvidencePilot\BE\.env" up -d --no-deps --force-recreate backend
Pop-Location
```

Chờ `GET /api/health/ready` trả `200` và `status=UP` trước khi bắt đầu. Dùng prefix dữ liệu duy nhất, ví dụ `Walkthrough v4 <timestamp>`.

### 2.3 Dữ liệu file

- Dùng đúng `C:\Users\HoangAnhDo\Downloads\Documents\v67i01.pdf` cho mọi upload PDF hợp lệ. File có 48 trang, A4, không mã hóa và có các heading `Abstract`, `Introduction` cùng nhiều section đánh số để worker tạo section.
- Một PDF nguồn upload trực tiếp vào project để Qdrant vector có project scope và dùng cho matching.
- Một PDF nguồn upload vào collection rồi share sang project để kiểm tra access contract.
- `.docx`, ba biến thể `.tex`, `.md`, `.doc` và file PDF lớn hơn 50 MiB cho ma trận định dạng.

Lưu ý hiện tại: nguồn được upload vào collection rồi share sang project chưa được re-scope vector trong Qdrant. Vì vậy M1 dùng PDF upload trực tiếp vào project cho suggestion matching; I6–I8 vẫn kiểm tra luồng collection/share.

## 3. Quy trình chính

Các placeholder `{...}` là ID lấy từ response của bước trước.

### 3.1 Admin và health

| ID | Hành động | Endpoint | Mong đợi | Kết quả thực tế 2026-07-20 |
|---|---|---|---|---|
| A1 | Đăng nhập admin | `POST /api/auth/login` | `200`, role=`ADMIN` | `200`, role=`ADMIN` |
| A2 | Liveness | `GET /api/health/live` | `200`, `UP` | `200`, `UP` |
| A3 | Readiness | `GET /api/health/ready` | `200`, `UP` | `200`, `UP` |
| A4 | Health tổng hợp | `GET /api/health` | `200` | `200` |
| A5 | Xem source categories | `GET /api/admin/source-categories` | `200` | `200` |
| A6 | Tạo source category với prefix v4 | `POST /api/admin/source-categories` | `201` | `201` |

Kiểm tra recovery readiness riêng:

| ID | Hành động | Endpoint | Mong đợi | Kết quả thực tế 2026-07-20 |
|---|---|---|---|---|
| P1-7-DOWN | Dừng Qdrant rồi gọi readiness | `GET /api/health/ready` | `503`, `DOWN` | `503`, `DOWN` |
| P1-7-UP | Khởi động Qdrant, chờ recovery | `GET /api/health/ready` | `200`, `UP` | `200`, `UP` |

### 3.2 Giảng viên tạo project và collection

| ID | Hành động | Endpoint | Mong đợi | Kết quả thực tế 2026-07-20 |
|---|---|---|---|---|
| I1 | Đăng nhập giảng viên | `POST /api/auth/login` | `200`, role=`INSTRUCTOR` | `200`, role=`INSTRUCTOR` |
| I2 | Tạo project `targetStandard=IEEE` | `POST /api/projects` | `201`, status=`ASSIGNED` | `201`, status=`ASSIGNED` |
| I3 | Thêm sinh viên làm editor | `POST /api/projects/{projectId}/members?userId={studentId}&role=EDITOR` | `201` | `201` |
| I4 | Tạo collection | `POST /api/collections` | `201` | `201` |
| I5 | Upload `.tex` vào collection | `POST /api/sources?collectionId={collectionId}` | `201` — HTTP contract chấp nhận | `201`; async `FAILED` do worker chỉ extract PDF |
| I6 | Upload PDF vào collection | `POST /api/sources?collectionId={collectionId}` | `201`, sau đó `READY` | `201`, sau đó `READY` |
| I7 | Share PDF source sang project | `POST /api/collections/{collectionId}/sources/{sourceId}/share-to-project/{projectId}` | `200` | `200` |
| I8 | Xem nguồn của project | `GET /api/projects/{projectId}/sources` | `200`, có nguồn đã share | `200`, `sources=1` |

| ID | Hành động | Endpoint | Mong đợi | Kết quả thực tế 2026-07-20 |
|---|---|---|---|---|
| P0-2 | Giảng viên đọc documents khi project đang hoạt động | `GET /api/projects/{projectId}/documents` | `200` | `200` |

### 3.3 Sinh viên upload paper, tạo claim và xử lý suggestions

| ID | Hành động | Endpoint | Mong đợi | Kết quả thực tế 2026-07-20 |
|---|---|---|---|---|
| S1 | Đăng nhập sinh viên | `POST /api/auth/login` | `200`, role=`STUDENT` | `200`, role=`STUDENT` |
| S2 | Xem danh sách project | `GET /api/projects` | `200`, thấy project v4 | `200`, thấy project v4 |
| S3 | Upload paper PDF | `POST /api/papers?projectId={projectId}` | `201`, `docType=PAPER` | `201`, `docType=PAPER` |
| S3-R | Poll trạng thái paper | `GET /api/documents/{paperId}` | `200`, cuối cùng `READY` | `200`, `READY` |
| S4 | Lấy sections trước khi sửa | `GET /api/papers/{paperId}/sections` | `200`, có ít nhất 1 section | `200`, `sections=4` |
| S5 | Cập nhật section đầu tiên | `PUT /api/papers/{paperId}/sections/{sectionId}` | `200` | `200` |
| S6 | Validate paper | `GET /api/papers/{paperId}/validate` | `200` | `200` |
| S7 | Ingest nguồn qua DOI | `POST /api/documents/ingest/doi` | `202` — chỉ xác minh queue contract | `202` |
| S8 | Tạo claim gắn với section | `POST /api/claims` | `201` | `201` |
| S9 | Cập nhật claim | `PUT /api/claims/{claimId}` | `200`, version tăng | `200`, version=`2` |
| M1 | Upload PDF nguồn trực tiếp vào project | `POST /api/documents?projectId={projectId}` | `202`, sau đó `READY` | `202`, sau đó `READY` |
| S10 | Generate suggestions | `POST /api/claims/{claimId}/suggestions/generate` | `201`, danh sách không rỗng | `201`, `suggestions=8` |
| S11 | Lấy và kiểm tra toàn bộ suggestions | `GET /api/claims/{claimId}/suggestions` | `200`; mọi item đạt contract scoring/provenance | `200`; `8/8` đạt |
| S12 | Chấp nhận đúng một suggestion | `PATCH /api/claims/suggestions/{suggestionId}/status?status=ACCEPTED` | `204` | `204` |
| S12-R | Từ chối toàn bộ suggestion còn lại | `PATCH /api/claims/suggestions/{suggestionId}/status?status=REJECTED` | Mỗi request `204` | `7/7` request trả `204` |
| S13 | Chấp nhận lại suggestion đã accepted | `PATCH /api/claims/suggestions/{suggestionId}/status?status=ACCEPTED` | `204`, idempotent | `204`, idempotent |
| S14 | Xem mappings | `GET /api/claims/{claimId}/mappings` | `200`, đúng 1 mapping | `200`, `mappings=1` |
| S15 | Sinh viên đọc source đã share | `GET /api/sources/{sharedSourceId}` | `200` | `200` |

Ở S11, với từng generated suggestion, bắt buộc kiểm tra:

- `modelName`, `modelVersion`, `promptVersion`, `rubricVersion` và `evaluatedAt` có giá trị.
- `relation=SUPPORTS`.
- `strengthScore` là số nguyên hợp lệ và `strengthBand` thuộc `LOW|MEDIUM|HIGH`.
- `scoreBreakdown` parse được thành JSON và có các thành phần rubric cần thiết.
- `score` vẫn là Qdrant similarity dạng số, không bị thay bằng strength score.

Kết quả accepted suggestion trong lần chạy này: Qdrant `score=0.740785`, `relation=SUPPORTS`, `strengthScore=45`, `strengthBand=MEDIUM`, `rubricVersion=1.0`.

### 3.4 Submit, review, return, resubmit và instructor approval

| ID | Hành động | Endpoint | Mong đợi | Kết quả thực tế 2026-07-20 |
|---|---|---|---|---|
| S16 | Sinh viên submit project | `POST /api/projects/{projectId}/reviews` | `201`, request=`PENDING` | `201`, request=`PENDING` |
| S17 | Lấy traceability sau khi reject phần còn lại | `GET /api/projects/{projectId}/traceability` | `200`, đúng 1 match | `200`, `matches=1` |
| I9 | Giảng viên xem project documents | `GET /api/projects/{projectId}/documents` | `200` | `200` |
| I10 | Giảng viên xem traceability | `GET /api/projects/{projectId}/traceability` | `200`, đúng 1 match | `200`, `matches=1` |
| P0-3-S | Sinh viên thử review mapping | `PATCH /api/claims/mappings/{mappingId}/review` | `403` | `403` |
| I11 | Giảng viên review mapping | `PATCH /api/claims/mappings/{mappingId}/review` | `200` | `200` |
| I12 | Giảng viên gửi feedback | `POST /api/feedback-requests/{requestId}/feedback` | `200` | `200` |
| I13 | Giảng viên trả project về | `PATCH /api/feedback-requests/{requestId}/status?status=RETURNED` | `200`, project=`RETURNED` | `200`, project=`RETURNED` |
| S18 | Sinh viên cập nhật claim | `PUT /api/claims/{claimId}` | `200` | `200` |
| S19 | Sinh viên resubmit | `POST /api/projects/{projectId}/reviews` | `201`, request=`PENDING` | `201`, request=`PENDING` |
| I14 | Giảng viên phê duyệt, không dùng admin fallback | `PATCH /api/projects/{projectId}/complete` | `200`, project=`APPROVED` | `200`, project=`APPROVED` |
| I15 | Sinh viên thử sửa project đã khóa | `PUT /api/projects/{projectId}` | `409` | `409` |
| F1 | Readiness cuối | `GET /api/health/ready` | `200`, `UP` | `200`, `UP` |

Ở S17/I10, match duy nhất phải giữ `relation=SUPPORTS`, confidence/strength=`45` và không bị đánh dấu weak. Luồng lifecycle bắt buộc là submit → return → resubmit → instructor approve → `APPROVED`; không đăng nhập admin để thay cho I14.

### 3.5 Bổ sung bao phủ route còn thiếu

Inventory runtime từ OpenAPI và đối chiếu controller có `78` handler method, tương ứng `79` route template vì `RagController.matchClaim` có hai alias. Bản v4 trước phần bổ sung này nêu `35/79` route; bảng dưới thêm `44` route còn thiếu. Route phá hủy chỉ dùng entity có prefix riêng của lần chạy. Download hợp lệ còn được worker gọi gián tiếp khi PDF đi tới `READY`; request trực tiếp dùng token sai để kiểm tra biên bảo mật.

| ID | Hành động | Endpoint | Mong đợi |
|---|---|---|---|
| R01 | Đọc collection vừa tạo | `GET /api/collections/{collectionId}` | `200` |
| R02 | Liệt kê source trong collection | `GET /api/collections/{collectionId}/sources` | `200` |
| R03 | Xóa collection tạm đã rỗng | `DELETE /api/collections/{collectionId}` | `204` |
| R04 | Gửi đăng ký thiếu field | `POST /api/auth/register` | `400` |
| R05 | Xác minh email bằng token sai | `GET /api/auth/verify-email` | `400` |
| R06 | Đọc notification của user hiện tại | `GET /api/notifications` | `200` |
| R07 | Đếm notification chưa đọc | `GET /api/notifications/unread-count` | `200` |
| R08 | Đánh dấu notification trả về là đã đọc; nếu inbox rỗng dùng UUID ngẫu nhiên | `PATCH /api/notifications/{notificationId}/read` | `200`, hoặc `404` với fallback không tồn tại |
| R09 | Đọc source category active | `GET /api/source-categories` | `200` |
| R10 | Sửa source category tạm | `PUT /api/admin/source-categories/{categoryId}` | `200` |
| R11 | Xóa source category tạm sau cleanup | `DELETE /api/admin/source-categories/{categoryId}` | `204` |
| R12 | Match claim qua alias paper | `POST /api/paper/{paperId}/claims/match` | `200` |
| R13 | Match claim qua alias source | `POST /api/sources/{sourceId}/claims/match` | `200` |
| R14 | Đọc project theo ID | `GET /api/projects/{projectId}` | `200` |
| R15 | Archive project sau approval | `PATCH /api/projects/{projectId}/archive` | `200` |
| R16 | Soft-delete project cleanup | `DELETE /api/projects/{cleanupProjectId}` | `204` |
| R17 | Liệt kê member của project | `GET /api/projects/{projectId}/members` | `200` |
| R18 | Liệt kê claim của project | `GET /api/projects/{projectId}/claims` | `200` |
| R19 | Liệt kê collection của project | `GET /api/projects/{projectId}/collections` | `200` |
| R20 | Xóa member khỏi project cleanup | `DELETE /api/projects/{cleanupProjectId}/members/{studentId}` | `204` |
| R21 | Liệt kê claim theo scope user | `GET /api/claims` | `200` |
| R22 | Đọc claim theo ID | `GET /api/claims/{claimId}` | `200` |
| R23 | Soft-delete claim cleanup | `DELETE /api/claims/{cleanupClaimId}` | `204` |
| R24 | Tạo suggestion thủ công từ chunk | `POST /api/claims/{claimId}/suggestions` | `201` |
| R25 | Đọc chunk qua source controller | `GET /api/sources/{sourceId}/chunks` | `200` |
| R26 | Đọc extracted text qua source controller | `GET /api/sources/{sourceId}/text` | `200` |
| R27 | Bỏ share source khỏi project | `DELETE /api/sources/projects/{projectId}/sources/{sourceId}` | `204` |
| R28 | Soft-delete source cleanup | `DELETE /api/sources/{sourceId}` | `204` |
| R29 | Admin đọc user theo ID | `GET /api/users/{studentId}` | `200` |
| R30 | User đọc profile của mình | `GET /api/users/profile` | `200` |
| R31 | User ghi lại profile hợp lệ | `PUT /api/users/profile` | `200` |
| R32 | Lookup DOI `10.18637/jss.v067.i01` | `POST /api/documents/lookup` | `200` |
| R33 | Attach `v67i01.pdf` vào document DOI; nếu OpenAlex đã attach PDF thì xác minh conflict | `POST /api/documents/{documentId}/file` | `200` khi `METADATA_FETCHED`, nếu không `400`; UUID không tồn tại phải `404` |
| R34 | Đọc chunk qua document controller | `GET /api/documents/{documentId}/chunks` | `200` |
| R35 | Đọc extracted text qua document controller | `GET /api/documents/{documentId}/text` | `200` |
| R36 | Gọi download với token sai; worker đồng thời chứng minh token đúng qua extraction | `GET /api/documents/{documentId}/download` | trực tiếp `403`; PDF vẫn tới `READY` |
| R37 | Soft-delete document cleanup | `DELETE /api/documents/{documentId}` | `204` |
| R38 | Liệt kê feedback request theo user | `GET /api/feedback-requests` | `200` |
| R39 | Liệt kê paper theo user | `GET /api/papers` | `200` |
| R40 | Đọc paper theo ID | `GET /api/papers/{paperId}` | `200` |
| R41 | Liệt kê paper của project | `GET /api/projects/{projectId}/papers` | `200` |
| R42 | Tạo section bổ sung | `POST /api/papers/{paperId}/sections/create` | `201` |
| R43 | Chạy AI paper review | `POST /api/papers/{paperId}/reviews` | `200` |
| R44 | Soft-delete paper cleanup | `DELETE /api/papers/{cleanupPaperId}` | `204` |

Sau khi chạy, canonicalize tên placeholder UUID (`{id}`, `{projectId}`, ...) rồi so tập route thực thi với `79` operation trong `/v3/api-docs`. Chỉ đạt route-completeness khi không còn route thiếu hoặc route ngoài inventory.

## 4. Ma trận định dạng upload

Tất cả request dùng `POST /api/documents?projectId={projectId}`, trừ khi mô tả khác. HTTP accepted không đồng nghĩa worker đã extract thành công.

| ID | File / Content-Type | Mong đợi HTTP | Kết quả HTTP 2026-07-20 | Trạng thái async hiện tại |
|---|---|---|---|---|
| M1 | `.pdf` / `application/pdf` | `202` | `202` | `READY` |
| M2 | `.docx` / `application/vnd.openxmlformats-officedocument.wordprocessingml.document` | `202` | `202` | `FAILED` — worker chỉ PDF |
| M3 | `.tex` / `application/x-tex` | `202` | `202` | `FAILED` — worker chỉ PDF |
| M4 | `.tex` / `text/plain` | `202` | `202` | `FAILED` — worker chỉ PDF |
| M5 | `.tex` / `application/octet-stream` | `202` | `202` | `FAILED` — worker chỉ PDF |
| M6 | `.md` / `text/markdown` | `202` | `202` | `FAILED` — worker chỉ PDF |
| M7 | `.doc` / `application/msword` | `415` | `415` | Không queue |
| M8 | PDF lớn hơn 50 MiB | `413` | `413` | Không queue |

## 5. Tiêu chí chấp nhận theo fix

| ID | Tiêu chí | Cách xác minh | Kết quả 2026-07-20 |
|---|---|---|---|
| P0-1 | Extraction request giữ đủ contract Python | PDF đi tới `READY`; không có lỗi field contract | PASS |
| P0-2 | Instructor project access không bị mutation lock chặn | `GET /api/projects/{projectId}/documents` trả `200` | PASS |
| P0-3 | Chỉ instructor review mapping | Sinh viên `403`, giảng viên `200` | PASS |
| P1-4 | Sinh viên đọc source được share | S15 trả `200` | PASS |
| P1-5 | Suggestion provenance và strength scoring đầy đủ | S11 kiểm tra `8/8` item | PASS |
| P1-6 | Traceability loại REJECTED | Sau 1 accepted + 7 rejected, S17 có đúng 1 match | PASS |
| P1-7 | Readiness phản ánh dependency outage | Qdrant down=`503`, recovered=`200` | PASS |
| P1-8 | Upload format/size contract | M1–M8 đúng status mong đợi | PASS |
| P1-9 | Return/resubmit/instructor approve | S16 → I13 → S19 → I14, không admin fallback | PASS |
| P2-1 | Flyway tắt, không chạy migration mới | Schema MySQL tạm có app tables nhưng không có `flyway_schema_history`; chỉ xóa schema tạm; bảng legacy persistent không đổi | PASS |
| E1 | Project khóa trả conflict | I15 trả `409`, không phải `403` | PASS |

### P2-1 — quy trình an toàn

1. Ghi fingerprint `flyway_schema_history` trên persistent DB trước test; không xóa hoặc sửa bảng này.
2. Tạo một schema MySQL tạm có tên duy nhất.
3. Khởi động đúng backend image v4 với schema tạm, `SPRING_FLYWAY_ENABLED=false`, Hibernate `ddl-auto=update` và consumer RabbitMQ tắt cho test schema.
4. Xác minh app tables được tạo và `flyway_schema_history` không tồn tại trong schema tạm.
5. Dừng temp backend, chỉ drop schema tạm.
6. Xác minh fingerprint persistent DB không đổi.

Tiêu chí này cho phép bảng legacy tồn tại trên DB cũ nhưng không được thay đổi; không được xóa `flyway_schema_history` trên persistent DB.

## 6. Bản ghi thực thi tham chiếu

- Prefix: `Walkthrough v4 20260720-132854`.
- Main lifecycle/API: `40/40` bước pass; các poll, quyền âm, readiness recovery và matrix phụ trợ đều pass.
- Java: tổng thực tế sau lần chạy là `61` suites, `270/270` tests, `0` failures/errors/skipped.
- Python: `21/21` tests pass trong virtualenv tạm; repo model vẫn sạch.
- Paper: `READY`, phát hiện `4` sections trước S5; không tạo section fallback.
- Suggestions: `8` generated, `1` accepted, `7` rejected, traceability `1` match.
- Lifecycle cuối: instructor I14 trả `200`, project `APPROVED`; readiness F1 `200 UP`.

## 7. Giới hạn đã biết

- Worker AI được dùng trong v4 chỉ extract PDF. `202` cho DOCX/TEX/Markdown xác minh upload contract, không phải cam kết extraction thành công.
- Collection source được share sang project chưa được re-scope vector theo project trong Qdrant. Dùng M1 project-scoped PDF cho matching cho đến khi hành vi này được sửa riêng.
- S7 chỉ xác minh DOI ingest được nhận và queue với `202`; walkthrough không yêu cầu chờ tài liệu DOI bên ngoài hoàn tất.
- Hỗ trợ extraction DOCX/TEX/Markdown trong repo model nằm ngoài phạm vi v4.
