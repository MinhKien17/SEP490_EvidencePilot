package com.evidencepilot.service;

import com.evidencepilot.model.Document;
import com.evidencepilot.model.SourceCategory;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.repository.SourceCategoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SourceCategoryClassifierTest {

    @Test
    void assignsAiSelectedActiveCategory() {
        DocumentRepository documents = mock(DocumentRepository.class);
        SourceCategoryRepository categories = mock(SourceCategoryRepository.class);
        AiModelClient ai = mock(AiModelClient.class);
        SourceCategoryClassifier classifier =
                new SourceCategoryClassifier(documents, categories, ai, new ObjectMapper());
        Document source = new Document();
        source.setId(UUID.randomUUID());
        source.setDocType(DocumentType.SOURCE);
        SourceCategory aiMl = category("AI_ML");
        SourceCategory other = category("OTHER");
        when(documents.findById(source.getId())).thenReturn(Optional.of(source));
        when(categories.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(aiMl, other));
        when(ai.generate(contains("neural network"))).thenReturn("{\"code\":\"AI_ML\"}");

        classifier.classifyIfMissing(source.getId(), "A neural network study");

        assertThat(source.getSourceCategory()).isSameAs(aiMl);
        verify(documents).save(source);
    }

    private static SourceCategory category(String code) {
        SourceCategory category = new SourceCategory();
        category.setId(UUID.randomUUID());
        category.setCode(code);
        category.setName(code);
        category.setActive(true);
        return category;
    }
}
