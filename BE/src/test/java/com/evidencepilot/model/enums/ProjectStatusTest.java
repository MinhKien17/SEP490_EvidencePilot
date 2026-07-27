package com.evidencepilot.model.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectStatusTest {

    @Test
    void onlyApprovedAndArchivedAreReadOnly() {
        assertThat(ProjectStatus.APPROVED.isReadOnly()).isTrue();
        assertThat(ProjectStatus.ARCHIVED.isReadOnly()).isTrue();
        assertThat(ProjectStatus.ASSIGNED.isReadOnly()).isFalse();
        assertThat(ProjectStatus.IN_PROGRESS.isReadOnly()).isFalse();
        assertThat(ProjectStatus.SUBMITTED_FOR_REVIEW.isReadOnly()).isFalse();
        assertThat(ProjectStatus.RETURNED.isReadOnly()).isFalse();
    }
}
