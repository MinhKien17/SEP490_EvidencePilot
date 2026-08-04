package com.evidencepilot.repository;

import com.evidencepilot.model.Claim;
import com.evidencepilot.model.enums.FunctionalType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClaimRepository extends JpaRepository<Claim, UUID>, JpaSpecificationExecutor<Claim> {
    @EntityGraph(attributePaths = {"project", "section", "section.document", "section.document.project"})
    List<Claim> findByProjectId(UUID projectId);
    List<Claim> findBySectionId(UUID sectionId);
    List<Claim> findByProjectIdAndSectionId(UUID projectId, UUID sectionId);
    List<Claim> findBySectionIdAndProjectIdOrderByClaimVersionDesc(UUID sectionId, UUID projectId);

    @Query("select c from Claim c join fetch c.project where c.id = :id")
    Optional<Claim> findByIdWithProject(UUID id);

    @Query("select c.functionalType as functionalType, count(c) as claimCount " +
           "from Claim c where c.project.id = :projectId and c.active = true " +
           "group by c.functionalType")
    List<FunctionalTypeCount> countByFunctionalType(@Param("projectId") UUID projectId);

    interface FunctionalTypeCount {
        FunctionalType getFunctionalType();
        long getClaimCount();
    }
}
