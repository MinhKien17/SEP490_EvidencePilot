package com.evidencepilot.repository;

import com.evidencepilot.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.UUID;
import java.util.List;
import org.springframework.data.jpa.repository.Query;

public interface ProjectRepository extends JpaRepository<Project, UUID>, JpaSpecificationExecutor<Project> {
    long countByActiveTrue();

    @Query("select project.status, count(project) from Project project where project.active = true group by project.status")
    List<Object[]> countActiveByStatus();
}
