package com.evidencepilot.dto;

import java.util.UUID;

public record ExportRequest(UUID jobId, UUID projectId, UUID userId, String format) {}
