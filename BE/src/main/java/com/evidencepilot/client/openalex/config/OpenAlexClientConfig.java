package com.evidencepilot.client.openalex.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class OpenAlexClientConfig {

    @Value("${openalex.api-base-url}")
    private String baseUrl;

    @Value("${openalex.api-key:}")
    private String apiKey;

    @Value("${openalex.user-agent}")
    private String userAgent;

    @Value("${openalex.read-timeout-seconds:30}")
    private long readTimeoutSeconds;

    @Bean("openAlexBaseUrl")
    public String openAlexBaseUrl() {
        return baseUrl;
    }

    @Bean("openAlexRestClient")
    public RestClient openAlexRestClient() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(Math.max(1, readTimeoutSeconds)));

        return RestClient.builder()
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.USER_AGENT, userAgent)
                .build();
    }

    @Bean("openAlexApiKey")
    public String openAlexApiKey() {
        return apiKey;
    }
}
