package com.evidencepilot.dto.response;

import java.util.List;

public record OpenAlexPreview(
        String title,
        Integer publicationYear,
        String publisher,
        List<String> authors,
        String oaUrl,
        boolean hasPdf
) {}
