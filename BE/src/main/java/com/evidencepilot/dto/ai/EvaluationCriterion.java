package com.evidencepilot.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EvaluationCriterion(
        String code,
        String description,
        double weight) {

    public EvaluationCriterion {
        if (code == null || code.isBlank() || code.length() > 100
                || description == null || description.isBlank()
                || weight < 0.0 || weight > 1.0) {
            throw new IllegalArgumentException("Invalid evaluation criterion");
        }
    }

    @JsonProperty("code")
    public String code() {
        return code;
    }

    @JsonProperty("description")
    public String description() {
        return description;
    }

    @JsonProperty("weight")
    public double weight() {
        return weight;
    }
}
