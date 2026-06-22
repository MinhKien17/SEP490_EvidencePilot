package com.evidencepilot.controller;

import com.evidencepilot.ai.AiModelClient;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HealthControllerTest {

    private final AiModelClient aiModelClient = mock(AiModelClient.class);
    private final HealthController controller = new HealthController(aiModelClient);

    @Test
    void healthCallsAiWorkerHealthEndpoint() {
        Map<String, Object> aiHealth = Map.of(
                "status", "ok",
                "model", "evidencopilot:latest"
        );
        when(aiModelClient.health()).thenReturn(aiHealth);

        Map<String, Object> response = controller.health();

        assertThat(response)
                .containsEntry("status", "ok")
                .containsEntry("ai", aiHealth);
        verify(aiModelClient).health();
    }
}
