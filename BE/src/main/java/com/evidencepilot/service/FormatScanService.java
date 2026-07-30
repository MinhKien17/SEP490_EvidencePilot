package com.evidencepilot.service;

import com.evidencepilot.dto.response.FormatScanResponse;
import java.util.UUID;

public interface FormatScanService {
    FormatScanResponse scanFormat(UUID documentId);
}
