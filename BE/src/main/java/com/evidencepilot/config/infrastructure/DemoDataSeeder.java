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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
@Order(2)
@ConditionalOnProperty(prefix = "app.seed.demo", name = "enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private static final String ADMIN_EMAIL = "admin@evidencepilot.dev";
    private static final String INSTRUCTOR_1_EMAIL = "instructor@evidencepilot.dev";
    private static final String INSTRUCTOR_2_EMAIL = "instructor2@evidencepilot.dev";
    private static final String INSTRUCTOR_3_EMAIL = "instructor3@evidencepilot.dev";
    private static final String STUDENT_1_EMAIL = "student@evidencepilot.dev";
    private static final String STUDENT_2_EMAIL = "student2@evidencepilot.dev";
    private static final String STUDENT_3_EMAIL = "student3@evidencepilot.dev";

    private static final String ADIPOKI = "Evaluation-of-the-adipoki.pdf";
    private static final String DIAGNOSTICS = "diagnostics-13-01126.pdf";
    private static final String JPM = "jpm-11-00976.pdf";
    private static final String TEST_DATA = "Test_Data.pdf";

    private static final String ADIPOKI_DOI = "10.5114/reum.2022.117839";
    private static final String DIAGNOSTICS_DOI = "10.3390/diagnostics13061126";
    private static final String JPM_DOI = "10.3390/jpm11100976";

    private final DatabaseSeeder databaseSeeder;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final CollectionRepository collectionRepository;
    private final DocumentRepository documentRepository;
    private final ProjectDocumentRepository projectDocumentRepository;
    private final PaperSectionRepository paperSectionRepository;
    private final DocumentPersistenceService documentPersistenceService;
    private final DocumentObjectStorage documentObjectStorage;
    private final PaperStandardService paperStandardService;
    private final String adminPassword;
    private final String instructorPassword;
    private final String studentPassword;
    private final String dataDirectory;

    public DemoDataSeeder(
            DatabaseSeeder databaseSeeder,
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository,
            CollectionRepository collectionRepository,
            DocumentRepository documentRepository,
            ProjectDocumentRepository projectDocumentRepository,
            PaperSectionRepository paperSectionRepository,
            DocumentPersistenceService documentPersistenceService,
            DocumentObjectStorage documentObjectStorage,
            PaperStandardService paperStandardService,
            @Value("${app.admin.password:}") String adminPassword,
            @Value("${app.instructor.password:}") String instructorPassword,
            @Value("${app.student.password:}") String studentPassword,
            @Value("${app.seed.demo.data-dir:}") String dataDirectory) {
        this.databaseSeeder = databaseSeeder;
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.collectionRepository = collectionRepository;
        this.documentRepository = documentRepository;
        this.projectDocumentRepository = projectDocumentRepository;
        this.paperSectionRepository = paperSectionRepository;
        this.documentPersistenceService = documentPersistenceService;
        this.documentObjectStorage = documentObjectStorage;
        this.paperStandardService = paperStandardService;
        this.adminPassword = adminPassword;
        this.instructorPassword = instructorPassword;
        this.studentPassword = studentPassword;
        this.dataDirectory = dataDirectory;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Path dataDir = validateConfiguration();

        databaseSeeder.ensureUser(ADMIN_EMAIL, adminPassword, "Admin", "", UserRole.ADMIN);
        User instructor1 = databaseSeeder.ensureUser(
                INSTRUCTOR_1_EMAIL, instructorPassword, "Instructor", "1", UserRole.INSTRUCTOR);
        databaseSeeder.ensureUser(
                INSTRUCTOR_2_EMAIL, instructorPassword, "Instructor", "2", UserRole.INSTRUCTOR);
        databaseSeeder.ensureUser(
                INSTRUCTOR_3_EMAIL, instructorPassword, "Instructor", "3", UserRole.INSTRUCTOR);
        User student1 = databaseSeeder.ensureUser(
                STUDENT_1_EMAIL, studentPassword, "Student", "1", UserRole.STUDENT);
        User student2 = databaseSeeder.ensureUser(
                STUDENT_2_EMAIL, studentPassword, "Student", "2", UserRole.STUDENT);
        User student3 = databaseSeeder.ensureUser(
                STUDENT_3_EMAIL, studentPassword, "Student", "3", UserRole.STUDENT);

        Project project1 = ensureProject("Project 1", PaperStandard.IEEE);
        Project project2 = ensureProject("Project 2", PaperStandard.IEEE);
        Project project3 = ensureProject("Project 3", PaperStandard.CUSTOM);

        ensureMember(project1, instructor1, ProjectRole.INSTRUCTOR);
        ensureMember(project1, student1, ProjectRole.LEADER);
        ensureMember(project1, student2, ProjectRole.MEMBER);
        ensureMember(project1, student3, ProjectRole.MEMBER);

        ensureMember(project2, instructor1, ProjectRole.INSTRUCTOR);
        ensureMember(project2, student1, ProjectRole.LEADER);

        ensureMember(project3, instructor1, ProjectRole.INSTRUCTOR);
        ensureMember(project3, student1, ProjectRole.LEADER);

        Collection collection1 = ensureCollection("Collection 1", instructor1);
        Collection collection2 = ensureCollection("Collection 2", instructor1);
        Collection collection3 = ensureCollection("Collection 3", instructor1);

        Document adipoki1 = ensureUploadedDocument(
                null, collection1, instructor1, DocumentType.SOURCE, dataDir, ADIPOKI, ADIPOKI_DOI);
        Document diagnostics = ensureUploadedDocument(
                null, collection1, instructor1, DocumentType.SOURCE, dataDir, DIAGNOSTICS, DIAGNOSTICS_DOI);
        Document jpm = ensureUploadedDocument(
                null, collection1, instructor1, DocumentType.SOURCE, dataDir, JPM, JPM_DOI);
        Document testData = ensureUploadedDocument(
                null, collection2, instructor1, DocumentType.SOURCE, dataDir, TEST_DATA, null);
        Document adipoki3 = ensureUploadedDocument(
                null, collection3, instructor1, DocumentType.SOURCE, dataDir, ADIPOKI, ADIPOKI_DOI);

        ensureShared(project1, adipoki1, instructor1);
        ensureShared(project1, diagnostics, instructor1);
        ensureShared(project1, jpm, instructor1);
        ensureShared(project2, testData, instructor1);
        ensureShared(project3, adipoki3, instructor1);

        ensureStandardPaper(project1, instructor1, PaperStandard.IEEE);
        ensureStandardPaper(project2, instructor1, PaperStandard.IEEE);
        ensureUploadedDocument(
                project3, null, instructor1, DocumentType.PAPER, dataDir, ADIPOKI, ADIPOKI_DOI);

        markInProgress(project1);
        markInProgress(project2);
        markInProgress(project3);

        log.info("Ensured demo data: 7 users, 3 projects, 3 collections, 5 sources, and 3 papers");
    }

    private Path validateConfiguration() {
        requireConfigured("ADMIN_PASSWORD", adminPassword);
        requireConfigured("INSTRUCTOR_PASSWORD", instructorPassword);
        requireConfigured("STUDENT_PASSWORD", studentPassword);
        requireConfigured("DEMO_SEED_DATA_DIR", dataDirectory);

        Path dataDir = Path.of(dataDirectory).toAbsolutePath().normalize();
        for (String filename : List.of(ADIPOKI, DIAGNOSTICS, JPM, TEST_DATA)) {
            if (!Files.isRegularFile(dataDir.resolve(filename))) {
                throw new IllegalStateException("Missing demo seed file: " + dataDir.resolve(filename));
            }
        }
        return dataDir;
    }

    private static void requireConfigured(String name, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " must be configured when demo seeding is enabled");
        }
    }

    private Project ensureProject(String title, PaperStandard standard) {
        // ponytail: demo-only linear lookup; add a seed key only if this runs on shared large datasets.
        Project project = projectRepository.findAll().stream()
                .filter(candidate -> title.equals(candidate.getTitle()))
                .findFirst()
                .orElseGet(() -> {
                    Project created = new Project();
                    created.setTitle(title);
                    created.setDescription("EvidencePilot demo seed data");
                    created.setStatus(ProjectStatus.CREATED);
                    created.setTargetStandard(standard);
                    created.setActive(true);
                    created.setCreatedAt(LocalDateTime.now());
                    created.setUpdatedAt(LocalDateTime.now());
                    return projectRepository.save(created);
                });
        if (project.getTargetStandard() != standard) {
            project.setTargetStandard(standard);
            project.setUpdatedAt(LocalDateTime.now());
            project = projectRepository.save(project);
        }
        return project;
    }

    private void ensureMember(Project project, User user, ProjectRole role) {
        List<ProjectMember> existing = projectMemberRepository
                .findByProjectIdAndUserId(project.getId(), user.getId());
        ProjectMember member = existing.isEmpty() ? new ProjectMember() : existing.getFirst();
        member.setProject(project);
        member.setUser(user);
        member.setRole(role);
        if (member.getJoinedAt() == null) {
            member.setJoinedAt(LocalDateTime.now());
        }
        projectMemberRepository.save(member);
    }

    private Collection ensureCollection(String title, User instructor) {
        return collectionRepository.findAll().stream()
                .filter(candidate -> title.equals(candidate.getTitle()))
                .filter(candidate -> candidate.getInstructor() != null
                        && Objects.equals(candidate.getInstructor().getId(), instructor.getId()))
                .findFirst()
                .orElseGet(() -> {
                    Collection created = new Collection();
                    created.setTitle(title);
                    created.setDescription("EvidencePilot demo seed data");
                    created.setInstructor(instructor);
                    created.setActive(true);
                    created.setCreatedAt(LocalDateTime.now());
                    return collectionRepository.save(created);
                });
    }

    private Document ensureUploadedDocument(
            Project project,
            Collection collection,
            User uploadedBy,
            DocumentType type,
            Path dataDir,
            String filename,
            String doi) throws IOException {
        List<Document> scoped = collection != null
                ? documentRepository.findByCollectionId(collection.getId())
                : documentRepository.findByProjectIdAndDocTypeAndActiveTrue(project.getId(), type);
        Document document = scoped.stream()
                .filter(candidate -> candidate.getDocType() == type)
                .filter(Document::isActive)
                .filter(candidate -> filename.equals(candidate.getOriginalFilename()))
                .findFirst()
                .orElse(null);

        Path file = dataDir.resolve(filename);
        if (document == null) {
            document = documentPersistenceService.savePendingDocument(
                    project,
                    collection,
                    uploadedBy,
                    type,
                    filename,
                    "application/pdf",
                    Files.size(file));
        }

        document.setTitle(filename.substring(0, filename.length() - ".pdf".length()));
        document.setDoi(doi);
        document.setActive(true);
        document = documentRepository.save(document);

        String objectKey = "sources/raw/" + document.getId() + ".pdf";
        ProcessingStatus status = document.getProcessingStatus();
        boolean retry = status == ProcessingStatus.PENDING_UPLOAD || status == ProcessingStatus.FAILED;
        if (retry || !documentObjectStorage.exists(objectKey)) {
            documentObjectStorage.write(objectKey, Files.readAllBytes(file), "application/pdf");
            document = documentPersistenceService.markDocumentAsUploaded(document.getId(), objectKey);
        }
        return document;
    }

    private void ensureShared(Project project, Document document, User sharedBy) {
        if (projectDocumentRepository.existsByProjectIdAndDocumentId(project.getId(), document.getId())) {
            return;
        }
        ProjectDocument shared = new ProjectDocument();
        shared.setProject(project);
        shared.setDocument(document);
        shared.setSharedBy(sharedBy);
        shared.setSharedAt(LocalDateTime.now());
        projectDocumentRepository.save(shared);
    }

    private void ensureStandardPaper(Project project, User uploadedBy, PaperStandard standard) {
        List<Document> papers = documentRepository
                .findByProjectIdAndDocTypeAndActiveTrue(project.getId(), DocumentType.PAPER);
        Document paper = papers.isEmpty() ? createStandardPaper(project, uploadedBy, standard) : papers.getFirst();
        if (!("_standard_" + standard.name() + ".tex").equals(paper.getOriginalFilename())) {
            return;
        }

        List<PaperSection> existing = paperSectionRepository
                .findByDocumentIdOrderBySectionOrderAsc(paper.getId());
        int nextOrder = existing.stream()
                .mapToInt(PaperSection::getSectionOrder)
                .max()
                .orElse(-1) + 1;
        List<PaperSection> missing = new java.util.ArrayList<>();
        for (String title : paperStandardService.getRequiredSections(standard)) {
            boolean present = existing.stream().anyMatch(section -> title.equals(section.getSectionTitle()));
            if (!present) {
                PaperSection section = new PaperSection();
                section.setDocument(paper);
                section.setSectionOrder(nextOrder++);
                section.setSectionTitle(title);
                section.setContentTex(paperStandardService.getSectionTemplate(standard, title));
                section.setActive(true);
                section.setUpdatedAt(LocalDateTime.now());
                missing.add(section);
            }
        }
        if (!missing.isEmpty()) {
            paperSectionRepository.saveAll(missing);
        }
    }

    private Document createStandardPaper(Project project, User uploadedBy, PaperStandard standard) {
        Document paper = new Document();
        paper.setProject(project);
        paper.setUploadedBy(uploadedBy);
        paper.setDocType(DocumentType.PAPER);
        paper.setFileUrl("placeholder");
        paper.setOriginalFilename("_standard_" + standard.name() + ".tex");
        paper.setContentType("text/plain");
        paper.setFileSizeBytes(0L);
        paper.setProcessingStatus(ProcessingStatus.READY);
        paper.setActive(true);
        paper.setCreatedAt(LocalDateTime.now());
        paper.setDownloadToken(UUID.randomUUID().toString());
        return documentRepository.save(paper);
    }

    private void markInProgress(Project project) {
        if (project.getStatus() == null
                || project.getStatus() == ProjectStatus.CREATED
                || project.getStatus() == ProjectStatus.ASSIGNED) {
            project.setStatus(ProjectStatus.IN_PROGRESS);
            project.setUpdatedAt(LocalDateTime.now());
            projectRepository.save(project);
        }
    }
}
