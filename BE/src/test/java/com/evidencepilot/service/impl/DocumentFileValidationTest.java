package com.evidencepilot.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentFileValidationTest {

    @Test
    void acceptsPdfAndDocxButRejectsLegacyDoc() {
        var pdf = new MockMultipartFile("file", "paper.pdf", "application/pdf", new byte[] {1});
        var docx = new MockMultipartFile(
                "file",
                "paper.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                new byte[] {1});
        var doc = new MockMultipartFile("file", "paper.doc", "application/msword", new byte[] {1});

        assertThatCode(() -> DocumentServiceImpl.validateFile(pdf)).doesNotThrowAnyException();
        assertThatCode(() -> DocumentServiceImpl.validateFile(docx)).doesNotThrowAnyException();
        assertThatThrownBy(() -> DocumentServiceImpl.validateFile(doc))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Only PDF and DOCX");
    }
}
