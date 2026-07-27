package com.evidencepilot.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;

import lombok.Getter;
import lombok.Setter;
import com.evidencepilot.model.enums.EvidenceRelation;
import com.evidencepilot.model.enums.MappingReviewStatus;
import com.evidencepilot.model.enums.MappingStatus;
import com.evidencepilot.model.enums.StrengthBand;

@Entity
@Table(name = "claim_evidence_mappings")
@Getter
@Setter
public class ClaimEvidenceMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "BINARY(16)")
    @JdbcTypeCode(java.sql.Types.BINARY)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "claim_id", columnDefinition = "BINARY(16)", referencedColumnName = "id", nullable = false)
    private Claim claim;

    @ManyToOne
    @JoinColumn(name = "document_chunk_id", columnDefinition = "BINARY(16)", referencedColumnName = "id", nullable = false)
    private DocumentChunk documentChunk;

    @ManyToOne
    @JoinColumn(name = "suggestion_id", columnDefinition = "BINARY(16)", referencedColumnName = "id")
    private AiSuggestion suggestion;

    @ManyToOne
    @JoinColumn(name = "created_by", columnDefinition = "BINARY(16)", referencedColumnName = "id", nullable = false)
    private User createdBy;

    @Enumerated(EnumType.STRING)
    private MappingStatus status;

    @Column(name = "instructor_rejected", nullable = false)
    private boolean instructorRejected = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private EvidenceRelation relation;

    @Column(name = "strength_score")
    private Integer strengthScore;

    @Column(name = "strength_band")
    @Enumerated(EnumType.STRING)
    private StrengthBand strengthBand;

    @Column(name = "review_status")
    @Enumerated(EnumType.STRING)
    private MappingReviewStatus reviewStatus;

    @ManyToOne
    @JoinColumn(name = "reviewed_by", columnDefinition = "BINARY(16)", referencedColumnName = "id")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_note", columnDefinition = "TEXT")
    private String reviewNote;

    @Column(name = "relation_override")
    @Enumerated(EnumType.STRING)
    private EvidenceRelation relationOverride;

    @Column(name = "score_breakdown", columnDefinition = "JSON")
    private String scoreBreakdown;

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ClaimEvidenceMapping that = (ClaimEvidenceMapping) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
