package com.evidencepilot.dto.request;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

public final class PagingRequest {

    private static final int MAX_SIZE = 100;

    private PagingRequest() {
    }

    public static Pageable pageable(
            int page,
            int size,
            String sort,
            Set<String> allowedSortFields,
            String defaultSort) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, MAX_SIZE));
        return PageRequest.of(safePage, safeSize, sort(sort, allowedSortFields, defaultSort));
    }

    private static Sort sort(String sort, Set<String> allowedSortFields, String defaultSort) {
        String requestedSort = sort == null || sort.isBlank() ? defaultSort : sort.trim();
        String[] parts = requestedSort.split(",", 2);
        String field = parts[0].trim();
        if (!allowedSortFields.contains(field)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unsupported sort field: " + field);
        }

        Sort.Direction direction = Sort.Direction.ASC;
        if (parts.length > 1 && !parts[1].isBlank()) {
            try {
                direction = Sort.Direction.fromString(parts[1].trim());
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Unsupported sort direction: " + parts[1].trim());
            }
        }
        return Sort.by(direction, field);
    }
}
