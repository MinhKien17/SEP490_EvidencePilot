package com.evidencepilot.service;

import com.evidencepilot.model.Claim;
import com.evidencepilot.model.ClaimEvidenceMapping;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.model.enums.MappingStatus;
import com.evidencepilot.repository.ClaimEvidenceMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EvidenceFilterService {

    private final ClaimEvidenceMappingRepository mappingRepository;

    public List<ClaimEvidenceMapping> activeMappings(Claim claim) {
        return mappingRepository.findByClaimId(claim.getId()).stream()
                .filter(mapping -> mapping.getStatus() == MappingStatus.ACTIVE)
                .filter(mapping -> mapping.getDocumentChunk() != null
                        && mapping.getDocumentChunk().isActive())
                .filter(mapping -> mapping.getDocumentChunk().getDocument() != null
                        && mapping.getDocumentChunk().getDocument().isActive()
                        && mapping.getDocumentChunk().getDocument().getDocType()
                                == DocumentType.SOURCE)
                .toList();
    }
}
