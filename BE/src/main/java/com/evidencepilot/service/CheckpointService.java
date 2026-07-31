package com.evidencepilot.service;

import com.evidencepilot.dto.response.CheckpointDiffResponse;
import java.util.UUID;

public interface CheckpointService {
    void capture(UUID projectId, String trigger);

    CheckpointDiffResponse getDiff(UUID projectId);
}
