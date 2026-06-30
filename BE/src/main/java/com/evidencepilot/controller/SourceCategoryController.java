package com.evidencepilot.controller;

import com.evidencepilot.dto.request.SourceCategoryRequest;
import com.evidencepilot.dto.response.SourceCategoryResponse;
import com.evidencepilot.service.SourceCategoryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Source Categories", description = "Source category configuration")
public class SourceCategoryController {

    private final SourceCategoryService sourceCategoryService;

    @GetMapping("/api/source-categories")
    public List<SourceCategoryResponse> activeCategories() {
        return sourceCategoryService.getActiveCategories();
    }

    @GetMapping("/api/admin/source-categories")
    @PreAuthorize("hasRole('ADMIN')")
    public List<SourceCategoryResponse> adminCategories(@RequestParam(required = false) Boolean active) {
        return sourceCategoryService.getCategories(active);
    }

    @PostMapping("/api/admin/source-categories")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public SourceCategoryResponse create(@Valid @RequestBody SourceCategoryRequest request) {
        return sourceCategoryService.create(request);
    }

    @PutMapping("/api/admin/source-categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public SourceCategoryResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody SourceCategoryRequest request,
            @RequestParam(required = false) Boolean active) {
        return sourceCategoryService.update(id, request, active);
    }

    @DeleteMapping("/api/admin/source-categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable UUID id) {
        sourceCategoryService.delete(id);
    }
}
