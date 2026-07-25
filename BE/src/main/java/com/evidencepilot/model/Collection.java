package com.evidencepilot.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "collections")
@Getter
@Setter
public class Collection {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "BINARY(16)")
    @JdbcTypeCode(java.sql.Types.BINARY)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "project_id", columnDefinition = "BINARY(16)", referencedColumnName = "id")
    private Project project;

    @ManyToOne
    @JoinColumn(name = "instructor_id", columnDefinition = "BINARY(16)", referencedColumnName = "id", nullable = false)
    private User instructor;

    private String title;

    private String description;

    @ManyToOne
    @JoinColumn(name = "category_id", columnDefinition = "BINARY(16)", referencedColumnName = "id")
    private CollectionCategory category;

    private boolean active = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Collection that = (Collection) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
