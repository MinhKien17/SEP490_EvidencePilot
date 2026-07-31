package com.evidencepilot.service;

import com.evidencepilot.dto.response.GraphResponse;
import java.util.UUID;

public interface GraphService {
    GraphResponse getGraph(UUID projectId, String scope);

    GraphResponse.ClaimStatsResponse getClaimStats(UUID projectId);
}
