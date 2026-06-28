package com.evidencepilot.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j
@Service
public class OllamaGateway {

    private final RestClient restClient;
    private final String embeddingModel;
    private final String generationModel;

    public OllamaGateway(
            @Value("${ollama.url:https://good-lumpish-headstone.ngrok-free.dev}") String ollamaUrl,
            @Value("${ollama.embedding.model:nomic-embed-text}") String embeddingModel,
            @Value("${ollama.generation.model:llama3}") String generationModel) {
        this.restClient = RestClient.builder()
                .baseUrl(ollamaUrl)
                .defaultHeader("ngrok-skip-browser-warning", "true")
                .build();
        this.embeddingModel = embeddingModel;
        this.generationModel = generationModel;
    }

    public List<Float> getEmbedding(String text) {
        var request = new EmbeddingRequest(embeddingModel, text);
        log.info("Requesting embedding from model={}", embeddingModel);

        EmbeddingResponse response = restClient.post()
                .uri("/ai/embeddings")
                .body(request)
                .retrieve()
                .body(EmbeddingResponse.class);

        if (response == null || response.embedding() == null) {
            log.warn("Embedding response was null or empty");
            return List.of();
        }

        log.info("Received embedding of size {}", response.embedding().size());
        return response.embedding();
    }

    public String generateEvaluation(String prompt) {
        var request = new GenerateRequest(prompt);
        log.info("Generating evaluation with model={}", generationModel);

        GenerateResponse response = restClient.post()
                .uri("/ai/generate")
                .body(request)
                .retrieve()
                .body(GenerateResponse.class);

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
