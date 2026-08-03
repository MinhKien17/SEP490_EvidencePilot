package com.evidencepilot.repository;

import com.evidencepilot.model.Claim;
import com.evidencepilot.model.enums.FunctionalType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClaimRepository extends JpaRepository<Claim, UUID>, JpaSpecificationExecutor<Claim> {
    List<Claim> findByProjectId(UUID projectId);
    List<Claim> findBySectionId(UUID sectionId);
    List<Claim> findByProjectIdAndSectionId(UUID projectId, UUID sectionId);
    List<Claim> findBySectionIdAndProjectIdOrderByClaimVersionDesc(UUID sectionId, UUID projectId);

    @Query("select c from Claim c join fetch c.project where c.id = :id")
    Optional<Claim> findByIdWithProject(UUID id);

    @Query("select c.functionalType as functionalType, c.claimQualityScore as claimQualityScore " +
           "from Claim c where c.project.id = :projectId and c.active = true")
    List<FunctionalTypeScore> findFunctionalTypeScores(@Param("projectId") UUID projectId);

    interface FunctionalTypeScore {
        FunctionalType getFunctionalType();
        Float getClaimQualityScore();
    }
}
