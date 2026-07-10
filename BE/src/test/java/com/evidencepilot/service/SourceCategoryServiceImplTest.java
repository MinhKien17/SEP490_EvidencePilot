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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SourceCategoryServiceImplTest {

    @Mock
    private SourceCategoryRepository sourceCategoryRepository;

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
        when(sourceCategoryRepository.existsByNameIgnoreCase("Journal")).thenReturn(false);
        when(sourceCategoryRepository.save(any(SourceCategory.class))).thenAnswer(invocation -> {
            SourceCategory category = invocation.getArgument(0);
            category.setId(UUID.randomUUID());
            return category;
        });

        var response = service().create(new SourceCategoryRequest("Journal", "Peer reviewed"));

        assertThat(response.name()).isEqualTo("Journal");
        assertThat(response.active()).isTrue();
    }

    @Test
    void rejectsDuplicateName() {
        when(sourceCategoryRepository.existsByNameIgnoreCase("Journal")).thenReturn(true);

        assertThatThrownBy(() -> service().create(new SourceCategoryRequest("Journal", null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void updatesCategory() {
        SourceCategory category = category("Old");

        when(sourceCategoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(sourceCategoryRepository.existsByNameIgnoreCaseAndIdNot("New", category.getId())).thenReturn(false);
        when(sourceCategoryRepository.save(category)).thenReturn(category);

        var response = service().update(category.getId(), new SourceCategoryRequest("New", "Updated"), false);

        assertThat(response.name()).isEqualTo("New");
        assertThat(response.description()).isEqualTo("Updated");
        assertThat(response.active()).isFalse();
    }

    @Test
    void softDeletesCategory() {
        SourceCategory category = category("Journal");

        when(sourceCategoryRepository.findById(category.getId())).thenReturn(Optional.of(category));

        service().delete(category.getId());

        assertThat(category.isActive()).isFalse();
        verify(sourceCategoryRepository).save(category);
    }

    private SourceCategoryServiceImpl service() {
        return new SourceCategoryServiceImpl(sourceCategoryRepository);
    }

    private SourceCategory category(String name) {
        SourceCategory category = new SourceCategory();
        category.setId(UUID.randomUUID());
        category.setName(name);
        category.setActive(true);
        return category;
    }
}
