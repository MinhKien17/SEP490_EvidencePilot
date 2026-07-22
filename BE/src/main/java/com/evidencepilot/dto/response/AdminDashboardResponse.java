package com.evidencepilot.dto.response;

import com.evidencepilot.model.enums.AccountStatus;
import com.evidencepilot.model.enums.ProjectStatus;
import com.evidencepilot.model.enums.UserRole;

import java.util.Map;

public record AdminDashboardResponse(
        long totalUsers,
        Map<UserRole, Long> usersByRole,
        Map<AccountStatus, Long> usersByStatus,
        long activeProjects,
        Map<ProjectStatus, Long> activeProjectsByStatus,
        long activeSourceCategories,
        long activeCollections,
        long activeSourceDocuments,
        long activePaperDocuments,
        Map<String, Object> infrastructureReadiness) {
}
