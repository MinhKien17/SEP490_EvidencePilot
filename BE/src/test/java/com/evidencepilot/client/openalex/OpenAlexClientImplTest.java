package com.evidencepilot.client.openalex;

import com.evidencepilot.dto.openalex.OpenAlexWorkResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayInputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenAlexClientImplTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RestClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private final OpenAlexWorkResponse sampleWork = new OpenAlexWorkResponse(
            "https://openalex.org/W123",
            "https://doi.org/10.1000/xyz123",
            "Test Paper Title",
            List.of(
                    new OpenAlexWorkResponse.OpenAlexAuthor(
                            new OpenAlexWorkResponse.Author("Alice Smith")),
                    new OpenAlexWorkResponse.OpenAlexAuthor(
                            new OpenAlexWorkResponse.Author("Bob Jones"))
            ),
            new OpenAlexWorkResponse.OpenAlexPrimaryLocation(
                    new OpenAlexWorkResponse.OpenAlexSource(
                            "Test Publisher", "Test Organization", "journal",
                            "https://example.com"),
                    "https://example.com/paper.pdf",
                    "https://example.com/paper",
                    "cc-by",
                    "acceptedVersion",
                    true
            ),
            null,
            new OpenAlexWorkResponse.OpenAlexOpenAccess(true, "green", "https://example.com/paper.pdf", true),
            null,
            2024
    );

    private static final String BASE = "https://api.openalex.org";

    @Test
    void fetchWork_returnsDeserializedResponse() {
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(BASE + "/works/doi:10.1000/xyz123")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(OpenAlexWorkResponse.class)).thenReturn(sampleWork);

        OpenAlexClientImpl client = new OpenAlexClientImpl(restClient, BASE);
        OpenAlexWorkResponse result = client.fetchWork("10.1000/xyz123");

        assertThat(result.title()).isEqualTo("Test Paper Title");
        assertThat(result.publicationYear()).isEqualTo(2024);
        assertThat(result.oaUrl()).isEqualTo("https://example.com/paper.pdf");
        assertThat(result.authorNames()).containsExactly("Alice Smith", "Bob Jones");
    }

    @Test
    void fetchWork_throwsOnNullResponse() {
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(BASE + "/works/doi:10.1000/bad-doi")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(OpenAlexWorkResponse.class)).thenReturn(null);

        OpenAlexClientImpl client = new OpenAlexClientImpl(restClient, BASE);

        assertThatThrownBy(() -> client.fetchWork("10.1000/bad-doi"))
                .isInstanceOf(OpenAlexClient.OpenAlexApiException.class)
                .hasMessageContaining("null");
    }

    @Test
    void fetchWork_prependsDoiPrefixWhenMissing() {
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(BASE + "/works/doi:10.1000/xyz")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(OpenAlexWorkResponse.class)).thenReturn(sampleWork);

        OpenAlexClientImpl client = new OpenAlexClientImpl(restClient, BASE);
        OpenAlexWorkResponse result = client.fetchWork("10.1000/xyz");

        assertThat(result).isNotNull();
    }

    @Test
    void fetchWork_handlesDoiWithPrefix() {
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(BASE + "/works/doi:10.1000/xyz")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(OpenAlexWorkResponse.class)).thenReturn(sampleWork);

        OpenAlexClientImpl client = new OpenAlexClientImpl(restClient, BASE);
        OpenAlexWorkResponse result = client.fetchWork("doi:10.1000/xyz");

        assertThat(result).isNotNull();
    }

    @Test
    void downloadPdf_returnsInputStream() throws Exception {
        byte[] pdfBytes = "fake-pdf-content".getBytes();

        HttpResponse<ByteArrayInputStream> httpResponse = mock();
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(new ByteArrayInputStream(pdfBytes));

        HttpClient httpClient = mock();
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        OpenAlexClientImpl client = new OpenAlexClientImpl(restClient, BASE, httpClient);
        try (var result = client.downloadPdf("https://example.com/file.pdf")) {
            assertThat(result).isNotNull();
            assertThat(result.readAllBytes()).isEqualTo(pdfBytes);
        }
    }
}
