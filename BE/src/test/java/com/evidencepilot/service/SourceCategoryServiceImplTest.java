package com.evidencepilot.service;

import com.evidencepilot.dto.request.SourceCategoryRequest;
import com.evidencepilot.model.SourceCategory;
import com.evidencepilot.repository.SourceCategoryRepository;
import com.evidencepilot.service.impl.SourceCategoryServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class SourceCategoryServiceImplTest {

    @Mock
    private SourceCategoryRepository sourceCategoryRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private AuditService auditService;

    @Test
    void listsActiveCategoriesForInstructors() {
        SourceCategory category = category("Journal");

        when(sourceCategoryRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(category));

        var results = service().getActiveCategories();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).name()).isEqualTo("Journal");
    }

    @Test
    void createsCategoryWithUniqueName() {
        com.evidencepilot.model.User admin = admin();
        when(currentUserService.requireCurrentUser()).thenReturn(admin);
        when(sourceCategoryRepository.existsByNameIgnoreCase("Journal")).thenReturn(false);
        when(sourceCategoryRepository.save(any(SourceCategory.class))).thenAnswer(invocation -> {
            SourceCategory category = invocation.getArgument(0);
            category.setId(UUID.randomUUID());
            return category;
        });

        var response = service().create(new SourceCategoryRequest("Journal", "Peer reviewed"));

        assertThat(response.name()).isEqualTo("Journal");
        assertThat(response.active()).isTrue();
        verify(auditService).record(org.mockito.ArgumentMatchers.eq("SOURCE_CATEGORY_CREATED"),
                org.mockito.ArgumentMatchers.eq("SOURCE_CATEGORY"), org.mockito.ArgumentMatchers.eq(response.id()),
                org.mockito.ArgumentMatchers.eq(admin), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.any(java.util.Map.class));
    }

    @Test
    void rejectsDuplicateName() {
        when(sourceCategoryRepository.existsByNameIgnoreCase("Journal")).thenReturn(true);

        assertThatThrownBy(() -> service().create(new SourceCategoryRequest("Journal", null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already exists");
        verifyNoInteractions(auditService);
    }

    @Test
    void failedPersistenceIsNotAudited() {
        when(sourceCategoryRepository.existsByNameIgnoreCase("Journal")).thenReturn(false);
        doThrow(new IllegalStateException("database down"))
                .when(sourceCategoryRepository).save(any(SourceCategory.class));

        assertThatThrownBy(() -> service().create(new SourceCategoryRequest("Journal", null)))
                .isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(auditService);
    }

    @Test
    void updatesCategory() {
        SourceCategory category = category("Old");
        when(currentUserService.requireCurrentUser()).thenReturn(admin());

        when(sourceCategoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(sourceCategoryRepository.existsByNameIgnoreCaseAndIdNot("New", category.getId())).thenReturn(false);
        when(sourceCategoryRepository.save(category)).thenReturn(category);

        var response = service().update(category.getId(), new SourceCategoryRequest("New", "Updated"), false);

        assertThat(response.name()).isEqualTo("New");
        assertThat(response.description()).isEqualTo("Updated");
        assertThat(response.active()).isFalse();
        verify(auditService).record(org.mockito.ArgumentMatchers.eq("SOURCE_CATEGORY_UPDATED"),
                org.mockito.ArgumentMatchers.eq("SOURCE_CATEGORY"), org.mockito.ArgumentMatchers.eq(category.getId()),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(java.util.Map.class),
                org.mockito.ArgumentMatchers.any(java.util.Map.class));
    }

    @Test
    void softDeletesCategory() {
        SourceCategory category = category("Journal");
        when(currentUserService.requireCurrentUser()).thenReturn(admin());

        when(sourceCategoryRepository.findById(category.getId())).thenReturn(Optional.of(category));

        service().delete(category.getId());

        assertThat(category.isActive()).isFalse();
        verify(sourceCategoryRepository).save(category);
        verify(auditService).record(org.mockito.ArgumentMatchers.eq("SOURCE_CATEGORY_DELETED"),
                org.mockito.ArgumentMatchers.eq("SOURCE_CATEGORY"), org.mockito.ArgumentMatchers.eq(category.getId()),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(java.util.Map.class),
                org.mockito.ArgumentMatchers.any(java.util.Map.class));
    }

    private SourceCategoryServiceImpl service() {
        return new SourceCategoryServiceImpl(sourceCategoryRepository, currentUserService, auditService);
    }

    private SourceCategory category(String name) {
        SourceCategory category = new SourceCategory();
        category.setId(UUID.randomUUID());
        category.setName(name);
        category.setActive(true);
        return category;
    }

    private com.evidencepilot.model.User admin() {
        com.evidencepilot.model.User user = new com.evidencepilot.model.User();
        user.setId(UUID.randomUUID());
        user.setEmail("admin@test.com");
        return user;
    }
}
