package com.evidencepilot.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "source_texts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SourceText {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false, unique = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Source source;

    @Column(name = "extracted_text", nullable = false, columnDefinition = "LONGTEXT")
    private String extractedText;

    @Column(name = "extraction_method", nullable = false, length = 50)
    private String extractionMethod;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
