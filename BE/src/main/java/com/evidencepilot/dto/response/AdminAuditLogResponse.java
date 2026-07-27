package com.evidencepilot.dto.response;

import com.evidencepilot.model.AuditLog;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdminAuditLogResponse(
        UUID actorId,
        String actorEmail,
        String action,
        String entityType,
        UUID entityId,
        String oldValue,
        String newValue,
        LocalDateTime occurredAt) {

    public static AdminAuditLogResponse from(AuditLog log) {
        return new AdminAuditLogResponse(log.getActor().getId(), log.getActor().getEmail(), log.getAction(),
                log.getEntityType(), log.getEntityId(), log.getOldValue(), log.getNewValue(), log.getOccurredAt());
    }
}
