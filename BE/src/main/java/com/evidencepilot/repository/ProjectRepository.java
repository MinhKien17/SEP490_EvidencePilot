package com.evidencepilot.repository;

import com.evidencepilot.domain.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Integer> {

    List<Project> findByStudentId(Integer studentId);

    List<Project> findByActiveTrue();

    List<Project> findByStudentIdAndActiveTrue(Integer studentId);
}
