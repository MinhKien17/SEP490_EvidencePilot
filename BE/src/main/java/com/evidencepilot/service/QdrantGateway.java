package com.evidencepilot.service;

import com.evidencepilot.dto.SparseVector;

import java.util.List;
import java.util.UUID;

public interface QdrantGateway {

    List<String> searchDocumentContext(UUID documentId, List<Float> denseVector, SparseVector sparseVector, int topK);
}
