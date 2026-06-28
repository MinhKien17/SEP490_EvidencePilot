package com.evidencepilot.service;

import com.evidencepilot.dto.response.TraceabilityExportResponse;
import java.util.UUID;

public interface TraceabilityExportService {
    TraceabilityExportResponse exportTraceability(UUID projectId);
}
