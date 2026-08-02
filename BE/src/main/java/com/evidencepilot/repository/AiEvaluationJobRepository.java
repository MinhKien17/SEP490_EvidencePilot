package com.evidencepilot.repository;

import com.evidencepilot.model.AiEvaluationJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AiEvaluationJobRepository extends JpaRepository<AiEvaluationJob, UUID> {
    List<AiEvaluationJob> findByStatus(String status);
}
