package com.evidencepilot.client.qdrant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record QdrantSearchResponse(List<ScoredPoint> result) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ScoredPoint(float score, Map<String, Object> payload) {
        public String getText() {
            if (payload != null && payload.containsKey("text")) {
                Object text = payload.get("text");
                return text != null ? text.toString() : "";
            }
            return "";
        }
    }
}
