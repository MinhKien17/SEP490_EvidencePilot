package com.evidencepilot.repository;

import com.evidencepilot.domain.entity.Paper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaperRepository extends JpaRepository<Paper, Integer> {

    List<Paper> findByProjectId(Integer projectId);

    List<Paper> findByProjectStudentId(Integer studentId);

    List<Paper> findByActiveTrue();

    List<Paper> findByProjectIdAndActiveTrue(Integer projectId);

    List<Paper> findByProjectStudentIdAndActiveTrue(Integer studentId);
}
