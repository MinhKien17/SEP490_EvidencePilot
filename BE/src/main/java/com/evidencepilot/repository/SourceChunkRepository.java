package com.evidencepilot.repository;

import com.evidencepilot.domain.entity.SourceChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SourceChunkRepository extends JpaRepository<SourceChunk, Integer> {

    List<SourceChunk> findBySourceIdAndActiveTrueOrderByChunkIndex(Integer sourceId);

    List<SourceChunk> findBySourceProjectIdAndSourceActiveTrueAndActiveTrueOrderBySourceIdAscChunkIndexAsc(Integer projectId);
}
