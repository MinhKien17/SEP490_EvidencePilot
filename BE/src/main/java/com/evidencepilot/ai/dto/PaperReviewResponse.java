package com.evidencepilot.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response body for the backend's local paper review.
 *
 * <p>All fields are returned by the local review service.
 * Style enums: conference | article | magazine | report | thesis | unknown</p>
 */
public record PaperReviewResponse(

        @JsonProperty("paper_id")
        String paperId,

        @JsonProperty("detected_style")
        String detectedStyle,

        @JsonProperty("target_style")
        String targetStyle,

        @JsonProperty("missing_sections")
        List<SectionIssue> missingSections,

        @JsonProperty("weak_sections")
        List<SectionIssue> weakSections,

        @JsonProperty("claim_recommendations")
        List<SectionIssue> claimRecommendations
) {}
