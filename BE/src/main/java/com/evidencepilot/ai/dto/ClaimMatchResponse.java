package com.evidencepilot.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response body for local claim-to-source matching.
 *
 * <p>Field summary:
 * <ul>
 *   <li>{@code claim}   – required; echo of the request claim text</li>
 *   <li>{@code matches} – required; ordered list of {@link ClaimMatch} items</li>
 * </ul>
 * </p>
 */
public record ClaimMatchResponse(

        @JsonProperty("claim")
        String claim,

        @JsonProperty("matches")
        List<ClaimMatch> matches
) {
    /** Returns {@code true} when the backend found at least one matching source. */
    public boolean hasMatches() {
        return matches != null && !matches.isEmpty();
    }
}
