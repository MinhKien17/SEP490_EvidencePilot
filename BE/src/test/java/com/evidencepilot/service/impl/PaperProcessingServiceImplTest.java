package com.evidencepilot.service.impl;

import com.evidencepilot.mapper.ProjectMapper;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.DocumentText;
import com.evidencepilot.model.PaperSection;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.ProcessingStatus;
import com.evidencepilot.model.enums.ProjectStatus;
import com.evidencepilot.model.enums.UserRole;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.repository.InstructorFeedbackRepository;
import com.evidencepilot.repository.PaperSectionRepository;
import com.evidencepilot.repository.ProjectRepository;
import com.evidencepilot.repository.UserRepository;
import com.evidencepilot.service.CurrentUserService;
import com.evidencepilot.service.PaperStandardService;
import com.evidencepilot.service.SystemNotificationService;
import com.evidencepilot.service.TexArchiveBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaperProcessingServiceImplTest {

    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private PaperSectionRepository paperSectionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ProjectMapper projectMapper;
    @Mock
    private CurrentUserService currentUserService;

    @Test
    void detectsLatexSections() {
        UUID documentId = UUID.randomUUID();
        Document document = new Document();
        document.setId(documentId);
        DocumentText text = new DocumentText();
        text.setDocument(document);
        text.setExtractedText("""
                \\documentclass{article}
                \\begin{document}
                \\section{Introduction}
                First section.
                \\section*{Methods}
                Second section.
                \\end{document}
                """);
        document.setDocumentText(text);

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(documentId)).thenReturn(List.of());
        List<PaperSection> saved = new ArrayList<>();
        when(paperSectionRepository.saveAll(anyList())).thenAnswer(invocation -> {
            Iterable<PaperSection> sections = invocation.getArgument(0);
            sections.forEach(saved::add);
            return saved;
        });

        service().detectAndPersistSections(documentId);

        assertThat(saved).extracting(PaperSection::getSectionTitle)
                .containsExactly("Introduction", "Methods");
        assertThat(saved).extracting(PaperSection::getContentTex)
                .containsExactly("First section.", "Second section.\n\\end{document}");
    }

    @Test
    void assignSectionMovesCreatedProjectToAssigned() {
        User instructor = user(UserRole.INSTRUCTOR);
        User student = user(UserRole.STUDENT);
        Project project = project(ProjectStatus.CREATED);
        Document paper = paper(project);
        PaperSection section = section(paper);
        when(currentUserService.requireCurrentUser()).thenReturn(instructor);
        when(currentUserService.isInstructor(instructor)).thenReturn(true);
        when(documentRepository.findById(paper.getId())).thenReturn(Optional.of(paper));
        when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(paperSectionRepository.findById(section.getId())).thenReturn(Optional.of(section));
        when(paperSectionRepository.save(section)).thenReturn(section);

        service().assignSection(paper.getId(), section.getId(), student.getId());

        assertThat(section.getAssignedUser()).isEqualTo(student);
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.ASSIGNED);
        verify(projectRepository).save(project);
    }

    @Test
    void assignSectionKeepsProjectStatusWhenAlreadyAssigned() {
        User instructor = user(UserRole.INSTRUCTOR);
        User student = user(UserRole.STUDENT);
        Project project = project(ProjectStatus.ASSIGNED);
        Document paper = paper(project);
        PaperSection section = section(paper);
        when(currentUserService.requireCurrentUser()).thenReturn(instructor);
        when(currentUserService.isInstructor(instructor)).thenReturn(true);
        when(documentRepository.findById(paper.getId())).thenReturn(Optional.of(paper));
        when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(paperSectionRepository.findById(section.getId())).thenReturn(Optional.of(section));
        when(paperSectionRepository.save(section)).thenReturn(section);

        service().assignSection(paper.getId(), section.getId(), student.getId());

        assertThat(project.getStatus()).isEqualTo(ProjectStatus.ASSIGNED);
        verify(projectRepository, never()).save(project);
    }

    @Test
    void assignedStudentSavingContentMovesAssignedProjectToInProgress() {
        User student = user(UserRole.STUDENT);
        Project project = project(ProjectStatus.ASSIGNED);
        Document paper = paper(project);
        PaperSection section = section(paper);
        section.setAssignedUser(student);
        when(currentUserService.requireCurrentUser()).thenReturn(student);
        when(documentRepository.findById(paper.getId())).thenReturn(Optional.of(paper));
        when(paperSectionRepository.findById(section.getId())).thenReturn(Optional.of(section));
        when(paperSectionRepository.save(section)).thenReturn(section);

        service().updateSection(paper.getId(), section.getId(), null, null, null, "draft text");

        assertThat(section.getContentTex()).isEqualTo("draft text");
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.IN_PROGRESS);
        verify(projectRepository).save(project);
    }

    @Test
    void emptyUpdateDoesNotMoveAssignedProjectToInProgress() {
        User student = user(UserRole.STUDENT);
        Project project = project(ProjectStatus.ASSIGNED);
        Document paper = paper(project);
        PaperSection section = section(paper);
        section.setAssignedUser(student);
        when(currentUserService.requireCurrentUser()).thenReturn(student);
        when(documentRepository.findById(paper.getId())).thenReturn(Optional.of(paper));
        when(paperSectionRepository.findById(section.getId())).thenReturn(Optional.of(section));
        when(paperSectionRepository.save(section)).thenReturn(section);

        service().updateSection(paper.getId(), section.getId(), null, null, null, null);

        assertThat(project.getStatus()).isEqualTo(ProjectStatus.ASSIGNED);
        verify(projectRepository, never()).save(project);
    }

    @Test
    void instructorSavingContentDoesNotMoveAssignedProject() {
        User instructor = user(UserRole.INSTRUCTOR);
        Project project = project(ProjectStatus.ASSIGNED);
        Document paper = paper(project);
        PaperSection section = section(paper);
        when(currentUserService.requireCurrentUser()).thenReturn(instructor);
        when(documentRepository.findById(paper.getId())).thenReturn(Optional.of(paper));
        when(paperSectionRepository.findById(section.getId())).thenReturn(Optional.of(section));
        when(paperSectionRepository.save(section)).thenReturn(section);

        service().updateSection(paper.getId(), section.getId(), null, null, null, "instructor edit");

        assertThat(project.getStatus()).isEqualTo(ProjectStatus.ASSIGNED);
        verify(projectRepository, never()).save(project);
    }

    private PaperProcessingServiceImpl service() {
        return new PaperProcessingServiceImpl(
                paperSectionRepository,
                mock(InstructorFeedbackRepository.class),
                documentRepository,
                projectMapper,
                currentUserService,
                mock(PaperStandardService.class),
                userRepository,
                projectRepository,
                mock(SystemNotificationService.class),
                mock(TexArchiveBuilder.class));
    }

    private User user(UserRole role) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setRole(role);
        user.setEmail(user.getId() + "@example.com");
        return user;
    }

    private Project project(ProjectStatus status) {
        Project project = new Project();
        project.setId(UUID.randomUUID());
        project.setStatus(status);
        project.setActive(true);
        return project;
    }

    private Document paper(Project project) {
        Document document = new Document();
        document.setId(UUID.randomUUID());
        document.setProject(project);
        document.setProcessingStatus(ProcessingStatus.READY);
        return document;
    }

    private PaperSection section(Document paper) {
        PaperSection section = new PaperSection();
        section.setId(UUID.randomUUID());
        section.setDocument(paper);
        section.setSectionTitle("Intro");
        section.setActive(true);
        return section;
    }
}
