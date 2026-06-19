package com.evidencepilot.controller;

import com.evidencepilot.domain.entity.Project;
import com.evidencepilot.domain.entity.User;
import com.evidencepilot.repository.ProjectRepository;
import com.evidencepilot.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * REST controller for Project CRUD operations.
 * Base path: /api/projects
 */
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectRepository projectRepository;
    private final CurrentUserService currentUserService;

    @GetMapping
    public List<Project> findAll() {
        User currentUser = currentUserService.requireCurrentUser();
        if (currentUserService.isAdmin(currentUser)) {
            return projectRepository.findAll();
        }
        return projectRepository.findByStudentId(currentUser.getId());
    }

    @GetMapping("/{id}")
    public Project findById(@PathVariable Integer id) {
        User currentUser = currentUserService.requireCurrentUser();
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Project not found: " + id));
        currentUserService.requireProjectAccess(currentUser, project);
        return project;
    }

    @GetMapping("/by-student/{studentId}")
    public List<Project> findByStudent(@PathVariable Integer studentId) {
        User currentUser = currentUserService.requireCurrentUser();
        currentUserService.requireUserIdOrAdmin(currentUser, studentId);
        return projectRepository.findByStudentId(studentId);
    }

    @PostMapping
    public ResponseEntity<Project> create(@RequestBody Project project) {
        User currentUser = currentUserService.requireCurrentUser();
        if (!currentUserService.isAdmin(currentUser)) {
            project.setStudent(currentUser);
        }
        Project saved = projectRepository.save(project);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public Project update(@PathVariable Integer id, @RequestBody Project project) {
        User currentUser = currentUserService.requireCurrentUser();
        Project existing = projectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Project not found: " + id));
        currentUserService.requireProjectWriteAccess(currentUser, existing);
        project.setId(id);
        if (!currentUserService.isAdmin(currentUser)) {
            project.setStudent(existing.getStudent());
        }
        if (project.getStatus() == null) {
            project.setStatus(existing.getStatus());
        }
        return projectRepository.save(project);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        User currentUser = currentUserService.requireCurrentUser();
        Project existing = projectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Project not found: " + id));
        currentUserService.requireProjectWriteAccess(currentUser, existing);
        projectRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
