package com.evidencepilot.repository;

import com.evidencepilot.domain.entity.Dataset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DatasetRepository extends JpaRepository<Dataset, Integer> {

    List<Dataset> findByInstructorId(Integer instructorId);

    List<Dataset> findByActiveTrue();

    List<Dataset> findByInstructorIdAndActiveTrue(Integer instructorId);
}
