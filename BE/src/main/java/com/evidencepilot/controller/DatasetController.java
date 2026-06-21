package com.evidencepilot.controller;

import com.evidencepilot.domain.entity.Dataset;
import com.evidencepilot.domain.entity.User;
import com.evidencepilot.domain.enums.UserRole;
import com.evidencepilot.dto.response.DatasetResponseDto;
import com.evidencepilot.repository.DatasetRepository;
import com.evidencepilot.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * REST controller for Dataset CRUD operations.
 * Base path: /api/datasets
 */
@RestController
@RequestMapping("/api/datasets")
@RequiredArgsConstructor
public class DatasetController {

    private final DatasetRepository datasetRepository;
    private final CurrentUserService currentUserService;

    @GetMapping
    public List<DatasetResponseDto> findAll() {
        User currentUser = currentUserService.requireCurrentUser();
        List<Dataset> datasets;
        if (currentUserService.isAdmin(currentUser)) {
            datasets = datasetRepository.findByActiveTrue();
        } else if (currentUserService.isInstructor(currentUser)) {
            datasets = datasetRepository.findByInstructorIdAndActiveTrue(currentUser.getId());
        } else {
            datasets = List.of();
        }
        return datasets.stream().map(DatasetResponseDto::fromEntity).toList();
    }

    @GetMapping("/{id}")
    public DatasetResponseDto findById(@PathVariable Integer id) {
        User currentUser = currentUserService.requireCurrentUser();
        Dataset dataset = datasetRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Dataset not found: " + id));
        if (!dataset.isActive()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Dataset not found: " + id);
        }
        currentUserService.requireDatasetAccess(currentUser, dataset);
        return DatasetResponseDto.fromEntity(dataset);
    }

    @GetMapping("/by-instructor/{instructorId}")
    public List<DatasetResponseDto> findByInstructor(@PathVariable Integer instructorId) {
        User currentUser = currentUserService.requireCurrentUser();
        currentUserService.requireUserIdOrAdmin(currentUser, instructorId);
        return datasetRepository.findByInstructorIdAndActiveTrue(instructorId).stream()
                .map(DatasetResponseDto::fromEntity)
                .toList();
    }

    @PostMapping
    public ResponseEntity<DatasetResponseDto> create(@RequestBody Dataset dataset) {
        User currentUser = currentUserService.requireCurrentUser();
        currentUserService.requireRole(currentUser, UserRole.INSTRUCTOR);
        if (!currentUserService.isAdmin(currentUser)) {
            dataset.setInstructor(currentUser);
        }
        Dataset saved = datasetRepository.save(dataset);
        return ResponseEntity.status(HttpStatus.CREATED).body(DatasetResponseDto.fromEntity(saved));
    }

    @PutMapping("/{id}")
    public DatasetResponseDto update(@PathVariable Integer id, @RequestBody Dataset dataset) {
        User currentUser = currentUserService.requireCurrentUser();
        Dataset existing = datasetRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Dataset not found: " + id));
        currentUserService.requireDatasetAccess(currentUser, existing);
        dataset.setId(id);
        if (!currentUserService.isAdmin(currentUser)) {
            dataset.setInstructor(existing.getInstructor());
        }
        Dataset saved = datasetRepository.save(dataset);
        return DatasetResponseDto.fromEntity(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        User currentUser = currentUserService.requireCurrentUser();
        Dataset existing = datasetRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Dataset not found: " + id));
        currentUserService.requireDatasetAccess(currentUser, existing);
        existing.setActive(false);
        datasetRepository.save(existing);
        return ResponseEntity.noContent().build();
    }
}
