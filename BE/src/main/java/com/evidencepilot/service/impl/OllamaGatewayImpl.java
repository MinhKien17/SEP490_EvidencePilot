package com.evidencepilot.service.impl;

import com.evidencepilot.service.AiModelClient;
import com.evidencepilot.service.OllamaGateway;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
public class OllamaGatewayImpl implements OllamaGateway {

    private final RestClient restClient;
    private final String embeddingModel;
    private final String generationModel;

    public OllamaGatewayImpl(
            @Value("${ollama.url:${AI_MODEL_BASE_URL:https://good-lumpish-headstone.ngrok-free.dev}}") String ollamaUrl,
            @Value("${ollama.embedding.model:nomic-embed-text}") String embeddingModel,
            @Value("${ollama.generation.model:evidencopilot:latest}") String generationModel,
            @Value("${ollama.read-timeout-seconds:${ai.model.read-timeout-seconds:300}}") long readTimeoutSeconds) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(Math.max(1, readTimeoutSeconds)));

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl(ollamaUrl)
                .defaultHeader("ngrok-skip-browser-warning", "true")
                .build();
        this.embeddingModel = embeddingModel;
        this.generationModel = generationModel;
    }

    @Override
    public List<Float> getDenseEmbedding(String text) {
        var request = new EmbeddingRequest(embeddingModel, text);
        log.info("Requesting embedding from model={}", embeddingModel);

        EmbeddingResponse response;
        try {
            response = restClient.post()
                    .uri("/ai/embeddings")
                    .body(request)
                    .retrieve()
                    .body(EmbeddingResponse.class);
        } catch (RestClientException e) {
            throw new AiModelClient.AiApiException("/ai/embeddings", 503, "AI model request failed", e);
        }

        if (response == null || response.embedding() == null || response.embedding().isEmpty()) {
            log.warn("Embedding response was null or empty");
            throw new IllegalStateException("Embedding response was null or empty");
        }

        log.info("Received embedding of size {}", response.embedding().size());
        return response.embedding();
    }

    @Override
    public String generateEvaluation(String prompt) {
        var request = new GenerateRequest(prompt);
        log.info("Generating evaluation with model={}", generationModel);

        GenerateResponse response;
        try {
            response = restClient.post()
                    .uri("/ai/generate")
                    .body(request)
                    .retrieve()
                    .body(GenerateResponse.class);
        } catch (RestClientException e) {
            throw new AiModelClient.AiApiException("/ai/generate", 503, "AI model request failed", e);
        }

        if (response == null || response.response() == null) {
            log.warn("Generate response was null");
            return "";
        }

        log.info("Generated evaluation of length {}", response.response().length());
        return response.response();
    }

    record EmbeddingRequest(String model, String text) {
    }

    record EmbeddingResponse(List<Float> embedding) {
    }

    record GenerateRequest(String prompt) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GenerateResponse(String response) {
    }
}
