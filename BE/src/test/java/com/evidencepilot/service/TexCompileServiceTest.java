package com.evidencepilot.service;

import com.evidencepilot.exception.TexCompileException;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TexCompileServiceTest {

    @Test
    void reportsUnavailableCompilerWithoutLeavingWorkspaceOpen() throws Exception {
        var root = Files.createTempDirectory("missing-compiler-test-");
        Files.writeString(root.resolve("main.tex"), "\\documentclass{article}");
        var workspace = new PaperTexAssembler.PaperTexWorkspace(root);
        TexCompileService service = new TexCompileService(
                "missing-tectonic-executable", 1, true, 1);

        try (workspace) {
            assertThatThrownBy(() -> service.compile(workspace))
                    .isInstanceOf(TexCompileException.class)
                    .hasMessage("TeX compiler is not available");
        }
    }
}
