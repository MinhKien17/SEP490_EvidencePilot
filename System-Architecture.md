# EvidencePilot System Architecture and Data Model

## Conceptual Diagram
demo
```mermaid
flowchart LR
    Student["Student"]
    Instructor["Instructor"]
    Admin["Admin"]

    Project["Project workspace"]
    Paper["Student paper"]
    Source["Evidence source"]
    Claim["Claim"]
    Evidence["Evidence mapping"]
    Feedback["Instructor feedback"]
    Export["Traceability export"]

    Student -->|"creates and manages"| Project
    Student -->|"uploads"| Paper
    Student -->|"uploads or selects"| Source
    Student -->|"writes"| Claim
    Claim -->|"matched against"| Source
    Source -->|"supports"| Evidence
    Evidence -->|"links to"| Claim
    Project --> Paper
    Project --> Source
    Project --> Claim
    Project --> Evidence
    Instructor -->|"reviews"| Project
    Instructor -->|"returns"| Feedback
    Feedback --> Project
    Admin -->|"manages users and catalogs"| Project
    Project -->|"produces"| Export
```

## System Overview

```mermaid
flowchart LR
    subgraph Client Layer
        Client["Client / Frontend"]
    end

    subgraph Core Application
        API["Spring Boot API\nREST + WebSocket"]
        Security["JWT filter + access checks"]
        Services["Application services"]
        Repos["JPA repositories"]
    end

    subgraph Data Layer
        MySQL[("MySQL")]
        MinIO[("MinIO\nraw files + processed Markdown")]
        Qdrant[("Qdrant\nvector index")]
    end

    subgraph Asynchronous Extraction Pipeline
        Rabbit["RabbitMQ\nextraction queue"]
        Worker["Document extraction worker"]
    end

    subgraph External / AI Services
        AI["AI model API\nextraction, embeddings, review"]
    end

    %% Core Flow
    Client --> API
    API --> Security
    Security --> Services
    Services --> Repos

    %% Synchronous Data Access
    Repos --> MySQL
    Services --> MinIO
    
    %% Asynchronous Messaging (Dashed Lines)
    Services -.->|"publish"| Rabbit
    Rabbit -.->|"consume"| Worker
    %% Worker Operations
    Worker -->|"save text, chunks, status"| MySQL
    Worker -->|"presigned URL; extraction/embeddings"| AI
    Worker -->|"processed Markdown checkpoint"| MinIO
    Worker -->|"upsert chunk vectors"| Qdrant

    %% Direct Service Operations
    Services -->|"claim evaluation"| AI
    Services -->|"vector search"| Qdrant
```

Business data is kept in MySQL, raw files and processed Markdown in MinIO, and searchable vectors in Qdrant. RabbitMQ decouples upload from the Java extraction worker. Python remains a stateless HTTP model service: Java sends a presigned MinIO URL for extraction, performs chunking, requests batch embeddings, persists the results, upserts Qdrant, and sets `READY` last.

## Processing Flow

```mermaid
flowchart TD
    subgraph Document Ingestion Pipeline [Automated Background Process]
        Upload["Upload source document"] --> StoreFile["Store raw file (MinIO)"]
        StoreFile --> MarkUploaded["Save to DB: status = UPLOADED"]
        
        MarkUploaded --> PushQueue["Publish job to RabbitMQ"]
        
        PushQueue -->|Ack Success| SaveQueued["Update DB: status = QUEUED"]
        PushQueue -->|Failure or Timeout| FailState["Update DB: status = FAILED"]
        
        SaveQueued --> AIParsing["Worker: Send presigned raw-file URL to AI API"]
        
        AIParsing -->|Returns Markdown| Checkpoint["Store processed Markdown (MinIO)"]
        AIParsing -->|Failure or Timeout| FailState
        Checkpoint --> Chunks["Worker: Chunk Markdown"]
        Chunks --> GenEmbed["Worker: Send chunk batches to AI API"]
        
        GenEmbed -->|Returns Vectors| Persist["Persist text and chunks (MySQL)"]
        GenEmbed -->|Failure or Timeout| FailState
        Persist --> Vector["Worker: Upsert vectors (Qdrant)"]
        Vector --> Ready["Update DB: status = READY"]
    end
```

```mermaid
flowchart TD
    subgraph User Workflow & Evaluation [Interactive Process]
        CreateClaim["Create or update claim"] --> Match["Evaluate claim against Ready documents"]
        Match --> Suggestions["Generate AI suggestions & edges"]
        
        Suggestions --> UserAction{Student Decision}
        UserAction -->|Accepts AI| Mapping["Save automated mapping"]
        UserAction -->|Overrides AI| Mapping["Save manual mapping"]
        
        Mapping --> Feedback["Submit for instructor feedback"]
        
        Feedback --> Export["Generate Traceability Export"]
        Mapping --> Export
    end
```

## Main Workflow Activity Diagrams

### 1.Authentication And Email Verification

```mermaid
flowchart TD
    Register["POST /api/auth/register"] --> Validate["Validate email and password format"]
    Validate --> Duplicate{"Email exists?"}
    Duplicate -->|Yes| Conflict["409 Conflict"]
    Duplicate -->|No| Save["Save user with BCrypt"]
    Save --> Token["Create verification token"]
    Token --> Mail["Send verification email"]
    Mail --> Verify["GET /api/auth/verify-email?token=..."]
    Verify --> TokenOk{"Token valid?"}
    TokenOk -->|No| Reject["400/404 Failure"]
    TokenOk -->|Yes| Active["Set email_verified = true"]
    Active --> Login["POST /api/auth/login"]
    Login --> Jwt["Return JWT with role"]
```

### 2.Workspace & Entity Lifecycle Workflow

```mermaid
flowchart TD
    Create["POST /api/projects"] --> SaveProj["Save project with role=OWNER"]
    SaveProj --> DeleteReq["DELETE /api/projects/{id}"]
    DeleteReq --> MarkDeleted["Soft Delete: projects.deleted_at = NOW"]
    MarkDeleted --> PushCleanup["Publish cleanup job to RabbitMQ"]
    PushCleanup --> Confirm["Return 204 No Content"]
    Confirm --> ConsumeJob["Worker consumes cleanup job"]
    ConsumeJob --> DropMinIO["Delete raw files from MinIO"]
    DropMinIO --> DropQdrant["Delete vectors from Qdrant"]
    DropQdrant --> HardDelete["Hard delete project from MySQL"]
```

### 3.Source Ingestion & Asynchronous Extraction Pipeline

```mermaid
flowchart TD
    Upload["POST /api/sources or /api/papers"] --> StoreFile["Store raw file in MinIO"]
    StoreFile --> MarkUploaded["DB status = UPLOADED"]
    MarkUploaded --> PushQueue["Publish to RabbitMQ"]
    PushQueue -->|Ack Success| SaveQueued["DB status = QUEUED"]
    PushQueue -->|Failure| FailInit["DB status = FAILED"]
    SaveQueued --> ConsumeJob["Worker consumes job"]
    ConsumeJob --> AIParsing["Send presigned URL for Markdown extraction"]
    AIParsing -->|Success| Checkpoint["Store processed Markdown in MinIO"]
    AIParsing -->|Failure| WorkerFail["DB status = FAILED"]
    Checkpoint --> Chunking["Chunk Markdown in Java"]
    Chunking --> GenEmbed["Request batch embeddings"]
    GenEmbed -->|Success| Persist["Persist text and chunks in MySQL"]
    GenEmbed -->|Failure| WorkerFail
    Persist --> VectorDB["Upsert to Qdrant"]
    VectorDB --> Ready["DB status = READY"]
```

### 4.Automated Claim Evaluation Workflow

```mermaid
flowchart TD
    Trigger["Document Ready Trigger"] --> FetchClaim["Fetch active claims for project"]
    FetchClaim --> SearchVectors["Perform Vector Similarity Search in Qdrant"]
    SearchVectors --> EvaluateMatch["AI evaluates claim vs document context"]
    EvaluateMatch -->|Match Found| SaveEdges["Persist edge type = AI_GENERATED"]
    EvaluateMatch -->|No Match| UpdateStatus["Update claim_evaluations.status"]
    SaveEdges --> UpdateStatus
```

### 5.Manual Mapping & AI Override Workflow

```mermaid
flowchart TD
    ReviewAI["Review AI Suggested Evidence"] --> Decision{"Accept AI?"}
    Decision -->|Yes| AcceptEdge["Update edge type = AI_ACCEPTED"]
    Decision -->|No| RejectEdge["Update edge status = REJECTED"]
    RejectEdge --> ManualSelect["Highlight specific document text"]
    ManualSelect --> SubmitOverride["POST /api/mappings"]
    SubmitOverride --> CreateManual["Create edge type = HUMAN_VERIFIED"]
    AcceptEdge --> UpdateClaim["Recalculate claim confidence score"]
    CreateManual --> UpdateClaim
```

### 6.Project Review & Handoff Workflow

```mermaid
flowchart TD
    Draft["Student creates project status = DRAFT"] --> Work["Upload papers, sources, claims status = ACTIVE"]
    Work --> Submit["POST /api/projects/{projectId}/reviews"]
    Submit --> CheckState{"Project ACTIVE?"}
    CheckState -->|No| RejectAction["400 / 403 Error"]
    CheckState -->|Yes| Pending["Create feedback_request PENDING and update project IN_REVIEW"]
    Pending --> Instructor["Instructor reviews project"]
    Instructor --> Feedback["POST /api/feedback-requests/{id}/feedback"]
    Feedback --> Decision["PATCH /api/feedback-requests/{id}/status"]
    Decision --> Process["Process Instructor payload"]
    Process --> CloseRequest["Update request Final State and update project ACTIVE"]
```

### 7.Traceability Export Generation Workflow
```mermaid
flowchart TD
    RequestExport["GET /api/projects/{id}/export"] --> ValidateAccess["Verify project access"]
    ValidateAccess --> Aggregate["Aggregate Claims, Sources, Edges, Feedback"]
    Aggregate --> GenerateDoc["Generate PDF/CSV Artifact"]
    GenerateDoc --> UploadMinIO["Upload artifact to MinIO"]
    UploadMinIO --> CreateSignedURL["Generate Presigned URL"]
    CreateSignedURL --> ReturnURL["Return URL to client"]
    ReturnURL --> Download["User downloads file"]
```


### 8.System Notification & Event Workflow
```mermaid
flowchart TD
    Event["Project, document, member, or feedback event"] --> Persist["Insert system_notifications row"]
    Persist --> Push["Send STOMP message to /user/queue/notifications"]
    Push --> Inbox["GET /api/notifications"]
    Inbox --> Count["GET /api/notifications/unread-count"]
    Inbox --> Read["PATCH /api/notifications/{id}/read"]
```

## Actor-Oriented Activity Diagrams

These diagrams show the main business flows by participating actor. Internal API, queue, database, and worker steps stay in the previous workflow diagrams.

### Student Project And Source Preparation

```mermaid
flowchart TD
    subgraph Student
        S1["Register or login"]
        S2["Create project"]
        S3["Upload paper and source documents"]
        S4["Create project claims"]
    end

    subgraph System
        A1["Verify account access"]
        A2["Store project workspace"]
        A3["Extract and index documents"]
        A4["Prepare claims for evaluation"]
    end

    S1 --> A1 --> S2
    S2 --> A2 --> S3
    S3 --> A3 --> S4
    S4 --> A4
```

### Evidence Review And Mapping

```mermaid
flowchart TD
    subgraph Student
        S1["Review AI evidence suggestions"]
        S2{"Use suggestion?"}
        S3["Accept suggestion"]
        S4["Select evidence manually"]
    end

    subgraph System
        A1["Match claims with indexed sources"]
        A2["Save evidence mapping"]
        A3["Update traceability status"]
    end

    A1 --> S1 --> S2
    S2 -->|Yes| S3 --> A2
    S2 -->|No| S4 --> A2
    A2 --> A3
```

### Instructor Review And Feedback

```mermaid
flowchart TD
    subgraph Student
        S1["Submit project for review"]
        S2["Read instructor feedback"]
        S3["Revise project evidence"]
    end

    subgraph Instructor
        I1["Open submitted project"]
        I2{"Approve evidence?"}
        I3["Send feedback"]
    end

    subgraph System
        A1["Mark project in review"]
        A2["Notify instructor"]
        A3["Store feedback and notify student"]
    end

    S1 --> A1 --> A2 --> I1 --> I2
    I2 -->|No| I3 --> A3 --> S2 --> S3
    I2 -->|Yes| A3
```

### Traceability Export

```mermaid
flowchart TD
    subgraph Student
        S1["Request traceability export"]
        S2["Download export file"]
    end

    subgraph System
        A1["Validate project access"]
        A2["Collect claims, sources, mappings, and feedback"]
        A3["Generate export artifact"]
    end

    S1 --> A1 --> A2 --> A3 --> S2
```

## Entity State Machines

### User Account

```mermaid
stateDiagram-v2
    [*] --> Verified: seeded account
    [*] --> Unverified: verification token
    Unverified --> Verified: succeeds
    Unverified --> Unverified: token regenerated
    Unverified --> ExpiredToken: token expires
    ExpiredToken --> Unverified: new token
```

### Project Review

```mermaid
stateDiagram-v2
  
    [*] --> DRAFT: create project
    DRAFT --> ACTIVE: paper and source
    ACTIVE --> IN_REVIEW: submit for review
    IN_REVIEW --> ACTIVE: feedback
    ACTIVE --> INACTIVE: delete
    ACTIVE --> COMPLETED: marks complete
    COMPLETED --> ARCHIVED: archive project
    ARCHIVED --> [*]
    INACTIVE --> [*]
```

### Document Processing

```mermaid
stateDiagram-v2
    [*] --> UPLOADED: upload document
    UPLOADED --> PROCESSING: extraction call
    PROCESSING --> READY: extraction, chunks, and vectors saved
    PROCESSING --> FAILED: error
    FAILED --> PROCESSING: retry
    READY --> Inactive: soft delete
    FAILED --> Inactive: soft delete
    Inactive --> [*]
```

### Claim And Evidence Traceability

```mermaid
stateDiagram-v2
    [*] --> ActiveClaim: create claim
    ActiveClaim --> ActiveClaim: claim edited ->update version
    ActiveClaim --> InactiveClaim: delete claim

    [*] --> PendingSuggestion: create or match suggestion
    PendingSuggestion --> AcceptedSuggestion: accept
    PendingSuggestion --> RejectedSuggestion: reject

    [*] --> ActiveMapping: mapping created
    ActiveMapping --> InactiveMapping: deactivate
```

### Feedback Request
RETURNED: sends the work back with feedback.

REVIEWED: has completed the review.

REJECTED: rejects the review/submission/request.

```mermaid
stateDiagram-v2
    [*] --> PENDING: submit
    PENDING --> RETURNED: returns feedback
    PENDING --> REVIEWED: reviewed
    PENDING --> REJECTED: rejects request
    RETURNED --> [*]
    REVIEWED --> [*]
    REJECTED --> [*]
```

### System Notification

```mermaid
stateDiagram-v2
    [*] --> Unread: event trigger create notification
    Unread --> Read: mark read
    Read --> [*]
```

## Logical ERD

SVG without relation: [logical-erd-no-relation.svg](https://www.plantuml.com/plantuml/svg/lLXHRzis47xtho3IXxL3WpPPasP1KTHrxIvOcXH9EcmO1e8HBp9hYTH8oetNxB-FqfaEJKcUq1XzCQpxZiVlEnwFbDuOoxGjysmm5Hn88dIImI236qki8bgaKqkuq91OeUJ0p8Gic6OvoOG4koY0A6sdAW2auc2W88VF0wDcSbOPzqDZmP8PI-7IBZ8WrGnCfXaq7SZN0I5lbUQWHofJcZnwyVBWuF1dPvqeQPUslRsze_sNInl6l5OP7_mx6Fn0JfsUO-84IYliuYafjFhi9ZHF2PESQ9WB-48ikyx03FoiEvyCQFACV4HVz--YzjiXZyP7axt9nF72U6IdTAvcXp13wa4LuH_TTEIOl3qv6rxyyNZly0bvzUhZhwzVEsXtr_r0paJc77uRNen-6LuytZyug5m-eKl1ZFdY9DCfCzF8oy5QPH1O3vu_yH7f7BIu4L3FVNv-EXwUHL32ggMaLir0OlXM4fj6mMqXD1ZAh1lxRdWrlZhvC7O3ReGsbagMX-JsdjViCjO99HgO1Hw5Smldq-kNgywmNuy-INR3CsYcZuD9j2WgHGwy6lz-vOp6CbkQHbOjqobOwb39cUODd2LMp27CThiUPoyBtaidpI6_1hqczqDjwYNQHsbwJT2hgjPEjBssFYiXDp5FL9PXZq3MezNpTYWBQQmk4wis4E-_sFrNhNrrk63hQ-CCMrgQ8bECEnRNYmuFutB0Kg0pMAoj4XOhtD8RaG7kipWJkSY1kgRTiWLntatgRm3uDKjcLCFV9PZbPfmihHTet4uIZ2qvo0TMGsCxxzT2aVt6TGwYUz4ezcG5UDczzEAK3vIJA6aTGL_1FShLB9pCd4fbL-qRPLpMm5bWxoLZSzTWc-cpkXTWySMz1Gd7OnsGWp4iXNlDmb0DZFVPjdZkZrniqCU4gwJCg_GbIaFZe7_dqP53yNW_6eV7kr6bJe2wP46gj81b7clh28sBNZbidd_-kDrce_9gaQPNjvszUT_ekttMqxLuIBTpKjgBfkfojDHPimSgBL8XMTRT2FrcmiWnYg6D-0SSBGkcPlR-vEpAGzMTYPefspyu30OVldY2-Kl0625JsdAVY3n0QwM37JqjvOmcggotUJsXzswvJ1JbTIRCD4OUM3BKmcrKDrVEqgjsV8pkzNQHj6C7l9x6EPsUdxr7zOXXOULWfogwQRMIhJEspdPKcRP45nT4I0wtgmL1ubsxivMbXXlG81DOPthXpLjYkvecOgpP5ngmxheeOKNvU_u2cCO8eQI25Q2f2Izw7olJ9Lgs98GP8r89d6u-SPOqQFrYY5m3Qwmntlbm6vHV0EIS9YoPGe36D_VuaBceNfAFIP8naJybmnDygOIvu22Jvcapglp8C-VeViReLPqwaydaftT7mQbJ1yGiuvYjD72VeFut3IQeizqWKMgg8zI1azFpeKDHDfs3DYugBiCtzY0tZodJ5CnzTGt5vShIIpwowTIstZcw_JfwTwmTRekCIHQSsEq9wQCUX_cgeJah2Y7JOFbzgNn0vBkcOxK8tJue48_o_ZdjaPXLIe2dWFdmgHWxo_VCmnmq5ubjRIj-V_IgaLpW3S1h8_slzNx_-bSJl8gYiVzhIPz6m4extNIdpuI4rU4MNslR6ea6ex8vzBw_rz_ITl-S2iv1_bL_YznT7Py3_oVsmDso77rtjxkhlgv_H1aid_g0mOSC3DWmnmDM_EvT6b3qvX33mKHDkJLmTnTUpoBGZ_AteIOQtq0Q7dYgnxeZe3EFN30Sr0B4Wt2nmh3hIFthJU5nC5ZJsEeHZctjeQqnsZt3m1iTKNYtmpghi9CrsSGURNcAzpuIQCVYv4TVRt3eDs3Hio_H-7j9H6uptFAPul_WDo1vcMV_0G00).

SVG with relation: [logical-erd-relation.svg](https://www.plantuml.com/plantuml/svg/lLbVRzis47_Nfo3SXxL3WpPPasP6KTHrxIvOcXHEEcmO1e6HpZ8RYTH8AedRzBkFqiaiJPc6i1XzoRox7u_t_qxi6wMfr5MHHmkM0acOJ7A8SfXheWMHB5jeaZ89YMQ2H-gQyP9AMf2SST3B4eYGUY4S1XSfA2Z9qNE7A2c_hea7Bh6aoJNDu48lSo3r3PHdENG1v6j4o5iXKv1ZaGj97hruUNXuzAkXJWNNBMr_V__uu6LBGvbN8a-ZRr7q4parzWo9CP1hffTFIId5PtEXUaeeJuc46mQtHDEh75IqkdNmIO4apyX7n9fVimOt88ycnzFzwTJeXVB8e39GDI1KaVfGe_3FvZeo96zFpaQplnxl7pqXhrvz_FtrQyFQspD_eA0iDu0_HhFnRwFPuuF3mnho2I4bLUhMc1mlg5fuACEM8WU7UlZyoKfYV0EIpHaqTv-Vdqv6PnuheNKeOoskWQFuW1BXKt1NCWagfjgSVJUwd5oUV9YO0tCcbOuvBLpbzfytskKqo4eaK0sf9-vRPDtRXWiztCQZbvFDyHowxaj19PALjKM6UJdvyz8GbQQwKXslQYepqB7XyfJAjCEdYMOtu7WxP0yXLPdsiZCkeBW2kMRkXuRKo-YLb2vgYgWwMmFSK6vz5enlqpmHUOuz0hKUs__VZSgCAoshH8kmuls37OvQp-YaZAx7nX1s59eo5pGrMbmj0mYbXQGPnDUmN8iI9YlSnNEM0zQPxuc25H2RfjqjGNjlsFevG7fbMcCiuTyAL5kCqvOwG-9kdA1qbG9_M3Pqo_cWCRJLVfkbZlILqMZiYMMipEvbBrxvG7CImRLHiCtWFkcgbaf34NEXRVj6CouQnfb3xsN6zgv1jwjFcrw0oUVt5bJOFsiO1IX5C_XkCrEn19hsALkS-sCZ6-IGf2AfYjfzYP3G6VJlLjonuU9uFvwuurs9IYOG6sT19YI3reyDTOp4PIyVTkUV7RSxR5GhQr5DQzrEQlDxemzZrjsrV4YtCwPiJfgwoyMLpBizK4YMCKxpO2EqnOIIVIwALkmB6BKqg4QpzoTdboshsPbYjQ07XqVEuQCNJz1_2IZ5U1O7zWa70L8AwLJqekBNSIAgfipnmXQzOPaegwkSgONFUM3An1hklBovD9HUkMDb1EDj94cZ7QJDDGPqUdxs7gr77YPM0NeXl4rhexADnzQr7TUa7QcB0M4yXJiR45GyL9qRjPGm1mayWNLDPoloZhIrrtWQIteREre75oNCg7RFNmALA65j0w8bo5YvYzx7cdI1b1qPGfLYMRrjRfyuhHgna2cqkZfKj35S-N3df9_3AD8ueSa276vuSySXCwi_aeSaoIahdfBH2NvKmWrBWIVTQJEkSUIP0Pe_4qJLKsSwdVxoxjYPEer0z3AUsKK3jmxglsbG5XlQ71MDLJr23NDwUZuoN3Ix_aXKHYg6uN_s87EFgh8CrFVi6h4BI-bbFceJj6szSuHnWTxjsmvtPKuvTIPsm24zhCTZgwo92rgMZ6TE-5UfyW4vFzGTDWX131g8exo_J_iunhe4qWnRgAsQYQ7yI3-OeAOiqTss4gYFO8uK33S0soJwh_Lj_TfF4rn5aTZ_iwHF8w2LNcpRwNF6mGutVwtTr4WaA97VGE_zlNbBs_jxmT8K-3_DM-IU7P_HweUFm6ffJz_Vx-s9hzuZoh1vpb1HIms15jI7ECyQXYzkkRAt-5pxKTUHOVKUDXwYjIREoIugaw8gLUHzRcoGXYps1QnQFWGNRcLzrq3kxzTsFGCRs4MjsNS65k-Bn6rhY9kI2i9cJfBOXEd2rtQ9uIf1qPW0PhKh16EPCcfWHVrVAwSBi8xnnE3SnLHmR_RvDf18GExwz1ewNLH5QmuCuijIIEq9tJWBZ8NuovFSuv3ntTgzsqyCQ990MIS6LGDqwFO3MDwU3Mx_ReHocY-_6A5Nou5dGE24RPnp8GURGkEtn2Rp4c0tjZX2C6nxVYfqNhgC4ks_6qYaM7-4yEsprdQKynWK-Kmirkup51gdIi4LkD8t8sybqvuJkYz2rbrzaBJkSc-0frMH_mS0)

## Database Schema

[https://dbdiagram.io/d/6a44e7394ac62e474c064594](https://dbdiagram.io/d/6a44e7394ac62e474c064594)

## Database Schema Notes

- `documents.doc_type` separates student papers from source evidence documents. Both use the same document storage and extraction pipeline.
- `source_categories` classifies source evidence documents through `documents.source_category_id`.
- `project_members` is the collaboration join table between `projects` and `users`.
- `document_texts` is one-to-one with `documents`; chunks, references, and paper sections are one-to-many.
- `claims` belong to projects and can optionally point to a paper section.
- `ai_suggestions`, `claim_evidence_mappings`, and `evidence_edges` model the traceability flow from AI suggestion to human-approved mapping and analysis result.
- `feedback_requests` connect a project, student, and instructor; each request can have one `instructor_feedbacks` row.

## Technology And Service Stack

| Area | Technologies in current checkout |
| --- | --- |
| Frontend | JavaScript, React 19, React DOM, React Router DOM 7, Axios, Vite 8, Tailwind CSS 4, `@vitejs/plugin-react`; source: `E:/Code/SEP490/EvidencePilot/FE/package.json`. |
| Backend | Java 21, Spring Boot 3.3.5, Spring Web, Spring WebSocket/STOMP, Spring Data JPA, Spring Security, Spring Mail, Spring AMQP, Bean Validation. |
| Persistence and migration | MySQL 8.0.46, Hibernate/JPA, Flyway, `schema.sql` bootstrap for fresh Docker MySQL databases. |
| Storage and async processing | MinIO object storage, RabbitMQ extraction queue, Qdrant vector database. |
| AI integration | External AI worker/model API configured by `AI_MODEL_BASE_URL`; used for extraction, embeddings, claim evaluation, and paper review. |
| API documentation | Springdoc OpenAPI / Swagger UI at `/swagger-ui.html` and OpenAPI JSON at `/v3/api-docs`. |
| Build and test | Maven 3.9 builder image, Spring Boot Maven plugin, Spring Boot Test, H2 test database, Dockerfile multi-stage build. |
| Mobile | N/A |

## Third-Party Services, DevOps Tools, And Environments

| Service/tool | Source/config | Purpose |
| --- | --- | --- |
| GitHub | `origin` remote: `https://github.com/MinhKien17/SEP490_Prototype.git` | Source repository and pull-request hosting. |
| Docker | `Dockerfile` | Builds backend with Maven and runs it on Eclipse Temurin 21 JRE Alpine as non-root `appuser`. |
| Docker Compose | `docker-compose.yml` | Local deployment stack for backend, MySQL, Qdrant, MinIO, RabbitMQ, and init jobs. |
| MySQL | `mysql:8.0.46` Compose service `db` | Relational source of truth. |
| Qdrant | `qdrant/qdrant:latest` Compose service `vector-db` | Dense/sparse vector index for source chunks. |
| MinIO | `minio/minio:latest` Compose service `minio` | Raw document object storage. |
| RabbitMQ | `rabbitmq:3.13-management-alpine` Compose service `rabbitmq` | Async document extraction queue. |
| SMTP provider | `spring.mail.*` environment variables | Sends account verification email. |
| External AI model API | `AI_MODEL_BASE_URL`, optional `AI_MODEL_API_KEY` | Extraction, embeddings, review, and claim evaluation. |

Deployment environments represented in the repository:

| Environment | Evidence | Notes |
| --- | --- | --- |
| Local backend development | `src/main/resources/application.yml` defaults to `localhost` for direct DB and service access where applicable. |
| Local Docker Compose | `docker-compose.yml` binds services to `127.0.0.1` host ports and uses internal service names on `evidence-network`. |
| Production/staging | Not defined in this checkout. Add only when infrastructure manifests, deployment settings, or CI/CD environment configuration exist. |

## Source Control And DevOps

| Item | Current value |
| --- | --- |
| Repository | `https://github.com/MinhKien17/SEP490_Prototype.git` |
| CI/CD workflow files |  |
| Local deploy command | `docker compose up --build` from `E:/Code/SEP490/EvidencePilot/BE` after required `.env` values are set. |
| Fast config check | `docker compose config --quiet` |

Raw Git author participation from `git shortlog -sne --all` on 2026-07-01:

| Commits | Git author |
| ---: | --- |
| 88 | `Adzzse <noik0vshack@gmail.com>` |
| 26 | `MinhKien17 <kiendmse170383@fpt.edu.vn>` |
| 15 | `SE171707_QuangHai <hainqse171707@fpt.edu.vn>` |
| 13 | `hainqse171707 <hainqse171707@fpt.edu.vn>` |
| 12 | `kelvinn0104 <duc142003@gmail.com>` |
| 6 | `Adzzse <134699660+adzzse@users.noreply.github.com>` |
| 2 | `Doan Minh Kien <kiendmse170383@fpt.edu.vn>` |
| 1 | `Nguyen Minh Duc <111220898+kelvinn0104@users.noreply.github.com>` |

Commit counts show repository participation, not man-hours or final contribution percentage.

## Contribution And Effort Tracking

The repository contains commit history but does not contain confirmed weekly man-hours or advisor sign-off. The table below uses 720 estimated team hours across 8 weekday-only weeks, distributed by agreed contribution percentage.

| Member | Main role | Week 1 hours | Week 2 hours | Week 3 hours | Week 4 hours | Week 5 hours | Week 6 hours | Week 7 hours | Week 8 hours | Total hours | Contribution % | Evidence link/note | GVHD confirmed |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- | --- |
| Do Hoang Anh | Backend | 22 | 23 | 21 | 24 | 22 | 23 | 22 | 23 | 180 | 25% |  | Pending |
| Doan Minh Kien | Backend/Leader | 31 | 33 | 30 | 32 | 31 | 32 | 32 | 31 | 252 | 35% |  | Pending |
| Nguyen Quang Hai  | Frontend | 13 | 14 | 13 | 14 | 13 | 14 | 14 | 13 | 108 | 15% |  | Pending |
| Nguyen Minh Duc | Frontend | 24 | 21 | 23 | 22 | 24 | 21 | 22 | 23 | 180 | 25% |  | Pending |

Advisor confirmation summary:

| Confirmed by | Date | Scope confirmed | Signature/note |
| --- | --- | --- | --- |
| GVHD | TBD | Week 1 to Week 8 estimated man-hours and contribution percentages | Pending confirmation |
