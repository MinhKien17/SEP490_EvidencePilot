package com.evidencepilot.service;

import com.evidencepilot.dto.response.ReviewGuideResponse;

import java.util.List;

public interface ReviewGuideService {

    List<ReviewGuideResponse> getActiveGuides();
}
