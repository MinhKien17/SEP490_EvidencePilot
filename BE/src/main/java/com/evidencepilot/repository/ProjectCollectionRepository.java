package com.evidencepilot.repository;

import com.evidencepilot.model.ProjectCollection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectCollectionRepository extends JpaRepository<ProjectCollection, UUID> {
    Optional<ProjectCollection> findByProjectIdAndCollectionId(UUID projectId, UUID collectionId);
    List<ProjectCollection> findByProjectId(UUID projectId);
    List<ProjectCollection> findByCollectionId(UUID collectionId);
}
