package com.evidencepilot.dto.response;

import com.evidencepilot.exception.TexCompileException;

import java.util.List;

public record TexCompileErrorResponse(
        String code,
        String message,
        List<TexCompileException.Diagnostic> diagnostics) {
}
