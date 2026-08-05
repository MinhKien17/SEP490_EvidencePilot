package com.evidencepilot.dto.response;

import java.util.List;

public record AdminUserImportResponse(int created, int updated, List<ImportError> errors) {

    public record ImportError(int item, String field, String message) {
    }
}
