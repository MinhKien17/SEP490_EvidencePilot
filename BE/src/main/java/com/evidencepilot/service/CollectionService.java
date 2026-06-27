package com.evidencepilot.service;

import com.evidencepilot.dto.request.CollectionRequest;
import com.evidencepilot.dto.response.CollectionResponse;
import com.evidencepilot.dto.response.PagedResponse;

import java.util.List;
import java.util.UUID;

public interface CollectionService {

    CollectionResponse createCollection(CollectionRequest request);

    CollectionResponse getCollectionById(UUID id);

    List<CollectionResponse> getCollectionsByProjectId(UUID projectId);
    PagedResponse<CollectionResponse> getCollectionsByProjectId(
            UUID projectId,
            int page,
            int size,
            String sort,
            String q,
            Boolean active);

    void deleteCollection(UUID id);
}
