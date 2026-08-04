package com.evidencepilot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "project_collections", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"project_id", "collection_id"})
})
@Getter
@Setter
public class ProjectCollection {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "BINARY(16)")
    @JdbcTypeCode(java.sql.Types.BINARY)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", columnDefinition = "BINARY(16)", referencedColumnName = "id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id", columnDefinition = "BINARY(16)", referencedColumnName = "id", nullable = false)
    private Collection collection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_by", columnDefinition = "BINARY(16)", referencedColumnName = "id", nullable = false)
    private User linkedBy;

    @Column(name = "linked_at", nullable = false)
    private LocalDateTime linkedAt;
}
