# Audit: Evidence Pilot Integration Layer

*Last updated: 2026-07-11*

---

## Previously Flagged Issues

### 1. DOI Requirement — **STILL OPEN**

**`Document.java` — missing `doi` field.**
The entity at `src/main/java/com/evidencepilot/model/Document.java` (107 lines) still has no `doi` field.

**Fix:** Add after line 76 (`private boolean active = true;`):
```java
    @Column(name = "doi")
    private String doi;
```
And create `src/main/resources/db/migration/V2__add_document_doi.sql`:
```sql
ALTER TABLE documents
    ADD COLUMN doi VARCHAR(255) NULL AFTER active;
```

---

### 2. Publishing Pipeline — **STILL OPEN (two issues)**

#### 2a. `uploadDocument` never transitions project status

**`DocumentServiceImpl.java:192-254`** — `uploadDocument` still never calls `refreshProjectStatus`. The method exists at line 328 and is called in `deleteDocument` (line 294), but not after upload. The `DocumentUploadedEvent` listener only triggers extraction — no project status refresh.

**Fix:** After `markDocumentAsUploaded(...)` (line 248), add:
```java
        if (project != null) {
            refreshProjectStatus(project);
        }
```

#### 2b. RabbitMQ payload is bare UUID string

**`SourceExtractionServiceImpl.java:36`** — Payload is still `documentId.toString()`. The document entity is loaded but only its ID is used. `ExtractionRequest.java` does not exist.

**Fix:** Create `src/main/java/com/evidencepilot/dto/ExtractionRequest.java`:
```java
package com.evidencepilot.dto;

import java.util.UUID;

public record ExtractionRequest(
    UUID documentId,
    String s3ObjectKey,
    UUID userId
) {}
```

Then update `SourceExtractionServiceImpl.triggerExtraction()`:
```java
    ExtractionRequest payload = new ExtractionRequest(
            doc.getId(), doc.getFileUrl(), doc.getUploadedBy().getId());
    rabbitTemplate.convertAndSend(RabbitMQConfig.EXTRACTION_QUEUE, payload);
```

---

### 3. Cloud Readiness

#### 3a. DB_HOST/DB_PORT/DB_NAME/DB_USERNAME fallbacks — **STILL OPEN**

| Line | Expression | Issue |
|------|-----------|-------|
| `application.yml:10` | `${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:evidence_pilot}` | Fallbacks mask missing env vars |
| `application.yml:11` | `${DB_USERNAME:root}` | Same |

**Fix:** Strip fallbacks — make them required env vars:
```yaml
url: jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
username: ${DB_USERNAME}
```

#### 3b. MINIO_URL fallback — **FIXED**

`application.yml:136` — `${MINIO_URL:http://minio:9000}` → `${MINIO_URL}`. Fallback correctly removed.

---

## New Findings (since previous audit)

### N1. `CurrentUserServiceImpl.requireProjectAccess` — **FIXED**

`CurrentUserServiceImpl.java:81-99` — Previously had a duplicate instructor block (lines 84-89 and 94-97 were identical) with no student-member check. Now correctly:
1. Admin → full access
2. Instructor → access only if reviewer for `SUBMITTED_FOR_REVIEW` project
3. Others → checked via `isProjectMember()`

---

### N2. Empty file validation — **FIXED**

`DocumentServiceImpl.java:199-201` — `uploadDocument` now validates:
```java
if (file == null || file.isEmpty()) {
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
}
```
Returns 400 for empty uploads instead of 202.

---

### N3. `.env` completeness — **FIXED**

`BE/.env` — All variables required by `docker-compose.yml` are now present (was missing MinIO, Qdrant, RabbitMQ, Ollama vars). Uses Docker internal service names for URLs (`http://minio:9000`, `http://vector-db:6333`, `rabbitmq`).

---

### N4. MinIO & Qdrant config injection — **PASS (no change needed)**

| File | Lines | Status |
|------|-------|--------|
| `MinioConfig.java` | 11-15 | `@Value("${minio.url}")` — proper injection |
| `QdrantClientImpl.java` | 40-42 | `@Value("${qdrant.url}")`, `@Value("${qdrant.api-key}")` |
| `QdrantGatewayImpl.java` | 23-25 | Same |

No hardcoded credentials.

---

### N5. `application.yml` — entries lacking defaults (for reference)

14 entries have bare `${VAR}` with no default. By design (fail-fast cloud posture), but `.env` covers all for local Docker:

| Line | Key | Expression |
|------|-----|-----------|
| 12 | `datasource.password` | `${DB_PASSWORD}` |
| 56 | `rabbitmq.host` | `${RABBITMQ_HOST}` |
| 58 | `rabbitmq.virtual-host` | `${RABBITMQ_VIRTUAL_HOST}` |
| 59 | `rabbitmq.username` | `${RABBITMQ_USERNAME}` |
| 60 | `rabbitmq.password` | `${RABBITMQ_PASSWORD}` |
| 86 | `jwt.secret` | `${JWT_SECRET}` |
| 112 | `ai.model.base-url` | `${AI_MODEL_BASE_URL}` |
| 120 | `ollama.embedding.model` | `${OLLAMA_EMBEDDING_MODEL}` |
| 122 | `ollama.generation.model` | `${OLLAMA_GENERATION_MODEL}` |
| 126 | `qdrant.url` | `${QDRANT_URL}` |
| 136-139 | `minio.*` | `${MINIO_URL}`, `${MINIO_ACCESS_KEY}`, `${MINIO_SECRET_KEY}`, `${MINIO_BUCKET_NAME}` |

---

## Summary

| # | Severity | Location | Issue | Status |
|---|----------|----------|-------|--------|
| 1 | HIGH | `Document.java` | Missing `doi` field | **OPEN** |
| 2 | HIGH | `DocumentServiceImpl.java:192-254` | `uploadDocument` never calls `refreshProjectStatus` | **OPEN** |
| 3 | HIGH | `SourceExtractionServiceImpl.java:36` | RabbitMQ payload is bare UUID | **OPEN** |
| 4 | MEDIUM | `application.yml:10-11` | DB host/port/name/username fallbacks mask missing vars | **OPEN** |
| 5 | MEDIUM | `application.yml:136` | MINIO_URL fallback removed | **FIXED** |
| N1 | MEDIUM | `CurrentUserServiceImpl.java:81-99` | Duplicate instructor block replaced with `isProjectMember` | **FIXED** |
| N2 | LOW | `DocumentServiceImpl.java:199-201` | Missing empty-file validation | **FIXED** |
| N3 | LOW | `BE/.env` | Incomplete env vars for docker-compose | **FIXED** |
| N4 | — | `MinioConfig.java`, Qdrant configs | Proper injection confirmed | **PASS** |
| N5 | INFO | `application.yml` (14 entries) | Bare placeholders without defaults | **INFO** |
