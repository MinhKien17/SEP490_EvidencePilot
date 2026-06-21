package com.evidencepilot.repository;

import com.evidencepilot.domain.entity.Source;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SourceRepository extends JpaRepository<Source, Integer> {

    List<Source> findByProjectId(Integer projectId);

    List<Source> findByDatasetId(Integer datasetId);

    List<Source> findByUploadedById(Integer userId);

    List<Source> findByProjectStudentId(Integer studentId);

    List<Source> findByDatasetInstructorId(Integer instructorId);

    List<Source> findByActiveTrue();

    List<Source> findByProjectIdAndActiveTrue(Integer projectId);

    List<Source> findByDatasetIdAndActiveTrue(Integer datasetId);

    List<Source> findByProjectStudentIdAndActiveTrue(Integer studentId);

    List<Source> findByDatasetInstructorIdAndActiveTrue(Integer instructorId);

    Optional<Source> findByIdAndProjectId(Integer id, Integer projectId);

    Optional<Source> findByIdAndProjectIdAndActiveTrue(Integer id, Integer projectId);
}
