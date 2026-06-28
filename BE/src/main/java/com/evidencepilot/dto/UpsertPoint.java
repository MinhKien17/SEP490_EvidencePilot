package com.evidencepilot.dto;

import java.util.Map;

public record UpsertPoint(String id, NamedVectors vector, Map<String, Object> payload) {
}
