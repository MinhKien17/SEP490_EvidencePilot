package com.evidencepilot.controller;

import com.evidencepilot.domain.entity.Dataset;
import com.evidencepilot.domain.entity.User;
import com.evidencepilot.domain.enums.UserRole;
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
    public List<Dataset> findAll() {
        User currentUser = currentUserService.requireCurrentUser();
        if (currentUserService.isAdmin(currentUser)) {
            return datasetRepository.findAll();
        }
        if (currentUserService.isInstructor(currentUser)) {
            return datasetRepository.findByInstructorId(currentUser.getId());
        }
        return List.of();
    }

    @GetMapping("/{id}")
    public Dataset findById(@PathVariable Integer id) {
        User currentUser = currentUserService.requireCurrentUser();
        Dataset dataset = datasetRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Dataset not found: " + id));
        currentUserService.requireDatasetAccess(currentUser, dataset);
        return dataset;
    }

    @GetMapping("/by-instructor/{instructorId}")
    public List<Dataset> findByInstructor(@PathVariable Integer instructorId) {
        User currentUser = currentUserService.requireCurrentUser();
        currentUserService.requireUserIdOrAdmin(currentUser, instructorId);
        return datasetRepository.findByInstructorId(instructorId);
    }

    @PostMapping
    public ResponseEntity<Dataset> create(@RequestBody Dataset dataset) {
        User currentUser = currentUserService.requireCurrentUser();
        currentUserService.requireRole(currentUser, UserRole.INSTRUCTOR);
        if (!currentUserService.isAdmin(currentUser)) {
            dataset.setInstructor(currentUser);
        }
        Dataset saved = datasetRepository.save(dataset);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public Dataset update(@PathVariable Integer id, @RequestBody Dataset dataset) {
        User currentUser = currentUserService.requireCurrentUser();
        Dataset existing = datasetRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Dataset not found: " + id));
        currentUserService.requireDatasetAccess(currentUser, existing);
        dataset.setId(id);
        if (!currentUserService.isAdmin(currentUser)) {
            dataset.setInstructor(existing.getInstructor());
        }
        return datasetRepository.save(dataset);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        User currentUser = currentUserService.requireCurrentUser();
        Dataset existing = datasetRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Dataset not found: " + id));
        currentUserService.requireDatasetAccess(currentUser, existing);
        datasetRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
