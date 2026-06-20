package com.evidencepilot.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents a paper (PDF/document) submitted for a project.
 * Maps to the {@code papers} table.
 */
@Entity
@Table(name = "papers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Paper {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    /**
     * The project this paper belongs to.
     * Foreign key: papers.project_id → projects.id
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "file_url", nullable = false)
    private String fileUrl;

    @Column(name = "original_filename")
    private String originalFilename;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "extracted_text", columnDefinition = "LONGTEXT")
    private String extractedText;

    @Column(name = "extraction_method", length = 50)
    private String extractionMethod;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;
}
