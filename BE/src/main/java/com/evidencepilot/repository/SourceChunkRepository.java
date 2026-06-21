package com.evidencepilot.repository;

import com.evidencepilot.domain.entity.SourceChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SourceChunkRepository extends JpaRepository<SourceChunk, Integer> {

    List<SourceChunk> findBySourceId(Integer sourceId);

    List<SourceChunk> findBySourceProjectId(Integer projectId);

    List<SourceChunk> findBySourceDatasetId(Integer datasetId);

    long countSourceId(Integer sourceId);
}
