package com.evidencepilot.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AiModelClientTest {

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

        AiModelClient client = new AiModelClient(builder.build(), "http://ai.test");

        assertThat(client.generate("Review this")).isEqualTo("Review text");
        server.verify();
    }

    @Test
    void extractDocumentPostsMultipartFileToExtractEndpoint() {
        RestClient.Builder builder = RestClient.builder()
                .defaultHeader("ngrok-skip-browser-warning", "true");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://ai.test/extract"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("ngrok-skip-browser-warning", "true"))
                .andRespond(withSuccess(
                        """
                        {"filename":"source.pdf","method":"liteparse","markdown":"# Extracted"}
                        """,
                        MediaType.APPLICATION_JSON));

        AiModelClient client = new AiModelClient(builder.build(), "http://ai.test");

        AiModelClient.ExtractedDocument result = client.extractDocument(
                "source.pdf",
                "application/pdf",
                "%PDF".getBytes());

        assertThat(result.filename()).isEqualTo("source.pdf");
        assertThat(result.method()).isEqualTo("liteparse");
        assertThat(result.markdown()).isEqualTo("# Extracted");
        server.verify();
    }
}
