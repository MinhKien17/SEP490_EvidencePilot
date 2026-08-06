package com.evidencepilot.model;

import com.evidencepilot.model.enums.SectionAuditFindingStatus;
import com.evidencepilot.model.enums.SectionAuditIssueType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "section_audit_findings")
@Getter
@Setter
public class SectionAuditFinding {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "BINARY(16)")
    @JdbcTypeCode(java.sql.Types.BINARY)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", columnDefinition = "BINARY(16)", referencedColumnName = "id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", columnDefinition = "BINARY(16)", referencedColumnName = "id", nullable = false)
    private PaperSection section;

    @Column(name = "content_fingerprint", nullable = false, length = 64)
    private String contentFingerprint;

    @Column(name = "start_index", nullable = false)
    private Integer startIndex;

    @Column(name = "end_index", nullable = false)
    private Integer endIndex;

    @Column(name = "original_text_snippet", nullable = false, columnDefinition = "TEXT")
    private String originalTextSnippet;

    @Enumerated(EnumType.STRING)
    @Column(name = "issue_type", nullable = false, length = 100)
    private SectionAuditIssueType issueType;

    @Column(name = "suggested_paraphrase", columnDefinition = "TEXT")
    private String suggestedParaphrase;

    @Column(name = "rationale", nullable = false, columnDefinition = "TEXT")
    private String rationale;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SectionAuditFindingStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", columnDefinition = "BINARY(16)", referencedColumnName = "id")
    private User createdBy;

    @Column(name = "model_name", length = 255)
    private String modelName;

    @Column(name = "prompt_version", length = 255)
    private String promptVersion;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
