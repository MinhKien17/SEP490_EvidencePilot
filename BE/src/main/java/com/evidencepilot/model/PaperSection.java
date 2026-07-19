package com.evidencepilot.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "paper_sections")
@Getter
@Setter
public class PaperSection {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "BINARY(16)")
    @JdbcTypeCode(java.sql.Types.BINARY)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "document_id", columnDefinition = "BINARY(16)", referencedColumnName = "id", nullable = false)
    private Document document;

    @ManyToOne
    @JoinColumn(name = "assigned_user_id", columnDefinition = "BINARY(16)", referencedColumnName = "id")
    private User assignedUser;

    @Column(name = "section_order", nullable = false)
    private Integer sectionOrder;

    @Column(name = "section_title", nullable = false)
    private String sectionTitle;

    @Column(name = "content_tex", nullable = false, columnDefinition = "LONGTEXT")
    private String contentTex;

    @Column(name = "content_md_cache", columnDefinition = "LONGTEXT")
    private String contentMdCache;

    @Column(name = "active")
    private boolean active = true;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "section")
    private List<SectionFeedback> sectionFeedback;

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        PaperSection that = (PaperSection) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
