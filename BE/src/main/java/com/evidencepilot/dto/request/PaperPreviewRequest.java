package com.evidencepilot.dto.request;

import com.evidencepilot.service.PaperTexAssembler;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record PaperPreviewRequest(
        @Valid SectionOverride sectionOverride,
        @Valid ShellOverride shellOverride) {

    @AssertTrue(message = "Only one preview override is allowed")
    public boolean hasAtMostOneOverride() {
        return sectionOverride == null || shellOverride == null;
    }

    public PaperTexAssembler.DraftOverride toDraftOverride() {
        return new PaperTexAssembler.DraftOverride(
                sectionOverride == null ? null : new PaperTexAssembler.SectionDraft(
                        sectionOverride.sectionId(), sectionOverride.contentTex()),
                shellOverride == null ? null : new PaperTexAssembler.ShellDraft(
                        shellOverride.kind(), shellOverride.contentTex()));
    }

    public record SectionOverride(
            @NotNull UUID sectionId,
            @NotNull @Size(max = PaperTexAssembler.MAX_SOURCE_BYTES) String contentTex) {
    }

    public record ShellOverride(
            @NotNull PaperTexAssembler.ShellKind kind,
            @NotNull @Size(max = PaperTexAssembler.MAX_SOURCE_BYTES) String contentTex) {
    }
}
