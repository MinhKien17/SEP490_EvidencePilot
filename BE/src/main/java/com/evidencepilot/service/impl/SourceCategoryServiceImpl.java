package com.evidencepilot.service.impl;

import com.evidencepilot.dto.request.SourceCategoryRequest;
import com.evidencepilot.dto.response.SourceCategoryResponse;
import com.evidencepilot.exception.ResourceNotFoundException;
import com.evidencepilot.model.SourceCategory;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.repository.SourceCategoryRepository;
import com.evidencepilot.service.AuditService;
import com.evidencepilot.service.CurrentUserService;
import com.evidencepilot.service.SourceCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SourceCategoryServiceImpl implements SourceCategoryService {

    private static final String OTHER = "OTHER";
    private static final long MIN_ACTIVE_CATEGORIES = 3;

    private final SourceCategoryRepository sourceCategoryRepository;
    private final DocumentRepository documentRepository;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;

    @Override
    public List<SourceCategoryResponse> getActiveCategories() {
        return sourceCategoryRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(SourceCategoryResponse::from)
                .toList();
    }

    @Override
    public List<SourceCategoryResponse> getCategories(Boolean active) {
        List<SourceCategory> categories = active == null
                ? sourceCategoryRepository.findAll()
                : sourceCategoryRepository.findByActiveOrderByNameAsc(active);
        return categories.stream()
                .map(SourceCategoryResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public SourceCategoryResponse create(SourceCategoryRequest request) {
        String code = normalizeCode(request.code());
        String name = request.name().trim();
        if (sourceCategoryRepository.existsByCode(code)
                || sourceCategoryRepository.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Source category code or name already exists.");
        }
        SourceCategory category = new SourceCategory();
        category.setCode(code);
        category.setName(name);
        category.setDescription(request.description());
        category.setActive(true);
        category.setCreatedAt(LocalDateTime.now());
        category = sourceCategoryRepository.save(category);
        auditService.record(
                "SOURCE_CATEGORY_CREATED", "SOURCE_CATEGORY", category.getId(),
                currentUserService.requireCurrentUser(), null, safeValue(category));
        return SourceCategoryResponse.from(category);
    }

    @Override
    @Transactional
    public SourceCategoryResponse update(
            UUID id, SourceCategoryRequest request, Boolean active) {
        SourceCategory category = sourceCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(id, "Source category"));
        String code = normalizeCode(request.code());
        if (!category.getCode().equals(code)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Source category code is immutable.");
        }
        String name = request.name().trim();
        if (sourceCategoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Source category name already exists.");
        }
        if (Boolean.FALSE.equals(active) && category.isActive()) {
            requireCanDeactivate(category);
        }

        Map<String, Object> oldValue = safeValue(category);
        category.setName(name);
        category.setDescription(request.description());
        if (active != null) category.setActive(active);
        category = sourceCategoryRepository.save(category);
        auditService.record(
                "SOURCE_CATEGORY_UPDATED", "SOURCE_CATEGORY", category.getId(),
                currentUserService.requireCurrentUser(), oldValue, safeValue(category));
        return SourceCategoryResponse.from(category);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        SourceCategory category = sourceCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(id, "Source category"));
        if (!category.isActive()) return;
        requireCanDeactivate(category);
        Map<String, Object> oldValue = safeValue(category);
        category.setActive(false);
        sourceCategoryRepository.save(category);
        auditService.record(
                "SOURCE_CATEGORY_DELETED", "SOURCE_CATEGORY", category.getId(),
                currentUserService.requireCurrentUser(), oldValue, safeValue(category));
    }

    private void requireCanDeactivate(SourceCategory category) {
        if (OTHER.equals(category.getCode())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "OTHER is the protected fallback category.");
        }
        if (documentRepository.countBySourceCategoryId(category.getId()) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Source category is currently in use.");
        }
        if (sourceCategoryRepository.countByActiveTrue() <= MIN_ACTIVE_CATEGORIES) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "At least three Source categories must remain active.");
        }
    }

    private static String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private static Map<String, Object> safeValue(SourceCategory category) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("code", category.getCode());
        value.put("name", category.getName());
        value.put("description", category.getDescription());
        value.put("active", category.isActive());
        return value;
    }
}
