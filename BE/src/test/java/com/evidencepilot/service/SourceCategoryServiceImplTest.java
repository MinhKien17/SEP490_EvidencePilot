package com.evidencepilot.service;

import com.evidencepilot.dto.request.SourceCategoryRequest;
import com.evidencepilot.model.SourceCategory;
import com.evidencepilot.model.User;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.repository.SourceCategoryRepository;
import com.evidencepilot.service.impl.SourceCategoryServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SourceCategoryServiceImplTest {

    private final SourceCategoryRepository categories =
            mock(SourceCategoryRepository.class);
    private final DocumentRepository documents = mock(DocumentRepository.class);
    private final CurrentUserService currentUsers = mock(CurrentUserService.class);
    private final AuditService audit = mock(AuditService.class);
    private final SourceCategoryServiceImpl service =
            new SourceCategoryServiceImpl(categories, documents, currentUsers, audit);

    @Test
    void createNormalizesStableCode() {
        when(categories.save(any(SourceCategory.class))).thenAnswer(invocation -> {
            SourceCategory category = invocation.getArgument(0);
            category.setId(UUID.randomUUID());
            return category;
        });
        when(currentUsers.requireCurrentUser()).thenReturn(new User());

        var response = service.create(new SourceCategoryRequest(
                "ai_ml", "AI and Machine Learning", "Models"));

        assertThat(response.code()).isEqualTo("AI_ML");
        assertThat(response.active()).isTrue();
    }

    @Test
    void deleteRejectsCategoryThatIsInUse() {
        SourceCategory category = category("SE");
        when(categories.findById(category.getId())).thenReturn(Optional.of(category));
        when(documents.countBySourceCategoryId(category.getId())).thenReturn(1L);

        assertThatThrownBy(() -> service.delete(category.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("in use");
        verify(categories, never()).save(category);
    }

    @Test
    void deleteProtectsOtherAndMinimumActiveTaxonomy() {
        SourceCategory other = category("OTHER");
        when(categories.findById(other.getId())).thenReturn(Optional.of(other));
        assertThatThrownBy(() -> service.delete(other.getId()))
                .hasMessageContaining("protected fallback");

        SourceCategory se = category("SE");
        when(categories.findById(se.getId())).thenReturn(Optional.of(se));
        when(categories.countByActiveTrue()).thenReturn(3L);
        assertThatThrownBy(() -> service.delete(se.getId()))
                .hasMessageContaining("At least three");
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
