package com.evidencepilot.service;

import com.evidencepilot.dto.response.GraphResponse;
import com.evidencepilot.model.Claim;
import com.evidencepilot.model.ClaimEvidenceMapping;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.DocumentChunk;
import com.evidencepilot.model.PaperSection;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.model.enums.EvidenceRelation;
import com.evidencepilot.model.enums.FunctionalType;
import com.evidencepilot.model.enums.MappingStatus;
import com.evidencepilot.repository.ClaimRepository;
import com.evidencepilot.repository.DocumentReferenceRepository;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.repository.PaperSectionRepository;
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
    private DocumentRepository documentRepository;
    @Mock
    private PaperSectionRepository paperSectionRepository;
    @Mock
    private DocumentReferenceRepository documentReferenceRepository;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private EvidenceFilterService evidenceFilterService;
    @Mock
    private ClaimContentConsistencyService claimContentConsistencyService;

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
        when(evidenceFilterService.activeMappings(claim))
                .thenReturn(List.of(weak, strong));
        when(claimContentConsistencyService.evaluate(claim))
                .thenReturn(com.evidencepilot.model.enums.ClaimContentStatus.PRESENT);
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

    @Test
    void graphIncludesPaperSectionsInSummariesWithoutClaims() {
        UUID projectId = UUID.randomUUID();
        Project project = new Project();
        project.setId(projectId);
        project.setActive(true);
        User user = new User();
        user.setId(UUID.randomUUID());

        Document paper = new Document();
        paper.setId(UUID.randomUUID());
        paper.setActive(true);
        PaperSection first = section(paper, "Introduction");
        PaperSection second = section(paper, "Methodology");
        PaperSection third = section(paper, "Results");

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(claimRepository.findByProjectId(projectId)).thenReturn(List.of());
        when(documentRepository.findByProjectIdAndDocTypeAndActiveTrue(
                projectId, DocumentType.PAPER)).thenReturn(List.of(paper));
        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(paper.getId()))
                .thenReturn(List.of(first, second, third));

        GraphResponse response = service.getGraph(projectId, "all");

        assertThat(response.sectionSummaries())
                .extracting(GraphResponse.GraphSectionSummary::sectionTitle)
                .containsExactly("Introduction", "Methodology", "Results");
        assertThat(response.sectionSummaries())
                .allSatisfy(summary -> {
                    assertThat(summary.claimCount()).isZero();
                    assertThat(summary.presentCount()).isZero();
                });
    }

    @Test
    void claimStatsGroupsByFunctionalTypeAndZeroFillsMissingTypes() {
        UUID projectId = UUID.randomUUID();
        Project project = new Project();
        project.setId(projectId);
        project.setActive(true);
        User user = new User();
        user.setId(UUID.randomUUID());

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(claimRepository.countByFunctionalType(projectId)).thenReturn(List.of(
                count(FunctionalType.EMPIRICAL, 2),
                count(FunctionalType.THEORETICAL, 1)));

        GraphResponse.ClaimStatsResponse stats = service.getClaimStats(projectId);

        assertThat(stats.totalClaims()).isEqualTo(3);
        assertThat(stats.byFunctionalType())
                .containsEntry("EMPIRICAL", 2L)
                .containsEntry("THEORETICAL", 1L)
                .containsEntry("METHODOLOGICAL", 0L)
                .containsEntry("ANALYTICAL", 0L)
                .containsEntry("APPLIED", 0L);
    }

    private static ClaimRepository.FunctionalTypeCount count(FunctionalType type, long count) {
        return new ClaimRepository.FunctionalTypeCount() {
            @Override
            public FunctionalType getFunctionalType() {
                return type;
            }

            @Override
            public long getClaimCount() {
                return count;
            }
        };
    }

    private static PaperSection section(Document paper, String title) {
        PaperSection section = new PaperSection();
        section.setId(UUID.randomUUID());
        section.setDocument(paper);
        section.setSectionTitle(title);
        section.setActive(true);
        return section;
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
