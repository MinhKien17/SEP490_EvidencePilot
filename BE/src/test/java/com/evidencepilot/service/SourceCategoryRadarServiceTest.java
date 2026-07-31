package com.evidencepilot.service;

import com.evidencepilot.model.Claim;
import com.evidencepilot.model.ClaimEvidenceMapping;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.DocumentChunk;
import com.evidencepilot.model.SourceCategory;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.model.enums.MappingStatus;
import com.evidencepilot.repository.ClaimEvidenceMappingRepository;
import com.evidencepilot.repository.ClaimRepository;
import com.evidencepilot.repository.SourceCategoryRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SourceCategoryRadarServiceTest {

    @Test
    void countsEachActiveMappedSourceOnceAndIncludesZeroAxes() {
        ClaimRepository claims = mock(ClaimRepository.class);
        ClaimEvidenceMappingRepository mappings = mock(ClaimEvidenceMappingRepository.class);
        SourceCategoryRepository categories = mock(SourceCategoryRepository.class);
        SourceCategoryRadarService service =
                new SourceCategoryRadarService(claims, mappings, categories);
        UUID projectId = UUID.randomUUID();
        SourceCategory ai = category("AI_ML", "AI/ML");
        SourceCategory se = category("SE", "Software Engineering");
        SourceCategory other = category("OTHER", "Other");
        Claim first = claim();
        Claim second = claim();
        Document source = source(ai);
        Document detachedCopy = source(ai);
        detachedCopy.setId(source.getId());
        Document uncategorized = source(null);
        ClaimEvidenceMapping inactive = mapping(source);
        inactive.setStatus(MappingStatus.INACTIVE);

        when(categories.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(ai, other, se));
        when(claims.findByProjectId(projectId)).thenReturn(List.of(first, second));
        when(mappings.findByClaimId(first.getId()))
                .thenReturn(List.of(mapping(source), inactive));
        when(mappings.findByClaimId(second.getId()))
                .thenReturn(List.of(mapping(detachedCopy), mapping(uncategorized)));

        var radar = service.calculate(projectId);

        assertThat(radar.totalSources()).isEqualTo(2);
        assertThat(radar.axes()).extracting(axis -> axis.code() + ":" + axis.sourceCount())
                .containsExactly("AI_ML:1", "OTHER:1", "SE:0");
        assertThat(radar.axes().getFirst().percentage()).isEqualTo(50);
    }

    private static SourceCategory category(String code, String name) {
        SourceCategory category = new SourceCategory();
        category.setId(UUID.randomUUID());
        category.setCode(code);
        category.setName(name);
        category.setActive(true);
        return category;
    }

    private static Claim claim() {
        Claim claim = new Claim();
        claim.setId(UUID.randomUUID());
        claim.setActive(true);
        return claim;
    }

    private static Document source(SourceCategory category) {
        Document source = new Document();
        source.setId(UUID.randomUUID());
        source.setDocType(DocumentType.SOURCE);
        source.setSourceCategory(category);
        source.setActive(true);
        return source;
    }

    private static ClaimEvidenceMapping mapping(Document source) {
        DocumentChunk chunk = new DocumentChunk();
        chunk.setId(UUID.randomUUID());
        chunk.setDocument(source);
        chunk.setActive(true);
        ClaimEvidenceMapping mapping = new ClaimEvidenceMapping();
        mapping.setDocumentChunk(chunk);
        mapping.setStatus(MappingStatus.ACTIVE);
        return mapping;
    }
}
