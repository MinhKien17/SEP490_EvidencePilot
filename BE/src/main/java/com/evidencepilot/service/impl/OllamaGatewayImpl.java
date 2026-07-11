package com.evidencepilot.service.impl;

import com.evidencepilot.config.infrastructure.RabbitMQConfig;
import com.evidencepilot.service.AiModelClient;
import com.evidencepilot.service.OllamaGateway;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class OllamaGatewayImpl implements OllamaGateway {

    private final RestClient restClient;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final String embeddingModel;
    private final String generationModel;

    public OllamaGatewayImpl(
            RabbitTemplate rabbitTemplate,
            ObjectMapper objectMapper,
            @Value("${ai.model.base-url}") String ollamaUrl,
            @Value("${ollama.embedding.model}") String embeddingModel,
            @Value("${ollama.generation.model}") String generationModel,
            @Value("${ai.model.read-timeout-seconds}") long readTimeoutSeconds) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        rabbitTemplate.setReplyTimeout(Duration.ofSeconds(Math.max(1, readTimeoutSeconds)).toMillis());

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
        try {
            byte[] body = objectMapper.writeValueAsBytes(Map.of(
                    "kind", "QUERY",
                    "text", text,
                    "model", embeddingModel));
            var request = MessageBuilder.withBody(body)
                    .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                    .setPriority(9)
                    .build();
            var reply = rabbitTemplate.sendAndReceive(
                    RabbitMQConfig.EXCHANGE,
                    RabbitMQConfig.ROUTING_KEY_EMBEDDING_REQUEST,
                    request);
            if (reply == null) {
                throw new IllegalStateException("Embedding worker timed out");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> response = objectMapper.readValue(reply.getBody(), Map.class);
            if (response.get("error") != null) {
                throw new IllegalStateException(String.valueOf(response.get("error")));
            }
            Object rawEmbedding = response.get("embedding");
            if (!(rawEmbedding instanceof List<?> values) || values.isEmpty()) {
                throw new IllegalStateException("Embedding response was null or empty");
            }
            return values.stream().map(value -> ((Number) value).floatValue()).toList();
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new AiModelClient.AiApiException("RabbitMQ embedding", 503, "AI model request failed", e);
        }
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

    record GenerateRequest(String prompt) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GenerateResponse(String response) {
    }
}
