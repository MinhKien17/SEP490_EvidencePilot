package com.evidencepilot.exception;

public class QdrantException extends RuntimeException {
    private final int statusCode;

    public QdrantException(String message) {
        super(message);
        this.statusCode = 0;
    }

    public QdrantException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 0;
    }

    public QdrantException(String operation, int statusCode) {
        super("Qdrant error on " + operation + " – HTTP " + statusCode);
        this.statusCode = statusCode;
    }

    public QdrantException(String operation, String message, Throwable cause) {
        super("Qdrant error on " + operation + " – " + message, cause);
        this.statusCode = 0;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
