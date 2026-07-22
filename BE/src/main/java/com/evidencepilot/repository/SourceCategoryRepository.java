package com.evidencepilot.repository;

import com.evidencepilot.model.SourceCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SourceCategoryRepository extends JpaRepository<SourceCategory, UUID> {
    List<SourceCategory> findByActiveTrueOrderByNameAsc();
    List<SourceCategory> findByActiveOrderByNameAsc(boolean active);
    Optional<SourceCategory> findByIdAndActiveTrue(UUID id);
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);
    long countByActiveTrue();
}
