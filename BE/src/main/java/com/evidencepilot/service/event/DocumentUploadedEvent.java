package com.evidencepilot.service.event;

import java.util.UUID;

public record DocumentUploadedEvent(UUID documentId) {
}
