package com.evidencepilot.service;

import com.evidencepilot.dto.SparseVector;

import java.util.List;

public interface OllamaGateway {
    List<Float> getEmbedding(String text);
    SparseVector getSparseEmbedding(String text);
    String generateEvaluation(String prompt);
}
