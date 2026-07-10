package com.evidencepilot.service;

import com.evidencepilot.dto.request.SourceCategoryRequest;
import com.evidencepilot.dto.response.SourceCategoryResponse;

import java.util.List;
import java.util.UUID;

public interface SourceCategoryService {
    List<SourceCategoryResponse> getActiveCategories();
    List<SourceCategoryResponse> getCategories(Boolean active);
    SourceCategoryResponse create(SourceCategoryRequest request);
    SourceCategoryResponse update(UUID id, SourceCategoryRequest request, Boolean active);
    void delete(UUID id);
}
