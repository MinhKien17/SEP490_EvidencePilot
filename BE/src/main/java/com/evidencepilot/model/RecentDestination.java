package com.evidencepilot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Phase B: private per-user navigation shortcut. Not an audit record and not
 * productivity evidence — only the owning user can read it (see
 * RecentDestinationService access checks). Routes are derived server-side;
 * the client never submits a URL.
 */
@Entity
@Table(name = "recent_destinations")
@Getter
@Setter
public class RecentDestination {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "BINARY(16)")
    @JdbcTypeCode(java.sql.Types.BINARY)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", columnDefinition = "BINARY(16)", referencedColumnName = "id", nullable = false)
    private User user;

    @Column(nullable = false, length = 30)
    private String kind;

    @Column(name = "ref_id", columnDefinition = "BINARY(16)", nullable = false)
    @JdbcTypeCode(java.sql.Types.BINARY)
    private UUID refId;

    @Column(length = 50)
    private String tab;

    @Column(length = 255)
    private String label;

    @Column(length = 255)
    private String context;

    @Column(nullable = false, length = 500)
    private String link;

    @Column(name = "last_opened_at", nullable = false)
    private LocalDateTime lastOpenedAt;
}
