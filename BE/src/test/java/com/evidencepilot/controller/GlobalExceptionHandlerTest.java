package com.evidencepilot.controller;

import com.evidencepilot.ai.AiModelClient;
import com.evidencepilot.dto.response.ApiErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleAiApiReturnsServiceUnavailable() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/ai/test");

        ResponseEntity<ApiErrorResponse> response = handler.handleAiApi(
                new AiModelClient.AiApiException("POST /ai/test", 503),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(503);
    }
}
