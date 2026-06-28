package com.evidencepilot.service;

import com.evidencepilot.dto.QdrantSearchResult;
import com.evidencepilot.dto.SparseVector;

import java.util.List;
import java.util.Map;

public interface QdrantClient {

    void upsertVector(String chunkId, List<Float> denseVector, SparseVector sparseVector, String scopeType, String scopeId, Map<String, Object> extraPayload);

    String findClosestChunkId(List<Float> queryVector, String projectId);

    List<QdrantSearchResult> findClosestChunks(List<Float> queryVector, String scopeType, String scopeId, int topK);
}
