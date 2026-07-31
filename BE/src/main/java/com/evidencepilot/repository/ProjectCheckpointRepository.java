package com.evidencepilot.repository;

import com.evidencepilot.model.ProjectCheckpoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProjectCheckpointRepository extends JpaRepository<ProjectCheckpoint, UUID> {
    List<ProjectCheckpoint> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
}
