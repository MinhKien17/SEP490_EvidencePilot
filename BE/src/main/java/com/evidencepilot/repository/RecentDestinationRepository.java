package com.evidencepilot.repository;

import com.evidencepilot.model.RecentDestination;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RecentDestinationRepository extends JpaRepository<RecentDestination, UUID> {
    List<RecentDestination> findByUserIdOrderByLastOpenedAtDesc(UUID userId);

    List<RecentDestination> findByUserIdAndKindAndRefIdAndTab(UUID userId, String kind, UUID refId, String tab);
}
