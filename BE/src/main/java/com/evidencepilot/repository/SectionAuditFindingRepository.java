package com.evidencepilot.repository;

import com.evidencepilot.model.SectionAuditFinding;
import com.evidencepilot.model.enums.SectionAuditFindingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SectionAuditFindingRepository extends JpaRepository<SectionAuditFinding, UUID> {

    List<SectionAuditFinding> findBySectionIdAndContentFingerprint(UUID sectionId, String contentFingerprint);

    List<SectionAuditFinding> findBySectionIdAndContentFingerprintOrderByStartIndexAsc(
            UUID sectionId, String contentFingerprint);

    List<SectionAuditFinding> findBySectionIdAndContentFingerprintAndStatusOrderByStartIndexAsc(
            UUID sectionId, String contentFingerprint, SectionAuditFindingStatus status);
}
