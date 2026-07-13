package com.evidencepilot.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;

import lombok.Getter;
import lombok.Setter;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.model.enums.ProcessingStatus;

@Entity
@Table(name = "documents")
@Getter
@Setter
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "BINARY(16)")
    @JdbcTypeCode(java.sql.Types.BINARY)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "project_id", columnDefinition = "BINARY(16)", referencedColumnName = "id")
    private Project project;

    @ManyToOne
    @JoinColumn(name = "collection_id", columnDefinition = "BINARY(16)", referencedColumnName = "id")
    private Collection collection;

    @ManyToOne
    @JoinColumn(name = "source_category_id", columnDefinition = "BINARY(16)", referencedColumnName = "id")
    private SourceCategory sourceCategory;

    @ManyToOne
    @JoinColumn(name = "uploaded_by", columnDefinition = "BINARY(16)", referencedColumnName = "id", nullable = false)
    private User uploadedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "doc_type")
    private DocumentType docType;

    @Column(name = "file_url", nullable = false)
    private String fileUrl;

    @Column(name = "original_filename")
    private String originalFilename;

    private String contentType;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "file_hash_sha256")
    private String fileHashSha256;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status")
    private ProcessingStatus processingStatus;

    @Column(name = "processing_error", columnDefinition = "TEXT")
    private String processingError;

    @Column(name = "chunk_count")
    private Integer chunkCount;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    private boolean active = true;

    @Column(name = "doi")
    private String doi;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String authors;

    @Column(name = "publication_year")
    private Integer publicationYear;

    private String publisher;

    @Column(name = "download_token", length = 36)
    private String downloadToken;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToOne(mappedBy = "document")
    private DocumentText documentText;

    @OneToMany(mappedBy = "document")
    private List<DocumentChunk> documentChunks;

    @OneToMany(mappedBy = "document")
    private List<DocumentReference> documentReferences;

    @OneToMany(mappedBy = "document")
    private List<PaperSection> paperSections;

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Document document = (Document) o;
        return id.equals(document.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
