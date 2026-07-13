package com.evidencepilot.model;

import com.evidencepilot.model.enums.EdgeType;
import jakarta.persistence.*;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "document_references")
@Getter
@Setter
public class DocumentReference {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "BINARY(16)")
    @JdbcTypeCode(java.sql.Types.BINARY)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "document_id", columnDefinition = "BINARY(16)", referencedColumnName = "id", nullable = false)
    private Document document;

    @Column(name = "reference_index", nullable = false)
    private Integer referenceIndex;

    private String rawText;
    private String title;

    @Column(name = "publication_year")
    private Integer publicationYear;

    private String doi;

    @Enumerated(EnumType.STRING)
    @Column(name = "edge_type")
    private EdgeType edgeType;

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        DocumentReference that = (DocumentReference) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
