package com.evidencepilot.model;

import com.evidencepilot.model.enums.FunctionalType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "claims")
@Getter
@Setter
public class Claim {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "BINARY(16)")
    @JdbcTypeCode(java.sql.Types.BINARY)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", columnDefinition = "BINARY(16)", referencedColumnName = "id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", columnDefinition = "BINARY(16)", referencedColumnName = "id")
    private PaperSection section;

    @Column(nullable = false)
    private String content;

    @Column(name = "ai_confidence_score")
    private Float aiConfidenceScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "functional_type")
    private FunctionalType functionalType;

    @Column(name = "claim_version", nullable = false)
    private Integer claimVersion;

    @Version
    @Column(name = "opt_version")
    private Long optVersion;

    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", columnDefinition = "BINARY(16)", referencedColumnName = "id")
    private User createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "claim")
    private List<AiSuggestion> aiSuggestions;

    @OneToMany(mappedBy = "claim")
    private List<ClaimEvidenceMapping> claimEvidenceMappings;

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Claim claim = (Claim) o;
        return id.equals(claim.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
