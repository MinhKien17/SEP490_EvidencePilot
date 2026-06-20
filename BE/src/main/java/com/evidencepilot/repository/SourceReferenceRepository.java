package com.evidencepilot.repository;

import com.evidencepilot.domain.entity.SourceReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SourceReferenceRepository extends JpaRepository<SourceReference, Integer> {

    List<SourceReference> findBySourceIdOrderByReferenceIndex(Integer sourceId);

    List<SourceReference> findBySourceProjectIdOrderBySourceIdAscReferenceIndexAsc(Integer projectId);

    List<SourceReference> findBySourceProjectIdAndSourceActiveTrueOrderBySourceIdAscReferenceIndexAsc(Integer projectId);
}
