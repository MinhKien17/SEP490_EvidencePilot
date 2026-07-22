package com.evidencepilot.model.enums;

public enum ProjectStatus {
    ASSIGNED, IN_PROGRESS, SUBMITTED_FOR_REVIEW, RETURNED, APPROVED, ARCHIVED;

    public boolean isReadOnly() {
        return this == APPROVED || this == ARCHIVED;
    }
}
