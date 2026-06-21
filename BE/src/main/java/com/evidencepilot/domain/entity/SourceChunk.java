package com.evidencepilot.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "source_chunks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SourceChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    @JsonIgnore
    private Source source;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Column(name = "page")
    private Integer page;

    @Column(name = "text", nullable = false, columnDefinition = "TEXT")
    private String text;

    /**
     * Dense vector embedding of this chunk's text, serialized as a JSON
     * array string (e.g. {@code "[0.123, -0.456, ...]"}).
     *
     * <p>
     * Nullable — chunks created before the vector migration will not
     * have embeddings. This is a transitional column; it will be replaced
     * by a native {@code VECTOR} type once the DB schema is finalized.
     * </p>
     */
    @Column(name = "embedding", columnDefinition = "TEXT")
    private String embedding;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
