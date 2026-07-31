package com.evidencepilot.service.impl;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaperProcessingServiceImplExtractJsonTest {

    @Test
    void extractJsonRecoversBalancedJsonFromNoisyLlmOutput() {
        assertThat(PaperProcessingServiceImpl.extractJson(
                "Sure! Here it is:\n```json\n{\"findings\":[{\"type\":\"OTHER\"}]}\n```\nHope this helps!"))
                .isEqualTo("{\"findings\":[{\"type\":\"OTHER\"}]}");
        assertThat(PaperProcessingServiceImpl.extractJson(
                "{\"a\":\"brace } inside string\",\"b\":{\"c\":1}} trailing text"))
                .isEqualTo("{\"a\":\"brace } inside string\",\"b\":{\"c\":1}}");
        assertThat(PaperProcessingServiceImpl.extractJson("[{\"x\":1},{\"x\":2}]"))
                .isEqualTo("[{\"x\":1},{\"x\":2}]");
        assertThat(PaperProcessingServiceImpl.extractJson("not-json")).isEqualTo("not-json");
        assertThat(PaperProcessingServiceImpl.extractJson(null)).isEmpty();
    }
}
