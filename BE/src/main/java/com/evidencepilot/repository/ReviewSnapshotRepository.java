package com.evidencepilot.repository;

import com.evidencepilot.model.ReviewSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReviewSnapshotRepository extends JpaRepository<ReviewSnapshot, UUID> {
    Optional<ReviewSnapshot> findByProjectIdAndStyleAndInputFingerprint(
            UUID projectId, String style, String inputFingerprint);
}
