package com.evidencepilot.service;

import com.evidencepilot.mapper.ClaimMapper;
import com.evidencepilot.model.AiSuggestion;
import com.evidencepilot.model.Claim;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.ProjectStatus;
import com.evidencepilot.repository.AiSuggestionRepository;
import com.evidencepilot.repository.ClaimEvidenceMappingRepository;
import com.evidencepilot.repository.ClaimRepository;
import com.evidencepilot.repository.EvidenceEdgeRepository;
import com.evidencepilot.repository.PaperSectionRepository;
import com.evidencepilot.repository.ProjectMemberRepository;
import com.evidencepilot.repository.ProjectRepository;
import com.evidencepilot.service.impl.ClaimServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    private AiSuggestionRepository aiSuggestionRepository;

    @Mock
    private ClaimEvidenceMappingRepository claimEvidenceMappingRepository;

    @Mock
    private EvidenceEdgeRepository evidenceEdgeRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private ClaimMapper claimMapper;

    @Test
    void getSuggestionsForClaimRequiresClaimAccess() {
        User user = user();
        Claim claim = claim();

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));
        when(aiSuggestionRepository.findByClaimId(claim.getId())).thenReturn(List.of());

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
    void getEdgesForClaimRequiresClaimAccess() {
        User user = user();
        Claim claim = claim();

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));
        when(evidenceEdgeRepository.findByClaimId(claim.getId())).thenReturn(List.of());

        service().getEdgesForClaim(claim.getId());

        verify(currentUserService).requireClaimAccess(user, claim);
    }

    @Test
    void createSuggestionRequiresProjectWriteAccess() {
        User user = user();
        Claim claim = claim();

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));

        service().createSuggestion(claim.getId(), UUID.randomUUID(), 0.9f, "Matched");

        verify(currentUserService).requireProjectWriteAccess(user, claim.getProject());
    }

    @Test
    void updateSuggestionStatusRequiresProjectWriteAccess() {
        User user = user();
        Claim claim = claim();
        AiSuggestion suggestion = new AiSuggestion();
        suggestion.setId(UUID.randomUUID());
        suggestion.setClaim(claim);

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(aiSuggestionRepository.findById(suggestion.getId())).thenReturn(Optional.of(suggestion));

        service().updateSuggestionStatus(suggestion.getId(), "ACCEPTED");

        verify(currentUserService).requireProjectWriteAccess(user, claim.getProject());
    }

    @Test
    void updateClaimRejectsCompletedProject() {
        User user = user();
        Claim claim = claim();
        claim.getProject().setStatus(ProjectStatus.APPROVED);

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));

        assertThatThrownBy(() -> service().updateClaim(claim.getId(), "Updated", null))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("Project is read-only.");
    }

    private ClaimServiceImpl service() {
        return new ClaimServiceImpl(
                claimRepository,
                projectRepository,
                projectMemberRepository,
                paperSectionRepository,
                aiSuggestionRepository,
                claimEvidenceMappingRepository,
                evidenceEdgeRepository,
                currentUserService,
                claimMapper);
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
        claim.setActive(true);
        return claim;
    }
}
