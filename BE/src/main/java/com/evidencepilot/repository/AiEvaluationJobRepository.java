package com.evidencepilot.repository;

import com.evidencepilot.model.AiEvaluationJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Collection;
import java.util.UUID;

public interface AiEvaluationJobRepository extends JpaRepository<AiEvaluationJob, UUID> {
    List<AiEvaluationJob> findByStatus(String status);

    List<AiEvaluationJob> findTop10ByProjectIdAndKindAndStatusOrderByCompletedAtDesc(
            UUID projectId, String kind, String status);

    List<AiEvaluationJob> findByProjectIdAndKindAndStatusInOrderByCreatedAtDesc(
            UUID projectId, String kind, Collection<String> statuses);
}
