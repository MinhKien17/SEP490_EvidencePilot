package com.evidencepilot.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents an uploaded source file that can belong to a project or a dataset.
 * Both {@code project_id} and {@code dataset_id} are nullable (one may be absent).
 * Maps to the {@code sources} table.
 */
@Entity
@Table(name = "sources")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Source {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "file_url", nullable = false)
    private String fileUrl;

    @Column(name = "original_filename")
    private String originalFilename;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    /**
     * The project this source belongs to (nullable).
     * Foreign key: sources.project_id → projects.id
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    /**
     * The dataset this source belongs to (nullable).
     * Foreign key: sources.dataset_id → datasets.id
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dataset_id")
    private Dataset dataset;

    /**
     * The user who uploaded this source.
     * Foreign key: sources.uploaded_by → users.id
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;
}
