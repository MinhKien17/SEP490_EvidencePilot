package com.evidencepilot.repository;

import com.evidencepilot.domain.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Project} entities.
 *
 * <p>
 * <strong>Tenancy policy</strong>: every query is permanently scoped to
 * a specific {@code studentId} and {@code active = true}. Global,
 * unfiltered finders are intentionally omitted to prevent cross-tenant
 * data leakage.
 * </p>
 */
@Repository
public interface ProjectRepository extends JpaRepository<Project, Integer> {

    /**
     * Finds a single active project owned by the given student.
     *
     * @param id        the project primary key
     * @param studentId the owning student's user ID
     * @return the project if it exists, is active, and belongs to the student
     */
    Optional<Project> findByIdAndStudentIdAndActiveTrue(Integer id, Integer studentId);

    /**
     * Lists all active projects owned by the given student.
     *
     * @param studentId the owning student's user ID
     * @return active projects for the student (never {@code null})
     */
    List<Project> findAllByStudentIdAndActiveTrue(Integer studentId);

    boolean existsByIdAndStudentIdAndActiveTrue(Integer id, Integer studentId);
}
