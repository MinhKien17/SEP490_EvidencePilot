package com.evidencepilot.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;

import lombok.Getter;
import lombok.Setter;
import com.evidencepilot.model.enums.EvidenceRelation;
import com.evidencepilot.model.enums.StrengthBand;
import com.evidencepilot.model.enums.SuggestionStatus;

@Entity
@Table(name = "ai_suggestions")
@Getter
@Setter
public class AiSuggestion {
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

    @Enumerated(EnumType.STRING)
    private SuggestionStatus status;

    @Column(name = "instructor_rejected", nullable = false)
    private boolean instructorRejected = false;

    private Float score;
    private String explanation;

    @Column(name = "claim_version", nullable = false)
    private Integer claimVersion;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "model_name")
    private String modelName;

    @Column(name = "model_version")
    private String modelVersion;

    @Column(name = "prompt_version")
    private String promptVersion;

    @Column(name = "rubric_version")
    private String rubricVersion;

    @Column(name = "evaluated_at")
    private LocalDateTime evaluatedAt;

    @Column(name = "score_breakdown", columnDefinition = "JSON")
    private String scoreBreakdown;

    @Enumerated(EnumType.STRING)
    private EvidenceRelation relation;

    @Column(name = "strength_score")
    private Integer strengthScore;

    @Column(name = "strength_band")
    @Enumerated(EnumType.STRING)
    private StrengthBand strengthBand;

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AiSuggestion that = (AiSuggestion) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
