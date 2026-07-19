package com.evidencepilot.client.openalex;

import com.evidencepilot.dto.openalex.OpenAlexWorkResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class OpenAlexClientImpl implements OpenAlexClient {

    private final RestClient restClient;
    private final String baseUrl;
    private final String apiKey;
    private final HttpClient httpClient;

    @org.springframework.beans.factory.annotation.Autowired
    public OpenAlexClientImpl(
            @Qualifier("openAlexRestClient") RestClient restClient,
            @Qualifier("openAlexBaseUrl") String baseUrl,
            @Qualifier("openAlexApiKey") String apiKey) {
        this(restClient, baseUrl, apiKey, HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(15))
                .build());
    }

    OpenAlexClientImpl(RestClient restClient, String baseUrl, String apiKey, HttpClient httpClient) {
        this.restClient = restClient;
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.apiKey = apiKey;
        this.httpClient = httpClient;
    }

    @Override
    public OpenAlexWorkResponse fetchWork(String doi) {
        String openAlexId = DoiUtils.toOpenAlexId(doi);
        if (openAlexId == null) {
            throw new OpenAlexApiException("Invalid DOI: " + doi, 0);
        }

        String uri = baseUrl + "/works/" + openAlexId;
        if (apiKey != null && !apiKey.isBlank()) {
            uri += "?api_key=" + apiKey;
        }

        log.info("Fetching OpenAlex work: {}/works/{}", baseUrl, openAlexId);
        OpenAlexWorkResponse response = restClient.get()
                .uri(uri)
                .retrieve()
                .body(OpenAlexWorkResponse.class);

        if (response == null) {
            throw new OpenAlexApiException("OpenAlex returned null response for DOI: " + doi, 0);
        }
        return response;
    }

    @Override
    public InputStream downloadPdf(String oaUrl) {
        log.info("Downloading PDF from: {}", oaUrl);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(java.net.URI.create(oaUrl))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Sec-Fetch-Dest", "document")
                    .header("Sec-Fetch-Mode", "navigate")
                    .header("Sec-Fetch-Site", "none")
                    .header("Sec-Fetch-User", "?1")
                    .header("Upgrade-Insecure-Requests", "1")
                    .timeout(Duration.ofSeconds(120))
                    .build();
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() >= 200 && response.statusCode() < 400) {
                return response.body();
            }
            throw new OpenAlexApiException(
                    "Download failed: HTTP " + response.statusCode() + " for " + oaUrl,
                    response.statusCode());
        } catch (OpenAlexApiException e) {
            throw e;
        } catch (Exception e) {
            throw new OpenAlexApiException("Failed to download PDF from " + oaUrl, e);
        }
    }

    private static String trimTrailingSlash(String url) {
        if (url == null || url.isBlank()) return "";
        String normalized = url.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
