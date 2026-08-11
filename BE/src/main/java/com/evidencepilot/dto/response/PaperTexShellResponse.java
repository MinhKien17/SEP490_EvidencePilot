package com.evidencepilot.dto.response;

import com.evidencepilot.service.PaperTexAssembler;

public record PaperTexShellResponse(
        String preambleTex,
        String frontMatterTex,
        Long version) {

    public static PaperTexShellResponse from(PaperTexAssembler.PaperTexShell shell) {
        return new PaperTexShellResponse(
                shell.preambleTex(), shell.frontMatterTex(), shell.version());
    }
}
