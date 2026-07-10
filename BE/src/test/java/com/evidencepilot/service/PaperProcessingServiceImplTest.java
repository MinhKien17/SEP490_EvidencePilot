package com.evidencepilot.service;

import com.evidencepilot.mapper.ProjectMapper;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.DocumentText;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.UserRole;
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
import static org.mockito.Mockito.verify;
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

    private PaperProcessingServiceImpl service() {
        return new PaperProcessingServiceImpl(
                aiModelClient,
                paperSectionRepository,
                documentRepository,
                projectMapper,
                currentUserService);
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
}
