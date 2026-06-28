package com.evidencepilot.mapper;

import com.evidencepilot.dto.response.AiSuggestionResponse;
import com.evidencepilot.dto.response.ClaimEvidenceMappingResponse;
import com.evidencepilot.dto.response.ClaimResponse;
import com.evidencepilot.dto.response.EvidenceEdgeResponse;
import com.evidencepilot.model.AiSuggestion;
import com.evidencepilot.model.Claim;
import com.evidencepilot.model.ClaimEvidenceMapping;
import com.evidencepilot.model.EvidenceEdge;
import com.evidencepilot.model.enums.MappingStatus;
import com.evidencepilot.model.enums.SuggestionStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ClaimMapper {

    @Mapping(target = "projectId", source = "project.id")
    @Mapping(target = "sectionId", source = "section.id")
    ClaimResponse toClaimResponse(Claim entity);

    @Mapping(target = "claimId", source = "claim.id")
    @Mapping(target = "documentChunkId", source = "documentChunk.id")
    @Mapping(target = "status", source = "status")
    AiSuggestionResponse toAiSuggestionResponse(AiSuggestion entity);

    @Mapping(target = "claimId", source = "claim.id")
    @Mapping(target = "documentChunkId", source = "documentChunk.id")
    EvidenceEdgeResponse toEvidenceEdgeResponse(EvidenceEdge entity);

    @Mapping(target = "claimId", source = "claim.id")
    @Mapping(target = "documentChunkId", source = "documentChunk.id")
    @Mapping(target = "suggestionId", source = "suggestion.id")
    @Mapping(target = "createdBy", source = "createdBy.id")
    @Mapping(target = "status", source = "status")
    ClaimEvidenceMappingResponse toClaimEvidenceMappingResponse(ClaimEvidenceMapping entity);

    default String mapSuggestionStatus(SuggestionStatus status) {
        return status != null ? status.name() : null;
    }

    default String mapMappingStatus(MappingStatus status) {
        return status != null ? status.name() : null;
    }
}
