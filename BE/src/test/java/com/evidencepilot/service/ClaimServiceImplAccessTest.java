package com.evidencepilot.service;

import com.evidencepilot.dto.request.ClaimCreationRequest;
import com.evidencepilot.dto.request.ClaimEvaluationRequest;
import com.evidencepilot.dto.request.MappingReviewRequest;
import com.evidencepilot.dto.response.JobSubmitResponse;
import com.evidencepilot.mapper.ClaimMapper;
import com.evidencepilot.model.AiSuggestion;
import com.evidencepilot.model.Claim;
import com.evidencepilot.model.ClaimEvidenceMapping;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.DocumentChunk;
import com.evidencepilot.model.PaperSection;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.EvidenceRelation;
import com.evidencepilot.model.enums.StrengthBand;
import com.evidencepilot.model.enums.SuggestionStatus;
import com.evidencepilot.model.enums.ProjectStatus;
import com.evidencepilot.model.enums.MappingReviewStatus;
import com.evidencepilot.model.enums.FunctionalType;
import com.evidencepilot.model.enums.MappingStatus;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.repository.AiSuggestionRepository;
import com.evidencepilot.repository.ClaimEvidenceMappingRepository;
import com.evidencepilot.repository.ClaimRepository;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.repository.PaperSectionRepository;
import com.evidencepilot.repository.ProjectMemberRepository;
import com.evidencepilot.repository.ProjectRepository;
import com.evidencepilot.service.AiEvaluationService;
import com.evidencepilot.service.impl.ClaimServiceImpl;
import com.evidencepilot.service.impl.ClaimQualityEvaluationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimServiceImplAccessTest {

    @Mock
    private ClaimRepository claimRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private PaperSectionRepository paperSectionRepository;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private AiSuggestionRepository aiSuggestionRepository;

    @Mock
    private ClaimEvidenceMappingRepository claimEvidenceMappingRepository;

    @Mock
    private ClaimMatchingService claimMatchingService;

    @Mock
    private ClaimQualityEvaluationService claimQualityEvaluationService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private ClaimMapper claimMapper;

    @Mock
    private AiEvaluationService aiEvaluationService;

    @Test
    void submitClaimEvaluationChecksSectionWriteAccessAndQueuesJob() {
        User user = user();
        Project project = new Project();
        project.setId(UUID.randomUUID());
        Document document = new Document();
        document.setProject(project);
        PaperSection section = new PaperSection();
        section.setId(UUID.randomUUID());
        section.setDocument(document);
        ClaimEvaluationRequest request = new ClaimEvaluationRequest(
                section.getId(), "A focused Claim draft");
        UUID jobId = UUID.randomUUID();

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(paperSectionRepository.findById(section.getId())).thenReturn(Optional.of(section));
        when(aiEvaluationService.submit(
                eq(project.getId()), eq(com.evidencepilot.model.AiEvaluationJob.KIND_CLAIM_QUALITY), any()))
                .thenReturn(new JobSubmitResponse(jobId));
        stubSourceDocument(project);

        assertThat(service().submitClaimEvaluation(request).jobId()).isEqualTo(jobId);

        verify(currentUserService).requireSectionContentWriteAccess(user, section);
        verify(aiEvaluationService).submit(
                eq(project.getId()), eq(com.evidencepilot.model.AiEvaluationJob.KIND_CLAIM_QUALITY), any());
        verify(claimRepository, never()).save(any());
    }

    @Test
    void submitClaimEvaluationRejectsProjectWithoutSources() {
        User user = user();
        Project project = new Project();
        Document document = new Document();
        document.setProject(project);
        PaperSection section = new PaperSection();
        section.setId(UUID.randomUUID());
        section.setDocument(document);
        ClaimEvaluationRequest request = new ClaimEvaluationRequest(
                section.getId(), "A focused Claim draft");

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(paperSectionRepository.findById(section.getId())).thenReturn(Optional.of(section));
        when(documentRepository.findByProjectIdAndDocTypeAndActiveTrue(
                project.getId(), DocumentType.SOURCE)).thenReturn(List.of());

        assertThatThrownBy(() -> service().submitClaimEvaluation(request))
                .hasMessageContaining("Attach at least one source");

        verify(aiEvaluationService, never()).submit(any(), any(), any());
        verify(claimRepository, never()).save(any());
    }

    @Test
    void getSuggestionsForClaimRequiresClaimAccess() {
        User user = user();
        Claim claim = claim();

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));
        when(aiSuggestionRepository.findByClaimIdAndClaimVersionOrderByCreatedAtDesc(
                claim.getId(), claim.getClaimVersion())).thenReturn(List.of());

        service().getSuggestionsForClaim(claim.getId());

        verify(currentUserService).requireClaimAccess(user, claim);
    }

    @Test
    void getMappingsForClaimRequiresClaimAccess() {
        User user = user();
        Claim claim = claim();

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));
        when(claimEvidenceMappingRepository.findByClaimId(claim.getId())).thenReturn(List.of());

        service().getMappingsForClaim(claim.getId());

        verify(currentUserService).requireClaimAccess(user, claim);
    }

    @Test
    void searchMatchesRequiresSectionContentWriteAccess() {
        User user = user();
        Claim claim = claim();

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));
        when(claimMatchingService.searchMatches(claim.getId(), claim.getProject().getId()))
                .thenReturn(List.of());

        service().searchMatches(claim.getId());

        verify(currentUserService).requireSectionContentWriteAccess(user, claim.getSection());
    }

    @Test
    void updateSuggestionStatusRequiresSectionContentWriteAccess() {
        User user = user();
        Claim claim = claim();
        AiSuggestion suggestion = suggestion(claim);
        DocumentChunk chunk = new DocumentChunk();
        chunk.setId(UUID.randomUUID());
        suggestion.setDocumentChunk(chunk);

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(aiSuggestionRepository.findById(suggestion.getId())).thenReturn(Optional.of(suggestion));
        when(claimEvidenceMappingRepository.findByClaimIdAndDocumentChunkId(any(), any())).thenReturn(List.of());

        service().updateSuggestionStatus(suggestion.getId(), "ACCEPTED");

        verify(currentUserService).requireSectionContentWriteAccess(user, claim.getSection());
    }

    @Test
    void updateClaimRejectsCompletedProject() {
        User user = user();
        Claim claim = claim();
        claim.getProject().setStatus(ProjectStatus.ARCHIVED);

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));
        doThrow(new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.CONFLICT, "Project is read-only."))
                .when(currentUserService).requireSectionContentWriteAccess(user, claim.getSection());

        assertThatThrownBy(() -> service().updateClaim(claim.getId(), "Updated", null, null))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("Project is read-only.");
    }

    @Test
    void archivedProjectRejectsAdminEvidenceMappingReview() {
        User admin = user();
        admin.setRole(com.evidencepilot.model.enums.UserRole.ADMIN);
        Claim claim = claim();
        claim.getProject().setStatus(ProjectStatus.ARCHIVED);
        ClaimEvidenceMapping mapping = new ClaimEvidenceMapping();
        mapping.setId(UUID.randomUUID());
        mapping.setClaim(claim);

        when(currentUserService.requireCurrentUser()).thenReturn(admin);
        when(claimEvidenceMappingRepository.findById(mapping.getId())).thenReturn(Optional.of(mapping));

        assertThatThrownBy(() -> service().reviewMapping(
                mapping.getId(), new MappingReviewRequest(MappingReviewStatus.VERIFIED, null, null)))
                .hasMessageContaining("Project is read-only.");
    }

    @Test
    void instructorCanReviewMappingWhileProjectIsSubmitted() {
        User instructor = user();
        instructor.setRole(com.evidencepilot.model.enums.UserRole.INSTRUCTOR);
        Claim claim = claim();
        claim.getProject().setStatus(ProjectStatus.SUBMITTED_FOR_REVIEW);
        ClaimEvidenceMapping mapping = new ClaimEvidenceMapping();
        mapping.setId(UUID.randomUUID());
        mapping.setClaim(claim);
        when(currentUserService.requireCurrentUser()).thenReturn(instructor);
        when(currentUserService.isInstructor(instructor)).thenReturn(true);
        when(claimEvidenceMappingRepository.findById(mapping.getId())).thenReturn(Optional.of(mapping));

        service().reviewMapping(
                mapping.getId(),
                new MappingReviewRequest(MappingReviewStatus.VERIFIED, "Reviewed", "SUPPORTS"));

        verify(currentUserService).requireProjectAccess(instructor, claim.getProject());
        verify(claimEvidenceMappingRepository).save(mapping);
        assertThat(mapping.getReviewStatus()).isEqualTo(MappingReviewStatus.VERIFIED);
        assertThat(mapping.getRelationOverride()).isEqualTo(EvidenceRelation.SUPPORTS);
    }

    @Test
    void getAllClaimsFiltersInactiveForAdmin() {
        User admin = user();
        Claim active = claim();
        Claim inactive = claim();
        inactive.setActive(false);
        when(currentUserService.requireCurrentUser()).thenReturn(admin);
        when(currentUserService.isAdmin(admin)).thenReturn(true);
        when(claimRepository.findAll()).thenReturn(List.of(active, inactive));

        assertThat(service().getAllClaims()).hasSize(1);
        verify(claimMapper).toClaimResponse(active);
    }

    @Test
    void getClaimByIdRequiresClaimAccess() {
        User user = user();
        Claim claim = claim();
        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));
        when(currentUserService.requireCurrentUser()).thenReturn(user);

        service().getClaimById(claim.getId());

        verify(currentUserService).requireClaimAccess(user, claim);
    }

    @Test
    void projectClaimQueriesRequireProjectAccess() {
        User user = user();
        Project project = claim().getProject();
        project.setStatus(ProjectStatus.ARCHIVED);
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(claimRepository.findByProjectId(project.getId())).thenReturn(List.of());
        when(claimRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        assertThat(service().getClaimsByProject(project.getId())).isEmpty();
        assertThat(service().getClaimsByProject(
                project.getId(), 0, 20, "createdAt,desc", null, true, null).content()).isEmpty();
        verify(currentUserService, org.mockito.Mockito.times(2)).requireProjectAccess(user, project);
    }

    @Test
    void createClaimPersistsSectionProjectAndVersion() {
        User user = user();
        Project project = claim().getProject();
        Document document = new Document();
        document.setProject(project);
        PaperSection section = new PaperSection();
        section.setId(UUID.randomUUID());
        section.setDocument(document);
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(paperSectionRepository.findById(section.getId())).thenReturn(Optional.of(section));
        when(claimRepository.save(any(Claim.class))).thenAnswer(invocation -> invocation.getArgument(0));
        stubSourceDocument(project);

        service().createClaim(new ClaimCreationRequest(section.getId(), "content", 0.5f, FunctionalType.THEORETICAL));

        verify(currentUserService).requireSectionContentWriteAccess(user, section);
        verify(claimRepository).save(argThat(saved -> saved.getProject() == project
                && saved.getSection() == section && saved.getClaimVersion() == 1 && saved.isActive()
                && saved.getFunctionalType() == FunctionalType.THEORETICAL));
    }

    @Test
    void updateAndDeleteClaimMutateActiveClaim() {
        User user = user();
        Claim claim = claim();
        claim.setClaimVersion(1);
        AiSuggestion pendingSuggestion = suggestion(claim);
        ClaimEvidenceMapping activeMapping = new ClaimEvidenceMapping();
        activeMapping.setStatus(MappingStatus.ACTIVE);
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));
        when(aiSuggestionRepository.findByClaimId(claim.getId()))
                .thenReturn(List.of(pendingSuggestion));
        when(claimEvidenceMappingRepository.findByClaimId(claim.getId()))
                .thenReturn(List.of(activeMapping));

        service().updateClaim(claim.getId(), "updated", 0.9f, FunctionalType.APPLIED);
        assertThat(claim.getContent()).isEqualTo("updated");
        assertThat(claim.getFunctionalType()).isEqualTo(FunctionalType.APPLIED);
        assertThat(claim.getClaimVersion()).isEqualTo(2);
        assertThat(pendingSuggestion.getStatus()).isEqualTo(SuggestionStatus.INVALIDATED);
        assertThat(activeMapping.getStatus()).isEqualTo(MappingStatus.INACTIVE);

        service().deleteClaim(claim.getId());
        assertThat(claim.isActive()).isFalse();
        verify(currentUserService, org.mockito.Mockito.times(2))
                .requireSectionContentWriteAccess(user, claim.getSection());
    }

    @Test
    void acceptAndRejectSuggestionSetExpectedStatus() {
        User user = user();
        Claim claim = claim();
        DocumentChunk chunk = new DocumentChunk();
        chunk.setId(UUID.randomUUID());
        AiSuggestion accepted = suggestion(claim);
        accepted.setDocumentChunk(chunk);
        accepted.setRelation(EvidenceRelation.SUPPORTS);
        accepted.setStrengthScore(45);
        accepted.setStrengthBand(StrengthBand.MEDIUM);
        accepted.setScoreBreakdown("{\"relation\":{\"max\":35,\"earned\":35}}");
        AiSuggestion rejected = suggestion(claim);
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(aiSuggestionRepository.findById(accepted.getId())).thenReturn(Optional.of(accepted));
        when(aiSuggestionRepository.findById(rejected.getId())).thenReturn(Optional.of(rejected));
        when(claimEvidenceMappingRepository.findByClaimIdAndDocumentChunkId(any(), any())).thenReturn(List.of());

        service().acceptSuggestion(accepted.getId());
        service().rejectSuggestion(rejected.getId());

        assertThat(accepted.getStatus()).isEqualTo(SuggestionStatus.ACCEPTED);
        assertThat(rejected.getStatus()).isEqualTo(SuggestionStatus.REJECTED);
        ArgumentCaptor<ClaimEvidenceMapping> mappingCaptor = ArgumentCaptor.forClass(ClaimEvidenceMapping.class);
        verify(claimEvidenceMappingRepository).save(mappingCaptor.capture());
        ClaimEvidenceMapping mapping = mappingCaptor.getValue();
        assertThat(mapping.getRelation()).isEqualTo(EvidenceRelation.SUPPORTS);
        assertThat(mapping.getStrengthScore()).isEqualTo(45);
        assertThat(mapping.getStrengthBand()).isEqualTo(StrengthBand.MEDIUM);
        assertThat(mapping.getScoreBreakdown()).isEqualTo(accepted.getScoreBreakdown());
    }

    private ClaimServiceImpl service() {
        return new ClaimServiceImpl(
                claimRepository,
                projectRepository,
                projectMemberRepository,
                paperSectionRepository,
                documentRepository,
                aiSuggestionRepository,
                claimEvidenceMappingRepository,
                claimMatchingService,
                claimQualityEvaluationService,
                currentUserService,
                claimMapper,
                aiEvaluationService,
                new com.fasterxml.jackson.databind.ObjectMapper());
    }

    private void stubSourceDocument(Project project) {
        Document source = new Document();
        source.setId(UUID.randomUUID());
        source.setDocType(DocumentType.SOURCE);
        when(documentRepository.findByProjectIdAndDocTypeAndActiveTrue(
                project.getId(), DocumentType.SOURCE)).thenReturn(List.of(source));
    }

    private User user() {
        User user = new User();
        user.setId(UUID.randomUUID());
        return user;
    }

    private Claim claim() {
        Project project = new Project();
        project.setId(UUID.randomUUID());
        project.setStatus(ProjectStatus.IN_PROGRESS);
        Claim claim = new Claim();
        claim.setId(UUID.randomUUID());
        claim.setProject(project);
        Document document = new Document();
        document.setProject(project);
        PaperSection section = new PaperSection();
        section.setId(UUID.randomUUID());
        section.setDocument(document);
        claim.setSection(section);
        claim.setActive(true);
        return claim;
    }

    private AiSuggestion suggestion(Claim claim) {
        AiSuggestion suggestion = new AiSuggestion();
        suggestion.setId(UUID.randomUUID());
        suggestion.setClaim(claim);
        suggestion.setStatus(SuggestionStatus.PENDING);
        return suggestion;
    }
}
