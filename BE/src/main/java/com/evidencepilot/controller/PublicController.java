package com.evidencepilot.controller;

import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.repository.ProjectRepository;
import com.evidencepilot.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "Public", description = "Anonymous public data")
public class PublicController {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final DocumentRepository documentRepository;

    @Operation(summary = "Aggregate platform stats for the landing page")
    @GetMapping("/api/public/stats")
    public Map<String, Object> stats() {
        return Map.of(
                "totalUsers", userRepository.count(),
                "totalProjects", projectRepository.countByActiveTrue(),
                "totalDocuments", documentRepository.count()
        );
    }
}
