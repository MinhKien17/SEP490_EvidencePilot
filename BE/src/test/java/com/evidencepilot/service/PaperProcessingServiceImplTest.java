package com.evidencepilot.service;

import com.evidencepilot.mapper.ProjectMapper;
import com.evidencepilot.exception.ResourceNotFoundException;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.DocumentText;
import com.evidencepilot.model.PaperSection;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.UserRole;
import com.evidencepilot.model.enums.ProjectStatus;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.repository.PaperSectionRepository;
import com.evidencepilot.service.impl.PaperProcessingServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaperProcessingServiceImplTest {

    @Mock
    private AiModelClient aiModelClient;

    @Mock
    private PaperSectionRepository paperSectionRepository;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private PaperStandardService paperStandardService;

    @Test
    void getPaperSectionsRequiresProjectAccess() {
        User user = user();
        Project project = project();
        Document document = document(project);

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(document.getId()))
                .thenReturn(List.of());

        service().getPaperSections(document.getId());

        verify(currentUserService).requireProjectAccess(user, project);
    }

    @Test
    void reviewRequiresProjectAccess() {
        User user = user();
        Project project = project();
        Document document = document(project);
        DocumentText text = new DocumentText();
        text.setDocument(document);
        text.setExtractedText("Paper text");
        document.setDocumentText(text);

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(aiModelClient.generate(anyString())).thenReturn("ok");

        service().review(document.getId(), null);

        verify(currentUserService).requireProjectAccess(user, project);
    }

    @Test
    void detectAndPersistSectionsReturnsEmptyWithoutExtractedText() {
        Document document = document(project());
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));

        org.assertj.core.api.Assertions.assertThat(service().detectAndPersistSections(document.getId())).isEmpty();
    }

    @Test
    void detectAndPersistSectionsCreatesFullTextSection() {
        Document document = document(project());
        DocumentText text = new DocumentText();
        text.setDocument(document);
        text.setExtractedText("lowercase content without a heading");
        document.setDocumentText(text);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(paperSectionRepository.saveAll(org.mockito.ArgumentMatchers.anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service().detectAndPersistSections(document.getId());

        verify(paperSectionRepository).saveAll(argThat(sections -> {
            var iterator = sections.iterator();
            return iterator.hasNext()
                    && iterator.next().getSectionTitle().equals("Full Text")
                    && !iterator.hasNext();
        }));
    }

    @Test
    void detectAndPersistSectionsKeepsExistingSectionsOnRetry() {
        Document document = document(project());
        DocumentText text = new DocumentText();
        text.setDocument(document);
        text.setExtractedText("Introduction\nExtracted content");
        document.setDocumentText(text);
        PaperSection existing = new PaperSection();
        existing.setId(UUID.randomUUID());
        existing.setDocument(document);
        existing.setSectionTitle("Edited Introduction");
        existing.setSectionOrder(0);

        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(document.getId()))
                .thenReturn(List.of(existing));

        org.assertj.core.api.Assertions.assertThat(service().detectAndPersistSections(document.getId()))
                .hasSize(1);

        verify(paperSectionRepository).findByDocumentIdOrderBySectionOrderAsc(document.getId());
        verifyNoMoreInteractions(paperSectionRepository);
    }

    @Test
    void archivedProjectRejectsSectionMutation() {
        User user = user();
        Project project = project();
        project.setStatus(ProjectStatus.ARCHIVED);
        Document document = document(project);
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        doThrow(new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.CONFLICT, "Project is read-only."))
                .when(currentUserService).requireProjectWriteAccess(user, project);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service().createSection(
                document.getId(), "Conclusion", null))
                .hasMessageContaining("Project is read-only.");
    }

    @Test
    void updateSectionRejectsSectionFromAnotherDocument() {
        User user = user();
        Document authorized = document(project());
        PaperSection foreign = section(document(project()));
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(documentRepository.findById(authorized.getId())).thenReturn(Optional.of(authorized));
        when(paperSectionRepository.findById(foreign.getId())).thenReturn(Optional.of(foreign));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service().updateSection(
                authorized.getId(), foreign.getId(), "Changed", null, null))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(paperSectionRepository, never()).save(any(PaperSection.class));
    }

    @Test
    void mergeRejectsTargetFromAnotherDocument() {
        User user = user();
        Document authorized = document(project());
        PaperSection source = section(authorized);
        PaperSection foreignTarget = section(document(project()));
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(documentRepository.findById(authorized.getId())).thenReturn(Optional.of(authorized));
        when(paperSectionRepository.findById(foreignTarget.getId())).thenReturn(Optional.of(foreignTarget));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service().updateSection(
                authorized.getId(), source.getId(), null, null, foreignTarget.getId()))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(paperSectionRepository, never()).save(any(PaperSection.class));
    }

    @Test
    void createSectionRejectsParentFromAnotherDocument() {
        User user = user();
        Document authorized = document(project());
        PaperSection foreignParent = section(document(project()));
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(documentRepository.findById(authorized.getId())).thenReturn(Optional.of(authorized));
        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(authorized.getId()))
                .thenReturn(List.of());
        when(paperSectionRepository.findById(foreignParent.getId())).thenReturn(Optional.of(foreignParent));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service().createSection(
                authorized.getId(), "Conclusion", foreignParent.getId()))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(paperSectionRepository, never()).save(any(PaperSection.class));
    }

    private PaperProcessingServiceImpl service() {
        return new PaperProcessingServiceImpl(
                aiModelClient,
                paperSectionRepository,
                documentRepository,
                projectMapper,
                currentUserService,
                paperStandardService);
    }

    private User user() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(user.getId() + "@example.com");
        user.setRole(UserRole.STUDENT);
        return user;
    }

    private Project project() {
        Project project = new Project();
        project.setId(UUID.randomUUID());
        project.setTitle("Capstone");
        project.setActive(true);
        return project;
    }

    private Document document(Project project) {
        Document document = new Document();
        document.setId(UUID.randomUUID());
        document.setProject(project);
        return document;
    }

    private PaperSection section(Document document) {
        PaperSection section = new PaperSection();
        section.setId(UUID.randomUUID());
        section.setDocument(document);
        section.setSectionTitle("Section");
        section.setSectionOrder(0);
        section.setContentTex("Content");
        return section;
    }
}
