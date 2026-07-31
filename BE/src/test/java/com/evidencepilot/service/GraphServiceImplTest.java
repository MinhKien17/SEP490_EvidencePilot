package com.evidencepilot.service;

import com.evidencepilot.dto.response.GraphResponse;
import com.evidencepilot.model.Claim;
import com.evidencepilot.model.ClaimEvidenceMapping;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.DocumentChunk;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.model.enums.EvidenceRelation;
import com.evidencepilot.model.enums.MappingStatus;
import com.evidencepilot.repository.ClaimEvidenceMappingRepository;
import com.evidencepilot.repository.ClaimRepository;
import com.evidencepilot.repository.DocumentReferenceRepository;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.repository.ProjectRepository;
import com.evidencepilot.service.impl.GraphServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GraphServiceImplTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ClaimRepository claimRepository;
    @Mock
    private ClaimEvidenceMappingRepository mappingRepository;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private DocumentReferenceRepository documentReferenceRepository;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private SourceCategoryRadarService sourceCategoryRadarService;

    @InjectMocks
    private GraphServiceImpl service;

    @Test
    void graphUsesStrongestActiveMapping() {
        UUID projectId = UUID.randomUUID();
        Project project = new Project();
        project.setId(projectId);
        project.setActive(true);
        User user = new User();
        user.setId(UUID.randomUUID());
        Claim claim = new Claim();
        claim.setId(UUID.randomUUID());
        claim.setProject(project);
        claim.setContent("Claim");
        claim.setActive(true);

        Document weakSource = source("weak.pdf");
        Document strongSource = source("strong.pdf");
        ClaimEvidenceMapping weak = mapping(
                claim, weakSource, MappingStatus.ACTIVE, EvidenceRelation.SUPPORTS, 35);
        ClaimEvidenceMapping strong = mapping(
                claim, strongSource, MappingStatus.ACTIVE, EvidenceRelation.CONTRADICTS, 80);
        ClaimEvidenceMapping inactive = mapping(
                claim, source("inactive.pdf"), MappingStatus.INACTIVE, EvidenceRelation.SUPPORTS, 99);

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(claimRepository.findByProjectId(projectId)).thenReturn(List.of(claim));
        when(mappingRepository.findByClaimId(claim.getId()))
                .thenReturn(List.of(weak, strong, inactive));
        when(documentReferenceRepository
                .findByDocumentProjectIdAndDocumentDocTypeAndDocumentActiveTrueOrderByDocumentIdAscReferenceIndexAsc(
                        projectId, DocumentType.SOURCE))
                .thenReturn(List.of());
        when(documentRepository.findAllById(any()))
                .thenReturn(List.of(weakSource, strongSource));

        GraphResponse response = service.getGraph(projectId, "all");

        assertThat(response.totalEdges()).isEqualTo(2);
        assertThat(response.claims()).singleElement().satisfies(graphClaim -> {
            assertThat(graphClaim.matchCount()).isEqualTo(2);
            assertThat(graphClaim.graphData())
                    .containsEntry("verdict", "CONTRADICTS")
                    .containsEntry("confidence", 80);
        });
    }

    private static Document source(String filename) {
        Document document = new Document();
        document.setId(UUID.randomUUID());
        document.setOriginalFilename(filename);
        document.setActive(true);
        return document;
    }

    private static ClaimEvidenceMapping mapping(
            Claim claim,
            Document document,
            MappingStatus status,
            EvidenceRelation relation,
            int strength) {
        DocumentChunk chunk = new DocumentChunk();
        chunk.setId(UUID.randomUUID());
        chunk.setDocument(document);
        ClaimEvidenceMapping mapping = new ClaimEvidenceMapping();
        mapping.setClaim(claim);
        mapping.setDocumentChunk(chunk);
        mapping.setStatus(status);
        mapping.setRelation(relation);
        mapping.setStrengthScore(strength);
        mapping.setCreatedAt(LocalDateTime.now());
        return mapping;
    }
}
