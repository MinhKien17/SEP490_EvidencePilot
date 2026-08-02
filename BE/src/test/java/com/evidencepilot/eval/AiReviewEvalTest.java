package com.evidencepilot.eval;

import com.evidencepilot.dto.response.AiReviewResponse;
import com.evidencepilot.mapper.ProjectMapper;
import com.evidencepilot.model.Claim;
import com.evidencepilot.model.ClaimEvidenceMapping;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.DocumentChunk;
import com.evidencepilot.model.InstructorFeedback;
import com.evidencepilot.model.PaperSection;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.model.enums.MappingStatus;
import com.evidencepilot.model.enums.UserRole;
import com.evidencepilot.repository.ClaimEvidenceMappingRepository;
import com.evidencepilot.repository.ClaimRepository;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.repository.InstructorFeedbackRepository;
import com.evidencepilot.repository.PaperSectionRepository;
import com.evidencepilot.repository.ProjectRepository;
import com.evidencepilot.repository.ReviewSnapshotRepository;
import com.evidencepilot.repository.UserRepository;
import com.evidencepilot.service.AiModelClient;
import com.evidencepilot.service.AuditService;
import com.evidencepilot.service.ClaimContentConsistencyService;
import com.evidencepilot.service.CurrentUserService;
import com.evidencepilot.service.EvidenceFilterService;
import com.evidencepilot.service.PaperProcessingService;
import com.evidencepilot.service.PaperStandardService;
import com.evidencepilot.service.SystemNotificationService;
import com.evidencepilot.service.TexArchiveBuilder;
import com.evidencepilot.service.impl.PaperProcessingServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// ponytail: offline eval harness; gold papers live in src/test/resources/eval/gold.
// AI chunk review + assertion alignment are stubbed (deterministic findings only).
// Live AI eval: follow-up once the gold set matures.
class AiReviewEvalTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deterministicFindingsMatchGoldLabels() throws Exception {
        Path goldDir = goldDir();
        Assumptions.assumeTrue(goldDir != null, "no gold papers yet — drop labeled papers into eval/gold");

        List<Path> goldFiles;
        try (Stream<Path> files = Files.list(goldDir)) {
            goldFiles = files.filter(path -> path.toString().endsWith(".json")).toList();
        }
        Assumptions.assumeTrue(!goldFiles.isEmpty(), "no gold papers yet — drop labeled papers into eval/gold");

        int totalGold = 0;
        int totalMatched = 0;
        int totalActual = 0;
        for (Path goldFile : goldFiles) {
            JsonNode paper = objectMapper.readTree(goldFile.toFile());
            List<AiReviewResponse.Finding> actual =
                    reviewPaper(goldFile.getFileName().toString(), paper);
            List<JsonNode> gold = toList(paper.get("goldFindings"));

            int goldCount = gold.size();
            int actualCount = actual.size();
            int matched = matchedCount(gold, actual);
            totalGold += goldCount;
            totalMatched += matched;
            totalActual += actualCount;

            System.out.printf("eval %-40s gold=%-3d actual=%-3d matched=%-3d precision=%.2f recall=%.2f%n",
                    paper.get("name").asText(), goldCount, actualCount, matched,
                    precision(matched, actualCount), recall(matched, goldCount));
        }
        System.out.printf("eval aggregate precision=%.2f recall=%.2f (gold=%d)%n",
                precision(totalMatched, totalActual), recall(totalMatched, totalGold), totalGold);

        // ponytail: sanity floor; raise once the gold set matures and AI eval is live
        assertThat(precision(totalMatched, totalActual)).isGreaterThanOrEqualTo(0.5);
        assertThat(recall(totalMatched, totalGold)).isGreaterThanOrEqualTo(0.5);
    }

    private List<AiReviewResponse.Finding> reviewPaper(String name, JsonNode paper) throws Exception {
        Project project = project();
        Document document = document(project);
        User user = user();
        List<PaperSection> sections = sections(document, paper);
        Map<UUID, PaperSection> sectionsById = new HashMap<>();
        for (PaperSection section : sections) {
            sectionsById.put(section.getId(), section);
        }
        List<Claim> claims = claims(project, paper, sectionsById);
        List<InstructorFeedback> feedback = feedback(paper);

        ClaimRepository claimRepository = mock(ClaimRepository.class);
        ClaimContentConsistencyService consistency =
                new ClaimContentConsistencyService(claimRepository);
        ClaimEvidenceMappingRepository mappingRepository =
                mock(ClaimEvidenceMappingRepository.class);
        Map<UUID, List<ClaimEvidenceMapping>> mappingsByClaim = new HashMap<>();
        for (JsonNode mapping : toList(paper.get("mappings"))) {
            mappingsByClaim.computeIfAbsent(UUID.fromString(mapping.get("claimId").asText()),
                    id -> new ArrayList<>()).add(mapping(mapping.get("chunkText").asText()));
        }
        for (Claim claim : claims) {
            when(mappingRepository.findByClaimId(claim.getId()))
                    .thenReturn(mappingsByClaim.getOrDefault(claim.getId(), List.of()));
        }
        EvidenceFilterService evidenceFilter = new EvidenceFilterService(mappingRepository);

        CurrentUserService currentUserService = mock(CurrentUserService.class);
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        DocumentRepository documentRepository = mock(DocumentRepository.class);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        PaperSectionRepository sectionRepository = mock(PaperSectionRepository.class);
        when(sectionRepository.findByDocumentIdOrderBySectionOrderAsc(document.getId()))
                .thenReturn(sections);
        ClaimRepository claimRepositoryAll = mock(ClaimRepository.class);
        when(claimRepositoryAll.findByProjectId(project.getId())).thenReturn(claims);
        InstructorFeedbackRepository feedbackRepository = mock(InstructorFeedbackRepository.class);
        when(feedbackRepository.findByRequestProjectId(project.getId())).thenReturn(feedback);
        ReviewSnapshotRepository snapshotRepository = mock(ReviewSnapshotRepository.class);
        when(snapshotRepository.findByProjectIdAndStyleAndInputFingerprint(
                any(), anyString(), anyString())).thenReturn(Optional.empty());
        AiModelClient aiModelClient = mock(AiModelClient.class);
        when(aiModelClient.generate(anyString())).thenAnswer(invocation -> {
            String prompt = invocation.getArgument(0);
            return prompt.contains("\"assertions\":")
                    ? "{\"assertions\":[]}" : "{\"findings\":[]}";
        });

        PaperProcessingService service = new PaperProcessingServiceImpl(
                aiModelClient, sectionRepository, claimRepositoryAll,
                mock(com.evidencepilot.repository.ClaimEvidenceMappingRepository.class),
                feedbackRepository, consistency, evidenceFilter,
                mock(AuditService.class), objectMapper,
                documentRepository, mock(ProjectMapper.class), currentUserService,
                mock(PaperStandardService.class), mock(UserRepository.class),
                mock(ProjectRepository.class), mock(SystemNotificationService.class),
                mock(TexArchiveBuilder.class), snapshotRepository);

        return service.review(document.getId(), "default").findings();
    }

    private static int matchedCount(List<JsonNode> gold, List<AiReviewResponse.Finding> actual) {
        Map<String, Integer> actualCounts = new HashMap<>();
        for (AiReviewResponse.Finding finding : actual) {
            actualCounts.merge(key(finding.type(), finding.claimId(), finding.sectionId()), 1, Integer::sum);
        }
        int matched = 0;
        for (JsonNode goldFinding : gold) {
            String type = goldFinding.get("type").asText();
            UUID claimId = nullableUuid(goldFinding, "claimId");
            UUID sectionId = nullableUuid(goldFinding, "sectionId");
            Integer count = actualCounts.get(type + "|" + claimId + "|" + sectionId);
            if (count != null && count > 0) {
                matched++;
                actualCounts.merge(type + "|" + claimId + "|" + sectionId, -1, Integer::sum);
            }
        }
        return matched;
    }

    private static String key(AiReviewResponse.FindingType type, UUID claimId, UUID sectionId) {
        return type + "|" + claimId + "|" + sectionId;
    }

    private static double precision(int matched, int actual) {
        return actual == 0 ? 1.0 : (double) matched / actual;
    }

    private static double recall(int matched, int gold) {
        return gold == 0 ? 1.0 : (double) matched / gold;
    }

    private static Path goldDir() throws URISyntaxException, IOException {
        var resource = AiReviewEvalTest.class.getResource("/eval/gold");
        return resource == null ? null : Path.of(resource.toURI());
    }

    private static List<JsonNode> toList(JsonNode node) {
        List<JsonNode> result = new ArrayList<>();
        if (node != null && node.isArray()) {
            node.forEach(result::add);
        }
        return result;
    }

    private static UUID nullableUuid(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : UUID.fromString(value.asText());
    }

    private static Project project() {
        Project project = new Project();
        project.setId(UUID.randomUUID());
        project.setTitle("Eval project");
        return project;
    }

    private static User user() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setRole(UserRole.STUDENT);
        return user;
    }

    private static Document document(Project project) {
        Document document = new Document();
        document.setId(UUID.randomUUID());
        document.setProject(project);
        return document;
    }

    private static List<PaperSection> sections(Document document, JsonNode paper) {
        List<PaperSection> sections = new ArrayList<>();
        for (JsonNode node : toList(paper.get("sections"))) {
            PaperSection section = new PaperSection();
            section.setId(UUID.fromString(node.get("id").asText()));
            section.setDocument(document);
            section.setSectionTitle(node.get("title").asText());
            section.setSectionOrder(node.get("order").asInt());
            section.setContentTex(node.get("content").asText());
            sections.add(section);
        }
        return sections;
    }

    private static List<Claim> claims(Project project, JsonNode paper,
            Map<UUID, PaperSection> sectionsById) {
        List<Claim> claims = new ArrayList<>();
        for (JsonNode node : toList(paper.get("claims"))) {
            Claim claim = new Claim();
            claim.setId(UUID.fromString(node.get("id").asText()));
            claim.setProject(project);
            claim.setContent(node.get("content").asText());
            claim.setActive(true);
            JsonNode sectionId = node.get("sectionId");
            if (sectionId != null && !sectionId.isNull()) {
                claim.setSection(sectionsById.get(UUID.fromString(sectionId.asText())));
            }
            claims.add(claim);
        }
        return claims;
    }

    private static List<InstructorFeedback> feedback(JsonNode paper) {
        List<InstructorFeedback> feedback = new ArrayList<>();
        for (JsonNode node : toList(paper.get("feedback"))) {
            InstructorFeedback item = new InstructorFeedback();
            item.setId(UUID.fromString(node.get("id").asText()));
            item.setContent(node.get("content").asText());
            item.setAnswered(node.get("answered").asBoolean());
            JsonNode sectionId = node.get("sectionId");
            if (sectionId != null && !sectionId.isNull()) {
                PaperSection section = new PaperSection();
                section.setId(UUID.fromString(sectionId.asText()));
                section.setActive(true);
                item.setSection(section);
            }
            feedback.add(item);
        }
        return feedback;
    }

    private static ClaimEvidenceMapping mapping(String chunkText) {
        Document source = new Document();
        source.setId(UUID.randomUUID());
        source.setOriginalFilename("source.pdf");
        source.setDocType(DocumentType.SOURCE);
        source.setActive(true);
        DocumentChunk chunk = new DocumentChunk();
        chunk.setDocument(source);
        chunk.setText(chunkText);
        chunk.setActive(true);
        ClaimEvidenceMapping mapping = new ClaimEvidenceMapping();
        mapping.setId(UUID.randomUUID());
        mapping.setDocumentChunk(chunk);
        mapping.setStatus(MappingStatus.ACTIVE);
        return mapping;
    }
}
