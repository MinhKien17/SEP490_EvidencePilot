package com.evidencepilot.dto.response;

import com.evidencepilot.model.SystemNotification;

import java.time.LocalDateTime;
import java.util.UUID;

public record SystemNotificationResponse(
        UUID id,
        UUID userId,
        UUID actorId,
        String actionType,
        UUID entityId,
        String message,
        boolean read,
        LocalDateTime createdAt
) {
    public static SystemNotificationResponse from(SystemNotification notification) {
        return new SystemNotificationResponse(
                notification.getId(),
                notification.getUser().getId(),
                notification.getActor() != null ? notification.getActor().getId() : null,
                notification.getActionType(),
                notification.getEntityId(),
                notification.getMessage(),
                notification.isRead(),
                notification.getCreatedAt());
    }
}
