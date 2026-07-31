package com.evidencepilot.service;

import com.evidencepilot.dto.response.SourceCategoryRadarResponse;
import com.evidencepilot.model.Claim;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.SourceCategory;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.model.enums.MappingStatus;
import com.evidencepilot.repository.ClaimEvidenceMappingRepository;
import com.evidencepilot.repository.ClaimRepository;
import com.evidencepilot.repository.SourceCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SourceCategoryRadarService {

    private final ClaimRepository claimRepository;
    private final ClaimEvidenceMappingRepository mappingRepository;
    private final SourceCategoryRepository categoryRepository;

    public SourceCategoryRadarResponse calculate(UUID projectId) {
        List<SourceCategory> categories = categoryRepository.findByActiveTrueOrderByNameAsc();
        SourceCategory other = categories.stream()
                .filter(category -> "OTHER".equals(category.getCode()))
                .findFirst()
                .orElse(null);
        Map<UUID, Document> sources = new LinkedHashMap<>();

        for (Claim claim : claimRepository.findByProjectId(projectId)) {
            if (!claim.isActive()) continue;
            mappingRepository.findByClaimId(claim.getId()).stream()
                    .filter(mapping -> mapping.getStatus() == MappingStatus.ACTIVE)
                    .map(mapping -> mapping.getDocumentChunk())
                    .filter(chunk -> chunk != null && chunk.isActive())
                    .map(chunk -> chunk.getDocument())
                    .filter(source -> source != null
                            && source.isActive()
                            && source.getDocType() == DocumentType.SOURCE)
                    .forEach(source -> sources.putIfAbsent(source.getId(), source));
        }

        Map<UUID, Integer> counts = new LinkedHashMap<>();
        for (Document source : sources.values()) {
            SourceCategory category = source.getSourceCategory();
            if (category == null || !category.isActive()) category = other;
            if (category != null) counts.merge(category.getId(), 1, Integer::sum);
        }
        int total = sources.size();
        List<SourceCategoryRadarResponse.Axis> axes = categories.stream()
                .map(category -> {
                    int count = counts.getOrDefault(category.getId(), 0);
                    return new SourceCategoryRadarResponse.Axis(
                            category.getId(),
                            category.getCode(),
                            category.getName(),
                            count,
                            total == 0 ? 0 : Math.round(count * 1000.0 / total) / 10.0);
                })
                .toList();
        return new SourceCategoryRadarResponse("Source categories", total, axes);
    }
}
