package com.evidencepilot.exception;

import org.springframework.http.HttpStatus;

import java.util.List;

public class TexCompileException extends RuntimeException {

    private final String code;
    private final HttpStatus status;
    private final List<Diagnostic> diagnostics;

    public TexCompileException(
            String code,
            HttpStatus status,
            String message,
            List<Diagnostic> diagnostics) {
        super(message);
        this.code = code;
        this.status = status;
        this.diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
    }

    public String code() {
        return code;
    }

    public HttpStatus status() {
        return status;
    }

    public List<Diagnostic> diagnostics() {
        return diagnostics;
    }

    public record Diagnostic(String file, Integer line, String message) {
    }
}
