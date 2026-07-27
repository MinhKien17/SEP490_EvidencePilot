package com.evidencepilot.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentFileValidationTest {

    @Test
    void acceptsPdfDocxAndMarkdownButRejectsTexAndLegacyDoc() {
        var pdf = new MockMultipartFile(
                "file", "source.pdf", "application/pdf", new byte[] {1});
        var docx = new MockMultipartFile(
                "file",
                "source.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                new byte[] {1});
        var md = new MockMultipartFile(
                "file", "source.md", "text/plain", new byte[] {1});
        var mdWithCharset = new MockMultipartFile(
                "file", "source.md", "text/plain; charset=utf-8", new byte[] {1});
        var markdown = new MockMultipartFile(
                "file", "source.markdown", "text/markdown", new byte[] {1});
        var tex = new MockMultipartFile(
                "file", "source.tex", "application/x-tex", new byte[] {1});
        var doc = new MockMultipartFile(
                "file", "source.doc", "application/msword", new byte[] {1});

        assertThatCode(() -> DocumentServiceImpl.validateFile(pdf)).doesNotThrowAnyException();
        assertThatCode(() -> DocumentServiceImpl.validateFile(docx)).doesNotThrowAnyException();
        assertThatCode(() -> DocumentServiceImpl.validateFile(md)).doesNotThrowAnyException();
        assertThatCode(() -> DocumentServiceImpl.validateFile(mdWithCharset)).doesNotThrowAnyException();
        assertThatCode(() -> DocumentServiceImpl.validateFile(markdown)).doesNotThrowAnyException();
        assertThatThrownBy(() -> DocumentServiceImpl.validateFile(tex))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Only PDF, DOCX, and Markdown");
        assertThatThrownBy(() -> DocumentServiceImpl.validateFile(doc))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Only PDF, DOCX, and Markdown");
    }
}
