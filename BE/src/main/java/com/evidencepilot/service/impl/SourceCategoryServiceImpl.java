package com.evidencepilot.service.impl;

import com.evidencepilot.dto.request.SourceCategoryRequest;
import com.evidencepilot.dto.response.SourceCategoryResponse;
import com.evidencepilot.exception.ResourceNotFoundException;
import com.evidencepilot.model.SourceCategory;
import com.evidencepilot.repository.SourceCategoryRepository;
import com.evidencepilot.service.SourceCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SourceCategoryServiceImpl implements SourceCategoryService {

    private final SourceCategoryRepository sourceCategoryRepository;

    @Override
    public List<SourceCategoryResponse> getActiveCategories() {
        return sourceCategoryRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(SourceCategoryResponse::from)
                .toList();
    }

    @Override
    public List<SourceCategoryResponse> getCategories(Boolean active) {
        var categories = active == null
                ? sourceCategoryRepository.findAll()
                : sourceCategoryRepository.findByActiveOrderByNameAsc(active);
        return categories.stream()
                .map(SourceCategoryResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public SourceCategoryResponse create(SourceCategoryRequest request) {
        String name = request.name().trim();
        if (sourceCategoryRepository.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Source category already exists");
        }

        SourceCategory category = new SourceCategory();
        category.setName(name);
        category.setDescription(request.description());
        category.setActive(true);
        category.setCreatedAt(LocalDateTime.now());
        return SourceCategoryResponse.from(sourceCategoryRepository.save(category));
    }

    @Override
    @Transactional
    public SourceCategoryResponse update(UUID id, SourceCategoryRequest request, Boolean active) {
        SourceCategory category = sourceCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(id, "Source category"));
        String name = request.name().trim();
        if (sourceCategoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Source category already exists");
        }

        category.setName(name);
        category.setDescription(request.description());
        if (active != null) {
            category.setActive(active);
        }
        return SourceCategoryResponse.from(sourceCategoryRepository.save(category));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        SourceCategory category = sourceCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(id, "Source category"));
        category.setActive(false);
        sourceCategoryRepository.save(category);
    }
}
