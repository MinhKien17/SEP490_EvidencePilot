package com.evidencepilot.service.impl;

import com.evidencepilot.model.Claim;
import com.evidencepilot.model.ClaimEvidenceMapping;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.DocumentChunk;
import com.evidencepilot.model.InstructorFeedback;
import com.evidencepilot.model.PaperSection;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.ClaimContentStatus;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.model.enums.EvidenceRelation;
import com.evidencepilot.repository.ClaimRepository;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.repository.InstructorFeedbackRepository;
import com.evidencepilot.repository.PaperSectionRepository;
import com.evidencepilot.repository.ProjectRepository;
import com.evidencepilot.service.ClaimContentConsistencyService;
import com.evidencepilot.service.CurrentUserService;
import com.evidencepilot.service.EvidenceFilterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProgressReportServiceImplTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ClaimRepository claimRepository;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private PaperSectionRepository paperSectionRepository;
    @Mock
    private InstructorFeedbackRepository instructorFeedbackRepository;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private EvidenceFilterService evidenceFilterService;
    @Mock
    private ClaimContentConsistencyService claimContentConsistencyService;

    private ProgressReportServiceImpl service;
    private Project project;
    private User user;
    private User assigned;

    @BeforeEach
    void setUp() {
        service = new ProgressReportServiceImpl(
                projectRepository, claimRepository, documentRepository, paperSectionRepository,
                instructorFeedbackRepository, currentUserService, evidenceFilterService,
                claimContentConsistencyService);
        project = new Project();
        project.setId(UUID.randomUUID());
        project.setActive(true);
        user = user("Alice");
        assigned = user("Bob");
    }

    @Test
    void reportBuildsPanelsMatrixAndReadinessFromActiveEvidence() {
        Document paper = new Document();
        paper.setId(UUID.randomUUID());
        paper.setDocType(DocumentType.PAPER);
        PaperSection section = section(paper, assigned, "Introduction");
        section.setContentTex("one two three four five");
        section.setVersion(3);
        section.setUpdatedAt(LocalDateTime.now());
        Claim claim = new Claim();
        claim.setId(UUID.randomUUID());
        claim.setProject(project);
        claim.setSection(section);
        claim.setContent("Claim text");
        claim.setActive(true);
        claim.setCreatedBy(user);
        ClaimEvidenceMapping mapping = mapping(claim, EvidenceRelation.SUPPORTS, 80);
        InstructorFeedback answered = new InstructorFeedback();
        answered.setSection(section);
        answered.setAnswered(true);
        InstructorFeedback open = new InstructorFeedback();
        open.setSection(section);
        open.setAnswered(false);

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(documentRepository.findByProjectIdAndDocTypeAndActiveTrue(project.getId(), DocumentType.PAPER))
                .thenReturn(List.of(paper));
        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(paper.getId()))
                .thenReturn(List.of(section));
        when(claimRepository.findByProjectId(project.getId())).thenReturn(List.of(claim));
        when(instructorFeedbackRepository.findByRequestProjectId(project.getId()))
                .thenReturn(List.of(answered, open));
        when(evidenceFilterService.activeMappings(claim)).thenReturn(List.of(mapping));
        when(claimContentConsistencyService.evaluate(claim)).thenReturn(ClaimContentStatus.PRESENT);

        var report = service.getProgressReport(project.getId(), "ALL");

        assertThat(report.sections()).singleElement().satisfies(panel -> {
            assertThat(panel.wordCount()).isEqualTo(5);
            assertThat(panel.claimCount()).isEqualTo(1);
            assertThat(panel.version()).isEqualTo(3);
            assertThat(panel.feedbackAnswered()).isEqualTo(1);
            assertThat(panel.feedbackUnanswered()).isEqualTo(1);
            assertThat(panel.assignedUserId()).isEqualTo(assigned.getId());
        });
        assertThat(report.matrix()).singleElement().satisfies(row -> {
            assertThat(row.activeEvidenceCount()).isEqualTo(1);
            assertThat(row.strongestRelation()).isEqualTo("SUPPORTS");
            assertThat(row.strongestScore()).isEqualTo(80);
            assertThat(row.createdByName()).isEqualTo("Alice");
        });
        assertThat(report.readiness().score()).isEqualTo(100);
    }

    @Test
    void memberFilterRestrictsSectionsAndTheirClaims() {
        Document paper = new Document();
        paper.setId(UUID.randomUUID());
        paper.setDocType(DocumentType.PAPER);
        PaperSection mine = section(paper, assigned, "Mine");
        PaperSection other = section(paper, user, "Other");
        Claim claimInOther = new Claim();
        claimInOther.setId(UUID.randomUUID());
        claimInOther.setProject(project);
        claimInOther.setSection(other);
        claimInOther.setContent("Other claim");
        claimInOther.setActive(true);

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(documentRepository.findByProjectIdAndDocTypeAndActiveTrue(project.getId(), DocumentType.PAPER))
                .thenReturn(List.of(paper));
        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(paper.getId()))
                .thenReturn(List.of(mine, other));
        when(claimRepository.findByProjectId(project.getId())).thenReturn(List.of(claimInOther));

        var report = service.getProgressReport(project.getId(), assigned.getId().toString());

        assertThat(report.sections()).singleElement().extracting("sectionId").isEqualTo(mine.getId());
        assertThat(report.matrix()).isEmpty();
        assertThat(report.readiness().score()).isEqualTo(0);
    }

    private static User user(String name) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setFirstName(name);
        return user;
    }

    private static PaperSection section(Document paper, User assigned, String title) {
        PaperSection section = new PaperSection();
        section.setId(UUID.randomUUID());
        section.setDocument(paper);
        section.setAssignedUser(assigned);
        section.setSectionTitle(title);
        section.setActive(true);
        return section;
    }

    private static ClaimEvidenceMapping mapping(Claim claim, EvidenceRelation relation, int strength) {
        DocumentChunk chunk = new DocumentChunk();
        chunk.setId(UUID.randomUUID());
        chunk.setActive(true);
        ClaimEvidenceMapping mapping = new ClaimEvidenceMapping();
        mapping.setClaim(claim);
        mapping.setDocumentChunk(chunk);
        mapping.setRelation(relation);
        mapping.setStrengthScore(strength);
        return mapping;
    }
}
