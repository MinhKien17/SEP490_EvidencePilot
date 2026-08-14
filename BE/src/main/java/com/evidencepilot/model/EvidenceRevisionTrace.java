package com.evidencepilot.model;

import com.evidencepilot.model.enums.InstructorJudgment;
import com.evidencepilot.model.enums.StudentAction;
import com.evidencepilot.model.enums.TraceOutcome;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "evidence_revision_traces")
@Getter
@Setter
public class EvidenceRevisionTrace {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "BINARY(16)")
    @JdbcTypeCode(java.sql.Types.BINARY)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", columnDefinition = "BINARY(16)", referencedColumnName = "id", nullable = false)
    private CitationReviewRound round;

    @Column(name = "finding_index", nullable = false)
    private Integer findingIndex;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", columnDefinition = "BINARY(16)", referencedColumnName = "id", nullable = false)
    private PaperSection section;

    @Column(name = "suggested_action", nullable = false)
    private String suggestedAction;

    @Column(name = "criticality")
    private String criticality;

    @Column(name = "parent_header")
    private String parentHeader;

    @Column(name = "excerpt", nullable = false, columnDefinition = "TEXT")
    private String excerpt;

    @Column(name = "excerpt_start", nullable = false)
    private Integer excerptStart;

    @Column(name = "excerpt_end", nullable = false)
    private Integer excerptEnd;

    @Column(name = "rationale", nullable = false, columnDefinition = "TEXT")
    private String rationale;

    @Column(name = "confidence", precision = 5, scale = 4)
    private BigDecimal confidence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", columnDefinition = "BINARY(16)", referencedColumnName = "id")
    private Document source;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chunk_id", columnDefinition = "BINARY(16)", referencedColumnName = "id")
    private DocumentChunk chunk;

    @Column(name = "evidence_quote", columnDefinition = "TEXT")
    private String evidenceQuote;

    @Column(name = "evidence_relation")
    private String evidenceRelation;

    @Enumerated(EnumType.STRING)
    @Column(name = "student_action")
    private StudentAction studentAction;

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "after_passage", columnDefinition = "LONGTEXT")
    private String afterPassage;

    @Column(name = "after_fingerprint", length = 64)
    private String afterFingerprint;

    @Column(name = "round_duration_ms")
    private Long roundDurationMs;

    @Column(name = "after_section_version")
    private Integer afterSectionVersion;

    @Column(name = "source_replaced")
    private Boolean sourceReplaced;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome")
    private TraceOutcome outcome;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id", columnDefinition = "BINARY(16)", referencedColumnName = "id")
    private User instructor;

    @Enumerated(EnumType.STRING)
    @Column(name = "judgment")
    private InstructorJudgment judgment;

    @Column(name = "instructor_feedback", columnDefinition = "TEXT")
    private String instructorFeedback;

    @Column(name = "judged_at")
    private LocalDateTime judgedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_round_id", columnDefinition = "BINARY(16)", referencedColumnName = "id")
    private CitationReviewRound linkedRound;

    @Column(name = "linked_mode")
    private String linkedMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_recheck_judgment")
    private InstructorJudgment aiRecheckJudgment;

    @Column(name = "ai_recheck_reason", columnDefinition = "TEXT")
    private String aiRecheckReason;

    @Column(name = "ai_rechecked_at")
    private LocalDateTime aiRecheckedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
