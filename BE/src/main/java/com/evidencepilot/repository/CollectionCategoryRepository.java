package com.evidencepilot.repository;

import com.evidencepilot.model.CollectionCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CollectionCategoryRepository extends JpaRepository<CollectionCategory, UUID> {
    List<CollectionCategory> findByActiveTrueOrderByNameAsc();
    List<CollectionCategory> findByActiveOrderByNameAsc(boolean active);
    Optional<CollectionCategory> findByIdAndActiveTrue(UUID id);
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);
    long countByActiveTrue();
}
