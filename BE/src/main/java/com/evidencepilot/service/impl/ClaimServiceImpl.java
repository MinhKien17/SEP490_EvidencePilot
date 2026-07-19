package com.evidencepilot.service.impl;

import com.evidencepilot.dto.request.ClaimCreationRequest;
import com.evidencepilot.dto.request.MappingReviewRequest;
import com.evidencepilot.dto.response.AiSuggestionResponse;
import com.evidencepilot.dto.response.ClaimEvidenceMappingResponse;
import com.evidencepilot.dto.response.ClaimResponse;
import com.evidencepilot.dto.response.PagedResponse;
import com.evidencepilot.exception.ResourceNotFoundException;
import com.evidencepilot.mapper.ClaimMapper;
import com.evidencepilot.model.AiSuggestion;
import com.evidencepilot.model.Claim;
import com.evidencepilot.model.ClaimEvidenceMapping;
import com.evidencepilot.model.DocumentChunk;
import com.evidencepilot.model.PaperSection;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.model.enums.MappingReviewStatus;
import com.evidencepilot.model.enums.MappingStatus;
import com.evidencepilot.model.enums.SuggestionStatus;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.ProjectStatus;
import com.evidencepilot.repository.AiSuggestionRepository;
import com.evidencepilot.repository.ClaimEvidenceMappingRepository;
import com.evidencepilot.repository.ClaimRepository;
import com.evidencepilot.repository.DocumentChunkRepository;
import com.evidencepilot.repository.PaperSectionRepository;
import com.evidencepilot.repository.ProjectMemberRepository;
import com.evidencepilot.repository.ProjectRepository;
import com.evidencepilot.service.ClaimMatchingService;
import com.evidencepilot.service.ClaimService;
import com.evidencepilot.service.CurrentUserService;
import com.evidencepilot.dto.request.PagingRequest;
import jakarta.persistence.criteria.Predicate;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClaimServiceImpl implements ClaimService {

    private static final Set<String> CLAIM_SORT_FIELDS = Set.of(
            "createdAt", "claimVersion", "aiConfidenceScore");

    private final ClaimRepository claimRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final PaperSectionRepository paperSectionRepository;
    private final AiSuggestionRepository aiSuggestionRepository;
    private final ClaimEvidenceMappingRepository claimEvidenceMappingRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final ClaimMatchingService claimMatchingService;
    private final CurrentUserService currentUserService;
    private final ClaimMapper claimMapper;

    @Override
    public List<ClaimResponse> getAllClaims() {
        User currentUser = currentUserService.requireCurrentUser();
        if (currentUserService.isAdmin(currentUser)) {
            return claimRepository.findAll().stream()
                    .filter(Claim::isActive)
                    .map(claimMapper::toClaimResponse)
                    .toList();
        }
        List<UUID> projectIds = projectMemberRepository.findByUserId(currentUser.getId()).stream()
                .map(m -> m.getProject().getId())
                .toList();
        return projectIds.stream()
                .flatMap(pid -> claimRepository.findByProjectId(pid).stream())
                .filter(Claim::isActive)
                .map(claimMapper::toClaimResponse)
                .toList();
    }

    @Override
    public PagedResponse<ClaimResponse> getAllClaims(
            int page,
            int size,
            String sort,
            String q,
            Boolean active) {
        User currentUser = currentUserService.requireCurrentUser();
        var pageable = PagingRequest.pageable(
                page, size, sort, CLAIM_SORT_FIELDS, "createdAt,desc");
        var results = claimRepository.findAll(
                claimSpec(currentUser, null, active, q),
                pageable);
        return PagedResponse.from(results.map(claimMapper::toClaimResponse));
    }

    @Override
    public ClaimResponse getClaimById(UUID id) {
        Claim claim = findActiveClaim(id);
        User currentUser = currentUserService.requireCurrentUser();
        currentUserService.requireClaimAccess(currentUser, claim);
        return claimMapper.toClaimResponse(claim);
    }

    @Override
    public List<ClaimResponse> getClaimsByProject(UUID projectId) {
        User currentUser = currentUserService.requireCurrentUser();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(projectId, "Project"));
        currentUserService.requireProjectAccess(currentUser, project);
        return claimRepository.findByProjectId(projectId).stream()
                .filter(Claim::isActive)
                .map(claimMapper::toClaimResponse)
                .toList();
    }

    @Override
    public PagedResponse<ClaimResponse> getClaimsByProject(
            UUID projectId,
            int page,
            int size,
            String sort,
            String q,
            Boolean active) {
        User currentUser = currentUserService.requireCurrentUser();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(projectId, "Project"));
        currentUserService.requireProjectAccess(currentUser, project);

        var pageable = PagingRequest.pageable(
                page, size, sort, CLAIM_SORT_FIELDS, "createdAt,desc");
        var results = claimRepository.findAll(
                claimSpec(currentUser, projectId, active, q),
                pageable);
        return PagedResponse.from(results.map(claimMapper::toClaimResponse));
    }

    @Override
    @Transactional
    public ClaimResponse createClaim(ClaimCreationRequest request) {
        User currentUser = currentUserService.requireCurrentUser();

        PaperSection section = paperSectionRepository.findById(request.sectionId())
                .orElseThrow(() -> new ResourceNotFoundException(request.sectionId(), "PaperSection"));
        Project project = section.getDocument().getProject();

        requireProjectContentWriteAccess(currentUser, project);

        Claim claim = new Claim();
        claim.setProject(project);
        claim.setSection(section);
        claim.setContent(request.content());
        claim.setAiConfidenceScore(request.aiConfidenceScore());
        claim.setClaimVersion(1);
        claim.setActive(true);
        claim.setCreatedAt(LocalDateTime.now());

        return claimMapper.toClaimResponse(claimRepository.save(claim));
    }

    @Override
    @Transactional
    public ClaimResponse updateClaim(UUID id, String content, Float aiConfidenceScore) {
        Claim claim = findActiveClaim(id);
        User currentUser = currentUserService.requireCurrentUser();
        requireProjectContentWriteAccess(currentUser, claim.getProject());

        claim.setContent(content);
        if (aiConfidenceScore != null) {
            claim.setAiConfidenceScore(aiConfidenceScore);
        }
        claim.setClaimVersion(claim.getClaimVersion() + 1);

        return claimMapper.toClaimResponse(claimRepository.save(claim));
    }

    @Override
    @Transactional
    public void deleteClaim(UUID id) {
        Claim claim = findActiveClaim(id);
        User currentUser = currentUserService.requireCurrentUser();
        requireProjectContentWriteAccess(currentUser, claim.getProject());
        claim.setActive(false);
        claimRepository.save(claim);
    }

    @Override
    public List<AiSuggestionResponse> getSuggestionsForClaim(UUID claimId) {
        requireClaimAccess(claimId);
        return aiSuggestionRepository.findByClaimId(claimId).stream()
                .map(claimMapper::toAiSuggestionResponse)
                .toList();
    }

    @Override
    @Transactional
    public AiSuggestionResponse createSuggestion(UUID claimId, UUID documentChunkId,
            Float score, String explanation) {
        Claim claim = requireClaimWriteAccess(claimId);

        DocumentChunk chunk = documentChunkRepository.findById(documentChunkId)
                .orElseThrow(() -> new ResourceNotFoundException(documentChunkId, "DocumentChunk"));
        if (!chunk.isActive() || chunk.getDocument() == null || !chunk.getDocument().isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Document chunk is not active or belongs to an inactive document");
        }
        if (chunk.getDocument().getDocType() != DocumentType.SOURCE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Document chunk must belong to a SOURCE document");
        }

        AiSuggestion suggestion = new AiSuggestion();
        suggestion.setClaim(claim);
        suggestion.setDocumentChunk(chunk);
        suggestion.setStatus(SuggestionStatus.PENDING);
        suggestion.setScore(score);
        suggestion.setExplanation(explanation);
        suggestion.setClaimVersion(claim.getClaimVersion());
        suggestion.setCreatedAt(LocalDateTime.now());

        return claimMapper.toAiSuggestionResponse(aiSuggestionRepository.save(suggestion));
    }

    @Override
    @Transactional
    public void acceptSuggestion(UUID suggestionId) {
        AiSuggestion suggestion = requireSuggestionWriteAccess(suggestionId);
        suggestion.setStatus(SuggestionStatus.ACCEPTED);
        aiSuggestionRepository.save(suggestion);

        User currentUser = currentUserService.requireCurrentUser();
        DocumentChunk chunk = suggestion.getDocumentChunk();
        if (chunk == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Suggestion has no associated document chunk");
        }

        List<ClaimEvidenceMapping> existing = claimEvidenceMappingRepository
                .findByClaimIdAndDocumentChunkId(suggestion.getClaim().getId(), chunk.getId());
        if (existing.isEmpty()) {
            ClaimEvidenceMapping mapping = new ClaimEvidenceMapping();
            mapping.setClaim(suggestion.getClaim());
            mapping.setDocumentChunk(chunk);
            mapping.setSuggestion(suggestion);
            mapping.setCreatedBy(currentUser);
            mapping.setStatus(MappingStatus.ACTIVE);
            mapping.setReviewStatus(MappingReviewStatus.PENDING);
            mapping.setCreatedAt(LocalDateTime.now());
            claimEvidenceMappingRepository.save(mapping);
        }
    }

    @Override
    @Transactional
    public void rejectSuggestion(UUID suggestionId) {
        AiSuggestion suggestion = requireSuggestionWriteAccess(suggestionId);
        suggestion.setStatus(SuggestionStatus.REJECTED);
        aiSuggestionRepository.save(suggestion);
    }

    @Override
    @Transactional
    public void updateSuggestionStatus(UUID suggestionId, String status) {
        SuggestionStatus newStatus;
        try {
            newStatus = SuggestionStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: " + status);
        }
        if (newStatus == SuggestionStatus.ACCEPTED) {
            acceptSuggestion(suggestionId);
        } else if (newStatus == SuggestionStatus.REJECTED) {
            rejectSuggestion(suggestionId);
        }
    }

    @Override
    public List<ClaimEvidenceMappingResponse> getMappingsForClaim(UUID claimId) {
        requireClaimAccess(claimId);
        return claimEvidenceMappingRepository.findByClaimId(claimId).stream()
                .map(claimMapper::toClaimEvidenceMappingResponse)
                .toList();
    }

    @Override
    @Transactional
    public ClaimEvidenceMappingResponse reviewMapping(UUID mappingId, MappingReviewRequest request) {
        ClaimEvidenceMapping mapping = claimEvidenceMappingRepository.findById(mappingId)
                .orElseThrow(() -> new ResourceNotFoundException(mappingId, "ClaimEvidenceMapping"));
        User currentUser = currentUserService.requireCurrentUser();
        currentUserService.requireProjectWriteAccess(currentUser, mapping.getClaim().getProject());

        mapping.setReviewStatus(request.reviewStatus());
        mapping.setReviewNote(request.reviewNote());
        mapping.setReviewedBy(currentUser);
        mapping.setReviewedAt(LocalDateTime.now());

        if (request.relationOverride() != null && !request.relationOverride().isBlank()) {
            try {
                mapping.setRelationOverride(
                        com.evidencepilot.model.enums.EvidenceRelation.valueOf(request.relationOverride().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid relationOverride value");
            }
        }

        return claimMapper.toClaimEvidenceMappingResponse(
                claimEvidenceMappingRepository.save(mapping));
    }

    @Override
    @Transactional
    public List<AiSuggestionResponse> generateSuggestions(UUID claimId) {
        Claim claim = findActiveClaim(claimId);
        User currentUser = currentUserService.requireCurrentUser();
        requireProjectContentWriteAccess(currentUser, claim.getProject());
        return claimMatchingService.matchClaim(claimId, claim.getProject().getId());
    }

    private Claim requireClaimAccess(UUID claimId) {
        Claim claim = findActiveClaim(claimId);
        User currentUser = currentUserService.requireCurrentUser();
        currentUserService.requireClaimAccess(currentUser, claim);
        return claim;
    }

    private Claim requireClaimWriteAccess(UUID claimId) {
        Claim claim = findActiveClaim(claimId);
        User currentUser = currentUserService.requireCurrentUser();
        requireProjectContentWriteAccess(currentUser, claim.getProject());
        return claim;
    }

    private AiSuggestion requireSuggestionWriteAccess(UUID suggestionId) {
        AiSuggestion suggestion = aiSuggestionRepository.findById(suggestionId)
                .orElseThrow(() -> new ResourceNotFoundException(suggestionId, "AiSuggestion"));
        User currentUser = currentUserService.requireCurrentUser();
        requireProjectContentWriteAccess(currentUser, suggestion.getClaim().getProject());
        return suggestion;
    }

    private void requireProjectContentWriteAccess(User currentUser, Project project) {
        currentUserService.requireProjectWriteAccess(currentUser, project);
        if (project.getStatus() == ProjectStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Project is read-only.");
        }
    }

    private Claim findActiveClaim(UUID id) {
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(id, "Claim"));
        if (!claim.isActive()) {
            throw new ResourceNotFoundException(id, "Claim");
        }
        return claim;
    }

    private Specification<Claim> claimSpec(
            User currentUser,
            UUID projectId,
            Boolean active,
            String q) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("active"), active != null ? active : true));

            if (projectId != null) {
                predicates.add(cb.equal(root.get("project").get("id"), projectId));
            } else if (!currentUserService.isAdmin(currentUser)) {
                if (query != null) {
                    query.distinct(true);
                }
                var project = root.join("project");
                var members = project.join("projectMembers");
                predicates.add(cb.equal(members.get("user").get("id"), currentUser.getId()));
            }

            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.like(cb.lower(root.get("content")), like));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
