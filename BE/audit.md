Here is the complete audit.

---

## Audit: Evidence Pilot Integration Layer

### 1. DOI Requirement — **FAIL**

**`Document.java` — missing `doi` field.**  
The entity at `src/main/java/com/evidencepilot/model/Document.java` (107 lines) has no `doi` field. The system stores papers and sources that may have DOIs assigned by a publisher, and the Python extraction worker likely needs to persist it after metadata extraction.

**Required fix — Add field to entity:**

Add after line 75 (`private boolean active = true;`):

```java
    @Column(name = "doi")
    private String doi;
```

**Required fix — Flyway migration `src/main/resources/db/migration/V2__add_document_doi.sql`:**

Add a new directory `src/main/resources/db/migration/` with this single file:

```sql
ALTER TABLE documents
    ADD COLUMN doi VARCHAR(255) NULL AFTER active;
```

This is a safe additive migration — it will not break existing rows because `doi` is nullable. With `flyway.baseline-on-migrate: true` and `baseline-version: 1` in `application.yml`, Flyway treats the existing schema as version 1 and applies V2 on first deploy.

---

### 2. Publishing Pipeline — **FAIL (two issues)**

#### 2a. `uploadDocument` never transitions project status

**`DocumentServiceImpl.java:188-248`** — The `uploadDocument` method calls `refreshProjectStatus` **nowhere**.  
**`DocumentServiceImpl.java:287-288`** — `refreshProjectStatus` is only called inside `deleteDocument`.

During upload, once both a `PAPER` and a `SOURCE` exist for the project, the project should transition from `ASSIGNED` → `IN_PROGRESS`. Without this call the status remains stuck at `ASSIGNED` forever.

**Required fix — Add `refreshProjectStatus` after the MinIO upload succeeds:**

In `DocumentServiceImpl.java`, after line 245 (`document = documentPersistenceService.markDocumentAsUploaded(...);`), add:

```java
        if (project != null) {
            refreshProjectStatus(project);
        }
```

This ensures the project status is reevaluated after every document upload, and matches the existing behaviour on delete.

#### 2b. RabbitMQ payload is too thin for the Python worker

**`SourceExtractionServiceImpl.java:36`** — The message published is:

```java
rabbitTemplate.convertAndSend(RabbitMQConfig.EXTRACTION_QUEUE, documentId.toString());
```

The payload is a bare UUID string. A cloud Python worker cannot run the extraction without:
- The **S3 object key** (to download the file from Cloudflare R2)
- The **user ID** (for audit/logging)

The document entity is loaded at line 30-31 but only its `id` is used.

**Required fix — Create a rich DTO and publish JSON:**

Create `src/main/java/com/evidencepilot/dto/ExtractionRequest.java`:

```java
package com.evidencepilot.dto;

import java.util.UUID;

public record ExtractionRequest(
    UUID documentId,
    String s3ObjectKey,
    UUID userId
) {}
```

Then update `SourceExtractionServiceImpl.triggerExtraction()` to use it:

```java
    @Override
    @Transactional
    public void triggerExtraction(UUID documentId) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException(documentId, "Document"));

        doc.setProcessingStatus(ProcessingStatus.PROCESSING);
        documentRepository.save(doc);

        ExtractionRequest payload = new ExtractionRequest(
                doc.getId(),
                doc.getFileUrl(),
                doc.getUploadedBy().getId()
        );

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXTRACTION_QUEUE, payload);
        log.info("Published extraction request for document {} to extraction.queue", documentId);
    }
```

Because `Jackson2JsonMessageConverter` is already registered as a bean (`RabbitMQConfig.java:61-63`), the record will be serialised to JSON automatically. The Python worker will receive a structured JSON payload:

```json
{"documentId":"...","s3ObjectKey":"sources/raw/...","userId":"..."}
```

---

### 3. Cloud Readiness — **FAIL (two hardcoded `localhost` fallbacks)**

| Line | Property | Fallback | Risk |
|------|----------|----------|------|
| `application.yml:10` | `DB_HOST` | `localhost` | If `DB_HOST` is unset in Railway, the app connects to `localhost:3306` instead of the managed MySQL — silent routing to nowhere |
| `application.yml:124` | `MINIO_URL` | `http://minio:9000` | If `MINIO_URL` is unset, the app tries the Docker-internal MinIO hostname, which does not exist on Railway — file uploads fail |

**Required fixes:**

- **Line 10**: Remove both fallbacks:
  ```yaml
    url: jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
    username: ${DB_USERNAME}
  ```
- **Line 124**: Remove the fallback:
  ```yaml
    url: ${MINIO_URL}
  ```

These three values (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `MINIO_URL`) **must** be provided as Railway environment variables or the application will not start — which is the correct cloud posture (fail fast, not silently route to localhost).

---

### Summary

| # | Severity | Location | Issue |
|---|----------|----------|-------|
| 1 | HIGH | `Document.java` | Missing `doi` field |
| 2 | HIGH | `DocumentServiceImpl.java:188-248` | `uploadDocument` never calls `refreshProjectStatus` — project status stuck at `ASSIGNED` |
| 3 | HIGH | `SourceExtractionServiceImpl.java:36` | RabbitMQ payload is bare UUID — Python worker cannot function |
| 4 | MEDIUM | `application.yml:10` | `DB_HOST:localhost` fallback masks missing env var in cloud |
| 5 | MEDIUM | `application.yml:124` | `MINIO_URL:http://minio:9000` fallback points to Docker-internal hostname |