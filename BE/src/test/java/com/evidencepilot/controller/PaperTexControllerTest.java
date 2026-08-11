package com.evidencepilot.controller;

import com.evidencepilot.service.PaperTexAssembler;
import com.evidencepilot.service.TexCompileService;
import com.evidencepilot.exception.TexCompileException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class PaperTexControllerTest {

    @Mock
    private PaperTexAssembler assembler;

    @Mock
    private TexCompileService compileService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new PaperTexController(assembler, compileService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void previewReturnsUncachedPdfAndClosesWorkspace() throws Exception {
        UUID paperId = UUID.randomUUID();
        byte[] pdf = "%PDF-1.7".getBytes(StandardCharsets.US_ASCII);
        PaperTexAssembler.PaperTexWorkspace workspace =
                mock(PaperTexAssembler.PaperTexWorkspace.class);
        when(assembler.assemble(eq(paperId), any())).thenReturn(workspace);
        when(compileService.compile(workspace)).thenReturn(pdf);

        mockMvc.perform(post("/api/papers/{documentId}/preview", paperId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().bytes(pdf));

        verify(workspace).close();
    }

    @Test
    void previewReturnsStructuredCompileDiagnostics() throws Exception {
        UUID paperId = UUID.randomUUID();
        PaperTexAssembler.PaperTexWorkspace workspace =
                mock(PaperTexAssembler.PaperTexWorkspace.class);
        when(assembler.assemble(eq(paperId), any())).thenReturn(workspace);
        when(compileService.compile(workspace)).thenThrow(new TexCompileException(
                "TEX_COMPILE_FAILED",
                org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY,
                "Paper could not be compiled",
                List.of(new TexCompileException.Diagnostic(
                        "03-section.tex", 12, "Undefined control sequence"))));

        mockMvc.perform(post("/api/papers/{documentId}/preview", paperId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_PDF, MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("TEX_COMPILE_FAILED"))
                .andExpect(jsonPath("$.diagnostics[0].file").value("03-section.tex"))
                .andExpect(jsonPath("$.diagnostics[0].line").value(12))
                .andExpect(jsonPath("$.diagnostics[0].message")
                        .value("Undefined control sequence"));

        verify(workspace).close();
    }

    @Test
    void readsAndUpdatesEditablePaperShell() throws Exception {
        UUID paperId = UUID.randomUUID();
        when(assembler.getShell(paperId)).thenReturn(
                new PaperTexAssembler.PaperTexShell("old preamble", "old front matter", 3L));
        when(assembler.updateShell(paperId, "new preamble", "new front matter", 3L)).thenReturn(
                new PaperTexAssembler.PaperTexShell("new preamble", "new front matter", 4L));

        mockMvc.perform(get("/api/papers/{documentId}/tex-shell", paperId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preambleTex").value("old preamble"))
                .andExpect(jsonPath("$.frontMatterTex").value("old front matter"))
                .andExpect(jsonPath("$.version").value(3));

        mockMvc.perform(put("/api/papers/{documentId}/tex-shell", paperId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "preambleTex": "new preamble",
                                  "frontMatterTex": "new front matter",
                                  "version": 3
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preambleTex").value("new preamble"))
                .andExpect(jsonPath("$.frontMatterTex").value("new front matter"))
                .andExpect(jsonPath("$.version").value(4));
    }
}
