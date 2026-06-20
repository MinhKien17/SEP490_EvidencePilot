package com.evidencepilot.domain.enums;

/**
 * Lifecycle states for a student's research project.
 * Must stay in sync with the MySQL {@code projects.status} ENUM column.
 */
public enum ProjectStatus {
    DRAFT,
    ACTIVE,
    IN_REVIEW,
    COMPLETED,
    ARCHIVED,
    DELETED
}
