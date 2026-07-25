package com.evidencepilot.service;

import com.evidencepilot.dto.request.CollectionRequest;
import com.evidencepilot.dto.response.CollectionResponse;
import com.evidencepilot.dto.response.PagedResponse;

import java.util.UUID;

public interface CollectionService {

    CollectionResponse createCollection(CollectionRequest request);

    CollectionResponse getCollectionById(UUID id);

    CollectionResponse updateCollection(UUID id, CollectionRequest request);

    PagedResponse<CollectionResponse> getMyCollections(int page, int size, String sort, String q, UUID categoryId);

    void deleteCollection(UUID id);
}
