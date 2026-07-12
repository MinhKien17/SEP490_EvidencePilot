package com.evidencepilot.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface AiModelClient {

    Map<String, Object> health();

    String generate(String prompt);

    ExtractedDocument extractDocument(
            UUID documentId,
            String filename,
            String contentType,
            String downloadUrl);

    List<Float> generateEmbedding(String text);

    List<List<Float>> generateEmbeddings(List<String> texts);

    record ExtractedDocument(String filename, String method, String markdown) {
    }

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
