package com.evidencepilot.service;

import java.util.Map;
import java.util.UUID;

public interface AiModelClient {

    Map<String, Object> health();

    String generate(String prompt);

    Map<String, Object> processClaim(UUID claimId, String claimText, UUID sourceId, String excerpt);

    double[] generateEmbedding(String text);

    final class AiApiException extends RuntimeException {
        private final int statusCode;

        public AiApiException(String endpoint, int statusCode) {
            this(endpoint, statusCode, null, null);
        }

        public AiApiException(String endpoint, int statusCode, Throwable cause) {
            this(endpoint, statusCode, null, cause);
        }

        public AiApiException(String endpoint, int statusCode, String message, Throwable cause) {
            super("AI API error on " + endpoint + " - HTTP " + statusCode + (message != null ? " " + message : ""),
                    cause);
            this.statusCode = statusCode;
        }

        public AiApiException(String endpoint, String message, Throwable cause) {
            super("AI API error on " + endpoint + " - " + message, cause);
            this.statusCode = 0;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }
}
