package com.evidencepilot.controller;

import com.evidencepilot.dto.request.PaperPreviewRequest;
import com.evidencepilot.dto.request.PaperTexShellUpdateRequest;
import com.evidencepilot.dto.response.PaperTexShellResponse;
import com.evidencepilot.service.PaperTexAssembler;
import com.evidencepilot.service.TexCompileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/papers")
@RequiredArgsConstructor
public class PaperTexController {

    private final PaperTexAssembler assembler;
    private final TexCompileService compileService;

    @GetMapping("/{documentId}/tex-shell")
    public PaperTexShellResponse shell(@PathVariable UUID documentId) {
        return PaperTexShellResponse.from(assembler.getShell(documentId));
    }

    @PutMapping("/{documentId}/tex-shell")
    public PaperTexShellResponse updateShell(
            @PathVariable UUID documentId,
            @Valid @RequestBody PaperTexShellUpdateRequest request) {
        return PaperTexShellResponse.from(assembler.updateShell(
                documentId,
                request.preambleTex(),
                request.frontMatterTex(),
                request.version()));
    }

    @PostMapping(
            value = "/{documentId}/preview",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> preview(
            @PathVariable UUID documentId,
            @Valid @RequestBody(required = false) PaperPreviewRequest request) {
        PaperTexAssembler.DraftOverride override = request == null
                ? new PaperTexAssembler.DraftOverride(null, null)
                : request.toDraftOverride();
        try (PaperTexAssembler.PaperTexWorkspace workspace = assembler.assemble(documentId, override)) {
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(compileService.compile(workspace));
        }
    }
}
