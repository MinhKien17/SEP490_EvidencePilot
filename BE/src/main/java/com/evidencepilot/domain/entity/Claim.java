package com.evidencepilot.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Represents an AI-extracted claim from a project's paper.
 * Maps to the {@code claims} table.
 */
@Entity
@Table(name = "claims")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Claim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    /**
     * The project this claim belongs to.
     * Foreign key: claims.project_id → projects.id
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Project project;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * Confidence score assigned by the AI pipeline (0.00 – 1.00).
     */
    @Column(name = "ai_confidence_score", precision = 5, scale = 4)
    private BigDecimal aiConfidenceScore;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private java.util.List<EvidenceEdge> evidenceEdges;
}

