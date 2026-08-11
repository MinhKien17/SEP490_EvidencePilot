package com.evidencepilot.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PaperTexShellUpdateRequest(
        @NotNull @Size(min = 1, max = 2 * 1024 * 1024) String preambleTex,
        @NotNull @Size(min = 1, max = 2 * 1024 * 1024) String frontMatterTex,
        @NotNull Long version) {
}
