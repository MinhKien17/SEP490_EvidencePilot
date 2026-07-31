package com.evidencepilot.service;

import com.evidencepilot.mapper.ProjectMapper;
import com.evidencepilot.dto.response.SourceCategoryRadarResponse;
import com.evidencepilot.exception.ResourceNotFoundException;
import com.evidencepilot.model.ClaimEvidenceMapping;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.DocumentText;
import com.evidencepilot.model.Claim;
import com.evidencepilot.model.DocumentChunk;
import com.evidencepilot.model.InstructorFeedback;
import com.evidencepilot.model.PaperSection;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.model.enums.UserRole;
import com.evidencepilot.model.enums.ProjectStatus;
import com.evidencepilot.model.enums.ClaimContentStatus;
import com.evidencepilot.model.enums.MappingStatus;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.repository.ClaimRepository;
import com.evidencepilot.repository.ClaimEvidenceMappingRepository;
import com.evidencepilot.repository.InstructorFeedbackRepository;
import com.evidencepilot.repository.PaperSectionRepository;
import com.evidencepilot.repository.ProjectRepository;
import com.evidencepilot.repository.SectionFeedbackRepository;
import com.evidencepilot.repository.UserRepository;
import com.evidencepilot.service.impl.PaperProcessingServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class PaperProcessingServiceImplTest {

    @Mock
    private AiModelClient aiModelClient;

    @Mock
    private PaperSectionRepository paperSectionRepository;

    @Mock
    private ClaimRepository claimRepository;

    @Mock
    private ClaimEvidenceMappingRepository claimEvidenceMappingRepository;

    @Mock
    private InstructorFeedbackRepository instructorFeedbackRepository;

    @Mock
    private ClaimContentConsistencyService claimContentConsistencyService;

    @Mock
    private SourceCategoryRadarService sourceCategoryRadarService;

    @Mock
    private AuditService auditService;

    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper =
            new com.fasterxml.jackson.databind.ObjectMapper();

    @Mock
    private SectionFeedbackRepository sectionFeedbackRepository;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private PaperStandardService paperStandardService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private SystemNotificationService systemNotificationService;

    @Mock
    private TexArchiveBuilder texArchiveBuilder;

    @Test
    void getPaperSectionsRequiresProjectAccess() {
        User user = user();
        Project project = project();
        Document document = document(project);

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(document.getId()))
                .thenReturn(List.of());

        service().getPaperSections(document.getId());

        verify(currentUserService).requireProjectAccess(user, project);
    }

    @Test
    void getPaperSectionsHidesSoftDeletedSections() {
        User user = user();
        Document document = document(project());
        PaperSection active = section(document);
        PaperSection deleted = section(document);
        deleted.setActive(false);
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(document.getId()))
                .thenReturn(List.of(active, deleted));

        service().getPaperSections(document.getId());

        verify(projectMapper).toPaperSectionResponse(active);
        verify(projectMapper, never()).toPaperSectionResponse(deleted);
    }

    @Test
    void reviewUsesCurrentSectionsEligibleEvidenceAndSectionFeedback() {
        User user = user();
        Project project = project();
        Document document = document(project);
        DocumentText text = new DocumentText();
        text.setDocument(document);
        text.setExtractedText("Stale extracted text");
        document.setDocumentText(text);
        PaperSection section = section(document);
        section.setContentTex("Latest saved section content");
        Claim claim = new Claim();
        claim.setId(UUID.randomUUID());
        claim.setProject(project);
        claim.setSection(section);
        claim.setContent("Latest saved section content");
        claim.setActive(true);
        ClaimEvidenceMapping eligible = mapping("Verified source snippet", MappingStatus.ACTIVE);
        ClaimEvidenceMapping excluded = mapping("Rejected source snippet", MappingStatus.INACTIVE);
        InstructorFeedback feedback = new InstructorFeedback();
        feedback.setId(UUID.randomUUID());
        feedback.setSection(section);
        feedback.setContent("Clarify this claim.");

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(sourceCategoryRadarService.calculate(project.getId())).thenReturn(radar());
        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(document.getId()))
                .thenReturn(List.of(section));
        when(instructorFeedbackRepository.findByRequestProjectId(project.getId()))
                .thenReturn(List.of(feedback));
        when(claimRepository.findByProjectId(project.getId())).thenReturn(List.of(claim));
        when(claimEvidenceMappingRepository.findByClaimId(claim.getId()))
                .thenReturn(List.of(eligible, excluded));
        when(claimContentConsistencyService.evaluate(claim)).thenReturn(ClaimContentStatus.PRESENT);
        when(aiModelClient.generate(anyString())).thenReturn(validReviewJson());

        var response = service().review(document.getId(), null);

        verify(currentUserService).requireProjectAccess(user, project);
        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(aiModelClient).generate(prompt.capture());
        assertThat(prompt.getValue())
                .contains("Latest saved section content")
                .contains("Verified source snippet")
                .contains("Clarify this claim.")
                .doesNotContain("Stale extracted text", "Rejected source snippet");
        assertThat(response.complete()).isTrue();
        assertThat(response.coverage().sectionsScanned()).isEqualTo(1);
        assertThat(response.summary()).contains("checked 1/1 active Claims");
    }

    @Test
    void reviewRetriesInvalidJsonOnce() {
        User user = user();
        Project project = project();
        Document document = document(project);
        PaperSection section = section(document);
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(sourceCategoryRadarService.calculate(project.getId())).thenReturn(radar());
        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(document.getId()))
                .thenReturn(List.of(section));
        when(instructorFeedbackRepository.findByRequestProjectId(project.getId()))
                .thenReturn(List.of());
        when(claimRepository.findByProjectId(project.getId())).thenReturn(List.of());
        when(aiModelClient.generate(anyString())).thenReturn("not-json");

        assertThatThrownBy(() -> service().review(document.getId(), null))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("invalid JSON");
        verify(aiModelClient, times(2)).generate(anyString());
    }

    @Test
    void reviewScansEveryPaperChunkWithoutSilentTruncation() {
        User user = user();
        Project project = project();
        Document document = document(project);
        PaperSection section = section(document);
        section.setContentTex("x".repeat(8_100) + "TAIL_MARKER");
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(sourceCategoryRadarService.calculate(project.getId())).thenReturn(radar());
        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(document.getId()))
                .thenReturn(List.of(section));
        when(instructorFeedbackRepository.findByRequestProjectId(project.getId()))
                .thenReturn(List.of());
        when(claimRepository.findByProjectId(project.getId())).thenReturn(List.of());
        when(aiModelClient.generate(anyString())).thenReturn(validReviewJson());

        var response = service().review(document.getId(), "IEEE");

        ArgumentCaptor<String> prompts = ArgumentCaptor.forClass(String.class);
        verify(aiModelClient, times(2)).generate(prompts.capture());
        assertThat(prompts.getAllValues()).anyMatch(prompt -> prompt.contains("TAIL_MARKER"));
        assertThat(response.coverage().totalChunks()).isEqualTo(2);
        assertThat(response.coverage().chunksScanned()).isEqualTo(2);
    }

    @Test
    void detectAndPersistSectionsReturnsEmptyWithoutExtractedText() {
        Document document = document(project());
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));

        org.assertj.core.api.Assertions.assertThat(service().detectAndPersistSections(document.getId())).isEmpty();
    }

    @Test
    void detectAndPersistSectionsCreatesFullTextSection() {
        Document document = document(project());
        DocumentText text = new DocumentText();
        text.setDocument(document);
        text.setExtractedText("lowercase content without a heading");
        document.setDocumentText(text);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(paperSectionRepository.saveAll(org.mockito.ArgumentMatchers.anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service().detectAndPersistSections(document.getId());

        verify(paperSectionRepository).saveAll(argThat(sections -> {
            var iterator = sections.iterator();
            return iterator.hasNext()
                    && iterator.next().getSectionTitle().equals("Full Text")
                    && !iterator.hasNext();
        }));
    }

    @Test
    void detectAndPersistSectionsKeepsExistingSectionsOnRetry() {
        Document document = document(project());
        DocumentText text = new DocumentText();
        text.setDocument(document);
        text.setExtractedText("Introduction\nExtracted content");
        document.setDocumentText(text);
        PaperSection existing = new PaperSection();
        existing.setId(UUID.randomUUID());
        existing.setDocument(document);
        existing.setSectionTitle("Edited Introduction");
        existing.setSectionOrder(0);

        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(document.getId()))
                .thenReturn(List.of(existing));

        org.assertj.core.api.Assertions.assertThat(service().detectAndPersistSections(document.getId()))
                .hasSize(1);

        verify(paperSectionRepository).findByDocumentIdOrderBySectionOrderAsc(document.getId());
        verifyNoMoreInteractions(paperSectionRepository);
    }

    @Test
    void archivedProjectRejectsSectionMutation() {
        User user = instructor();
        Project project = project();
        project.setStatus(ProjectStatus.ARCHIVED);
        Document document = document(project);
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(currentUserService.isInstructor(user)).thenReturn(true);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        doThrow(new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.CONFLICT, "Project is read-only."))
                .when(currentUserService).requireProjectWriteAccess(user, project);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service().createSection(
                document.getId(), "Conclusion", null))
                .hasMessageContaining("Project is read-only.");
    }

    @Test
    void updateSectionRejectsSectionFromAnotherDocument() {
        User user = instructor();
        Document authorized = document(project());
        PaperSection foreign = section(document(project()));
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(currentUserService.isInstructor(user)).thenReturn(true);
        when(documentRepository.findById(authorized.getId())).thenReturn(Optional.of(authorized));
        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(authorized.getId()))
                .thenReturn(List.of());
        when(paperSectionRepository.findById(foreign.getId())).thenReturn(Optional.of(foreign));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service().updateSection(
                authorized.getId(), foreign.getId(), "Changed", null, null, null))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(paperSectionRepository, never()).save(any(PaperSection.class));
    }

    @Test
    void mergeRejectsTargetFromAnotherDocument() {
        User user = instructor();
        Document authorized = document(project());
        PaperSection source = section(authorized);
        PaperSection foreignTarget = section(document(project()));
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(currentUserService.isInstructor(user)).thenReturn(true);
        when(documentRepository.findById(authorized.getId())).thenReturn(Optional.of(authorized));
        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(authorized.getId()))
                .thenReturn(List.of());
        when(paperSectionRepository.findById(foreignTarget.getId())).thenReturn(Optional.of(foreignTarget));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service().updateSection(
                authorized.getId(), source.getId(), null, null, foreignTarget.getId(), null))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(paperSectionRepository, never()).save(any(PaperSection.class));
    }

    @Test
    void contentUpdateUsesSectionContentPermission() {
        User user = user();
        Document document = document(project());
        PaperSection section = section(document);
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(paperSectionRepository.findById(section.getId())).thenReturn(Optional.of(section));
        when(paperSectionRepository.save(section)).thenReturn(section);

        service().updateSection(document.getId(), section.getId(), null, null, null, "Updated");

        verify(currentUserService).requireSectionContentWriteAccess(user, section);
    }

    @Test
    void instructorCanRenameUnassignedSection() {
        User user = instructor();
        Document document = document(project());
        PaperSection section = section(document);
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(currentUserService.isInstructor(user)).thenReturn(true);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(document.getId()))
                .thenReturn(List.of(section));
        when(paperSectionRepository.findById(section.getId())).thenReturn(Optional.of(section));
        when(paperSectionRepository.save(section)).thenReturn(section);

        service().updateSection(
                document.getId(), section.getId(), "Renamed", null, null, null);

        assertThat(section.getSectionTitle()).isEqualTo("Renamed");
        verify(currentUserService, never()).requireSectionContentWriteAccess(any(), any());
    }

    @Test
    void studentCannotRenameSection() {
        User user = user();
        Document document = document(project());
        PaperSection section = section(document);
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));

        assertThatThrownBy(() -> service().updateSection(
                document.getId(), section.getId(), "Renamed", null, null, null))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("Only instructors");

        verify(paperSectionRepository, never()).save(any(PaperSection.class));
    }

    @Test
    void structureChangesAreLockedWhenAnySectionIsAssigned() {
        User user = instructor();
        Document document = document(project());
        PaperSection assigned = section(document);
        assigned.setAssignedUser(user());
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(currentUserService.isInstructor(user)).thenReturn(true);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(document.getId()))
                .thenReturn(List.of(assigned));

        assertThatThrownBy(() -> service().updateSection(
                document.getId(), assigned.getId(), "Renamed", null, null, null))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("structure is locked");

        verify(paperSectionRepository, never()).save(any(PaperSection.class));
    }

    @Test
    void updateRejectsMixedStructureAndContent() {
        assertThatThrownBy(() -> service().updateSection(
                UUID.randomUUID(), UUID.randomUUID(), "Renamed", null, null, "Content"))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("must be updated separately");
    }

    @Test
    void instructorCanCreateTopLevelSectionWhileStructureIsUnlocked() {
        User user = instructor();
        Document document = document(project());
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(currentUserService.isInstructor(user)).thenReturn(true);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(document.getId()))
                .thenReturn(List.of());
        when(paperStandardService.getSectionTemplate(
                com.evidencepilot.model.enums.PaperStandard.CUSTOM, "New Section"))
                .thenReturn("% template");
        when(paperSectionRepository.save(any(PaperSection.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service().createSection(document.getId(), "New Section", null);

        verify(paperSectionRepository).save(argThat(section ->
                section.getDocument() == document
                        && section.getAssignedUser() == null
                        && section.getSectionOrder() == 0
                        && "New Section".equals(section.getSectionTitle())
                        && "% template".equals(section.getContentTex())));
    }

    @Test
    void mergeMovesActiveClaimsToTargetSection() {
        User user = instructor();
        Document document = document(project());
        PaperSection source = section(document);
        PaperSection target = section(document);
        Claim claim = new Claim();
        claim.setActive(true);
        claim.setSection(source);
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(currentUserService.isInstructor(user)).thenReturn(true);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(document.getId()))
                .thenReturn(List.of(source, target));
        when(paperSectionRepository.findById(source.getId())).thenReturn(Optional.of(source));
        when(paperSectionRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(claimRepository.findBySectionId(source.getId())).thenReturn(List.of(claim));

        service().updateSection(document.getId(), source.getId(), null, null, target.getId(), null);

        assertThat(claim.getSection()).isSameAs(target);
        verify(claimRepository).saveAll(List.of(claim));
    }

    @Test
    void deleteSectionRejectsActiveClaims() {
        User user = instructor();
        Document document = document(project());
        PaperSection section = section(document);
        section.setContentTex("");
        Claim claim = new Claim();
        claim.setActive(true);
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(currentUserService.isInstructor(user)).thenReturn(true);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(document.getId()))
                .thenReturn(List.of(section));
        when(paperSectionRepository.findById(section.getId())).thenReturn(Optional.of(section));
        when(claimRepository.findBySectionId(section.getId())).thenReturn(List.of(claim));

        assertThatThrownBy(() -> service().deleteSection(document.getId(), section.getId()))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("active claims");
        verify(paperSectionRepository, never()).save(section);
    }

    @Test
    void deleteSectionRejectsInstructorFeedback() {
        User user = instructor();
        Document document = document(project());
        PaperSection section = section(document);
        section.setContentTex("");
        InstructorFeedback feedback = new InstructorFeedback();
        feedback.setSection(section);
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(currentUserService.isInstructor(user)).thenReturn(true);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(document.getId()))
                .thenReturn(List.of(section));
        when(paperSectionRepository.findById(section.getId())).thenReturn(Optional.of(section));
        when(sectionFeedbackRepository.findBySectionId(section.getId())).thenReturn(List.of());
        when(instructorFeedbackRepository.findByRequestProjectId(document.getProject().getId()))
                .thenReturn(List.of(feedback));

        assertThatThrownBy(() -> service().deleteSection(document.getId(), section.getId()))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("feedback");

        verify(paperSectionRepository, never()).save(section);
    }

    @Test
    void resetStandardRejectsSectionsWithActiveClaims() {
        User user = user();
        Project project = project();
        Document document = document(project);
        PaperSection section = section(document);
        section.setContentTex("");
        Claim claim = new Claim();
        claim.setActive(true);
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(currentUserService.isInstructor(user)).thenReturn(true);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(documentRepository.findByProjectIdAndDocTypeAndActiveTrue(
                project.getId(), DocumentType.PAPER)).thenReturn(List.of(document));
        when(documentRepository.save(document)).thenReturn(document);
        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(document.getId()))
                .thenReturn(List.of(section));
        when(claimRepository.findBySectionId(section.getId())).thenReturn(List.of(claim));

        assertThatThrownBy(() -> service().resetSectionsForStandard(project.getId(), "IEEE"))
                .hasMessageContaining("active claims");
        verify(paperSectionRepository, never()).deleteAll(any());
    }

    @Test
    void texExportRemainsAvailableWhenClaimsAreInconsistent() {
        User user = user();
        Project project = project();
        project.setStatus(ProjectStatus.APPROVED);
        Path archive = Path.of("export.zip");
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(texArchiveBuilder.build(project.getId())).thenReturn(archive);

        assertThat(service().exportTexArchive(project.getId())).isEqualTo(archive);
        verify(texArchiveBuilder).build(project.getId());
    }

    @Test
    void createSectionRejectsParentFromAnotherDocument() {
        User user = instructor();
        Document authorized = document(project());
        PaperSection foreignParent = section(document(project()));
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(currentUserService.isInstructor(user)).thenReturn(true);
        when(documentRepository.findById(authorized.getId())).thenReturn(Optional.of(authorized));
        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(authorized.getId()))
                .thenReturn(List.of());
        when(paperSectionRepository.findById(foreignParent.getId())).thenReturn(Optional.of(foreignParent));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service().createSection(
                authorized.getId(), "Conclusion", foreignParent.getId()))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(paperSectionRepository, never()).save(any(PaperSection.class));
    }

    @Test
    private PaperProcessingServiceImpl service() {
        return new PaperProcessingServiceImpl(
                aiModelClient,
                paperSectionRepository,
                claimRepository,
                claimEvidenceMappingRepository,
                instructorFeedbackRepository,
                claimContentConsistencyService,
                sourceCategoryRadarService,
                auditService,
                objectMapper,
                sectionFeedbackRepository,
                documentRepository,
                projectMapper,
                currentUserService,
                paperStandardService,
                userRepository,
                projectRepository,
                systemNotificationService,
                texArchiveBuilder);
    }

    private User user() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(user.getId() + "@example.com");
        user.setRole(UserRole.STUDENT);
        return user;
    }

    private User instructor() {
        User user = user();
        user.setRole(UserRole.INSTRUCTOR);
        return user;
    }

    private Project project() {
        Project project = new Project();
        project.setId(UUID.randomUUID());
        project.setTitle("Capstone");
        project.setActive(true);
        return project;
    }

    private Document document(Project project) {
        Document document = new Document();
        document.setId(UUID.randomUUID());
        document.setProject(project);
        return document;
    }

    private PaperSection section(Document document) {
        PaperSection section = new PaperSection();
        section.setId(UUID.randomUUID());
        section.setDocument(document);
        section.setSectionTitle("Section");
        section.setSectionOrder(0);
        section.setContentTex("Content");
        return section;
    }

    private ClaimEvidenceMapping mapping(String snippet, MappingStatus status) {
        Document source = new Document();
        source.setId(UUID.randomUUID());
        source.setOriginalFilename("source.pdf");
        source.setDocType(DocumentType.SOURCE);
        source.setActive(true);
        DocumentChunk chunk = new DocumentChunk();
        chunk.setDocument(source);
        chunk.setText(snippet);
        chunk.setActive(true);
        ClaimEvidenceMapping mapping = new ClaimEvidenceMapping();
        mapping.setId(UUID.randomUUID());
        mapping.setDocumentChunk(chunk);
        mapping.setStrengthScore(80);
        mapping.setStatus(status);
        return mapping;
    }

    private SourceCategoryRadarResponse radar() {
        return new SourceCategoryRadarResponse("Source categories", 0, List.of());
    }

    private String validReviewJson() {
        return """
                {
                  "findings":[]
                }
                """;
    }
}
