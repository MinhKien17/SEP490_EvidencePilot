package com.evidencepilot.service.impl;

import com.evidencepilot.mapper.ProjectMapper;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.DocumentText;
import com.evidencepilot.model.PaperSection;
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
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaperProcessingServiceImplTest {

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

        DocumentRepository documentRepository = mock(DocumentRepository.class);
        PaperSectionRepository paperSectionRepository = mock(PaperSectionRepository.class);
        List<PaperSection> saved = new ArrayList<>();
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(documentId)).thenReturn(List.of());
        when(paperSectionRepository.saveAll(anyList())).thenAnswer(invocation -> {
            Iterable<PaperSection> sections = invocation.getArgument(0);
            sections.forEach(saved::add);
            return saved;
        });

        service(documentRepository, paperSectionRepository).detectAndPersistSections(documentId);

        assertThat(saved).extracting(PaperSection::getSectionTitle)
                .containsExactly("Introduction", "Methods");
        assertThat(saved).extracting(PaperSection::getContentTex)
                .containsExactly("First section.", "Second section.\n\\end{document}");
    }

    private static PaperProcessingServiceImpl service(
            DocumentRepository documentRepository, PaperSectionRepository paperSectionRepository) {
        return new PaperProcessingServiceImpl(
                paperSectionRepository,
                mock(InstructorFeedbackRepository.class),
                documentRepository,
                mock(ProjectMapper.class),
                mock(CurrentUserService.class),
                mock(PaperStandardService.class),
                mock(UserRepository.class),
                mock(ProjectRepository.class),
                mock(SystemNotificationService.class),
                mock(TexArchiveBuilder.class));
    }
}
