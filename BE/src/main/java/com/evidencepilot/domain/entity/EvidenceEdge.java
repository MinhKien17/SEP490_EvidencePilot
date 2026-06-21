package com.evidencepilot.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Represents an evidence edge in the traceability graph.
 * Maps to the {@code evidence_edges} table.
 */
@Entity
@Table(name = "evidence_edges")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvidenceEdge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Claim claim;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_chunk_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private SourceChunk sourceChunk;

    @Column(name = "verdict", nullable = false)
    private String verdict;

    @Column(name = "confidence_score", precision = 5, scale = 4, nullable = false)
    private BigDecimal confidenceScore;

    @Column(name = "explanation", nullable = false, columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "missing_evidence", columnDefinition = "TEXT")
    private String missingEvidence;
}
