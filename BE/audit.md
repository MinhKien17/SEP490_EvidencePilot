Here is the comprehensive, strictly factual audit report.

---

# EVIDENCEPILOT BACKEND AUDIT REPORT

## PILLAR 1: SCHEMA & STATE MACHINE

### 1.1 Database Init

**File:** `schema.sql:1`

```sql
--DROP DATABASE IF EXISTS evidence_pilot;
```

The destructive `DROP DATABASE IF EXISTS evidence_pilot` is **present but commented out**. The next line `CREATE DATABASE IF NOT EXISTS evidence_pilot` is active. This means schema migrations are cumulative — the schema only creates tables that don't exist yet and does **not** destroy existing data. Any column added to `schema.sql` after initial deployment would **not** be applied to running databases unless a Flyway migration or manual `ALTER TABLE` is executed.

### 1.2 Bipartite Scoring Tables — Missing `instructor_rejected` Column

**`ai_suggestions` table** (`schema.sql:121-130`) — Status check constraint:
```sql
status VARCHAR(50) NOT NULL CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'INVALIDATED'))
```

**`claim_evidence_mappings` table** (`schema.sql:132-143`) — Status check constraint:
```sql
status VARCHAR(50) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE'))
```

**JPA entity `AiSuggestion.java`**: No `instructorRejected` field exists. The entity only has:
```
id, claim, documentChunk, status (SuggestionStatus enum), score, explanation, claimVersion, createdAt
```

**JPA entity `ClaimEvidenceMapping.java`**: No `instructorRejected` field exists. The entity only has:
```
id, claim, documentChunk, suggestion, createdBy, status (MappingStatus enum), createdAt
```

**Verdict:** Neither the `schema.sql` tables nor the JPA entities contain a boolean `instructor_rejected` column/field. This feature has **not been implemented**.

### 1.3 State Machine — Still Using Old Student-Led Workflow

**`ProjectStatus.java`:**
```java
public enum ProjectStatus {
    DRAFT, ACTIVE, IN_REVIEW, COMPLETED, ARCHIVED
}
```

This is the **old student-led workflow**. The new Instructor-led flow would require values like `ASSIGNED`, `IN_PROGRESS`, `SUBMITTED_FOR_REVIEW`, `RETURNED`, `APPROVED`.

The current transition logic (haphazardly distributed across service code):

| Transition | Trigger | Location |
|---|---|---|
| (new) → `DRAFT` | `createProject()` | `ProjectServiceImpl.java:92` |
| `DRAFT`/`ACTIVE` → `IN_REVIEW` | `submitForReview()` | `FeedbackServiceImpl.java:80` |
| `IN_REVIEW` → `ACTIVE` | `updateStatus()` → `transition()` | `FeedbackServiceImpl.java:131` |

There is **no formal state machine** (no Spring Statemachine, no enum-based transition matrix). Transitions are hardcoded inline. The `RETURNED` and `REVIEWED` values exist in the `FeedbackStatus` enum but are **only applied to the `feedback_requests` table**, not to the `projects.status` column — the `transition()` method always sets the project back to `ACTIVE` regardless of which `FeedbackStatus` is applied.

---

## PILLAR 2: SECURITY & RBAC CONTROLS

### 2.1 Write-Lock Implementation

**`CurrentUserServiceImpl.requireProjectWriteAccess()`** (lines 100-113):
```java
public void requireProjectWriteAccess(User currentUser, Project project) {
    if (isAdmin(currentUser))
        return;
    if (isInstructor(currentUser)) {
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Instructors cannot modify student projects");
    }
    User student = project.getStudent();
    if (student == null || !student.getId().equals(currentUser.getId())) {
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Write access denied to project");
    }
}
```

### 2.2 Critical Gap: No Status-Based Write Block

The method checks **only** role (`ADMIN` → allow, `INSTRUCTOR` → always deny, `STUDENT` → if owner) and **ownership**. It does **not** examine the project's `status` field.

**Consequence:** A `STUDENT` user can execute `POST/PUT/DELETE` against a project even when that project is in the `IN_REVIEW` state. There is no `if (project.getStatus() == IN_REVIEW) throw FORBIDDEN` logic anywhere in the service layer.

Callers that rely on `requireProjectWriteAccess`:
- `ProjectServiceImpl.updateProject()` — lines 108-119
- `ProjectServiceImpl.deleteProject()` — lines 123-130
- `ProjectServiceImpl.addMember()` — lines 146-168
- `ProjectServiceImpl.removeMember()` — lines 170-184
- `ClaimServiceImpl.createClaim()` — lines 136-155
- `ClaimServiceImpl.updateClaim()` — lines 159-172
- `ClaimServiceImpl.deleteClaim()` — lines 175-187
- `ClaimServiceImpl.createSuggestion()` — lines 193-206
- `ClaimServiceImpl.acceptSuggestion()` / `rejectSuggestion()` / `updateSuggestionStatus()`
- `FeedbackServiceImpl.submitForReview()` — line 66

**Specific lines governing the missing block:** `CurrentUserServiceImpl.java:100-113` — no status predicate exists in the method body.

---

## PILLAR 3: INGESTION GATEWAY & DISTRIBUTED TRANSACTIONS

### 3.1 OpenAlex Integration

**Result: ZERO references.** A case-insensitive search across all `.java`, `.yml`, `.yaml`, `.xml`, `.properties` files for `openalex`, `open_alex`, or `OpenAlex` returned no matches. There is no controller, service, HTTP client, or configuration for querying the OpenAlex API. The system ingests documents exclusively via multipart file upload (PDF/DOCX). There is no DOI-based paper fetch mechanism.

### 3.2 Transaction Boundary — MinIO Upload Inside `@Transactional`

**File:** `DocumentServiceImpl.java:166` (the `uploadDocument()` overload with `collectionId`):
```java
@Override
@Transactional  // <-- SINGLE transaction wraps DB + MinIO + MQ
public DocumentResponse uploadDocument(UUID projectId, UUID collectionId, MultipartFile file, DocumentType docType) {
    // ...
    documentRepository.save(document);          // DB write inside TX
    minioClient.putObject(...);                 // MinIO HTTP call inside TX
    sourceExtractionService.triggerExtraction(  // MQ publish inside TX
            document.getId());
    return DocumentResponse.from(document);
}
```

The MinIO object storage call (`minioClient.putObject`) executes **inside** the `@Transactional` block. If the MinIO call fails (network timeout, S3 error, bucket full), the entire transaction rolls back, which is correct — the database state remains consistent. However, if the MinIO call **succeeds** but the subsequent MQ publish or the transaction commit fails, the file is orphaned in MinIO with no corresponding database record. The MinIO upload is **not** safely decoupled from the database transaction.

For comparison, the correct pattern would be: commit the DB transaction first, then upload to MinIO in a separate non-transactional method, or implement a compensating action (garbage collection for orphaned MinIO objects).

### 3.3 Race Condition — MQ Publish Before Transaction Commit

**Execution sequence in the upload path:**

```
uploadDocument (@Transactional)
  ├── 1. documentRepository.save(document)       // writes to DB (uncommitted)
  ├── 2. minioClient.putObject(...)               // uploads to MinIO
  ├── 3. sourceExtractionService.triggerExtraction(  // REQUIRED propagation → JOINs same TX
  │       ├── a. documentRepository.findById(id)     // same persistence context → sees entity
  │       ├── b. documentRepository.save(doc)        // sets status to PROCESSING
  │       └── c. rabbitTemplate.convertAndSend(...)  // MESSAGE PUBLISHED (TX still uncommitted)
  └── 4. OUTER TRANSACTION COMMITS

DocumentExtractionListener (separate thread, new TX)
  └── documentRepository.findById(id)              // UNCOMMITTED → may return empty or stale
```

The race condition exists because `RabbitTemplate.convertAndSend()` is invoked at step 3c while the outer transaction is still **uncommitted**. The listener on a different thread cannot see the uncommitted document data under `READ_COMMITTED` isolation. By the time the listener's DB read executes, the outer transaction may or may not have committed — there is **no guarantee**.

**The `fileUrl` assignment vs publish sequence is safe:** The `fileUrl` is set on the entity (`document.setFileUrl(objectKey)`) before `triggerExtraction()` is called, and both run in the same transaction. So when the transaction commits, the `fileUrl` is persisted atomically with everything else. The race is between the MQ message delivery and the outer transaction commit — the listener may attempt to read the document before it's visible.

### 3.4 Note on Transaction Manager Heterogeneity

`DocumentServiceImpl` uses `jakarta.transaction.Transactional` (Java EE / Jakarta namespace). Other services (e.g., `FeedbackServiceImpl`, `ProjectServiceImpl`) use `org.springframework.transaction.annotation.Transactional`. While both ultimately drive the same Spring `PlatformTransactionManager`, this inconsistency should be normalized.

---

## SUMMARY OF DEFICIENCIES

| # | Severity | Component | Issue |
|---|----------|-----------|-------|
| 1 | **HIGH** | `ProjectStatus` enum | Still uses old student-led states (`DRAFT, ACTIVE, IN_REVIEW, COMPLETED, ARCHIVED`). Instructor-led flow not implemented. |
| 2 | **HIGH** | `CurrentUserServiceImpl.requireProjectWriteAccess()` | Lines 100-113 — No status-based write-lock. Students can mutate projects in `IN_REVIEW` state. |
| 3 | **HIGH** | `schema.sql` / JPA entities | `ai_suggestions` and `claim_evidence_mappings` missing `instructor_rejected` boolean column in both DDL and entities. |
| 4 | **MEDIUM** | `DocumentServiceImpl.uploadDocument()` | MinIO upload executes inside `@Transactional`. A failed commit after successful MinIO upload orphans the file. |
| 5 | **MEDIUM** | `DocumentServiceImpl` → `triggerExtraction()` → `DocumentExtractionListener` | MQ message published before transaction commits. Listener may read stale/unavailable data. |
| 6 | **LOW** | Transaction annotation mix | `DocumentServiceImpl` uses `jakarta.transaction.Transactional`; other services use `org.springframework.transaction.annotation.Transactional`. |
| 7 | **INFO** | OpenAlex integration | **Not implemented** — no code exists for DOI/URL-based paper fetching. |