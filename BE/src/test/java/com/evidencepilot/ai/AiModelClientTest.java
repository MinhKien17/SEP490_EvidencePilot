package com.evidencepilot.ai;

import com.evidencepilot.ai.dto.ClaimMatchRequest;
import com.evidencepilot.ai.dto.ClaimMatchResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AiModelClientTest {

    @Test
    void matchClaimTriesLocalThenNgrokThenConfiguredFallback() {
        RestClient.Builder builder = RestClient.builder()
                .defaultHeader("ngrok-skip-browser-warning", "true");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiModelClient client = new AiModelClient(builder.build(), List.of(
                "http://127.0.0.1:8000",
                "https://good-lumpish-headstone.ngrok-free.dev",
                "https://configured-ai.example.test"
        ));

        server.expect(requestTo("http://127.0.0.1:8000/match/claim"))
                .andRespond(withServerError());
        server.expect(requestTo("https://good-lumpish-headstone.ngrok-free.dev/match/claim"))
                .andRespond(withServerError());
        server.expect(requestTo("https://configured-ai.example.test/match/claim"))
                .andExpect(header("ngrok-skip-browser-warning", "true"))
                .andRespond(withSuccess("""
                        {
                          "claim": "test claim",
                          "matches": []
                        }
                        """, MediaType.APPLICATION_JSON));

        ClaimMatchResponse response = client.matchClaim(ClaimMatchRequest.of("test claim", 1));

        assertThat(response.claim()).isEqualTo("test claim");
        server.verify();
    }
}
