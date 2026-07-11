package com.evidencepilot.service;

import com.evidencepilot.service.impl.AiModelClientImpl;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AiModelClientTest {

    @Test
    void healthReturnsWorkerPayload() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://ai.test/health"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"status\":\"ok\"}", MediaType.APPLICATION_JSON));

        assertThat(new AiModelClientImpl(builder.build(), "http://ai.test/").health())
                .containsEntry("status", "ok");
        server.verify();
    }

    @Test
    void generateReturnsResponseFieldFromGenerateEndpoint() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://ai.test/ai/generate"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        """
                        {"model":"evidencopilot:latest","response":"Review text","done":true}
                        """,
                        MediaType.APPLICATION_JSON));

        AiModelClientImpl client = new AiModelClientImpl(builder.build(), "http://ai.test");

        assertThat(client.generate("Review this")).isEqualTo("Review text");
        server.verify();
    }

    @Test
    void processClaimPostsToClaimEndpoint() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://ai.test/process/claim"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"verdict\":\"supported\"}", MediaType.APPLICATION_JSON));

        var response = new AiModelClientImpl(builder.build(), "http://ai.test")
                .processClaim(UUID.randomUUID(), "claim", UUID.randomUUID(), "excerpt");

        assertThat(response).containsEntry("verdict", "supported");
        server.verify();
    }

    @Test
    void generateEmbeddingConvertsNumericArray() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://ai.test/ai/embeddings"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"embedding\":[0.25,-0.5,1]}", MediaType.APPLICATION_JSON));

        assertThat(new AiModelClientImpl(builder.build(), "http://ai.test").generateEmbedding("text"))
                .containsExactly(0.25, -0.5, 1.0);
        server.verify();
    }

    @Test
    void missingBaseUrlAndEmptyResponsesThrowAiApiException() {
        assertThatThrownBy(() -> new AiModelClientImpl(RestClient.create(), " ").health())
                .isInstanceOf(AiModelClient.AiApiException.class)
                .hasMessageContaining("not configured");

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://ai.test/ai/generate"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> new AiModelClientImpl(builder.build(), "http://ai.test").generate("prompt"))
                .isInstanceOf(AiModelClient.AiApiException.class)
                .hasMessageContaining("empty response");
    }
}
