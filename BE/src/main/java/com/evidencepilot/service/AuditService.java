package com.evidencepilot.service;

import com.evidencepilot.model.AuditLog;
import com.evidencepilot.model.User;
import com.evidencepilot.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String action, String entityType, UUID entityId, User actor, Object oldValue, Object newValue) {
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setActor(actor);
        log.setOccurredAt(LocalDateTime.now());
        try {
            log.setOldValue(oldValue != null ? objectMapper.writeValueAsString(oldValue) : null);
            log.setNewValue(newValue != null ? objectMapper.writeValueAsString(newValue) : null);
        } catch (Exception e) {
            log.setOldValue(oldValue != null ? oldValue.toString() : null);
            log.setNewValue(newValue != null ? newValue.toString() : null);
        }
        auditLogRepository.save(log);
    }
}
