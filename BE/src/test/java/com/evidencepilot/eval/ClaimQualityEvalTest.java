package com.evidencepilot.eval;

import com.evidencepilot.dto.response.ClaimQualityEvaluationResponse;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.PaperSection;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.enums.FunctionalType;
import com.evidencepilot.service.AiModelClient;
import com.evidencepilot.service.impl.AiModelClientImpl;
import com.evidencepilot.service.impl.ClaimQualityEvaluationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.io.InputStream;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ClaimQualityEvalTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void goldCasesMatchDeterministicRubricRules() throws Exception {
        GoldSet gold = goldSet();
        assertThat(gold.cases()).hasSize(12);
        Map<ClaimQualityEvaluationResponse.CriterionCode, Integer> criterionMatches =
                new EnumMap<>(ClaimQualityEvaluationResponse.CriterionCode.class);
        int totalMatches = 0;
        int decisionMatches = 0;
        int typeMatches = 0;

        for (GoldCase item : gold.cases()) {
            List<ClaimQualityEvaluationResponse.Criterion> criteria =
                    item.expected().criteria().entrySet().stream()
                            .map(entry -> new ClaimQualityEvaluationResponse.Criterion(
                                    entry.getKey(), entry.getValue(), "Human gold label"))
                            .toList();
            ClaimQualityEvaluationResponse response = ClaimQualityEvaluationResponse.from(
                    criteria, item.expected().functionalType(), item.claim());

            assertThat(response.criteria()).as(item.id())
                    .hasSize(item.expected().criteria().size());
            for (ClaimQualityEvaluationResponse.Criterion criterion : response.criteria()) {
                assertThat(criterion.score()).as(item.id() + " " + criterion.code())
                        .isEqualTo(item.expected().criteria().get(criterion.code()));
                criterionMatches.merge(criterion.code(), 1, Integer::sum);
            }
            assertThat(response.totalScore()).as(item.id())
                    .isEqualTo(item.expected().criteria().values().stream()
                            .mapToInt(Integer::intValue).sum());
            totalMatches++;
            assertThat(response.decision()).as(item.id()).isEqualTo(item.expected().decision());
            decisionMatches++;
            assertThat(response.suggestedFunctionalType()).as(item.id())
                    .isEqualTo(item.expected().functionalType());
            typeMatches++;
        }

        System.out.printf(
                "Claim Quality offline agreement: total=%d/%d, decision=%d/%d, type=%d/%d%n",
                totalMatches, gold.cases().size(), decisionMatches, gold.cases().size(),
                typeMatches, gold.cases().size());
        for (ClaimQualityEvaluationResponse.CriterionCode code
                : ClaimQualityEvaluationResponse.CriterionCode.values()) {
            System.out.printf("  %s=%d/%d%n", code,
                    criterionMatches.getOrDefault(code, 0), gold.cases().size());
        }
    }

    @Test
    void liveModelReportsAgreementWhenExplicitlyEnabled() throws Exception {
        Assumptions.assumeTrue(Boolean.parseBoolean(System.getenv("RUN_CLAIM_QUALITY_EVAL")),
                "Set RUN_CLAIM_QUALITY_EVAL=true for the manual live-model harness");
        String baseUrl = System.getenv("AI_MODEL_BASE_URL");
        Assumptions.assumeTrue(baseUrl != null && !baseUrl.isBlank(),
                "AI_MODEL_BASE_URL is required for the manual live-model harness");
        String apiKey = System.getenv("AI_MODEL_API_KEY");
        Assumptions.assumeTrue(apiKey != null && !apiKey.isBlank(),
                "AI_MODEL_API_KEY is required for the manual live-model harness");

        AiModelClient client = new AiModelClientImpl(
                RestClient.builder()
                        .requestFactory(new SimpleClientHttpRequestFactory())
                        .defaultHeader("Content-Type", "application/json")
                        .defaultHeader("Accept", "application/json")
                        .defaultHeader("X-API-Key", apiKey)
                        .build(),
                baseUrl);
        ClaimQualityEvaluationService evaluator =
                new ClaimQualityEvaluationService(client, objectMapper);
        GoldSet gold = goldSet();
        int decisionMatches = 0;
        int totalScoreMatches = 0;
        int typeMatches = 0;
        Map<ClaimQualityEvaluationResponse.CriterionCode, Integer> criterionMatches =
                new EnumMap<>(ClaimQualityEvaluationResponse.CriterionCode.class);

        for (GoldCase item : gold.cases()) {
            ClaimQualityEvaluationResponse actual = evaluator.evaluate(
                    project(item), section(item), item.claim());
            if (actual.decision() == item.expected().decision()) decisionMatches++;
            int expectedTotal = item.expected().criteria().values().stream()
                    .mapToInt(Integer::intValue).sum();
            if (actual.totalScore() == expectedTotal) totalScoreMatches++;
            if (actual.suggestedFunctionalType() == item.expected().functionalType()) typeMatches++;
            for (ClaimQualityEvaluationResponse.Criterion criterion : actual.criteria()) {
                if (criterion.score() == item.expected().criteria().get(criterion.code())) {
                    criterionMatches.merge(criterion.code(), 1, Integer::sum);
                }
            }
        }

        System.out.printf(
                "Claim Quality live agreement: decision=%d/%d, total=%d/%d, type=%d/%d%n",
                decisionMatches, gold.cases().size(), totalScoreMatches, gold.cases().size(),
                typeMatches, gold.cases().size());
        for (ClaimQualityEvaluationResponse.CriterionCode code
                : ClaimQualityEvaluationResponse.CriterionCode.values()) {
            System.out.printf("  %s=%d/%d%n", code,
                    criterionMatches.getOrDefault(code, 0), gold.cases().size());
        }
    }

    private GoldSet goldSet() throws Exception {
        try (InputStream input = getClass().getResourceAsStream(
                "/eval/gold/claims/claim-quality-v1.json")) {
            if (input == null) throw new IllegalStateException("Claim Quality gold set is missing");
            return objectMapper.readValue(input, GoldSet.class);
        }
    }

    private Project project(GoldCase item) {
        Project project = new Project();
        project.setTitle(item.projectTitle());
        return project;
    }

    private PaperSection section(GoldCase item) {
        Document document = new Document();
        document.setTitle(item.projectTitle());
        PaperSection section = new PaperSection();
        section.setDocument(document);
        section.setSectionTitle(item.sectionTitle());
        section.setContentTex(item.sectionContent());
        return section;
    }

    private record GoldSet(List<GoldCase> cases) {}

    private record GoldCase(
            String id,
            String language,
            String projectTitle,
            String sectionTitle,
            String sectionContent,
            String claim,
            Expected expected
    ) {}

    private record Expected(
            Map<ClaimQualityEvaluationResponse.CriterionCode, Integer> criteria,
            ClaimQualityEvaluationResponse.Decision decision,
            FunctionalType functionalType
    ) {}
}
