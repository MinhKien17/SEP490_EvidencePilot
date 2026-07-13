package com.evidencepilot.service;

import com.evidencepilot.service.impl.AiModelClientImpl;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
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
    void extractDocumentPostsPresignedUrlToExtractEndpoint() {
        RestClient.Builder builder = RestClient.builder()
                .defaultHeader("ngrok-skip-browser-warning", "true");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://ai.test/extract"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("ngrok-skip-browser-warning", "true"))
                .andExpect(content().json("""
                        {
                          "filename":"source.pdf",
                          "download_url":"https://storage.test/source.pdf"
                        }
                        """))
                .andRespond(withSuccess(
                        """
                        {
                          "markdown":"# Extracted",
                          "blocks":[
                            {"type":"heading","text":"Extracted","level":1},
                            {"type":"paragraph","text":"Body"}
                          ]
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        AiModelClientImpl client = new AiModelClientImpl(builder.build(), "http://ai.test");

        AiModelClient.ExtractedDocument result = client.extractDocument(
                "source.pdf", "https://storage.test/source.pdf");

        assertThat(result.markdown()).isEqualTo("# Extracted");
        assertThat(result.blocks()).extracting(AiModelClient.ExtractionBlock::type)
                .containsExactly("heading", "paragraph");
        server.verify();
    }

    @Test
    void extractDocumentRejectsLegacyMarkdownOnlyResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://ai.test/extract"))
                .andRespond(withSuccess(
                        "{\"filename\":\"source.pdf\",\"method\":\"mineru\",\"markdown\":\"# Extracted\"}",
                        MediaType.APPLICATION_JSON));

        AiModelClientImpl client = new AiModelClientImpl(builder.build(), "http://ai.test");

        assertThatThrownBy(() -> client.extractDocument("source.pdf", "https://storage.test/source.pdf"))
                .isInstanceOf(AiModelClient.AiApiException.class)
                .hasMessageContaining("blocks");
    }

    @Test
    void generateEmbeddingConvertsNumericArray() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://ai.test/ai/embeddings"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"embedding\":[0.25,-0.5,1]}", MediaType.APPLICATION_JSON));

        assertThat(new AiModelClientImpl(builder.build(), "http://ai.test").generateEmbedding("text"))
                .containsExactly(0.25f, -0.5f, 1.0f);
        server.verify();
    }

    @Test
    void generateEmbeddingsReturnsBatchInOrder() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://ai.test/ai/embeddings/batch"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"embeddings\":[[0.25,-0.5],[1,2]]}",
                        MediaType.APPLICATION_JSON));

        assertThat(new AiModelClientImpl(builder.build(), "http://ai.test")
                .generateEmbeddings(List.of("one", "two")))
                .containsExactly(List.of(0.25f, -0.5f), List.of(1.0f, 2.0f));
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
