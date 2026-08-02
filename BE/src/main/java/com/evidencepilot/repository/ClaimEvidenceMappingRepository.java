package com.evidencepilot.repository;

import com.evidencepilot.model.ClaimEvidenceMapping;
import com.evidencepilot.model.enums.MappingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClaimEvidenceMappingRepository extends JpaRepository<ClaimEvidenceMapping, UUID> {
    List<ClaimEvidenceMapping> findByClaimId(UUID claimId);
    List<ClaimEvidenceMapping> findByClaimIdIn(List<UUID> claimIds);
    List<ClaimEvidenceMapping> findByDocumentChunkId(UUID documentChunkId);
    List<ClaimEvidenceMapping> findByClaimIdAndDocumentChunkId(UUID claimId, UUID documentChunkId);
    List<ClaimEvidenceMapping> findByDocumentChunkDocumentIdAndStatus(UUID documentId, MappingStatus status);
}