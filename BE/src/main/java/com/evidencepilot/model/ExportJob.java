package com.evidencepilot.model;

import com.evidencepilot.model.enums.ExportFormat;
import com.evidencepilot.model.enums.ExportStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;

@Entity
@Table(name = "export_jobs")
@Getter
@Setter
public class ExportJob {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "BINARY(16)")
    @JdbcTypeCode(java.sql.Types.BINARY)
    private UUID id;

    @Column(name = "project_id", columnDefinition = "BINARY(16)", nullable = false)
    @JdbcTypeCode(java.sql.Types.BINARY)
    private UUID projectId;

    @Column(name = "user_id", columnDefinition = "BINARY(16)", nullable = false)
    @JdbcTypeCode(java.sql.Types.BINARY)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ExportStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "format", nullable = false, length = 20)
    private ExportFormat format;

    @Column(name = "download_url", length = 1024)
    private String downloadUrl;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
