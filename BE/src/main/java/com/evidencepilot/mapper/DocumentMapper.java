package com.evidencepilot.mapper;

import com.evidencepilot.dto.response.DocumentChunkResponse;
import com.evidencepilot.dto.response.DocumentResponse;
import com.evidencepilot.dto.response.DocumentTextResponse;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.DocumentChunk;
import com.evidencepilot.model.DocumentText;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DocumentMapper {

    @Mapping(target = "projectId", source = "project.id")
    @Mapping(target = "collectionId", source = "collection.id")
    @Mapping(target = "sourceCategoryId", source = "sourceCategory.id")
    @Mapping(target = "sourceCategoryName", source = "sourceCategory.name")
    @Mapping(target = "uploadedBy", source = "uploadedBy.id")
    DocumentResponse toDocumentResponse(Document entity);

    @Mapping(target = "documentId", source = "document.id")
    DocumentTextResponse toDocumentTextResponse(DocumentText entity);

    @Mapping(target = "documentId", source = "document.id")
    DocumentChunkResponse toDocumentChunkResponse(DocumentChunk entity);
}
