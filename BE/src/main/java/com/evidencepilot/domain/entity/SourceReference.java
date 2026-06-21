package com.evidencepilot.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "source_references")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SourceReference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Source source;

    @Column(name = "reference_index", nullable = false)
    private Integer referenceIndex;

    @Column(name = "raw_text", nullable = false, columnDefinition = "TEXT")
    private String rawText;

    @Column(name = "title")
    private String title;

    @Column(name = "year")
    private Integer year;
}
