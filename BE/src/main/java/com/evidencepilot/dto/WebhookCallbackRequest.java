package com.evidencepilot.dto;

import java.util.UUID;

public record WebhookCallbackRequest(UUID documentId, String status) {}
