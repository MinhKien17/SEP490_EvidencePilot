package com.evidencepilot.service;

import com.evidencepilot.dto.request.CollectionCategoryRequest;
import com.evidencepilot.dto.response.CollectionCategoryResponse;

import java.util.List;
import java.util.UUID;

public interface CollectionCategoryService {
    List<CollectionCategoryResponse> getActiveCategories();
    List<CollectionCategoryResponse> getCategories(Boolean active);
    CollectionCategoryResponse create(CollectionCategoryRequest request);
    CollectionCategoryResponse update(UUID id, CollectionCategoryRequest request, Boolean active);
    void delete(UUID id);
}
