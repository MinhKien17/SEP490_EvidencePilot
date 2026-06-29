package com.evidencepilot.service;

import java.util.List;

public interface OllamaGateway {
    List<Float> getDenseEmbedding(String text);

    String generateEvaluation(String prompt);
}
