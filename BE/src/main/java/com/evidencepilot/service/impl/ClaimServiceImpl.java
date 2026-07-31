package com.evidencepilot.service.impl;

import com.evidencepilot.dto.request.ClaimCreationRequest;
import com.evidencepilot.dto.request.MappingReviewRequest;
import com.evidencepilot.dto.response.AiSuggestionResponse;
import com.evidencepilot.dto.response.ClaimEvidenceMappingResponse;
import com.evidencepilot.dto.response.ClaimMatchCandidateResponse;
import com.evidencepilot.dto.response.ClaimResponse;
import com.evidencepilot.dto.response.ClaimSourceAuditResponse;
import com.evidencepilot.dto.response.ClaimSourceAuditResponse.ClaimAuditItem;
import com.evidencepilot.dto.response.ClaimSourceAuditResponse.MappingAuditItem;
import com.evidencepilot.dto.response.PagedResponse;
import com.evidencepilot.exception.ResourceNotFoundException;
import com.evidencepilot.mapper.ClaimMapper;
import com.evidencepilot.model.AiSuggestion;
import com.evidencepilot.model.Claim;
import com.evidencepilot.model.ClaimEvidenceMapping;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.DocumentChunk;
import com.evidencepilot.model.PaperSection;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.enums.FunctionalType;
import com.evidencepilot.model.enums.MappingReviewStatus;
import com.evidencepilot.model.enums.MappingStatus;
import com.evidencepilot.model.enums.SuggestionStatus;
import com.evidencepilot.model.User;
import com.evidencepilot.repository.AiSuggestionRepository;
import com.evidencepilot.repository.ClaimEvidenceMappingRepository;
import com.evidencepilot.repository.ClaimRepository;
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
import java.util.Objects;
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

        currentUserService.requireSectionContentWriteAccess(currentUser, section);

        Claim claim = new Claim();
        claim.setProject(project);
        claim.setSection(section);
        claim.setCreatedBy(currentUser);
        claim.setContent(request.content());
        claim.setAiConfidenceScore(request.aiConfidenceScore());
        claim.setFunctionalType(request.functionalType());
        claim.setClaimVersion(1);
        claim.setActive(true);
        claim.setCreatedAt(LocalDateTime.now());

        return claimMapper.toClaimResponse(claimRepository.save(claim));
    }

    @Override
    @Transactional
    public ClaimResponse updateClaim(UUID id, String content, Float aiConfidenceScore, FunctionalType functionalType) {
        Claim claim = findActiveClaim(id);
        User currentUser = currentUserService.requireCurrentUser();
        requireClaimContentWriteAccess(currentUser, claim);

        claim.setContent(content);
        if (aiConfidenceScore != null) {
            claim.setAiConfidenceScore(aiConfidenceScore);
        }
        if (functionalType != null) {
            claim.setFunctionalType(functionalType);
        }
        claim.setClaimVersion(claim.getClaimVersion() + 1);
        claim.setUpdatedAt(LocalDateTime.now());

        List<AiSuggestion> invalidated = aiSuggestionRepository.findByClaimId(id).stream()
                .filter(suggestion -> suggestion.getStatus() == SuggestionStatus.PENDING)
                .peek(suggestion -> suggestion.setStatus(SuggestionStatus.INVALIDATED))
                .toList();
        aiSuggestionRepository.saveAll(invalidated);

        List<ClaimEvidenceMapping> deactivated = claimEvidenceMappingRepository.findByClaimId(id).stream()
                .filter(mapping -> mapping.getStatus() == MappingStatus.ACTIVE)
                .peek(mapping -> mapping.setStatus(MappingStatus.INACTIVE))
                .toList();
        claimEvidenceMappingRepository.saveAll(deactivated);

        return claimMapper.toClaimResponse(claimRepository.save(claim));
    }

    @Override
    @Transactional
    public void deleteClaim(UUID id) {
        Claim claim = findActiveClaim(id);
        User currentUser = currentUserService.requireCurrentUser();
        requireClaimContentWriteAccess(currentUser, claim);
        claim.setActive(false);
        claimRepository.save(claim);
    }

    @Override
    public List<AiSuggestionResponse> getSuggestionsForClaim(UUID claimId) {
        Claim claim = requireClaimAccess(claimId);
        return aiSuggestionRepository
                .findByClaimIdAndClaimVersionOrderByCreatedAtDesc(
                        claimId, claim.getClaimVersion())
                .stream()
                .map(claimMapper::toAiSuggestionResponse)
                .toList();
    }

    @Override
    public List<ClaimMatchCandidateResponse> searchMatches(UUID claimId) {
        Claim claim = requireClaimWriteAccess(claimId);
        return claimMatchingService.searchMatches(claimId, claim.getProject().getId());
    }

    @Override
    public AiSuggestionResponse evaluateMatch(UUID claimId, UUID documentChunkId) {
        Claim claim = requireClaimWriteAccess(claimId);
        return claimMatchingService.evaluateMatch(
                claimId, claim.getProject().getId(), documentChunkId);
    }

    @Override
    @Transactional
    public void acceptSuggestion(UUID suggestionId) {
        AiSuggestion suggestion = requireSuggestionWriteAccess(suggestionId);
        requirePendingCurrentSuggestion(suggestion);
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
        ClaimEvidenceMapping mapping = existing.isEmpty()
                ? new ClaimEvidenceMapping()
                : existing.get(0);
        mapping.setClaim(suggestion.getClaim());
        mapping.setDocumentChunk(chunk);
        mapping.setSuggestion(suggestion);
        mapping.setCreatedBy(currentUser);
        mapping.setRelation(suggestion.getRelation());
        mapping.setStrengthScore(suggestion.getStrengthScore());
        mapping.setStrengthBand(suggestion.getStrengthBand());
        mapping.setScoreBreakdown(suggestion.getScoreBreakdown());
        mapping.setStatus(MappingStatus.ACTIVE);
        mapping.setReviewStatus(MappingReviewStatus.PENDING);
        mapping.setInstructorRejected(false);
        mapping.setReviewedBy(null);
        mapping.setReviewedAt(null);
        mapping.setReviewNote(null);
        mapping.setRelationOverride(null);
        mapping.setCreatedAt(LocalDateTime.now());
        claimEvidenceMappingRepository.save(mapping);
    }

    @Override
    @Transactional
    public void rejectSuggestion(UUID suggestionId) {
        AiSuggestion suggestion = requireSuggestionWriteAccess(suggestionId);
        requirePendingCurrentSuggestion(suggestion);
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
        } else {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Status must be ACCEPTED or REJECTED");
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
        Project project = mapping.getClaim().getProject();
        if (project.getStatus().isReadOnly()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Project is read-only.");
        }
        if (!currentUserService.isAdmin(currentUser)) {
            if (!currentUserService.isInstructor(currentUser)) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Only instructors can review mappings");
            }
            currentUserService.requireProjectAccess(currentUser, project);
        }

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

    private Claim requireClaimAccess(UUID claimId) {
        Claim claim = findActiveClaim(claimId);
        User currentUser = currentUserService.requireCurrentUser();
        currentUserService.requireClaimAccess(currentUser, claim);
        return claim;
    }

    private Claim requireClaimWriteAccess(UUID claimId) {
        Claim claim = findActiveClaim(claimId);
        User currentUser = currentUserService.requireCurrentUser();
        requireClaimContentWriteAccess(currentUser, claim);
        return claim;
    }

    private AiSuggestion requireSuggestionWriteAccess(UUID suggestionId) {
        AiSuggestion suggestion = aiSuggestionRepository.findById(suggestionId)
                .orElseThrow(() -> new ResourceNotFoundException(suggestionId, "AiSuggestion"));
        User currentUser = currentUserService.requireCurrentUser();
        requireClaimContentWriteAccess(currentUser, suggestion.getClaim());
        return suggestion;
    }

    private void requirePendingCurrentSuggestion(AiSuggestion suggestion) {
        if (suggestion.getStatus() != SuggestionStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Suggestion decision is final");
        }
        if (!Objects.equals(
                suggestion.getClaimVersion(),
                suggestion.getClaim().getClaimVersion())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Suggestion belongs to an older claim version");
        }
    }

    private void requireClaimContentWriteAccess(User currentUser, Claim claim) {
        if (claim.getSection() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Claim has no owning section and is read-only.");
        }
        currentUserService.requireSectionContentWriteAccess(currentUser, claim.getSection());
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

    @Override
    public ClaimSourceAuditResponse auditClaimSources(UUID projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(projectId, "Project"));
        currentUserService.requireProjectAccess(currentUserService.requireCurrentUser(), project);

        List<Claim> claims = claimRepository.findByProjectId(projectId).stream()
                .filter(Claim::isActive).toList();
        List<ClaimAuditItem> items = new ArrayList<>();
        int totalMappings = 0, claimsWithNoSources = 0, claimsWithWeakSources = 0;

        for (Claim c : claims) {
            List<ClaimEvidenceMapping> mappings = claimEvidenceMappingRepository.findByClaimId(c.getId());
            List<MappingAuditItem> mappingItems = mappings.stream().map(m -> {
                DocumentChunk chunk = m.getDocumentChunk();
                if (chunk == null) return null;
                Document doc = chunk.getDocument();
                if (doc == null) return null;
                return new MappingAuditItem(
                        m.getId(),
                        doc.getOriginalFilename(),
                        doc.isActive(),
                        chunk.isActive(),
                        m.getStrengthScore(),
                        m.getStrengthBand() != null ? m.getStrengthBand().name() : null,
                        m.getReviewStatus() != null ? m.getReviewStatus().name() : null);
            }).filter(java.util.Objects::nonNull).toList();

            totalMappings += mappingItems.size();
            if (mappingItems.isEmpty()) claimsWithNoSources++;
            long activeMappings = mappingItems.stream().filter(m -> m.sourceActive() && m.chunkActive()).count();
            if (activeMappings == 0 && !mappingItems.isEmpty()) claimsWithWeakSources++;

            items.add(new ClaimAuditItem(c.getId(), c.getContent(), c.getSection() != null ? c.getSection().getId() : null, mappingItems.size(), mappingItems));
        }

        return new ClaimSourceAuditResponse(projectId, claims.size(), totalMappings, claimsWithNoSources, claimsWithWeakSources, items);
    }
}
