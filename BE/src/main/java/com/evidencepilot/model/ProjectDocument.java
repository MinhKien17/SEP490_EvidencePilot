package com.evidencepilot.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "project_documents", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"project_id", "document_id"})
})
@Getter
@Setter
public class ProjectDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "BINARY(16)")
    @JdbcTypeCode(java.sql.Types.BINARY)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "project_id", columnDefinition = "BINARY(16)", referencedColumnName = "id", nullable = false)
    private Project project;

    @ManyToOne
    @JoinColumn(name = "document_id", columnDefinition = "BINARY(16)", referencedColumnName = "id", nullable = false)
    private Document document;

    @ManyToOne
    @JoinColumn(name = "shared_by", columnDefinition = "BINARY(16)", referencedColumnName = "id", nullable = false)
    private User sharedBy;

    @Column(name = "shared_at")
    private LocalDateTime sharedAt;
}