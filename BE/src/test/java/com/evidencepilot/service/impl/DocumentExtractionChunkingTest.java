package com.evidencepilot.service.impl;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentExtractionChunkingTest {

    @Test
    void chunksLongTextWithOneHundredCharacterOverlap() {
        String text = "a".repeat(1200);

        var chunks = DocumentExtractionWorkerImpl.chunkText(text);

        assertThat(chunks).hasSize(2);
        assertThat(chunks.getFirst()).hasSize(1000);
        assertThat(chunks.get(1)).hasSize(300);
        assertThat(chunks.getFirst().substring(900)).isEqualTo(chunks.get(1).substring(0, 100));
    }
}
