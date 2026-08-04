package com.evidencepilot.config.infrastructure;

import com.evidencepilot.model.Collection;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.PaperSection;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.ProjectDocument;
import com.evidencepilot.model.ProjectMember;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.model.enums.PaperStandard;
import com.evidencepilot.model.enums.ProcessingStatus;
import com.evidencepilot.model.enums.ProjectRole;
import com.evidencepilot.model.enums.ProjectStatus;
import com.evidencepilot.model.enums.UserRole;
import com.evidencepilot.repository.CollectionRepository;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.repository.PaperSectionRepository;
import com.evidencepilot.repository.ProjectDocumentRepository;
import com.evidencepilot.repository.ProjectMemberRepository;
import com.evidencepilot.repository.ProjectRepository;
import com.evidencepilot.service.DocumentObjectStorage;
import com.evidencepilot.service.PaperStandardService;
import com.evidencepilot.service.impl.DocumentPersistenceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemoDataSeederTest {

    @Mock private DatabaseSeeder databaseSeeder;
    @Mock private ProjectRepository projectRepository;
    @Mock private ProjectMemberRepository projectMemberRepository;
    @Mock private CollectionRepository collectionRepository;
    @Mock private DocumentRepository documentRepository;
    @Mock private ProjectDocumentRepository projectDocumentRepository;
    @Mock private PaperSectionRepository paperSectionRepository;
    @Mock private DocumentPersistenceService documentPersistenceService;
    @Mock private DocumentObjectStorage documentObjectStorage;
    @Mock private PaperStandardService paperStandardService;

    @TempDir
    Path dataDir;

    @Test
    void seedsRequestedDatasetWithoutDuplicatesOnSecondRun() throws Exception {
        for (String filename : List.of(
                "Evaluation-of-the-adipoki.pdf",
                "diagnostics-13-01126.pdf",
                "jpm-11-00976.pdf",
                "Test_Data.pdf")) {
            Files.writeString(dataDir.resolve(filename), filename);
        }

        Map<String, User> users = new HashMap<>();
        List<Project> projects = new ArrayList<>();
        List<Collection> collections = new ArrayList<>();
        List<Document> documents = new ArrayList<>();
        Map<String, ProjectMember> members = new HashMap<>();
        Map<String, ProjectDocument> shares = new HashMap<>();
        Map<UUID, List<PaperSection>> sections = new HashMap<>();
        Set<String> storedObjects = new HashSet<>();

        when(databaseSeeder.ensureUser(
                anyString(), anyString(), anyString(), anyString(), any(UserRole.class)))
                .thenAnswer(invocation -> users.computeIfAbsent(invocation.getArgument(0), email -> {
                    User user = new User();
                    user.setId(UUID.randomUUID());
                    user.setEmail(email);
                    user.setFirstName(invocation.getArgument(2));
                    user.setLastName(invocation.getArgument(3));
                    user.setRole(invocation.getArgument(4));
                    return user;
                }));

        when(projectRepository.findAll()).thenAnswer(invocation -> List.copyOf(projects));
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
            Project project = invocation.getArgument(0);
            if (project.getId() == null) {
                project.setId(UUID.randomUUID());
                projects.add(project);
            }
            return project;
        });

        when(collectionRepository.findAll()).thenAnswer(invocation -> List.copyOf(collections));
        when(collectionRepository.save(any(Collection.class))).thenAnswer(invocation -> {
            Collection collection = invocation.getArgument(0);
            if (collection.getId() == null) {
                collection.setId(UUID.randomUUID());
                collections.add(collection);
            }
            return collection;
        });

        when(projectMemberRepository.findByProjectIdAndUserId(any(UUID.class), any(UUID.class)))
                .thenAnswer(invocation -> {
                    ProjectMember member = members.get(key(invocation.getArgument(0), invocation.getArgument(1)));
                    return member == null ? List.of() : List.of(member);
                });
        when(projectMemberRepository.save(any(ProjectMember.class))).thenAnswer(invocation -> {
            ProjectMember member = invocation.getArgument(0);
            if (member.getId() == null) {
                member.setId(UUID.randomUUID());
            }
            members.put(key(member.getProject().getId(), member.getUser().getId()), member);
            return member;
        });

        when(documentRepository.findByCollectionId(any(UUID.class))).thenAnswer(invocation -> documents.stream()
                .filter(document -> document.getCollection() != null)
                .filter(document -> document.getCollection().getId().equals(invocation.getArgument(0)))
                .toList());
        when(documentRepository.findByProjectIdAndDocTypeAndActiveTrue(any(UUID.class), any(DocumentType.class)))
                .thenAnswer(invocation -> documents.stream()
                        .filter(document -> document.getProject() != null)
                        .filter(document -> document.getProject().getId().equals(invocation.getArgument(0)))
                        .filter(document -> document.getDocType() == invocation.getArgument(1))
                        .filter(Document::isActive)
                        .toList());
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
            Document document = invocation.getArgument(0);
            if (document.getId() == null) {
                document.setId(UUID.randomUUID());
                documents.add(document);
            }
            return document;
        });
        when(documentPersistenceService.savePendingDocument(
                any(), any(), any(User.class), any(DocumentType.class),
                anyString(), anyString(), anyLong()))
                .thenAnswer(invocation -> {
                    Document document = new Document();
                    document.setId(UUID.randomUUID());
                    document.setProject(invocation.getArgument(0));
                    document.setCollection(invocation.getArgument(1));
                    document.setUploadedBy(invocation.getArgument(2));
                    document.setDocType(invocation.getArgument(3));
                    document.setOriginalFilename(invocation.getArgument(4));
                    document.setProcessingStatus(ProcessingStatus.PENDING_UPLOAD);
                    document.setActive(true);
                    documents.add(document);
                    return document;
                });
        when(documentPersistenceService.markDocumentAsUploaded(any(UUID.class), anyString()))
                .thenAnswer(invocation -> {
                    Document document = documents.stream()
                            .filter(candidate -> candidate.getId().equals(invocation.getArgument(0)))
                            .findFirst()
                            .orElseThrow();
                    document.setFileUrl(invocation.getArgument(1));
                    document.setProcessingStatus(ProcessingStatus.UPLOADED);
                    return document;
                });

        when(documentObjectStorage.exists(anyString()))
                .thenAnswer(invocation -> storedObjects.contains(invocation.getArgument(0)));
        doAnswer(invocation -> {
            storedObjects.add(invocation.getArgument(0));
            return null;
        }).when(documentObjectStorage).write(anyString(), any(byte[].class), eq("application/pdf"));

        when(projectDocumentRepository.existsByProjectIdAndDocumentId(any(UUID.class), any(UUID.class)))
                .thenAnswer(invocation -> shares.containsKey(key(invocation.getArgument(0), invocation.getArgument(1))));
        when(projectDocumentRepository.save(any(ProjectDocument.class))).thenAnswer(invocation -> {
            ProjectDocument share = invocation.getArgument(0);
            share.setId(UUID.randomUUID());
            shares.put(key(share.getProject().getId(), share.getDocument().getId()), share);
            return share;
        });

        when(paperSectionRepository.findByDocumentIdOrderBySectionOrderAsc(any(UUID.class)))
                .thenAnswer(invocation -> List.copyOf(sections.getOrDefault(invocation.getArgument(0), List.of())));
        when(paperSectionRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<PaperSection> saved = invocation.getArgument(0);
            for (PaperSection section : saved) {
                section.setId(UUID.randomUUID());
                sections.computeIfAbsent(section.getDocument().getId(), ignored -> new ArrayList<>()).add(section);
            }
            return saved;
        });
        when(paperStandardService.getRequiredSections(PaperStandard.IEEE))
                .thenReturn(List.of("Abstract", "Introduction"));
        when(paperStandardService.getSectionTemplate(eq(PaperStandard.IEEE), anyString()))
                .thenAnswer(invocation -> "% " + invocation.getArgument(1));

        DemoDataSeeder seeder = new DemoDataSeeder(
                databaseSeeder,
                projectRepository,
                projectMemberRepository,
                collectionRepository,
                documentRepository,
                projectDocumentRepository,
                paperSectionRepository,
                documentPersistenceService,
                documentObjectStorage,
                paperStandardService,
                "admin-password",
                "instructor-password",
                "student-password",
                dataDir.toString());

        seeder.run(null);
        seeder.run(null);

        assertThat(users.keySet()).containsExactlyInAnyOrder(
                "admin@evidencepilot.dev",
                "instructor@evidencepilot.dev",
                "instructor2@evidencepilot.dev",
                "instructor3@evidencepilot.dev",
                "student@evidencepilot.dev",
                "student2@evidencepilot.dev",
                "student3@evidencepilot.dev");
        assertThat(projects).extracting(Project::getTitle)
                .containsExactlyInAnyOrder("Project 1", "Project 2", "Project 3");
        assertThat(projects).allMatch(project -> project.getStatus() == ProjectStatus.IN_PROGRESS);
        assertThat(collections).extracting(Collection::getTitle)
                .containsExactlyInAnyOrder("Collection 1", "Collection 2", "Collection 3");
        assertThat(documents).filteredOn(document -> document.getDocType() == DocumentType.SOURCE).hasSize(5);
        assertThat(documents).filteredOn(document -> document.getDocType() == DocumentType.PAPER).hasSize(3);
        assertThat(members).hasSize(8);
        assertThat(shares).hasSize(5);
        assertThat(sections.values()).flatExtracting(value -> value).hasSize(4);
        verify(documentPersistenceService, times(6)).savePendingDocument(
                any(), any(), any(User.class), any(DocumentType.class),
                anyString(), eq("application/pdf"), anyLong());
        verify(documentObjectStorage, times(6))
                .write(anyString(), any(byte[].class), eq("application/pdf"));
    }

    private static String key(UUID first, UUID second) {
        return first + ":" + second;
    }
}
