package com.evidencepilot.model;

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

    @ManyToOne
    @JoinColumn(name = "project_id", columnDefinition = "BINARY(16)", referencedColumnName = "id", nullable = false)
    private Project project;

    @ManyToOne
    @JoinColumn(name = "section_id", columnDefinition = "BINARY(16)", referencedColumnName = "id")
    private PaperSection section;

    @Column(nullable = false)
    private String content;

    @Column(name = "ai_confidence_score")
    private Float aiConfidenceScore;

    @Column(name = "claim_version", nullable = false)
    private Integer claimVersion;

    private boolean active = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

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
